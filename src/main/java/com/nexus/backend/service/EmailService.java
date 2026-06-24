package com.nexus.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendCredentials(String toEmail, String name, String role, String password, String course) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Nexus Training Center - Your Login Credentials");

            String body = buildEmailBody(name, role, toEmail, password, course);
            helper.setText(body, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    private String credRow(String label, String value, String valueColor) {
        return "<tr>"
            + "<td style='padding:7px 12px 7px 0;color:#6b7280;font-size:13px;white-space:nowrap;vertical-align:top;'>" + label + "</td>"
            + "<td style='padding:7px 0;color:#6b7280;font-size:13px;vertical-align:top;'>–</td>"
            + "<td style='padding:7px 0 7px 10px;color:" + valueColor + ";font-weight:700;font-size:13px;word-break:break-all;overflow-wrap:anywhere;vertical-align:top;'>" + value + "</td>"
            + "</tr>";
    }

    private String buildEmailBody(String name, String role, String email, String password, String course) {
        String roleDisplay = role.equals("STUDENT") ? "Student Portal" : "Teacher Portal";
        String portalUrl = "http://localhost:8081";
        String extraRow = "";
        if (role.equals("STUDENT") && course != null && !course.isBlank()) {
            extraRow = credRow("Course", course, "#7B2CBF");
        } else if (role.equals("TEACHER") && course != null && !course.isBlank()) {
            // For teacher, course param carries "specialization|employmentType"
            String[] parts = course.split("\\|", 2);
            if (parts.length == 2) {
                extraRow = credRow("Specialization", parts[0], "#7B2CBF")
                         + credRow("Employment", parts[1], "#7B2CBF");
            } else {
                extraRow = credRow("Specialization", parts[0], "#7B2CBF");
            }
        }

        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#ffffff;'>"
            + "<div style='background:#7B2CBF;padding:28px 32px;text-align:center;border-radius:12px 12px 0 0;'>"
            + "<span style='font-size:34px;font-weight:bold;color:#fff;letter-spacing:3px;'>NE<span style='color:#FFB703;'>X</span>US</span>"
            + "<div style='font-size:11px;color:#FFB703;letter-spacing:2px;margin-top:6px;font-weight:600;'>CORPORATE TRAINING CENTER LLP</div>"
            + "</div>"
            + "<div style='background:#ffffff;padding:28px 32px;border-left:1px solid #e5e7eb;border-right:1px solid #e5e7eb;'>"
            + "<h2 style='color:#1f2937;margin:0 0 10px;font-size:20px;'>Welcome, " + name + "!</h2>"
            + "<p style='color:#4b5563;font-size:14px;line-height:1.6;margin:0 0 20px;'>Your account has been created on the <strong>Nexus " + roleDisplay + "</strong>. Here are your login credentials:</p>"
            + "<div style='background:#f5f3ff;border:1px solid #ddd6fe;border-radius:10px;padding:16px 20px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 12px;font-size:11px;font-weight:700;color:#7B2CBF;text-transform:uppercase;letter-spacing:1px;'>Your Login Details</p>"
            + "<table cellpadding='0' cellspacing='0' border='0' style='width:100%;border-collapse:collapse;'>"
            + credRow("Email", email, "#1f2937")
            + credRow("Password", password, "#1f2937")
            + credRow("Role", role, "#7B2CBF")
            + extraRow
            + "</table>"
            + "</div>"
            + "<a href='" + portalUrl + "' style='display:inline-block;background:#7B2CBF;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:700;font-size:14px;'>Login to Portal</a>"
            + "<p style='color:#9ca3af;font-size:11px;margin-top:20px;border-top:1px solid #f3f4f6;padding-top:14px;'>Please change your password after first login. Contact support@nexus.com for help.</p>"
            + "</div>"
            + "<div style='background:#f3f4f6;padding:14px;text-align:center;border-radius:0 0 12px 12px;border:1px solid #e5e7eb;border-top:none;'>"
            + "<p style='margin:0;color:#9ca3af;font-size:11px;'>2026 Nexus Corporate Training Center LLP. All rights reserved.</p>"
            + "</div></div>";
    }
}
