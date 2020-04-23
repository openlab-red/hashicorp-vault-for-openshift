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

* Upgraded to MutatingWebhookConfiguration v1 API: [#112](https://github.com/hashicorp/vault-k8s/pull/112)
* Agent RunAsUser has as a default value the RunAsUser defined in the application container: [#126](https://github.com/hashicorp/vault-k8s/pull/126)

## vault-k8s container image

The [openlabred/vault-k8s:0.3.1](https://hub.docker.com/layers/openlabred/vault-k8s/0.3.1/images/sha256-ecef1945754a7334a4c8591a6bb00c37fca2789366351fea4b41f9167ecd8529?context=repo) image which contains the additional changes.

Source code available: https://github.com/openlab-red/vault-k8s/tree/openshift

# Reference

* https://www.vaultproject.io/docs/platform/k8s/injector/
* https://github.com/hashicorp/vault-k8s

