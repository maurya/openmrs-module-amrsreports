package org.openmrs.module.amrsreports.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.amrsreports.QueuedReport;
import org.openmrs.module.amrsreports.service.QueuedReportService;
import org.openmrs.module.amrsreports.util.MOHReportUtil;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * controller for View AMRS Reports page
 */
@Controller
public class MohHistoryController {

	private static final Log log = LogFactory.getLog(MohHistoryController.class);

	@ModelAttribute("queuedReports")
	public List<QueuedReport> getQueuedReports() {
		return Context.getService(QueuedReportService.class).getQueuedReportsWithStatus(QueuedReport.STATUS_NEW);
	}

	@ModelAttribute("runningReports")
	public List<QueuedReport> getRunningReport() {
		return Context.getService(QueuedReportService.class).getQueuedReportsWithStatus(QueuedReport.STATUS_RUNNING);
	}

	@ModelAttribute("errorReports")
	public List<QueuedReport> getErrorReport() {
		return Context.getService(QueuedReportService.class).getQueuedReportsWithStatus(QueuedReport.STATUS_ERROR);
	}

	@ModelAttribute("completeReports")
	public List<QueuedReport> getCompleteReport() {
		return Context.getService(QueuedReportService.class).getQueuedReportsWithStatus(QueuedReport.STATUS_COMPLETE);
	}

	@RequestMapping(method = RequestMethod.GET, value = "module/amrsreports/mohHistory.form")
	public void preparePage() {
		// pass
	}

	@RequestMapping(value = "/module/amrsreports/downloadxls")
	public void downloadXLS(HttpServletResponse response,
	                        @RequestParam(required = true, value = "reportId") Integer reportId) throws IOException {

		if (reportId == null) {
			// TODO say something ...
			return;
		}

		QueuedReport report = Context.getService(QueuedReportService.class).getQueuedReport(reportId);

		if (report == null) {
			// TODO say something ...
			return;
		}

		String folderName = Context.getAdministrationService().getGlobalProperty("amrsreports.file_dir");

		File fileDir = OpenmrsUtil.getDirectoryInApplicationDataDirectory(folderName);
		File amrsFileToDownload = new File(fileDir, report.getXlsFilename());

		response.setContentType("application/vnd.ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=" + report.getXlsFilename());
		response.setContentLength((int) amrsFileToDownload.length());

		FileCopyUtils.copy(new FileInputStream(amrsFileToDownload), response.getOutputStream());
	}

}
