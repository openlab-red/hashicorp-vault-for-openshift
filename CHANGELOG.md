# 1.9.6-ubi  | Next 

## Version
* [hashicorp/vault-helm:v0.19.0](https://github.com/hashicorp/vault-helm/releases/tag/v0.19.0)
* [hashicorp/vault:1.9.6](https://catalog.redhat.com/software/containers/hashicorp/vault/5fda55bd2937386820429e0c?tag=1.9.6-ubi&push_date=1651295139000&container-tabs=overview)
* [hashicorp/vault-k8s:0.14.2](https://catalog.redhat.com/software/containers/hashicorp/vault-k8s/5fda6941ecb524508951c434?tag=0.14.2-ubi&push_date=1642660877000)

## Installation
* [Standalone Deployment](vault/standalone/README.md)
* [High Availability Deployment](vault/ha/README.md)
* [Vault Injector](vault/injector/README.md)

## OpenShift Tested Version

* [4.9](https://docs.openshift.com/container-platform/4.9/welcome/index.html)
* [4.10](https://docs.openshift.com/container-platform/4.10/welcome/index.html)

## Cert Manager Integration
* [PKI Secret Engine](https://github.com/redhat-cop/vault-config-operator/tree/main/test/pkisecretengine)

## Examples
* [Spring Example](examples/spring-example/README.md)
* [Thorntail Example](examples/thorntail-example/README.md)
* [EAP Example](examples/eap-example/README.md)
* [Python Example](examples/python3-example/README.md)
* [Wildfly MP Example](examples/wildfly-example/README.md)
* [Quarkus Example](examples/quarkus-example/README.md)
* [Golang Example](examples/golang-example/README.md)
* [Quarkus Mutual TLS](examples/quarkus-mtls-example/README.md)

# Articles
* [Secure Cloud Native Applications with HashiCorp Vault and Cert-Manager](./articles/Secure%20Cloud%20Native%20App.md)

## Image Version

* [hashicorp/vault:1.4.1](https://hub.docker.com/layers/vault/library/vault/1.4.1/images/sha256-4161adbd9733623c089bbac60cbac66c55326284baf7fb72f5781d9a56184088?context=explore)



# 1.3.5 | 05/05/2020

## Image Version

* [hashicorp/vault:1.3.5](https://hub.docker.com/layers/vault/library/vault/1.3.5/images/sha256-f14406083da62be6d8e620a97e2333bdd1965e9022fc254e58d3e17d038cf87c?context=explore)

## New Examples
* [Golang Example](examples/golang-example/README.md)

## Contributors

Thanks a lot to [@radudd](https://github.com/openlab-red/hashicorp-vault-for-openshift/commits?author=radudd).

# 1.3.2 | 23/04/2020

* [hashicorp/vault:1.3.2](https://hub.docker.com/layers/vault/library/vault/1.3.2/images/sha256-e6ed7d173e84765278879501b31ea7b475047f82a3b12e88aaf5640e8660f650?context=explore)
* [openlabred/vault-k8s:0.3.1](https://hub.docker.com/layers/openlabred/vault-k8s/0.3.1/images/sha256-ecef1945754a7334a4c8591a6bb00c37fca2789366351fea4b41f9167ecd8529?context=repo)
    * Upgraded to MutatingWebhookConfiguration v1 API: [#112](https://github.com/hashicorp/vault-k8s/pull/112)
    * Agent RunAsUser has as a default value the RunAsUser defined in the application container: [#126](https://github.com/hashicorp/vault-k8s/pull/126)
