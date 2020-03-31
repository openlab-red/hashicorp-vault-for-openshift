# Vault Injector Deployment

```
oc project hashicorp

oc apply -f ./vault/injector/install/
```

## Additional changes

* Upgraded to MutatingWebhookConfiguration v1 API
* Agent RunAsUser has as default value the RunAsUser app container.

# Reference

* https://www.vaultproject.io/docs/platform/k8s/injector/
* https://github.com/hashicorp/vault-k8s

