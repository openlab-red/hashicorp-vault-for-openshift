# Allow a token to get a secret from the generic secret backend
# for the client role.
path "secret/example" {
  capabilities = ["read", "list"]
}
