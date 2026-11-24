# CS 3364 Project 3 Report

**Finding the shortest path from the Computer Science building to the other buildings**

Team members and their contributions are listed in [section 6](#6-team-considerations).

Contents:

1. [Interpretation of the problem](#1-interpretation-of-the-problem)
2. [Methodology](#2-methodology)
3. [Experimental results](#3-experimental-results)
4. [Conclusions](#4-conclusions)
5. [Future work](#5-future-work)
6. [Team considerations](#6-team-considerations)
7. [Appendix: the map data and how to reproduce the results](#7-appendix)

## 1. Interpretation of the problem

### 1.1 The real-world problem

The assignment gives a map of a campus (page 3 of `Project3.pdf`). Boxes are buildings, lines are
walkways between them, and the number on each line is the length of that walkway. Someone standing
in the Computer Science building wants to know the shortest way to walk to each of the other
buildings.

### 1.2 The simplified model

We model the map as a weighted graph:

| Real world | Model |
| --- | --- |
| Building | Vertex (19 of them) |
| Walkway between two buildings | Edge (37 of them) |
| Length of the walkway | Edge weight, a positive whole number |
| Walking a route | A path; its cost is the sum of its edge weights |

The simplifications behind this model are:

- A walkway can be walked in both directions, so the graph is undirected.
- The only routes are the walkways drawn on the map.
- The cost of a route is only its length. Stairs, doors and crowds are ignored.
- The map does not name a unit for the distances, so we report them as plain numbers.

In this model the task is the **single-source shortest path problem**: given the source vertex
"Computer Science", find for each of the other 18 vertices the smallest possible path cost and a
path that achieves it.

### 1.3 What we were asked to build

The assignment asks us to implement and test two algorithms for this problem, one for each of two
design strategies:

- **Dijkstra's algorithm**, a greedy algorithm;
- **the Bellman-Ford algorithm**, a dynamic programming algorithm.

The rubric also asks for a design "with certain flexibilities". We took that to mean that the
program must not be tied to this one map, and designed for the following:

- The graph is read from a text file, so a different map needs no change to the code.
- Any vertex can be the source, and one destination or all of them can be reported.
- Graphs may be undirected or directed.
- Weights may be negative. Bellman-Ford handles them, and Dijkstra, which is not correct for them,
  refuses to run instead of returning a wrong answer.
- Vertices that cannot be reached are reported as unreachable.
- A negative cycle, for which no shortest path exists, is detected and shown to the user.

## 2. Methodology

### 2.1 Structure of the program

The program is written in Java (it compiles with Java 8 and newer) and uses only the standard
library. It has three layers, and each layer only knows about the ones above it in this list:

| Package | Responsibility | Main classes |
| --- | --- | --- |
| `graph` | The graph abstract data type and reading it from a file | `Graph`, `Edge`, `GraphReader` |
| `shortestpath` | The two algorithms and the form of their answer | `Dijkstra`, `BellmanFord`, `ShortestPaths` |
| `app` | The programs a user runs | `CampusNavigator`, `Experiments` |

Both algorithms implement the same small interface,

```java
public interface ShortestPathAlgorithm {
    String name();
    ShortestPaths run(Graph graph, int source);
}
```

so the rest of the program treats them identically. The command-line program, the tests and the
experiments all run "an algorithm" without knowing which one it is. This is what makes it possible
to run both on the same input and compare their answers.

### 2.2 The graph abstract data type

`Graph` stores a weighted graph as **adjacency lists**. It was chosen over an adjacency matrix
because maps are sparse: the campus graph has 74 arcs (37 walkways, each in two directions), where
a 19 by 19 matrix would hold 361 cells, most of them empty. Adjacency lists use O(V + E) memory and
let an algorithm visit the neighbours of a vertex in time proportional to their number.

- Vertices are created by name and numbered 0, 1, 2, ... in the order they first appear. The
  algorithms use the numbers, so they can keep distances and predecessors in plain arrays. Names
  are used only for input and output.
- An undirected edge is stored as two directed arcs, one in each direction. Both algorithms
  therefore work on directed and undirected graphs without any special cases.
- The graph also keeps one list of all arcs in the order they were added, because Bellman-Ford
  needs to walk through every edge again and again.
- The graph remembers whether any weight is negative, which lets Dijkstra reject such a graph in
  constant time.

`GraphReader` builds a graph from a text file with one edge per line (`from, to, weight`). Every
problem in the file is reported with the file name and line number.

### 2.3 The greedy algorithm: Dijkstra

Both algorithms keep, for every vertex `v`, a tentative distance `dist[v]` (the cost of the best
route found so far, infinity at the start) and a predecessor `pred[v]` (the vertex before `v` on
that route). Both improve these values with the same basic step, **relaxing** an edge `(u, v)` of
weight `w`: if `dist[u] + w < dist[v]`, then going through `u` is better, so `dist[v]` becomes
`dist[u] + w` and `pred[v]` becomes `u`. The algorithms differ only in which edges they relax and
in what order.

```
Dijkstra(G, s):
    dist[v] = infinity for every vertex v;  dist[s] = 0
    Q = priority queue containing s
    while Q is not empty:
        u = the vertex in Q with the smallest dist that is not settled yet   # greedy choice
        settle u
        for each edge (u, v) with weight w:
            relax (u, v);  if dist[v] became smaller, add v to Q
```

**The greedy choice.** At every step the algorithm commits to the closest vertex that is not
settled yet and never reconsiders it.

**Why this is correct.** Suppose `u` is the unsettled vertex with the smallest tentative distance,
and suppose some other route to `u` were shorter. That route starts at the source, which is
settled, and has to leave the settled region at some point; let `x` be the first unsettled vertex
on it. Three facts then contradict the assumption:

- The part of the route up to `x` costs at least `dist[x]`. The vertex just before `x` is settled,
  so its distance is final and its edge to `x` has already been relaxed.
- `dist[x]` is at least `dist[u]`, because `u` has the smallest tentative distance.
- The rest of the route, from `x` to `u`, cannot have a negative cost, because no edge weight is
  negative.

So the other route costs at least `dist[u]` and cannot be shorter. The tentative distance of `u`
is therefore final.

**Why negative weights break it.** The argument uses "the rest of the route cannot have a negative
cost". With a negative edge that is false, and a vertex may be settled too early. The file
`data/negative-weights.txt` is a four-vertex example: Dijkstra would settle B at distance 4 (the
direct edge A -> B) before it discovers the route A -> C -> B, which costs 5 - 3 = 2. Our
implementation therefore checks the graph first and refuses to run if it has a negative weight.

**Implementation.** Vertices that have been reached but not settled wait in a binary heap
(`java.util.PriorityQueue`). That class cannot lower the key of an entry that is already in the
heap, so each improvement adds a new entry, and entries that have become outdated are skipped when
they come out. The heap can therefore hold up to E entries instead of V.

**Cost.** Every vertex is settled once and every arc is examined once. Each examination may cause
one heap insertion, and each insertion and removal costs O(log E) = O(log V). The total is
**O((V + E) log V)** time and O(V + E) memory.

### 2.4 The dynamic programming algorithm: Bellman-Ford

**The subproblem.** Let `d(k, v)` be the cost of the shortest path from the source to `v` that uses
**at most `k` edges**. Then

```
d(0, s) = 0        d(0, v) = infinity for every other vertex v

d(k, v) = min( d(k-1, v),   min over all edges (u, v) of  d(k-1, u) + w(u, v) )
```

The recurrence says: the best path to `v` with at most `k` edges either already used at most `k-1`
edges, or it is a best path with at most `k-1` edges to some neighbour `u`, followed by the edge
`(u, v)`. This is the optimal substructure that dynamic programming needs: a shortest path is
built from shortest paths to earlier vertices.

If the graph has no negative cycle, a shortest path never visits a vertex twice, so it has at most
V - 1 edges, and `d(V-1, v)` is the answer.

```
BellmanFord(G, s):
    dist[v] = infinity for every vertex v;  dist[s] = 0
    repeat at most V - 1 times:                 # pass k fills row k of the table
        for each edge (u, v) with weight w:
            relax (u, v)
        if the pass changed nothing: stop
    if the last pass still changed something:
        if any edge can still be relaxed: report a negative cycle
```

**Implementation.** Three decisions are worth explaining.

1. *One row instead of a table.* Row `k` depends only on row `k-1`, so the full table is not
   stored. We go one step further and keep a single array that is updated in place. During pass
   `k` a value may then already contain an improvement made earlier in the same pass. That value
   is still the cost of a real path and is never worse than `d(k, v)`, so the result stays correct
   and the algorithm can only finish sooner.
2. *Stopping early.* If a complete pass changes no value, the next pass would start from exactly
   the same values and change nothing either. The algorithm stops there, which in practice saves
   most of the V - 1 passes (see section 3).
3. *Negative cycles.* If values are still changing after V - 1 passes, one more pass is run. If it
   can still relax an edge, some path keeps getting cheaper by going around a cycle with a negative
   total weight, and there is no shortest path. The program then follows the predecessor links
   backwards to recover the cycle itself, so that the error message can name it.

**Cost.** At most V passes, each over all E edges: **O(VE)** time in the worst case, and O(V)
memory besides the graph. When the passes stop early the cost is O(E) times the number of passes
actually made.

### 2.5 The two algorithms side by side

| | Dijkstra | Bellman-Ford |
| --- | --- | --- |
| Design strategy | Greedy | Dynamic programming |
| Key idea | Settle the closest unsettled vertex | Extend the allowed path length by one edge per pass |
| Order of relaxations | Each edge once, in order of distance | Every edge in every pass |
| Worst-case time | O((V + E) log V) | O(VE) |
| Extra memory | O(V + E) | O(V) |
| Negative weights | Not supported (rejected) | Supported |
| Negative cycles | Not applicable | Detected and reported |
| Extra data structure | Priority queue | None |

### 2.6 How we checked correctness

A shortest path program can print plausible but wrong numbers, so we did not rely on the two
algorithms agreeing with each other alone:

1. **The map data.** The map was typed into `data/campus.txt` by hand. To catch typing mistakes we
   compared the 37 distance labels printed on the map with the 37 weights in the file, as
   multisets, and checked that no pair of buildings appears twice.
2. **Hand-computed answers.** The 18 shortest distances were worked out by hand from the map and
   are stored in the test suite, which compares both algorithms against them.
3. **An independent reference.** The tests include a third algorithm, Floyd-Warshall, which is
   only a few lines long and shares no code with the other two. Both algorithms are compared with
   it on 300 random undirected graphs, on 300 random directed graphs, on a street grid, and from
   every one of the 19 buildings of the campus map. Bellman-Ford is also compared with it on 300
   random directed graphs that have negative edges but no negative cycle.
4. **Negative cycles.** On 500 small random graphs in which negative cycles may occur, the tests
   check that Bellman-Ford reports a cycle exactly when one can be reached from the source, and
   that the cycle it names really exists in the graph and has a negative total weight.
5. **The routes, not only the distances.** For every result the tests walk along each reported
   route and check that it starts at the source, ends at the destination, uses only edges that
   exist, and adds up to the reported distance.
6. **Special cases and output.** Unreachable vertices, a graph with one vertex, self-loops,
   parallel edges, zero-weight edges, invalid input files and invalid command-line arguments each
   have a test. The output printed for the campus map is compared, character for character, with
   the sample output shown in `README.md`.
7. **Testing the tests.** We deliberately broke the program in 14 different ways, one at a time,
   and confirmed that every one of them makes at least one test fail. Examples are relaxing with
   "at most" instead of "less than", running one pass too few, not counting a distance update,
   and printing the destinations without sorting them.

### 2.7 Development process

The work was done in this order, and each step was finished and tested before the next began:

1. Read the map and write it down as a data file (section 1.2 and check 1 above).
2. Build the graph data type and the file reader.
3. Implement Dijkstra and compare its answers with the hand-computed distances.
4. Implement Bellman-Ford behind the same interface and compare it with Dijkstra.
5. Add the independent reference and the random tests, then the special cases.
6. Add negative-cycle detection and the two small example graphs with negative weights.
7. Write the command-line program, then the experiments, then this report.
8. Review the code and the report, strengthen the tests where the review found gaps (check 7 in
   section 2.6), and correct the report where it was imprecise.

## 3. Experimental results

### 3.1 The answer to the assignment

Shortest paths from the Computer Science building, as printed by the program. Dijkstra and
Bellman-Ford return the same distance and the same route for every building.

| Destination | Distance | Shortest route |
| --- | ---: | --- |
| Burdick | 30 | Computer Science -> Burdick |
| Torreyson Library | 40 | Computer Science -> Torreyson Library |
| Prince Center | 70 | Computer Science -> Torreyson Library -> Prince Center |
| Old Main | 70 | Computer Science -> Torreyson Library -> Old Main |
| Speech Language Hearing | 130 | Computer Science -> Burdick -> Speech Language Hearing |
| Lewis Science Center | 150 | Computer Science -> Lewis Science Center |
| Fine Art | 160 | Computer Science -> Torreyson Library -> Old Main -> Fine Art |
| McAlister Hall | 170 | Computer Science -> Torreyson Library -> Old Main -> McAlister Hall |
| Police Dept. | 170 | Computer Science -> Torreyson Library -> Prince Center -> Police Dept. |
| Wingo | 220 | Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo |
| Student Center | 240 | Computer Science -> Torreyson Library -> Old Main -> Fine Art -> Student Center |
| Maintenance College | 250 | Computer Science -> Burdick -> Speech Language Hearing -> Maintenance College |
| Student Health Center | 270 | Computer Science -> Torreyson Library -> Prince Center -> Police Dept. -> Student Health Center |
| New Business Building | 270 | Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo -> New Business Building |
| Brewer-Hegeman | 290 | Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo -> New Business Building -> Brewer-Hegeman |
| Oak Tree Apt. | 300 | Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo -> New Business Building -> Oak Tree Apt. |
| College Square | 350 | Computer Science -> Lewis Science Center -> College Square |
| Bear Village Apt. | 640 | Computer Science -> Torreyson Library -> Old Main -> McAlister Hall -> Wingo -> New Business Building -> Brewer-Hegeman -> Bear Village Apt. |

All routes together form the shortest-path tree below (distance from Computer Science in
brackets). It uses 18 of the 37 walkways.

```
Computer Science (0)
|-- Burdick (30)
|   `-- Speech Language Hearing (130)
|       `-- Maintenance College (250)
|-- Torreyson Library (40)
|   |-- Prince Center (70)
|   |   `-- Police Dept. (170)
|   |       `-- Student Health Center (270)
|   `-- Old Main (70)
|       |-- Fine Art (160)
|       |   `-- Student Center (240)
|       `-- McAlister Hall (170)
|           `-- Wingo (220)
|               `-- New Business Building (270)
|                   |-- Brewer-Hegeman (290)
|                   |   `-- Bear Village Apt. (640)
|                   `-- Oak Tree Apt. (300)
`-- Lewis Science Center (150)
    `-- College Square (350)
```

Two observations about the answer:

- **The direct walkway is not always the shortest.** Computer Science and Prince Center are joined
  by a walkway of length 80, but going through Torreyson Library costs 40 + 30 = 70. In the same
  way Burdick has a direct walkway to Maintenance College (30 + 300 = 330 from Computer Science),
  but the route through Speech Language Hearing costs only 250.
- **Every shortest route on this map is unique.** We checked that no building has two different
  routes of the same smallest cost, which is why the two algorithms report identical routes and
  not only identical distances.

### 3.2 Work done on the campus map

Each result records how many edges the algorithm examined, which is a measure of work that does
not depend on the computer.

| Algorithm | Rounds | Edges examined | Distance updates |
| --- | ---: | ---: | ---: |
| Dijkstra | 19 vertices settled | 74 | 23 |
| Bellman-Ford | 3 passes | 222 | 23 |

- Dijkstra examines each of the 74 arcs (37 walkways, two directions each) exactly once.
- Bellman-Ford examines all 74 arcs in each pass. It needed 3 passes instead of the worst-case
  V - 1 = 18: the first pass made 22 improvements, the second made one, and the third made none,
  which ended the run.
- The second pass was needed for a single building, College Square. Its two walkways are the first
  two lines of the data file, so in pass 1 they are examined before anything is known about the
  buildings at their other ends.
- The longest shortest route (to Bear Village Apt.) has 7 edges. A Bellman-Ford that fills the
  table strictly row by row would therefore need 7 passes, plus one to notice that nothing changes.
  Updating the array in place (section 2.4) brought this down to 3.

On a graph this small both algorithms finish in about a microsecond.

### 3.3 Larger graphs

To see how the two algorithms behave as the problem grows, the program `app.Experiments` runs them
on generated graphs. In every row below both algorithms returned identical distances.

The number of edges examined is exact and repeatable. Times are the fastest of 25 runs after a
warm-up, on OpenJDK 21 on a shared 2.1 GHz Intel Xeon virtual machine. They changed by up to a
factor of two between repetitions of the experiment, so our conclusions rest on the edge counts and
use the times only as a rough confirmation.

In the tables, B-F stands for Bellman-Ford, "edges" is the number of edges examined, times are in
microseconds, and the two ratios are the Bellman-Ford value divided by the Dijkstra value.

**Random connected graphs with E = 4V.**

| V | E | Dijkstra edges | Dijkstra time | B-F passes | B-F edges | B-F time | Edges ratio | Time ratio |
| --------: | --------: | ---------: | ---------: | -------: | -----------: | ---------: | -------: | ------: |
| 500 | 2,000 | 4,000 | 81 | 8 | 32,000 | 48 | 8x | 0.6x |
| 1,000 | 4,000 | 8,000 | 280 | 9 | 72,000 | 217 | 9x | 0.8x |
| 2,000 | 8,000 | 16,000 | 625 | 10 | 160,000 | 313 | 10x | 0.5x |
| 4,000 | 16,000 | 32,000 | 1,790 | 11 | 352,000 | 2,297 | 11x | 1.3x |
| 8,000 | 32,000 | 64,000 | 4,056 | 11 | 704,000 | 4,741 | 11x | 1.2x |
| 16,000 | 64,000 | 128,000 | 8,972 | 16 | 2,048,000 | 9,119 | 16x | 1.0x |
| 32,000 | 128,000 | 256,000 | 19,475 | 13 | 3,328,000 | 8,450 | 13x | 0.4x |

**Square street grids (n by n junctions), source in the middle.**

| V | E | Dijkstra edges | Dijkstra time | B-F passes | B-F edges | B-F time | Edges ratio | Time ratio |
| --------: | --------: | ---------: | ---------: | -------: | -----------: | ---------: | -------: | ------: |
| 625 | 1,200 | 2,400 | 43 | 26 | 62,400 | 92 | 26x | 2.1x |
| 2,500 | 4,900 | 9,800 | 372 | 53 | 519,400 | 1,227 | 53x | 3.3x |
| 10,000 | 19,800 | 39,600 | 1,930 | 103 | 4,078,800 | 6,287 | 103x | 3.3x |
| 40,000 | 79,600 | 159,200 | 9,966 | 215 | 34,228,000 | 110,139 | 215x | 11.1x |

**A path of V vertices whose edges are stored far end first (worst case for Bellman-Ford).**

| V | E | Dijkstra edges | Dijkstra time | B-F passes | B-F edges | B-F time | Edges ratio | Time ratio |
| --------: | --------: | ---------: | ---------: | -------: | -----------: | ---------: | -------: | ------: |
| 500 | 499 | 998 | 10 | 500 | 499,000 | 572 | 500x | 59x |
| 1,000 | 999 | 1,998 | 21 | 1,000 | 1,998,000 | 2,582 | 1,000x | 125x |
| 2,000 | 1,999 | 3,998 | 39 | 2,000 | 7,996,000 | 10,492 | 2,000x | 266x |
| 4,000 | 3,999 | 7,998 | 80 | 4,000 | 31,992,000 | 40,406 | 4,000x | 507x |

What the tables show:

1. **Dijkstra's work grows in step with the graph.** It examines every arc once, so the number of
   edges examined is 2E in every row of all three tables. A graph that is twice as large costs
   twice as many examinations, and one that is four times as large costs four times as many.
2. **Bellman-Ford's work is the number of passes times the size of the graph, and the number of
   passes depends on the shape of the graph and on the order of its edges.** The largest number of
   edges on any shortest path, plus one, is an upper limit. A favourable edge order needs fewer
   passes (3 instead of 8 on the campus map), and an unfavourable one needs all of them.
   - In random graphs every vertex is only a few edges away from every other, so 8 to 16 passes are
     enough even for 32,000 vertices. That is far below the bound of V - 1 passes.
   - In a street grid the far corners are many junctions away. The number of passes grows with the
     side of the grid (26, 53, 103, 215 passes for sides of 25, 50, 100, 200), so the work grows
     faster than the graph does.
   - In the worst case every one of the V - 1 passes is needed. Doubling V then multiplies the
     edges examined by four, which is the O(VE) bound in action.
3. **Counting edges and measuring time tell different stories on random graphs.** There
   Bellman-Ford examines about ten times as many edges as Dijkstra but takes a similar time, and is
   sometimes faster. One Bellman-Ford step is a comparison and an addition while walking straight
   through a list; one Dijkstra step may insert into or remove from a heap, which is far more
   expensive. Once the number of passes becomes large (grids, and above all the worst case),
   Dijkstra is clearly faster, by a factor of up to several hundred in the last table.

### 3.4 Negative weights

`data/negative-weights.txt` is a directed graph with edges A -> B (4), A -> C (5), C -> B (-3) and
B -> D (2). Bellman-Ford finds the route through the negative edge. This is the complete output of
`java -cp out app.CampusNavigator --graph data/negative-weights.txt --source A`:

```
Graph: data/negative-weights.txt (directed, 4 vertices, 4 edges)
Note: the graph has negative weights, so only Bellman-Ford is run.
Shortest paths from A

Destination     Distance  Route
-----------  -----------  -----
B                      2  A -> C -> B
D                      4  A -> C -> B -> D
C                      5  A -> C

Algorithm     Rounds  Edges examined  Distance updates
------------  ------  --------------  ----------------
Bellman-Ford       2               8                 4
(Rounds: vertices settled by Dijkstra, passes over all edges by Bellman-Ford.)
```

Asking for Dijkstra on this graph (`--algorithm dijkstra`) gives an error. Without that check, the
textbook algorithm would settle B at 4 and D at 6, both of which are wrong:

```
Graph: data/negative-weights.txt (directed, 4 vertices, 4 edges)
Error: Dijkstra's algorithm requires non-negative edge weights; use Bellman-Ford instead
```

`data/negative-cycle.txt` contains the cycle B -> C -> B with total weight -2 + 1 = -1.
Bellman-Ford detects it and names it:

```
Graph: data/negative-cycle.txt (directed, 4 vertices, 4 edges)
Note: the graph has negative weights, so only Bellman-Ford is run.
Error: The graph contains a negative cycle: C -> B -> C
```

### 3.5 Test results

The test suite (`java -cp out ShortestPathTests`) contains 26 tests, covering the checks listed in
section 2.6, and all of them pass. The code was also compiled with `javac --release 8 -Xlint:all`,
which confirms that it needs nothing newer than Java 8 and reports no warnings about the code.

## 4. Conclusions

- The shortest route from the Computer Science building to each of the other 18 buildings is given
  in section 3.1. The nearest building is Burdick (30) and the farthest is Bear Village Apt. (640).
  Two independent algorithms, a third reference algorithm and a calculation by hand all agree on
  these distances.
- **For this problem Dijkstra is the right tool.** Walkway lengths are never negative, which is
  exactly the condition the greedy choice needs, and Dijkstra then does the least work: it looks at
  every edge once. Its advantage grows with the size of the map and is largest on graphs that are
  shaped like maps, where destinations are many edges away.
- **Bellman-Ford pays for its generality.** It solves a harder problem, with negative weights and
  with detection of negative cycles, which Dijkstra cannot solve at all. The price is a worst case
  of O(VE). With in-place updates and early stopping the typical cost is much lower than the worst
  case, and on small or well-connected graphs it is competitive in practice.
- **The two strategies differ in how much they commit.** The greedy algorithm makes one
  irrevocable decision per step and needs a proof that the decision is safe; when the proof's
  assumption fails, so does the algorithm. Dynamic programming makes no early commitment. It keeps
  improving a table of answers to smaller subproblems, which costs more work but stays correct
  under weaker assumptions.
- Keeping the map in a data file and both algorithms behind one interface cost very little and
  paid off: the same program was used unchanged for the campus map, the negative-weight examples
  and all the experiments, and each algorithm served as a check on the other.

## 5. Future work

- **A faster priority queue for Dijkstra.** An indexed heap that supports "decrease key" would
  keep at most V entries instead of E and avoid creating an object for every improvement. Section
  3.3 suggests that this is where most of Dijkstra's time goes.
- **A queue-based Bellman-Ford.** Relaxing only the edges that leave vertices whose distance
  changed in the previous pass (the "shortest path faster algorithm") avoids examining edges that
  cannot improve anything, and would make the algorithm insensitive to the order of the edges in
  the file.
- **Stopping at the destination.** When only one destination is wanted, Dijkstra can stop as soon
  as that building is settled; with building coordinates, A* search could reduce the work further.
- **A richer map.** Real coordinates, one-way or stair-free routes, walking time instead of
  distance, and all-pairs queries ("from any building to any building") would make the tool useful
  for actual navigation.
- **A graphical front end** that draws the map and highlights the chosen route.

## 6. Team considerations

**How the design supports working as a team.** The three packages meet at two small, stable
agreements: the text format of a graph file and the `ShortestPathAlgorithm` interface. Once those
are fixed, the graph and file reader, each of the two algorithms, the command-line program and the
tests can be written by different people at the same time without waiting for one another. The
shared test suite is the common definition of "done": a part is finished when its tests pass, and
the cross-checks of section 2.6 mean that a mistake in one algorithm is caught by code that was
written independently of it.

**How changes are integrated.** The repository has a `dev` branch. Changes are made on separate
branches and merged into `dev` through pull requests, which gives another team member the chance to
read a change before it becomes part of the project.

**Contributions of each team member.** The assignment requires this table. *It must be completed
by the team before the report is submitted.*

| Team member | Contributions |
| --- | --- |
| _(name)_ | _(to be completed)_ |
| _(name)_ | _(to be completed)_ |
| _(name)_ | _(to be completed)_ |
| _(name)_ | _(to be completed)_ |
| _(name)_ | _(to be completed)_ |

## 7. Appendix

### 7.1 The map as data

The contents of `data/campus.txt`, one walkway per line. Three labels that are cut short on the
map are written out in full: McAlister Hall, Student Health Center and New Business Building. Two
names are also capitalised normally; the map prints them as "McALister" and "Bear village Apt.".

| Building | Building | Distance |
| --- | --- | ---: |
| College Square | Lewis Science Center | 200 |
| College Square | Prince Center | 300 |
| Lewis Science Center | Speech Language Hearing | 250 |
| Lewis Science Center | Computer Science | 150 |
| Computer Science | Burdick | 30 |
| Computer Science | Prince Center | 80 |
| Computer Science | Torreyson Library | 40 |
| Speech Language Hearing | Burdick | 100 |
| Speech Language Hearing | Maintenance College | 120 |
| Burdick | Torreyson Library | 80 |
| Burdick | McAlister Hall | 200 |
| Burdick | Maintenance College | 300 |
| Prince Center | Torreyson Library | 30 |
| Prince Center | Police Dept. | 100 |
| Torreyson Library | Old Main | 30 |
| Old Main | Police Dept. | 200 |
| Old Main | Fine Art | 90 |
| Old Main | McAlister Hall | 100 |
| Police Dept. | Fine Art | 50 |
| Police Dept. | Student Health Center | 100 |
| Fine Art | McAlister Hall | 180 |
| Fine Art | Student Center | 80 |
| McAlister Hall | Maintenance College | 150 |
| McAlister Hall | Student Center | 100 |
| McAlister Hall | Wingo | 50 |
| Maintenance College | Wingo | 100 |
| Maintenance College | New Business Building | 150 |
| Maintenance College | Oak Tree Apt. | 160 |
| Student Center | Wingo | 100 |
| Student Center | Student Health Center | 50 |
| Student Center | New Business Building | 110 |
| Wingo | New Business Building | 50 |
| New Business Building | Oak Tree Apt. | 30 |
| New Business Building | Brewer-Hegeman | 20 |
| Oak Tree Apt. | Brewer-Hegeman | 40 |
| Student Health Center | Brewer-Hegeman | 200 |
| Brewer-Hegeman | Bear Village Apt. | 350 |

### 7.2 How to reproduce the results

From the repository root, with a Java Development Kit (version 8 or newer) installed:

```
javac -d out src/graph/*.java src/shortestpath/*.java src/app/*.java test/*.java

java -cp out app.CampusNavigator                                             (section 3.1 and 3.2)
java -cp out app.Experiments                                                 (section 3.3)
java -cp out app.CampusNavigator --graph data/negative-weights.txt --source A   (section 3.4)
java -cp out app.CampusNavigator --graph data/negative-cycle.txt --source A     (section 3.4)
java -cp out ShortestPathTests                                               (section 3.5)
```

The edge counts will be identical on any computer. The times will differ.
