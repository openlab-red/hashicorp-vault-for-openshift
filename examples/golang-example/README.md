# Golang Example

## Build the Application

```
oc new-project app

oc new-app --name=golang-example https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=examples/golang-example/
```

## Deploy
 
### Mutating Webhook Configuration

```
    oc apply -f examples/golang-example/python3-inject.yaml
```
