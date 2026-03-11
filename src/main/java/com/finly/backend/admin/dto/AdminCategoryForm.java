package com.finly.backend.admin.dto;

import com.finly.backend.domain.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCategoryForm {
    @NotBlank
    private String name;

    @NotBlank
    private String icon;

    @NotBlank
    private String color;

    @NotNull
    private CategoryType type;
}
