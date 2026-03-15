package com.open.spring.mvc.donation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DonationGraph} — adjacency list graph with
 * BFS recommendations, connected component detection, and influence ranking.
 *
 * @author Ahaan
 */
class DonationGraphTest {

    private DonationGraph graph;

    @BeforeEach
    void setUp() {
        graph = new DonationGraph();
        // Build a small donor→receiver network:
        //   alice -> bob, charlie
        //   bob   -> dave
        //   eve   -> frank
        graph.addEdge("alice", "bob");
        graph.addEdge("alice", "charlie");
        graph.addEdge("bob", "dave");
        graph.addEdge("eve", "frank");
    }

    @Nested
    @DisplayName("Graph Structure")
    class StructureTests {

        @Test
        @DisplayName("Node count includes all donors and receivers")
        void testNodeCount() {
            // alice, bob, charlie, dave, eve, frank = 6 nodes
            assertEquals(6, graph.nodeCount());
        }

        @Test
        @DisplayName("Edge count matches added edges")
        void testEdgeCount() {
            assertEquals(4, graph.edgeCount());
        }

        @Test
        @DisplayName("getNeighbors returns correct neighbors")
        void testGetNeighbors() {
            Set<String> aliceNeighbors = graph.getNeighbors("alice");
            assertEquals(2, aliceNeighbors.size());
            assertTrue(aliceNeighbors.contains("bob"));
            assertTrue(aliceNeighbors.contains("charlie"));
        }

        @Test
        @DisplayName("getNeighbors returns empty set for leaf nodes")
        void testLeafNodeNeighbors() {
            assertTrue(graph.getNeighbors("charlie").isEmpty());
            assertTrue(graph.getNeighbors("dave").isEmpty());
        }

        @Test
        @DisplayName("getNeighbors returns empty set for unknown nodes")
        void testUnknownNodeNeighbors() {
            assertTrue(graph.getNeighbors("unknown").isEmpty());
        }
    }

    @Nested
    @DisplayName("BFS Recommendations")
    class BfsTests {

        @Test
        @DisplayName("BFS depth 1 returns direct neighbors")
        void testBfsDepth1() {
            List<String> recs = graph.bfsRecommendations("alice", 1);
            assertTrue(recs.contains("bob"));
            assertTrue(recs.contains("charlie"));
            assertFalse(recs.contains("dave")); // depth 2
        }

        @Test
        @DisplayName("BFS depth 2 returns transitive neighbors")
        void testBfsDepth2() {
            List<String> recs = graph.bfsRecommendations("alice", 2);
            assertTrue(recs.contains("bob"));
            assertTrue(recs.contains("charlie"));
            assertTrue(recs.contains("dave")); // reachable via bob
            assertFalse(recs.contains("eve")); // disconnected component
        }

        @Test
        @DisplayName("BFS from isolated node returns empty list")
        void testBfsFromLeaf() {
            List<String> recs = graph.bfsRecommendations("frank", 3);
            assertTrue(recs.isEmpty()); // frank has no outgoing edges
        }

        @Test
        @DisplayName("BFS does not include start node")
        void testBfsExcludesStart() {
            List<String> recs = graph.bfsRecommendations("alice", 5);
            assertFalse(recs.contains("alice"));
        }
    }

    @Nested
    @DisplayName("Connected Components / Community Detection")
    class CommunityTests {

        @Test
        @DisplayName("Finds two separate communities")
        void testTwoCommunities() {
            List<Set<String>> communities = graph.findCommunities();
            assertEquals(2, communities.size());
        }

        @Test
        @DisplayName("Each community contains the expected nodes")
        void testCommunityContents() {
            List<Set<String>> communities = graph.findCommunities();
            // One community: {alice, bob, charlie, dave}
            // Other community: {eve, frank}
            boolean foundLarge = false;
            boolean foundSmall = false;
            for (Set<String> c : communities) {
                if (c.size() == 4) {
                    assertTrue(c.contains("alice"));
                    assertTrue(c.contains("dave"));
                    foundLarge = true;
                }
                if (c.size() == 2) {
                    assertTrue(c.contains("eve"));
                    assertTrue(c.contains("frank"));
                    foundSmall = true;
                }
            }
            assertTrue(foundLarge, "Should find the 4-node community");
            assertTrue(foundSmall, "Should find the 2-node community");
        }

        @Test
        @DisplayName("Single-node graph has one community")
        void testSingleNode() {
            DonationGraph g = new DonationGraph();
            g.addEdge("solo", "solo"); // self-loop
            assertEquals(1, g.findCommunities().size());
        }
    }

    @Nested
    @DisplayName("Influence Ranking")
    class InfluenceTests {

        @Test
        @DisplayName("Alice has highest influence (out-degree 2)")
        void testAliceTopInfluence() {
            Map<String, Integer> ranking = graph.influenceRanking(3);
            assertEquals(2, ranking.get("alice"));
        }

        @Test
        @DisplayName("Ranking is ordered by out-degree descending")
        void testRankingOrder() {
            Map<String, Integer> ranking = graph.influenceRanking(10);
            List<Integer> scores = new java.util.ArrayList<>(ranking.values());
            for (int i = 0; i < scores.size() - 1; i++) {
                assertTrue(scores.get(i) >= scores.get(i + 1));
            }
        }

        @Test
        @DisplayName("Limit parameter caps the result size")
        void testRankingLimit() {
            Map<String, Integer> ranking = graph.influenceRanking(1);
            assertEquals(1, ranking.size());
        }
    }

    @Test
    @DisplayName("getSummary returns all expected keys")
    void testGetSummary() {
        Map<String, Object> summary = graph.getSummary();
        assertTrue(summary.containsKey("nodes"));
        assertTrue(summary.containsKey("edges"));
        assertTrue(summary.containsKey("communities"));
        assertEquals(6, summary.get("nodes"));
        assertEquals(4, summary.get("edges"));
    }
}
