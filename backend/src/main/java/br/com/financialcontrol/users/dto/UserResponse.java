package br.com.financialcontrol.users.dto;

import br.com.financialcontrol.users.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id, String name, String email, boolean active, Instant createdAt, Instant updatedAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.isActive(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
