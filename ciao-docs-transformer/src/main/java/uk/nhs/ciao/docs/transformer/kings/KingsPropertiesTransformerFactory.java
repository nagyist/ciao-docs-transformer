package uk.nhs.ciao.docs.transformer.kings;

import uk.nhs.ciao.docs.transformer.PropertiesTransformer;
import uk.nhs.ciao.docs.transformer.PropertyAppender;

/**
 * Factory to create {@link PropertiesTransformer}s for Kings documents
 */
public class KingsPropertiesTransformerFactory {
	/**
	 * Transforms properties previously extracted from a Kings word document to use names
	 * suitable for building into a CDA document
	 */
	public static PropertiesTransformer createWordDischargeNotificationTransformer() {
		/*
		 * The top-level properties transformer provides a DSL for creating other types
		 * of transformation. The transformations are performed in series.
		 */
		final PropertiesTransformer transformer = new PropertiesTransformer();
		
		/*
		 * Rename property moves/copies a property value to a new property name
		 * By default, the original property is retained
		 */
		transformer.renameProperty("Ward", "documentAuthorWorkgroupName");
		transformer.renameProperty("Hospital Number", "patientLocalID");
		transformer.renameProperty("Consultant", "documentAuthorFullName");
		transformer.renameProperty("NHS Number", "patientNHSNo");
		transformer.renameProperty("Patient Name", "patientFullName");
		
		/*
		 * Split property uses a regex to split a property value into one or more
		 * capturing groups. The captured values are used to set one or more
		 * properties. The specified property names should match the regex group count.
		 * In this case, a single group is captured and assigned to one property name
		 */
		transformer.splitProperty("D\\.O\\.B", "(\\d{2}/\\d{2}/\\d{4}).*",
				"patientBirthDate");
		transformer.renameProperty("Usual residence", "patientAddressFull");
		transformer.renameProperty("Clinical Narative", "clinicalSummary");
		transformer.splitProperty("Screened by", "(.+) on (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}).*",
				"medicationsPharmacistScreeningAuthorFullName", "medicationsPharmacistScreeningDate");
		transformer.renameProperty("Contact Details", "medicationsPharmacistScreeningAuthorTelephone");
		
		/*
		 * Reformat date property checks that a property value matches a known input format
		 * and converts it to the specified output date format. If the input format does not
		 * match, the value is not changed
		 */
		transformer.reformatDateProperty("patientBirthDate", "dd/MM/yyyy", "yyyyMMdd");
		transformer.reformatDateProperty("medicationsPharmacistScreeningDate",
				"yyyy-MM-dd HH:mm:ss.SSS", "yyyyMMddHHmmss.SSS");
		transformer.reformatDateProperty("Date of Admission", "dd/MM/yyyy HH:mm", "dd-MMM-yyyy, HH:mm");
		transformer.reformatDateProperty("Date of Discharge", "dd/MM/yyyy HH:mm", "dd-MMM-yyyy, HH:mm");
		
		/*
		 * Combine properties combines a set of named properties into a single output property. The default
		 * output format is encoded HTML. 
		 */
		transformer.combineProperties("admissionDetails",
				"Reason For Admission", "Self Discharge", "Date of Admission", "Method of admission", "Source of admission");
		
		transformer.combineProperties("dischargeDetails",
				"Ward", "Consultant", "Specialty", "Date of Discharge", "Discharge Address", "Discharge Status",
				"Discharged by");

		transformer.combineProperties("plan", 
				"Recommended further mgmt/action by GP", "Pharmacy recommendations", "External Referral",
				"Next appointment", "Consultant follow up");
		
		transformer.combineProperties("diagnoses",
				"Main Diagnosis", "Other Diagnosis", "Other");
		
		transformer.combineProperties("procedures",
				"Procedure(s)/Operation(s)", "Operation notes");
		
		transformer.combineProperties("investigations",
				"Laboratory", "Radiology", "Future tests/procedure booked");
		
		/*
		 * A standard properties transformer operates at the root of the properties map
		 * A nested transformer is rooted at some nested property
		 */
		transformer.nestedTransformer("allergens").combineProperties(
				new PropertyAppender("allergies"), "Allergen", "Reaction", "Comments");
		
		transformer.nestedTransformer("dischargeMedication").combineProperties(
				new PropertyAppender("medications"), "Medication", "Status", "Supply", "Pharmacy");
		
		/*
		 * The GP properties does not follow an easily described structure - so a custom
		 * transformation class has been coded to handle this 
		 */
		transformer.addTransformation(new GPPropertiesTransformation());
		
		return transformer;
	}
}
