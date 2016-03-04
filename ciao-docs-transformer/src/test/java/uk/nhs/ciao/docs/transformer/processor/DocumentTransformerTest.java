package uk.nhs.ciao.docs.transformer.processor;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import uk.nhs.ciao.docs.parser.Document;
import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.ciao.docs.parser.PropertyName;
import uk.nhs.ciao.docs.transformer.PropertiesTransformation;
import uk.nhs.ciao.docs.transformer.TransformationRecorder;

/**
 * Unit tests for {@link DocumentTransformer}
 */
public class DocumentTransformerTest {
	@Test
	public void testClonedTransformation() throws Exception {
		final DocumentTransformer transformer = new DocumentTransformer(new PropertiesTransformation() {
			@Override
			public void apply(final TransformationRecorder recorder,
					final Map<String, Object> source, final Map<String, Object> destination) {
				// Update all properties to an arbitrary value
				for (final PropertyName name: PropertyName.findAll(source, false)) {
					name.set(destination, "altered");
					recorder.record(name, name);
				}
			}
		});
		transformer.setInPlace(false);
		
		final Map<String, Object> sourceProperties = Maps.newLinkedHashMap();
		sourceProperties.put("name", "name");
		sourceProperties.put("Age", 17);
		sourceProperties.put("values", Lists.newArrayList("abc", "def", "ghi"));
		
		
		
		final ParsedDocument destination = transformer.transform(new ParsedDocument(new Document("example.txt", "value".getBytes()),
				sourceProperties));
		Assert.assertNotSame(sourceProperties, destination.getProperties());
		Assert.assertEquals(PropertyName.findAll(sourceProperties, false), PropertyName.findAll(destination.getProperties(), false));
	}
}
