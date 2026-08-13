package br.com.financialcontrol.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private static final int MINIMUM_SECRET_BYTES = 32;

  private final JwtProperties properties;
  private final Clock clock;
  private final byte[] secret;

  public JwtService(JwtProperties properties, Clock clock) {
    byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < MINIMUM_SECRET_BYTES) {
      throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256");
    }
    this.properties = properties;
    this.clock = clock;
    this.secret = secretBytes;
  }

  public String createAccessToken(UUID userId) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plusSeconds(expiresInSeconds());
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(userId.toString())
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiresAt))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    try {
      jwt.sign(new MACSigner(secret));
      return jwt.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException("Failed to sign access token", exception);
    }
  }

  public Optional<UUID> parseUserId(String token) {
    try {
      SignedJWT jwt = SignedJWT.parse(token);
      if (!jwt.verify(new MACVerifier(secret))) {
        return Optional.empty();
      }
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      Date expiration = claims.getExpirationTime();
      if (expiration == null || expiration.toInstant().isBefore(clock.instant())) {
        return Optional.empty();
      }
      String subject = claims.getSubject();
      if (subject == null || subject.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(UUID.fromString(subject));
    } catch (ParseException | JOSEException | IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  public int expiresInSeconds() {
    return properties.expirationMinutes() * 60;
  }
}
