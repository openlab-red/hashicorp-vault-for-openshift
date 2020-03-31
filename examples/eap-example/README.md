# EAP Example 

## Build the Application

1. Enable Annotation Property Replacement and Vault Module for properties

    ```
        oc project app

        oc create configmap jboss-cli --from-file=postconfigure.sh=extensions/postconfigure.sh --from-file=extensions.cli=extensions/extensions.cli

2. Build    
    ```
        oc new-build --name=eap-example registry.access.redhat.com/jboss-eap-7/eap71-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/eap-example 
    ```

## Deploy
 
### Manual Sidecar Container

```
    oc apply -f examples/eap-example/eap-example.yaml
```

### Mutating Webhook Configuration

```
    oc apply -f examples/eap-example/eap-inject.yaml
```
