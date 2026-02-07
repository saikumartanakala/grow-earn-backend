package com.growearn.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class WithdrawalRequestDTO {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum withdrawal amount is ₹1")
    private BigDecimal amount;
    
    @NotBlank(message = "UPI ID is required")
    private String upiId;

    // Constructors
    public WithdrawalRequestDTO() {
    }

    public WithdrawalRequestDTO(BigDecimal amount, String upiId) {
        this.amount = amount;
        this.upiId = upiId;
    }

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }
}
