package com.supplyai.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record SupplierRequest(
            @NotBlank String name,
            @Email String email,
            @Min(1) int leadTimeDays,
            boolean delayed) {
    }

    public record SupplierResponse(Long id, String name, String email, int leadTimeDays, boolean delayed) {
    }
}
