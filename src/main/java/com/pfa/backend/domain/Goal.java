package com.pfa.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "goals", indexes = {
	@Index(name = "idx_goal_target_date", columnList = "targetDate")
})
public class Goal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal targetAmount;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal savedAmount = BigDecimal.ZERO;

	private LocalDate targetDate;
}

