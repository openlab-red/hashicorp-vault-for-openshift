# High Availability Deployment

The official way of installing Vault to Kubernetes is using Helm Charts. This includes [support for OpenShift](https://www.vaultproject.io/docs/platform/k8s/helm/openshift).

This includes for now only the possibility to create OpenShift passthrough routes, but we [forked these official charts](http://github.com/radudd/vault-helm) and added the possibility to use reencrypt routes and to rely on OpenShift internal CA for signing Vault

## Install

### About Storage backends

Install [High Availability Storage Compatible](https://www.vaultproject.io/docs/configuration/storage) with Hashicorp High Availability.

We will use Consul storage backend in our examples which is officially supported bby HashiCorp.

> :warning: **DISCLAIMER**: HashiCorp **doesn't recommend** that Vault connects directly to Consul backend, but [through the Consul Agents](https://learn.hashicorp.com/vault/operations/ops-vault-ha-consul#consul-client-agent-configuration). However, as the Consul Agents deployment on OpenShift / K8S requires the SecurityContext to allow [opening the _8502_ _hostPort_](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#host-namespaces) on the OpenShift nodes, we will disable Agent deployment in our example. Hence we will not respect the recommendation and we will connect directly to Consul. **Consider this carefully when moving to a production deployment.**

> :warning: **DISCLAIMER**: Consul doesn't use ACLs for our first scenario, so Consul endpoint must be secured. Anyone with anonymous access to Consul might *delete* Vault data. For production usage, you should implement ACLs. Use the following references: [Vault](https://www.vaultproject.io/docs/configuration/storage/consul#acls) and [Consul](https://www.consul.io/docs/k8s/helm#v-global-acls-bootstraptoken). You can also check the [minimal implementation](#Consul-HA-ACL) from this repository.


### Deployment Storage Backend and Vault

Create new project

```
oc new-project hashicorp
```

Then fetch Consul Helm chart dependency.

```
helm repo add hashicorp https://helm.releases.hashicorp.com
```

Before installing Consul and Vault, clone our forked repository
```
git clone -b openshift4 --single-branch https://github.com/radudd/vault-helm.git
```

### Consul HA and no ACLs

If deploying HA Vault with Consul with no agents and no ACLs, update the `override/consul-noacl.yaml` according to your needs.

Then deploy Consul (from official charts)

```
helm install vault-backend hashicorp/consul -f override/consul-noacl.yaml
```

Adapt `override/vault-ha-consul-noacl.yaml` to your needs and then deploy Vault (from our forked repo)

```
helm install vault . -f override/vault-ha-consul-noacl.yaml
```

### Consul HA ACL

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
