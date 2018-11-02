# Allow a token to get a secret from the generic secret backend
# for the client role.
path "database/creds/pg-readwrite" {
  capabilities = ["read"]
}
path "secret/example" {
  capabilities = ["read", "list"]
}