package bbmovie.commerce.entitlement_service.application.rules;

public interface DecisionRule {
    DecisionRuleResult evaluate(DecisionContext context);
}
