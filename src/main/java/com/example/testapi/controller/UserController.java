package com.example.testapi.controller;

import com.example.testapi.dto.BalanceResponse;
import com.example.testapi.security.CustomUserDetails;
import com.example.testapi.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ExpenseService expenseService;

    public UserController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/{id}/balances")
    public ResponseEntity<List<BalanceResponse>> getUserBalances(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(expenseService.getBalancesForUser(id, userDetails.getId()));
    }
}
