package br.com.financialcontrol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UuidV7Test {

  @Test
  void shouldGenerateVersion7Identifier() {
    var id = UuidV7.create();

    assertThat(id.version()).isEqualTo(7);
    assertThat(id.variant()).isEqualTo(2);
    assertThat(UuidV7.create()).isNotEqualTo(id);
  }
}
