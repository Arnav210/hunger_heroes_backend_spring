package com.open.spring.mvc.donation;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CategoryTree} — N-ary tree representing the
 * hierarchical food category classification system.
 *
 * @author Ahaan
 */
class CategoryTreeTest {

    private CategoryTree tree;

    @BeforeEach
    void setUp() {
        tree = new CategoryTree();
    }

    @Nested
    @DisplayName("Tree Structure")
    class StructureTests {

        @Test
        @DisplayName("Root node is 'All Food'")
        void testRootName() {
            assertEquals("All Food", tree.getRoot().getName());
        }

        @Test
        @DisplayName("Root has 3 top-level children")
        void testRootChildren() {
            assertEquals(3, tree.getRoot().getChildren().size());
        }

        @Test
        @DisplayName("Perishable group has 4 leaf categories")
        void testPerishableChildren() {
            CategoryTree.CategoryNode perishable = tree.getRoot().getChildren().get(0);
            assertEquals("Perishable", perishable.getName());
            assertEquals(4, perishable.getChildren().size());
        }

        @Test
        @DisplayName("Total leaf count matches DonationConstants category count")
        void testLeafCount() {
            // 12 categories in DonationConstants.ALLOWED_CATEGORIES
            assertEquals(12, tree.countLeaves());
        }

        @Test
        @DisplayName("Root depth is 0")
        void testRootDepth() {
            assertEquals(0, tree.getRoot().getDepth());
        }

        @Test
        @DisplayName("Leaf category depth is 2")
        void testLeafDepth() {
            CategoryTree.CategoryNode dairy = tree.search("dairy");
            assertNotNull(dairy);
            assertEquals(2, dairy.getDepth());
        }

        @Test
        @DisplayName("Root is not a leaf")
        void testRootNotLeaf() {
            assertFalse(tree.getRoot().isLeaf());
        }

        @Test
        @DisplayName("Terminal categories are leaves")
        void testLeafCategories() {
            assertTrue(tree.search("canned").isLeaf());
            assertTrue(tree.search("bakery").isLeaf());
        }
    }

    @Nested
    @DisplayName("DFS Search")
    class SearchTests {

        @Test
        @DisplayName("Search finds existing category")
        void testSearchFound() {
            CategoryTree.CategoryNode result = tree.search("frozen");
            assertNotNull(result);
            assertEquals("frozen", result.getName());
        }

        @Test
        @DisplayName("Search is case-insensitive")
        void testSearchCaseInsensitive() {
            assertNotNull(tree.search("DAIRY"));
            assertNotNull(tree.search("Bakery"));
        }

        @Test
        @DisplayName("Search returns null for non-existent category")
        void testSearchNotFound() {
            assertNull(tree.search("pizza"));
        }

        @Test
        @DisplayName("Search finds intermediate nodes")
        void testSearchIntermediate() {
            CategoryTree.CategoryNode perishable = tree.search("Perishable");
            assertNotNull(perishable);
            assertFalse(perishable.isLeaf());
        }
    }

    @Nested
    @DisplayName("Pre-Order Traversal")
    class TraversalTests {

        @Test
        @DisplayName("Traversal starts with root")
        void testTraversalStartsWithRoot() {
            List<String> traversal = tree.preOrderTraversal();
            assertEquals("All Food", traversal.get(0));
        }

        @Test
        @DisplayName("Traversal includes all nodes")
        void testTraversalIncludesAll() {
            List<String> traversal = tree.preOrderTraversal();
            // root (1) + 3 groups + 12 leaves = 16
            assertEquals(16, traversal.size());
        }

        @Test
        @DisplayName("Traversal visits parent before children (pre-order)")
        void testPreOrderProperty() {
            List<String> traversal = tree.preOrderTraversal();
            int perishableIdx = traversal.indexOf("Perishable");
            int dairyIdx = traversal.indexOf("dairy");
            assertTrue(perishableIdx < dairyIdx, "Parent should appear before child in pre-order");
        }
    }

    @Nested
    @DisplayName("Path to Root")
    class PathTests {

        @Test
        @DisplayName("Path from leaf to root is correct")
        void testLeafPath() {
            List<String> path = tree.pathToRoot("dairy");
            assertEquals(3, path.size());
            assertEquals("dairy", path.get(0));
            assertEquals("Perishable", path.get(1));
            assertEquals("All Food", path.get(2));
        }

        @Test
        @DisplayName("Path from root to root is just root")
        void testRootPath() {
            List<String> path = tree.pathToRoot("All Food");
            assertEquals(1, path.size());
            assertEquals("All Food", path.get(0));
        }

        @Test
        @DisplayName("Path for non-existent category is empty")
        void testNonExistentPath() {
            List<String> path = tree.pathToRoot("pizza");
            assertTrue(path.isEmpty());
        }

        @Test
        @DisplayName("Path for bakery goes through Prepared group")
        void testBakeryPath() {
            List<String> path = tree.pathToRoot("bakery");
            assertEquals("bakery", path.get(0));
            assertEquals("Prepared & Specialty", path.get(1));
            assertEquals("All Food", path.get(2));
        }
    }

    @Nested
    @DisplayName("Tree to Map (JSON)")
    class ToMapTests {

        @Test
        @DisplayName("toMap includes name, description, depth, isLeaf")
        void testMapKeys() {
            Map<String, Object> map = tree.toMap();
            assertTrue(map.containsKey("name"));
            assertTrue(map.containsKey("description"));
            assertTrue(map.containsKey("depth"));
            assertTrue(map.containsKey("isLeaf"));
        }

        @Test
        @DisplayName("Root node has children in map")
        void testMapHasChildren() {
            Map<String, Object> map = tree.toMap();
            assertTrue(map.containsKey("children"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) map.get("children");
            assertEquals(3, children.size());
        }
    }
}
