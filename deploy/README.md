# About

With the materials provided in this folder, it is possible to deploy the OpenShift Cert-Manager operator and an instance of HashiCorp Vault using certificates issued by Cert-Manager. These resources will allow you to set up a secure and efficient system for managing your certificates, ensuring that your applications and services are properly secured.

## How

### Prerequisites

An OpenShift 4 cluster with the OpenShift GitOps operator installed can be used to manage and automate the deployment of applications and configurations using Git-based version control. This allows for easy collaboration and rollbacks, as well as the ability to track changes and ensure that the cluster is always in a known state.

One should start by creating the necessary namespaces for Vault and Cert-Manager, and then set up the appropriate RBAC rules for the ArgoCD service account to be able to deploy all the necessary resources. This will ensure that the deployment of the OpenShift Cert-Manager operator and an instance of HashiCorp Vault using certificates issued by Cert-Manager can proceed smoothly.

```bash
oc apply -f prereq/
```

Next, generate the necessary certificate authority chain using the `../script/ca-chain.sh` script before creating a Cert-Manager Issuer and deploying the necessary resources.

```bash
sh ../script/ca-chain.sh
```

Deploy the intermediate certificate authority as Secrets on the OpenShift cluster:

```bash
oc create secret tls intermediate --cert=ca-chain/intermediate/ca.crt --key=ca-chain/intermediate/ca.key -n hashicorp-vault
```

In the next step, we will use the `values.yaml` file from the `helm/vault-install` Helm chart to configure the Vault deployment. We will set the `base.server.route.host` parameter to the cluster's base domain obtained by running `oc get dns cluster -o jsonpath='{.spec.baseDomain}'`. 

For instance: `echo vault.apps.$(oc get dns cluster -o jsonpath='{.spec.baseDomain}')`

Once this is done, we can proceed to deploy the ArgoCD Application.

```bash
oc apply -f argocd/cert-manager.yaml
oc apply -f argocd/vault.yaml
```

At this stage, log in to the ArgoCD user interface and verify that the application is being synced and that Vault and Cert-Manager are being deployed. Additionally, you can check the status of the Vault pods within the console.

```bash
oc get pod -n hashicorp-vault
```

The next step is to initialize and unseal Vault. This is already done by utilizing the `vault-install` Helm chart, which includes a bootstrap script that automates the process. 
It is important to note that this method should not be used in a production environment, and a proper unsealing mechanism, such as one provided by Vault, should be employed instead.


At this point we are ready to install the Vault Config Operator, please configure the ArgoCD application:


TOOD: Automate with server side apply and job/workflow
```bash

oc create configmap int-ca --from-file=ca-chain/intermediate/ca.crt -n vault-config-operator

cat <<EOF > ./policy.hcl
path "/*" {
    capabilities = ["create", "read", "update", "delete", "list","sudo"]
}
EOF

vault policy write -tls-skip-verify vault-admin ./policy.hcl

oc apply -f argocd/vault-config-operator.yaml

JWT=$(oc sa get-token controller-manager)
KUBERNETES_HOST=https://kubernetes.default.svc:443

oc extract configmap/kube-root-ca.crt -n vault-config-operator

vault write -tls-skip-verify auth/kubernetes/config token_reviewer_jwt=$JWT kubernetes_host=$KUBERNETES_HOST kubernetes_ca_cert=@./ca.crt
vault write -tls-skip-verify auth/kubernetes/role/vault-admin bound_service_account_names=controller-manager bound_service_account_namespaces=vault-config-operator policies=vault-admin ttl=1h

```
