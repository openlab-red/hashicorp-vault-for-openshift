# Python Example

## Build the Application

```
oc new-project app

oc new-app --name=python3-example python:3.6~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=examples/python3-example/
```

## Deploy
 
### Manual Sidecar Container

```
    oc apply -f examples/python3-example/python3-example.yaml
```

### Mutating Webhook Configuration

```
    oc apply -f examples/python3-example/python3-inject.yaml
```