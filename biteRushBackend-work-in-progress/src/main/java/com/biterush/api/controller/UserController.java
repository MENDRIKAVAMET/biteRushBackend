package com.biterush.api.controller;

import com.biterush.api.dto.UserRequestDTO;
import com.biterush.api.dto.UserResponseDTO;
import com.biterush.api.entity.User;
import com.biterush.api.repository.UserRepository;
import com.biterush.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@CrossOrigin("*")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // GET all users
    @GetMapping
    public List<UserResponseDTO> getAll() {
        return userService.getAll();
    }

    // POST create user
    @PostMapping
    public User save(@Valid @RequestBody UserRequestDTO dto) {
        return userService.save(dto);
    }

    @GetMapping("/{id}")
    public UserResponseDTO getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void  deleteById(@PathVariable Long id) {
        userRepository.deleteById(id);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @Valid @RequestBody UserRequestDTO dto) {
        return userService.update(id, dto);
    }
}