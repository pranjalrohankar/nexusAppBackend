package com.nexus.backend.controller;

import com.nexus.backend.service.NotificationService;

public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }


}