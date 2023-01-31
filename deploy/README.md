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

### Vault

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

### Vault Config Operator

At this point we are ready to install the Vault Config Operator, please configure the ArgoCD application:

```bash
oc apply -f argocd/vault-config-operator.yaml
```

### PKI Engine
It is time to create the CA chain hierarchy with an offline root CA and online intermediate CAs in Vault for each application namespace.

Having a dedicated intermediate CA per organization or team can increase security and gain greater control over the chain of trust in your ecosystem, allowing you to trust only certificates issued by your trust model.


```bash
oc apply -f argocd/vault-pki-engine.yaml
```

Signing the vault intermediate certificate.

```bash
oc extract secret/intermediate --keys=csr -n vault-config-operator
openssl ca -config ca-chain/root/openssl.cnf -extensions v3_intermediate_ca -days 365 -notext -md sha256 -in csr -out tls.crt
oc create secret generic signed-intermediate --from-file=tls.crt  -n vault-config-operator

cat <<EOF > patch-pki.yaml
spec:
  externalSignSecret:
    name: signed-intermediate
EOF

oc patch pkisecretengineconfig intermediate --type=merge --patch-file patch-pki.yaml -n vault-config-operator
```

At this point, it is time to configure the PKI for the application namespace, for this example we configure the team-one namespace.


```bash
oc apply -f argocd/vault-app-pki-engine.yaml
```

### Application

TODO

