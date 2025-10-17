package app.battleship.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttackRequest(
    @NotBlank String actionId,
    @NotNull Integer turnNumber,
    @NotBlank String type,
    @NotNull Coord target
) {}


