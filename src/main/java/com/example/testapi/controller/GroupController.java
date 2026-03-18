package com.example.testapi.controller;

import com.example.testapi.dto.AddMemberRequest;
import com.example.testapi.dto.BalanceResponse;
import com.example.testapi.dto.CreateGroupRequest;
import com.example.testapi.dto.ExpenseResponse;
import com.example.testapi.dto.GroupResponse;
import com.example.testapi.security.CustomUserDetails;
import com.example.testapi.service.ExpenseService;
import com.example.testapi.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final ExpenseService expenseService;

    public GroupController(GroupService groupService, ExpenseService expenseService) {
        this.groupService = groupService;
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroups(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return ResponseEntity.ok(groupService.getGroupsForUser(currentUserId));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request,
                                                     Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(request, currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return ResponseEntity.ok(groupService.getGroupById(id, currentUserId));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupResponse> addMember(@PathVariable Long id,
                                                   @Valid @RequestBody AddMemberRequest request,
                                                   Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return ResponseEntity.ok(groupService.addMember(id, request, currentUserId));
    }

    @GetMapping("/{id}/expenses")
    public ResponseEntity<List<ExpenseResponse>> getGroupExpenses(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return ResponseEntity.ok(expenseService.getExpensesByGroup(id, currentUserId));
    }

    @GetMapping("/{id}/balances")
    public ResponseEntity<List<BalanceResponse>> getGroupBalances(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return ResponseEntity.ok(expenseService.getBalancesByGroup(id, currentUserId));
    }

    private Long getCurrentUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }
}
