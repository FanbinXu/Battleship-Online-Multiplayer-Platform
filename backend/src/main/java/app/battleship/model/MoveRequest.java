package app.battleship.model;
import jakarta.validation.constraints.*;
public record MoveRequest(
  @NotBlank String moveId,
  @NotBlank String playerId,
  @NotNull Integer turn,
  @NotBlank String cell
) {}
