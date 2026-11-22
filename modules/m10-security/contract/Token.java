// Mint a JWT for a Module 10 course user, signed with the course key (keys/README.md). JDK only.
//
//   bin/course token <user> [options]        user: ada ben max olu root (see the realm), or any name
//     --expired          exp one hour ago
//     --ttl <seconds>    lifetime (default 300, like the realm)
//     --forged           signed with a fresh random key, but claiming the course key's kid
//     --none             "alg": "none" and no signature
//     --aud <audience>   default shelfie-api
//     --iss <issuer>     default http://keycloak:8180/realms/shelfie
//     --roles a,b        override the user's roles
//     --claim k=v        add a string claim (repeatable), e.g. --claim tenant=acme (Module 14)
//     --header           print "Authorization: Bearer <token>" instead of the bare token
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

public class Token {
  static final Path KEYS = Path.of("/course/modules/m10-security/contract/keys");
  static final Map<String, List<String>> ROLES = Map.of(
      "ada", List.of("member"), "ben", List.of("member"), "max", List.of("member", "moderator"),
      "olu", List.of("member"), "root", List.of("member", "admin"));

  public static void main(String[] args) throws Exception {
    if (args.length == 0) { System.err.println("usage: bin/course token <user> [--expired] [--forged] [--none] [--ttl s] [--aud a] [--iss i] [--roles a,b] [--header]"); System.exit(2); }
    String user = args[0], aud = "shelfie-api", iss = "http://keycloak:8180/realms/shelfie";
    long ttl = 300, now = System.currentTimeMillis() / 1000;
    boolean expired = false, forged = false, none = false, header = false;
    List<String> roles = ROLES.getOrDefault(user, List.of("member"));
    StringBuilder extra = new StringBuilder();
    for (int i = 1; i < args.length; i++) {
      switch (args[i]) {
        case "--expired" -> expired = true;
        case "--forged" -> forged = true;
        case "--none" -> none = true;
        case "--header" -> header = true;
        case "--ttl" -> ttl = Long.parseLong(args[++i]);
        case "--aud" -> aud = args[++i];
        case "--iss" -> iss = args[++i];
        case "--roles" -> roles = List.of(args[++i].split(","));
        case "--claim" -> { String[] kv = args[++i].split("=", 2); extra.append(",").append(q(kv[0])).append(":").append(q(kv[1])); }
        default -> { System.err.println("unknown option " + args[i]); System.exit(2); }
      }
    }
    long iat = expired ? now - 3900 : now, exp = expired ? now - 3600 : now + ttl;

    byte[] spki = pem(KEYS.resolve("course-public.pem"));
    String kid = b64(MessageDigest.getInstance("SHA-256").digest(spki));
    String head = none ? "{\"alg\":\"none\",\"typ\":\"JWT\"}"
        : "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"" + kid + "\"}";
    String body = "{\"iss\":" + q(iss) + ",\"aud\":" + q(aud) + ",\"sub\":" + q("sub-" + user)
        + ",\"azp\":\"shelfie-cli\",\"typ\":\"Bearer\",\"preferred_username\":" + q(user)
        + ",\"roles\":[" + String.join(",", roles.stream().map(Token::q).toList()) + "]"
        + extra + ",\"iat\":" + iat + ",\"exp\":" + exp + "}";
    String signingInput = b64(head.getBytes(StandardCharsets.UTF_8)) + "." + b64(body.getBytes(StandardCharsets.UTF_8));

    String sig = "";
    if (!none) {
      PrivateKey key = forged
          ? KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate()
          : KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pem(KEYS.resolve("course-private.pem"))));
      Signature s = Signature.getInstance("SHA256withRSA");
      s.initSign(key);
      s.update(signingInput.getBytes(StandardCharsets.US_ASCII));
      sig = b64(s.sign());
    }
    String token = signingInput + "." + sig;
    System.out.println(header ? "Authorization: Bearer " + token : token);
  }

  static byte[] pem(Path p) throws Exception {
    String s = Files.readString(p).replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
    return Base64.getDecoder().decode(s);
  }

  static String b64(byte[] b) { return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }

  static String q(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
}
