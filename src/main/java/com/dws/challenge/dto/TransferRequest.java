package com.dws.challenge.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotNull(message = "Source account ID cannot be null")
    @NotEmpty(message = "Source account ID cannot be empty")
    private String accountFromId;

    @NotNull(message = "Destination account ID cannot be null")
    @NotEmpty(message = "Destination account ID cannot be empty")
    private String accountToId;

    @Min(value = 1, message = "Transfer amount must be greater than 0")
    private BigDecimal amount;
}

