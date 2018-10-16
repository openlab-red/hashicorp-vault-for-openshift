package com.vault;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class VaultDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(VaultDemoApplication.class, args);
	}
    
}
