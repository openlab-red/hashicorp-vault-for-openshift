# Vault

## Deploy Vault on OpenShift

```
oc new-project hashicorp-vault

oc adm policy add-scc-to-user privileged -z default
oc get scc privileged -o yaml | grep system:serviceaccount:hashicorp-vault:default

oc create configmap vault-config --from-file=vault-config=./vault/vault-config.json
oc get cm vault-config -o yaml

oc create -f ./vault/vault.yaml
oc create route reencrypt vault --port=8200 --service=vault
```

## Initialize Vault

```
export VAULT_ADDR=https://$(oc get route vault --no-headers -o custom-columns=HOST:.spec.host)
echo $VAULT_ADDR
```
vault operator init -tls-skip-verify -key-shares=1 -key-threshold=1

Save the `Unseal Key 1` and the `Initial Root Token`:

```
Unseal Key 1: NRvJGYdLeUc9emtX+eWJfa+JV7I0wzLb2lTlOcK5lmU=
Initial Root Token: 4Zh3yRX5orXFqdQUXdKrNxmg
```

And export them as environment variables, for further use:

```
export KEYS=NRvJGYdLeUc9emtX+eWJfa+JV7I0wzLb2lTlOcK5lmU=
export ROOT_TOKEN=4Zh3yRX5orXFqdQUXdKrNxmg
export VAULT_TOKEN=$ROOT_TOKEN
```

## Unseal Vault

```
vault operator unseal -tls-skip-verify $KEYS
```

## Configure Kubernetes Auth with the Vault

```
oc create sa vault-auth
oc adm policy add-cluster-role-to-user system:auth-delegator system:serviceaccount:hashicorp:vault-auth
reviewer_service_account_jwt=$(oc serviceaccounts get-token vault-auth)

pod=$(oc get pods -lapp=vault --no-headers -o custom-columns=NAME:.metadata.name)
oc exec $pod -- cat /var/run/secrets/kubernetes.io/serviceaccount/ca.crt > /tmp/ca.crt

vault auth enable -tls-skip-verify kubernetes

export OPENSHIFT_HOST=https://openshift-master.openlab.red

vault write -tls-skip-verify auth/kubernetes/config token_reviewer_jwt=$reviewer_service_account_jwt kubernetes_host=$OPENSHIFT_HOST kubernetes_ca_cert=@/tmp/ca.crt
```

## Sample Policy

```
vault policy write -tls-skip-verify policy-example policy/policy-example.hcl
```

## Authorisation

```
vault write -tls-skip-verify auth/kubernetes/role/example \
    bound_service_account_names=default bound_service_account_namespaces='app' \
    policies=policy-example \
    ttl=2h
```

## Write Sample Data

```
vault write -tls-skip-verify secret/example password=pwd
```

## Expose Vault to Other OpenShift Projects/Client Applications

### With SDN Multi Tenant

```
oc adm  pod-network make-projects-global hashicorp-vault
```

### With SDN Network Policy

TODO


## Test Vault Client

```
oc new-project app

default_account_token=$(oc sa get-token default)
vault write -tls-skip-verify auth/kubernetes/login role=example jwt=${default_account_token}

```

Sample output:

```
Key                                       Value
---                                       -----
token                                     wLflBsnHbEfJSsKYIdLVLrnx
token_accessor                            1z2BrfAgEIFZGs8VrKJTpbUh
token_duration                            2h
token_renewable                           true
token_policies                            ["default" "policy-example"]
identity_policies                         []
policies                                  ["default" "policy-example"]
token_meta_service_account_namespace      app
token_meta_service_account_secret_name    default-token-s275r
token_meta_service_account_uid            b971fe8c-cbb9-11e8-9913-2687f436e2c4
token_meta_role                           example
token_meta_service_account_name           default
```

Read the secret:

```
export VAULT_TOKEN=wLflBsnHbEfJSsKYIdLVLrnx
vault read -tls-skip-verify secret/example
```

# Vault Agent

## Standalone

```
oc project app

oc create configmap vault-agent-config --from-file=vault-agent-config=agent/vault-agent.config
oc create -f agent/vault-agent.yaml
```

Find token under */var/run/secrets/vaultproject.io/token*

```
pod=$(oc get pods -lapp=vault-agent --no-headers -o custom-columns=NAME:.metadata.name)
oc exec $pod -- cat /var/run/secrets/vaultproject.io/token

```

```
87kor7VqW7N4GZIAwnNWGijr
```

Read the secret:

```
export VAULT_TOKEN=87kor7VqW7N4GZIAwnNWGijr
vault read -tls-skip-verify secret/example
```

## Side Container

```
oc project app
```

Using Agent Vault and [Vault Agent Token Handler ](https://github.com/openlab-red/vault-agent-token-handler) as sidecar containers

### Spring Example

```
    oc new-build --name=spring-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/spring-example
    oc create -f examples/spring-example/spring-example.yaml
```

> *Note*
>
> Right now spring only read the properties file at bootstrap.
>

### Thorntail Example

```
    oc new-build --name=thorntail-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/thorntail-example
    oc create -f examples/thorntail-example/thorntail-example.yaml
```

> *Note*
>
> Right now thorntail only read the properties file at bootstrap.
>

### EAP Example

TBD

1. Deploy EAP application

    ```
        oc new-appd --name=eap-example \
            -p APPLICATION_NAME=eap-example \
            -p SOURCE_REPOSITORY_URL=https://github.com/openlab-red/hashicorp-vault-for-openshift \
            -p CONTEXT_DIR=/examples/eap-example \
            -p SOURCE_REPOSITORY_REF=master
    ```

2. Enable Annotation Property Replacement and Vault Module for properties

    ```
        oc create configmap jboss-cli --from-file=postconfigure.sh=extensions/postconfigure.sh --from-file=extensions.cli=extensions/extensions.cli
        oc volume dc/eap-example --add --name=jboss-cli -m /opt/eap/extensions -t configmap --configmap-name=jboss-cli --default-mode='0755' --overwrite
    ```

3. 

## MutatingWebhookConfiguration

TBD

[Mutating Webhook Configuration ](https://github.com/openlab-red/mutating-webhook-vault-agent)

# References

* https://github.com/raffaelespazzoli/credscontroller
* https://blog.openshift.com/managing-secrets-openshift-vault-integration/
* https://blog.openshift.com/vault-integration-using-kubernetes-authentication-method
* https://github.com/jboss-developer/jboss-eap-quickstarts
* https://github.com/thorntail/thorntail-examples
* https://github.com/spring-projects/spring-boot
* https://dzone.com/articles/how-to-inject-property-file-properties-with-cdi
