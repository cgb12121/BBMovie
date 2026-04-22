package com.bbmovie.droolsengine.config;

import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

@Configuration
public class DroolsConfig {

    @Value("${student.verification.auto-approve-threshold:80}")
    private int autoApproveThreshold;

    @Value("${student.verification.auto-reject-threshold:30}")
    private int autoRejectThreshold;

    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        // Load all .drl files from classpath:rules/
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:rules/*.drl");

        for (Resource resource : resources) {
            kieFileSystem.write(ResourceFactory.newClassPathResource("rules/" + resource.getFilename()));
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();
        Results results = kieBuilder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("Drools compilation errors: " + results.getMessages(Message.Level.ERROR));
        }


        return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }

    @Bean
    public int autoApproveThreshold() {
        return autoApproveThreshold;
    }

    @Bean
    public int autoRejectThreshold() {
        return autoRejectThreshold;
    }
}
