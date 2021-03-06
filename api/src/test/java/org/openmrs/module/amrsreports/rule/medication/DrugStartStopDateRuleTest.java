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
import org.openmrs.module.amrsreports.cache.MohCacheUtils;
import org.openmrs.module.amrsreports.service.MohCoreService;
import org.openmrs.module.amrsreports.util.MOHReportUtil;
import org.openmrs.module.amrsreports.util.MohFetchRestriction;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Test class for DrugStartStopDateRule
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class DrugStartStopDateRuleTest {

	private static final String START_CONCEPT = "startConcepts";
	private static final String STOP_CONCEPT = "stopConcept";

	private static final int PATIENT_ID = 5;

	private ConceptService conceptService;
	private MohCoreService mohCoreService;

	private DrugStartStopDateRule rule;

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
		Mockito.when(conceptService.getConcept(START_CONCEPT)).thenReturn(new Concept(i++));
		Mockito.when(conceptService.getConcept(STOP_CONCEPT)).thenReturn(new Concept(i++));

		// build the MOH Core service
		mohCoreService = Mockito.mock(MohCoreService.class);

		Map<String, Collection<OpenmrsObject>> startRestrictions = new HashMap<String, Collection<OpenmrsObject>>();
		startRestrictions.put("concept", Arrays.<OpenmrsObject>asList(new Concept[]{conceptService.getConcept(START_CONCEPT)}));

		Map<String, Collection<OpenmrsObject>> stopRestrictions = new HashMap<String, Collection<OpenmrsObject>>();
		stopRestrictions.put("concept", Arrays.<OpenmrsObject>asList(new Concept[]{conceptService.getConcept(STOP_CONCEPT)}));

		Mockito.when(mohCoreService.getPatientObservations(
				Mockito.eq(PATIENT_ID), Mockito.eq(startRestrictions), Mockito.any(MohFetchRestriction.class),
				Mockito.any(Date.class))).thenReturn(currentStartObs);

		Mockito.when(mohCoreService.getPatientObservations(
				Mockito.eq(PATIENT_ID), Mockito.eq(stopRestrictions), Mockito.any(MohFetchRestriction.class),
				Mockito.any(Date.class))).thenReturn(currentStopObs);

		// set up Context
		PowerMockito.mockStatic(Context.class);
		Mockito.when(Context.getConceptService()).thenReturn(conceptService);
		Mockito.when(Context.getService(MohCoreService.class)).thenReturn(mohCoreService);

		// create a rule instance
		rule = new TestDrugStartStopDateRule();
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
		} catch(Exception e){
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
	private void addStartObs(String date) {
		Obs obs = new Obs();
		obs.setConcept(conceptService.getConcept(START_CONCEPT));
		obs.setObsDatetime(makeDate(date));
		currentStartObs.add(obs);
	}

	/**
	 * adds a stop observation with the given date as the obs datetime
	 *
	 * @param conceptName
	 * @param date
	 */
	private void addStopObs(String date) {
		Obs obs = new Obs();
		obs.setConcept(conceptService.getConcept(STOP_CONCEPT));
		obs.setObsDatetime(makeDate(date));
		currentStopObs.add(obs);
	}

	/**
	 * @verifies return blank result for no dates found
	 * @see DrugStartStopDateRule#getResult(Integer)
	 */
	@Test
	public void getResult_shouldReturnBlankResultForNoDatesFound() throws Exception {
		Assert.assertEquals(new Result(""), rule.getResult(PATIENT_ID, new Date()));
	}

	/**
	 * @verifies properly format a single start date
	 * @see DrugStartStopDateRule#getResult(Integer)
	 */
	@Test
	public void getResult_shouldProperlyFormatASingleStartDate() throws Exception {
		addStartObs("16 Oct 1975");
		Assert.assertEquals(new Result("16/10/1975 - Unknown"), rule.getResult(PATIENT_ID, new Date()));
	}

	/**
	 * @verifies properly format a single stop date
	 * @see DrugStartStopDateRule#getResult(Integer)
	 */
	@Test
	public void getResult_shouldProperlyFormatASingleStopDate() throws Exception {
		addStopObs("16 Oct 1975");
		Assert.assertEquals(new Result("Unknown - 16/10/1975"), rule.getResult(PATIENT_ID, new Date()));
	}

	/**
	 * @verifies properly format a start and stop date
	 * @see DrugStartStopDateRule#getResult(Integer)
	 */
	@Test
	public void getResult_shouldProperlyFormatAStartAndStopDate() throws Exception {
		addStartObs("12 Oct 1975");
		addStopObs("16 Oct 1975");
		Assert.assertEquals(new Result("12/10/1975 - 16/10/1975"), rule.getResult(PATIENT_ID, new Date()));
	}

	/**
	 * @verifies properly format two starts followed by one stop
	 * @see DrugStartStopDateRule#getResult(Integer)
	 */
	@Test
	public void getResult_shouldProperlyFormatTwoStartsFollowedByOneStop() throws Exception {
		addStartObs("12 Oct 1975");
		addStartObs("14 Oct 1975");
		addStopObs("16 Oct 1975");

		String expected = MOHReportUtil.joinAsSingleCell(
				"12/10/1975 - Unknown",
				"14/10/1975 - 16/10/1975");

		Assert.assertEquals(new Result(expected), rule.getResult(PATIENT_ID, new Date()));
	}

	/**
	 * @verifies properly format one start followed by two stops
	 * @see DrugStartStopDateRule#getResult(Integer)
	 */
	@Test
	public void getResult_shouldProperlyFormatOneStartFollowedByTwoStops() throws Exception {
		addStartObs("12 Oct 1975");
		addStopObs("14 Oct 1975");
		addStopObs("16 Oct 1975");

		String expected = MOHReportUtil.joinAsSingleCell(
				"12/10/1975 - 14/10/1975",
				"Unknown - 16/10/1975");

		Assert.assertEquals(new Result(expected), rule.getResult(PATIENT_ID, new Date()));
	}

	/**
	 * @verifies properly format two start and stop periods
	 * @see DrugStartStopDateRule#getResult(Integer)
	 */
	@Test
	public void getResult_shouldProperlyFormatTwoStartAndStopPeriods() throws Exception {
		addStartObs("12 Oct 1975");
		addStopObs("14 Oct 1975");
		addStartObs("16 Oct 1975");
		addStopObs("18 Oct 1975");

		String expected = MOHReportUtil.joinAsSingleCell(
				"12/10/1975 - 14/10/1975",
				"16/10/1975 - 18/10/1975");

		Assert.assertEquals(new Result(expected), rule.getResult(PATIENT_ID, new Date()));
	}

	/**
	 * implementation of DrugStartStopDateRule for testing
	 */
	private class TestDrugStartStopDateRule extends DrugStartStopDateRule {

		public String TOKEN = "ACK";

		public TestDrugStartStopDateRule() {
			this.startConcepts = Collections.singletonList((OpenmrsObject) MohCacheUtils.getConcept(START_CONCEPT));
			this.stopConcepts = Collections.singletonList((OpenmrsObject) MohCacheUtils.getConcept(STOP_CONCEPT));
		}

		@Override
		protected Result evaluate(LogicContext context, Integer patientId, Map<String, Object> parameters) {
			return this.getResult(patientId, new Date());
		}

		@Override
		protected boolean validateStartObs(Obs obs) {
			return true;
		}

		@Override
		protected boolean validateStopObs(Obs obs) {
			return true;
		}

		@Override
		protected String getEvaluableToken() {
			return TOKEN;
		}
	}
}
