package org.openmrs.module.amrsreports.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.amrsreports.MOHFacility;
import org.openmrs.module.amrsreports.QueuedReport;
import org.openmrs.module.amrsreports.service.MOHFacilityService;
import org.openmrs.module.amrsreports.service.MohCoreService;
import org.openmrs.module.amrsreports.service.QueuedReportService;
import org.openmrs.module.amrsreports.service.ReportProviderRegistrar;
import org.openmrs.module.amrsreports.service.UserFacilityService;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * controller for Run AMRS Reports page
 */
@Controller
public class MohRenderController {

	private static final Log log = LogFactory.getLog(MohRenderController.class);

	private static final String SUCCESS_VIEW = "redirect:mohHistory.form";

	@ModelAttribute("queuedReports")
	public List<QueuedReport> getQueuedReports() {
		return Context.getService(QueuedReportService.class).getQueuedReportsWithStatus(QueuedReport.STATUS_NEW);
	}

	@ModelAttribute("currentReport")
	public List<QueuedReport> getCurrentReport() {
		return Context.getService(QueuedReportService.class).getQueuedReportsWithStatus(QueuedReport.STATUS_RUNNING);
	}

	@ModelAttribute("facilities")
	public List<MOHFacility> getFacilities() {
		return Context.getService(UserFacilityService.class).getAllowedFacilitiesForUser(Context.getAuthenticatedUser());
	}

	@ModelAttribute("reportNames")
	public Set<String> getReportNames() {
		return ReportProviderRegistrar.getInstance().getAllReportProviderNames();
	}

	@RequestMapping(method = RequestMethod.GET, value = "module/amrsreports/mohRender.form")
	public void preparePage() {
		// pass
	}

	@RequestMapping(method = RequestMethod.POST, value = "module/amrsreports/mohRender.form")
	public void processForm(HttpServletRequest request,
	                        @RequestParam(value = "immediate", required = false) Boolean immediate,
	                        @RequestParam("reportDate") Date reportDate,
	                        @RequestParam("dateScheduled") Date dateScheduled,
	                        @RequestParam("facility") Integer facilityId,
	                        @RequestParam("reportName") String reportName) throws Exception {

		// find the facility
		MOHFacility facility = Context.getService(MOHFacilityService.class).getFacility(facilityId);

		// create a queued report
		QueuedReport queuedReport = new QueuedReport();
		queuedReport.setFacility(facility);
		queuedReport.setReportName(reportName);
		queuedReport.setEvaluationDate(reportDate);
		if (immediate == null)
			queuedReport.setDateScheduled(dateScheduled);
		else
			queuedReport.setDateScheduled(new Date());

		// save it
		QueuedReportService queuedReportService = Context.getService(QueuedReportService.class);
		queuedReportService.saveQueuedReport(queuedReport);

		// kindly respond
		HttpSession httpSession = request.getSession();
		httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Report queued for processing.");

//		return SUCCESS_VIEW;
	}

}
