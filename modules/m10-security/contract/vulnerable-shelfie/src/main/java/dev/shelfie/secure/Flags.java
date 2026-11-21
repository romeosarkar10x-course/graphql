package dev.shelfie.secure;

import java.security.MessageDigest;
import java.util.HexFormat;

/** Capture the flag: one flag per vulnerability. Playing fair means not reading this file. */
final class Flags {
  private Flags() {}

  static String of(String slug) {
    try {
      byte[] h = MessageDigest.getInstance("SHA-256").digest(("shelfie-ctf-2026/" + slug).getBytes());
      return "FLAG{" + slug + "-" + HexFormat.of().formatHex(h).substring(0, 8) + "}";
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
