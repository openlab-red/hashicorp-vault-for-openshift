#!/bin/sh

export CA_BUNDLE=$(oc get secret vault-certs -n hashicorp-vault -o json | jq -r '.data."ca.crt"')

oc apply -f default-token.yaml

cat <<EOF| oc apply -f -
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: team-one-issuer-vault
  namespace: team-one
spec:
  vault:
    path: app-pki/team-one/sign/team-one
    server: https://vault-active.hashicorp-vault.svc:8200
    caBundle: $CA_BUNDLE
    auth:
      kubernetes:
        role: team-one
        mountPath: /v1/auth/app-kubernetes/team-one
        secretRef:
          key: token
          name: default-token
EOF
