package uk.nhs.ciao.docs.transformer.processor;

import static uk.nhs.ciao.logging.CiaoLogMessage.logMsg;

import java.util.Set;

import com.google.common.base.Preconditions;

import uk.nhs.ciao.docs.parser.ParsedDocument;
import uk.nhs.ciao.docs.parser.PropertyName;
import uk.nhs.ciao.docs.parser.transformer.MappedPropertiesRecorder;
import uk.nhs.ciao.docs.parser.transformer.PropertiesTransformation;
import uk.nhs.ciao.docs.parser.transformer.PropertyCloneUtils;
import uk.nhs.ciao.logging.CiaoLogger;

public class DocumentTransformer {
	private static final CiaoLogger LOGGER = CiaoLogger.getLogger(DocumentTransformer.class);
	
	private final PropertiesTransformation propertiesTransformation;

	/**
	 * If true, the transformation is performed directly on the input properties,
	 * otherwise the input properties are cloned before transformation
	 */
	private boolean inPlace = true;
	
	public DocumentTransformer(final PropertiesTransformation propertiesTransformation) {
		this.propertiesTransformation = Preconditions.checkNotNull(propertiesTransformation);
	}
	
	public ParsedDocument transform(final ParsedDocument source) throws Exception {
		Preconditions.checkNotNull(source);
		
		final ParsedDocument destination = getDestinationDocument(source);
		
		final boolean includeContainers = false;
		final Set<PropertyName> unmappedProperties = PropertyName.findAll(source.getProperties(), includeContainers);
		final MappedPropertiesRecorder recorder = new MappedPropertiesRecorder();
		
		propertiesTransformation.apply(recorder, source.getProperties(), destination.getProperties());

		unmappedProperties.removeAll(recorder.getMappedProperties());
		
		if (!unmappedProperties.isEmpty()) {
			LOGGER.info(logMsg("Document Transformation contains unmapped properties").documentProperties(unmappedProperties));
		}
		
		return destination;
	}
	
	private ParsedDocument getDestinationDocument(final ParsedDocument document) {
		if (inPlace) {
			return document;
		}
		
		return new ParsedDocument(document.getOriginalDocument(), PropertyCloneUtils.deepClone(document.getProperties()));
	}
}
