package org.openmrs.module.amrsreports.rule.medication;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicContext;
import org.openmrs.logic.result.Result;
import org.openmrs.module.amrsreports.rule.MohEvaluableNameConstants;
import org.openmrs.module.amrsreports.service.MohCoreService;
import org.openmrs.module.amrsreports.util.MohFetchRestriction;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * test class for {@link MOHCTXStartStopDateRule}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class MOHCTXStartStopDateRuleTest {

	private static final List<String> initConcepts = Arrays.asList(
			MohEvaluableNameConstants.PCP_PROPHYLAXIS_STARTED,
			MohEvaluableNameConstants.CURRENT_MEDICATIONS,
			MohEvaluableNameConstants.PATIENT_REPORTED_CURRENT_PCP_PROPHYLAXIS,
			MohEvaluableNameConstants.REASON_PCP_PROPHYLAXIS_STOPPED,
			MohEvaluableNameConstants.NONE
	);

	private static final int PATIENT_ID = 5;

	private ConceptService conceptService;
	private MohCoreService mohCoreService;

	private MOHCTXStartStopDateRule rule;
	private LogicContext logicContext;

	private List<Obs> currentStartObs;
	private List<Obs> currentStopObs;

	@Before
	public void setup() {

		// initialize the current obs
		currentStartObs = new ArrayList<Obs>();
		currentStopObs = new ArrayList<Obs>();

		// build the concept service
		int i = 0;
		conceptService = Mockito.mock(ConceptService.class);
		for (String conceptName : initConcepts) {
			Mockito.when(conceptService.getConcept(conceptName)).thenReturn(new Concept(i++));
		}
		Mockito.when(conceptService.getConcept((String) null)).thenReturn(null);

		// build the MOH Core service
		mohCoreService = Mockito.mock(MohCoreService.class);

		Map<String, Collection<OpenmrsObject>> startRestrictions = new HashMap<String, Collection<OpenmrsObject>>();
		startRestrictions.put("concept", Arrays.<OpenmrsObject>asList(
				conceptService.getConcept(MohEvaluableNameConstants.PCP_PROPHYLAXIS_STARTED),
				conceptService.getConcept(MohEvaluableNameConstants.CURRENT_MEDICATIONS),
				conceptService.getConcept(MohEvaluableNameConstants.PATIENT_REPORTED_CURRENT_PCP_PROPHYLAXIS)
		));

		Map<String, Collection<OpenmrsObject>> stopRestrictions = new HashMap<String, Collection<OpenmrsObject>>();
		stopRestrictions.put("concept", Arrays.<OpenmrsObject>asList(
				conceptService.getConcept(MohEvaluableNameConstants.REASON_PCP_PROPHYLAXIS_STOPPED)
		));

		Mockito.when(mohCoreService.getPatientObservations(Mockito.eq(PATIENT_ID), Mockito.eq(startRestrictions),
				Mockito.any(MohFetchRestriction.class), Mockito.any(Date.class))).thenReturn(currentStartObs);

		Mockito.when(mohCoreService.getPatientObservations(Mockito.eq(PATIENT_ID), Mockito.eq(stopRestrictions),
				Mockito.any(MohFetchRestriction.class), Mockito.any(Date.class))).thenReturn(currentStopObs);

		// set up Context
		PowerMockito.mockStatic(Context.class);
		Mockito.when(Context.getConceptService()).thenReturn(conceptService);
		Mockito.when(Context.getService(MohCoreService.class)).thenReturn(mohCoreService);

		// create a rule instance
		rule = new MOHCTXStartStopDateRule();

		// initialize logic context
		logicContext = Mockito.mock(LogicContext.class);
		Mockito.when(logicContext.getIndexDate()).thenReturn(new Date());
	}

	/**
	 * generate a date from a string
	 *
	 * @param date
	 * @return
	 */
	private Date makeDate(String date) {
		try {
			return new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).parse(date);
		} catch (Exception e) {
			// pass
		}
		return new Date();
	}

	/**
	 * adds a start observation with the given date as the obs datetime
	 *
	 * @param conceptName
	 * @param date
	 */
	private void addStartObs(String concept, String answer, String date) {
		Obs obs = new Obs();
		obs.setConcept(conceptService.getConcept(concept));
		obs.setValueCoded(conceptService.getConcept(answer));
		obs.setObsDatetime(makeDate(date));
		currentStartObs.add(obs);
	}

	/**
	 * adds a stop observation with the given date as the obs datetime
	 *
	 * @param conceptName
	 * @param date
	 */
	private void addStopObs(String concept, String answer, String date) {
		Obs obs = new Obs();
		obs.setConcept(conceptService.getConcept(concept));
		obs.setValueCoded(conceptService.getConcept(answer));
		obs.setObsDatetime(makeDate(date));
		currentStopObs.add(obs);
	}

	/**
	 * @verifies start on PCP_PROPHYLAXIS_STARTED with not null answer
	 * @see MOHCTXStartStopDateRule#evaluate(org.openmrs.logic.LogicContext, Integer, java.util.Map)
	 */
	@Test
	public void evaluate_shouldStartOnPCP_PROPHYLAXIS_STARTEDWithNotNullAnswer() throws Exception {
		addStartObs(MohEvaluableNameConstants.PCP_PROPHYLAXIS_STARTED, MohEvaluableNameConstants.NONE, "16 Oct 1975");
		Assert.assertEquals(new Result("16/10/1975 - Unknown"), rule.evaluate(logicContext, PATIENT_ID, null));
	}

	/**
	 * @verifies not start on PCP_PROPHYLAXIS_STARTED with null answer
	 * @see MOHCTXStartStopDateRule#evaluate(org.openmrs.logic.LogicContext, Integer, java.util.Map)
	 */
	@Test
	public void evaluate_shouldNotStartOnPCP_PROPHYLAXIS_STARTEDWithNullAnswer() throws Exception {
		addStartObs(MohEvaluableNameConstants.PCP_PROPHYLAXIS_STARTED, null, "17 Oct 1975");
		Assert.assertEquals(new Result(""), rule.evaluate(logicContext, PATIENT_ID, null));
	}

	/**
	 * @verifies stop on REASON_PCP_PROPHYLAXIS_STOPPED with not null answer
	 * @see MOHCTXStartStopDateRule#evaluate(org.openmrs.logic.LogicContext, Integer, java.util.Map)
	 */
	@Test
	public void evaluate_shouldStopOnREASON_PCP_PROPHYLAXIS_STOPPEDWithNotNullAnswer() throws Exception {
		addStopObs(MohEvaluableNameConstants.REASON_PCP_PROPHYLAXIS_STOPPED, MohEvaluableNameConstants.NONE, "18 Oct 1975");
		Assert.assertEquals(new Result("Unknown - 18/10/1975"), rule.evaluate(logicContext, PATIENT_ID, null));
	}

	/**
	 * @verifies not stop on REASON_PCP_PROPHYLAXIS_STOPPED with null answer
	 * @see MOHCTXStartStopDateRule#evaluate(org.openmrs.logic.LogicContext, Integer, java.util.Map)
	 */
	@Test
	public void evaluate_shouldNotStopOnREASON_PCP_PROPHYLAXIS_STOPPEDWithNullAnswer() throws Exception {
		addStopObs(MohEvaluableNameConstants.PCP_PROPHYLAXIS_STARTED, null, "19 Oct 1975");
		Assert.assertEquals(new Result(""), rule.evaluate(logicContext, PATIENT_ID, null));
	}

	/**
	 * @verifies start on CURRENT_MEDICATIONS equal to TRIMETHOPRIM_AND_SULFAMETHOXAZOLE
	 * @see MOHCTXStartStopDateRule#evaluate(org.openmrs.logic.LogicContext, Integer, java.util.Map)
	 */
	@Test
	public void evaluate_shouldStartOnCURRENT_MEDICATIONSEqualToTRIMETHOPRIM_AND_SULFAMETHOXAZOLE() throws Exception {
		addStartObs(MohEvaluableNameConstants.CURRENT_MEDICATIONS, MohEvaluableNameConstants.TRIMETHOPRIM_AND_SULFAMETHOXAZOLE, "20 Oct 1975");
		Assert.assertEquals(new Result("20/10/1975 - Unknown"), rule.evaluate(logicContext, PATIENT_ID, null));
	}

	/**
	 * @verifies not start on CURRENT_MEDICATIONS equal to something other than TRIMETHOPRIM_AND_SULFAMETHOXAZOLE
	 * @see MOHCTXStartStopDateRule#evaluate(org.openmrs.logic.LogicContext, Integer, java.util.Map)
	 */
	@Test
	public void evaluate_shouldNotStartOnCURRENT_MEDICATIONSEqualToSomethingOtherThanTRIMETHOPRIM_AND_SULFAMETHOXAZOLE() throws Exception {
		addStartObs(MohEvaluableNameConstants.CURRENT_MEDICATIONS, MohEvaluableNameConstants.NONE, "20 Oct 1975");
		Assert.assertEquals(new Result(""), rule.evaluate(logicContext, PATIENT_ID, null));
	}

	/**
	 * @verifies start on PATIENT_REPORTED_CURRENT_PCP_PROPHYLAXIS equal to TRIMETHOPRIM_AND_SULFAMETHOXAZOLE
	 * @see MOHCTXStartStopDateRule#evaluate(org.openmrs.logic.LogicContext, Integer, java.util.Map)
	 */
	@Test
	public void evaluate_shouldStartOnPATIENT_REPORTED_CURRENT_PCP_PROPHYLAXISEqualToTRIMETHOPRIM_AND_SULFAMETHOXAZOLE() throws Exception {
		addStartObs(MohEvaluableNameConstants.PATIENT_REPORTED_CURRENT_PCP_PROPHYLAXIS, MohEvaluableNameConstants.TRIMETHOPRIM_AND_SULFAMETHOXAZOLE, "21 Oct 1975");
		Assert.assertEquals(new Result("21/10/1975 - Unknown"), rule.evaluate(logicContext, PATIENT_ID, null));
	}

	/**
	 * @verifies not start on PATIENT_REPORTED_CURRENT_PCP_PROPHYLAXIS equal to something other than
	 * TRIMETHOPRIM_AND_SULFAMETHOXAZOLE
	 * @see MOHCTXStartStopDateRule#evaluate(org.openmrs.logic.LogicContext, Integer, java.util.Map)
	 */
	@Test
	public void evaluate_shouldNotStartOnPATIENT_REPORTED_CURRENT_PCP_PROPHYLAXISEqualToSomethingOtherThanTRIMETHOPRIM_AND_SULFAMETHOXAZOLE() throws Exception {
		addStartObs(MohEvaluableNameConstants.PATIENT_REPORTED_CURRENT_PCP_PROPHYLAXIS, MohEvaluableNameConstants.NONE, "21 Oct 1975");
		Assert.assertEquals(new Result(""), rule.evaluate(logicContext, PATIENT_ID, null));
	}
}
