package bbmovie.auth.mfa_service.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MfaStateStore {

    private final Map<String, MfaState> states = new ConcurrentHashMap<>();

    public MfaState getOrCreate(String email) {
        return states.computeIfAbsent(email.toLowerCase(), ignored -> new MfaState(null, false));
    }

    public void save(String email, MfaState state) {
        states.put(email.toLowerCase(), state);
    }

    public record MfaState(String totpSecret, boolean enabled) {
    }
}
