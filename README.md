# greedy_dynamic_sort

CS 3364 Project 3: greedy and dynamic programming algorithms that find the shortest path between
locations (nodes) on a map with distances between them (edges with a numerical value).

The program answers the question of the assignment, "what is the shortest path from the Computer
Science building to every other building?", with two algorithms:

| Algorithm | Strategy | Negative weights | Worst-case time |
| --- | --- | --- | --- |
| Dijkstra | Greedy | Not allowed (rejected with an error) | O((V + E) log V) |
| Bellman-Ford | Dynamic programming | Allowed; negative cycles are detected and reported | O(VE) |

Both are run on the same graph and their answers are compared. The assignment is in
[Project3.pdf](Project3.pdf) (the map is on page 3) and the full write-up is in
[docs/REPORT.md](docs/REPORT.md).

## Requirements

A Java Development Kit, version 8 or newer. No libraries or build tools are needed.

## Build and run

Run these commands from the repository root. They work unchanged in a Linux or macOS shell and in
Windows `cmd` or PowerShell.

```
javac -d out src/graph/*.java src/shortestpath/*.java src/app/*.java test/*.java
java -cp out app.CampusNavigator
```

Output:

```
Graph: data/campus.txt (undirected, 19 vertices, 37 edges)
Shortest paths from Computer Science

Destination                 Distance  Route
-----------------------  -----------  -----
Burdick                           30  Computer Science -> Burdick
Torreyson Library                 40  Computer Science -> Torreyson Library
Prince Center                     70  Computer Science -> Torreyson Library -> Prince Center
Old Main                          70  Computer Science -> Torreyson Library -> Old Main
Speech Language Hearing          130  Computer Science -> Burdick -> Speech Language Hearing
Lewis Science Center             150  Computer Science -> Lewis Science Center
Fine Art                         160  Computer Science -> Torreyson Library -> Old Main -> Fine Art
McAlister Hall                   170  Computer Science -> Torreyson Library -> Old Main -> McAlister Hall
Police Dept.                     170  Computer Science -> Torreyson Library -> Prince Center -> Police Dept.
Wingo                            220  Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo
Student Center                   240  Computer Science -> Torreyson Library -> Old Main -> Fine Art -> Student Center
Maintenance College              250  Computer Science -> Burdick -> Speech Language Hearing -> Maintenance College
Student Health Center            270  Computer Science -> Torreyson Library -> Prince Center -> Police Dept. -> Student Health Center
New Business Building            270  Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo -> New Business Building
Brewer-Hegeman                   290  Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo -> New Business Building -> Brewer-Hegeman
Oak Tree Apt.                    300  Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo -> New Business Building -> Oak Tree Apt.
College Square                   350  Computer Science -> Lewis Science Center -> College Square
Bear Village Apt.                640  Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo -> New Business Building -> Brewer-Hegeman -> Bear Village Apt.

Dijkstra and Bellman-Ford agree on the distance to every building.

Algorithm     Rounds  Edges examined  Distance updates
------------  ------  --------------  ----------------
Dijkstra          19              74                23
Bellman-Ford       3             222                23
(Rounds: vertices settled by Dijkstra, passes over all edges by Bellman-Ford.)
```

### Options

| Option | Meaning | Default |
| --- | --- | --- |
| `--graph <file>` | Graph file to load | `data/campus.txt` |
| `--source <name>` | Start building | `Computer Science` |
| `--to <name>` | Show only the route to this building | all buildings |
| `--algorithm <name>` | `dijkstra`, `bellman-ford` or `both` | `both` |
| `--help` | Show the list of options | |

Building names and algorithm names are not case-sensitive.

Examples:

```
java -cp out app.CampusNavigator --source "Old Main" --to "Wingo"
java -cp out app.CampusNavigator --algorithm bellman-ford
java -cp out app.CampusNavigator --graph data/negative-weights.txt --source A
java -cp out app.CampusNavigator --graph data/negative-cycle.txt --source A
```

## Graph files

The map is data, not code, so the program works on any graph. A graph file lists one edge per line:

```
# Comments start with '#'. Blank lines are ignored.
undirected                       # optional first entry: "directed" or "undirected" (the default)
Computer Science, Burdick, 30    # from, to, weight
```

Vertex names may contain anything except a comma or a `#`. Weights are whole numbers and may be
negative.

| File | Contents |
| --- | --- |
| `data/campus.txt` | The campus map of the assignment: 19 buildings, 37 walkways |
| `data/negative-weights.txt` | A small directed graph with a negative edge, where the greedy choice fails |
| `data/negative-cycle.txt` | A small directed graph with a negative cycle, which Bellman-Ford reports |

## Tests and experiments

```
java -cp out ShortestPathTests
java -cp out app.Experiments
```

`ShortestPathTests` needs no testing library and exits with a non-zero code if a test fails. It
checks the campus answers against values worked out by hand, and both algorithms against a third,
independent algorithm (Floyd-Warshall) on more than a thousand random graphs, including graphs with
negative weights and negative cycles. It also checks that the sample output above is exactly what
the program prints.

`Experiments` prints the tables used in the report: both algorithms on the campus map, on random
graphs, on street grids and on a worst case for Bellman-Ford.

## Project layout

```
data/                    graph files
docs/REPORT.md           project report
src/graph/               the graph abstract data type
    Graph.java               weighted directed or undirected graph, adjacency lists
    Edge.java                one directed arc
    GraphReader.java         loads a graph from a text file
    GraphFormatException.java
    GraphGenerator.java      synthetic graphs for tests and experiments
src/shortestpath/        the algorithms
    ShortestPathAlgorithm.java   common interface
    Dijkstra.java                greedy algorithm
    BellmanFord.java             dynamic programming algorithm
    ShortestPaths.java           the result: distances, routes and work counters
    NegativeCycleException.java
src/app/                 programs
    CampusNavigator.java     the command-line program
    Experiments.java         the experiments of the report
test/ShortestPathTests.java  the test suite
```

## Report

The report is written in Markdown so that it can be read on GitHub and reviewed like code. To turn
it into a Word document for submission, use [pandoc](https://pandoc.org):

```
pandoc docs/REPORT.md -o REPORT.docx
```
