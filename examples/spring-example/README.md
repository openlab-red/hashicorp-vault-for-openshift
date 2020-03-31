# Spring Example 

## Build the Application

```
oc project app

oc new-build --name=spring-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/spring-example
```

## Deploy
 
### Manual Sidecar Container

```
    oc apply -f examples/spring-example/spring-example.yaml
```

### Mutating Webhook Configuration

```
    oc apply -f examples/spring-example/spring-inject.yaml
```