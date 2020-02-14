# Vault

## Deploy Vault on OpenShift

### [Standalone Deployment](vault/standalone/README.md)

### [High Availability Deployment](vault/ha/README.md)

## Configure Kubernetes Auth with the Vault

* Standalone
    ```
    POD=$(oc get pods -lapp.kubernetes.io/name=vault --no-headers -o custom-columns=NAME:.metadata.name)
    oc rsh $POD
    ```

* HA
    ```
    oc rsh vault-0
    ```

Exec the following command.

```
export KEYS=60Lr2MnuPlmuKnFBoNgWWYjiUSdQ9NEkXpyFUsKnMKc=
export ROOT_TOKEN=s.v4mWjWAPsf78LBV6LQ0DzVzM
export VAULT_TOKEN=$ROOT_TOKEN

JWT=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
KUBERNETES_HOST=https://${KUBERNETES_PORT_443_TCP_ADDR}:443

vault auth enable --tls-skip-verify kubernetes
vault write --tls-skip-verify auth/kubernetes/config token_reviewer_jwt=$JWT kubernetes_host=$KUBERNETES_HOST kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
```

## Use Vault from your local

```
export VAULT_ADDR=https://$(oc get route vault --no-headers -o custom-columns=HOST:.spec.host)
```

### Sample Policy

```
vault policy write --tls-skip-verify policy-example policy/policy-example.hcl
```

### Authorisation

```
vault write --tls-skip-verify auth/kubernetes/role/example \
    bound_service_account_names=default bound_service_account_namespaces='app' \
    policies=policy-example \
    ttl=2h
```

### Write Sample Data

```
vault secrets enable --tls-skip-verify -path=secret kv
vault write --tls-skip-verify  secret/example password=pwd
```

### Test Vault Client

```
oc new-project app

JWT=$(oc sa get-token default -n app)
vault write --tls-skip-verify auth/kubernetes/login role=example jwt=${JWT}

```

Sample output:

```
Key                                       Value
---                                       -----
token                                     s.Uq8cOrLAIFdFh4kxfkKpMicx
token_accessor                            ZGqVZg8FMzA6mlBUufp894FK
token_duration                            2h
token_renewable                           true
token_policies                            ["default" "policy-example"]
identity_policies                         []
policies                                  ["default" "policy-example"]
token_meta_role                           example
token_meta_service_account_name           default
token_meta_service_account_namespace      app
token_meta_service_account_secret_name    default-token-v8fjh
token_meta_service_account_uid            4c69a1f8-fc93-4300-958c-04f349936431
```

Read the secret:

```
export VAULT_TOKEN=s.Uq8cOrLAIFdFh4kxfkKpMicx
vault read --tls-skip-verify secret/example
```

### Vault Database secret

#### Deploy postgresql

```
oc new-app postgresql-persistent \
    --name=postgresql -lname=postgresql  \
    --param DATABASE_SERVICE_NAME=postgresql --param POSTGRESQL_DATABASE=sampledb \
    --param POSTGRESQL_USER=user --param POSTGRESQL_PASSWORD=redhat \
    --param VOLUME_CAPACITY=1Gi \
    --env POSTGRESQL_ADMIN_PASSWORD=postgres 
```

#### Enable datatabase secret in vault

```
vault secrets enable database
```

#### Install postgresql plugin

```
vault write --tls-skip-verify database/config/postgresql \
    plugin_name=postgresql-database-plugin \
    allowed_roles="pg-readwrite" \
    connection_url="postgresql://{{username}}:{{password}}@postgresql.app.svc:5432/sampledb?sslmode=disable" \
    username="postgres" \
    password="postgres" 
```

#### Role mapping

```

vault write --tls-skip-verify database/roles/pg-readwrite \
    db_name=postgresql \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
        GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

```
    
#### Sample Policy

The policy contains both secret/example path and database/creds/pg-readwrite

```
vault policy write --tls-skip-verify pg-readwrite policy/policy-database.hcl 
```


#### Authorisation

```
vault write --tls-skip-verify auth/kubernetes/role/example \
    bound_service_account_names=default bound_service_account_namespaces='app' \
    policies=pg-readwrite \
    ttl=2h 
```


#### Read credentials

```
vault read --tls-skip-verify database/creds/pg-readwrite

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

#### Test Vault Client

```
oc project app

JWT=$(oc sa get-token default -n app)
vault write --tls-skip-verify auth/kubernetes/login role=pg-readwrite jwt=${JWT}

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
vault read  database/creds/pg-readwrite
vault read  secret/example
```

# Vault Agent

## [Standalone Deployment](agent/README.md)

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
        oc create -f eap-example.yaml
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
