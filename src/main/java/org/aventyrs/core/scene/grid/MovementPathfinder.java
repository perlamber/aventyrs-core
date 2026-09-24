package org.aventyrs.core.scene.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Cheapest paths across a columns by rows board, priced and gated by a mover's {@link StepRules}.
 * A plain Dijkstra search: every step's cost is at least 1, so the first time a hex leaves the
 * frontier its cost is final.
 *
 * <p>Two rules shape every search. A path may only run <b>through</b> a hex it {@link
 * StepRules#canPass}, and may only <b>end</b> on one it {@link StepRules#canStop} — so an ally's
 * hex is on the way but never the destination, and a hex that stops you (never passable) is still
 * reachable as the last step. Ties are broken by column then row, so the same board always yields
 * the same path.
 *
 * <p>Pure geometry: it decides nothing about who may go where — that is the {@code StepRules}'.
 */
public final class MovementPathfinder {

    private static final Comparator<Node> CHEAPEST_FIRST = Comparator.comparingInt(Node::cost)
            .thenComparingInt(node -> node.hex().x())
            .thenComparingInt(node -> node.hex().y());

    private final int columns;
    private final int rows;

    public MovementPathfinder(final int columns, final int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    /**
     * Every hex a mover at from can end a movement on within budget UD, with its cost — the
     * origin itself excluded. Empty for a budget below 1.
     */
    public Map<GridPosition, Integer> reachable(final GridPosition from, final int budget, final StepRules rules) {
        Map<GridPosition, Integer> stoppable = new LinkedHashMap<>();
        search(from, budget, rules).costs().forEach((hex, cost) -> {
            if (!hex.equals(from) && rules.canStop(hex)) {
                stoppable.put(hex, cost);
            }
        });
        return stoppable;
    }

    /**
     * The cheapest path from from that ends on to, if any — empty when to is where the mover
     * already stands, when they may not stop there, or when nothing connects the two.
     */
    public Optional<MovementPath> pathTo(final GridPosition from, final GridPosition to, final StepRules rules) {
        if (from.equals(to) || !rules.canStop(to)) {
            return Optional.empty();
        }
        Search search = search(from, Integer.MAX_VALUE, rules);
        if (!search.costs().containsKey(to)) {
            return Optional.empty();
        }
        List<GridPosition> steps = new ArrayList<>();
        for (GridPosition hex = to; !hex.equals(from); hex = search.previous().get(hex)) {
            steps.add(hex);
        }
        Collections.reverse(steps);
        boolean touchesDifficult = steps.stream().anyMatch(rules::isDifficult);
        return Optional.of(new MovementPath(steps, search.costs().get(to), touchesDifficult));
    }

    private Search search(final GridPosition from, final int budget, final StepRules rules) {
        Map<GridPosition, Integer> costs = new HashMap<>();
        Map<GridPosition, GridPosition> previous = new HashMap<>();
        PriorityQueue<Node> frontier = new PriorityQueue<>(CHEAPEST_FIRST);
        costs.put(from, 0);
        frontier.add(new Node(from, 0));
        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            if (node.cost() > costs.getOrDefault(node.hex(), Integer.MAX_VALUE)) {
                continue;
            }
            // A hex that cannot be passed is a dead end: reached as a destination, never left.
            if (!node.hex().equals(from) && !rules.canPass(node.hex())) {
                continue;
            }
            for (GridPosition next : HexGrid.neighbours(node.hex(), columns, rows)) {
                if (!rules.canPass(next) && !rules.canStop(next)) {
                    continue;
                }
                int cost = node.cost() + Math.max(1, rules.enterCost(next));
                if (cost > budget || cost >= costs.getOrDefault(next, Integer.MAX_VALUE)) {
                    continue;
                }
                costs.put(next, cost);
                previous.put(next, node.hex());
                frontier.add(new Node(next, cost));
            }
        }
        return new Search(costs, previous);
    }

    private record Node(GridPosition hex, int cost) {
    }

    private record Search(Map<GridPosition, Integer> costs, Map<GridPosition, GridPosition> previous) {
    }
}
