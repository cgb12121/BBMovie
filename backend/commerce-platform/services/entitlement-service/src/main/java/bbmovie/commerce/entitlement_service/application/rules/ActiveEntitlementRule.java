package bbmovie.commerce.entitlement_service.application.rules;

import org.springframework.stereotype.Component;

@Component
public class ActiveEntitlementRule implements DecisionRule {
    @Override
    public DecisionRuleResult evaluate(DecisionContext context) {
        if (context.activeRecord() == null) {
            return DecisionRuleResult.deny("NO_ACTIVE_ENTITLEMENT");
        }
        return DecisionRuleResult.pass();
    }
}
