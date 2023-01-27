#!/bin/bash
## From a new terminal set the env vars to access vault as root
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=root

## Enable approle authentication and
vault auth enable approle

## Add policy for deident to authenticate and pull secrets
vault policy write app - << EOF
path "secret/data/deident/*" {
  capabilities = [ "read" ]
}
path "secret/data/qomop/deident-credentials" {
  capabilities = [ "read" ]
}
EOF

vault policy write deployment - << EOF
path "auth/approle/role/deident/*" {
  capabilities = [ "read", "update" ]
}
EOF

## Add a role for deident using this policy with a short TTL
vault write auth/approle/role/deident token_policies=app token_ttl=20s

## Add the secrets so deident doesn't crash on startup
vault kv put secret/deident/rds-credentials username=username password=password
vault kv put secret/deident/research-platform client-cert=123 server-cert=456 client-key=789

## Create a initial deployment token
vault token create -field token -policy=deployment
