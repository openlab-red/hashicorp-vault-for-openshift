## Standalone

```
oc project app
oc apply -f agent/install/ -n app
```

Find token under */var/run/secrets/vaultproject.io/token*

```
POD=$(oc get pods -lapp.kubernetes.io/name=vault-agent --no-headers -o custom-columns=NAME:.metadata.name -n app)
oc -n app exec $POD -- cat /var/run/secrets/vaultproject.io/token
```

```
s.u8TtYRtHXZCz3KWbmlkLIvMc
```

Read the secret:

```
export VAULT_TOKEN=s.u8TtYRtHXZCz3KWbmlkLIvMc
vault read --tls-skip-verify secret/example
```