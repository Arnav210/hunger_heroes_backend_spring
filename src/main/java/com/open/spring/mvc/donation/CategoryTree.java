package com.open.spring.mvc.donation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tree-based hierarchical classification of food categories.
 *
 * <p>Implements a <strong>general tree</strong> (N-ary tree) where each node
 * represents a food category. The root node "All Food" branches into
 * top-level groups (Perishable, Non-Perishable, Prepared), which further
 * branch into the specific categories used by the donation system.</p>
 *
 * <h3>Tree Operations</h3>
 * <ul>
 *   <li><strong>DFS Traversal</strong> — pre-order traversal to list all
 *       categories in hierarchical order. Time: O(n).</li>
 *   <li><strong>Search</strong> — finds a node by name using DFS. Time: O(n).</li>
 *   <li><strong>Path to Root</strong> — traces the ancestry chain from a
 *       leaf category to the root. Time: O(h) where h = tree height.</li>
 *   <li><strong>Count Leaves</strong> — counts terminal categories (leaves).
 *       Time: O(n).</li>
 * </ul>
 *
 * <p><strong>Data Structures Used:</strong></p>
 * <ul>
 *   <li>{@code List<CategoryNode>} — children list for each tree node</li>
 *   <li>{@code ArrayList} — traversal result accumulation</li>
 *   <li>{@code LinkedHashMap} — ordered tree-to-JSON conversion</li>
 * </ul>
 *
 * @author Ahaan
 * @version 1.0
 */
public class CategoryTree {

    /**
     * Represents a single node in the category tree.
     * Each node has a name, optional description, and a list of children.
     */
    public static class CategoryNode {
        private final String name;
        private final String description;
        private final List<CategoryNode> children = new ArrayList<>();
        private CategoryNode parent;

        /**
         * Creates a new category node.
         *
         * @param name        the category name
         * @param description a short description
         */
        public CategoryNode(String name, String description) {
            this.name = name;
            this.description = description;
        }

        /**
         * Adds a child node to this category.
         *
         * @param child the child category node
         * @return the child node (for chaining)
         */
        public CategoryNode addChild(CategoryNode child) {
            child.parent = this;
            children.add(child);
            return child;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<CategoryNode> getChildren() { return children; }
        public CategoryNode getParent() { return parent; }
        public boolean isLeaf() { return children.isEmpty(); }

        /**
         * Returns the depth of this node in the tree (root = 0).
         *
         * @return the depth level
         */
        public int getDepth() {
            int depth = 0;
            CategoryNode current = this;
            while (current.parent != null) {
                depth++;
                current = current.parent;
            }
            return depth;
        }
    }

    /** The root of the category tree. */
    private final CategoryNode root;

    /**
     * Constructs the default Hunger Heroes food category tree.
     * The hierarchy mirrors the categories in {@link DonationConstants#ALLOWED_CATEGORIES}.
     */
    public CategoryTree() {
        root = new CategoryNode("All Food", "Root category for all food donations");

        // Level 1: Top-level groupings
        CategoryNode perishable = root.addChild(
            new CategoryNode("Perishable", "Foods that require temperature control"));
        CategoryNode nonPerishable = root.addChild(
            new CategoryNode("Non-Perishable", "Shelf-stable foods"));
        CategoryNode prepared = root.addChild(
            new CategoryNode("Prepared & Specialty", "Ready-to-eat and specialty items"));

        // Level 2: Specific categories under Perishable
        perishable.addChild(new CategoryNode("dairy", "Milk, cheese, yogurt, and other dairy products"));
        perishable.addChild(new CategoryNode("meat-protein", "Beef, chicken, fish, tofu, and protein sources"));
        perishable.addChild(new CategoryNode("fresh-produce", "Fruits, vegetables, and fresh herbs"));
        perishable.addChild(new CategoryNode("frozen", "Frozen meals, vegetables, and ice cream"));

        // Level 2: Specific categories under Non-Perishable
        nonPerishable.addChild(new CategoryNode("canned", "Canned soups, vegetables, and fruits"));
        nonPerishable.addChild(new CategoryNode("grains", "Rice, pasta, flour, and cereals"));
        nonPerishable.addChild(new CategoryNode("beverages", "Water, juice, coffee, and tea"));
        nonPerishable.addChild(new CategoryNode("snacks", "Chips, crackers, granola bars, and trail mix"));

        // Level 2: Specific categories under Prepared & Specialty
        prepared.addChild(new CategoryNode("prepared-meals", "Ready-to-eat meals and meal kits"));
        prepared.addChild(new CategoryNode("bakery", "Bread, pastries, cakes, and baked goods"));
        prepared.addChild(new CategoryNode("baby-food", "Formula, baby cereal, and purees"));
        prepared.addChild(new CategoryNode("other", "Condiments, spices, and miscellaneous items"));
    }

    /**
     * Returns the root node of the tree.
     *
     * @return the root {@link CategoryNode}
     */
    public CategoryNode getRoot() {
        return root;
    }

    // ═══════════════════════════════════════════════════════
    //  DFS Pre-Order Traversal — O(n)
    // ═══════════════════════════════════════════════════════

    /**
     * <strong>DFS Pre-Order Traversal</strong> of the category tree.
     *
     * <p>Visits each node before its children, producing a hierarchical
     * listing of all categories. Time: O(n) where n = number of nodes.</p>
     *
     * @return list of category names in pre-order
     */
    public List<String> preOrderTraversal() {
        List<String> result = new ArrayList<>();
        preOrderHelper(root, result);
        return result;
    }

    private void preOrderHelper(CategoryNode node, List<String> result) {
        result.add(node.getName());
        for (CategoryNode child : node.getChildren()) {
            preOrderHelper(child, result);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Search — DFS find by name — O(n)
    // ═══════════════════════════════════════════════════════

    /**
     * Searches the tree for a node with the given name using DFS.
     *
     * <p><strong>Complexity:</strong> O(n) worst case.</p>
     *
     * @param name the category name to search for
     * @return the matching {@link CategoryNode}, or {@code null} if not found
     */
    public CategoryNode search(String name) {
        return searchHelper(root, name);
    }

    private CategoryNode searchHelper(CategoryNode node, String name) {
        if (node.getName().equalsIgnoreCase(name)) return node;
        for (CategoryNode child : node.getChildren()) {
            CategoryNode found = searchHelper(child, name);
            if (found != null) return found;
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════
    //  Path to Root — O(h) where h = height
    // ═══════════════════════════════════════════════════════

    /**
     * Returns the path from a named category up to the root.
     *
     * <p><strong>Complexity:</strong> O(n) for the search + O(h) to walk up.</p>
     *
     * @param categoryName the leaf/node category name
     * @return list of category names from the node to the root, or empty if not found
     */
    public List<String> pathToRoot(String categoryName) {
        CategoryNode node = search(categoryName);
        if (node == null) return List.of();
        List<String> path = new ArrayList<>();
        while (node != null) {
            path.add(node.getName());
            node = node.getParent();
        }
        return path;
    }

    // ═══════════════════════════════════════════════════════
    //  Count Leaves — O(n)
    // ═══════════════════════════════════════════════════════

    /**
     * Counts the number of leaf nodes (terminal categories) in the tree.
     *
     * <p><strong>Complexity:</strong> O(n).</p>
     *
     * @return number of leaf categories
     */
    public int countLeaves() {
        return countLeavesHelper(root);
    }

    private int countLeavesHelper(CategoryNode node) {
        if (node.isLeaf()) return 1;
        int count = 0;
        for (CategoryNode child : node.getChildren()) {
            count += countLeavesHelper(child);
        }
        return count;
    }

    /**
     * Converts the tree to a nested map structure suitable for JSON serialisation.
     *
     * @return nested map representing the tree hierarchy
     */
    public Map<String, Object> toMap() {
        return nodeToMap(root);
    }

    private Map<String, Object> nodeToMap(CategoryNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", node.getName());
        map.put("description", node.getDescription());
        map.put("depth", node.getDepth());
        map.put("isLeaf", node.isLeaf());
        if (!node.isLeaf()) {
            List<Map<String, Object>> childMaps = new ArrayList<>();
            for (CategoryNode child : node.getChildren()) {
                childMaps.add(nodeToMap(child));
            }
            map.put("children", childMaps);
        }
        return map;
    }
}
