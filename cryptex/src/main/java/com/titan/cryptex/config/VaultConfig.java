package com.titan.cryptex.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

import java.net.URI;

/**
 * HashiCorp Vault Configuration.
 * Connects to the Vault server to manage encryption keys securely.
 * Uses Transit Engine for Encryption-as-a-Service.
 */
@Slf4j
@Configuration
public class VaultConfig extends AbstractVaultConfiguration {

    @Override
    public VaultEndpoint vaultEndpoint() {
        String vaultUri = "http://localhost:8200"; // Dev mode default
        return VaultEndpoint.from(URI.create(vaultUri));
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        // In production, use Kubernetes or AppRole auth.
        // For this portfolio demo, we use the root token from docker-compose.
        return new TokenAuthentication("dev-root-token-titan-2026");
    }
}
