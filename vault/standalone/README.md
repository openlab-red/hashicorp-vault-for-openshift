
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
* vault Serivce
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
export VAULT_ADDR=https://$(oc get route vault --no-headers -o custom-columns=HOST:.spec.host)
echo $VAULT_ADDR

vault operator init --tls-skip-verify -key-shares=1 -key-threshold=1
```

Save the `Unseal Key 1` and the `Initial Root Token`:

```
Unseal Key 1: 0kTgW1xkR5ffzJIhXq03E/n1hRsejfAvyODyqDu2RZg=

Initial Root Token: s.HwgcebFWZ4cJ6VuqAgHTmkCS
```

And export them as environment variables, for further use:

```
export KEYS=0kTgW1xkR5ffzJIhXq03E/n1hRsejfAvyODyqDu2RZg=
export ROOT_TOKEN=s.HwgcebFWZ4cJ6VuqAgHTmkCS
export VAULT_TOKEN=$ROOT_TOKEN
```

## Unseal Vault

```
vault operator unseal --tls-skip-verify $KEYS
```