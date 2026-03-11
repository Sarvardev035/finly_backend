package com.finly.backend.repository;

import com.finly.backend.domain.model.Notification;
import com.finly.backend.domain.model.NotificationType;
import com.finly.backend.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, NotificationType type);

    List<Notification> findAllByUser(User user);

    List<Notification> findAllByUserOrderByCreatedAtDesc(User user);

    Optional<Notification> findByIdAndUser(UUID id, User user);
}
