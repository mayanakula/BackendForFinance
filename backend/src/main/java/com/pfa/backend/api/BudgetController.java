package com.pfa.backend.api;

import com.pfa.backend.api.dto.BudgetStatus;
import com.pfa.backend.api.dto.UpsertBudgetRequest;
import com.pfa.backend.domain.Budget;
import com.pfa.backend.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

	private final BudgetService budgetService;

	@PostMapping
	public Budget upsert(@Valid @RequestBody UpsertBudgetRequest req) {
		return budgetService.upsert(req);
	}

	@GetMapping
	public List<Budget> list(@RequestParam int year, @RequestParam int month) {
		return budgetService.list(year, month);
	}

	@GetMapping("/status")
	public List<BudgetStatus> status(@RequestParam int year, @RequestParam int month) {
		return budgetService.status(year, month);
	}
}

