package com.pfa.backend.service;

import com.pfa.backend.api.dto.CreateGoalRequest;
import com.pfa.backend.api.dto.UpdateGoalSavedAmountRequest;
import com.pfa.backend.domain.Goal;
import com.pfa.backend.repo.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

	private final GoalRepository goalRepository;

	public List<Goal> list() {
		return goalRepository.findAll();
	}

	@Transactional
	public Goal create(CreateGoalRequest req) {
		Goal g = new Goal();
		g.setName(req.name().trim());
		g.setTargetAmount(req.targetAmount());
		g.setSavedAmount(req.savedAmount() == null ? BigDecimal.ZERO : req.savedAmount());
		g.setTargetDate(req.targetDate());
		return goalRepository.save(g);
	}

	@Transactional
	public Goal updateSaved(Long id, UpdateGoalSavedAmountRequest req) {
		Goal g = goalRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Goal not found"));
		g.setSavedAmount(req.savedAmount());
		return goalRepository.save(g);
	}
}

