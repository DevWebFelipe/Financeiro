package br.com.financialcontrol.users;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

  @Test
  void shouldTrimAndLowercaseEmail() {
    assertThat(EmailNormalizer.normalize("  User@Example.COM  ")).isEqualTo("user@example.com");
  }

  @Test
  void shouldReturnNullWhenEmailIsNull() {
    assertThat(EmailNormalizer.normalize(null)).isNull();
  }
}
