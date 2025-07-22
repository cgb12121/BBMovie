package com.bbmovie.auth.controller.admin;

import com.bbmovie.auth.security.jose.config.JwkInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    private final JwkInspector jwkInspector;

    @Autowired
    public DebugController(JwkInspector jwkInspector) {
        this.jwkInspector = jwkInspector;
    }

    @GetMapping("/debug/beans")
    public String inspectBeans() {
        jwkInspector.inspectBeans();
        return "Bean values logged. Check application logs.";
    }
}