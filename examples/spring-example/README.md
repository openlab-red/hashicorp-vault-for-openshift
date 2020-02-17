# Spring Example 


## Manual Sidecar Container

```
    oc project app

    oc new-build --name=spring-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/spring-example
    oc apply -f spring-example.yaml
```


## Mutating Webhook Configuration

```
oc project app

oc new-build --name=spring-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/spring-example
oc apply -f spring-inject.yaml
```
