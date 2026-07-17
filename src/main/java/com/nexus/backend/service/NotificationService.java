package com.nexus.backend.service;

import com.nexus.backend.model.Notification;
import com.nexus.backend.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void createNotification(String message, String title, String receiverRole) {
        logger.info("Creating notification for role: {}", receiverRole);
        notificationRepository.save(new Notification(title, message, receiverRole));
        logger.info("Notification created. Title: '{}', Receiver: '{}'", title, receiverRole);
    }

    public void createNotificationForEmail(String title, String message, String receiverRole, String receiverEmail) {
        logger.info("Creating notification for email: {}", receiverEmail);
        notificationRepository.save(new Notification(title, message, receiverRole, receiverEmail));
        logger.info("Notification created for email: {}", receiverEmail);
    }

    public List<Notification> getNotificationsByEmail(String email) {
        logger.info("Fetching notifications for email: {}", email);
        return notificationRepository.findByReceiverEmailOrderByCreatedAtDesc(email);
    }

    public void markAllAsReadByEmail(String email) {
        List<Notification> list = notificationRepository.findByReceiverEmailOrderByCreatedAtDesc(email);
        list.forEach(n -> n.setSeen(true));
        notificationRepository.saveAll(list);
        logger.info("All notifications marked as read for email: {}", email);
    }

    public List<Notification> getNotifications(String role) {

        logger.info("Fetching notifications for role: {}", role);

        List<Notification> notifications =
                notificationRepository.findByReceiverRoleOrderByCreatedAtDesc(role);

        logger.info("Total notifications found for {}: {}", role, notifications.size());

        return notifications;
    }

    public long unreadCount(String role) {

        logger.info("Calculating notification count for role: {}", role);

        long count =
                notificationRepository.findByReceiverRoleOrderByCreatedAtDesc(role).size();

        logger.info("Notification count for {}: {}", role, count);

        return count;
    }

    public void markAsRead(Long id) {

        logger.info("Marking notification as read. Notification ID: {}", id);

        Notification notification =
                notificationRepository.findById(id)
                        .orElseThrow(() -> {
                            logger.error("Notification not found with ID: {}", id);
                            return new RuntimeException("Notification not found");
                        });

        notification.setSeen(true);

        notificationRepository.save(notification);

        logger.info("Notification {} marked as read successfully.", id);
    }

    public void markAllAsRead(String role) {
        List<Notification> list = notificationRepository.findByReceiverRoleOrderByCreatedAtDesc(role);
        list.forEach(n -> n.setSeen(true));
        notificationRepository.saveAll(list);
        logger.info("All notifications marked as read for role: {}", role);
    }
}