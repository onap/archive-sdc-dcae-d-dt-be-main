package org.onap.sdc.dcae.errormng;

import org.apache.commons.lang.ArrayUtils;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.onap.sdc.common.onaplog.enums.LogLevel;

public class ErrorConfigurationLoader {

	private static ErrorConfigurationLoader instance;
	private String jettyBase;
	private ErrorConfiguration errorConfiguration = new ErrorConfiguration();
	private OnapLoggerError errLogger = OnapLoggerError.getInstance();
	private OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

	public ErrorConfigurationLoader(String sourcePath) {
		jettyBase = sourcePath;
		loadErrorConfiguration();
		instance = this;
	}

	private void loadErrorConfiguration(){

		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "ErrorConfigurationLoader: Trying to load error configuration");
		if (jettyBase == null) {
			String msg = "Couldn't resolve jetty.base environmental variable";
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), msg);
			throw new ExceptionInInitializerError (msg + ". Failed to load error configuration files... aborting");
		}

		String path = jettyBase + "/config/dcae-be";
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "jetty.base={}", jettyBase);
		debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Configuration Path={}", path);

		File dir = new File(path);
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				return name.equals("error-configuration.yaml");
			}
		});

		if (ArrayUtils.isEmpty(files)) {
			String msg = "No error configuration files found";
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), msg);
			throw new ExceptionInInitializerError (msg);
		}else if (files.length>1){
			String msg = "Multiple configuration files found. Make sure only one file exists. Path: "+ path;
			errLogger.log(LogLevel.ERROR, this.getClass().getName(), msg);
			throw new ExceptionInInitializerError (msg);
		}
		else {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Loading error configuration file: {}", files[0].getName());
			try {
				errorConfiguration = parseErrConfFileAndSaveToMap(files[0].getCanonicalPath());
//				convertToUsefulMaps(errorConfiguration);
			} catch (IOException e) {
				String msg = "Exception thrown while trying to read the error configuration file path. File="+files[0].getName();
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), msg);
				throw new ExceptionInInitializerError (msg);
			}
			if(errorConfiguration == null){
				String msg = "Error configuration file couldn't be parsed";
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), msg);
				throw new ExceptionInInitializerError (msg);
			}
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Error Configuration: {}", errorConfiguration.toString());
		}
	}
	

	private ErrorConfiguration parseErrConfFileAndSaveToMap(String fullFileName) {

		Yaml yaml = new Yaml();

		InputStream in = null;
		ErrorConfiguration errorConfiguration = null;
		try {

			File f = new File(fullFileName);
			if (false == f.exists()) {
				errLogger.log(LogLevel.ERROR, this.getClass().getName(), "The file {} cannot be found. Ignore reading configuration.", fullFileName);
				return null;
			}
			in = Files.newInputStream(Paths.get(fullFileName));

			errorConfiguration = yaml.loadAs(in, ErrorConfiguration.class);

		} catch (Exception e) {
			debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Failed to convert yaml file {} to object. {}", fullFileName, e);
			return null;
		} 
		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Failed to close input stream {}", e.getMessage());
				}
			}
		}

		return errorConfiguration;
	}

	ErrorConfiguration getErrorConfiguration() {
		return errorConfiguration;
	}

	public static ErrorConfigurationLoader getErrorConfigurationLoader() {
		return instance;
	}
}
