package com.biterush.api.service;

import com.biterush.api.dto.UserRequestDTO;
import com.biterush.api.dto.UserResponseDTO;
import com.biterush.api.entity.User;
import com.biterush.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDTO> getAll(){
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserResponseDTO getById(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Utilisateur introuvable"));
        return mapToResponse(user);
    }

    public User save(UserRequestDTO dto){
        User user = new User();
        user.setNom(dto.name);
        user.setEmail(dto.email);
        user.setPassword(dto.password);
        return userRepository.save(user);
    }

    public User update(Long id, UserRequestDTO dto) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setNom(dto.name);
                    user.setEmail(dto.email);
                    user.setPassword(dto.password);

                    return userRepository.save(user);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    //UTILS

    private UserResponseDTO mapToResponse(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.id = user.getId();
        dto.role = user.getRole().name();
        dto.email = user.getEmail();
        return dto;
    }
}
