package com.example.bbmovie.controller;

import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.entity.SubscriptionPlan;
import com.example.bbmovie.service.payment.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscription-plan")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @GetMapping("/plan")
    public ResponseEntity<ApiResponse<List<SubscriptionPlan>>> getSubscriptionPlan() {
        List<SubscriptionPlan> plan = subscriptionPlanService.getAllSubscriptionPlans();
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @GetMapping("/plan/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlan>> getSubscriptionPlanById(@PathVariable Long id) {
        SubscriptionPlan plan = subscriptionPlanService.getSubscriptionPlanById(id);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @GetMapping("/plan/{name}")
    public ResponseEntity<ApiResponse<Object>> getSubscriptionPlanByName(@PathVariable String name) {
        Object plan = subscriptionPlanService.getSubscriptionPlanByName(name);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @PostMapping("/plan/create")
    @PreAuthorize("hasRole('ROLE_ADMIN' || 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlan>> createSubscriptionPlan(@RequestBody SubscriptionPlan plan) {
        SubscriptionPlan createdPlan = subscriptionPlanService.createSubscriptionPlan(plan);
        return ResponseEntity.ok(ApiResponse.success(createdPlan));
    }

    @PutMapping("/plan/update")
    @PreAuthorize("hasRole('ROLE_ADMIN' || 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlan>> updateSubscriptionPlan(@RequestBody SubscriptionPlan plan) {
        SubscriptionPlan updatedPlan = subscriptionPlanService.updateSubscriptionPlan(plan);
        return ResponseEntity.ok(ApiResponse.success(updatedPlan));
    }

    @DeleteMapping("/plan/delete/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN' || 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteSubscriptionPlan(@PathVariable String id) {
        String deletedPlan = subscriptionPlanService.deleteSubscriptionPlan(id);
        return ResponseEntity.ok(ApiResponse.success(deletedPlan));
    }

    @PatchMapping("/plan/enable/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN' || 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> enableSubscriptionPlan(@PathVariable String id) {
        String enabledPlan = subscriptionPlanService.enableSubscriptionPlan(id);
        return ResponseEntity.ok(ApiResponse.success(enabledPlan));
    }

    @PatchMapping("/plan/disable/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN' || 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> disableSubscriptionPlan(@PathVariable String id) {
        String disabledPlan = subscriptionPlanService.disableSubscriptionPlan(id);
        return ResponseEntity.ok(ApiResponse.success(disabledPlan));
    }
}
