package com.open.spring.mvc.hungerheroes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Hunger Heroes Game Leaderboard.
 *
 * Endpoints:
 *   GET  /api/hunger-heroes/leaderboard              → all scores (optional ?levelId=&limit=)
 *   GET  /api/hunger-heroes/leaderboard/top/{limit}   → top N scores
 *   POST /api/hunger-heroes/leaderboard               → submit score (requires auth)
 *   GET  /api/hunger-heroes/leaderboard/user/{personId} → user's scores
 *
 * Pattern reference: gamespring2 rpg/games/GameApiController.java
 *                    gamespring2 leaderboard/LeaderboardController.java
 *
 * CORS: Allow all origins for GET, require auth for POST.
 */
@RestController
@RequestMapping("/api/hunger-heroes")
@CrossOrigin(
    origins = "*",
    allowedHeaders = "*",
    methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.OPTIONS
    }
)
public class HungerHeroScoreController {

    @Autowired
    private HungerHeroScoreRepository repo;

    /**
     * GET /api/hunger-heroes/leaderboard
     * Optional query params: ?levelId=hunger-heroes-foodbank&limit=50
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<HungerHeroScore>> getLeaderboard(
            @RequestParam(required = false) String levelId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<HungerHeroScore> scores;
            if (levelId != null && !levelId.isEmpty()) {
                scores = repo.findByLevelOrderByScoreDesc(levelId)
                        .stream()
                        .limit(limit)
                        .collect(Collectors.toList());
            } else {
                scores = repo.findAllOrderByScoreDesc()
                        .stream()
                        .limit(limit)
                        .collect(Collectors.toList());
            }
            return ResponseEntity.ok(scores != null ? scores : List.of());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * GET /api/hunger-heroes/leaderboard/top/{limit}
     */
    @GetMapping("/leaderboard/top/{limit}")
    public ResponseEntity<List<HungerHeroScore>> getTopScores(
            @PathVariable int limit) {
        List<HungerHeroScore> scores = repo.findAllOrderByScoreDesc()
                .stream()
                .limit(Math.min(limit, 100))
                .collect(Collectors.toList());
        return ResponseEntity.ok(scores != null ? scores : List.of());
    }

    /**
     * POST /api/hunger-heroes/leaderboard
     *
     * Expects JSON body:
     * {
     *   "playerName": "HeroPlayer",
     *   "npcsVisited": 5,
     *   "dialoguesCompleted": 23,
     *   "timePlayedSeconds": 180,
     *   "levelId": "hunger-heroes-foodbank",
     *   "details": "{\"bonus\": true}"
     * }
     *
     * Score is computed SERVER-SIDE — do not trust the client.
     */
    @PostMapping("/leaderboard")
    public ResponseEntity<?> submitScore(@RequestBody HungerHeroScore score) {
        try {
            // Validate required fields
            if (score.getNpcsVisited() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "npcsVisited is required"));
            }
            if (score.getDialoguesCompleted() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "dialoguesCompleted is required"));
            }

            // Clamp values to valid ranges
            score.setNpcsVisited(Math.min(5, Math.max(0, score.getNpcsVisited())));
            score.setDialoguesCompleted(Math.max(0, score.getDialoguesCompleted()));
            score.setTimePlayedSeconds(
                score.getTimePlayedSeconds() != null ? Math.max(0, score.getTimePlayedSeconds()) : 0
            );

            // Server-side score calculation (never trust client)
            score.computeScore();

            // Set timestamp
            score.setCreatedAt(LocalDateTime.now());

            // Default level ID
            if (score.getLevelId() == null || score.getLevelId().isEmpty()) {
                score.setLevelId("hunger-heroes-foodbank");
            }

            HungerHeroScore saved = repo.save(score);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "Failed to save score: " + e.getMessage()));
        }
    }

    /**
     * GET /api/hunger-heroes/leaderboard/user/{personId}
     */
    @GetMapping("/leaderboard/user/{personId}")
    public ResponseEntity<List<HungerHeroScore>> getUserScores(
            @PathVariable Long personId) {
        List<HungerHeroScore> scores = repo.findByPersonIdOrderByCreatedAtDesc(personId);
        return ResponseEntity.ok(scores != null ? scores : List.of());
    }
}
