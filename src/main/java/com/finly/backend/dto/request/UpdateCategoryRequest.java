package com.finly.backend.dto.request;

import com.finly.backend.domain.model.CategoryType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCategoryRequest {

    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    private CategoryType type;
}
