package com.biterush.api.service;

import org.springframework.stereotype.Service;

/**
 * =============================================================
 * CALCUL DES FRAIS DE LIVRAISON
 * =============================================================
 * Calcul par DISTANCE réelle (formule de Haversine), pas par "zone" :
 * le modèle de données actuel n'a aucune notion de zone tarifaire
 * (le `zone` de `DeliveryPerson` est juste la zone de travail du
 * livreur, pas une zone de facturation), donc un calcul par distance
 * entre le restaurant et l'adresse de livraison est l'approche la
 * plus fidèle à ce qui existe réellement.
 *
 * Barème : frais = BASE_FEE + (distance_km * PER_KM_RATE), borné
 * entre MIN_FEE et MAX_FEE.
 *
 * FALLBACK : si les coordonnées du restaurant OU de la destination
 * sont absentes (restaurant pas encore géolocalisé, ou requête sans
 * lat/lng), on applique un tarif forfaitaire par défaut plutôt que
 * de faire échouer la commande. Il n'y a pas d'accès à un service de
 * géocodage externe dans ce sandbox pour convertir une adresse texte
 * en coordonnées.
 */
@Service
public class DeliveryFeeService {

    private static final double BASE_FEE = 1000.0;      // frais de base (Ar)
    private static final double PER_KM_RATE = 300.0;     // tarif par km (Ar)
    private static final double MIN_FEE = 1000.0;        // plancher
    private static final double MAX_FEE = 10000.0;       // plafond
    private static final double DEFAULT_FEE = 2000.0;    // fallback si coordonnées manquantes

    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * Calcule les frais de livraison entre le restaurant et l'adresse
     * de livraison. Retourne le tarif forfaitaire par défaut si l'une
     * des paires de coordonnées est absente.
     */
    public double computeFee(Double restaurantLat, Double restaurantLng,
                              Double destinationLat, Double destinationLng) {

        if (restaurantLat == null || restaurantLng == null
                || destinationLat == null || destinationLng == null) {
            return DEFAULT_FEE;
        }

        double distanceKm = haversineDistanceKm(
                restaurantLat, restaurantLng,
                destinationLat, destinationLng
        );

        double fee = BASE_FEE + (distanceKm * PER_KM_RATE);

        return Math.min(MAX_FEE, Math.max(MIN_FEE, fee));
    }

    /**
     * Distance en kilomètres entre deux points GPS (formule de Haversine).
     * Aucune dépendance externe requise.
     */
    public double haversineDistanceKm(double lat1, double lon1,
                                       double lat2, double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
