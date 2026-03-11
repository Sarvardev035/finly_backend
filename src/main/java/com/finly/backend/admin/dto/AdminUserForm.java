package com.finly.backend.admin.dto;

import com.finly.backend.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserForm {

    @NotBlank
    @Size(max = 120)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @Size(min = 6, message = "Password must be at least 6 chars")
    private String password;

    private Role role = Role.USER;
}
