// Writes the router's JWKS (router/jwks.json) from the course public key, so Hive Router can check tokens
// offline. kid is base64url(SHA-256(DER public key)), the same as bin/course token puts in the header.
//   java scripts/Jwks.java /course/modules/m10-security/contract/keys/course-public.pem > router/jwks.json
import java.math.BigInteger;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Jwks {
    public static void main(String[] args) throws Exception {
        String pem = Files.readString(Path.of(args[0])).replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        RSAPublicKey key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        String kid = b64(MessageDigest.getInstance("SHA-256").digest(der));
        System.out.printf("{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"%s\",\"n\":\"%s\",\"e\":\"%s\"}]}%n",
                kid, b64(unsigned(key.getModulus())), b64(unsigned(key.getPublicExponent())));
    }

    static byte[] unsigned(BigInteger v) {
        byte[] b = v.toByteArray();
        return b[0] == 0 ? java.util.Arrays.copyOfRange(b, 1, b.length) : b;
    }

    static String b64(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
