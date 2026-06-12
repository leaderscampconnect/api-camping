package com.esprit.microservice.apicamping.client;

import com.esprit.microservice.apicamping.config.FeignConfig;
import com.esprit.microservice.apicamping.dto.NotificationPageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service", url = "${notification.service.url:${NOTIFICATION_SERVICE_URL:}}", configuration = FeignConfig.class)
public interface NotificationClient {

    @GetMapping("/api/notifications/admin/all")
    NotificationPageDto getAllNotifications(@RequestParam(value = "userId", required = false) String userId);

    @GetMapping("/api/notifications")
    NotificationPageDto getUserNotifications();

    @org.springframework.web.bind.annotation.PostMapping("/notifications/v2/camping")
    com.esprit.microservice.apicamping.dto.NotificationDto createNotification(@org.springframework.web.bind.annotation.RequestBody com.esprit.microservice.apicamping.dto.NotificationDto request);
}
