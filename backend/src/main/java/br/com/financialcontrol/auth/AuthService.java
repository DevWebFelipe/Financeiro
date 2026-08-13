package br.com.financialcontrol.auth;

import br.com.financialcontrol.UuidV7;
import br.com.financialcontrol.auth.dto.LoginRequest;
import br.com.financialcontrol.auth.dto.LoginResponse;
import br.com.financialcontrol.auth.dto.RegisterRequest;
import br.com.financialcontrol.config.ConflictException;
import br.com.financialcontrol.config.UnauthorizedException;
import br.com.financialcontrol.security.JwtService;
import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
import br.com.financialcontrol.users.dto.UserResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  static final String INVALID_CREDENTIALS = "Credenciais inválidas.";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final Clock clock;
  private final String dummyPasswordHash;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.clock = clock;
    this.dummyPasswordHash = passwordEncoder.encode("dummy-timing-protection-value");
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("E-mail já cadastrado.");
    }
    Instant now = Instant.now(clock);
    User user = new User();
    user.setId(UuidV7.create());
    user.setName(request.name());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setActive(true);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    try {
      userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      throw new ConflictException("E-mail já cadastrado.");
    }
    return UserResponse.from(user);
  }

  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.email()).orElse(null);
    String hashToVerify = user != null ? user.getPasswordHash() : dummyPasswordHash;
    boolean passwordMatches = passwordEncoder.matches(request.password(), hashToVerify);
    if (user == null || !user.isActive() || !passwordMatches) {
      throw new UnauthorizedException(INVALID_CREDENTIALS);
    }
    return new LoginResponse(
        jwtService.createAccessToken(user.getId()), "Bearer", jwtService.expiresInSeconds());
  }
}
