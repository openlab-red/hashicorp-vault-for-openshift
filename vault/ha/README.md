# High Availability Deployment

High Availability installation with 3 replicas.

## Prerequisite

Install [High Availability Storage Compatible](https://www.vaultproject.io/docs/configuration/storage) with Hashicorp High Availability.

The Consul storage backend is officially supported by HashiCorp.

```
oc new-project hashicorp

git clone https://github.com/hashicorp/consul-helm.git /tmp/consul-helm
helm install ha-backend /tmp/consul-helm
```

>
> Consul Helm has a fixed securityContext.fsGroup: 1000
> The vaule 1000 is not an allowed group for OpenShift, will apply a patch to remove it.
>

```
oc -n hashicorp patch sts consul-consul-server --type json --patch="[
  {"op": "remove", "path": "/spec/template/spec/securityContext"}
]"
```

Expose Consul UI

```
oc -n hashicorp create route reencrypt consul --port=8500 --service=consul-consul-server
```

## Vault Installation

The official way of installing Vault to Kubernetes is using Helm Charts. Their latest release includes [beta support for OpenShift](https://www.vaultproject.io/docs/platform/k8s/helm/openshift).

This includes for now only the possibility to create OpenShift passthrough routes, but we forked these official charts and added the possibility to use reencrypt routes and to rely on OpenShift internal CA for signing Vault. We are working with Hashicorp to include these features in further Helm charts releases.

```
# Clone the forked repository
git clone -b openshift4 --single-branch https://github.com/radudd/vault-helm.git

# Define Route 
export VAULT_URL=vault.apps.domain.name

# Create override file
cat <<EOF > override-ha.yaml
global:
  tlsDisable: false
  openshift: true

server:
  route:
    enabled: true
    host: $VAULT_URL
  standalone:
    enabled: false
  ha:
    enabled: true
    replicas: 3
    config: |
      ui = true
      listener "tcp" {
        address = "[::]:8200"
        cluster_address = "[::]:8201"
        tls_cert_file = "/var/run/secrets/kubernetes.io/certs/tls.crt"
        tls_key_file = "/var/run/secrets/kubernetes.io/certs/tls.key"
      }
      storage "consul" {
        path = "vault"
        address = "ha-backend-consul-server:8500"
      }
      service_registration "kubernetes" {}
EOF

# Install Vault
helm install ha . -f override-ha.yaml
```

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
oc adm  pod-network make-projects-global hashicorp
```


## Initialize Vault

In case of High Availability, the unseal has to be done in all vault replicas. 

```
oc rsh vault-0

vault operator init -key-shares=1 -key-threshold=1
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

vault operator unseal $KEYS
```

### Auto Unseal Vault

More information about Auto Unseal: https://learn.hashicorp.com/vault/operations/autounseal-transit


# Reference
* https://github.com/hashicorp/vault-helm