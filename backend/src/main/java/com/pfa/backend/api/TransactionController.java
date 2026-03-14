package com.pfa.backend.api;

import com.pfa.backend.api.dto.CreateTransactionRequest;
import com.pfa.backend.api.dto.ImportResult;
import com.pfa.backend.domain.Category;
import com.pfa.backend.domain.Transaction;
import com.pfa.backend.domain.TransactionType;
import com.pfa.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@GetMapping
	public List<Transaction> list(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
		@RequestParam(required = false) TransactionType type,
		@RequestParam(required = false) Category category
	) {
		return transactionService.list(start, end, type, category);
	}

	@PostMapping
	public Transaction create(@Valid @RequestBody CreateTransactionRequest req) {
		return transactionService.createManual(req);
	}

	@PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ImportResult importFile(
		@RequestPart("file") MultipartFile file,
		@RequestParam(required = false) String defaultCurrency
	) {
		String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
		if (name.endsWith(".xlsx")) return transactionService.importExcel(file, defaultCurrency);
		return transactionService.importCsv(file, defaultCurrency);
	}
}

