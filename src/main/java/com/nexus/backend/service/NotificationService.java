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

    public void createNotification(String message,
                                   String title,
                                   String receiverRole) {

        logger.info("Creating notification for role: {}", receiverRole);

        Notification notification =
                new Notification(title, message, receiverRole);

        notificationRepository.save(notification);

        logger.info("Notification created successfully. Title: '{}', Receiver: '{}'",
                title, receiverRole);
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
}