package org.openmrs.module.amrsreports.rule.medication;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicException;
import org.openmrs.logic.result.Result;
import org.openmrs.logic.result.Result.Datatype;
import org.openmrs.logic.rule.RuleParameterInfo;
import org.openmrs.module.amrsreports.rule.MohEvaluableRule;
import org.openmrs.module.amrsreports.service.MohCoreService;
import org.openmrs.module.amrsreports.util.MOHReportUtil;
import org.openmrs.module.amrsreports.util.MohFetchOrdering;
import org.openmrs.module.amrsreports.util.MohFetchRestriction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class DrugStartStopDateRule extends MohEvaluableRule {

	private static final Log log = LogFactory.getLog(DrugStartStopDateRule.class);

	// decides if we should look at values versus obs for datetime
	private boolean valueBased = false;

	// Lists of concepts and possible answers to be used in comparison as for start and stop dates
	protected List<OpenmrsObject> startConcepts = null;
	protected List<OpenmrsObject> stopConcepts = null;

	/**
	 * returns a list of date ranges based on start and stop concepts for a given patient
	 *
	 * @param patientId the patient for evaluation
	 * @return formatted list of start and stop dates
	 * @throws LogicException
	 * @should return blank result for no dates found
	 * @should properly format a single start date
	 * @should properly format a single stop date
	 * @should properly format a start and stop date
	 * @should properly format two starts followed by one stop
	 * @should properly format one start followed by two stops
	 * @should properly format two start and stop periods
	 */
	protected Result getResult(final Integer patientId, Date evaluationDate) throws LogicException {

		// set up query for observations in order by ascending date
		Map<String, Collection<OpenmrsObject>> restrictions = new HashMap<String, Collection<OpenmrsObject>>();
		restrictions.put("concept", startConcepts);
		MohFetchRestriction fetchRestriction = new MohFetchRestriction();
		fetchRestriction.setFetchOrdering(MohFetchOrdering.ORDER_ASCENDING);

		// get the start observations
		List<Obs> startObservations = Context.getService(MohCoreService.class).getPatientObservations(patientId, restrictions, fetchRestriction, evaluationDate);

		// get the stop observations
		restrictions.put("concept", stopConcepts);
		List<Obs> stopObservations = Context.getService(MohCoreService.class).getPatientObservations(patientId, restrictions, fetchRestriction, evaluationDate);

		return buildResultFromObservations(startObservations, stopObservations);
	}

	/**
	 * validates the start observation, possibly evaluating the answer against a list of expected questions
	 *
	 * @param obs the observation to be validated
	 * @return whether the observation is valid
	 */
	protected abstract boolean validateStartObs(Obs obs);

	/**
	 * validates the stop observation, possibly evaluating the answer against a list of expected questions
	 *
	 * @param obs the observation to be validated
	 * @return whether the observation is valid
	 */
	protected abstract boolean validateStopObs(Obs obs);

	/**
	 * Sanitize observations from duplicate
	 *
	 * @param observations list of observations with duplicates.
	 * @return obs without duplicates.
	 */
	private List<Obs> sanitize(final List<Obs> observations) {
		List<Obs> sanitized = new ArrayList<Obs>();
		int counter = 0;
		Obs processedObs = null;
		while (counter < observations.size()) {
			Obs obs = observations.get(counter++);
			if (processedObs != null) {
				if (this.isValueBased()) {
					if (DateUtils.isSameDay(processedObs.getValueDatetime(), obs.getValueDatetime()))
						continue;
				} else {
					if (DateUtils.isSameDay(processedObs.getObsDatetime(), obs.getObsDatetime()))
						continue;
				}
			}
			processedObs = obs;
			sanitized.add(obs);
		}
		return sanitized;
	}

	/**
	 * creates a Result from two lists, one with start observations and the second with stop observations.
	 *
	 * @param inputStarters
	 * @param inputStoppers
	 * @return
	 */
	protected Result buildResultFromObservations(List<Obs> inputStarters, List<Obs> inputStoppers) {
		List<Obs> starters = sanitize(inputStarters);
		List<Obs> stoppers = sanitize(inputStoppers);

		Iterator<Obs> startObs = starters.iterator();
		Iterator<Obs> stopObs = stoppers.iterator();
		Obs currentStartObs = startObs.hasNext() ? startObs.next() : null;
		Obs currentStopObs = stopObs.hasNext() ? stopObs.next() : null;

		List<Date[]> ranges = new ArrayList<Date[]>();
		Date currentStartDate;
		Date currentStopDate;

		while (currentStartObs != null || currentStopObs != null) {

			// validate observations
			while (currentStartObs != null && !validateStartObs(currentStartObs)) {
				currentStartObs = startObs.hasNext() ? startObs.next() : null;
			}
			while (currentStopObs != null && !validateStopObs(currentStopObs)) {
				currentStopObs = stopObs.hasNext() ? stopObs.next() : null;
			}

			// set dates
			if (this.isValueBased()) {
				currentStartDate = currentStartObs == null ? null : currentStartObs.getValueDatetime();
				currentStopDate = currentStopObs == null ? null : currentStopObs.getValueDatetime();
			} else {
				currentStartDate = currentStartObs == null ? null : currentStartObs.getObsDatetime();
				currentStopDate = currentStopObs == null ? null : currentStopObs.getObsDatetime();
			}

			// decide where to go next
			if (currentStopObs != null) {

				// we are dealing with a real stop date
				if (currentStartObs == null || currentStopDate.before(currentStartDate)) {

					// start date is either empty or after the stop date
					ranges.add(new Date[]{null, currentStopDate});
					currentStopObs = stopObs.hasNext() ? stopObs.next() : null;

				} else if (currentStartObs != null) {

					// start date is after start date and is not null
					Obs nextStartObs = startObs.hasNext() ? startObs.next() : null;
					Date nextStartDate = null;

					if (this.isValueBased()) {
						nextStartDate = nextStartObs == null ? null : nextStartObs.getValueDatetime();
					} else {
						nextStartDate = nextStartObs == null ? null : nextStartObs.getObsDatetime();
					}

					if (nextStartDate != null && currentStopDate.after(nextStartDate)) {

						// next start date exists and is after stop date
						ranges.add(new Date[]{currentStartDate, null});

					} else {

						// current start date and stop date are good
						ranges.add(new Date[]{currentStartDate, currentStopDate});
						currentStopObs = stopObs.hasNext() ? stopObs.next() : null;
					}

					currentStartObs = nextStartObs;
				}
			} else if (currentStartObs != null) {

				// no more stop dates
				ranges.add(new Date[]{currentStartDate, null});
				currentStartObs = startObs.hasNext() ? startObs.next() : null;
			}
		}

		// build the response
		List<String> results = new ArrayList<String>();
		for (Date[] range : ranges) {
			results.add(MOHReportUtil.formatdates(range[0]) + " - " + MOHReportUtil.formatdates(range[1]));
		}

		return new Result(MOHReportUtil.joinAsSingleCell(results));
	}

	public boolean isValueBased() {
		return valueBased;
	}

	public void setValueBased(boolean valueBased) {
		this.valueBased = valueBased;
	}

	//return the tokens
	protected abstract String getEvaluableToken();

	/**
	 * @see org.openmrs.logic.Rule#getDependencies()
	 */
	//@Override
	public String[] getDependencies() {
		return new String[]{};
	}

	@Override
	public Datatype getDefaultDatatype() {
		return Datatype.TEXT;
	}

	public Set<RuleParameterInfo> getParameterList() {
		return null;
	}

	@Override
	public int getTTL() {
		return 0;
	}
}