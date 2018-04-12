package org.onap.sdc.dcae.catalog.asdc;

import org.onap.sdc.dcae.composition.util.SystemProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ASDCEngine {

	/**
	 * Creates and returns a new instance of a {@link SystemProperties} class.
	 * 
	 * @return New instance of {@link SystemProperties}.
	 */
	@Bean
	public SystemProperties systemProperties() {
		return new SystemProperties();
	}

	public static void main(String[] args) {
		SpringApplication.run(ASDCEngine.class, args);
	}

}
