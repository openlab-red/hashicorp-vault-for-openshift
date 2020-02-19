# Thorntail Example 

## Build the Application

```
oc project app

oc new-build --name=thorntail-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/thorntail-example
```

## Deploy
 
### Manual Sidecar Container

```
    oc apply -f examples/thorntail-example/thorntail-example.yaml
```

### Mutating Webhook Configuration

```
    oc apply -f examples/thorntail-example/thorntail-inject.yaml
```
