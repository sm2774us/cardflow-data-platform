package com.cardflow.platform.authorization;

import com.cardflow.platform.authorization.AuthorizationEngine.Decision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authorizations")
public class AuthorizationController {
    public record AuthRequest(
            @NotBlank String cardToken,
            @NotBlank @Pattern(regexp = "\\d{4}", message = "must be 4 digits") String mcc,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "ISO-3166 alpha-2") String country) {}

    private final AuthorizationEngine engine;

    public AuthorizationController(AuthorizationEngine engine) { this.engine = engine; }

    /** Synchronous what-if authorization (does not persist). Useful for demos and contract tests. */
    @PostMapping
    public Decision authorize(@Valid @RequestBody AuthRequest r) {
        return engine.decide(r.cardToken(), r.mcc(), r.amount(), r.country());
    }
}
