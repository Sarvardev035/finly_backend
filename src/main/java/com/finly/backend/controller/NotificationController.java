package com.finly.backend.controller;

import com.finly.backend.domain.model.Notification;
import com.finly.backend.dto.response.ApiResponse;
import com.finly.backend.repository.UserRepository;
import com.finly.backend.security.UserDetailsImpl;
import com.finly.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotifications(
                userRepository.findByEmail(principal.getUsername()).orElseThrow())));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Notification>>> getAllNotifications(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getAllNotifications(
                userRepository.findByEmail(principal.getUsername()).orElseThrow())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        notificationService.markAsRead(id,
                userRepository.findByEmail(principal.getUsername()).orElseThrow());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
