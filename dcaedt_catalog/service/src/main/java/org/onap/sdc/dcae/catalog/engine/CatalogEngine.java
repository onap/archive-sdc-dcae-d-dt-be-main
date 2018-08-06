package org.onap.sdc.dcae.catalog.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("org.onap.sdc.dcae")
public class CatalogEngine {

	public static void main(String[] args) {

			SpringApplication.run(CatalogEngine.class, args);
	}

}
