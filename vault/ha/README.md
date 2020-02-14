# High Availability Deployment

High Availability installation with 3 replicas.

## Prerequisite

Install [High Availability Storage Compatible](https://www.vaultproject.io/docs/configuration/storage) with Hashicorp High Availability.

The Consul storage backend is officially supported by HashiCorp.

```
oc new-project hashicorp

git clone https://github.com/hashicorp/consul-helm.git /tmp/consul-helm
helm install consul /tmp/consul-helm
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

>
> Vault helm chart is not used on purpose, the aim is to understand all components involed.
>
> If you like to use vault helm chart in OpenShift consider the following.
>
> IPC_LOCK capability is fixed and it cannot be removed. This is not necessary because in OpenShift the swap is disabled.
>
> The User, Group and fsGroup id are fixed on the manifest, the values can be overridden but still is not necessary to force the id.
>


```
oc apply -f ./vault/ha/install/
```

The following kubernetes components will be created:

* vault-server-binding ClusterRoleBinding
* vault ServiceAccount
* vault-config ConfigMap
* vault PodDisruptionBudget
* vault StatefulSet
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