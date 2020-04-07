# Vault Injector Deployment

```
oc project hashicorp

oc apply -f ./vault/injector/install/
```

The following kubernetes components will be created:

* vault-injector ClusterRole
* vault-injector ClusterRoleBinding
* vault-injector ServiceAccount
* vault-injector Deployment
* vault-injector Service
* vault-injector NetworkPolicy
* vault-injector MutatingWebhookConfiguration

## Additional changes

* Upgraded to MutatingWebhookConfiguration v1 API
* Agent RunAsUser has as a default value the RunAsUser defined in the application container.

# Reference

* https://www.vaultproject.io/docs/platform/k8s/injector/
* https://github.com/hashicorp/vault-k8s

