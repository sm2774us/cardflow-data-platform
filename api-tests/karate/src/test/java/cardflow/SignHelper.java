package cardflow;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Static helpers callable from Karate via Java.type('cardflow.SignHelper'). */
public final class SignHelper {
    private SignHelper() {}

    public static String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder sb = new StringBuilder("sha256=");
        for (byte b : mac.doFinal(body.getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static String uuid() { return UUID.randomUUID().toString(); }
}
