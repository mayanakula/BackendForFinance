package com.pfa.backend.service;

import com.pfa.backend.domain.Transaction;
import com.pfa.backend.domain.TransactionType;
import com.pfa.backend.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

	private final TransactionRepository transactionRepository;

	public String buildMonthlyCsv(int year, int month) {
		YearMonth ym = YearMonth.of(year, month);
		LocalDate start = ym.atDay(1);
		LocalDate end = ym.atEndOfMonth();
		List<Transaction> txns = transactionRepository.findFiltered(start, end, null, null);

		StringBuilder sb = new StringBuilder();
		sb.append("date,description,merchant,type,category,amount,currency\n");
		for (Transaction t : txns) {
			sb.append(t.getTxnDate()).append(',')
				.append(csv(t.getDescription())).append(',')
				.append(csv(t.getMerchant())).append(',')
				.append(t.getType()).append(',')
				.append(t.getCategory()).append(',')
				.append(t.getAmount()).append(',')
				.append(t.getCurrency())
				.append('\n');
		}
		return sb.toString();
	}

	public byte[] buildMonthlyPdf(int year, int month) {
		YearMonth ym = YearMonth.of(year, month);
		LocalDate start = ym.atDay(1);
		LocalDate end = ym.atEndOfMonth();
		List<Transaction> txns = transactionRepository.findFiltered(start, end, null, null);

		BigDecimal income = txns.stream().filter(t -> t.getType() == TransactionType.INCOME).map(Transaction::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal expense = txns.stream().filter(t -> t.getType() == TransactionType.EXPENSE).map(Transaction::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal net = income.subtract(expense);

		try (PDDocument doc = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.LETTER);
			doc.addPage(page);
			try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
				PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
				PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

				cs.beginText();
				cs.setFont(fontBold, 16);
				cs.newLineAtOffset(50, 740);
				cs.showText("Personal Finance Analyzer - Monthly Report");
				cs.newLineAtOffset(0, -22);
				cs.setFont(fontRegular, 12);
				cs.showText("Month: " + ym);
				cs.newLineAtOffset(0, -18);
				cs.showText("Income: " + income);
				cs.newLineAtOffset(0, -16);
				cs.showText("Expense: " + expense);
				cs.newLineAtOffset(0, -16);
				cs.showText("Net: " + net);
				cs.newLineAtOffset(0, -24);
				cs.showText("Transactions: " + txns.size());
				cs.endText();
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			doc.save(baos);
			return baos.toByteArray();
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to generate PDF: " + e.getMessage(), e);
		}
	}

	private String csv(String s) {
		if (s == null) return "";
		String v = s.replace("\"", "\"\"");
		if (v.contains(",") || v.contains("\n")) return "\"" + v + "\"";
		return v;
	}
}

