# Documentation API - Biterush

## Vue d’ensemble
Cette documentation récapitule les endpoints principaux, les champs attendus, les rôles autorisés et les erreurs possibles.

## 1. Authentification

### POST /auth/register
Créer un compte utilisateur.

Champs attendus :
- name : string, obligatoire
- email : string, obligatoire, format email valide
- password : string, obligatoire, minimum 6 caractères
- role : enum, obligatoire
- address : string, optionnel (pour client)
- vehicule : string, optionnel (pour livreur)
- zone : string, optionnel (pour livreur)

Erreurs possibles :
- 400 : validation échouée
- 409 : email déjà utilisé

### POST /auth/login
Connexion utilisateur.

Champs attendus :
- email : string, obligatoire, email valide
- password : string, obligatoire

Erreurs possibles :
- 400 : validation échouée
- 401 : identifiants invalides

### POST /auth/refresh
Renouveler le token JWT.

### POST /auth/logout
Déconnecter l’utilisateur.

### GET /auth/me
Récupérer le profil connecté.

---

## 2. Utilisateurs

### GET /users
Lister les utilisateurs.

### POST /users
Créer un utilisateur.

Champs attendus :
- name : string, obligatoire
- email : string, obligatoire, email valide
- password : string, obligatoire, minimum 6 caractères

Erreurs possibles :
- 400 : validation échouée

### GET /users/{id}
Récupérer un utilisateur par ID.

### PUT /users/{id}
Mettre à jour un utilisateur.

### DELETE /users/{id}
Supprimer un utilisateur.

---

## 3. Restaurants

### GET /restaurants
Lister les restaurants.

### GET /restaurants/{id}
Récupérer un restaurant.

### POST /restaurants
Créer un restaurant.

Champs attendus :
- name : string, obligatoire, max 100 caractères
- address : string, obligatoire, max 255 caractères
- phoneNumber : string, obligatoire, max 20 caractères
- email : string, obligatoire, email valide

Erreurs possibles :
- 400 : validation échouée
- 403 : accès refusé

### PUT /restaurants/{id}
Mettre à jour un restaurant.

### DELETE /restaurants/{id}
Supprimer un restaurant.

### GET /restaurants/search?query=...
Rechercher des restaurants.

---

## 4. Produits / Menu

### GET /menu-items
Lister les éléments de menu.

### GET /menu-items/{id}
Récupérer un élément de menu.

### POST /menu-items
Créer un élément de menu.

Champs attendus :
- name : string, obligatoire, max 100 caractères
- description : string, obligatoire, max 500 caractères
- price : number, obligatoire, minimum 0
- stock : integer, obligatoire, minimum 0
- category : string, obligatoire
- restaurantId : long, optionnel

Erreurs possibles :
- 400 : validation échouée
- 404 : restaurant introuvable
- 403 : accès refusé

### PUT /menu-items/{id}
Mettre à jour un élément de menu.

### DELETE /menu-items/{id}
Supprimer un élément de menu.

### GET /menu-items/restaurant/{restaurantId}/menu
Récupérer le menu d’un restaurant.

### GET /menu-items/search?query=...
Recherche par nom.

### GET /menu-items/category/{category}
Filtrer par catégorie.

---

## 5. Commandes

### POST /orders
Créer une commande.

Champs attendus :
- clientName : string, obligatoire, max 100 caractères
- phone : string, obligatoire, max 20 caractères
- address : string, obligatoire, max 255 caractères
- items : array, obligatoire, non vide
  - productId : long, obligatoire
  - quantity : integer, obligatoire, minimum 1

Erreurs possibles :
- 400 : validation échouée
- 409 : stock insuffisant
- 409 : statut de commande invalide

### GET /orders/{id}
Récupérer une commande.

### PATCH /orders/{id}/cancel
Annuler une commande.

### PUT /orders/admin/{id}
Mettre à jour une commande en mode admin.

### DELETE /orders/admin/{id}
Supprimer une commande en mode admin.

### PATCH /orders/admin/{id}/deliver
Marquer une commande comme livrée.

---

## 6. Adresses

### POST /addresses
Créer une adresse.

Champs attendus :
- street : string, obligatoire
- city : string, obligatoire
- zipCode : string, obligatoire
- country : string, obligatoire
- latitude : number, obligatoire, entre -90 et 90
- longitude : number, obligatoire, entre -180 et 180
- label : string, optionnel
- isDefault : boolean, optionnel

Erreurs possibles :
- 400 : validation échouée

### GET /addresses
Lister les adresses.

### GET /addresses/{id}
Récupérer une adresse.

### PUT /addresses/{id}
Mettre à jour une adresse.

### DELETE /addresses/{id}
Supprimer une adresse.

---

## 7. Livraison

### POST /api/deliveries/assign
Assigner une livraison.

Champs attendus :
- orderId : long, obligatoire
- livreur : long, obligatoire

### GET /api/deliveries
Lister les livraisons.

### GET /api/deliveries/me
Récupérer les livraisons de l’utilisateur courant.

### PATCH /api/deliveries/{id}/start
Démarrer une livraison.

### PATCH /api/deliveries/{id}/deliver
Valider une livraison.

### PATCH /api/deliveries/{id}/cancel
Annuler une livraison.

### GET /api/deliveries/{id}
Récupérer une livraison par ID.

---

## 8. Avis

### POST /reviews/orders/{orderId}
Créer un avis.

Champs attendus :
- rating : integer, obligatoire, entre 1 et 5
- comment : string, obligatoire

### GET /reviews/restaurants/{restaurantId}
Récupérer les avis d’un restaurant.

### GET /reviews/restaurants/{restaurantId}/average
Calculer la moyenne des avis.

---

## 9. Erreurs globales

Les erreurs sont renvoyées dans un format commun :

```json
{
  "timestamp": "2026-07-19T10:00:00",
  "status": 400,
  "error": "Validation failed",
  "errors": {
    "fieldName": "message de validation"
  },
  "path": "/endpoint"
}
```

### Codes HTTP courants
- 400 : mauvaise requête / validation échouée / JSON mal formé
- 403 : accès refusé
- 404 : ressource introuvable
- 409 : conflit métier (statut invalide, stock insuffisant)
- 500 : erreur interne serveur
