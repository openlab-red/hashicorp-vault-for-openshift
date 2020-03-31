package com.vault.demo;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class SecretService {

    @Inject
    @ConfigProperty(name="secret.example.password")
    String password;


    public String secret() {
        return "My secret " + password + "!";
    }

}
