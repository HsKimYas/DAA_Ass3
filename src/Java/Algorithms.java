import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


public final class Algorithms {
    private Algorithms() {
    }

    /** DSU with path compression and union by size; return states. */
    static Map<String, Object> dsu(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] operations = Input.operations(input);

        int[] parent = new int[n];
        int[] size = new int[n];
        Arrays.fill(size, 1);
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        int components = n;
        int largest = n == 0 ? 0 : 1;
        List<List<Integer>> states = new ArrayList<>(operations.length);

        for (int[] operation : operations) {
            int a = find(parent, operation[0]);
            int b = find(parent, operation[1]);
            if (a != b) {
                if (size[a] < size[b]) {
                    int tmp = a;
                    a = b;
                    b = tmp;
                }
                parent[b] = a;
                size[a] += size[b];
                components--;
                largest = Math.max(largest, size[a]);
            }
            states.add(List.of(components, largest));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("states", states);
        return result;
    }

    /** Separate Kruskal implementation; return cost, edge_ids, components. */
    static Map<String, Object> kruskal(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        Integer[] order = new Integer[edges.length];
        for (int i = 0; i < edges.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingInt((Integer id) -> edges[id][2])
                .thenComparingInt(id -> id));

        int[] parent = new int[n];
        int[] size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }

        int components = n;
        long cost = 0L;
        List<Integer> chosen = new ArrayList<>(Math.max(0, n - 1));

        for (int id : order) {
            int u = edges[id][0];
            int v = edges[id][1];
            if (u == v) {
                continue;
            }
            int a = find(parent, u);
            int b = find(parent, v);
            if (a == b) {
                continue;
            }
            if (size[a] < size[b]) {
                int tmp = a;
                a = b;
                b = tmp;
            }
            parent[b] = a;
            size[a] += size[b];
            components--;
            cost += edges[id][2];
            chosen.add(id);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cost", cost);
        result.put("edge_ids", chosen);
        result.put("components", components);
        return result;
    }

    /** Separate Prim implementation; restart for every unvisited component. */
    static Map<String, Object> prim(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        List<List<WeightedEdge>> graph = buildWeightedGraph(n, edges);
        boolean[] visited = new boolean[n];
        PriorityQueue<Candidate> queue = new PriorityQueue<>(
                Comparator.comparingInt((Candidate c) -> c.weight)
                        .thenComparingInt(c -> c.edgeId)
                        .thenComparingInt(c -> c.from)
                        .thenComparingInt(c -> c.to));

        long cost = 0L;
        int components = 0;
        List<Integer> chosen = new ArrayList<>(Math.max(0, n - 1));

        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visited[start] = true;
            addOutgoing(graph, start, visited, queue);

            while (!queue.isEmpty()) {
                Candidate candidate = queue.poll();
                if (visited[candidate.to]) {
                    continue;
                }
                visited[candidate.to] = true;
                cost += candidate.weight;
                chosen.add(candidate.edgeId);
                addOutgoing(graph, candidate.to, visited, queue);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cost", cost);
        result.put("edge_ids", chosen);
        result.put("components", components);
        return result;
    }

    /** Kosaraju's two DFS passes; return component and exact condensation. */
    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        List<List<Integer>> graph = buildDirectedGraph(n, edges, false);
        List<List<Integer>> reverse = buildDirectedGraph(n, edges, true);

        boolean[] visited = new boolean[n];
        List<Integer> finishOrder = new ArrayList<>(n);
        for (int vertex = 0; vertex < n; vertex++) {
            if (!visited[vertex]) {
                iterativeDfsFinish(graph, vertex, visited, finishOrder);
            }
        }

        int[] component = new int[n];
        Arrays.fill(component, -1);
        int label = 0;
        for (int i = finishOrder.size() - 1; i >= 0; i--) {
            int start = finishOrder.get(i);
            if (component[start] != -1) {
                continue;
            }
            assignComponent(reverse, start, component, label++);
        }

        Set<Long> pairSet = new HashSet<>();
        List<List<Integer>> condensation = new ArrayList<>();
        for (int[] edge : edges) {
            int from = component[edge[0]];
            int to = component[edge[1]];
            if (from == to) {
                continue;
            }
            long key = (((long) from) << 32) ^ (to & 0xffffffffL);
            if (pairSet.add(key)) {
                condensation.add(List.of(from, to));
            }
        }

        List<Integer> labels = new ArrayList<>(n);
        for (int value : component) {
            labels.add(value);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("component", labels);
        result.put("condensation", condensation);
        return result;
    }

    /** Three-state DFS and reverse finish order; return acyclic and order. */
    static Map<String, Object> topo(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        TopologicalResult topological = topologicalOrder(n, edges);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("acyclic", topological.acyclic);
        result.put("order", topological.order);
        return result;
    }

    /** Whole-graph cycle check and DAG relaxation; return acyclic, distance, parent_edge. */
    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);

        TopologicalResult topological = topologicalOrder(n, edges);
        Map<String, Object> result = new LinkedHashMap<>();
        if (!topological.acyclic) {
            result.put("acyclic", false);
            result.put("distance", List.of());
            result.put("parent_edge", List.of());
            return result;
        }

        List<List<WeightedEdge>> graph = buildDirectedWeightedGraph(n, edges);
        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;

        for (int vertex : topological.order) {
            if (distance[vertex] == null) {
                continue;
            }
            for (WeightedEdge edge : graph.get(vertex)) {
                long candidate = distance[vertex] + edge.weight;
                if (distance[edge.to] == null || candidate < distance[edge.to]) {
                    distance[edge.to] = candidate;
                    parentEdge[edge.to] = edge.id;
                }
            }
        }

        List<Object> distanceOutput = new ArrayList<>(n);
        for (Long value : distance) {
            distanceOutput.add(value);
        }
        List<Integer> parentOutput = new ArrayList<>(n);
        for (int value : parentEdge) {
            parentOutput.add(value);
        }
        result.put("acyclic", true);
        result.put("distance", distanceOutput);
        result.put("parent_edge", parentOutput);
        return result;
    }

    /**
     * Reconstruct a vertex route from source to target using parent edge IDs.
     * Returns an empty list when target is unreachable; source itself gives [source].
     */
    public static List<Integer> reconstructPath(int source, int target,
                                                  int[] parentEdge, int[][] edges) {
        if (source < 0 || source >= parentEdge.length
                || target < 0 || target >= parentEdge.length) {
            throw new IllegalArgumentException("vertex outside parent array");
        }
        if (source == target) {
            return List.of(source);
        }

        ArrayList<Integer> reversed = new ArrayList<>();
        boolean[] seen = new boolean[parentEdge.length];
        int current = target;
        while (current != source) {
            if (seen[current]) {
                throw new IllegalArgumentException("parent edges contain a cycle");
            }
            seen[current] = true;
            int id = parentEdge[current];
            if (id < 0 || id >= edges.length) {
                return List.of();
            }
            int from = edges[id][0];
            int to = edges[id][1];
            if (to != current) {
                throw new IllegalArgumentException("parent edge does not end at current vertex");
            }
            reversed.add(current);
            current = from;
        }
        reversed.add(source);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private static int find(int[] parent, int vertex) {
        int root = vertex;
        while (root != parent[root]) {
            root = parent[root];
        }
        while (vertex != root) {
            int next = parent[vertex];
            parent[vertex] = root;
            vertex = next;
        }
        return root;
    }

    private static List<List<WeightedEdge>> buildWeightedGraph(int n, int[][] edges) {
        List<List<WeightedEdge>> graph = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        for (int id = 0; id < edges.length; id++) {
            int u = edges[id][0];
            int v = edges[id][1];
            int weight = edges[id][2];
            graph.get(u).add(new WeightedEdge(v, weight, id));
            if (u != v) {
                graph.get(v).add(new WeightedEdge(u, weight, id));
            }
        }
        return graph;
    }

    private static List<List<WeightedEdge>> buildDirectedWeightedGraph(int n, int[][] edges) {
        List<List<WeightedEdge>> graph = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        for (int id = 0; id < edges.length; id++) {
            int u = edges[id][0];
            int v = edges[id][1];
            int weight = edges[id][2];
            graph.get(u).add(new WeightedEdge(v, weight, id));
        }
        return graph;
    }

    private static void addOutgoing(List<List<WeightedEdge>> graph, int vertex,
                                    boolean[] visited, PriorityQueue<Candidate> queue) {
        for (WeightedEdge edge : graph.get(vertex)) {
            if (!visited[edge.to]) {
                queue.offer(new Candidate(vertex, edge.to, edge.weight, edge.id));
            }
        }
    }

    private static List<List<Integer>> buildDirectedGraph(int n, int[][] edges, boolean reverse) {
        List<List<Integer>> graph = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            int from = reverse ? edge[1] : edge[0];
            int to = reverse ? edge[0] : edge[1];
            graph.get(from).add(to);
        }
        return graph;
    }

    private static void iterativeDfsFinish(List<List<Integer>> graph, int start,
                                           boolean[] visited, List<Integer> finishOrder) {
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        visited[start] = true;
        stack.push(new Frame(start));
        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            List<Integer> neighbors = graph.get(frame.vertex);
            if (frame.nextIndex < neighbors.size()) {
                int next = neighbors.get(frame.nextIndex++);
                if (!visited[next]) {
                    visited[next] = true;
                    stack.push(new Frame(next));
                }
            } else {
                finishOrder.add(frame.vertex);
                stack.pop();
            }
        }
    }

    private static void assignComponent(List<List<Integer>> graph, int start,
                                         int[] component, int label) {
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        stack.push(start);
        component[start] = label;
        while (!stack.isEmpty()) {
            int vertex = stack.pop();
            for (int next : graph.get(vertex)) {
                if (component[next] == -1) {
                    component[next] = label;
                    stack.push(next);
                }
            }
        }
    }

    private static TopologicalResult topologicalOrder(int n, int[][] edges) {
        List<List<Integer>> graph = buildDirectedGraph(n, edges, false);
        byte[] state = new byte[n]; // 0 = unvisited, 1 = active, 2 = finished
        List<Integer> finishOrder = new ArrayList<>(n);

        for (int start = 0; start < n; start++) {
            if (state[start] != 0) {
                continue;
            }
            ArrayDeque<Frame> stack = new ArrayDeque<>();
            state[start] = 1;
            stack.push(new Frame(start));

            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                List<Integer> neighbors = graph.get(frame.vertex);
                boolean descended = false;
                while (frame.nextIndex < neighbors.size()) {
                    int next = neighbors.get(frame.nextIndex++);
                    if (state[next] == 1) {
                        return new TopologicalResult(false, List.of());
                    }
                    if (state[next] == 0) {
                        state[next] = 1;
                        stack.push(new Frame(next));
                        descended = true;
                        break;
                    }
                }
                if (!descended) {
                    state[frame.vertex] = 2;
                    finishOrder.add(frame.vertex);
                    stack.pop();
                }
            }
        }

        java.util.Collections.reverse(finishOrder);
        return new TopologicalResult(true, finishOrder);
    }

    private static final class WeightedEdge {
        final int to;
        final int weight;
        final int id;

        WeightedEdge(int to, int weight, int id) {
            this.to = to;
            this.weight = weight;
            this.id = id;
        }
    }

    private static final class Candidate {
        final int from;
        final int to;
        final int weight;
        final int edgeId;

        Candidate(int from, int to, int weight, int edgeId) {
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.edgeId = edgeId;
        }
    }

    private static final class Frame {
        final int vertex;
        int nextIndex;

        Frame(int vertex) {
            this.vertex = vertex;
        }
    }

    private static final class TopologicalResult {
        final boolean acyclic;
        final List<Integer> order;

        TopologicalResult(boolean acyclic, List<Integer> order) {
            this.acyclic = acyclic;
            this.order = order;
        }
    }
}