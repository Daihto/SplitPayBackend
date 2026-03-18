package com.example.testapi.controller;

import com.example.testapi.dto.CreateExpenseRequest;
import com.example.testapi.dto.ExpenseResponse;
import com.example.testapi.security.CustomUserDetails;
import com.example.testapi.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody CreateExpenseRequest request,
                                                         Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ExpenseResponse response = expenseService.createExpense(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
