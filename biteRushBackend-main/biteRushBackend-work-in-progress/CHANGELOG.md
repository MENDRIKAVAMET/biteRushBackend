# CHANGELOG — session du 01/08/2026 (suite) — backend uniquement

⚠️ **Remarque sur le zip reçu en entrée** : le zip contenait TROIS copies imbriquées du
projet (`/` racine sans CHANGELOG ni frontend, `/biteRushBackend-work-in-progress/` avec
CHANGELOG + frontend, et `/biteRushBackend-payment-mock/` contenant elle-même une
quatrième copie imbriquée `biteRushBackend-main/` + `biteRushFrontend-main/`, avec un
CHANGELOG différent et plus ancien). J'ai travaillé exclusivement dans
`biteRushBackend-work-in-progress/`, qui correspond à l'état "01/08/2026, état réel"
décrit dans la mission (mêmes bugs, mêmes fichiers cités). Les deux autres copies n'ont
pas été touchées — à vérifier avec vous si l'une d'elles était censée être la référence,
car leur contenu diverge par endroits (ex: `AuthController`, `DeliveryController`,
`entity/DeliveryPerson.java` diffèrent entre `biteRushBackend-payment-mock/.../src` et
`biteRushBackend-work-in-progress/src`).

Frontend explicitement ignoré pour cette session, à la demande de l'utilisateur —
le point 2 de la mission (adaptation du frontend au nouveau contrat `POST /orders`)
n'a **pas** été traité et reste entièrement à faire.

## Fait et vérifié dans le code (backend)

### 1. `SecurityConfig` — corrigé
Toutes les affirmations du point 1 de la mission ont été vérifiées dans le fichier
avant modification (état constaté identique à ce que décrivait le CHANGELOG précédent) :
- `/users/**` : `permitAll()` → `hasRole("ADMIN")`
- `/notifications/**` : `permitAll()` → `authenticated()` (le filtrage par utilisateur
  est déjà fait côté `NotificationController`/`NotificationService` pour la liste, le
  compteur non-lus et "tout marquer comme lu" — voir bug découvert ci-dessous pour
  `markAsRead`/`delete`)
- `hasAnyRole("USER", "ADMIN")` sur `GET /orders/**` → remplacé par
  `hasAnyRole("CLIENT", "LIVREUR", "ADMIN")` (`USER` n'existe pas dans l'enum `Role`,
  vérifié dans `entity/Role.java` : seuls `CLIENT, ADMIN, LIVREUR, RESTAURANT_STAFF`
  existent)
- `POST /orders` : `permitAll()` → `hasRole("CLIENT")`. Vérifié au préalable dans
  `OrderService.createOrder()` que le flux utilise bien
  `SecurityContextHolder.getContext().getAuthentication()` (via `getCurrentUser()`
  interne) pour résoudre l'utilisateur par email — donc un appel non authentifié
  échouait de toute façon avec 401 côté service ; la règle `permitAll()` était trompeuse
  et masquait cette dépendance. Pas de risque de casser le flux : on aligne juste
  `SecurityConfig` sur ce que le service exige déjà.
- Ajout de règles explicites pour `RESTAURANT_STAFF` :
  `/restaurant/orders/**` → `hasAnyRole("RESTAURANT_STAFF","ADMIN")` (vérifié :
  `RestaurantOrderController` n'avait aucune annotation `@PreAuthorize`, reposait
  entièrement sur `SecurityConfig`, qui ne le couvrait pas du tout avant → tombait sur
  `anyRequest().authenticated()`, donc accessible à n'importe quel rôle authentifié)
- `/payments/**` : déjà présent, non dupliqué (vérifié avant d'ajouter)
- `/api/deliveries/**` : **manquait entièrement** (vérifié : aucun matcher, tombait sur
  `anyRequest().authenticated()` — n'importe quel rôle pouvait assigner une livraison,
  lister toutes les livraisons ou en supprimer). Ajouté avec une distinction que la
  mission ne précisait pas — **décision de conception documentée ici, à valider** :
  - `POST /api/deliveries/assign` (assigner un livreur à une commande) →
    `RESTAURANT_STAFF, ADMIN` (c'est une action métier restaurant/admin, pas livreur)
  - `GET /api/deliveries` (liste complète) → `RESTAURANT_STAFF, ADMIN`
  - `DELETE /api/deliveries/**` → `ADMIN` uniquement
  - Le reste (`/me`, `/{id}/start`, `/{id}/deliver`, `/{id}/cancel`, `GET /{id}`) →
    `LIVREUR, ADMIN` (ce sont les actions du livreur lui-même sur ses propres
    livraisons ; `BusinessSecurityUtil.verifyDeliveryAccess()` filtre déjà par
    propriétaire pour `GET /{id}`)

### 2. `ClientController` — filtre de propriétaire ajouté
Vérifié avant correctif : aucun filtre, `GET/PUT/DELETE /clients/{id}` et
`POST /clients/{userId}` étaient ouverts à tout utilisateur authentifié sans
vérification — un client pouvait lire/modifier/supprimer le profil d'un autre client
en devinant son id. Vérifié aussi que `Client.id` correspond à `User.id`
(`@OneToOne @MapsId` dans `entity/Client.java`), donc `verifyOwner()` compare
directement l'id du client visé à l'id de l'utilisateur courant (via
`BusinessSecurityUtil.getCurrentUser()`, réutilisé plutôt que dupliqué comme dans
`AddressController`). `GET /clients` (liste complète) restreint à `ADMIN`, qui n'était
pas non plus filtré avant. ADMIN garde un accès complet sans restriction de
propriétaire, cohérent avec le patron `AddressController`/`RestaurantService`.

## Vérifié dans le code — déjà fait dans une session précédente (donc PAS retouché)

Ces points étaient listés comme "vérifié" dans la section précédente du CHANGELOG ;
je les ai re-vérifiés moi-même dans le code avant de m'appuyer dessus (consigne de
méthode) :
- Bug JWT (cast en `User`) : confirmé corrigé dans `BusinessSecurityUtil`,
  `DeliveryService`, `WebSocketController` (résolution par `findByEmail`)
- `@EnableMethodSecurity` : confirmé présent sur `ApiApplication`
- `AddressController.verifyOwner()` : confirmé présent et correct
- `Role.RESTAURANT_STAFF`, `AuthService.register()` avec `restaurantId` : confirmé
- `Order.user`/`creator`/`restaurant` renseignés dans `createOrder()` : confirmé,
  `OrderRequestDTO.restaurantId` est bien `@NotNull`
- `RestaurantService` (vérification appartenance staff↔restaurant) : confirmé,
  `verifyOrderBelongsToStaffRestaurant()` appelé dans les 5 méthodes citées
- Verrou pessimiste sur le stock : **déjà fait**, contrairement à ce que suggérait le
  point 4 de la mission comme "à vérifier". Confirmé dans `ProductRepository`
  (`findAllByIdIn` avec `@Lock(LockModeType.PESSIMISTIC_WRITE)`) et confirmé que
  `OrderService.buildOrderItems()` l'utilise bien pour charger les produits avant de
  décrémenter le stock. Rien à faire ici.

## 🆕 Nouveau bug découvert cette session (non corrigé, ajouté ici comme demandé)

- `NotificationController.markAsRead(id)` et `deleteNotification(id)` ne vérifient
  **pas** que la notification appartient à l'utilisateur courant (contrairement à
  `getMyNotifications`, `getUnreadNotifications`, `countUnread`, `markAllAsRead` qui
  filtrent bien par `userId`). Un utilisateur authentifié peut donc marquer comme lue
  ou supprimer la notification d'un autre utilisateur en devinant son id. À corriger
  avec le même patron que `AddressController`/`ClientController`
  (`NotificationService` devrait exposer l'`userId` propriétaire de la notification
  pour permettre la vérification).

## Toujours en attente (non traité cette session, priorité décroissante)

- `notifyDeliveryAssigned()` dans `DeliveryService` : toujours un stub vide (vérifié,
  inchangé)
- `/restaurant-staff/**` : réglé sur `ADMIN` uniquement pour cette session (décision
  par défaut, la plus sûre en l'absence de logique de filtrage par restaurant dans
  `RestaurantStaffService`/`RestaurantStaffController`, qui n'existe pas actuellement).
  Ouvrir en lecture aux `RESTAURANT_STAFF` pour leur propre restaurant nécessiterait
  d'ajouter cette logique de filtrage d'abord — non fait.
- Bug notifications ci-dessus
- Frontend entièrement intact, non adapté au contrat `POST /orders`
- Email, mot de passe oublié, rate limiting `/auth/login`, frais de livraison par
  distance : toujours pas traités
- Tests d'intégration `OrderService`/`RestaurantService` : toujours pas ajoutés

## Important
Pas d'accès à Maven Central dans cet environnement non plus (uniquement
npm/GitHub/crates.io/PyPI/pypi.org autorisés). Tous les changements ci-dessus ont été
relus manuellement fichier par fichier pour cohérence de types/imports, mais
`./mvnw compile` reste à lancer de votre côté avant de considérer ceci comme validé.

---

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
