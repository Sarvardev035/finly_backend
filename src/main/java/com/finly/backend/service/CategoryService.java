package com.finly.backend.service;

import com.finly.backend.domain.model.Category;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateCategoryRequest;
import com.finly.backend.dto.request.UpdateCategoryRequest;
import com.finly.backend.dto.response.CategoryResponse;
import com.finly.backend.exception.CategoryNotFoundException;
import com.finly.backend.exception.DuplicateCategoryNameException;
import com.finly.backend.exception.InvalidCredentialsException;
import com.finly.backend.repository.CategoryRepository;
import com.finly.backend.repository.UserRepository;
import com.finly.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return createCategory(request, user);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request, User user) {

        if (categoryRepository.existsByNameAndUser(request.getName(), user)) {
            throw new DuplicateCategoryNameException("Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .user(user)
                .name(request.getName())
                .type(request.getType())
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getUserCategories(UserDetailsImpl principal) {
        User user = getUser(principal);
        return getUserCategories(user);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getUserCategories(User user) {
        return categoryRepository.findAllByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        return getCategoryById(id, user);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id, User user) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return updateCategory(id, request, user);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request, User user) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            // Check uniqueness ignoring the current category's own case
            if (categoryRepository.existsByNameIgnoreCaseAndUserAndIdNot(request.getName(), user, id)) {
                throw new DuplicateCategoryNameException(
                        "Category with name '" + request.getName() + "' already exists");
            }
            category.setName(request.getName());
        }

        if (request.getType() != null) {
            category.setType(request.getType());
        }

        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        deleteCategory(id, user);
    }

    @Transactional
    public void deleteCategory(UUID id, User user) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));

        // TODO: Validate that no transactions are linked to this category before
        // deleting.
        // If there are transactions, throw new CategoryInUseException("Cannot delete
        // category in use by transactions");

        categoryRepository.delete(category);
    }

    // ---- Helper methods ----

    private User getUser(UserDetailsImpl principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user not found."));
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
