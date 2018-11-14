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

vault operator init -tls-skip-verify -key-shares=1 -key-threshold=1
```

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
oc adm policy add-cluster-role-to-user system:auth-delegator system:serviceaccount:hashicorp-vault:vault-auth
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

```yml
apiVersion: extensions/v1beta1
kind: NetworkPolicy
metadata:
  name: allow-vault
spec:
  ingress:
  - from:
    - namespaceSelector: {}
    ports:
    - port: 8200
      protocol: TCP
  podSelector:
    matchLabels:
      app: vault
  policyTypes:
  - Ingress
```

```
oc apply -f vault/app-allow-vault.yaml
```


## Test Vault Client

```
oc new-project app

default_account_token=$(oc sa get-token default -n app)
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

## Vault Database secret

### Deploy postgresql

```
oc new-app postgresql-persistent \
    --name=postgresql -lname=postgresql  \
    --param DATABASE_SERVICE_NAME=postgresql --param POSTGRESQL_DATABASE=sampledb \
    --param POSTGRESQL_USER=user --param POSTGRESQL_PASSWORD=redhat \
    --param VOLUME_CAPACITY=1Gi \
    --env POSTGRESQL_ADMIN_PASSWORD=postgres 
```

### Enable datatabase secret in vault

```
vault secrets enable -tls-skip-verify database
```

### Install postgresql plugin

```
vault write -tls-skip-verify database/config/postgresql \
    plugin_name=postgresql-database-plugin \
    allowed_roles="pg-readwrite" \
    connection_url="postgresql://{{username}}:{{password}}@postgresql.hashicorp-vault.svc:5432/sampledb?sslmode=disable" \
    username="postgres" \
    password="postgres" 
```

### Role mapping

```

vault write -tls-skip-verify database/roles/pg-readwrite \
    db_name=postgresql \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
        GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

```
    
### Sample Policy

The policy contains both secret/example path and database/creds/pg-readwrite

```
vault policy write -tls-skip-verify pg-readwrite policy/policy-database.hcl 
```


### Authorisation

```
vault write -tls-skip-verify auth/kubernetes/role/example \
    bound_service_account_names=default bound_service_account_namespaces='app' \
    policies=pg-readwrite \
    ttl=2h 
```


### Read credentials

```
vault read -tls-skip-verify database/creds/pg-readwrite

```

Sample output:
```
Key                Value
---                -----
lease_id           database/creds/pg-readwrite/2R94Xr0qfA6JNeWSx5O2I375
lease_duration     1h
lease_renewable    true
password           A1a-Sx82lF4AET6khzbj
username           v-root-pg-readw-57OZENOr1AYEhqt3n4xY-1541144444
```

Verify as well in Postgresql

```
sh-4.2$ psql
psql (9.6.10)
Type "help" for help.

postgres=# \du
                                                      List of roles
                    Role name                    |                         Attributes                         | Member of
-------------------------------------------------+------------------------------------------------------------+-----------
 postgres                                        | Superuser, Create role, Create DB, Replication, Bypass RLS | {}
 v-root-pg-readw-57OZENOr1AYEhqt3n4xY-1541144444 | Password valid until 2018-11-02 08:40:49+00                | {}
```

### Test Vault Client

```
oc project app

default_account_token=$(oc sa get-token default -n app)
vault write -tls-skip-verify auth/kubernetes/login role=pg-readwrite jwt=${default_account_token}

```

Sample Output:

```
Key                                       Value
---                                       -----
token                                     418R4AnbyUKNWEPR8uTbUQyR
token_accessor                            WopxlNqCAQdcid0MlhYBtDFB
token_duration                            2h
token_renewable                           true
token_policies                            ["default" "pg-readwrite"]
identity_policies                         []
policies                                  ["default" "pg-readwrite"]
token_meta_service_account_name           default
token_meta_service_account_namespace      app
token_meta_service_account_secret_name    default-token-t265w
token_meta_service_account_uid            0aea2a8e-db8f-11e8-b25a-026c5425dd64
token_meta_role                           pg-readwrite
```

Read the secret:

```
export VAULT_TOKEN=418R4AnbyUKNWEPR8uTbUQyR
vault read -tls-skip-verify database/creds/pg-readwrite
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

## Manual Sidecar Container

Using **Agent Vault** and **Vault Secret Fetcher** as sidecar containers

Follow the instruction from [Vault Secret Fetcher ](https://github.com/openlab-red/vault-secret-fetcher) to publish the vault secret fetcher image in OpenShift.


> **Note**
>
> Right now all the examples only read the properties file at bootstrap.
>

### Spring Example

```
    oc project app

    oc new-build --name=spring-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/spring-example
    oc create -f examples/spring-example/spring-example.yaml
```

### Thorntail Example

```
    oc project app

    oc new-build --name=thorntail-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/thorntail-example
    oc create -f examples/thorntail-example/thorntail-example.yaml
```

### EAP Example

1. Enable Annotation Property Replacement and Vault Module for properties

    ```
        oc project app

        cd examples/eap-example
        oc create configmap jboss-cli --from-file=postconfigure.sh=extensions/postconfigure.sh --from-file=extensions.cli=extensions/extensions.cli
    ```

2. Deploy EAP application

    ```     
        oc new-build --name=eap-example registry.access.redhat.com/jboss-eap-7/eap71-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/eap-example    
        oc create -f examples/eap-example/eap-example.yaml
    ``` 

## Mutating Webhook Configuration

1. Configure Mutating WebHook

    Follow the setup instruction from [Mutating Webhook Configuration ](https://github.com/openlab-red/mutating-webhook-vault-agent)

2. Enable the vault webhook for the **app** project

    ```
    oc label namespace app vault-agent-webhook=enabled
    ```
        
### Spring Example

```
oc project app

oc new-build --name=spring-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/spring-example
oc create -f examples/spring-example/spring-inject.yaml
```



### Thorntail Example

```
oc project app

oc new-build --name=thorntail-example  registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/thorntail-example
oc create -f examples/thorntail-example/thorntail-inject.yaml
```

### EAP Example


1. Enable Annotation Property Replacement and Vault Module for properties

    ```
        cd examples/eap-example
        oc create configmap jboss-cli --from-file=postconfigure.sh=extensions/postconfigure.sh --from-file=extensions.cli=extensions/extensions.cli
    ```

2. Deploy EAP application

    ```     
        oc new-build --name=eap-example registry.access.redhat.com/jboss-eap-7/eap71-openshift~https://github.com/openlab-red/hashicorp-vault-for-openshift --context-dir=/examples/eap-example    
        oc create -f examples/eap-example/eap-inject.yaml
    ``` 

# References

* https://github.com/raffaelespazzoli/credscontroller
* https://blog.openshift.com/managing-secrets-openshift-vault-integration/
* https://blog.openshift.com/vault-integration-using-kubernetes-authentication-method
* https://github.com/jboss-developer/jboss-eap-quickstarts
* https://github.com/thorntail/thorntail-examples
* https://github.com/spring-projects/spring-boot
* https://dzone.com/articles/how-to-inject-property-file-properties-with-cdi
