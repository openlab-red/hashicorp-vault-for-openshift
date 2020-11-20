
# Standalone Deployment

The official way of installing Vault to Kubernetes is using Helm Charts. This includes [support for OpenShift](https://www.vaultproject.io/docs/platform/k8s/helm/openshift).

This includes for now only the possibility to create OpenShift passthrough routes, but we [forked these official charts](http://github.com/radudd/vault-helm) and added the possibility to use reencrypt routes and to rely on OpenShift internal CA for signing Vault

## Deployment

```
# Clone the forked repository
git clone -b openshift4 --single-branch https://github.com/radudd/vault-helm.git

# Define Route 
export VAULT_URL=vault.apps.domain.name

# Create override file
cat <<EOF > override-standalone.yaml
global:
  tlsDisable: false
  openshift: true

server:
  route:
    enabled: true
    host: $VAULT_URL
  standalone:
    enabled: true
    config: |
      ui = true
      listener "tcp" {
        address = "[::]:8200"
        cluster_address = "[::]:8201"
        tls_cert_file = "/var/run/secrets/kubernetes.io/certs/tls.crt"
        tls_key_file = "/var/run/secrets/kubernetes.io/certs/tls.key"
      }
      storage "file" {
        path = "/vault/data"
      }
  ha:
    enabled: false
EOF

# Install Vault
helm install standalone . -f override-standalone.yaml
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
* server PersistentVolumeClaim (template from StatefulSet)

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

```
POD=$(oc get pods -lapp.kubernetes.io/name=vault --no-headers -o custom-columns=NAME:.metadata.name)
oc rsh $POD

vault operator init --tls-skip-verify -key-shares=1 -key-threshold=1
```

Save the `Unseal Key 1` and the `Initial Root Token`:

```
Unseal Key 1: QzlUGvdPbIcM83UxyjuGd2ws7flZdNimQVCNbUvI2aU=

Initial Root Token: s.UPBPfhDXYOtnv8mELhPA4br7
```

And export them as environment variables, for further use:

```
export KEYS=QzlUGvdPbIcM83UxyjuGd2ws7flZdNimQVCNbUvI2aU=
export ROOT_TOKEN=s.UPBPfhDXYOtnv8mELhPA4br7
export VAULT_TOKEN=$ROOT_TOKEN
```

## Unseal Vault

```
vault operator unseal --tls-skip-verify $KEYS
```
