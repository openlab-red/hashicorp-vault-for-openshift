## Quarkus Mutual TLS with Cert Manager and Vault PKI

### Getting Certificate From PKI Provider

![certificate_workflow.png](diagram/certificate_workflow.png)
1. Create issuer.
2. Watch certificates.
3. Request certificates to Vault PKI.
4. Get certificate and create Kubernetes secrets.
5. Mount certificate in client application.
6. Mount certificate in server application.
7. Client-Server connection with  mTLS.
8. Access client exposed application.

## Local Deployment

### Build
  ```
  mvn clean package -Dquarkus.profile=prod
  ```
### Build & Run OpenShift
  ```
  mvn oc:build oc:resource oc:apply -Pprod
  ```
### Uninstall resources in OpenShift
  ```
  mvn oc:undeploy -Pprod
  ```
## Reference

* https://cert-manager.io
* https://learn.hashicorp.com/tutorials/vault/kubernetes-cert-manager
* https://quarkus.io/blog/quarkus-mutual-tls/
