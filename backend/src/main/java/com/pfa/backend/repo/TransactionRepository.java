package com.pfa.backend.repo;

import com.pfa.backend.domain.Category;
import com.pfa.backend.domain.Transaction;
import com.pfa.backend.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
	Optional<Transaction> findByDedupeHash(String dedupeHash);

	List<Transaction> findAllByTxnDateBetween(LocalDate startInclusive, LocalDate endInclusive);

	@Query("""
		select t from Transaction t
		where (:type is null or t.type = :type)
		  and (:category is null or t.category = :category)
		  and t.txnDate between :start and :end
	""")
	List<Transaction> findFiltered(
		@Param("start") LocalDate start,
		@Param("end") LocalDate end,
		@Param("type") TransactionType type,
		@Param("category") Category category
	);
}

