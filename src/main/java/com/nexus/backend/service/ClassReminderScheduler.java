package com.nexus.backend.service;

import com.nexus.backend.model.Batch;
import com.nexus.backend.repository.BatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ClassReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ClassReminderScheduler.class);

    private final BatchRepository batchRepository;
    private final AppNotificationService appNotificationService;

    private final Set<Long> notifiedToday = new HashSet<>();
    private int lastCheckedDay = -1;

    public ClassReminderScheduler(BatchRepository batchRepository,
                                   AppNotificationService appNotificationService) {
        this.batchRepository = batchRepository;
        this.appNotificationService = appNotificationService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void checkUpcomingClasses() {
        int today = LocalDate.now().getDayOfYear();
        if (today != lastCheckedDay) {
            notifiedToday.clear();
            lastCheckedDay = today;
        }

        LocalTime now = LocalTime.now();
        LocalTime windowStart = now.plusMinutes(14);
        LocalTime windowEnd   = now.plusMinutes(16);
        String todayAbbr = LocalDate.now().getDayOfWeek().name().substring(0, 3);

        List<Batch> batches = batchRepository.findAll();
        for (Batch batch : batches) {
            if (notifiedToday.contains(batch.getId())) continue;
            if (batch.getClassDays() == null || batch.getClassTimings() == null) continue;

            boolean classToday = batch.getClassDays().stream()
                    .anyMatch(d -> d.name().equalsIgnoreCase(todayAbbr));
            if (!classToday) continue;

            LocalTime classStart = parseTime(batch.getClassTimings().split("-")[0].trim());
            if (classStart == null) continue;

            if (classStart.isAfter(windowStart) && classStart.isBefore(windowEnd)) {
                logger.info("Sending 15-min reminder for: {}", batch.getSelectCourse());
                appNotificationService.notifyClassStartingSoon(batch.getSelectCourse(), batch.getBatchName());
                notifiedToday.add(batch.getId());
            }
        }
    }

    private LocalTime parseTime(String raw) {
        for (String fmt : new String[]{"h:mm a", "hh:mm a", "H:mm", "HH:mm"}) {
            try {
                return LocalTime.parse(raw.toUpperCase(), DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
