package br.com.financialcontrol.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "test-jwt-secret-that-is-at-least-thirty-two-bytes-long";
  private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

  private JwtService jwtService;
  private Clock clock;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(NOW, ZoneOffset.UTC);
    jwtService = new JwtService(new JwtProperties(SECRET, 30), clock);
  }

  @Test
  void shouldCreateTokenWithSubIatAndExp() throws Exception {
    UUID userId = UUID.fromString("01800000-0000-7000-8000-000000000001");
    String token = jwtService.createAccessToken(userId);
    SignedJWT jwt = SignedJWT.parse(token);

    assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
    assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(userId.toString());
    assertThat(jwt.getJWTClaimsSet().getIssueTime()).isEqualTo(Date.from(NOW));
    assertThat(jwt.getJWTClaimsSet().getExpirationTime())
        .isEqualTo(Date.from(NOW.plusSeconds(1800)));
    assertThat(jwt.getJWTClaimsSet().getClaims().keySet())
        .containsExactlyInAnyOrder("sub", "iat", "exp");
  }

  @Test
  void shouldParseUserIdFromValidToken() {
    UUID userId = UUID.fromString("01800000-0000-7000-8000-000000000002");
    String token = jwtService.createAccessToken(userId);

    assertThat(jwtService.parseUserId(token)).contains(userId);
  }

  @Test
  void shouldRejectExpiredToken() throws Exception {
    UUID userId = UUID.fromString("01800000-0000-7000-8000-000000000003");
    String token = jwtService.createAccessToken(userId);
    JwtService later =
        new JwtService(
            new JwtProperties(SECRET, 30), Clock.fixed(NOW.plusSeconds(1801), ZoneOffset.UTC));

    assertThat(later.parseUserId(token)).isEmpty();
  }

  @Test
  void shouldRejectTokenWithInvalidSignature() throws Exception {
    UUID userId = UUID.fromString("01800000-0000-7000-8000-000000000004");
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(userId.toString())
            .issueTime(Date.from(NOW))
            .expirationTime(Date.from(NOW.plusSeconds(1800)))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(
        new MACSigner(
            "other-secret-that-is-at-least-thirty-two-b".getBytes(StandardCharsets.UTF_8)));

    assertThat(jwtService.parseUserId(jwt.serialize())).isEmpty();
  }

  @Test
  void shouldRejectMalformedToken() {
    assertThat(jwtService.parseUserId("not-a-jwt")).isEmpty();
  }

  @Test
  void shouldRequireSecretOfAtLeast32Bytes() {
    assertThatThrownBy(
            () -> new JwtService(new JwtProperties("short-secret", 30), Clock.systemUTC()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void expiresInSecondsShouldBe1800() {
    assertThat(jwtService.expiresInSeconds()).isEqualTo(1800);
  }
}
