package bbmovie.commerce.entitlement_service.application.rules;

public record DecisionRuleResult(
        boolean passed,
        String reasonCode
) {
    public static DecisionRuleResult pass() {
        return new DecisionRuleResult(true, "OK");
    }

    public static DecisionRuleResult deny(String reasonCode) {
        return new DecisionRuleResult(false, reasonCode);
    }
}
