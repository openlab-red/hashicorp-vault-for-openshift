# High Availability Deployment

The official way of installing Vault to Kubernetes is using Helm Charts. This includes [support for OpenShift](https://www.vaultproject.io/docs/platform/k8s/helm/openshift).

This includes for now only the possibility to create OpenShift passthrough routes, but we [forked these official charts](http://github.com/radudd/vault-helm) and added the possibility to use reencrypt routes and to rely on OpenShift internal CA for signing Vault

## Install

### About Storage backends

Install [High Availability Storage Compatible](https://www.vaultproject.io/docs/configuration/storage) with Hashicorp High Availability.

The recommended way to deploy Vault in a HA manner is to use Vault integrated storage (which leverages on RAFT protocol)

However, we will provide some examples on how to deploy Vault using Consul as a backend, though this configuration is not maintained by us anymore.
### Deployment Storage Backend and Vault

Create new project

```
oc new-project hashicorp
```

Then fetch Consul Helm chart dependency.

```
helm repo add hashicorp https://helm.releases.hashicorp.com
```

### Vault Integrated Storage 

This is the recommended way to deploy Vault in a HA scenario in a cloud native environment. However, the challenge with this model is the creation of SSL certificates containing SAN capabilities.
OpenShift Service CA Signer doesn't support creation of SSL certificates with SAN support, hence another mechanism will need to be used for generating the certificates. 
SAN capable SSL certificates are required because the they need to match all of the below hosts: 
* Vault pods (using headless services) for communication between members 
* Vault service 
* Vault route

> **_Tip:_**  One of the ways to generate SSL certificates with SAN capabilities is to use [CertManager](https://cert-manager.io/v0.15-docs/installation/openshift/). 
 
Let's imagine we have already SSL certificates created for Vault with the previously SANs configured.

Let's create a Kubernetes Secret out of them:

```
oc create secret tls vault-certs --cert=vault.crt --key=vault.key
```

Define Vault Route

```
export VAULT_ROUTE=vault.apps.mycluster.com
```

Update the configuration paramaeters as required in `manifests/override-ha-raft.yaml`
> **_Important:_**  Update `leader_tls_servername` with the same value as VAULT_ROUTE

Install Vault

```
helm install vault hashicorp/vault -f values.yaml \
--set server.route.host=$VAULT_ROUTE \
--set server.extraEnvironmentVars.VAULT_TLS_SERVER_NAME=$VAULT_ROUTE \
-n hashicorp
```


### Consul HA (not maintained)

#### No ACL

Before installing Consul and Vault, clone our forked repository
```
git clone -b openshift4 --single-branch https://github.com/radudd/vault-helm.git
```

> :warning: **DISCLAIMER**: HashiCorp **doesn't recommend** that Vault connects directly to Consul backend, but [through the Consul Agents](https://learn.hashicorp.com/vault/operations/ops-vault-ha-consul#consul-client-agent-configuration). However, as the Consul Agents deployment on OpenShift / K8S requires the SecurityContext to allow [opening the _8502_ _hostPort_](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#host-namespaces) on the OpenShift nodes, we will disable Agent deployment in our example. Hence we will not respect the recommendation and we will connect directly to Consul. **Consider this carefully when moving to a production deployment.**

> :warning: **DISCLAIMER**: Consul doesn't use ACLs for this scenario, so Consul endpoint must be secured. Anyone with anonymous access to Consul might *delete* Vault data. For production usage, you should implement ACLs. Use the following references: [Vault](https://www.vaultproject.io/docs/configuration/storage/consul#acls) and [Consul](https://www.consul.io/docs/k8s/helm#v-global-acls-bootstraptoken). You can also check the [minimal implementation](#Consul-HA-ACL) from this repository.


If deploying HA Vault with Consul with no agents and no ACLs, update the `override/consul-noacl.yaml` according to your needs.

Then deploy Consul (from official charts)

```
helm install vault-backend hashicorp/consul -f override/consul-noacl.yaml
```

Adapt `override/vault-ha-consul-noacl.yaml` to your needs and then deploy Vault (from our forked repo)

```
helm install vault . -f override/vault-ha-consul-noacl.yaml
```

#### Using ACLs

> :warning: **DISCLAIMER**: HashiCorp **doesn't recommend** that Vault connects directly to Consul backend, but [through the Consul Agents](https://learn.hashicorp.com/vault/operations/ops-vault-ha-consul#consul-client-agent-configuration). However, as the Consul Agents deployment on OpenShift / K8S requires the SecurityContext to allow [opening the _8502_ _hostPort_](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#host-namespaces) on the OpenShift nodes, we will disable Agent deployment in our example. Hence we will not respect the recommendation and we will connect directly to Consul. **Consider this carefully when moving to a production deployment.**

If deploying HA Vault with Consul with no agents and you want to use ACLs, update the `override/consul-acl.yaml` according to your needs.

Then deploy Consul (from official charts)

```
helm install vault-backend hashicorp/consul -f override/consul-acl.yaml
```

Extract the generated ACL token

```
TOKEN=`oc get secret consul-consul-bootstrap-acl-token -o template --template '{{.data.token}}'|base64 -d`
```

Add this token to Vault override

```
sed -i "/token/s/dummytoken/$TOKEN" override/vault-ha-consul-acl.yaml
```

Adapt `override/vault-ha-consul-acl.yaml` to your needs and then deploy Vault (from our forked repo)

```
helm install vault . -f override/vault-ha-consul-acl.yaml
```

## Post install

The following kubernetes components will be created.

Injector components:
* injector ClusterRole 
* injector ClusterRoleBinding
* injector MutatingWebhookConfiguration
* injector Deployment
* injector ServiceAccount

Server components:
* server ClusterRole 
* server ClusterRoleBinding
* server ServiceAccount
* server ConfigMap
* server Service
* server Service Active
* server Service Internal
* server Service Standby
* server StatefulSet
* server Route
* server NetworkPolicy

>
> server ClusterRoleBinding allows vault service account to leverage Kubernetes oauth with the oauth-delegator ClusterRole
>

>
> In case of OpenShift SDN Multitenant
>

```
oc adm pod-network make-projects-global hashicorp
```


### Initialize Vault

In case of High Availability, the unseal has to be done in all vault replicas. 

```
oc rsh ha-vault-0

vault operator init -key-shares=1 -key-threshold=1 -tls-skip-verify
```

Save the `Unseal Key 1` and the `Initial Root Token`:

```
Unseal Key 1: hhL/LRDPsSGRzG8N8UvEuHBo7TC4GOGyKV6VwhX2OHU=
Initial Root Token: s.HUA25MAzSqgguvqW8NozZP0Z

```

And export them as environment variables, for further use:

```
export KEYS=hhL/LRDPsSGRzG8N8UvEuHBo7TC4GOGyKV6VwhX2OHU=
export ROOT_TOKEN=s.HUA25MAzSqgguvqW8NozZP0Z
export VAULT_TOKEN=$ROOT_TOKEN
```

### Manual Unseal Vault

For each replica:

```
export KEYS=hhL/LRDPsSGRzG8N8UvEuHBo7TC4GOGyKV6VwhX2OHU=
export ROOT_TOKEN=s.HUA25MAzSqgguvqW8NozZP0Z
export VAULT_TOKEN=$ROOT_TOKEN

vault operator unseal -tls-skip-verify $KEYS
```

### Auto Unseal Vault

More information about Auto Unseal: https://learn.hashicorp.com/vault/operations/autounseal-transit


# Reference
* https://github.com/hashicorp/vault-helm
