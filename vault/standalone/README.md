
# Standalone Deployment

Single instance installation

```
oc new-project hashicorp

oc apply -f ./vault/standalone/install/
```

The following kubernetes components will be created:

* vault-server-binding ClusterRoleBinding
* vault ServiceAccount
* vault-storage PersistentVolumeClaim with 10Gi size
* vault-config ConfigMap
* vault Service
* vault Deployment
* vault Route
* vault NetworkPolicy

>
> vault-server-binding ClusterRoleBinding allows vault service account to leverage Kubernetes oauth with the oauth-delegator ClusterRole
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
