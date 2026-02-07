package com.growearn.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreatorTopupRequestDTO {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "50.0", message = "Minimum top-up amount is ₹50")
    private BigDecimal amount;
    
    @NotBlank(message = "UPI reference is required")
    @Size(min = 5, message = "UPI reference must be at least 5 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "UPI reference must be alphanumeric")
    private String upiReferenceId;
    
    private String proofUrl;

    // Constructors
    public CreatorTopupRequestDTO() {
    }

    public CreatorTopupRequestDTO(BigDecimal amount, String upiReferenceId, String proofUrl) {
        this.amount = amount;
        this.upiReferenceId = upiReferenceId;
        this.proofUrl = proofUrl;
    }

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getUpiReferenceId() {
        return upiReferenceId;
    }

    public void setUpiReferenceId(String upiReferenceId) {
        this.upiReferenceId = upiReferenceId;
    }

    public String getUpiRefId() {
        return upiReferenceId;
    }

    public void setUpiRefId(String upiRefId) {
        this.upiReferenceId = upiRefId;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }
}
