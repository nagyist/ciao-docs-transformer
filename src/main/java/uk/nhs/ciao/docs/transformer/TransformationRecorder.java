package uk.nhs.ciao.docs.transformer;

import uk.nhs.ciao.docs.parser.PropertyName;

public interface TransformationRecorder {
	void record(PropertyName from, PropertyName to);
}
