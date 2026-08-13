package br.com.financialcontrol;

import java.security.SecureRandom;
import java.util.UUID;

/** Application-generated UUID v7 (RFC 9562). The database does not generate identifiers. */
public final class UuidV7 {

  private static final SecureRandom RANDOM = new SecureRandom();

  private UuidV7() {}

  public static UUID create() {
    byte[] bytes = new byte[16];
    RANDOM.nextBytes(bytes);

    long millis = System.currentTimeMillis();
    bytes[0] = (byte) (millis >>> 40);
    bytes[1] = (byte) (millis >>> 32);
    bytes[2] = (byte) (millis >>> 24);
    bytes[3] = (byte) (millis >>> 16);
    bytes[4] = (byte) (millis >>> 8);
    bytes[5] = (byte) millis;
    bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x70);
    bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80);

    long mostSignificantBits = 0;
    long leastSignificantBits = 0;
    for (int i = 0; i < 8; i++) {
      mostSignificantBits = (mostSignificantBits << 8) | (bytes[i] & 0xff);
    }
    for (int i = 8; i < 16; i++) {
      leastSignificantBits = (leastSignificantBits << 8) | (bytes[i] & 0xff);
    }
    return new UUID(mostSignificantBits, leastSignificantBits);
  }
}
