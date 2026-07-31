# CHANGELOG — corrections & ajouts en cours

## Fait

### Module paiement (mock) — nouveau
- `entity/PaymentMethod.java`, `entity/PaymentStatus.java`, `entity/Payment.java`
- `repository/PaymentRepository.java`
- `dto/PaymentRequestDTO.java`, `dto/PaymentResponseDTO.java`
- `service/PaymentService.java` (paiement simulé : 90% de réussite pour CARTE/MOBILE_MONEY,
  ESPECES reste EN_ATTENTE jusqu'à `/mark-paid`, endpoint `/webhook` pour simuler un callback PSP)
- `controller/PaymentController.java` : `POST /payments`, `GET /payments/{id}`,
  `GET /payments/order/{orderId}`, `PATCH /payments/{id}/mark-paid`,
  `PATCH /payments/{id}/refund`, `POST /payments/{id}/webhook`
- `entity/Order.java` : relation `OneToOne` vers `Payment`
- `service/OrderService.java` : `confirmOrderAfterPayment(orderId)` appelé quand un paiement réussit

### Bug critique corrigé : authentification cassée
- `BusinessSecurityUtil.getCurrentUser()` castait le principal JWT en `User`, alors que
  `JwtFilter` place l'**email (String)** comme principal → **401 systématique** sur
  `GET /orders/{id}`, `PATCH /orders/{id}/cancel`, et tout ce qui dépend de cette classe.
  Corrigé : résolution de l'utilisateur via `UserRepository.findByEmail(auth.getName())`.
- Même bug corrigé dans `DeliveryService.getCurrentUser()`.

### Endpoints manquants ajoutés (alignement avec le frontend)
- `GET /orders/my-orders` (+ `OrderService.findMyOrders()`)
- `POST /auth/change-password` (+ DTO `ChangePasswordDTO` + `AuthService.changePassword()`)
- `GET /api/deliveries/profile`, `PUT /api/deliveries/profile`,
  `PATCH /api/deliveries/availability` (+ champ `available` ajouté à `DeliveryPerson`)
- `PATCH /api/deliveries/{id}/accept` (alias métier de `/start`)

### Frontend
- `src/services/api.ts` : préfixes livraison unifiés sur `/api/deliveries/**`
  (avant : mélange incohérent `/deliveries/**` et `/api/deliveries/**` → 404 garantis)
- Ajout des appels `createPayment`, `getPaymentByOrder`, `getDeliveryProfile`,
  `updateDeliveryProfile`, `setDeliveryAvailability`
- `src/types/api.ts` : ajout des types `PaymentDTO`, `DeliveryPersonProfile`

### `SecurityConfig.java` — réécrit (partiellement bloqué, voir "À FAIRE")
- Faille corrigée : `/users/**` n'est plus `permitAll()` → `hasRole("ADMIN")`
- `/notifications/**` : `permitAll()` → `authenticated()`
- Ajout de règles explicites par rôle pour `/restaurant/orders/**`, `/api/deliveries/**`,
  `/payments/**`, `/restaurant-staff/**`, etc. (routes qui retombaient avant sur
  `anyRequest().authenticated()` sans filtre de rôle)
- Bug corrigé : `hasAnyRole("USER","ADMIN")` → `USER` n'existe pas dans l'enum `Role`
  (remplacé par une logique correcte, cf. ci-dessous)

## À FAIRE (bloquant / important)

### 🔴 Rôle `RESTAURANT_STAFF` manquant dans l'enum `Role`
`entity/Role.java` ne contient que `CLIENT`, `ADMIN`, `LIVREUR`. Il n'existe **aucun rôle**
pour le personnel de restaurant, alors que `RestaurantStaffController`, `RestaurantOrderController`
et les nouvelles règles `SecurityConfig` en dépendent. Résultat : les règles
`hasAnyRole("RESTAURANT_STAFF", "ADMIN")` que j'ai ajoutées ne matcheront **jamais** tant que :
1. `RESTAURANT_STAFF` n'est pas ajouté à `Role.java`
2. `AuthService.register()` ne gère pas la création d'un `RestaurantStaff` pour ce rôle
   (actuellement seuls `CLIENT` et `LIVREUR` sont gérés à l'inscription)
3. `RegisterRequestDTO` n'accepte pas de `restaurantId` pour rattacher le staff à un restaurant

Tant que ce n'est pas fait, l'espace restaurant tourne probablement avec des comptes `ADMIN`
en pratique — à vérifier/valider avec vous avant de trancher.

### 🟠 Autres points identifiés mais pas encore traités
- Vérification "ce staff appartient bien à CE restaurant" absente sur `/restaurant/orders/**`
- `notifyDeliveryAssigned()` dans `DeliveryService` : stub vide, jamais branché à `NotificationService`
- Pas d'email (confirmation commande, reset mot de passe)
- Pas de flow "mot de passe oublié"
- Pas de calcul de frais de livraison / distance
- Pas de rate limiting sur `/auth/login`
- Pas de tests au-delà du fichier généré par défaut

## Important
Le projet **n'a pas été compilé** dans ce sandbox (pas d'accès réseau vers Maven Central
pour ce module précis). Lancez `mvn compile` (backend) et `npm run build` (frontend)
de votre côté pour valider avant de merger.
