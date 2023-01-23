# About

Using the resources in this folder, one can deploy OpenShift Cert-Manager operator and an instance of HashiCorp Vault using certificates issued by Cert-Manager. 

# How

> **_PREREQUISITES:_** An OpenShift 4 cluster with OpenShift GitOps operator installed.

Initially, one should create the required namespaces for Vault and Cert-Manager and create the appropiate RBAC rules for ArgoCD service account to be able to deploy all the required resources.

```
oc apply -f manifests
```

The next step would be to configure your Vault deployment in the `values.yaml` from `helm/vault-install` Helm chart. Configure at minimum the Vault route with the `base.server.route.host` parameter.
After this, the ArgoCD Application can be deployed. 

```
oc apply -f argocd/application.yaml
```

Login to ArgoCD UI and check if the application is getting synced and Vault with Cert-Manager are getting deployed.

In the console, you can check the status of Vault pods

```
oc get pod -n hashicorp-vault
```

The next step would be to initialiaze Vault

```
oc -n hashicorp-vault exec -ti vault-0 -- vault operator init -key-threshold=1 -key-shares=1
```
```
Unseal Key 1: 7tbxdHjNqLsCAS16b0ac92jb+uvXEVSPwFZyf2Ln8Gk=

Initial Root Token: s.lSHpKvhYhjy5xwR0wtkXEk6H
```

Now it's time to unseal all Vault instances
```
oc -n hashicorp-vault exec -ti vault-0 -- vault operator unseal
```
```
Unseal Key (will be hidden):
```
```
oc -n hashicorp-vault exec -ti vault-1 -- vault operator unseal
```
```
Unseal Key (will be hidden):
```
```
oc -n hashicorp-vault exec -ti vault-2 -- vault operator unseal
```
```
Unseal Key (will be hidden):
```