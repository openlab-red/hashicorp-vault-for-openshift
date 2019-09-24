# EAP Example 


## Manual Sidecar Container

1. Enable Annotation Property Replacement and Vault Module for properties

    ```
        oc project app

        cd examples/eap-example
        oc create configmap jboss-cli --from-file=postconfigure.sh=extensions/postconfigure.sh --from-file=extensions.cli=extensions/extensions.cli
    ```

2. Deploy EAP application

    ```     
        oc new-build --name=eap-example registry.access.redhat.com/jboss-eap-7/eap71-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/eap-example    
        oc create -f eap-example.yaml
    ``` 


## Mutating Webhook Configuration


1. Enable Annotation Property Replacement and Vault Module for properties

    ```
        cd examples/eap-example
        oc create configmap jboss-cli --from-file=postconfigure.sh=extensions/postconfigure.sh --from-file=extensions.cli=extensions/extensions.cli
    ```

2. Deploy EAP application

    ```     
        oc new-build --name=eap-example registry.access.redhat.com/jboss-eap-7/eap71-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/eap-example    
        oc create -f eap-inject.yaml
    ``` 
