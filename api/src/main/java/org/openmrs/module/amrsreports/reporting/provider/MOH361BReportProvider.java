package org.openmrs.module.amrsreports.reporting.provider;

import org.apache.commons.io.IOUtils;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.amrsreports.reporting.cohort.definition.Moh361ACohortDefinition;
import org.openmrs.module.amrsreports.reporting.converter.DecimalAgeConverter;
import org.openmrs.module.amrsreports.reporting.converter.MultiplePatientIdentifierConverter;
import org.openmrs.module.amrsreports.reporting.data.AgeAtEvaluationDateDataDefinition;
import org.openmrs.module.amrsreports.reporting.data.DateARTStartedDataDefinition;
import org.openmrs.module.amrsreports.service.MohCoreService;
import org.openmrs.module.amrsreports.util.MOHReportUtil;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.*;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.ReportDesignResource;
import org.openmrs.module.reporting.report.definition.PeriodIndicatorReportDefinition;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.renderer.ExcelTemplateRenderer;
import org.openmrs.util.OpenmrsClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides mechanisms for rendering the MOH 361A Pre-ART Register
 */
public class MOH361BReportProvider implements ReportProvider {

	@Override
	public String getName() {
		return "MOH 361B";
	}

	@Override
	public ReportDefinition getReportDefinition() {

		String nullString = null;
		ObjectFormatter nullStringConverter = new ObjectFormatter();
		MohCoreService service = Context.getService(MohCoreService.class);

		ReportDefinition report = new PeriodIndicatorReportDefinition();
		report.setName("MOH 361B Report");

		// set up the DSD
		PatientDataSetDefinition dsd = new PatientDataSetDefinition();
		dsd.setName("allPatients");

		// set up the columns ...

		// patient id ... until we get this thing working proper
		dsd.addColumn("Person ID", new PersonIdDataDefinition(), nullString);

		// b. Date ART started (Transfer to ART register)
		dsd.addColumn("Date ART Started", new DateARTStartedDataDefinition(), nullString);

		// c. Unique Patient Number
		PatientIdentifierType pit = service.getCCCNumberIdentifierType();
		PatientIdentifierDataDefinition cccColumn = new PatientIdentifierDataDefinition("CCC", pit);
		dsd.addColumn("Unique Patient Number", cccColumn, nullString, new MultiplePatientIdentifierConverter());

		// d. Patient's Name
		dsd.addColumn("Name", new PreferredNameDataDefinition(), nullString);

		// e. Sex
		dsd.addColumn("Sex", new GenderDataDefinition(), nullString);

		// f1. Date of Birth
		dsd.addColumn("Date of Birth", new BirthdateDataDefinition(), nullString, new BirthdateConverter(MOHReportUtil.DATE_FORMAT));

		// f1. Age
		AgeAtEvaluationDateDataDefinition add = new AgeAtEvaluationDateDataDefinition();
		dsd.addColumn("Age", add, nullString, new DecimalAgeConverter(2));

        PreferredAddressDataDefinition padd = new PreferredAddressDataDefinition();
        dsd.addColumn("Address",padd,nullString);

        PersonAttributeType pat = Context.getPersonService().getPersonAttributeTypeByName("PersonPhoneContact");

        PersonAttributeDataDefinition patientPhoneContact = new PersonAttributeDataDefinition(pat);

        dsd.addColumn("Phone Number",patientPhoneContact,nullString);

		report.addDataSetDefinition(dsd, null);

		return report;
	}

	@Override
	public CohortDefinition getCohortDefinition() {
		return new Moh361ACohortDefinition();
	}

	@Override
	public ReportDesign getReportDesign() {
		ReportDesign design = new ReportDesign();
		design.setName("MOH 361B Register Design");
		design.setReportDefinition(this.getReportDefinition());
		design.setRendererType(ExcelTemplateRenderer.class);

		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:4,dataset:allPatients");

		design.setProperties(props);

		ReportDesignResource resource = new ReportDesignResource();
		resource.setName("template.xls");
		InputStream is = OpenmrsClassLoader.getInstance().getResourceAsStream("templates/MOH361AReportTemplate.xls");

		if (is == null)
			throw new APIException("Could not find report template.");

		try {
			resource.setContents(IOUtils.toByteArray(is));
		} catch (IOException ex) {
			throw new APIException("Could not create report design for MOH 361B Register.", ex);
		}

		IOUtils.closeQuietly(is);
		design.addResource(resource);

		return design;
	}
}
