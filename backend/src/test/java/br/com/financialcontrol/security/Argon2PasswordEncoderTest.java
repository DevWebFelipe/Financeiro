package br.com.financialcontrol.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class Argon2PasswordEncoderTest {

  @Test
  void shouldHashWithArgon2idAndMatchOriginalPassword() {
    PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    String password = "senha-segura";
    String hash = encoder.encode(password);

    assertThat(hash).isNotEqualTo(password);
    assertThat(hash).startsWith("$argon2id$");
    assertThat(encoder.matches(password, hash)).isTrue();
    assertThat(encoder.matches("outra-senha", hash)).isFalse();
  }
}
