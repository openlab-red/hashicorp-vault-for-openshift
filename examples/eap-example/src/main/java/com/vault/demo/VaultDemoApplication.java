package com.vault.demo;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.net.URL;

@ApplicationPath("/")
public class VaultDemoApplication extends Application {

}
