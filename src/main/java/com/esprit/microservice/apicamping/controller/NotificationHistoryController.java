package com.esprit.microservice.apicamping.controller;

import com.esprit.microservice.apicamping.client.NotificationClient;
import com.esprit.microservice.apicamping.dto.NotificationPageDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/camping-notifications")
public class NotificationHistoryController {

    private final NotificationClient notificationClient;

    public NotificationHistoryController(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    private static final java.util.List<String> CAMPING_TYPES = java.util.Arrays.asList(
        "REGISTRATION_CONFIRMED",
        "REGISTRATION_CANCELLED",
        "booking.confirmed",
        "booking.cancelled",
        "GENERAL"
    );

    private NotificationPageDto filterCampingNotifications(NotificationPageDto page) {
        if (page == null || page.getData() == null) {
            return page;
        }
        java.util.List<com.esprit.microservice.apicamping.dto.NotificationDto> filteredList = page.getData().stream()
            .filter(n -> n.getType() != null && CAMPING_TYPES.contains(n.getType()))
            .collect(java.util.stream.Collectors.toList());
        
        page.setData(filteredList);
        
        if (page.getPagination() != null) {
            page.getPagination().setTotal(filteredList.size());
            page.getPagination().setPages(1); 
        }
        return page;
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasAnyRole('CAMPER', 'USER', 'SITE_OWNER', 'ADMIN', 'ORGANIZER')")
    public ResponseEntity<NotificationPageDto> getMyNotificationHistory() {
        NotificationPageDto page = notificationClient.getUserNotifications();
        return ResponseEntity.ok(filterCampingNotifications(page));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationPageDto> getAllNotificationHistory(@RequestParam(value = "userId", required = false) String userId) {
        NotificationPageDto page = notificationClient.getAllNotifications(userId);
        return ResponseEntity.ok(filterCampingNotifications(page));
    }
}
