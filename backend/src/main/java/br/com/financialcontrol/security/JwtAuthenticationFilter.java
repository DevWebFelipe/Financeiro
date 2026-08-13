package br.com.financialcontrol.security;

import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final UserRepository userRepository;

  public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    extractBearerToken(request)
        .flatMap(jwtService::parseUserId)
        .flatMap(this::loadActiveUser)
        .ifPresent(user -> authenticate(request, user));
    filterChain.doFilter(request, response);
  }

  private Optional<String> extractBearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      return Optional.empty();
    }
    String token = header.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(token);
  }

  private Optional<User> loadActiveUser(UUID userId) {
    return userRepository.findById(userId).filter(User::isActive);
  }

  private void authenticate(HttpServletRequest request, User user) {
    AuthenticatedUser principal = new AuthenticatedUser(user.getId());
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, List.of());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
