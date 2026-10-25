# Assignment 3 — Graph Algorithms Analysis

## 1. Implementation overview

The solution is implemented in Java and keeps the supplied JSON Lines interface unchanged. `submission.json` selects `Java`. All algorithm state is allocated inside the corresponding method, so requests are independent even when the same JVM processes many input lines.

Implemented methods in `src/Java/Algorithms.java`:

- `dsu`: disjoint-set union with path compression and union by size.
- `kruskal`: minimum spanning forest using sorted edge IDs and DSU.
- `prim`: minimum spanning forest using an adjacency list and a priority queue, restarting at every unvisited vertex.
- `scc`: Kosaraju's algorithm with two **iterative** DFS passes and a transposed graph.
- `topo`: three-state DFS with an explicit stack and a stored next-neighbor position.
- `dag`: whole-graph cycle detection through the same DFS/topological-order routine, followed by shortest-path relaxation in topological order.
- `reconstructPath`: helper that follows selected parent edge IDs backwards and returns the vertex sequence from source to target. An unreachable target returns an empty list; the source itself returns `[source]`.

The Java source is compatible with Java 17. The local benchmark environment used OpenJDK 21 with `javac --release 17`; this does not change the Java language target of the source.

## 2. DSU

### Steps

1. Initially set `parent[v] = v` and `size[v] = 1` for every vertex.
2. For every operation `(u,v)`, find both roots with path compression.
3. If the roots differ, attach the smaller tree to the larger tree, decrease the component count, and update the largest component size.
4. Append `[components, largest]` after every operation, including redundant and self-unions.

### Invariant and correctness

Every root represents exactly one current connected component, and `size[root]` is its number of vertices. Path compression changes only parent pointers toward the same root, so it does not change the partition. Union by size joins two different components and updates both the component count and the size of the new root. Therefore every reported state is the required state after that operation.

### Edge cases

`n=0` returns no states. Self-unions and repeated unions leave the partition unchanged. The largest component is `0` for an empty DSU and `1` initially for a non-empty DSU.

### Complexity

Amortized time: `O((n+q) alpha(n))`.
Memory: `O(n+q)` including returned states.

## 3. Kruskal

### Steps

1. Keep each input edge's original index as its `edge_id`.
2. Sort edge IDs by `(weight, edge_id)`.
3. Scan the sorted IDs. Ignore loops. Add an edge exactly when its endpoints currently belong to different DSU components.
4. Add its weight to a Java `long`, record the original ID, and decrement the component count.

### Invariant and correctness

At every scan position, the chosen edges are acyclic. By the cut property, the lightest edge crossing two different current components can safely be added to an MST of the corresponding component. Continuing until no more safe edges exist gives a minimum spanning forest. Isolated vertices remain separate components.

### Edge cases

Parallel edges, negative weights, equal weights, loops, isolated vertices, `n=0`, and sums beyond 32-bit range are handled. Loops are never selected. Original IDs are retained after sorting.

### Complexity

Sorting dominates: `O(m log(m+1))`, plus DSU work. Overall `O(n + m log(m+1))` time and `O(n+m)` memory.

## 4. Prim

### Steps

1. Build an undirected adjacency list. A non-loop edge is stored in both endpoint lists; a loop is not useful for the MST and cannot introduce a new vertex.
2. For every unvisited start vertex, begin a new component and put all outgoing candidate edges into a min-priority queue.
3. Repeatedly remove the lightest candidate. If its destination is already visited, discard it; otherwise accept it, mark the destination visited, add its weight, record its original ID, and insert its outgoing candidates.
4. Restart from the next unvisited vertex so disconnected graphs produce a minimum spanning forest.

### Invariant and correctness

For the currently growing component, the visited vertices form a connected tree. Every queued edge crosses from the visited set to an unvisited vertex. The minimum such edge is safe by the cut property, so accepting it preserves optimality. Restarting on every unvisited vertex applies the same argument independently to every connected component.

### Edge cases

The implementation handles isolated vertices, loops, parallel edges, equal weights, negative weights, empty graphs, and 64-bit costs. The priority queue is empty before each new disconnected component starts.

### Complexity

With a lazy priority queue: `O(n + m log(m+1))` time and `O(n+m)` memory.

## 5. Kosaraju SCC

### Steps

1. Build the directed graph and its transpose.
2. Run the first DFS over the original graph. Instead of recursive calls, an explicit stack stores `(vertex, next-neighbor-index)` frames. A vertex is appended to `finishOrder` only after all its outgoing neighbors have been processed.
3. Process vertices in decreasing finishing time on the transposed graph using an explicit stack. All vertices reached in one pass receive the same SCC label.
4. Scan original edges. For every edge crossing two different labels, insert the label pair into a hash set so each condensation edge appears once.

### Invariant and correctness

The first pass produces the required decreasing finishing-time order. Kosaraju's theorem states that processing the transpose in that order discovers exactly one SCC per traversal. Thus equal labels are exactly mutual-reachability classes. The final edge scan is exactly the definition of the condensation graph, with internal edges and duplicate component pairs removed.

### Edge cases

Empty graphs, isolated vertices, duplicate edges, self-loops, and deep chains are handled. No recursive DFS is used, so a chain of 100,000 vertices does not depend on the JVM call stack.

### Complexity

`O(n+m)` expected time when hash-set operations are amortized constant time. Memory: `O(n+m)`.

## 6. Topological sorting

### Steps

1. Build the directed adjacency list.
2. Maintain three states: `0 = unvisited`, `1 = active`, `2 = finished`.
3. For every unvisited vertex, start an explicit DFS stack. Each frame stores the vertex and the next neighbor index to inspect.
4. An edge to an active vertex is a directed cycle, so return `acyclic=false` and an empty order immediately.
5. When a vertex has no remaining neighbors, mark it finished and append it to the finish list. Reverse that list at the end.

### Invariant and correctness

An active vertex is on the current DFS path. Therefore an edge from the current path to an active vertex closes a directed cycle. Edges to finished vertices cannot close a currently active DFS path. If no active back-edge exists, reversing DFS finishing order gives a valid topological order.

### Edge cases

Empty graphs return an empty order. Self-loops are detected as cycles. Disconnected components are all checked, including components unrelated to the first vertex.

### Complexity

`O(n+m)` time and `O(n+m)` memory. The explicit frame stack prevents repeated rescanning of adjacency lists and avoids recursion-depth overflow.

## 7. Shortest paths in a DAG

### Steps

1. Run the complete topological cycle check over the **whole graph**, not only vertices reachable from `source`.
2. If a cycle exists anywhere, return `acyclic=false` and two empty arrays.
3. Otherwise initialize `distance[source]=0` and all other distances to `null`; initialize every `parent_edge` to `-1`.
4. Process vertices in topological order. For a reachable vertex, relax each outgoing directed edge. A strictly smaller candidate distance replaces the old value and stores that edge's original ID.
5. Never relax from an unreachable vertex.

### Invariant and correctness

When a vertex is processed in topological order, every possible predecessor of that vertex has already been processed. Therefore its stored distance is already the shortest distance from the source. Relaxing its outgoing edges propagates these optimal values forward. Negative weights are safe because a DAG contains no cycle, so no repeated relaxation is required.

### Edge cases

The source has distance `0` and parent `-1`. An unreachable vertex has `distance=null` and parent `-1`; `-1` is never used as an unreachable distance. A cycle in a disconnected component still invalidates the whole DAG request. Parallel equal-weight edges may produce different but valid parent IDs.

### Complexity

Cycle detection plus relaxation is `O(n+m)` time and `O(n+m)` memory.

## 8. Route reconstruction helper

`reconstructPath(source, target, parentEdge, edges)` follows the selected input edge IDs from the target back toward the source. It verifies that each selected edge ends at the current vertex, prevents an accidental parent cycle with a `seen` array, reverses the collected vertices, and returns the source-to-target route.

The chosen contract for an unreachable target is an empty list. The source target returns `[source]`.

Direct test results from `tests/RouteHelperTest.java`:

- sample reachable target `3`: parent IDs `[-1, 0, 2, 3, -1]`, so the selected edge IDs are `0, 2, 3` and the route is `[0, 1, 2, 3]`; its weight is `3 + (-7) + 4 = 0`;
- source target `0`: route `[0]`; no edge is needed;
- unreachable target `4`: parent ID `-1`, therefore `[]`;
- equal parallel edges: parent IDs `[-1, 1, 2]`, selecting parallel edge ID `1` followed by edge ID `2`, giving `[0, 1, 2]` and total weight `7`.

Command:

```text
javac --release 17 -encoding UTF-8 -d .build/RouteTest src/Java/Algorithms.java src/Java/Input.java src/Java/Json.java src/Java/Main.java tests/RouteHelperTest.java
java -cp .build/RouteTest RouteHelperTest
```

The helper is `O(L)` time and `O(L)` auxiliary/result space for a route of `L` vertices.

## 9. State reset and deep DFS

No mutable algorithm data is stored in static fields. Every request creates new arrays, adjacency lists, queues, DSU structures, and result collections. The supplied `Main` keeps one JVM alive for multiple JSON Lines, so this design explicitly prevents state leakage between requests.

SCC and topological/DAG traversal use explicit stacks. A frame stores a vertex and the next adjacency position, which simulates recursive DFS without using the Java call stack. This is important for the assignment's deep-chain cases and was also tested at `n=100000`.

## 10. Tests

### Public checks

Command:

```text
python tools/run.py --setup
python tools/run.py --check
```

Result: all 36 public cases passed, and the complete public batch passed again when repeated twice in the same JVM process. This covers the published examples, edge cases, 64-bit sums, cycles outside the source's reachable region, and 2,000-vertex deep chains.

### Custom tests

Files:

- `tests/custom_cases.jsonl` — mixed cases for all six algorithms.
- `tests/expected.md` — independently reasoned expected properties/results.
- `tests/custom_actual.jsonl` — actual outputs from the implementation.
- `tests/RouteHelperTest.java` — direct calls to the required helper.

Command:

```text
python tools/run.py --input tests/custom_cases.jsonl
```

### Full-limit stress test

`tests/generate_stress.py` creates six requests at the published limits (`n=100000`, `m/q=250000` where applicable) using seed `20261025`. The generated graph families include a 100,000-vertex chain and forward-only DAGs, which specifically exercise deep traversal without recursion.

Commands:

```text
python tests/generate_stress.py
python tools/run.py --input tests/stress_cases.jsonl > tests/stress_actual.jsonl 2> tests/stress_run.log
python tests/validate_stress.py
```

Observed run: `15.21 s` wall time, peak resident memory `343712 KB` in the local container. Validation result: `PASS full-limit stress validation: DSU, both MSTs, SCC, topo, DAG`.

The stress validator independently checks DSU final connectivity, both MST forests and equal minimum costs, SCC partition/condensation, topological edge directions, and DAG shortest distances plus parent-edge witnesses.

## 11. Kruskal vs Prim benchmark

### Experimental setup

- Generator: deterministic Java `Benchmark.java`.
- Seed: `20261025`.
- Graphs: the **same generated graph** is passed to Kruskal and Prim for each size.
- Sizes: `(n,m) = (1000,5000), (10000,50000), (50000,250000)`.
- Graph construction: a path first guarantees connectivity; remaining edges are pseudorandom, with weights uniformly sampled from `[-1,000,000, 1,000,000]`.
- Warm-up: 2 runs of each algorithm before recorded measurements.
- Measurements: 5 runs per method and graph size; median reported.
- Timer: `System.nanoTime()` around the algorithm call only.
- Graph generation and benchmark input construction are outside the timed region. JSON parsing, process startup, compilation, and command-line launcher overhead are excluded. Both algorithms use the same already-built request object.
- Correctness was checked by requiring `components=1` and equal MST costs for both algorithms on every measured graph.

Command:

```text
javac --release 17 -encoding UTF-8 -d .build/Benchmark src/Java/Algorithms.java src/Java/Input.java src/Java/Json.java src/Java/Main.java src/Java/Benchmark.java
java -Xmx384m -cp .build/Benchmark Benchmark > tests/benchmark_raw.csv
```

Environment used for the recorded measurements:

- OpenJDK 21.0.12.1 (source compiled with `--release 17`)
- Python 3.13.5 for test tooling
- Linux x86_64, kernel 6.18.55
- Intel Xeon Platinum 8573C, 5 CPUs available in the container
- JVM: 64-bit OpenJDK Server VM

### Results

| n | m | Kruskal trials (ms) | Kruskal median | Prim trials (ms) | Prim median |
|---:|---:|---|---:|---|---:|
| 1,000 | 5,000 | 9.371, 8.531, 8.525, 16.259, 7.650 | **8.531** | 16.909, 12.269, 17.601, 14.791, 13.330 | **14.791** |
| 10,000 | 50,000 | 120.388, 123.430, 124.886, 72.665, 79.645 | **120.388** | 177.357, 96.646, 129.687, 107.381, 77.059 | **107.381** |
| 50,000 | 250,000 | 435.937, 550.968, 542.767, 597.409, 711.706 | **550.968** | 967.037, 1041.641, 860.778, 1793.300, 1096.168 | **1041.641** |

### Observations and limitations

Kruskal was faster in the smallest and largest measured cases, while the middle case had a lower Prim median. The largest graph shows a clearer practical advantage for Kruskal in this implementation. The measurements include neither parsing nor JVM startup, but they do include the algorithm's internal adjacency construction for Prim and edge sorting for Kruskal. Results are therefore implementation- and environment-dependent rather than universal constants.

The asymptotic bounds explain the overall scaling: Kruskal is dominated by `O(m log m)` sorting, while this lazy-queue Prim implementation is also `O(m log m)` in the stated assignment model. A different graph density, heap implementation, JVM, processor, or memory configuration can change the crossover point.

## 12. Build, run, and test commands

From the repository root:

```text
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/custom_cases.jsonl
```

Direct Java compilation is also possible:

```text
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java
java -Xmx384m -cp .build/Java Main < tests/custom_cases.jsonl
```

IntelliJ IDEA can open the repository as a Java project, use a Java 17 SDK, and run `Main` from `src/Java/Main.java`. No external Java dependencies are required.

## 13. Sources and AI assistance

### Sources

1. Assignment 3 repository and supplied specification: `README.md`, `README_RU.md`, `docs/Assignment_3_EN.pdf`, `docs/Assignment_3_RU.pdf`.
### AI assistance

ChatGPT was used to help interpret the assignment requirements, review the provided Java template, draft versions, and write documentation. ChatGPT helped me understand what was expected of me and explained various topics.

It also wrote this file; it’s great—it writes documentation well—but I’m not so great; I’m bad at writing documentation. :D
