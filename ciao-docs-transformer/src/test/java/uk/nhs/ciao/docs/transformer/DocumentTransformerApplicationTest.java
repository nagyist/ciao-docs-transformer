package uk.nhs.ciao.docs.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.apache.camel.model.ModelCamelContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Closeables;

import uk.nhs.ciao.camel.CamelApplicationRunner;
import uk.nhs.ciao.camel.CamelApplicationRunner.AsyncExecution;
import uk.nhs.ciao.camel.CamelUtils;
import uk.nhs.ciao.configuration.CIAOConfig;
import uk.nhs.ciao.configuration.impl.MemoryCipProperties;
import uk.nhs.ciao.docs.parser.Document;
import uk.nhs.ciao.docs.parser.ParsedDocument;
import static org.junit.Assert.*;


/**
 * Tests for the ciao-docs-transformer CIP application
 */
public class DocumentTransformerApplicationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentTransformerApplicationTest.class);
	private static final String CIP_NAME = "ciao-docs-transformer";
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};
	
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);
	
	private ExecutorService executorService;
	private DocumentTransformerApplication application;
	private AsyncExecution execution;
	private MockEndpoint transformedDocumentEndpoint;
	private ObjectMapper objectMapper;
	private ProducerTemplate producerTemplate;
	
	@Before
	public void setup() throws Exception {
		final CIAOConfig ciaoConfig = setupCiaoConfig();
		application = new DocumentTransformerApplication(ciaoConfig);
		
		executorService = Executors.newSingleThreadExecutor();
		objectMapper = new ObjectMapper();
	}
	
	private CIAOConfig setupCiaoConfig() throws IOException {
		final MemoryCipProperties cipProperties = new MemoryCipProperties(CIP_NAME, "tests");
		addProperties(cipProperties, CIP_NAME + ".properties");
		addProperties(cipProperties, CIP_NAME + "-test.properties");
		
		return new CIAOConfig(cipProperties);
	}
	
	private void addProperties(final MemoryCipProperties cipProperties, final String resourcePath) throws IOException {
		final Resource resource = new ClassPathResource(resourcePath);
		final Properties properties = PropertiesLoaderUtils.loadProperties(resource);
		cipProperties.addConfigValues(properties);
	}
	
	private void runApplication() throws Exception {
		LOGGER.info("About to start camel application");
		
		execution = CamelApplicationRunner.runApplication(application, executorService);
		
		interceptJMSEndpoints();
		
		producerTemplate = new DefaultProducerTemplate(getCamelContext());
		producerTemplate.start();
		
		LOGGER.info("Camel application has started");
	}
	
	private void interceptJMSEndpoints() throws Exception {
		LOGGER.info("Interecepting JMS endpoints with mocks");
		
		final ModelCamelContext context = (ModelCamelContext)getCamelContext();
		for (final String id: Arrays.asList("transform-document-kings", "transform-document-kent")) {
			context.getRouteDefinition(id).adviceWith(context, new AdviceWithRouteBuilder() {				
				@Override
				public void configure() throws Exception {
					mockEndpoints("jms:queue:transformed-documents");
				}
			});
		}
		
		transformedDocumentEndpoint = MockEndpoint.resolve(context, "mock:jms:queue:transformed-documents");
	}
	
	@After
	public void tearDown() {
		try {
			stopApplication();
		} finally {
			// Always stop the executor service
			executorService.shutdownNow();
		}
	}
	
	private void stopApplication() {
		if (execution == null) {
			return;
		}
		
		final CamelContext context = getCamelContext();
		try {
			LOGGER.info("About to stop camel application");
			execution.getRunner().stop();
			execution.getFuture().get(); // wait for task to complete
			LOGGER.info("Camel application has stopped");
		} catch (Exception e) {
			LOGGER.warn("Exception while trying to stop camel application", e);
		} finally {
			if (context != null) {
				MockEndpoint.resetMocks(context);
			}
			CamelUtils.stopQuietly(producerTemplate);
		}
	}
	
	private CamelContext getCamelContext() {
		if (execution == null) {
			return null;
		}
		
		final List<CamelContext> camelContexts = execution.getRunner().getCamelContexts();
		return camelContexts.isEmpty() ? null : camelContexts.get(0);
	}
	
	@Test
	public void testApplicationStartsUsingSpringConfig() throws Exception {
		LOGGER.info("Checking the application starts via spring config");

		runApplication();
		
		assertNotNull(execution);
		assertFalse(execution.getRunner().getCamelContexts().isEmpty());
		assertNotNull(getCamelContext());
	}
	
	@Test
	public void testKingsDocumentIsTransformed() throws Exception {
		LOGGER.info("testKingsDocumentIsTransformed()");

		runApplication();
		
		final ParsedDocument input = new ParsedDocument(new Document("example.txt", "hello".getBytes(), "text/plain"),
				loadProperties("kings/input/Example4.txt"));
		final ParsedDocument expected = new ParsedDocument(new Document("example.txt", "hello".getBytes(), "text/plain"),
				loadProperties("kings/output/Example4.txt"));
		
		transformedDocumentEndpoint.expectedMessageCount(1);
		transformedDocumentEndpoint.expectedBodiesReceived(objectMapper.writeValueAsString(expected));
		
		sendKingsDocument(input);
		
		transformedDocumentEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void testKentDocumentIsTransformed() throws Exception {
		LOGGER.info("testKentDocumentIsTransformed()");

		runApplication();
		
		final ParsedDocument input = new ParsedDocument(new Document("example.txt", "hello".getBytes(), "text/plain"),
				loadProperties("kent/input/Example7.txt"));
		final ParsedDocument expected = new ParsedDocument(new Document("example.txt", "hello".getBytes(), "text/plain"),
				loadProperties("kent/output/Example7.txt"));
		
		transformedDocumentEndpoint.expectedMessageCount(1);
		transformedDocumentEndpoint.expectedBodiesReceived(objectMapper.writeValueAsString(expected));
		
		sendKentDocument(input);
		
		transformedDocumentEndpoint.assertIsSatisfied();
	}
	
	private void sendKingsDocument(final ParsedDocument parsedDocument) throws Exception {
		producerTemplate.sendBody("jms:queue:parsed-kings-documents",
				ExchangePattern.InOnly, objectMapper.writeValueAsString(parsedDocument));
	}
	
	private void sendKentDocument(final ParsedDocument parsedDocument) throws Exception {
		producerTemplate.sendBody("jms:queue:parsed-kent-documents",
				ExchangePattern.InOnly, objectMapper.writeValueAsString(parsedDocument));
	}
	
	private Map<String, Object> loadProperties(final String name) throws Exception {
		final InputStream in = getClass().getResourceAsStream(name);
		try {
			return objectMapper.readValue(in, MAP_TYPE);
		} finally {
			Closeables.closeQuietly(in);
		}
	}
}
