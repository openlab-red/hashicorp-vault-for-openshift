package com.vault.demo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import javax.inject.Inject;

public class SecretService {

    @Inject
    @ConfigProperty(name="secret.example.password")
    String password;


    public String secret() {
        return "My secret " + password + "!";
    }

}
