package com.yuemo.cart.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleSelectRequest(
        @NotNull Boolean selected
) {}
