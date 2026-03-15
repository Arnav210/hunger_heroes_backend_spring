package com.open.spring.mvc.donation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graph-based model of donor→receiver relationships in the Hunger Heroes system.
 *
 * <p>Uses an <strong>adjacency list</strong> ({@code HashMap<String, Set<String>>})
 * to represent a directed graph where each edge connects a donor to a receiver
 * who accepted one of their donations.</p>
 *
 * <h3>Graph Algorithms Implemented</h3>
 * <ul>
 *   <li><strong>BFS (Breadth-First Search)</strong> — finds all donors reachable
 *       within {@code N} hops from a receiver, enabling "recommended donors"
 *       based on mutual connections. Time: O(V + E).</li>
 *   <li><strong>Connected Components</strong> — identifies independent donation
 *       communities (groups of donors/receivers who interact with each other
 *       but not with other groups). Time: O(V + E).</li>
 *   <li><strong>Influence Ranking</strong> — a simplified PageRank-style algorithm
 *       that ranks donors by the number of unique receivers they serve (out-degree).
 *       Time: O(V + E).</li>
 * </ul>
 *
 * <h3>Data Structures Used</h3>
 * <ul>
 *   <li>{@link HashMap} — adjacency list storage</li>
 *   <li>{@link HashSet} — neighbor sets, visited tracking</li>
 *   <li>{@link ArrayDeque} — BFS queue</li>
 *   <li>{@link LinkedHashMap} — ordered ranking results</li>
 * </ul>
 *
 * @author Ahaan
 * @version 1.0
 */
public class DonationGraph {

    /**
     * Adjacency list: donor → set of receivers who accepted their donations.
     * Demonstrates <strong>Graph</strong> data structure using adjacency list representation.
     */
    private final Map<String, Set<String>> adjacency = new HashMap<>();

    /**
     * All unique nodes in the graph (both donors and receivers).
     */
    private final Set<String> allNodes = new HashSet<>();

    /**
     * Adds a directed edge from donor to receiver.
     *
     * @param donor    the donor identifier (e.g. email)
     * @param receiver the receiver identifier (e.g. email or "accepted_by" value)
     */
    public void addEdge(String donor, String receiver) {
        adjacency.computeIfAbsent(donor, k -> new HashSet<>()).add(receiver);
        allNodes.add(donor);
        allNodes.add(receiver);
    }

    /**
     * Returns the neighbors (receivers) of a given donor.
     *
     * @param node the donor identifier
     * @return unmodifiable set of connected receivers
     */
    public Set<String> getNeighbors(String node) {
        return Collections.unmodifiableSet(adjacency.getOrDefault(node, Set.of()));
    }

    /**
     * Returns the number of unique nodes in the graph.
     *
     * @return node count
     */
    public int nodeCount() {
        return allNodes.size();
    }

    /**
     * Returns the number of edges in the graph.
     *
     * @return edge count
     */
    public int edgeCount() {
        return adjacency.values().stream().mapToInt(Set::size).sum();
    }

    // ═══════════════════════════════════════════════════════
    //  BFS — Breadth-First Search for Donor Recommendations
    //  Time: O(V + E)   Space: O(V)
    // ═══════════════════════════════════════════════════════

    /**
     * <strong>BFS-based donor recommendation.</strong>
     *
     * <p>Starting from a given node, performs a Breadth-First Search up to
     * {@code maxDepth} hops and returns all reachable nodes (excluding the
     * start node itself). This finds "donors who serve similar receivers"
     * or "receivers who share donors" — enabling community-based recommendations.</p>
     *
     * <p><strong>Complexity:</strong> O(V + E) time, O(V) space.</p>
     *
     * @param startNode the starting node (donor or receiver identifier)
     * @param maxDepth  maximum BFS depth (number of hops)
     * @return list of reachable nodes within maxDepth hops, ordered by discovery
     */
    public List<String> bfsRecommendations(String startNode, int maxDepth) {
        List<String> recommendations = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        // ArrayDeque used as a Queue for BFS traversal
        Deque<String> queue = new ArrayDeque<>();
        Map<String, Integer> depthMap = new HashMap<>();

        visited.add(startNode);
        queue.offer(startNode);
        depthMap.put(startNode, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depthMap.get(current);

            if (currentDepth >= maxDepth) continue;

            Set<String> neighbors = adjacency.getOrDefault(current, Set.of());
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                    depthMap.put(neighbor, currentDepth + 1);
                    recommendations.add(neighbor);
                }
            }
        }

        return recommendations;
    }

    // ═══════════════════════════════════════════════════════
    //  Connected Components — Community Detection
    //  Time: O(V + E)   Space: O(V)
    // ═══════════════════════════════════════════════════════

    /**
     * <strong>Connected Components — Community Detection.</strong>
     *
     * <p>Finds all connected components in the undirected version of the graph,
     * treating each component as a "donation community" — a group of donors
     * and receivers who interact with each other but not with other groups.</p>
     *
     * <p><strong>Complexity:</strong> O(V + E) time, O(V) space.</p>
     *
     * @return list of components, each component is a set of node identifiers
     */
    public List<Set<String>> findCommunities() {
        // Build undirected adjacency for community detection
        Map<String, Set<String>> undirected = new HashMap<>();
        for (String node : allNodes) {
            undirected.putIfAbsent(node, new HashSet<>());
        }
        for (Map.Entry<String, Set<String>> entry : adjacency.entrySet()) {
            String from = entry.getKey();
            for (String to : entry.getValue()) {
                undirected.computeIfAbsent(from, k -> new HashSet<>()).add(to);
                undirected.computeIfAbsent(to, k -> new HashSet<>()).add(from);
            }
        }

        Set<String> visited = new HashSet<>();
        List<Set<String>> communities = new ArrayList<>();

        for (String node : allNodes) {
            if (!visited.contains(node)) {
                Set<String> component = new HashSet<>();
                // BFS to find all nodes in this component
                Deque<String> queue = new ArrayDeque<>();
                queue.offer(node);
                visited.add(node);
                while (!queue.isEmpty()) {
                    String current = queue.poll();
                    component.add(current);
                    for (String neighbor : undirected.getOrDefault(current, Set.of())) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.offer(neighbor);
                        }
                    }
                }
                communities.add(component);
            }
        }

        return communities;
    }

    // ═══════════════════════════════════════════════════════
    //  Influence Ranking — PageRank-style Donor Scoring
    //  Time: O(V + E)   Space: O(V)
    // ═══════════════════════════════════════════════════════

    /**
     * <strong>Influence Ranking — simplified PageRank-style scoring.</strong>
     *
     * <p>Ranks donors by the number of unique receivers they've served
     * (out-degree in the graph). Higher out-degree = more influential donor
     * in the food donation network.</p>
     *
     * <p><strong>Complexity:</strong> O(V + E) time, O(V) space.</p>
     *
     * @param limit maximum number of top donors to return
     * @return ordered map of donor → influence score (out-degree), highest first
     */
    public Map<String, Integer> influenceRanking(int limit) {
        // Calculate out-degree for each donor
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : adjacency.entrySet()) {
            entries.add(Map.entry(entry.getKey(), entry.getValue().size()));
        }

        // Sort by out-degree descending (Comparator usage)
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        // Take top N
        Map<String, Integer> result = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Integer> e : entries) {
            if (count >= limit) break;
            result.put(e.getKey(), e.getValue());
            count++;
        }
        return result;
    }

    /**
     * Returns a summary of the graph for API responses.
     *
     * @return map containing node count, edge count, and community count
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("nodes", nodeCount());
        summary.put("edges", edgeCount());
        summary.put("communities", findCommunities().size());
        return summary;
    }
}
