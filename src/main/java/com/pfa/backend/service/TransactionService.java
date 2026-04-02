package com.pfa.backend.service;

import com.opencsv.CSVReader;
import com.pfa.backend.api.dto.CreateTransactionRequest;
import com.pfa.backend.api.dto.ImportResult;
import com.pfa.backend.domain.Category;
import com.pfa.backend.domain.Transaction;
import com.pfa.backend.domain.TransactionType;
import com.pfa.backend.repo.TransactionRepository;
import com.pfa.backend.util.Hashing;
import com.pfa.backend.util.Parsing;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

	private final TransactionRepository transactionRepository;
	private final MerchantExtractionService merchantExtractionService;
	private final CategorizationService categorizationService;

	public List<Transaction> list(LocalDate start, LocalDate end, TransactionType type, Category category) {
		LocalDate s = start == null ? LocalDate.of(1970, 1, 1) : start;
		LocalDate e = end == null ? LocalDate.of(2999, 12, 31) : end;
		return transactionRepository.findFiltered(s, e, type, category);
	}

	@Transactional
	public Transaction createManual(CreateTransactionRequest req) {
		Transaction t = new Transaction();
		t.setTxnDate(req.txnDate());
		t.setDescription(Parsing.normalizeDescription(req.description()));
		t.setMerchant(req.merchant() == null ? merchantExtractionService.extractMerchant(req.description()) : req.merchant());
		t.setAmount(req.amount());
		t.setCurrency(req.currency().trim().toUpperCase(Locale.ROOT));
		t.setType(req.type());
		t.setCategory(req.category());
		t.setDedupeHash(dedupeHash(t.getTxnDate(), t.getDescription(), t.getAmount(), t.getCurrency()));
		return transactionRepository.save(t);
	}

	@Transactional
	public ImportResult importCsv(MultipartFile file, String defaultCurrency) {
		int processed = 0, inserted = 0, dupes = 0, failed = 0;
		String currency = (defaultCurrency == null || defaultCurrency.isBlank()) ? "USD" : defaultCurrency.trim().toUpperCase(Locale.ROOT);
		try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			List<String[]> rows = reader.readAll();
			if (rows.isEmpty()) return new ImportResult(0, 0, 0, 0);

			Map<String, Integer> header = headerIndex(rows.get(0));
			for (int i = 1; i < rows.size(); i++) {
				processed++;
				try {
					String[] r = rows.get(i);
					Transaction t = fromRow(
						get(r, header, "date", "transaction date", "posted date"),
						get(r, header, "description", "details", "narration", "merchant"),
						get(r, header, "amount", "transaction amount"),
						get(r, header, "debit"),
						get(r, header, "credit"),
						get(r, header, "currency"),
						currency
					);
					if (t == null) continue;
					if (transactionRepository.findByDedupeHash(t.getDedupeHash()).isPresent()) {
						dupes++;
						continue;
					}
					transactionRepository.save(t);
					inserted++;
				} catch (Exception ex) {
					failed++;
				}
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to import CSV: " + e.getMessage(), e);
		}
		return new ImportResult(processed, inserted, dupes, failed);
	}

	@Transactional
	public ImportResult importExcel(MultipartFile file, String defaultCurrency) {
		int processed = 0, inserted = 0, dupes = 0, failed = 0;
		String currency = (defaultCurrency == null || defaultCurrency.isBlank()) ? "USD" : defaultCurrency.trim().toUpperCase(Locale.ROOT);
		try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> it = sheet.rowIterator();
			if (!it.hasNext()) return new ImportResult(0, 0, 0, 0);
			Row headerRow = it.next();
			Map<String, Integer> header = headerIndex(rowToStrings(headerRow));

			while (it.hasNext()) {
				processed++;
				Row row = it.next();
				try {
					List<String> cells = rowToStrings(row);
					Transaction t = fromRow(
						get(cells, header, "date", "transaction date", "posted date"),
						get(cells, header, "description", "details", "narration", "merchant"),
						get(cells, header, "amount", "transaction amount"),
						get(cells, header, "debit"),
						get(cells, header, "credit"),
						get(cells, header, "currency"),
						currency
					);
					if (t == null) continue;
					if (transactionRepository.findByDedupeHash(t.getDedupeHash()).isPresent()) {
						dupes++;
						continue;
					}
					transactionRepository.save(t);
					inserted++;
				} catch (Exception ex) {
					failed++;
				}
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to import Excel: " + e.getMessage(), e);
		}
		return new ImportResult(processed, inserted, dupes, failed);
	}

	private Transaction fromRow(
		String rawDate,
		String rawDescription,
		String rawAmount,
		String rawDebit,
		String rawCredit,
		String rawCurrency,
		String fallbackCurrency
	) {
		LocalDate date = Parsing.parseDateLenient(rawDate);
		String description = Parsing.normalizeDescription(rawDescription);
		if (date == null || description.isBlank()) return null;

		BigDecimal amount = null;
		if (rawAmount != null && !rawAmount.isBlank()) {
			amount = Parsing.parseAmountLenient(rawAmount);
		} else {
			BigDecimal debit = (rawDebit == null || rawDebit.isBlank()) ? BigDecimal.ZERO : Parsing.parseAmountLenient(rawDebit).abs();
			BigDecimal credit = (rawCredit == null || rawCredit.isBlank()) ? BigDecimal.ZERO : Parsing.parseAmountLenient(rawCredit).abs();
			amount = credit.subtract(debit);
		}

		String currency = (rawCurrency == null || rawCurrency.isBlank()) ? fallbackCurrency : rawCurrency.trim().toUpperCase(Locale.ROOT);
		TransactionType type = amount.signum() >= 0 ? TransactionType.INCOME : TransactionType.EXPENSE;
		BigDecimal normalizedAmount = amount.abs();

		String merchant = merchantExtractionService.extractMerchant(description);
		Category category = categorizationService.categorize(merchant, description);

		Transaction t = new Transaction();
		t.setTxnDate(date);
		t.setDescription(description);
		t.setMerchant(merchant);
		t.setAmount(normalizedAmount);
		t.setCurrency(currency);
		t.setType(type);
		t.setCategory(category);
		t.setDedupeHash(dedupeHash(date, description, normalizedAmount, currency));
		return t;
	}

	private String dedupeHash(LocalDate date, String description, BigDecimal amount, String currency) {
		String key = date + "|" + description.toLowerCase(Locale.ROOT) + "|" + amount + "|" + currency;
		return Hashing.sha256Hex(key);
	}

	private Map<String, Integer> headerIndex(String[] header) {
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < header.length; i++) {
			String k = header[i] == null ? "" : header[i].trim().toLowerCase(Locale.ROOT);
			if (!k.isBlank()) map.put(k, i);
		}
		return map;
	}

	private Map<String, Integer> headerIndex(List<String> header) {
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < header.size(); i++) {
			String k = header.get(i) == null ? "" : header.get(i).trim().toLowerCase(Locale.ROOT);
			if (!k.isBlank()) map.put(k, i);
		}
		return map;
	}

	private String get(String[] row, Map<String, Integer> header, String... keys) {
		for (String k : keys) {
			Integer idx = header.get(k);
			if (idx != null && idx >= 0 && idx < row.length) return row[idx];
		}
		return null;
	}

	private String get(List<String> row, Map<String, Integer> header, String... keys) {
		for (String k : keys) {
			Integer idx = header.get(k);
			if (idx != null && idx >= 0 && idx < row.size()) return row.get(idx);
		}
		return null;
	}

	private List<String> rowToStrings(Row row) {
		int max = row.getLastCellNum();
		List<String> out = new java.util.ArrayList<>(Math.max(0, max));
		for (int i = 0; i < max; i++) {
			Cell c = row.getCell(i);
			if (c == null) out.add(null);
			else out.add(switch (c.getCellType()) {
				case STRING -> c.getStringCellValue();
				case NUMERIC -> String.valueOf(c.getNumericCellValue());
				case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
				default -> c.toString();
			});
		}
		return out;
	}
}

