import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Reproducible Kruskal-vs-Prim benchmark. Compilation is intentionally outside the timed region.
 * The same generated graph is supplied to both algorithms; graph generation is also outside timing.
 */
public final class Benchmark {
    private static final int[] SIZES = {1_000, 10_000, 50_000};
    private static final int[] EDGES = {5_000, 50_000, 250_000};
    private static final int WARMUPS = 2;
    private static final int TRIALS = 5;
    private static final long SEED = 20261025L;

    public static void main(String[] args) {
        System.out.println("seed,n,m,method,trial,time_ms,cost,components");
        Random random = new Random(SEED);
        for (int sizeIndex = 0; sizeIndex < SIZES.length; sizeIndex++) {
            int n = SIZES[sizeIndex];
            int m = EDGES[sizeIndex];
            Map<String, Object> request = makeGraph(n, m, random);

            // Warm up both methods on the exact same graph.
            for (int i = 0; i < WARMUPS; i++) {
                Algorithms.kruskal(request);
                Algorithms.prim(request);
            }

            for (int trial = 1; trial <= TRIALS; trial++) {
                long start = System.nanoTime();
                Map<String, Object> kruskal = Algorithms.kruskal(request);
                long elapsed = System.nanoTime() - start;
                System.out.printf("%d,%d,%d,kruskal,%d,%.3f,%d,%d%n",
                        SEED, n, m, trial, elapsed / 1_000_000.0,
                        ((Number) kruskal.get("cost")).longValue(),
                        ((Number) kruskal.get("components")).intValue());

                start = System.nanoTime();
                Map<String, Object> prim = Algorithms.prim(request);
                elapsed = System.nanoTime() - start;
                System.out.printf("%d,%d,%d,prim,%d,%.3f,%d,%d%n",
                        SEED, n, m, trial, elapsed / 1_000_000.0,
                        ((Number) prim.get("cost")).longValue(),
                        ((Number) prim.get("components")).intValue());
            }
        }
    }

    private static Map<String, Object> makeGraph(int n, int m, Random random) {
        List<List<Integer>> edges = new ArrayList<>(m);
        // A path makes the graph connected while keeping the remaining edges random.
        for (int vertex = 1; vertex < n; vertex++) {
            int weight = random.nextInt(2_000_001) - 1_000_000;
            edges.add(List.of(vertex - 1, vertex, weight));
        }
        while (edges.size() < m) {
            int u = random.nextInt(n);
            int v = random.nextInt(n);
            int weight = random.nextInt(2_000_001) - 1_000_000;
            edges.add(List.of(u, v, weight));
        }

        Map<String, Object> request = new HashMap<>();
        request.put("task", "kruskal");
        request.put("n", n);
        request.put("edges", edges);
        return request;
    }
}
