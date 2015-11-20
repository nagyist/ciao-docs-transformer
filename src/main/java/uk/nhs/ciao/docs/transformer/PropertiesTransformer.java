package uk.nhs.ciao.docs.transformer;

import java.util.List;
import java.util.Map;

import org.joda.time.format.DateTimeFormat;

import uk.nhs.ciao.docs.parser.PropertyName;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Transforms an incoming set of properties
 */
public class PropertiesTransformer implements PropertiesTransformation {
	/**
	 * If true, the transformation is performed directly on the input properties,
	 * otherwise the input properties are cloned before transformation
	 */
	private boolean inPlace = true;
	
	/**
	 * The registered list of transformation to perform.
	 * <p>
	 * The transformations are processed sequentially based on registration order
	 */
	private List<PropertiesTransformation> transformations = Lists.newArrayList();
	
	@Override
	public void apply(final TransformationRecorder recorder, final Map<String, Object> source, final Map<String, Object> destination) {
		for (final PropertiesTransformation transformation: transformations) {
			transformation.apply(recorder, source, destination);
		}
	}
	
	public boolean isInPlace() {
		return inPlace;
	}
	
	public void setInPlace(final boolean inPlace) {
		this.inPlace = inPlace;
	}
	
	public List<PropertiesTransformation> getTransformations() {
		return transformations;
	}
	
	public void setTransformations(final List<PropertiesTransformation> transformations) {
		this.transformations = Preconditions.checkNotNull(transformations);
	}
	
	public void addTransformation(final PropertiesTransformation transformation) {
		if (transformation != null) {
			transformations.add(transformation);
		}
	}
	
	public void renameProperty(final String from, final String to) {
		transformations.add(new RenamePropertyTransformation(PropertyName.valueOf(from),
				new PropertyMutator(to)));
	}
	
	public void splitProperty(final String from, final String pattern, final String... to) {
		final PropertyMutator[] mutators = new PropertyMutator[to.length];
		for (int index = 0; index < to.length; index++) {
			mutators[index] = new PropertyMutator(to[index]);
		}
		transformations.add(new SplitPropertyTransformation(PropertyName.valueOf(from), pattern, mutators));
	}
	
	public void splitListProperty(final String from, final String pattern, final String to) {
		transformations.add(new SplitListPropertyTransformation(PropertyName.valueOf(from), pattern,
				new PropertyMutator(to)));
	}
	
	public void combineProperties(final String to, final String... from) {
		combineProperties(new PropertyMutator(to), from);
	}
	
	public void combineProperties(final PropertyMutator to, final String... from) {
		final PropertyName[] fromNames = new PropertyName[from.length];
		for (int index = 0; index < from.length; index++) {
			fromNames[index] = PropertyName.valueOf(from[index]);
		}
		transformations.add(new CombinePropertiesTransformation(to, fromNames));
	}
	
	public void reformatDateProperty(final String property, final String fromFormat, final String toFormat) {
		formatDateProperty(property, fromFormat, property, toFormat);
	}
	
	public void formatDateProperty(final String from, final String fromFormat, final String to, final String toFormat) {
		transformations.add(new FormatDatePropertyTransformation(PropertyName.valueOf(from),
				DateTimeFormat.forPattern(fromFormat).withZoneUTC(),
				new PropertyMutator(to),
				DateTimeFormat.forPattern(toFormat).withZoneUTC()));
	}
	
	public void findAndFormatDateProperties(final String fromFormat, final String toFormat) {
		transformations.add(new FindAndFormatDatePropertiesTransformation(
				DateTimeFormat.forPattern(fromFormat).withZoneUTC(),
				DateTimeFormat.forPattern(toFormat).withZoneUTC()));
	}
	
	public PropertiesTransformer nestedTransformer(final String from) {
		final PropertiesTransformer transformer = new PropertiesTransformer();
		transformer.setInPlace(inPlace);
		transformations.add(new NestedPropertiesTransformation(PropertyName.valueOf(from), transformer));
		return transformer;
	}
}
