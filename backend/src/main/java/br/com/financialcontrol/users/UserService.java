package br.com.financialcontrol.users;

import br.com.financialcontrol.config.ConflictException;
import br.com.financialcontrol.config.UnauthorizedException;
import br.com.financialcontrol.security.AuthenticatedUser;
import br.com.financialcontrol.users.dto.ChangePasswordRequest;
import br.com.financialcontrol.users.dto.UpdateProfileRequest;
import br.com.financialcontrol.users.dto.UserResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public UserResponse getMe(AuthenticatedUser authenticatedUser) {
    return UserResponse.from(requireUser(authenticatedUser));
  }

  @Transactional
  public UserResponse updateMe(AuthenticatedUser authenticatedUser, UpdateProfileRequest request) {
    User user = requireUser(authenticatedUser);
    if (!request.email().equals(user.getEmail()) && userRepository.existsByEmail(request.email())) {
      throw new ConflictException("E-mail já cadastrado.");
    }
    user.setName(request.name());
    user.setEmail(request.email());
    user.setUpdatedAt(Instant.now(clock));
    try {
      userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      throw new ConflictException("E-mail já cadastrado.");
    }
    return UserResponse.from(user);
  }

  @Transactional
  public void changePassword(AuthenticatedUser authenticatedUser, ChangePasswordRequest request) {
    User user = requireUser(authenticatedUser);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Credenciais inválidas.");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setUpdatedAt(Instant.now(clock));
    userRepository.save(user);
  }

  private User requireUser(AuthenticatedUser authenticatedUser) {
    return userRepository
        .findById(authenticatedUser.userId())
        .orElseThrow(() -> new UnauthorizedException("Não autenticado."));
  }
}
