package com.example.testapi.service;

import com.example.testapi.dto.BalanceResponse;
import com.example.testapi.dto.CreateExpenseRequest;
import com.example.testapi.dto.ExpenseResponse;
import com.example.testapi.entity.Expense;
import com.example.testapi.entity.ExpenseParticipant;
import com.example.testapi.entity.Group;
import com.example.testapi.entity.User;
import com.example.testapi.exception.BadRequestException;
import com.example.testapi.exception.ResourceNotFoundException;
import com.example.testapi.repository.ExpenseRepository;
import com.example.testapi.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository, GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request, Long currentUserId) {
        Group group = groupService.getGroupAndValidateMember(request.getGroupId(), currentUserId);

        User payer = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean payerIsGroupMember = group.getMembers()
            .stream()
            .anyMatch(member -> member.getId().equals(payer.getId()));

        if (!payerIsGroupMember) {
            throw new BadRequestException("Payer must be a member of the group");
        }

        Set<Long> participantIds = new LinkedHashSet<>(request.getParticipantIds());
        if (participantIds.isEmpty()) {
            throw new BadRequestException("participantIds is required");
        }

        Set<Long> groupMemberIds = new HashSet<>();
        for (User member : group.getMembers()) {
            groupMemberIds.add(member.getId());
        }

        for (Long participantId : participantIds) {
            if (!groupMemberIds.contains(participantId)) {
                throw new BadRequestException("All participants must be members of the group");
            }
        }

        Expense expense = new Expense();
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        expense.setPaidBy(payer);
        expense.setGroup(group);

        for (Long participantId : participantIds) {
            User participantUser = userRepository.findById(participantId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            ExpenseParticipant expenseParticipant = new ExpenseParticipant();
            expenseParticipant.setExpense(expense);
            expenseParticipant.setUser(participantUser);
            expense.getParticipants().add(expenseParticipant);
        }

        Expense saved = expenseRepository.save(expense);
        return toExpenseResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByGroup(Long groupId, Long currentUserId) {
        groupService.getGroupAndValidateMember(groupId, currentUserId);

        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(this::toExpenseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalancesByGroup(Long groupId, Long currentUserId) {
        Group group = groupService.getGroupAndValidateMember(groupId, currentUserId);

        Map<Long, String> userNames = new HashMap<>();
        for (User member : group.getMembers()) {
            userNames.put(member.getId(), member.getName());
        }

        List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        return calculateBalances(expenses, userNames);
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalancesForUser(Long userId, Long currentUserId) {
        if (!userId.equals(currentUserId)) {
            throw new AccessDeniedException("You are not allowed to access this resource");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, BalanceResponse> combined = new HashMap<>();

        for (Group group : user.getGroups()) {
            List<BalanceResponse> groupBalances = getBalancesByGroup(group.getId(), currentUserId);
            for (BalanceResponse balance : groupBalances) {
                if (!balance.getFromUserId().equals(userId) && !balance.getToUserId().equals(userId)) {
                    continue;
                }

                String key = balance.getFromUserId() + ":" + balance.getToUserId();
                BalanceResponse existing = combined.get(key);
                if (existing == null) {
                    combined.put(key, balance);
                } else {
                    BigDecimal updated = existing.getAmount().add(balance.getAmount());
                    existing.setAmount(updated.setScale(2, RoundingMode.HALF_UP));
                }
            }
        }

        List<BalanceResponse> responses = new ArrayList<>(combined.values());
        responses.sort(Comparator.comparing(BalanceResponse::getFromUserId).thenComparing(BalanceResponse::getToUserId));
        return responses;
    }

    private List<BalanceResponse> calculateBalances(List<Expense> expenses, Map<Long, String> userNames) {
        Map<String, BigDecimal> rawDebts = new HashMap<>();

        for (Expense expense : expenses) {
            if (expense.getParticipants().isEmpty()) {
                continue;
            }

            BigDecimal splitAmount = expense.getAmount()
                    .divide(BigDecimal.valueOf(expense.getParticipants().size()), 2, RoundingMode.HALF_UP);

            Long payerId = expense.getPaidBy().getId();

            for (ExpenseParticipant participant : expense.getParticipants()) {
                Long participantId = participant.getUser().getId();
                if (participantId.equals(payerId)) {
                    continue;
                }

                String debtKey = participantId + ":" + payerId;
                BigDecimal currentValue = rawDebts.getOrDefault(debtKey, BigDecimal.ZERO);
                rawDebts.put(debtKey, currentValue.add(splitAmount));
            }
        }

        return buildNetBalances(rawDebts, userNames);
    }

    private List<BalanceResponse> buildNetBalances(Map<String, BigDecimal> rawDebts, Map<Long, String> userNames) {
        List<BalanceResponse> balances = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (Map.Entry<String, BigDecimal> entry : rawDebts.entrySet()) {
            String key = entry.getKey();
            if (visited.contains(key)) {
                continue;
            }

            String[] parts = key.split(":");
            Long fromUserId = Long.parseLong(parts[0]);
            Long toUserId = Long.parseLong(parts[1]);
            BigDecimal amount = entry.getValue();

            String reverseKey = toUserId + ":" + fromUserId;
            BigDecimal reverseAmount = rawDebts.getOrDefault(reverseKey, BigDecimal.ZERO);

            visited.add(key);
            visited.add(reverseKey);

            BigDecimal netAmount = amount.subtract(reverseAmount).setScale(2, RoundingMode.HALF_UP);
            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                balances.add(new BalanceResponse(
                        fromUserId,
                        userNames.getOrDefault(fromUserId, "Unknown"),
                        toUserId,
                        userNames.getOrDefault(toUserId, "Unknown"),
                        netAmount,
                        "UNPAID",
                        false
                ));
            } else if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
                balances.add(new BalanceResponse(
                        toUserId,
                        userNames.getOrDefault(toUserId, "Unknown"),
                        fromUserId,
                        userNames.getOrDefault(fromUserId, "Unknown"),
                        netAmount.abs(),
                        "UNPAID",
                        false
                ));
            }
        }

        balances.sort(Comparator.comparing(BalanceResponse::getFromUserId).thenComparing(BalanceResponse::getToUserId));
        return balances;
    }

    private ExpenseResponse toExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getName(),
                expense.getCreatedAt(),
                expense.getGroup().getId()
        );
    }
}
