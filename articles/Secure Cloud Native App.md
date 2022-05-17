# Secure Cloud-Native Applications With HashiCorp Vault and Cert Manager

When companies talk about security, they are referring to preventing data loss and securely automating and integrating applications.

That cannot be done without knowing who is doing what to which assets, and that is where identity management, like HashiCorp Vault, comes in. The “who” in the equation becomes very important.

Properly issued certificates enable end-to-end security through a trusted chain of identities.

As with most security objectives, there is usually tension between the requirement to make things secure and trying to get the actual work done. 
The art here is to balance the two conflicting requirements, one way to reduce the burden on the developer is to automate as much as possible.

In this blog, we will illustrate how OpenShift together with Cert Manager and HashiCorp Vault can be used to achieve an automated and reproducible process to increase the security of applications.

From the developer's point of view, this automated approach is easy to use and is also instrumented so that we know what is going on and can take appropriate action if it fails.


## Certificate Authority

The purpose of a Certificate authority (CA) is to validate and issue certificates. A Certificate Authority may be a third-party entity or organization that runs its own provider to issue digital certificates.

An intermediate certificate authority is a CA signed by a superior CA (for example, a root CA or another Intermediate CA) and signs CAs (for example, another intermediate or subordinate CA).

If an Intermediate CA exists, it is positioned within the middle of a trust chain between the trust anchor, or root, and the subscriber certificate that is issuing subordinate CAs. So not use a root CA directly?

Typically, the root CA does not sign server or client certificates directly. The root CA is used only to create one or more intermediate CAs. Using an intermediate CA is primarily for security purposes and the root CA is hosted elsewhere in a secure place; offline, and used as infrequently as possible.

So, it is better to not expose it within target environments and to instead issue a shorter-lived intermediate CA. Using intermediate CA also aligns with industry best practices.


## CA Hierarchy

In large organizations, it may be ideal to delegate responsibility for issuing certificates to different certificate authorities for granular security controls appropriate to each CA.

For example, the number of certificates may be too large for a single CA to effectively track the certificates it has issued; or each departmental unit may have different policies and rules, such as validity periods; or it may be important to differentiate certificates for internal or external communication.

The X.509 standard includes a template for setting up a hierarchy of CAs:

![](https://lh3.googleusercontent.com/iEBvVytjDqt-tXnFBZ65ZNHsq6OZDdBvMYKsm2LLg1spEtng3TK1OPvTdd7JaIvABagB_ZKZca6kVxRUlP14wSWTOX-aSgg3WzWHEp8y0jhZiqkYEaMu8SXlAq986n1N1wHcnzYTLduqKPzRZw)

# Installation

## Cert Manager

[Cert Manager](https://cert-manager.io/) is a tool for Kubernetes and OpenShift that automates certificate management in cloud-native environments.

It builds on top of these platforms to provide X.509 certificates and issuers as first-class resource types.

It provides easy-to-use tools to manage certificates including a “certificates as a service” for securely enabling developers and applications working within a cluster and a standardized API for interacting with multiple certificate authorities (CAs). This gives security teams the confidence to allow developers to manage certificates in a self-service fashion.

Various integrations are available including ACME (Let’s Encrypt), HashiCorp Vault, Venafi, and self-signed and internal certificate authorities. In addition, extension points can be added to support custom, internal or otherwise unsupported CAs.

To install Cert Manager Operator within an OpenShift Container Platform environment, log into the web console as a user with the cluster-admin role. As of this writing, the Cert Manager Operator requires version 4.10 or higher.

1. Click Operators → OperatorHub.

2. Type the name of the Operator into the filter box and select “cert-manager Operator for Red Hat OpenShift”.

![](https://lh3.googleusercontent.com/eBo_v8WgixmZYVT0drwkzRBclN6-gZI84Qg5j3e_aJv1HRK2I0HnPr_QTs6sdYznc9Ej6_Szr6pSqNnPO2eL4_BPtAMi0_Imw7bEqTUTXwIXIqZ5gO2NVmYOugl7TqKklwc__4geA7XhWz6OXA)

3. Click Install.

![](https://lh5.googleusercontent.com/sehePLADuhc5-bQ9JF75beYQwJDvza--rptblM6lMLmousnh1CpIQQBAEKj64oymGbc7HZh_nIodH9QH1d5DSecp4_NKm7-C5cnQ4RWzP2f-pcf-OBLiPVrCpvK9OVLHrHBTv2O9L0AqBaqcng)

4. On the Install Operator page, select installation options.

    4.1 in the Update Channel section, select **tech-preview**.

    4.2. The Cert Manager operator is installed in the **openshift-cert-manager-operator** namespace.

    4.3. Click **Install** and wait until the Operator is installed.

![](https://lh5.googleusercontent.com/xkL0evIv_nJmeIONqB2zrXDL47AoQTtoUKMq_iHImweNjna8eUQCXzLfKx2nBGGQHYpzEjujwIEbo7A4Fctx5byv_tlDj_NtWR1uH4wW5G-U81-haZTVtAgs2194CyRaFY9oBL9jVssk9P2BQQ)

5. Click Operators → Installed Operators to verify that the Operator installed successfully.

![](https://lh3.googleusercontent.com/_XALPk-1fgzbDJkUAp-3W3_brDsCExKaE-CEVSLuY8c3u6aA3jqfhJg4DxIfwLEvTwhkIup7vA4aOfXFZdqu5LhV66XNDH12FACkKEwklgLPiYUWGmv4_Ym3pGiDMv_vfjfW2pvdi0HVavvagQ)


## Create the CA Chain

Let’s start from scratch and simulate the creation of our own certificate authority and building the CA hierarchy.

We are going to create the root CA certificate-key pair using ​​OpenSSL program.

![](https://lh6.googleusercontent.com/Kcs47lFD9_HIfLUUfenzZao1jiElPv9T6HgFDvBVhkezWGd0p5l4_8SU62iVDkxdCcCSXLRmX3cbMagfH2Yw5qFHqSJKHUANuQHMGHG5BdCchRcBm_mlnfWFbkFQQEctPeFSn7BEVYYu8Bpm6A)

First, navigate to a directory for which the certificates will be created.

```bash
export CERT_ROOT=$(pwd)
```

Define the directory structure:

```bash
mkdir -p ${CERT_ROOT}/{root,intermediate}
```

Generate the CA Private Key:

```bash
cd ${CERT_ROOT}/root/

openssl genrsa -out ca.key 2048

touch index.txt
echo 1000 > serial
mkdir -p newcerts
```

Define the openssl.cnf file:

```bash
cat <<EOF > openssl.cnf
[ ca ]
default_ca = CA_default

[ CA_default ]
# Directory and file locations.
dir               = ${CERT_ROOT}/root
certs             = \$dir/certs
crl_dir           = \$dir/crl
new_certs_dir     = \$dir/newcerts
database          = \$dir/index.txt
serial            = \$dir/serial
RANDFILE          = \$dir/private/.rand

# The root key and root certificate.
private_key       = \$dir/ca.key
certificate       = \$dir/ca.crt

# For certificate revocation lists.
crlnumber         = \$dir/crlnumber
crl               = \$dir/crl/ca.crl
crl_extensions    = crl_ext
default_crl_days  = 30

# SHA-1 is deprecated, so use SHA-2 instead.
default_md        = sha256

name_opt          = ca_default
cert_opt          = ca_default
default_days      = 375
preserve          = no

policy            = policy_strict

[ policy_strict ]
# The root CA should only sign intermediate certificates that match.
countryName               = match
stateOrProvinceName       = optional
organizationName          = optional
organizationalUnitName    = optional
commonName                = supplied
emailAddress              = optional

[ v3_intermediate_ca ]
# Extensions for a typical intermediate CA.
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true, pathlen:1
keyUsage = critical, digitalSignature, cRLSign, keyCertSign

[req_distinguished_name]
countryName = CH
countryName = Country Name
countryName_default = CH
stateOrProvinceName = State or Province Name
stateOrProvinceName_default = ZH
localityName= Locality Name
localityName_default = Zurich
organizationName= Organization Name
organizationName_default = Red Hat
commonName= Company Name
commonName_default = company.io
commonName_max = 64

[req]
distinguished_name = req_distinguished_name
[ v3_ca ]
basicConstraints = critical,CA:TRUE
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer:always
EOF
```

Generate the certificate:

```bash
openssl req -x509 -new -nodes -key ca.key -sha256 -days 1024 -out ca.crt -extensions v3_ca -config openssl.cnf
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----

Country Name [CH]:

State or Province Name [ZH]:

Locality Name [Zurich]:

Organization Name [Red Hat]:

Company Name [company.io]:
```

As shown in the output above, the value defined in the_openssl.cnf_ configuration file includes a _req_distinguished_name_ entry which is used as the default set of values when generating the certificate. The default values can be used or a user defined set of values can be provided .

Now with the root CA, we can start with the second step of the chain: the intermediate CA.

Generate the Intermediate CA Private Key:

```bash
cd ../intermediate

openssl genrsa -out ca.key 2048
```

Generate the Certificate Siging Request:

```bash
openssl req -new -sha256 -key ca.key -out ca.csr

You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) []:CH
State or Province Name (full name) []:ZH
Locality Name (eg, city) []:Zurich
Organization Name (eg, company) []:Red Hat
Organizational Unit Name (eg, section) []:RH
Common Name (eg, fully qualified host name) []:int.company.io
Email Address []:
Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
```

Make sure the **Country Name** and the **Common Name** are defined, because the policy ( _policy_strict_ ) entry on the _openssl.cnf_ requires a matching_countryName_ and a defined _commonName_.

Create the intermediate certificate:

```bash
openssl ca -config ../root/openssl.cnf -extensions v3_intermediate_ca -days 365 -notext -md sha256 -in ca.csr -out ca.crt

...

Certificate is to be certified until May 12 12:52:52 2023 GMT (365 days)

Sign the certificate? [y/n]:y
1 out of 1 certificate requests certified, commit? [y/n]y

Write out database with 1 new entries
Data Base Updated
```

Our intermediate CA is now ready to be used.

## Issuer with Cert Manager

The first thing you will need to configure after you have installed cert-manager is for an Issuer to be created, which can be then used to issue certificates.

Issuers are Kubernetes’ resources that represent Certificate Authorities (CAs) that are able to generate signed certificates by honoring Certificate Signing Requests.

The simplest issuer type is the CA, which references the Kubernetes TLS Secret containing a ca-key pair.


## Generate SSL Certificates for Vault using Cert Manager

Before Vault can be installed, certificates must be provisioned within a newly created namespace.

First, define the namespace where we want to install Vault:

```bash
oc new-project hashicorp
```
From the intermediate directory, let’s create the Kubernetes Secret containing the certificate previously generated

```bash
oc create secret tls intermediate --cert=${CERT_ROOT}/intermediate/ca.crt --key=${CERT_ROOT}/intermediate/ca.key -n hashicorp
```

After the secret is created, we can apply a cert-manager Issuer CR of type CA to the cluster:

```yaml
cat <<EOF | oc apply -f -
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: int-ca-issuer
spec:
  ca:
    secretName: intermediate
EOF
```

Let’s check the Issuer status to confirm it was successfully created:

```bash
oc get issuer int-ca-issuer

NAME          READY     AGE

int-ca-issuer True      5s
```
>
>**_NOTE:_**_This specific Cert-Manager Issuer containing the Intermediate certificate authority is strictly used to sign the certificate of Vault only. No other applications will request certificates._
>
>

It is time to create a Certificate CR for HashiCorp Vault. First, let’s define some convenient variables:

```bash
export BASE_DOMAIN=$(oc get dns cluster -o jsonpath='{.spec.baseDomain}')
export VAULT_HELM_RELEASE=vault
export VAULT_ROUTE=${VAULT_HELM_RELEASE}.apps.$BASE_DOMAIN
export VAULT_ADDR=https://${VAULT_ROUTE}
export VAULT_SERVICE=${VAULT_HELM_RELEASE}-active.hashicorp.svc
```

Deploy the certificate:

```yaml
cat <<EOF|oc apply -f -
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: vault-certs
spec:
  secretName: vault-certs
  issuerRef:
    name: int-ca-issuer
    kind: Issuer
  dnsNames: 
  - ${VAULT_ROUTE}
  # Service Active FQDN
  - ${VAULT_SERVICE}
  organization:
  - company.io
EOF
```

>
>**_NOTE_**_: The generated certificate must be valid for both the Vault active service and the Vault Route._
>
>

Once cert-manager detects the creation of the _Certificate_ CR, an SSL certificate will be generated on behalf of the internal intermediate certificate authority and save it as a Kubernetes TLS secret.

```bash
oc get secret vault-certs

NAME        TYPE              DATA  AGE

vault-certs kubernetes.io/tls 3     12s
```

**_NOTE_**_: This certificate will be used by the HashiCorp pods for securing the Vault API port 8200. When the Vault nodes will join the cluster, they will need to make an API request to the Vault active node (through the vault-active service). However, later on, when the Vault cluster is set up, the nodes will communicate through Cluster port 8201, which is secured by a certificate generated by the Vault active node internally._


# HashiCorp Vault

[HashiCorp Vault](https://www.vaultproject.io/docs/what-is-vault) is an identity-based secret and encryption management system.

A secret is anything you want to tightly control access to, such as API encryption keys, passwords, or certificates.

Vault provides encryption services that are gated by authentication and authorization methods. Using Vault’s UI, CLI, or HTTP API, access to secrets and other sensitive data can be securely stored and managed, tightly controlled (restricted), and auditable.

With the certificate now in pace, it is time to deploy and configure Vault.

The official way to install Hashicorp Vault is to use the [Vault Helm Chart](https://github.com/hashicorp/vault-helm).

In production environments, it is deployed in a high-availability manner. HashiCorp Vault needs an underlying storage backend to store the data, and it can rely on its [integrated storage](https://www.vaultproject.io/docs/concepts/integrated-storage), which uses the [RAFT](https://raft.github.io/) consensus algorithm.

When deploying to OpenShift, the integrated storage is a convenient solution for storing data, as this removes the burden of managing an additional storage component for storing Vault’s data.

1. Create a working directory for the Vault installation.

```bash
mkdir -p vault
cd vault/
```

2. Configure Helm Repository:

```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
helm repo update
```

3. Define the Helmvalues.yaml that will configure a Highly Available Vault environment with Raft storage:

```yaml
cat <<EOF > values.yaml
global:
  tlsDisable: false
  openshift: true
injector:
  image:
    repository: "registry.connect.redhat.com/hashicorp/vault-k8s"
    tag: "0.14.2-ubi"
  agentImage:
    repository: "registry.connect.redhat.com/hashicorp/vault"
    tag: "1.9.6-ubi"
ui:
  enabled: true
server:
  image:
    repository: "registry.connect.redhat.com/hashicorp/vault"
    tag: "1.9.6-ubi"
  route:
    enabled: true
    host:
  extraEnvironmentVars:
    VAULT_CACERT: "/etc/vault-tls/vault-certs/ca.crt"
    VAULT_TLS_SERVER_NAME:
  standalone:
    enabled: false
  auditStorage:
    enabled: true
    size: 15Gi
  extraVolumes:
    - type: "secret"
      name: "vault-certs"
      path: "/etc/vault-tls"
  ha:
    enabled: true
    raft:
      enabled: true
      setNodeId: true
      config: |
        ui = true
        listener "tcp" {
          address = "[::]:8200"
          cluster_address = "[::]:8201"
          tls_cert_file = "/etc/vault-tls/vault-certs/tls.crt"
          tls_key_file = "/etc/vault-tls/vault-certs/tls.key"
          tls_client_ca_file = "/etc/vault-tls/vault-certs/ca.crt"
        }
        storage "raft" {
          path = "/vault/data"
          retry_join {
            leader_api_addr = "https://vault-active.hashicorp.svc:8200"
            leader_ca_cert_file = "/etc/vault-tls/vault-certs/ca.crt"
          }
        }
        log_level = "debug"
        service_registration "kubernetes" {}
  service:
    enabled: true
EOF
```

3. Install the Vault Helm chart using the previously configured values:

```bash
helm install vault hashicorp/vault -f values.yaml \
    --set server.route.host=$VAULT_ROUTE \
    --set server.extraEnvironmentVars.VAULT_TLS_SERVER_NAME=$VAULT_ROUTE \
    --wait \
    -n hashicorp
```

4. Initialize Vault and save the generated key and token:

```bash
oc -n hashicorp exec -ti vault-0 -- vault operator init -key-threshold=1 -key-shares=1

Unseal Key 1: 7tbxdHjNqLsCAS16b0ac92jb+uvXEVSPwFZyf2Ln8Gk=

Initial Root Token: s.lSHpKvhYhjy5xwR0wtkXEk6H
```

5. Unseal all the Vault instances because they starts in a[sealed](https://www.vaultproject.io/docs/concepts/seal) state:

```bash
oc -n hashicorp exec -ti vault-0 -- vault operator unseal

Unseal Key (will be hidden):

oc -n hashicorp exec -ti vault-1 -- vault operator unseal

Unseal Key (will be hidden):

oc -n hashicorp exec -ti vault-2 -- vault operator unseal

Unseal Key (will be hidden):
```

6. Now that the vault has been sealed, check the Raft storage has one leader and two followers:

```bash
oc -n hashicorp rsh vault-0

vault login

Token (will be hidden):

vault operator raft list-peers

vault operator raft list-peers
Node       Address                        State       Voter
----       -------                        -----       -----
vault-0    vault-0.vault-internal:8201    leader      true
vault-1    vault-1.vault-internal:8201    follower    true
vault-2    vault-2.vault-internal:8201    follower    true
```

7. Verify the access from Vault UI. On OpenShift Console Click _Networking → Routes_ and click the _vault_ route.

![](https://lh6.googleusercontent.com/oAZAwlRp_YE699L5gG017ymIIJUUDeihp0XBIzm2pjNiXN8Q4ffeM-crdAz16tbE-BUwzyO53SaYQ6pUIesf5FvArfK1EOTE2u1avC7b_xKRMB8kbvk5Ar9hGDLcv8zFsGVMlTA_QAi5n1nezA)

8. To authenticate use the root token generated before by the initialize command.


# Integrate Vault with OpenShift

At this point, Vault is ready to be integrated into the OpenShift platform.

## Kubernetes Auth Method

The [Kubernetes authentication method](https://www.vaultproject.io/docs/auth/kubernetes) can be used to authenticate with Vault using a Kubernetes service account token. The token for a pod’s service account is automatically mounted within a pod at _/var/run/secrets/kubernetes.io/serviceaccount/token_ and is sent to Vault for authentication.

Vault, like all pods in Kubernetes, is configured with a service account that has permissions to access the [TokenReview](https://docs.openshift.org/latest/rest_api/apis-authentication.k8s.io/v1.TokenReview.html) API. This service account can then be used to make authenticated calls to Kubernetes to verify tokens of the service accounts of pods that want to connect to Vault to get secrets.


## Configuring Vault for OpenShift Operator-based

To support a variety of Vault configuration workflows and conventions, we will leverage the [Vault config operator](https://github.com/redhat-cop/vault-config-operator) to automate the setup and configuration.

The advantage of a Vault Config Operator is that we can now configure Vault by creating a set of custom resources (CRs), which in turn can be packaged as Helm charts or kustomize manifests and managed in a GitOps fashion. So, configuring Vault is no longer an imperative action, but is now declarative. Read more about the background and capabilities of the Vault Config Operator on [this blog](https://cloud.redhat.com/blog/configuring-vault-for-kubernetes-an-operator-based-approach).


## Vault Config Operator

To install the Vault Config Operator, return to OpenShift Container Platform web console as a user with the cluster-admin role.

1. Click Operators → OperatorHub.

2. Type the name of the Operator into the filter box and select Vault Config Operator:

![](https://lh5.googleusercontent.com/6tKD_SOl2K_-rvRv-IfV8IuFW9nm7xJVAjqQvsGuKNr4lBEsbBoFDwLQ7a0HJeyrlSbmUhKfjd6phAoQBDys5WCPFbIqE__KrrpMfKTnS7fjn95FDuyGb7q_EaNIC9Ksv6aROYa8zKaM0DXFRg)

3. Click Install.

![](https://lh6.googleusercontent.com/_VS2GJa6LcnKvMckERFoRPN1bH2cahLFueI2-3DMMKI91PUtaoIl-FHXWxANkp100HdMLoYHOZJ3WJz-P1Kf5d2T6V2kvHDSYUbFwg-bMI5w85Qdg8Vs2Q1yyUKascTidZ0Hvh6TT8UwfgFfNg)

4. On the Install Operator page, select installation options.

    4.1 in the Update Channel section, select **alpha**.

    4.2. The Vault config operator is installed in the **vault-config-operator** namespace.

    4.3. Click Install. Wait until the Operator is installed.

![](https://lh6.googleusercontent.com/vFYGYLvPD3pFxOZ9FYd3QfKZwvUXAlcCcBLUnOQuEew0U8Abo-mlAfus5i_5W0uzioeP4jY5VTLFN1JWtZRvD4HhhXbW1CPwF2XA00iNEOqosJ7eFUT1A1MdRcKFrYrZPLcYCTlcdA815w_7Og)

5. Click Operators → Installed Operators to verify that the Operator installed successfully.

![](https://lh3.googleusercontent.com/ktXTiMXVwUeEeRco3uX7OK41xO1HqtLNkv57IS4DGuZh6d1zYSnuy7ueHWd6F-eD-BiAhyyROYOTBmXrsw2PKRqZ_BQesDidD0aigVZxMcrjENqZo7a2EVSeOe7hwTazelkSw2fiuEV1Eb27eQ)


### Connect to Vault.

The connection to Vault can be initialized with [Vault's standard environment variables](https://www.vaultproject.io/docs/commands#environment-variables) that are applied to the Vault Config Operator pod. See the [OLM documentation](https://github.com/operator-framework/operator-lifecycle-manager/blob/master/doc/design/subscription-config.md#env) on how to pass environment variables via a Subscription. The variables that are read at client initialization are listed [ here](https://github.com/hashicorp/vault/blob/14101f866414d2ed7850648b465c746ac8fda621/api/client.go#L35).

For the operator to trust the certificates presented by Vault, the recommended approach is to mount a Secret or ConfigMap containing the certificate as described [here](https://github.com/operator-framework/operator-lifecycle-manager/blob/master/doc/design/subscription-config.md#volumes), and then configure the corresponding variables to reference the file locations in the path mounted into the container.

The configurations that are applied against Vault by the operator need to authenticate via a Kubernetes Authentication. To facilitate authenticating with Vault by the Operator, a root Kubernetes authentication mount point and role are required.

1. Download and install the Vault client on your local computer by following the instructions [here](https://www.vaultproject.io/downloads).

2. Login to Vault using the root password that was displayed earlier when initializing Vault:

```bash
vault login -tls-skip-verify

Token (will be hidden):

Success! You are now authenticated.
```

3. Create an admin [policy](https://www.vaultproject.io/docs/concepts/policies):

```bash
cat <<EOF > ./policy.hcl
path "/*" {
  capabilities = ["create", "read", "update", "delete", "list","sudo"]
}
EOF

vault policy -tls-skip-verify write vault-admin ./policy.hcl
```

![](https://lh4.googleusercontent.com/mhKn6pXHkMKN5xJfLA2x_6K47MXwyhSUtqnhLkzCvX-CCQPwVUENphdZTftM5fTi_ggxAPif1SRkJt0sc3EDBSnqJvmUJRywPdNz44h3nXthj_ofFhxaZwAWTs56IvS3X0rBC_lj1Dj33-TvdQ)

>
>**_NOTE:_**_ this policy is intentionally broad to allow testing anything in Vault. In a real life scenario this policy would be scoped down._
>

4. Switch to the Vault Config Operator project which was automatically created when the operator was installed:

```bash
oc project vault-config-operator
```

5. Enable the Kubernetes auth method by first obtaining details related to the Kubernetes API including the certificate:

```bash
JWT_SECRET=$(oc get sa controller-manager -o jsonpath='{.secrets}' | jq '.\[] | select(.name|test("token-")).name')
JWT=$(oc sa get-token controller-manager)
KUBERNETES_HOST=https://kubernetes.default.svc:443

oc extract configmap/kube-root-ca.crt -n vault-config-operator

vault auth enable -tls-skip-verify kubernetes

vault write -tls-skip-verify auth/kubernetes/config token_reviewer_jwt=$JWT kubernetes_host=$KUBERNETES_HOST kubernetes_ca_cert=@./ca.crt
```

6. Create a Role and assign it to the policy previously creted

```bash
vault write -tls-skip-verify auth/kubernetes/role/vault-admin bound_service_account_names=controller-manager bound_service_account_namespaces=vault-config-operator policies=vault-admin ttl=1h
```

Verify the recently created _kubernetes_ auth method from _Vault UI → Access → AuthMethods_.

![](https://lh3.googleusercontent.com/wa4CBMMA5-jbmOIP-KmQ6TU8nFx-Av8t0yk6O3WyYbQ1_R4-jHgm4H4idDe3RHtZxolNKGtOpNiO3Z7MmmdsAEW-_Bqy9h0AmllBO4e6IEOjRd4TRgXnYFDdPxXLMQ2zph0iEhuK9ypdFqK61Q)

7. Create a configmap that contains our intermediate CA:

```bash
oc create configmap int-ca --from-file=${CERT_ROOT}/intermediate/ca.crt -n vault-config-operator
```

8. Patch the subscription to include the connection to our Vault instance:

```yaml
cat <<EOF > patch.yaml
spec:
  config:
    env:
    - name: VAULT_ADDR
      value: https://vault-active.hashicorp.svc:8200
    - name: VAULT_CACERT
      value: /vault-ca/ca.crt
    - name: VAULT_TOKEN
      valueFrom:
        secretKeyRef:
          name: $JWT_SECRET
          key: token
    volumes:
    - name: vault-ca
      configMap:
        name: int-ca
    volumeMounts:
    - mountPath: /vault-ca
      name: vault-ca
EOF

oc patch subscription vault-config-operator --type=merge --patch-file patch.yaml -n vault-config-operator
```

The patch of the subscription updates the Deployment of the vault-config-operator with the new configuration as show in the picture below.

![](https://lh4.googleusercontent.com/6USpuwqeu6r_1DgT8MuiS6b-OyQAOuIZSmDibucofdb1ATA64qTZfmextNzGT1mTw6665IUk9vuUz1Yfd4N7HJlOHs4ssOf3dErSctaZVRoT4BygJmT4r1Fh77r4at8jOWp15GxDu-1UCLtg9Q)

9. Add the token review role for the controller-manager Service Account for which the Vault Config Operator use:

```bash
oc adm policy add-cluster-role-to-user system:auth-delegator -z controller-manager
```

# Vault PKI Secrets Engine.

The [Vault PKI secrets engine](https://www.vaultproject.io/api-docs/secret/pki) generates dynamic X.509 certificates, without requiring all of the manual actions.

Vault's built-in authentication and authorization mechanisms provide the necessary verification functionality.

![](https://lh5.googleusercontent.com/8b6q4kVEYKQ9QMa36FOEaX7pqavmLmjUKIYnV9XcmN726YyoZpR_5kZV6FkSuL-o1YKOMVKDz0PwPbbCf_50sL-Q8wA7vNhUG3vejhldl1EK6U1c6u4PIU8nhi-wPg4HJcK3qjTRpSYQyuS4Ww)

As seen in the diagram above, there are several steps to enable Vault to be a certificate manager in OpenShift. These steps are detailed below:

Vault:

1. Enable the Kubernetes Auth Engine in Vault.

2. Authorize the Vault SA in k8s to Token Review.

3. Enable the PKI secrets engine.

4. Configure the PKI role in Vault.

5. Configure the PKI policy in Vault.

6. Authorize/Bind issuer SA to use (policy) the PKI role.

Cert-Manager:

1. Create the Vault Issuer in-app namespace with issuer SA.

2. cert-manager validates the credentials of the issuer against Vault.

Thanks to the Vault Config Operator and cert-manager, define the relative customer resource and let them do the job for you.

It is time to create the CA chain hierarchy with an offline root CA and online intermediate CAs in Vault for each application namespace.

Having a dedicated intermediate CA per organization or team can increase security and gain greater control over the chain of trust in your ecosystem, allowing you to trust only certificates issued by your trust model.

![](https://lh4.googleusercontent.com/kM3gYOHrgcBCZWcSwaD7WX3E4GwyJ_m_qBeZZ_HPk8vyFAgu1hOYe19DUEjyBrK0VVoDBJ7veA2UkTRBj2lfsEi3eqiPfVhm8sc77IIh3vl7eZrVnNSjjTb4Fb7g7NoVrZJqYVCroxbyXtOz2Q)

  
1. Create an Intermediate _SecretEngineMount_:

```yaml

cat <<EOF|oc apply -f -
apiVersion: redhatcop.redhat.io/v1alpha1
kind: SecretEngineMount
metadata:
  name: intermediate
spec:
  authentication:
    path: kubernetes
    role: vault-admin
    serviceAccount:
      name: controller-manager
  type: pki
  path: pki
  config:
    # 1 Year
    maxLeaseTTL: "8760h"
EOF
```

Verify from _Vault UI → Secrets_ the PKI Secret Engine created.

![](https://lh6.googleusercontent.com/N36etMmKFA9-jaAGB1GcYgIYbOa_pJbCibH7pTbfP7_1ltDRZEgj76DmijcmJszlCC5N03ThDp4Yqr2ktgvPCUxKKXPOBqR6mUBmXUDGuw8Mi6owRXxFJfJtvM31oertZvzgfH4oWVNF9VnYNg)

2. Configure an intermediate _PKISecretEngineConfig_:

```yaml
cat <<EOF|oc apply -f -
apiVersion: redhatcop.redhat.io/v1alpha1
kind: PKISecretEngineConfig
metadata:
  name: intermediate
spec:
  authentication:
    path: kubernetes
    role: vault-admin
    serviceAccount:
      name: controller-manager
  path: pki/intermediate
  commonName: vault.int.company.io
  TTL: "8760h"
  type: intermediate
  privateKeyType: exported
  country: CH
  province: ZH
  locality: Zurich
  organization: Red Hat
  maxPathLength: 1
  issuingCertificates:
  - https://${VAULT_ROUTE}/v1/pki/intermediate/ca
  crlDistributionPoints:
  - https://${VAULT_ROUTE}/v1/pki/intermediate/crl"
EOF
```
>
>**_Note:_** _PKISecretEngineConfig stays in error status until the signed certificate has been provided._
>

```bash
Waiting spec.externalSignSecret with signed intermediate certificate.
```

3. Sign the CSR with the company root CA:

```bash
oc extract secret/intermediate --keys=csr

openssl ca -config ${CERT_ROOT}/root/openssl.cnf -extensions v3_intermediate_ca -days 365 -notext -md sha256 -in csr -out tls.crt
```

4. Create the secret with the signed intermediate certificate:

```bash
oc create secret generic signed-intermediate --from-file=tls.crt
```

5. Patch the _PKISecretEngineConfig_ with the new signed-intermediate secret.

```yaml
cat <<EOF > patch-pki.yaml
spec:
  externalSignSecret:
    name: signed-intermediate
EOF

oc patch pkisecretengineconfig intermediate --type=merge --patch-file patch-pki.yaml -n vault-config-operator
```

Verify from _Vault UI → Secret → pki/intermediate_ the signed certificate.

![](https://lh6.googleusercontent.com/Ye3mwRK3pIkQ9nRtTj3mvlq5UzMSucwpw0TcQIoAa7uTYossfhOrYHRQxVK3K_RqHLshFKhvQrP4udOnlsqLn9Zary85_2eStvXYLRvF3d5otvPESKqE10GQf_flQrjhJNJ-BdVBUjgm-ENAgA)

At this point, it is time to configure the PKI for the application namespace, for this example we configure the _team-one_ namespace.

We walk through each step in the process; nevertheless, these steps can be automated in the future by leveraging the [Namespace Configuration Operator](https://github.com/redhat-cop/namespace-configuration-operator).

1. Create _AuthEngineMount_ to define an [authentication engine endpoint](https://www.vaultproject.io/docs/auth):

```yaml
cat <<EOF|oc apply -f -
apiVersion: redhatcop.redhat.io/v1alpha1
kind: AuthEngineMount
metadata:
  name: team-one
spec:
  authentication:
    path: kubernetes
    role: vault-admin
    serviceAccount:
      name: controller-manager
  type: kubernetes
  path: app-kubernetes
EOF
```

Verify from _Vault UI → Access → AuthMethods_.

![](https://lh4.googleusercontent.com/xzVQ-e6tJxp6Rqydav6gknPpTltcgELPfnlE1hsgK7mwpa-M7bVduQcYlmMRpNomePr_KUOJsGQqP2aC7sAkma91UTLso6vD77NHXUafdHIj3YwJV5I9oAXI_zk7INzVMNIBbpuAQ2_qEQJR0g)


2. Create _KubernentesAuthEngineConfig_ to configure the auth engine mount to point to a specific Kubernetes master API endpoint:

```yaml
cat <<EOF|oc apply -f -
apiVersion: redhatcop.redhat.io/v1alpha1
kind: KubernetesAuthEngineConfig
metadata:
  name: team-one
spec:
  authentication:
    path: kubernetes
    role: vault-admin
    serviceAccount:
      name: controller-manager
  tokenReviewerServiceAccount:
    name: controller-manager
  path: app-kubernetes
EOF
```

3. Create the _KubernetesAuthEngineRole_, which configures all of the default service accounts in the application namespace:

```yaml
cat <<EOF|oc apply -f -
apiVersion: redhatcop.redhat.io/v1alpha1
kind: KubernetesAuthEngineRole
metadata:
  name: team-one
spec:
  authentication:
    path: kubernetes
    role: vault-admin
    serviceAccount:
      name: controller-manager
  path: app-kubernetes/team-one
  policies:
  - team-one-pki-engine
  targetServiceAccounts:
  - default
  targetNamespaces:
    targetNamespaces:
    - team-one
EOF
```

Verify from _Vault UI → Access → app-kubernetes/team-one → Roles_.

![](https://lh3.googleusercontent.com/o0Ttf_5xBo4QwobLUfx1e4SUltOsQAdIWYRiSXqsfYKeZ8kWEaJG8PZDKg6_TTpC5yA7WPOPQHj9Ae9nlFsJsZzR7EEjC_fDzuOaILQ_tjF0t5bU8qoqFApO811aCZ0Ni6M8xnULzTOkPITYbw)

4. Define the policy to give the right access to the PKI engine:

```yaml
cat <<EOF|oc apply -f -
apiVersion: redhatcop.redhat.io/v1alpha1
kind: Policy
metadata:
  name: team-one-pki-engine
spec:
  authentication:
    path: kubernetes
    role: vault-admin
    serviceAccount:
      name: controller-manager
  policy: |
    # query existing mounts
    path "/sys/mounts" {
      capabilities = [ "list", "read"]
      allowed_parameters = {
        "type" = ["pki"]
        "*"   = []
      }
    }

    # mount pki secret engines
    path "/sys/mounts/app-pki/team-one*" {
      capabilities = ["create", "read", "update", "delete", "list"]
    }

    # tune
    path "/sys/mounts/app-pki/team-one/tune" {
      capabilities = ["create", "read", "update", "delete", "list"]
    }

    # internal sign pki
    path "pki/intermediate/root/sign-intermediate" {
      capabilities = ["create"]
    }

    # pki 
    path "app-pki/team-one*" {
      capabilities = ["create", "read", "update", "delete", "list"]
    }
EOF
```

5. Now, create the _team-one_ application namespace that can leverage the resources that we previously configured:

```bash
oc new-project team-one
```

6. Create the _SecretEngineMount_ of type PKI in the application namespace _team-one_:

```yaml
cat <<EOF|oc apply -f -
apiVersion: redhatcop.redhat.io/v1alpha1
kind: SecretEngineMount
metadata:
  name: team-one
spec:
  authentication:
    path: app-kubernetes/team-one
    role: team-one
  type: pki
  path: app-pki
  config:
    # 1 Year
    maxLeaseTTL: "8760h"
EOF
```

Verify from _Vault UI → Secrets_

![](https://lh4.googleusercontent.com/yHsrNJNkGeStpqFOkMSDiLjxqhieXTos3_49fcyR2HUwNlwqNFmrZmh-j4CsCSwhfMwCYQuJUKM80Q0XMcieE450rd6Pb9CrVwuASVdxaBtwsPx1ld25SCXAcK0H2kDfjQw3BJ3cjOX7CwCjJg)


7. Generate the intermediate certificate signed by the internal Vault _pki/intermediate_ CA:

```yaml
cat <<EOF|oc apply -f
apiVersion: redhatcop.redhat.io/v1alpha1
kind: PKISecretEngineConfig
metadata:
  name: team-one
spec:
  authentication:
    path: app-kubernetes/team-one
    role: team-one
  path: app-pki/team-one
  commonName: team-one.vault.int.company.io
  TTL: "8760h"
  type: intermediate
  privateKeyType: exported
  internalSign:
    name: pki/intermediate
  issuingCertificates:
  - https://${VAULT_ROUTE}/v1/app-pki/team-one/ca
  crlDistributionPoints:
  - https://${VAULT_ROUTE}/v1/app-pki/team-one/crl"
EOF
```

Verify from _Vault UI → Secret → app-pki/team-one_ the signed certificate.

![](https://lh3.googleusercontent.com/iZGmD7ZjUZJLFjud-9ZjL9VgCSnFcSHMG7NprnNZRfRAfLfJSgNwns1ez1QEm0NpB4IRoh-y5Wy7LFqpSXwefTgwX0OtjfMgG-j1dFqhnPZayRmyQOgBPz0d4W6KmO6b1Qa3S4uT1ZKi9pkBQQ)

9. Configure the PKI role:

```yaml
cat <<EOF|oc apply -f -
apiVersion: redhatcop.redhat.io/v1alpha1
kind: PKISecretEngineRole
metadata:
  name: team-one
spec:
  authentication:
    path: app-kubernetes/team-one
    role: team-one
  path: app-pki/team-one
  allowedDomains:
   - team-one.vault.int.company.io
   - team-one.svc
   - "*-team-one.apps.${BASE_DOMAIN}"
  allowSubdomains: true
  allowedOtherSans: "*"
  allowGlobDomains: true
  allowedURISans:
  - "*-team-one.apps.${BASE_DOMAIN}"
  maxTTL: "8760h"
EOF
```

Verify from _Vault UI → Secret → app-pki/team-one → Roles_.

![](https://lh4.googleusercontent.com/-Gawb1zWxY1uiDGYMjFY7c3JVfSqI2jXSTxnrSrQfdXDdG5bI-NcYdyNW1v7QMmjIg7RC1F9kb86h7U5P3ekDCccp4tXmLchIk9A1CIzElwMEaJu2IYiX2FL_nMxvWUT71NhmcQJJ0xvMh9pVA)

# Deploy sample application

To demonstrate the end-to-end functionality, a demonstration application can be deployed consisting of two simple Quarkus client/server services that leverage mutual TLS communication through certificates provided by cert-manager and Hashicorp Vault integration.

## Requesting certificate from HashiCorp Vault PKI provider

![](https://lh5.googleusercontent.com/RG3KBqkw2814myxWQQiSwQP-2TyZM5li72JZkNoACZgJMV-XwGUELYdxLh9Bf9PlL5EwDsJFsKnpKo86PdftuDSlIUU5vIg0ytXRVslw6xAkxPfIvc_CbrkOi3F-EsshUu1LzW-YnFm6MsZG-Q)

As we can see in the above diagram, there are several steps to request certificates from Hashicorp Vault that can be consumed by applications.

On OpenShift:

1. The cert-manager Issuer is the interface between certificates and HashiCorp Vault. We defined the path where certificates will be created and Kubernetes authentication to access PKI team-one role. Let’s create an issuer at the namespace level, which is the best way to isolate certificates. So, it is not possible to issue certificates from an Issuer in a different namespace:

    1.1 Get HashiCorp Vault CA Bundle and team-one default service account token.

    ```bash
        export CA_BUNDLE=$(oc get secret vault-certs -n hashicorp -o json | jq -r '.data."ca.crt"')

        export DEFAULT_SECRET=$(oc get sa default -n team-one -o json | jq -r '.secrets[0].name')
    ```

    1.2 Create cert-manager issuer.

    ```yaml
    cat <<EOF| oc apply -f -
    apiVersion: cert-manager.io/v1
    kind: Issuer
    metadata:
      name: team-one-issuer-vault
      namespace: team-one
    spec:
      vault:
        path: app-pki/team-one/sign/team-one
        server: https://vault-active.hashicorp.svc:8200
        caBundle: $CA_BUNDLE
        auth:
          kubernetes:
            role: team-one
            mountPath: /v1/auth/app-kubernetes/team-one
            secretRef:
              key: token
              name: $DEFAULT_SECRET
    EOF
    ```

As we can observe in the code sample above, authentication is performed using namespace default service account, which is specified as **secretRef** in the issuer authentication section. 

This is configured in _Vault UI → Access → Auth Methods → app-kubernetes/team-one → Roles → team-one_ as follows:

![](https://lh3.googleusercontent.com/bpuviCHDmi9nReg240Ba-7rJNHP2sZpKl0tQ2O3C2shYEeBbyEVf2z47JDQgePYPbLKmg2Mw6MjhRffbFX1L1H9G-SdLR4QSYws370xTebn61OvggfzwzvxW6-HATsEYt-4qqu1XNp8NcYFJkA)

We have developed a demo Java application which you can find in the following repository:

[Mutual TLS code sample](https://github.com/openlab-red/hashicorp-vault-for-openshift/tree/master/examples/quarkus-mtls-example)

Since the Issuer is already created, we can deploy the full example performing next steps:

  1. Git clone repository <https://github.com/openlab-red/hashicorp-vault-for-openshift.git>
  2. Connect to your OCP cluster.
  3. Access repository folder examples/quarkus-mtls-example/
  4. Build and deploy the application stack:
      ```bash
      mvn oc:build oc:resource oc:apply -Pprod
      ```

Now, what is happening, behind the scenes, during the application deployment.

2. The deployment creates a certificate that is watched by the cert-manager controller. To support the demo application, there will be two certificates needed in order to enable mutual TLS authentication between client and server.

The Client Certificate.

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: client
  namespace: team-one
spec:
  commonName: client.team-one.vault.int.company.io
  dnsNames:
  - client.team-one.vault.int.company.io
  - client.team-one.svc
  issuerRef:
    name: team-one-issuer-vault
  keystores:
    pkcs12:
      create: true
      passwordSecretRef:
        key: password
        name: client-keystore-pass
  secretName: client
```
  
The Server Certificate.

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: server
  namespace: team-one
spec:
  commonName: server.team-one.vault.int.company.io
  dnsNames:
  - server.team-one.vault.int.company.io
  - server.team-one.svc
  issuerRef:
    name: team-one-issuer-vault
  keystores:
    pkcs12:
      create: true
      passwordSecretRef:
        key: password
        name: server-keystore-pass
  secretName: server
```

3. The cert-manager controller via Key Manager component creates a temporary private key as a secret in the namespace. Then, the Request Manager component will create a certificate request and will sign the CSR using the temporary private key. For more details on the requesting certificate process, it is recommended to review the following [resource](https://cert-manager.io/docs/concepts/certificate/#certificate-lifecycle).

In the _Vault UI → Secrets → app-pki/team-one_ you will find the two certificates recently created by the deployment.

![](https://lh5.googleusercontent.com/9pERdvFYjfRudRFRB3JJuV0mw5ojieBMlSi52luNCkjW4ADWcRJ8HTeLRWZJrEY609W2noznxnp7MMeKdzX1Rz_ppbjtIaBGG7fBZ0oADN-BI2y75Oa-qKvGmc9TyAbCV9juM4XcE9Axv6UKRg)

![](https://lh6.googleusercontent.com/pjoq00SpKoPrR6twwrATyU5_Sx5zeFDuOUUK8SvVPc_2Trsc5cIsbPWbDh3m9wE__F3KK2QggIGKvJ-qeZRwFxQhV_rAPeEeE1tCIPj08y4R_03AQGLNIWdThHAba74zBStM2lX_3IKa-hYPnQ)

4. The cert-manager fetches the certificate from Vault PKI, creates, and populates the certificate secret and finally copies the temporary private key into the secret:

```bash
oc describe secret client

Name:         client
Namespace:    team-one
Labels:       <none>
Annotations:  cert-manager.io/alt-names:  client.team-one.svc,client.team-one.vault.int.company.io
              cert-manager.io/certificate-name: client
              cert-manager.io/common-name: client.team-one.vault.int.company.io
              cert-manager.io/ip-sans:
              cert-manager.io/issuer-group:
              cert-manager.io/issuer-kind: Issuer
              cert-manager.io/issuer-name: team-one-issuer-vault
              cert-manager.io/uri-sans:

Type:  kubernetes.io/tls

Data
====
ca.crt:          1281 bytes
keystore.p12:    4391 bytes
tls.crt:         2550 bytes
tls.key:         1675 bytes
truststore.p12:  1210 bytes

oc describe secret server

Name:         server
Namespace:    team-one
Labels:       <none>
Annotations:  cert-manager.io/alt-names: server.team-one.svc,server.team-one.vault.int.company.io
              cert-manager.io/certificate-name: server
              cert-manager.io/common-name: server.team-one.vault.int.company.io
              cert-manager.io/ip-sans:
              cert-manager.io/issuer-group:
              cert-manager.io/issuer-kind: Issuer
              cert-manager.io/issuer-name: team-one-issuer-vault
              cert-manager.io/uri-sans:

Type:  kubernetes.io/tls

Data
====
truststore.p12:  1210 bytes
ca.crt:          1281 bytes
keystore.p12:    4391 bytes
tls.crt:         2550 bytes
tls.key:         1679 bytes

```

5. The client certificate is mounted as volume in the client application:

```yaml
kind: Deployment
apiVersion: apps/v1
metadata:
  name: client
spec:
  replicas: 1
  selector:
    matchLabels:
      app: client
  Template:
    metadata:
      labels:
        app: client
    spec:
      volumes:
        - name: client
            secret:
            secretName: client
        - name: config
          configMap:
            name: client
      containers:
        - name: client
            ports:
            - containerPort: 8443
              protocol: TCP
            resources: {}
            volumeMounts:
            - name: client
              readOnly: true
              mountPath: /deployments/tls
            - name: config
              mountPath: /deployments/config
```

6. The server certificate is mounted as volume in the server application:

```yaml
kind: Deployment
apiVersion: apps/v1
metadata:
  name: server
spec:
  replicas: 1
  Selector:
  matchLabels:
    app: server
  template:
    metadata:
      labels:
        app: server
    spec:
      volumes:
        - name: server
          secret:
            secretName: server
         - name: config
             configMap:
            name: server
      Containers:
        - name: server
          ports:
            - containerPort: 8443 
              protocol: TCP
             resources: {}
             volumeMounts:
            - name: server
              readOnly: true
              mountPath: /deployments/tls
            - name: config
              mountPath: /deployments/config
```

7. Client-Server connection via mTLS

The sample code is based on [Quarkus](https://quarkus.io/), we define ssl configuration in the client _application.properties_ file as follow:

```java
org.acme.client.mtls.GreetingService/mp-rest/url=https://server:8443
org.acme.client.mtls.GreetingService/mp-rest/trustStore=/deployments/tls/truststore.p12
org.acme.client.mtls.GreetingService/mp-rest/trustStorePassword=123423556
org.acme.client.mtls.GreetingService/mp-rest/keyStore=/deployments/tls/keystore.p12
org.acme.client.mtls.GreetingService/mp-rest/keyStorePassword=123423556

quarkus.http.ssl.certificate.key-store-file=/deployments/tls/keystore.p12
quarkus.http.ssl.certificate.key-store-password=123423556
quarkus.http.ssl.certificate.trust-store-file=/deployments/tls/truststore.p12
quarkus.http.ssl.certificate.trust-store-password=123423556
quarkus.ssl.native=true
````

As well, in server _application.properties_ file:

```java
quarkus.ssl.native=true
quarkus.http.ssl.certificate.key-store-file=/deployments/tls/keystore.p12
quarkus.http.ssl.certificate.key-store-password=123423556
quarkus.http.ssl.certificate.trust-store-file=/deployments/tls/truststore.p12
quarkus.http.ssl.certificate.trust-store-password=123423556

quarkus.http.ssl.client-auth=required
```

In both files the _ssl.native_ is set to true, _keystore_ and _truststore_ is defined and _client-auth_ field is set to **required**.

For more information see the blog, [Quarkus Mutual TLS](https://quarkus.io/blog/quarkus-mutual-tls/), to explore in detail how SSL and Mutual TLS works in Quarkus.

8. Access client exposed application:

```bash
oc extract secret/client --keys=ca.crt
curl --cacert ca.crt https://client-team-one.apps.${BASE_DOMAIN}/hello-client
hello from server
```

# Conclusion

In this article, we walked through an effective and automated approach to manage the lifecycle of application certificates in a Kubernetes environment. This was accomplished by building a certificate authority (CA) in Vault, creating the CA chain hierarchy with an offline root CA and online intermediate CAs.

We also explored cert-manager, the de facto cloud-native solution for certificate issuance and renewal. Cert-manager interacts with HashiCorp Vault, an identity management system. We then introduced how Vault can be installed in a HA manner using integrated storage and leverage SSL certificates issued by cert-manager. We also described the integration of HashiCorp Vault with Kubernetes Authentication method and service account to manage access to resources within Vault.

The configuration of Vault is defined as a declarative process which is then leveraged by the [Vault Config Operator](https://github.com/redhat-cop/vault-config-operator). The X.509 certificates are provided by Vault’s PKI engine, using roles and policies. The cert-manager issuer will connect to the specific PKI engine and request an application certificate.

Last but not least, to understand the user and developer experience, we introduced a demo application that makes use of the full integration involving all the players in the game. Ultimately, the end user can request a certificate through cert-manager and HashiCorp Vault. Certificate secrets are available at Openshift namespace and can be mounted by any application to enable end to end security via trusted chain.
