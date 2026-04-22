package com.bbmovie.droolsengine.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DroolsConfigTest {

    @Test
    void kieContainerBuildFailureIsSurfaced() {
        DroolsConfig config = new DroolsConfig();
        assertThrows(RuntimeException.class, config::kieContainer);
    }

    @Test
    void thresholdBeansReturnConfiguredValues() {
        DroolsConfig config = new DroolsConfig();
        ReflectionTestUtils.setField(config, "autoApproveThreshold", 85);
        ReflectionTestUtils.setField(config, "autoRejectThreshold", 25);

        assertEquals(85, config.autoApproveThreshold());
        assertEquals(25, config.autoRejectThreshold());
    }
}
