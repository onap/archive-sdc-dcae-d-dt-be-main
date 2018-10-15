package org.onap.sdc.dcae.composition;

import org.onap.sdc.common.onaplog.Enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.composition.util.SystemProperties;
import org.onap.sdc.dcae.errormng.ErrConfMgr;
import org.onap.sdc.dcae.errormng.ErrorConfigurationLoader;
import org.onap.sdc.dcae.filter.LoggingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Configuration
@EnableScheduling
@SpringBootApplication
@ComponentScan("org.onap.sdc.dcae")
@EnableAutoConfiguration
@PropertySource("file:${jetty.base}/config/dcae-be/application.properties")
public class CompositionEngine extends SpringBootServletInitializer implements CommandLineRunner{
	private static final String SPECIFICATION_VERSION = "Specification-Version";
	@Autowired
	ServletContext servletContext;
	private static final String MANIFEST_FILE_NAME = "/META-INF/MANIFEST.MF";
	private static String dcaeVersion;
	private OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	public static void main(String[] args) {
		SpringApplication.run(CompositionEngine.class, args);
	}
	
	/**
	 * Creates and returns a new instance of a {@link SystemProperties} class.
	 * 
	 * @return New instance of {@link SystemProperties}.
	 */
	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CompositionEngine.class);
    }
   
	
	@Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                		.allowedOrigins("*")
                		.allowedHeaders("*")
                		.allowedMethods("GET", "POST", "OPTIONS", "PUT")
                		.allowCredentials(false)
                		.maxAge(3600);
                		
            }
        };
    }

	@Override
	public void run(String... args) throws Exception {

		ErrorConfigurationLoader errorConfigurationLoader = new ErrorConfigurationLoader(System.getProperty("jetty.base"));
		ErrConfMgr instance = ErrConfMgr.INSTANCE;
		InputStream inputStream = servletContext.getResourceAsStream(MANIFEST_FILE_NAME);

		//setLogbackXmlLocation();

		String version = null;
		try {
			Manifest mf = new Manifest(inputStream);
			Attributes atts = mf.getMainAttributes();
			version = atts.getValue(SPECIFICATION_VERSION);
			if (version == null || version.isEmpty()) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "failed to read DCAE version from MANIFEST.");
			} else {
				debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "DCAE version from MANIFEST is {}", version);
				dcaeVersion = version;
			}

		} catch (IOException e) {
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), "failed to read DCAE version from MANIFEST: {}", e.getMessage());
		}
		
	}

	private void setLogbackXmlLocation() throws Exception {
		String jettyBase = System.getProperty("config.home");
		Properties props = System.getProperties();
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Current System Properties are: {}", props);
		if (jettyBase == null) {
			String msg = "Couldn't resolve config.home environmental variable";
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), msg);
			throw new Exception(msg + ". Failed to configure logback.xml location... aborting.");
		}
		String logbackXmlLocation = jettyBase+"/dcae-be/logback.xml";
		props.setProperty("logback.configurationFile", logbackXmlLocation);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Successfuly set the logback.xml location to {}", logbackXmlLocation);
	}

    @Bean
    public FilterRegistrationBean contextLifecycleFilter() {
        Collection<String> urlPatterns = new ArrayList<>();
        urlPatterns.add("/*");

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new LoggingFilter());
        filterRegistrationBean.setUrlPatterns(urlPatterns);

        return filterRegistrationBean;
    }

	public static String getDcaeVersion() {
		return dcaeVersion;
	}
	
}
