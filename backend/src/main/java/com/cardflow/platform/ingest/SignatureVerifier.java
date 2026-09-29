package com.cardflow.platform.ingest;

import com.cardflow.platform.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** HMAC-SHA256 webhook authentication over the raw body: header {@code X-Signature: sha256=<hex>}. Constant-time compare. */
@Component
public class SignatureVerifier {
    private static final String ALGO = "HmacSHA256";
    private final byte[] secret;

    public SignatureVerifier(AppProperties props) {
        this.secret = props.webhook().secret().getBytes(StandardCharsets.UTF_8);
    }

    public String sign(byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret, ALGO));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean verify(byte[] body, String header) {
        if (header == null || header.isBlank()) return false;
        return MessageDigest.isEqual(sign(body).getBytes(StandardCharsets.UTF_8), header.trim().getBytes(StandardCharsets.UTF_8));
    }
}
