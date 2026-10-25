# Expected results for custom cases

These expectations are derived directly from the assignment contracts, independently of the program output.
For SCC, Prim, and topological order, multiple outputs are valid; the recorded actual output is checked by the contract rather than byte-for-byte equality.

1. **DSU:** after self-union, joining 0-1, repeated union, 2-3, 3-4, the states are `[[5,1],[4,2],[4,2],[3,2],[2,3]]`.
2. **Kruskal:** loops are ignored; the minimum spanning forest has cost `4` and `components=1`, with five selected edges and cost 4.
3. **Prim:** must have the same minimum cost `4` and `components=1` as Kruskal; any valid optimal five-edge forest is accepted.
4. **SCC:** components are `{0,1}`, `{2,3}`, `{4,5}` and condensation contains exactly `A→B` and `B→C`, once each.
5. **Topo acyclic:** all six vertices exactly once; every directed edge goes forward.
6. **Topo cyclic:** `acyclic=false`, `order=[]` because 0→1→2→0 is a cycle.
7. **DAG:** the component containing 4-5 is unreachable. Distances must be `[0,3,-4,0,null,null]`; parent edges must form shortest paths from source.
8. **DAG unreachable cycle:** `acyclic=false` even though the cycle is unreachable from source; both arrays must be empty.
9. **DAG equal/parallel paths:** distances are `[0,1,1,2]`; either valid parent edge for vertex 1 and either valid shortest predecessor for vertex 3 is acceptable.
