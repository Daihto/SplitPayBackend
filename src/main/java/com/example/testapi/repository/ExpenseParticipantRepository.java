package com.example.testapi.repository;

import com.example.testapi.entity.ExpenseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, Long> {
}
