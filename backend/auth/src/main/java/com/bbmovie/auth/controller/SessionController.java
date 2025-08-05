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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/device")
public class SessionController {

    private final AuthService authService;

    @Autowired
    public SessionController(AuthService authService) {
        this.authService = authService;
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

    //TODO: do not use AuthenticationPrincipal, pass jwt instead
    @PostMapping("/v1/sessions/revoke")
    public ResponseEntity<ApiResponse<String>> revokeDeviceLoggedIntoAccount(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody RevokeDeviceRequest request
    ) {
        List<String> revokedDevices = new ArrayList<>();
        for (DeviceRevokeEntry device : request.getDevices()) {
            authService.logoutFromOneDevice(accessToken, device.getDeviceName());
            revokedDevices.add(device.getDeviceName());
        }
        return ResponseEntity.ok(ApiResponse.success(
                String.join(", ", revokedDevices) + " was forced to logout successfully")
        );
    }
}
