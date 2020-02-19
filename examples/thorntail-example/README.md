# Thorntail Example 


## Manual Sidecar Container

```
    oc project app

    cd examples/thorntail-example
    oc new-build --name=thorntail-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/thorntail-example
    oc apply -f thorntail-example.yaml
```


## Mutating Webhook Configuration

```
oc project app

cd examples/thorntail-example
oc new-build --name=thorntail-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/thorntail-example
oc apply -f thorntail-inject.yaml
```
