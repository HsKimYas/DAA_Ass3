import java.util.List;

/** Direct tests for the required route-reconstruction helper. */
public final class RouteHelperTest {
    public static void main(String[] args) {
        int[][] sample = {
                {0, 1, 3}, {0, 2, 8}, {1, 2, -7}, {2, 3, 4}
        };
        int[] sampleParents = {-1, 0, 2, 3, -1};
        check(List.of(0, 1, 2, 3),
                Algorithms.reconstructPath(0, 3, sampleParents, sample), "reachable target");
        check(List.of(0),
                Algorithms.reconstructPath(0, 0, sampleParents, sample), "source target");
        check(List.of(),
                Algorithms.reconstructPath(0, 4, sampleParents, sample), "unreachable target");

        int[][] parallel = {
                {0, 1, 5}, {0, 1, 5}, {1, 2, 2}
        };
        int[] parallelParents = {-1, 1, 2};
        check(List.of(0, 1, 2),
                Algorithms.reconstructPath(0, 2, parallelParents, parallel),
                "equal parallel edges");

        System.out.println("PASS route helper tests");
    }

    private static void check(List<Integer> expected, List<Integer> actual, String name) {
        if (!expected.equals(actual)) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }
}
