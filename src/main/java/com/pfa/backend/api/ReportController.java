package com.pfa.backend.api;

import com.pfa.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	@GetMapping(value = "/monthly.csv", produces = "text/csv")
	public ResponseEntity<byte[]> monthlyCsv(@RequestParam int year, @RequestParam int month) {
		String csv = reportService.buildMonthlyCsv(year, month);
		String filename = "pfa-report-" + year + "-" + String.format("%02d", month) + ".csv";
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
			.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
			.body(csv.getBytes(StandardCharsets.UTF_8));
	}

	@GetMapping(value = "/monthly.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> monthlyPdf(@RequestParam int year, @RequestParam int month) {
		byte[] pdf = reportService.buildMonthlyPdf(year, month);
		String filename = "pfa-report-" + year + "-" + String.format("%02d", month) + ".pdf";
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
			.contentType(MediaType.APPLICATION_PDF)
			.body(pdf);
	}
}

