package bbmovie.commerce.payment_orchestrator_service.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "bbmovie.commerce.payment_orchestrator_service",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule no_legacy_api_package =
            noClasses()
                    .should()
                    .resideInAnyPackage("..api..");

    @ArchTest
    static final ArchRule no_legacy_provider_package =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..payment_orchestrator_service.provider..")
                    .because("provider package was migrated to adapter.outbound.provider");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring_or_jpa =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate.."
                    );
}

