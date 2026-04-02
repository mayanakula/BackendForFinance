package com.pfa.backend.api;

import com.pfa.backend.api.dto.CreateGoalRequest;
import com.pfa.backend.api.dto.UpdateGoalSavedAmountRequest;
import com.pfa.backend.domain.Goal;
import com.pfa.backend.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

	private final GoalService goalService;

	@GetMapping
	public List<Goal> list() {
		return goalService.list();
	}

	@PostMapping
	public Goal create(@Valid @RequestBody CreateGoalRequest req) {
		return goalService.create(req);
	}

	@PatchMapping("/{id}/saved")
	public Goal updateSaved(@PathVariable Long id, @Valid @RequestBody UpdateGoalSavedAmountRequest req) {
		return goalService.updateSaved(id, req);
	}
}

