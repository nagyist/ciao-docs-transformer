package uk.nhs.ciao.docs.transformer.route;

import static uk.nhs.ciao.logging.CiaoCamelLogMessage.camelLogMsg;

import org.apache.camel.Exchange;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spring.spi.TransactionErrorHandlerBuilder;

import uk.nhs.ciao.camel.BaseRouteBuilder;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.docs.parser.HeaderNames;
import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.ciao.docs.parser.route.InProgressFolderManagerRoute;
import uk.nhs.ciao.exceptions.CIAOConfigurationException;
import uk.nhs.ciao.logging.CiaoCamelLogger;

/**
 * Creates a Camel route for the specified name / property prefix.
 * <p>
 * Each configurable property is determined by:
 * <ul>
 * <li>Try the specific property: <code>${ROOT_PROPERTY}.${name}.${propertyName}</code></li>
 * <li>If missing fallback to: <code>${ROOT_PROPERTY}.${propertyName}</code></li>
 * </ul>
 */
public class TransformDocumentRoute extends BaseRouteBuilder {
	private static final CiaoCamelLogger LOGGER = CiaoCamelLogger.getLogger(TransformDocumentRoute.class);
	
	/**
	 * The root property 
	 */
	public static final String ROOT_PROPERTY = "documentTransformerRoutes";
	
	private final String name;
	private final String inputQueue;
	private final String transformerId;
	private final String outputQueue;
	private String inProgressFolderManagerUri;
	
	/**
	 * Creates a new route builder for the specified name / property prefix
	 * 
	 * @param name The route name / property prefix
	 * @throws CIAOConfigurationException If required properties were missing
	 */
	public TransformDocumentRoute(final String name, final CIAOConfig config) throws CIAOConfigurationException {
		this.name = name;
		this.inputQueue = findProperty(config, "inputQueue");
		this.transformerId = findProperty(config, "transformerId");
		this.outputQueue = findProperty(config, "outputQueue");
	}
	
	public void setInProgressFolderManagerUri(final String inProgressFolderManagerUri) {
		this.inProgressFolderManagerUri = inProgressFolderManagerUri;
	}
	
	/**
	 * Try the specific 'named' property then fall back to the general 'all-routes' property
	 */
	private String findProperty(final CIAOConfig config, final String propertyName) throws CIAOConfigurationException {
		final String specificName = ROOT_PROPERTY + "." + name + "." + propertyName;
		final String genericName = ROOT_PROPERTY + "." + propertyName;
		if (config.getConfigKeys().contains(specificName)) {
			return config.getConfigValue(specificName);
		} else if (config.getConfigKeys().contains(genericName)) {
			return config.getConfigValue(genericName);
		} else {
			throw new CIAOConfigurationException("Could not find property " + propertyName +
					" for route " + name);
		}
	}

	/**
	 * Configures / creates a new Camel route corresponding to the set of CIAO-config
	 * properties associated with the route name.
	 */
	@Override
	public void configure() throws Exception {
		from("jms:queue:" + inputQueue)
			.id("transform-document-" + name)

			.errorHandler(new TransactionErrorHandlerBuilder()
				.maximumRedeliveries(0)) // redeliveries are disabled (transformation is only tried once)
				.transacted("PROPAGATION_NOT_SUPPORTED")
			.doTry()
				.process(LOGGER.info(camelLogMsg("Transforming incoming document")
						.documentId(header(Exchange.CORRELATION_ID))
						.eventName(constant("transforming-document"))
						.originalFileName(header(HeaderNames.SOURCE_FILE_NAME))))
				
				.unmarshal().json(JsonLibrary.Jackson, ParsedDocument.class)
						
				.beanRef(transformerId, "transform", true)
				
				.process(LOGGER.info(camelLogMsg("Transformed incoming document")
						.documentId(header(Exchange.CORRELATION_ID))
						.eventName(constant("transform-document"))
						.originalFileName(header(HeaderNames.SOURCE_FILE_NAME))))
				
				.marshal().json(JsonLibrary.Jackson)
				
				.to("jms:queue:" + outputQueue)
			.endDoTry()
			.doCatch(Exception.class)
				.process(LOGGER.warn(camelLogMsg("Unable to transform document")
						.documentId(header(Exchange.CORRELATION_ID))
						.eventName(constant("document-transform-failed"))
						.originalFileName(header(HeaderNames.SOURCE_FILE_NAME))))
						
				// Add a preparation-failed event to the in-progress directory
				.setHeader(InProgressFolderManagerRoute.Header.ACTION, constant(InProgressFolderManagerRoute.Action.STORE))
				.setHeader(InProgressFolderManagerRoute.Header.FILE_TYPE, constant(InProgressFolderManagerRoute.FileType.EVENT))
				.setHeader(InProgressFolderManagerRoute.Header.EVENT_TYPE, constant(InProgressFolderManagerRoute.EventType.MESSAGE_PREPARATION_FAILED))
				.setHeader(Exchange.FILE_NAME).constant(InProgressFolderManagerRoute.MessageType.DOCUMENT)
				.setBody().simple("ciao-docs-transformer\n\n${exception.message}\n${exception.stacktrace}")
				.to(inProgressFolderManagerUri)
			.endDoTry()
		.end();
	}
}