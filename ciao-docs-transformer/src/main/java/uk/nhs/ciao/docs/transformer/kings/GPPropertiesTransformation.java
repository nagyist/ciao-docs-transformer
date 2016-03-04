package uk.nhs.ciao.docs.transformer.kings;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import uk.nhs.ciao.docs.parser.PropertyName;
import uk.nhs.ciao.docs.transformer.PropertiesTransformation;
import uk.nhs.ciao.docs.transformer.PropertyMutator;
import uk.nhs.ciao.docs.transformer.TransformationRecorder;

/**
 * Custom {@link PropertiesTransformation} to handle the mapping of incoming
 * GP lines (as List), to transfer of care fields.
 * <p>
 * The expected format is
 * 
 * <ul>
 * <li>${GP name} -> *ignored*</li>
 * <li>${GP practice name} -> usualGPOrgName
 * <li>${address line 1} -> usualGPAddress.addressLine
 * <li>...
 * <li>${address line N} -> usualGPAddress.addressLine
 * <li>${postcode} -> usualGPAddress.postcode */
public class GPPropertiesTransformation implements PropertiesTransformation {
	private final PropertyName GP = PropertyName.valueOf("GP");
	private final PropertyMutator orgName = new PropertyMutator("usualGPOrgName");
	private final PropertyMutator addressLines = new PropertyMutator("usualGPAddressLine");
	private final PropertyMutator postcode = new PropertyMutator("usualGPAddressPostcode");
	
	@Override
	public void apply(final TransformationRecorder recorder, final Map<String, Object> source, final Map<String, Object> destination) {
		final Object candidate = GP.get(source);
		if (!(candidate instanceof List<?>)) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		final List<String> lines = (List<String>)candidate;
		if (lines.size() < 2) {
			return;
		}
		
		orgName.set(recorder, GP.getChild(1), destination, lines.get(1));
		if (lines.size() < 3) {
			return;
		}
		
		if (lines.size() > 3) {
			final List<String> addressLines = Lists.newArrayList(lines.subList(2, lines.size() - 1));
			final List<PropertyName> from = Lists.newArrayList();
			for (int index = 2; index < lines.size() - 1; index++) {
				from.add(GP.getChild(index));
			}
			
			this.addressLines.set(recorder, from, destination, addressLines);
		}
		
		postcode.set(recorder, GP.getChild(lines.size() - 1), destination, lines.get(lines.size() - 1));
	}
}
