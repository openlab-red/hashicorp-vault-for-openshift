# Thorntail Example 


## Manual Sidecar Container

```
    oc project app

    oc new-build --name=thorntail-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/thorntail-example
    oc create -f thorntail-example.yaml
```


## Mutating Webhook Configuration

```
oc project app

oc new-build --name=thorntail-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/thorntail-example
oc create -f thorntail-inject.yaml
```
