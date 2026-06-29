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

    public void sendEnquiryNotification(String adminEmail, com.nexus.backend.model.Enquiry enquiry) {
        if (fromEmail == null || adminEmail == null) {
            throw new IllegalArgumentException("Email addresses cannot be null");
        }
        String safeFromEmail = fromEmail;
        String safeAdminEmail = adminEmail;
        String charset = "UTF-8";
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, charset);
            helper.setFrom(safeFromEmail);
            helper.setTo(safeAdminEmail);
            String fullName = enquiry.getFullName() != null ? enquiry.getFullName() : "Unknown";
            helper.setSubject("New Enquiry from " + fullName);
            String body = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;'>"
                + "<div style='background:#7B2CBF;padding:24px 32px;border-radius:12px 12px 0 0;text-align:center;'>"
                + "<span style='font-size:28px;font-weight:bold;color:#fff;letter-spacing:3px;'>NE<span style='color:#FFB703;'>X</span>US</span>"
                + "</div>"
                + "<div style='padding:24px 32px;border:1px solid #e5e7eb;border-top:none;'>"
                + "<h2 style='color:#1f2937;margin:0 0 16px;'>New Enquiry Received</h2>"
                + "<table cellpadding='0' cellspacing='0' style='width:100%;border-collapse:collapse;'>"
                + "<tr><td style='padding:8px 0;color:#6b7280;width:120px;'>Name</td><td style='padding:8px 0;font-weight:600;color:#1f2937;'>" + enquiry.getFullName() + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#6b7280;'>Email</td><td style='padding:8px 0;font-weight:600;color:#1f2937;'>" + enquiry.getEmail() + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#6b7280;'>Phone</td><td style='padding:8px 0;font-weight:600;color:#1f2937;'>" + enquiry.getPhoneNumber() + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#6b7280;'>Course</td><td style='padding:8px 0;font-weight:600;color:#7B2CBF;'>" + (enquiry.getCourse() != null ? enquiry.getCourse() : "Not specified") + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#6b7280;vertical-align:top;'>Message</td><td style='padding:8px 0;color:#1f2937;'>" + (enquiry.getMessage() != null ? enquiry.getMessage() : "-") + "</td></tr>"
                + "</table>"
                + "</div>"
                + "<div style='background:#f3f4f6;padding:14px;text-align:center;border-radius:0 0 12px 12px;border:1px solid #e5e7eb;border-top:none;'>"
                + "<p style='margin:0;color:#9ca3af;font-size:11px;'>2026 Nexus Corporate Training Center LLP</p>"
                + "</div></div>";
            helper.setText(body, true);
            mailSender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("Failed to send enquiry email: " + e.getMessage(), e);
        }
    }

    public void sendCredentials(String toEmail, String name, String role, String password, String course, boolean isNew) {
        if (fromEmail == null || toEmail == null) {
            throw new IllegalArgumentException("Email addresses cannot be null");
        }
        String charset = "UTF-8";
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, charset);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            String subject = isNew
                ? "Welcome to Nexus Training Center - Your Login Credentials"
                : "Nexus Training Center - New Course Enrollment";
            helper.setSubject(subject);
            String body = buildEmailBody(name, role, toEmail, password, course, isNew);
            if (body == null) throw new IllegalStateException("Email body cannot be null");
            helper.setText(body, true);
            mailSender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private String credRow(String label, String value, String valueColor) {
        String safeLabel = label != null ? label : "";
        String safeValue = value != null ? value : "";
        String safeColor = valueColor != null ? valueColor : "#000000";
        return "<tr>"
            + "<td style='padding:7px 12px 7px 0;color:#6b7280;font-size:13px;white-space:nowrap;vertical-align:top;'>" + safeLabel + "</td>"
            + "<td style='padding:7px 0;color:#6b7280;font-size:13px;vertical-align:top;'>–</td>"
            + "<td style='padding:7px 0 7px 10px;color:" + safeColor + ";font-weight:700;font-size:13px;word-break:break-all;overflow-wrap:anywhere;vertical-align:top;'>" + safeValue + "</td>"
            + "</tr>";
    }

    private String buildEmailBody(String name, String role, String email, String password, String course, boolean isNew) {
        String roleDisplay = resolveRoleDisplay(role);
        String extraRow = resolveExtraRows(role, course);
        String headline = resolveHeadline(isNew, name);
        String intro = resolveIntro(isNew, roleDisplay);
        String credTitle = resolveCredTitle(isNew);
        String credRows = credRow("Email", email, "#1f2937")
            + credRow("Password", password, "#1f2937")
            + credRow("Role", role, "#7B2CBF")
            + extraRow;
        return buildCredentialsHtml(headline, intro, credTitle, credRows);
    }

    private String resolveRoleDisplay(String role) {
        return role.equals("STUDENT") ? "Student Portal" : "Teacher Portal";
    }

    private String resolveCredTitle(boolean isNew) {
        return isNew ? "Your Login Details" : "Your Login Credentials";
    }

    private String buildCredentialsHtml(String headline, String intro, String credTitle, String credRows) {
        String portalUrl = "http://localhost:8081";
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#ffffff;'>"
            + "<div style='background:#7B2CBF;padding:28px 32px;text-align:center;border-radius:12px 12px 0 0;'>"
            + "<span style='font-size:34px;font-weight:bold;color:#fff;letter-spacing:3px;'>NE<span style='color:#FFB703;'>X</span>US</span>"
            + "<div style='font-size:11px;color:#FFB703;letter-spacing:2px;margin-top:6px;font-weight:600;'>CORPORATE TRAINING CENTER LLP</div>"
            + "</div>"
            + "<div style='background:#ffffff;padding:28px 32px;border-left:1px solid #e5e7eb;border-right:1px solid #e5e7eb;'>"
            + "<h2 style='color:#1f2937;margin:0 0 10px;font-size:20px;'>" + headline + "</h2>"
            + "<p style='color:#4b5563;font-size:14px;line-height:1.6;margin:0 0 20px;'>" + intro + "</p>"
            + "<div style='background:#f5f3ff;border:1px solid #ddd6fe;border-radius:10px;padding:16px 20px;margin-bottom:24px;'>"
            + "<p style='margin:0 0 12px;font-size:11px;font-weight:700;color:#7B2CBF;text-transform:uppercase;letter-spacing:1px;'>" + credTitle + "</p>"
            + "<table cellpadding='0' cellspacing='0' border='0' style='width:100%;border-collapse:collapse;'>" + credRows + "</table>"
            + "</div>"
            + "<a href='" + portalUrl + "' style='display:inline-block;background:#7B2CBF;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:700;font-size:14px;'>Login to Portal</a>"
            + "<p style='color:#9ca3af;font-size:11px;margin-top:20px;border-top:1px solid #f3f4f6;padding-top:14px;'>Please change your password after first login. Contact support@nexus.com for help.</p>"
            + "</div>"
            + "<div style='background:#f3f4f6;padding:14px;text-align:center;border-radius:0 0 12px 12px;border:1px solid #e5e7eb;border-top:none;'>"
            + "<p style='margin:0;color:#9ca3af;font-size:11px;'>2026 Nexus Corporate Training Center LLP. All rights reserved.</p>"
            + "</div></div>";
    }

    private String resolveExtraRows(String role, String course) {
        if (course == null || course.isBlank()) return "";
        if (role.equals("STUDENT")) {
            return credRow("Course", course, "#7B2CBF");
        }
        if (role.equals("TEACHER")) {
            String[] parts = course.split("\\|", 2);
            return parts.length == 2
                ? credRow("Specialization", parts[0], "#7B2CBF") + credRow("Employment", parts[1], "#7B2CBF")
                : credRow("Specialization", parts[0], "#7B2CBF");
        }
        return "";
    }

    private String resolveHeadline(boolean isNew, String name) {
        return isNew ? "Welcome, " + name + "!" : "New Course Enrolled, " + name + "!";
    }

    private String resolveIntro(boolean isNew, String roleDisplay) {
        return isNew
            ? "Your account has been created on the <strong>Nexus " + roleDisplay + "</strong>. Here are your login credentials:"
            : "You have been enrolled in a new course on the <strong>Nexus " + roleDisplay + "</strong>. Use your existing credentials to login:";
    }
}
