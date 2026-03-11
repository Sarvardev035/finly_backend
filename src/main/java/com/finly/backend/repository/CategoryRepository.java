package com.finly.backend.repository;

import com.finly.backend.domain.model.Category;
import com.finly.backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserId(UUID userId);

    List<Category> findAllByUser(User user);

    Optional<Category> findByIdAndUser(UUID id, User user);

    boolean existsByNameAndUser(String name, User user);

    boolean existsByNameIgnoreCaseAndUserAndIdNot(String name, User user, UUID id);

    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
