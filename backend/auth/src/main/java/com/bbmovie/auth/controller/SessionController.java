package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.dto.request.DeviceRevokeEntry;
import com.bbmovie.auth.dto.request.RevokeDeviceRequest;
import com.bbmovie.auth.dto.response.LoggedInDeviceResponse;
import com.bbmovie.auth.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/device")
public class SessionController {

    private final AuthService authService;

    @Autowired
    public SessionController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(ApiResponse.success("Hallo"));
    }

    @GetMapping("/v1/sessions/all")
    public ResponseEntity<ApiResponse<List<LoggedInDeviceResponse>>> getAllDeviceLoggedIntoAccount(
            @RequestHeader("Authorization") String accessToken, HttpServletRequest request
    ) {
        List<LoggedInDeviceResponse> devices = authService.getAllLoggedInDevices(accessToken, request);
        return devices.isEmpty()
                ? ResponseEntity.ok(ApiResponse.success(List.of()))
                : ResponseEntity.ok(ApiResponse.success(devices));
    }

    @PostMapping("/v1/sessions/revoke")
    public ResponseEntity<ApiResponse<Map<String, String>>> revokeDeviceLoggedIntoAccount(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody RevokeDeviceRequest request
    ) {
        Map<String, String> result = new HashMap<>();
        for (DeviceRevokeEntry device : request.getDevices()) {
            authService.logoutFromOneDevice(accessToken, device.getDeviceName());
            result.put(device.getDeviceName(), device.getIp());
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
