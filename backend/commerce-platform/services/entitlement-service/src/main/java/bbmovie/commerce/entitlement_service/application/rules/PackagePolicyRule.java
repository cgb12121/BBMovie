package bbmovie.commerce.entitlement_service.application.rules;

import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.PlanContentPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PackagePolicyRule implements DecisionRule {
    private final PlanContentPolicyRepository policyRepository;

    @Override
    public DecisionRuleResult evaluate(DecisionContext context) {
        String requestedPackage = context.request().contentPackage();
        if (requestedPackage == null || requestedPackage.isBlank() || context.activeRecord() == null) {
            return DecisionRuleResult.pass();
        }
        boolean allowed = policyRepository.findByPlanIdAndEnabledTrue(context.activeRecord().getPlanId())
                .stream()
                .anyMatch(policy -> requestedPackage.equalsIgnoreCase(policy.getContentPackage()));
        return allowed ? DecisionRuleResult.pass() : DecisionRuleResult.deny("PLAN_PACKAGE_MISMATCH");
    }
}
