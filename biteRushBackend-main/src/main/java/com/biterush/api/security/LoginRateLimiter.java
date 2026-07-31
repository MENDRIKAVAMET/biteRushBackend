package com.biterush.api.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Limiteur de débit en mémoire pour /auth/login.
 *
 * Décision de conception : pas de dépendance externe (Bucket4j, Resilience4j...) —
 * aucun accès Maven Central confirmé dans cet environnement sur plusieurs sessions,
 * impossible de vérifier qu'une nouvelle dépendance se résoudrait et compilerait.
 * Implémentation "fenêtre glissante" maison avec une simple structure en mémoire,
 * sans dépendance supplémentaire dans le pom.xml.
 *
 * Limite connue et assumée : purement en mémoire, donc par instance. Si l'application
 * tourne un jour sur plusieurs instances derrière un load balancer, la limite réelle
 * sera "N tentatives par instance" et non "N tentatives globales" — un rate limiting
 * distribué (Redis, etc.) serait nécessaire pour ce cas. Non traité ici, hors
 * périmètre pour une seule instance.
 *
 * Décision de portée : le comptage se fait par IP cliente (pas par email), et compte
 * TOUTES les tentatives de login (pas seulement les échecs). Raisonnement : compter
 * uniquement les échecs demanderait de faire remonter l'information jusqu'ici depuis
 * AuthService après tentative d'authentification, ce qui complique le filtre (qui
 * intervient avant le contrôleur) sans bénéfice de sécurité réel contre le
 * bruteforce - un attaquant qui devine le mot de passe au premier essai n'a de toute
 * façon pas besoin d'en faire plusieurs. Compter par IP plutôt que par email protège
 * aussi contre l'énumération de comptes (un attaquant qui teste plein d'emails
 * différents depuis la même IP est également freiné).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    /**
     * Enregistre une tentative pour la clé donnée (typiquement l'IP cliente) et
     * indique si elle est autorisée.
     *
     * @return true si la tentative est autorisée (sous la limite), false si la
     *         limite est dépassée (la tentative n'est PAS comptée dans ce cas, pour
     *         ne pas repousser indéfiniment la fenêtre si le client retente en boucle).
     */
    public boolean tryAcquire(String key) {

        Instant now = Instant.now();
        Instant windowStart = now.minus(WINDOW);

        Deque<Instant> attempts = attemptsByKey.computeIfAbsent(
                key, k -> new ConcurrentLinkedDeque<>()
        );

        synchronized (attempts) {
            // Purge des tentatives hors fenêtre
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(windowStart)) {
                attempts.pollFirst();
            }

            if (attempts.size() >= MAX_ATTEMPTS) {
                return false;
            }

            attempts.addLast(now);
            return true;
        }
    }

    /**
     * Secondes avant la prochaine tentative autorisée pour cette clé, pour le header
     * Retry-After. 0 si aucune tentative enregistrée (ne devrait pas être appelée
     * dans ce cas côté filtre, mais robuste par défaut).
     */
    public long secondsUntilRetry(String key) {

        Deque<Instant> attempts = attemptsByKey.get(key);
        if (attempts == null || attempts.isEmpty()) {
            return 0;
        }

        Instant oldest;
        synchronized (attempts) {
            oldest = attempts.peekFirst();
        }
        if (oldest == null) {
            return 0;
        }

        Instant retryAt = oldest.plus(WINDOW);
        long seconds = Duration.between(Instant.now(), retryAt).getSeconds();
        return Math.max(seconds, 1);
    }
}
