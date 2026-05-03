package com.example.testapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExpenseResponse {

    private Long id;
    private String description;
    private BigDecimal amount;
    private String paidByName;
    private LocalDateTime createdAt;
    private Long groupId;

    public ExpenseResponse() {
    }

    public ExpenseResponse(Long id, String description, BigDecimal amount, String paidByName, LocalDateTime createdAt, Long groupId) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.paidByName = paidByName;
        this.createdAt = createdAt;
        this.groupId = groupId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public void setPaidByName(String paidByName) {
        this.paidByName = paidByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
