# Wildfly MicroProfile Example 

## Build the Application

1. Build    
    ```
        oc new-build --name=wildfly-example registry.redhat.io/jboss-eap-7/eap72-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/wildfly-example 
    ```

## Deploy
 
### Manual Sidecar Container

```
    oc apply -f examples/wildfly-example/wildfly-example.yaml
```

### Mutating Webhook Configuration

```
    oc apply -f examples/wildfly-example/wildfly-inject.yaml
```
