package com.biterush.api.service;

import com.biterush.api.dto.*;
import com.biterush.api.entity.DeliveryPerson;
import com.biterush.api.entity.Restaurant;
import com.biterush.api.entity.RestaurantStaff;
import com.biterush.api.entity.Role;
import com.biterush.api.entity.User;
import com.biterush.api.repository.DeliveryPersonRepository;
import com.biterush.api.repository.RestaurantRepository;
import com.biterush.api.repository.RestaurantStaffRepository;
import com.biterush.api.repository.UserRepository;
import com.biterush.api.security.JwtService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final RestaurantStaffRepository restaurantStaffRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       DeliveryPersonRepository deliveryPersonRepository,
                       RestaurantStaffRepository restaurantStaffRepository,
                       RestaurantRepository restaurantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {

        this.userRepository = userRepository;
        this.deliveryPersonRepository = deliveryPersonRepository;
        this.restaurantStaffRepository = restaurantStaffRepository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponseDTO register(RegisterRequestDTO dto) {

        // Sécurité : la création d'un compte ADMIN via /auth/register (permitAll)
        // est réservée aux administrateurs déjà connectés (token JWT). Un visiteur
        // non authentifié (ou un CLIENT/LIVREUR/STAFF) reçoit 403. La parade côté
        // frontend (RegisterPage) avait déjà retiré l'option ADMIN du formulaire
        // public ; cette garde protège aussi les appels directs à l'API.
        if (dto.role == Role.ADMIN) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.isAuthenticated()
                    && auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "La création d'un compte ADMIN est réservée aux administrateurs connectés"
                );
            }
        }

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
        // CLIENT : l'adresse est persistée sur la ligne `users` elle-même (colonne
        // `address`, ajoutée à l'entité User). L'entité `Client` est une vue
        // @MapsId de CETTE MÊME table — appeler clientRepository.save() faisait
        // un INSERT en double (même PK) → 500 systématique à l'inscription, et
        // aucun profil n'était jamais créé (bug bloquant, cf. CHANGELOG).
        if (dto.role == Role.CLIENT) {
            user.setAddress(dto.address);
        }
        User saved = userRepository.save(user);

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

    /*
     * =========================================================
     * MOT DE PASSE OUBLIÉ
     * =========================================================
     * Décision de conception, faute de spécification : `spring-boot-starter-mail`
     * est absent du pom.xml (signalé dans le CHANGELOG depuis plusieurs sessions) et
     * pas d'accès Maven Central confirmé pour l'ajouter et vérifier qu'il compile.
     * L'envoi d'email est donc MOCKÉ : le lien de réinitialisation est journalisé
     * (log) au lieu d'être envoyé, exactement comme PaymentController/PaymentService
     * documentent déjà leurs paiements comme "MOCK, aucune vraie passerelle n'est
     * appelée" - même patron, même honnêteté sur ce qui est simulé.
     */

    public void forgotPassword(ForgotPasswordRequestDTO dto) {

        // Ne JAMAIS révéler si l'email existe ou non (protection contre
        // l'énumération de comptes) : on répond silencieusement (void, 204 côté
        // contrôleur) que l'utilisateur existe ou pas.
        userRepository.findByEmail(dto.email).ifPresent(user -> {

            String token = generateSecureToken();

            user.setResetPasswordToken(token);
            user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));

            userRepository.save(user);

            // MOCK : à remplacer par un vrai envoi d'email une fois
            // spring-boot-starter-mail ajouté au pom.xml. Le lien contient le token
            // en clair dans les logs uniquement - ne jamais renvoyer le token dans la
            // réponse HTTP.
            log.info(
                    "[MOCK EMAIL] Lien de réinitialisation pour {} : /auth/reset-password?token={} (expire dans 1h)",
                    user.getEmail(),
                    token
            );
        });
    }

    public void resetPassword(ResetPasswordRequestDTO dto) {

        User user = userRepository.findByResetPasswordToken(dto.token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Token invalide ou expiré"
                ));

        if (user.getResetPasswordTokenExpiry() == null
                || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Token invalide ou expiré"
            );
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);

        userRepository.save(user);
    }

    private String generateSecureToken() {

        SecureRandom random = new SecureRandom();

        byte[] bytes = new byte[32];

        random.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    /*
     * =========================================================
     * CHANGEMENT DE MOT DE PASSE (utilisateur connecté)
     * =========================================================
     */
    public void changePassword(String email, ChangePasswordDTO dto) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        if (!passwordEncoder.matches(dto.currentPassword, user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ancien mot de passe incorrect"
            );
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword));
        userRepository.save(user);
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