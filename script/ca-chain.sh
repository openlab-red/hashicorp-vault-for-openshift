#!/bin/sh

export CERT_ROOT=$(pwd)

mkdir -p ${CERT_ROOT}/ca-chain/{root,intermediate}

#Generate the CA Private Key

cd ${CERT_ROOT}/ca-chain/root/

openssl genrsa -out ca.key 2048

touch index.txt
echo 1000 > serial
mkdir -p newcerts

#Define the openssl.cnf

cat <<EOF > openssl.cnf
[ ca ]
default_ca = CA_default

[ CA_default ]
# Directory and file locations.
dir               = ${CERT_ROOT}/ca-chain/root
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

# Generate the certificate

openssl req -x509 -new -nodes -key ca.key -sha256 -days 1024 -out ca.crt -extensions v3_ca -config openssl.cnf

# Generate the Intermediate CA Private Key

cd ${CERT_ROOT}/ca-chain/intermediate/

openssl genrsa -out ca.key 2048

# Generate the Certificate Siging Request:

openssl req -new -sha256 -key ca.key -out ca.csr

# Create the intermediate certificate

openssl ca -config ${CERT_ROOT}/ca-chain/root/openssl.cnf -extensions v3_intermediate_ca -days 365 -notext -md sha256 -in ca.csr -out ca.crt

cd ..
cd ..