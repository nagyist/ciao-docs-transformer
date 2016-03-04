package uk.nhs.ciao.docs.transformer;

import uk.nhs.ciao.camel.CamelApplication;
import uk.nhs.ciao.camel.CamelApplicationRunner;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.exceptions.CIAOConfigurationException;

/**
 * The main ciao-docs-transformer application
 * <p>
 * The application configuration is handled by Spring loading META-INF/spring/beans.xml 
 * resource off the class-path. Additional spring configuration is loaded based on
 * properties specified in CIAO-config (ciao-docs-transformer.properties). At runtime the application
 * can start multiple routes (one per input queue) determined via the specified CIAO-config properties. 
 */
public class DocumentTransformerApplication extends CamelApplication {
	/**
	 * Runs the document parser application
	 * 
	 * @see CIAOConfig#CIAOConfig(String[], String, String, java.util.Properties)
	 * @see CamelApplicationRunner
	 */
	public static void main(final String[] args) throws Exception {
		final CamelApplication application = new DocumentTransformerApplication(args);
		CamelApplicationRunner.runApplication(application);
	}
	
	public DocumentTransformerApplication(final String... args) throws CIAOConfigurationException {
		super("ciao-docs-transformer.properties", args);
	}
	
	public DocumentTransformerApplication(final CIAOConfig ciaoConfig, final String... args) {
		super(ciaoConfig, args);
	}
}
