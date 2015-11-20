package uk.nhs.ciao.docs.transformer.kent;

import uk.nhs.ciao.docs.transformer.PropertiesTransformer;

/**
 * Factory to create {@link PropertiesTransformer}s capable of
 * transforming properties from Kent HTML documents.
 */
public class KentPropertiesTransformerFactory {
	private KentPropertiesTransformerFactory() {
		// Suppress default constructor
	}
	
	/**
	 * Properties transformer for the electronic discharge notification HTML documents
	 */
	public static PropertiesTransformer createEDNTransformer() {
		final PropertiesTransformer transformer = new PropertiesTransformer();
		transformer.splitListProperty("hospitalName", "\\r?\\n", "hospitalName");
		transformer.splitListProperty("Address", " *\\r?\\n *", "Address");
		transformer.splitProperty("gpName", "Dear (.+)", "gpName");
		
		transformer.splitProperty("dischargeSummary", "This patient was an? (.+) under the care of (.+)(?: \\(Specialty: (.+)\\)) on (.+) at (.+) on (.+). The patient was discharged on (.+?)\\s*.",
				"patientType", "doctorName", "doctorSpeciality", "ward", "hospital", "admissionDate", "dischargeDate");
		
		transformer.splitProperty("NHS No\\.", "([\\d ]*\\d)(?: \\(.*)?", "nhsNumber");
		transformer.splitProperty("NHS No\\.", ".*\\((.+)\\)\\s*", "nhsNumberVerification");
		
		transformer.findAndFormatDateProperties("dd/MM/yyyy", "yyyy-MM-dd");
		transformer.findAndFormatDateProperties("dd/MM/yyyy HH:mm", "yyyy-MM-dd HH:mm");
		transformer.findAndFormatDateProperties("dd/MM/yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss");

		return transformer;
	}
}
