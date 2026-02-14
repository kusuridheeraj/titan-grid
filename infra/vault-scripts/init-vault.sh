#!/bin/sh

echo "Waiting for Vault to be ready..."
until vault status > /dev/null 2>&1; do
  echo "Vault is unavailable - sleeping"
  sleep 1
done

echo "Vault is up - attempting to initialize..."

# Authenticate with the Root Token
export VAULT_ADDR='http://titan-vault:8200'
export VAULT_TOKEN='dev-root-token-titan-2026'

# Enable the Transit Secrets Engine (Encryption-as-a-Service)
if vault secrets list | grep -q "transit/"; then
    echo "Transit secrets engine already enabled."
else
    echo "Enabling transit secrets engine..."
    vault secrets enable transit
fi

# Create the specific encryption key for Cryptex
if vault list transit/keys | grep -q "titan-grid-key"; then
    echo "Key 'titan-grid-key' already exists."
else
    echo "Creating encryption key 'titan-grid-key'..."
    vault write -f transit/keys/titan-grid-key type=aes256-gcm96
fi

echo "Vault initialization complete!"
