package com.copilot.rssi.controller;

import com.copilot.rssi.dto.response.ApiResponse;
import com.copilot.rssi.dto.response.RoleResponse;
import com.copilot.rssi.dto.response.UserResponse;
import com.copilot.rssi.service.interfaces.AuthService;
import com.copilot.rssi.service.interfaces.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RSSI')")
public class DashboardController {

    private final AuthService authService;
    private final RoleService roleService;

    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.ok(authService.getAllUsers());
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> getRoles() {
        return ApiResponse.ok(roleService.getAll());
    }
}