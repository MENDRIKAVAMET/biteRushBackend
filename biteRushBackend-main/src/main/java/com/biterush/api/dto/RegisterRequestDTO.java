package com.biterush.api.dto;

import com.biterush.api.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public class RegisterRequestDTO {
    @NotBlank(message = "Name is required")
    public String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    public String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    public String password;

    @NotNull(message = "Role is required")
    public Role role;

    // client
    public String address;

    // livreur
    public String vehicule;
    public String zone;
}