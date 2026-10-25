import json
import random
from pathlib import Path

OUT = Path(__file__).with_name('stress_cases.jsonl')
N = 100_000
M = 250_000
Q = 250_000
rng = random.Random(20261025)

def write(obj, fh):
    fh.write(json.dumps(obj, separators=(',', ':')) + '\n')

with OUT.open('w', encoding='utf-8') as fh:
    ops = []
    for i in range(Q):
        if i < N - 1:
            ops.append([i, i + 1])
        else:
            ops.append([rng.randrange(N), rng.randrange(N)])
    write({'task': 'dsu', 'n': N, 'operations': ops}, fh)

    weighted = [[i, i + 1, (i % 2001) - 1000] for i in range(N - 1)]
    while len(weighted) < M:
        weighted.append([rng.randrange(N), rng.randrange(N), rng.randrange(-10**9, 10**9 + 1)])
    write({'task': 'kruskal', 'n': N, 'edges': weighted}, fh)
    write({'task': 'prim', 'n': N, 'edges': weighted}, fh)

    directed = [[i, i + 1] for i in range(N - 1)]
    while len(directed) < M:
        u = rng.randrange(N - 1)
        v = rng.randrange(u + 1, N)
        directed.append([u, v])
    write({'task': 'scc', 'n': N, 'edges': directed}, fh)
    write({'task': 'topo', 'n': N, 'edges': directed}, fh)

    weighted_dag = [[i, i + 1, (i % 101) - 50] for i in range(N - 1)]
    while len(weighted_dag) < M:
        u = rng.randrange(N - 1)
        v = rng.randrange(u + 1, N)
        weighted_dag.append([u, v, rng.randrange(-10**9, 10**9 + 1)])
    write({'task': 'dag', 'n': N, 'source': 0, 'edges': weighted_dag}, fh)

print(OUT)
