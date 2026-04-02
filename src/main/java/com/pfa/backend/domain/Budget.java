package com.pfa.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(
	name = "budgets",
	indexes = {
		@Index(name = "idx_budget_year_month", columnList = "budgetYear,budgetMonth"),
		@Index(name = "idx_budget_category", columnList = "category")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_budget_year_month_category", columnNames = {"budgetYear", "budgetMonth", "category"})
	}
)
public class Budget {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "budgetYear", nullable = false)
	private int year;

	@Column(name = "budgetMonth", nullable = false)
	private int month; // 1-12

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private Category category;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal monthlyLimit;
}

