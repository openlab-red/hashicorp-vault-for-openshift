# Quarkus Example Example 

## Build the Application

1. Build    
    ```
        oc new-build --name=quarkus-example registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/quarkus-example
    ```

## Deploy

### Mutating Webhook Configuration

```
    oc apply -f examples/quarkus-example/quarkus-inject.yaml
```