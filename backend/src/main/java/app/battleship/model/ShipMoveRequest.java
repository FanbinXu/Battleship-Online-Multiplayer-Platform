package app.battleship.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShipMoveRequest(
    @NotBlank String actionId,
    @NotNull Integer turnNumber,
    @NotBlank String shipId,
    @NotNull Coord newPosition,
    @NotNull Boolean isHorizontal
) {}

