package com.nexus.backend.service;

import com.nexus.backend.dto.LoginRequest;
import com.nexus.backend.dto.LoginResponse;
import com.nexus.backend.model.LoginHistory;
import com.nexus.backend.model.SecuritySettings;
import com.nexus.backend.model.User;
import com.nexus.backend.repository.LoginHistoryRepository;
import com.nexus.backend.repository.SecuritySettingsRepository;
import com.nexus.backend.repository.UserRepository;
import com.nexus.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SecuritySettingsRepository securitySettingsRepository;
    private final EmailService emailService;

    @Value("${nexus.enquiry.admin-email}")
    private String adminNotificationEmail;

    // In-memory failed attempt counter: email -> count
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    private static final long SESSION_TIMEOUT_MS = 30L * 60 * 1000; // 30 minutes

    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = null;

        try {
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid ID or Password"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                handleFailedAttempt(user, ipAddress, userAgent, request.getDeviceFingerprint());
                throw new RuntimeException("Invalid ID or Password");
            }

            String expectedRole = request.getRole().toUpperCase(Locale.ROOT);
            if (!user.getRole().name().equals(expectedRole)) {
                handleFailedAttempt(user, ipAddress, userAgent, request.getDeviceFingerprint());
                String actualRoleStr = user.getRole().name().substring(0, 1).toUpperCase(Locale.ROOT) + user.getRole().name().substring(1).toLowerCase(Locale.ROOT);
                String requestedRoleStr = request.getRole().substring(0, 1).toUpperCase(Locale.ROOT) + request.getRole().substring(1).toLowerCase(Locale.ROOT);
                throw new RuntimeException("Invalid Role: Account is registered as " + actualRoleStr + ", not " + requestedRoleStr);
            }

            failedAttempts.remove(request.getEmail());

            LocalDateTime now = LocalDateTime.now();
            user.setLastLogin(now);
            userRepository.save(user);

            LoginHistory record = saveLoginHistory(user.getId(), ipAddress, userAgent, "success", request.getDeviceFingerprint());

            final Long currentUserId = user.getId();
            SecuritySettings settings = securitySettingsRepository.findByUserId(currentUserId).orElseGet(() -> {
                SecuritySettings s = new SecuritySettings();
                s.setUserId(currentUserId);
                return s;
            });
            settings.setOnline(true);
            securitySettingsRepository.save(settings);

            if (settings.isLoginAlerts()) {
                sendLoginAlertIfNewDevice(user, record, ipAddress);
            }

            String lastLoginStr = now.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a", Locale.ROOT));

            long expiryMs = (settings != null && settings.isSessionTimeout())
                    ? SESSION_TIMEOUT_MS
                    : 86400000L;

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), expiryMs);
            return new LoginResponse(token, user.getRole().name(), user.getName(), user.getEmail(), lastLoginStr, user.getId());

        } catch (RuntimeException e) {
            throw e;
        }
    }

    private void handleFailedAttempt(User user, String ipAddress, String userAgent, String deviceFingerprint) {
        saveLoginHistory(user.getId(), ipAddress, userAgent, "failed", deviceFingerprint);

        AtomicInteger count = failedAttempts.computeIfAbsent(user.getEmail(), k -> new AtomicInteger(0));
        int attempts = count.incrementAndGet();

        if (attempts >= 3) {
            failedAttempts.remove(user.getEmail());

            SecuritySettings settings = securitySettingsRepository.findByUserId(user.getId()).orElse(null);
            if (settings != null && settings.isFailedLoginAlerts()) {
                try {
                    String[] parsed = parseUserAgent(userAgent);
                    String device = parsed[0] + " on " + parsed[1];
                    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a", Locale.ROOT));
                    emailService.sendFailedLoginAlert(adminNotificationEmail, device, ipAddress, time);
                } catch (Exception ex) {
                    System.err.println("Failed login alert email error: " + ex.getMessage());
                }
            }
        }
    }

    private void sendLoginAlertIfNewDevice(User user, LoginHistory current, String ipAddress) {
        try {
            List<LoginHistory> previousSuccess = loginHistoryRepository
                    .findByUserIdOrderByLoginTimeDesc(user.getId())
                    .stream()
                    .filter(h -> "success".equals(h.getStatus()) && !h.getId().equals(current.getId()))
                    .toList();

            boolean isNewDevice;
            if (previousSuccess.isEmpty()) {
                // Very first successful login — always alert
                isNewDevice = true;
            } else if (current.getDeviceFingerprint() != null && !current.getDeviceFingerprint().isBlank()) {
                // Fingerprint present — new device only if this fingerprint was never seen before
                isNewDevice = previousSuccess.stream()
                        .noneMatch(h -> current.getDeviceFingerprint().equals(h.getDeviceFingerprint()));
            } else {
                // No fingerprint (e.g. API client) — compare IP
                isNewDevice = previousSuccess.stream()
                        .noneMatch(h -> ipAddress.equals(h.getIpAddress()));
            }

            if (isNewDevice) {
                String time = current.getLoginTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a", Locale.ROOT));
                emailService.sendLoginAlert(adminNotificationEmail, current.getDevice(), ipAddress, time);
            }
        } catch (Exception ex) {
            System.err.println("Login alert email error: " + ex.getMessage());
        }
    }

    private LoginHistory saveLoginHistory(Long userId, String ipAddress, String userAgent, String status, String deviceFingerprint) {
        LoginHistory history = new LoginHistory();
        history.setUserId(userId);
        history.setIpAddress(ipAddress);
        history.setStatus(status);
        history.setLoginTime(LocalDateTime.now());
        history.setActive(true);
        history.setDeviceFingerprint(deviceFingerprint);

        String[] parsed = parseUserAgent(userAgent);
        history.setBrowser(parsed[0]);
        history.setOs(parsed[1]);
        history.setDevice(parsed[0] + " on " + parsed[1]);
        history.setLocation("Unknown");

        return loginHistoryRepository.save(history);
    }

    private String[] parseUserAgent(String ua) {
        if (ua == null || ua.isBlank()) return new String[]{"Unknown", "Unknown"};

        String browser = "Unknown";
        String os = "Unknown";

        if (ua.contains("Edg/") || ua.contains("Edge/")) browser = "Edge";
        else if (ua.contains("OPR/") || ua.contains("Opera")) browser = "Opera";
        else if (ua.contains("Chrome")) browser = "Chrome";
        else if (ua.contains("Firefox")) browser = "Firefox";
        else if (ua.contains("Safari") && !ua.contains("Chrome")) browser = "Safari";
        else if (ua.contains("MSIE") || ua.contains("Trident")) browser = "Internet Explorer";

        if (ua.contains("Windows NT")) os = "Windows";
        else if (ua.contains("Mac OS X")) os = "MacOS";
        else if (ua.contains("Android")) os = "Android";
        else if (ua.contains("iPhone") || ua.contains("iPad")) os = "iOS";
        else if (ua.contains("Linux")) os = "Linux";

        return new String[]{browser, os};
    }
}
