import json
from pathlib import Path

inp = Path(__file__).with_name('stress_cases.jsonl')
out = Path(__file__).with_name('stress_actual.jsonl')
requests = [json.loads(line) for line in inp.read_text(encoding='utf-8').splitlines()]
answers = [json.loads(line) for line in out.read_text(encoding='utf-8').splitlines()]
assert len(requests) == len(answers) == 6

# DSU: all chain operations connect all vertices.
dsu = answers[0]
assert len(dsu['states']) == 250_000
assert dsu['states'][-1] == [1, 100_000]

# MST: connected graph -> exactly n-1 selected edges; both implementations agree on optimum.
kr, pr = answers[1], answers[2]
for ans in (kr, pr):
    assert ans['components'] == 1
    assert len(ans['edge_ids']) == 99_999
assert kr['cost'] == pr['cost']

def valid_forest(req, ans):
    n = req['n']; edges = req['edges']
    parent = list(range(n)); size = [1] * n
    def root(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x
    cost = 0
    for eid in ans['edge_ids']:
        u,v,w = edges[eid]
        a,b = root(u),root(v)
        assert a != b
        if size[a] < size[b]: a,b=b,a
        parent[b]=a; size[a]+=size[b]; cost += w
    assert cost == ans['cost']
    assert len({root(i) for i in range(n)}) == 1
valid_forest(requests[1], kr)
valid_forest(requests[2], pr)

# SCC: all edges point from lower to higher vertex, so every vertex is its own SCC.
scc = answers[3]
labels = scc['component']
assert len(labels) == 100_000 and len(set(labels)) == 100_000
expected_pairs = {(labels[u], labels[v]) for u,v in requests[3]['edges'] if labels[u] != labels[v]}
assert set(map(tuple, scc['condensation'])) == expected_pairs
assert len(scc['condensation']) == len(expected_pairs)

# Topological order is valid for the forward-only graph.
topo = answers[4]
assert topo['acyclic'] is True and len(topo['order']) == 100_000
pos = [0] * 100_000
for i,v in enumerate(topo['order']): pos[v]=i
assert all(pos[u] < pos[v] for u,v in requests[4]['edges'])

# DAG: independently compute shortest paths in the natural topological order.
dag_req = requests[5]; dag = answers[5]
assert dag['acyclic'] is True
n=dag_req['n']; INF=None
dist=[INF]*n; dist[0]=0
adj=[[] for _ in range(n)]
for eid,(u,v,w) in enumerate(dag_req['edges']):
    adj[u].append((v,w))
for u in range(n):
    if dist[u] is None:
        continue
    for v,w in adj[u]:
        cand=dist[u]+w
        if dist[v] is None or cand<dist[v]: dist[v]=cand
assert dag['distance'] == dist
parents=dag['parent_edge']
for v in range(n):
    if v==0 or dist[v] is None:
        assert parents[v] == -1
    else:
        eid=parents[v]; u,w_from=dag_req['edges'][eid][0],dag_req['edges'][eid][2]
        assert dag_req['edges'][eid][1] == v
        assert dist[u] is not None and dist[u]+w_from == dist[v]

print('PASS full-limit stress validation: DSU, both MSTs, SCC, topo, DAG')
