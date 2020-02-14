## Standalone

```
oc project app
oc apply -f agent/install/ -n app
```

Find token under */var/run/secrets/vaultproject.io/token*

```
pod=$(oc get pods -lapp.kubernetes.io/name=vault-agent --no-headers -o custom-columns=NAME:.metadata.name -n app)
oc exec $pod -- cat /var/run/secrets/vaultproject.io/token
```

```
87kor7VqW7N4GZIAwnNWGijr
```

Read the secret:

```
export VAULT_TOKEN=87kor7VqW7N4GZIAwnNWGijr
vault read --tls-skip-verify secret/example
```