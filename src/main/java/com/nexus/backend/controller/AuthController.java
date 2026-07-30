package com.nexus.backend.controller;

import com.nexus.backend.dto.ApiResponse;
import com.nexus.backend.dto.LoginRequest;
import com.nexus.backend.model.LoginHistory;
import com.nexus.backend.model.SecuritySettings;
import com.nexus.backend.repository.LoginHistoryRepository;
import com.nexus.backend.repository.SecuritySettingsRepository;
import com.nexus.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SecuritySettingsRepository securitySettingsRepository;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            var response = authService.login(request, ipAddress, userAgent);
            return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/login-history")
    public ResponseEntity<ApiResponse> getLoginHistory(@RequestParam Long userId) {
        List<LoginHistory> history = loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(userId);
        return ResponseEntity.ok(ApiResponse.ok("Login history fetched", history));
    }

    @GetMapping("/security-settings")
    public ResponseEntity<ApiResponse> getSecuritySettings(@RequestParam Long userId) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    SecuritySettings defaults = new SecuritySettings();
                    defaults.setUserId(userId);
                    return securitySettingsRepository.save(defaults);
                });
        return ResponseEntity.ok(ApiResponse.ok("Security settings fetched", settings));
    }

    @PutMapping("/security-settings")
    public ResponseEntity<ApiResponse> updateSecuritySettings(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    SecuritySettings s = new SecuritySettings();
                    s.setUserId(userId);
                    return s;
                });
        if (body.containsKey("sessionTimeout"))
            settings.setSessionTimeout(Boolean.parseBoolean(body.get("sessionTimeout").toString()));
        if (body.containsKey("loginAlerts"))
            settings.setLoginAlerts(Boolean.parseBoolean(body.get("loginAlerts").toString()));
        if (body.containsKey("failedLoginAlerts"))
            settings.setFailedLoginAlerts(Boolean.parseBoolean(body.get("failedLoginAlerts").toString()));
        securitySettingsRepository.save(settings);
        return ResponseEntity.ok(ApiResponse.ok("Security settings updated", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestBody(required = false) Map<String, Object> body) {
        if (body != null && body.containsKey("userId")) {
            Long userId = Long.valueOf(body.get("userId").toString());
            securitySettingsRepository.findByUserId(userId).ifPresent(s -> {
                s.setOnline(false);
                securitySettingsRepository.save(s);
            });
        }
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse> heartbeat(@RequestBody Map<String, Object> body) {
        if (body != null && body.containsKey("userId")) {
            Long userId = Long.valueOf(body.get("userId").toString());
            securitySettingsRepository.findByUserId(userId).ifPresent(s -> {
                s.setOnline(true);
                securitySettingsRepository.save(s);
            });
        }
        return ResponseEntity.ok(ApiResponse.ok("Heartbeat received", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;
        return request.getRemoteAddr();
    }
}
 