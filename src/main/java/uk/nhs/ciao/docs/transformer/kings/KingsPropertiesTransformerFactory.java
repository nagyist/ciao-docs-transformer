package uk.nhs.ciao.docs.transformer.kings;

import uk.nhs.ciao.docs.parser.transformer.PropertiesTransformer;
import uk.nhs.ciao.docs.parser.transformer.PropertyAppender;

public class KingsPropertiesTransformerFactory {
	public static PropertiesTransformer createWordDischargeNotificationTransformer() {
		// TODO: example property transformation - perhaps these can be configured via a resource or spring etc?
		final PropertiesTransformer transformer = new PropertiesTransformer();
		transformer.renameProperty("Ward", "documentAuthorWorkgroupName");
		transformer.renameProperty("Hospital Number", "patientLocalID");
		transformer.renameProperty("Consultant", "documentAuthorFullName");
		transformer.renameProperty("NHS Number", "patientNHSNo");
		transformer.renameProperty("Patient Name", "patientFullName");
		transformer.splitProperty("D\\.O\\.B", "(\\d{2}/\\d{2}/\\d{4}).*",
				"patientBirthDate");
		transformer.renameProperty("Usual residence", "patientAddressFull");
		transformer.renameProperty("Clinical Narative", "clinicalSummary");
		transformer.splitProperty("Screened by", "(.+) on (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}).*",
				"medicationsPharmacistScreeningAuthorFullName", "medicationsPharmacistScreeningDate");
		transformer.renameProperty("Contact Details", "medicationsPharmacistScreeningAuthorTelephone");
		
		transformer.reformatDateProperty("patientBirthDate", "dd/MM/yyyy", "yyyyMMdd");
		transformer.reformatDateProperty("medicationsPharmacistScreeningDate",
				"yyyy-MM-dd HH:mm:ss.SSS", "yyyyMMddHHmmss.SSS");
		transformer.reformatDateProperty("Date of Admission", "dd/MM/yyyy HH:mm", "dd-MMM-yyyy, HH:mm");
		transformer.reformatDateProperty("Date of Discharge", "dd/MM/yyyy HH:mm", "dd-MMM-yyyy, HH:mm");
		
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
		
		transformer.nestedTransformer("allergens").combineProperties(
				new PropertyAppender("allergies"), "Allergen", "Reaction", "Comments");
		
		transformer.nestedTransformer("dischargeMedication").combineProperties(
				new PropertyAppender("medications"), "Medication", "Status", "Supply", "Pharmacy");
		
		transformer.addTransformation(new GPPropertiesTransformation());
		
		return transformer;
	}
}
