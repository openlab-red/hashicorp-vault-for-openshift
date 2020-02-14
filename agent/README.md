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
vault read --tls-skip-verify secret/example
```