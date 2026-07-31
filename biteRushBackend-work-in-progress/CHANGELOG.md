# CHANGELOG — état réel au 01/08/2026 (session en cours, non terminée)

⚠️ **Le CHANGELOG précédent (conservé plus bas) décrivait des correctifs qui n'étaient
en réalité PAS appliqués au code de ce zip** (bug d'auth toujours cassé, endpoints
manquants absents, SecurityConfig non modifié). Vérifié fichier par fichier avant de
faire confiance à son contenu. Cette section documente ce qui a **réellement** été fait
dans cette session, et ce qui reste en attente.

## Fait (vérifié, mais PAS compilé — voir "Important" en bas)

### Bug critique corrigé : authentification cassée (pour de vrai cette fois)
- `BusinessSecurityUtil.getCurrentUser()`, `DeliveryService.getCurrentUser()`,
  `WebSocketController.getCurrentUserId()` castaient tous le principal JWT en `User`,
  alors que `JwtFilter` place l'**email (String)** comme principal → 401/500 systématique.
  Corrigés par résolution via `UserRepository.findByEmail(auth.getName())`.

### 🔴 Faille critique découverte (non signalée avant) : `@PreAuthorize` totalement ignoré
`ApiApplication` n'avait jamais `@EnableMethodSecurity`. Les annotations `@PreAuthorize`
présentes dans `ReviewController`, `MenuController`, `AddressController`,
`HistoryController`, `RestaurantController` étaient donc **silencieusement ignorées** :
ces endpoints n'étaient protégés que par le filtre générique de `SecurityConfig`
(dans le pire cas, n'importe quel utilisateur authentifié, tous rôles confondus).
Corrigé : `@EnableMethodSecurity` ajouté sur `ApiApplication`.

### `AddressController` — filtre de propriétaire manquant
`GET/PUT/DELETE /addresses/{id}` ne vérifiaient pas que l'adresse appartenait à
l'utilisateur connecté : un client pouvait lire/modifier/supprimer l'adresse d'un
autre client en devinant son ID. Corrigé (méthode `verifyOwner()`).

### Rôle `RESTAURANT_STAFF` câblé (tâche prioritaire de cette session)
- Ajouté à `entity/Role.java`
- `RegisterRequestDTO` : champ `restaurantId` ajouté
- `AuthService.register()` : crée désormais une entité `RestaurantStaff` liée au
  `restaurantId` fourni quand `role == RESTAURANT_STAFF` (avec vérification que le
  restaurant existe)
- **Bug corrigé au passage** : `AuthService.register()` exigeait une `address` pour
  TOUS les rôles (y compris LIVREUR et désormais RESTAURANT_STAFF), alors que ce champ
  n'a de sens que pour CLIENT. Restreint à `role == CLIENT`.

### 🔴 Bug bloquant découvert : création de commande impossible
`Order.user` et `Order.creator` sont `@JoinColumn(nullable = false)` en base, mais
`OrderService.createOrder()` ne les renseignait **jamais** → toute tentative de
commande échouait avec une violation de contrainte SQL. C'est la conséquence directe
du point 3 du tout premier audit ("deux systèmes de commande en parallèle") resté
non tranché.

**Décision prise faute de spécification claire, à valider avec vous** : plutôt que de
garder le flux "commande anonyme", j'ai basculé `createOrder()` sur un flux authentifié :
- `POST /orders` nécessite maintenant un client connecté (`user`/`creator` sont
  renseignés depuis le contexte de sécurité)
- `OrderRequestDTO` a un nouveau champ obligatoire `restaurantId`
- La commande est liée à l'entité `Restaurant` correspondante

C'est un changement cassant côté frontend (le flux `POST /orders` n'est plus `permitAll`
et doit désormais envoyer `restaurantId`) — **`SecurityConfig` n'a pas encore été mis à
jour en conséquence** (voir "À FAIRE" ci-dessous), donc dans l'état actuel la route
retombe sur `anyRequest().authenticated()`, ce qui fonctionne mais sans restriction de
rôle CLIENT explicite.

### `OrderService` / `OrderController`
- `findMyOrders()` + `GET /orders/my-orders` ajoutés (le staff/client doit être connecté)
- `OrderResponseDTO` expose désormais `restaurantId`

### `RestaurantService` réécrit — vérification d'appartenance staff↔restaurant
C'était le second point bloquant demandé : rien n'empêchait un staff du restaurant A
d'agir sur les commandes du restaurant B. Corrigé :
- `getCurrentStaffRestaurantId()` résout le restaurant du staff connecté (ou `null`
  pour un ADMIN, qui n'est pas scoped)
- `verifyOrderBelongsToStaffRestaurant()` appelé dans `acceptOrder`, `startPreparing`,
  `markOrderReady`, `assignToDelivery`, `rejectOrder`
- Dashboard et listes (`pending`/`preparing`/`ready`) désormais filtrées par restaurant
  pour le staff (ADMIN garde la vue globale) via deux nouvelles requêtes dans
  `OrderRepository` (`findByStatusAndRestaurant_IdOrderByCreateAtDesc`,
  `findByRestaurant_IdOrderByCreateAtDesc`)

## À FAIRE — bloquant pour considérer RESTAURANT_STAFF "terminé"

- 🔴 **`SecurityConfig` pas encore mis à jour** : aucune règle explicite pour
  `RESTAURANT_STAFF`/`/restaurant/orders/**`/`/restaurant-staff/**`/`/api/deliveries/**`
  n'a été ajoutée dans cette session (le fichier est resté dans l'état où je l'avais
  laissé lors du tout premier passage : `/users/**` et `/notifications/**` encore trop
  permissifs, `hasAnyRole("USER","ADMIN")` sur les commandes alors que `USER` n'existe
  pas dans l'enum `Role`). C'est la prochaine étape immédiate.
- Décider si `/restaurant-staff/**` (gestion des comptes staff) doit rester ADMIN-only
  ou être ouvert en lecture aux RESTAURANT_STAFF de leur propre restaurant
- `ClientController` (`/clients/**`) : aucun filtre de propriétaire du tout, à traiter
  comme `AddressController`
- `notifyDeliveryAssigned()` dans `DeliveryService` : toujours un stub vide
- Race condition sur le stock (`OrderService.createOrderItem()`) : pas encore de verrou
  pessimiste
- Pas de tests au-delà du fichier généré par défaut
- Email, mot de passe oublié, rate limiting sur `/auth/login`, frais de livraison :
  toujours pas traités
- Frontend (`biteRushFrontend-main/`) : **pas encore adapté** au nouveau contrat de
  `POST /orders` (restaurantId obligatoire, auth requise) ni au reste des changements
  de cette session

## Important
Le projet n'a **pas pu être compilé** dans ce sandbox : pas d'accès réseau vers Maven
Central (uniquement npm/GitHub/crates.io/PyPI sont autorisés ici). Tous les changements
ci-dessus ont été relus manuellement pour cohérence avec le reste du code, mais
`mvn compile` (ou `./mvnw compile`) et `npm run build` restent à lancer de votre côté
avant de considérer quoi que ce soit comme validé. **Cette livraison est un point
d'étape volontairement incomplet**, à la demande de l'utilisateur.

---

# CHANGELOG — contenu original du zip (non fiable, voir avertissement ci-dessus)

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

*(le reste de ce CHANGELOG original décrivait des corrections qui, à vérification,
n'étaient pas présentes dans le code — voir la section du haut)*


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
