package uk.nhs.ciao.docs.transformer.kings;

import static uk.nhs.ciao.docs.transformer.kings.KingsPropertiesTransformerFactory.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.unitils.reflectionassert.ReflectionAssert;

import uk.nhs.ciao.docs.transformer.PropertiesTransformation;
import uk.nhs.ciao.docs.transformer.TransformationRecorder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Closeables;

/**
 * Tests for transformers created by {@link KingsPropertiesTransformerFactory}
 */
public class KingsPropertiesTransformerFactoryTest {
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String,Object>>() {};
	private ObjectMapper objectMapper;
	
	@Before
	public void setup() {
		this.objectMapper = new ObjectMapper();
	}
	
	@Test
	public void testWordDischargeNotificationTransformer() throws IOException {
		transformAndAssert("Example4.txt", createWordDischargeNotificationTransformer());
	}
	
	private void transformAndAssert(final String name, final PropertiesTransformation transformation) throws IOException {
		final Map<String, Object> actual = loadResource("input/" + name);
		final Map<String, Object> expected = loadResource("output/" + name);
		
		transformation.apply(Mockito.mock(TransformationRecorder.class), actual, actual);

		ReflectionAssert.assertReflectionEquals(expected, actual);
	}
	
	private Map<String, Object> loadResource(final String resourceName) throws IOException {
		final InputStream in = getClass().getResourceAsStream(resourceName);
		try {
			return objectMapper.readValue(in, MAP_TYPE);
		} finally {
			Closeables.closeQuietly(in);
		}
	}
}
