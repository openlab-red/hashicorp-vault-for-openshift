# Vault

## Deploy

```
oc new-project hashicorp

oc adm policy add-scc-to-user privileged -z default

oc create configmap vault-config --from-file=vault-config=vault-config.json

oc create -f vault.yaml

oc create route reencrypt vault --port=8200 --service=vault
```

### With SDN Multi Tenant

```
oc adm  pod-network make-projects-global hashicorp
```

### With SDN Network Policy

TBD


## Configuration

```
export VAULT_ADDR=https://$(oc get route vault --no-headers -o custom-columns=HOST:.spec.host)

vault init -tls-skip-verify -key-shares=1 -key-threshold=1
```

Sample output:

```
Unseal Key 1: NRvJGYdLeUc9emtX+eWJfa+JV7I0wzLb2lTlOcK5lmU=
Initial Root Token: 4Zh3yRX5orXFqdQUXdKrNxmg
```

Export as environment variables

```
export KEYS=NRvJGYdLeUc9emtX+eWJfa+JV7I0wzLb2lTlOcK5lmU=
export ROOT_TOKEN=4Zh3yRX5orXFqdQUXdKrNxmg
export VAULT_TOKEN=$ROOT_TOKEN
```

Unseal the vault

```
vault operator unseal -tls-skip-verify $KEYS
```

## Kubernetes Auth

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

## Policy Sample

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

## Write sample data

```
vault write -tls-skip-verify secret/example password=pwd
```

## Test Client Vault

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

# Agent Vault

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

```json
{
  "token": "87kor7VqW7N4GZIAwnNWGijr",
  "accessor": "4DJJ705wjJlhb6LeSwJZ9nHX",
  "ttl": 60,
  "creation_time": "2018-10-09T12:57:35.49025018Z",
  "creation_path": "sys/wrapping/wrap",
  "wrapped_accessor": ""
}
```

Read the secret:

```
export VAULT_TOKEN=87kor7VqW7N4GZIAwnNWGijr
vault read -tls-skip-verify secret/example
```

## Side Container

TBD

```
oc project app
```


### Agent Vault


### Vault Token Handler


# References

* https://github.com/raffaelespazzoli/credscontroller
* https://blog.openshift.com/managing-secrets-openshift-vault-integration/
* https://blog.openshift.com/vault-integration-using-kubernetes-authentication-method

