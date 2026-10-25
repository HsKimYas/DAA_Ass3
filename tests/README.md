# Tests / Тесты

This directory contains both required evidence and reproducible test tools.

## Files

- `custom_cases.jsonl` — mixed hand-designed JSON Lines cases for all six algorithms.
- `expected.md` — independently reasoned expected results/properties.
- `custom_actual.jsonl` — actual output produced by the submitted implementation.
- `RouteHelperTest.java` — direct tests of `Algorithms.reconstructPath(...)` for reachable, source, unreachable, and equal/parallel-edge cases.
- `generate_stress.py` — generates requests at the published `n=100000`, `m/q=250000` limits with seed `20261025`.
- `stress_cases.jsonl` — generated full-limit inputs.
- `stress_actual.jsonl` — actual full-limit outputs.
- `validate_stress.py` — independent structural/optimality validation of the stress outputs.
- `stress_validation.txt` — successful stress-validation result.
- `benchmark_raw.csv` — raw Kruskal/Prim timing measurements.

## Commands

From the repository root:

```sh
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/custom_cases.jsonl
```

Route helper:

```sh
javac --release 17 -encoding UTF-8 -d .build/RouteTest src/Java/Algorithms.java src/Java/Input.java src/Java/Json.java src/Java/Main.java tests/RouteHelperTest.java
java -cp .build/RouteTest RouteHelperTest
```

Full-limit stress:

```sh
python tests/generate_stress.py
python tools/run.py --input tests/stress_cases.jsonl > tests/stress_actual.jsonl 2> tests/stress_run.log
python tests/validate_stress.py
```

The full methodology and results are documented in the root `ANALYSIS.md`.
