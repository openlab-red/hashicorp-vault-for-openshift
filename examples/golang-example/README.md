# Golang Example

This example is injecting secrets using ConfigMaps instead of templating them directly in deployment annotations: https://www.vaultproject.io/docs/platform/k8s/injector/examples#configmap-example

## Build the Application

```
oc new-project app

oc new-app --name=golang-example https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=examples/golang-example/
```

## Deploy
 
### Mutating Webhook Configuration

```
    oc apply -f examples/golang-example/golang-inject.yaml
```
