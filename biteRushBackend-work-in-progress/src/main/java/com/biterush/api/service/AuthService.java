package com.biterush.api.service;

import com.biterush.api.dto.*;
import com.biterush.api.entity.Client;
import com.biterush.api.entity.DeliveryPerson;
import com.biterush.api.entity.Restaurant;
import com.biterush.api.entity.RestaurantStaff;
import com.biterush.api.entity.Role;
import com.biterush.api.entity.User;
import com.biterush.api.repository.ClientRepository;
import com.biterush.api.repository.DeliveryPersonRepository;
import com.biterush.api.repository.RestaurantRepository;
import com.biterush.api.repository.RestaurantStaffRepository;
import com.biterush.api.repository.UserRepository;
import com.biterush.api.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       ClientRepository clientRepository,
                       DeliveryPersonRepository deliveryPersonRepository,
                       RestaurantStaffRepository restaurantStaffRepository,
                       RestaurantRepository restaurantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {

        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.deliveryPersonRepository = deliveryPersonRepository;
        this.restaurantStaffRepository = restaurantStaffRepository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponseDTO register(RegisterRequestDTO dto) {

        if(dto.role == Role.CLIENT && (dto.address == null || dto.address.isBlank())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address required");
        }
        if(userRepository.existsByEmail(dto.email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exits");
        }
        User user = new User();
        user.setNom(dto.name);
        user.setEmail(dto.email);
        user.setPassword(passwordEncoder.encode(dto.password));
        user.setRole(dto.role);
        User saved = userRepository.save(user);
        if(dto.role == Role.CLIENT){

            Client client = new Client();
            client.setUser(user);
            client.setAddress(dto.address);
            clientRepository.save(client);
        }

        if(dto.role == Role.LIVREUR){
            if(dto.vehicule.isBlank() || dto.zone.isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicule & zone is required");
            }
            DeliveryPerson delivery = new DeliveryPerson();
            delivery.setUser(user);
            delivery.setVehicule(dto.vehicule);
            delivery.setZone(dto.zone);

            deliveryPersonRepository.save(delivery);
        }

        if(dto.role == Role.RESTAURANT_STAFF){
            if(dto.restaurantId == null){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "restaurantId is required for RESTAURANT_STAFF");
            }

            Restaurant restaurant = restaurantRepository.findById(dto.restaurantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant introuvable"));

            RestaurantStaff staff = new RestaurantStaff();
            staff.setUser(user);
            staff.setRestaurantId(restaurant.getId());

            restaurantStaffRepository.save(staff);
        }

        return mapToResponse(saved);
    }
    public AuthResponseDTO login(LoginRequestDTO dto) {

        User user = userRepository.findByEmail(dto.email)
                .orElseThrow(() ->
                        new  ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect")
                );

        if (!passwordEncoder.matches(dto.password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        }

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponseDTO(token, refreshToken, user.getId(), user.getEmail(), user.getNom(), user.getRole());
    }

    public AuthResponseDTO refreshToken(String refreshToken) {
        try {
            String email = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if (jwtService.isTokenValid(refreshToken, user)) {
                String newToken = jwtService.generateToken(user);
                String newRefreshToken = jwtService.generateRefreshToken(user);
                return new AuthResponseDTO(newToken, newRefreshToken);
            }

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid refresh token");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Token refresh failed: " + e.getMessage());
        }
    }

    public UserResponseDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToResponse(user);
    }

    private UserResponseDTO mapToResponse(User user) {

        UserResponseDTO dto = new UserResponseDTO();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.role = user.getRole().name();

        return dto;
    }
}