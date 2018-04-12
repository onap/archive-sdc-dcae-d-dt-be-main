package org.onap.sdc.dcae.catalog.engine;

import org.onap.sdc.dcae.catalog.engine.CatalogEngine;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import java.util.Arrays;



@SpringBootApplication

public class CatalogEngine {

	public static void main(String[] args) {

			SpringApplication.run(CatalogEngine.class, args);
	}

}
