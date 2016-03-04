package uk.nhs.ciao.docs.transformer;

import java.util.List;
import java.util.Map;

import uk.nhs.ciao.docs.parser.PropertyName;

import com.google.common.base.Preconditions;

public class NestedPropertiesTransformation implements PropertiesTransformation {
	private final PropertyName from;
	private final PropertiesTransformation transformation;
	
	public NestedPropertiesTransformation(final PropertyName from, final PropertiesTransformation transformation) {
		this.from = Preconditions.checkNotNull(from);
		this.transformation = Preconditions.checkNotNull(transformation);
	}
	
	@Override
	public void apply(final TransformationRecorder recorder, final Map<String, Object> source, final Map<String, Object> destination) {
		final Object originalValue = from.get(source);
		
		if (originalValue instanceof Map) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> nestedProperties = (Map<String, Object>)originalValue;
			transformation.apply(new NestedTransformationRecorder(from, recorder),
					nestedProperties, destination);
		} else if (originalValue instanceof List) {
			for (final PropertyName childProperty: from.listChildren(source)) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> nestedProperties = childProperty.get(Map.class, source);
				transformation.apply(new NestedTransformationRecorder(childProperty, recorder),
						nestedProperties, destination);
			}
		}
	}
}
