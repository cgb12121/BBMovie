package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.dto.request.DeviceRevokeEntry;
import com.bbmovie.auth.dto.request.RevokeDeviceRequest;
import com.bbmovie.auth.dto.response.LoggedInDeviceResponse;
import com.bbmovie.auth.exception.UnauthorizedUserException;
import com.bbmovie.auth.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/device")
public class DeviceSessionController {

    private final AuthService authService;

    @Autowired
    public DeviceSessionController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/sessions/all")
    public ResponseEntity<ApiResponse<List<LoggedInDeviceResponse>>> getAllDeviceLoggedIntoAccount(
            @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request
    ) {
        if (userDetails == null) { throw new UnauthorizedUserException("User not authenticated"); }
        List<LoggedInDeviceResponse> devices = authService.getAllLoggedInDevices(userDetails.getUsername(), request);
        return devices.isEmpty()
                ? ResponseEntity.ok(ApiResponse.success(List.of()))
                : ResponseEntity.ok(ApiResponse.success(devices));
    }

    @PostMapping("/sessions/revoke")
    public ResponseEntity<ApiResponse<String>> revokeDeviceLoggedIntoAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RevokeDeviceRequest request
    ) {
        if (userDetails == null) { throw new UnauthorizedUserException("User not authenticated"); }
        List<String> revokedDevices = new ArrayList<>();
        for (DeviceRevokeEntry device : request.getDevices()) {
            authService.logoutFromOneDevice(userDetails.getUsername(), device.getDeviceName());
            revokedDevices.add(device.getDeviceName());
        }
        return ResponseEntity.ok(ApiResponse.success(
                String.join(", ", revokedDevices) + " was forced to logout successfully")
        );
    }
}
