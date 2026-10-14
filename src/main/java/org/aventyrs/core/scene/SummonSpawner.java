package org.aventyrs.core.scene;

import lombok.Getter;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Something that invokes creatures for its caster at each Rodada boundary while it lasts — Totem de Gaea's "A cada
 * Rodada um novo animal é criado desta forma" (core 0.0.92), Festim dos Mortos' Sarcófago Negro (core 0.1.6.25). See
 * {@link Scene#addSummonSpawner}.
 *
 * <p>Since 0.1.6.25 a spawner is driven by a {@link Rule} rather than one {@code Supplier}: each Rodada it may bring
 * several creatures, some of them {@link Spawned#uncontrolled() uncontrolled} (placed in this spawner's own hostile
 * group, {@link #getHostileGroup()}), and it may hold its creatures back a Rodada ({@link #isActsNextRodada()}) and
 * take them with it when it ends ({@link #isSummonsLeaveWithIt()}).
 */
@Getter
public final class SummonSpawner {

    /** One creature a spawner brings this Rodada, and whether it obeys nobody. */
    public record Spawned(CombatantSheet sheet, boolean uncontrolled) {
    }

    /**
     * What a spawner brings at one firing.
     *
     * @see #spawn(int, int)
     */
    @FunctionalInterface
    public interface Rule {
        /**
         * @param firing how many times it has fired before — 0 at its creation
         * @param room   how many more invocações its caster may control ({@code Scene#remainingSummonRoom})
         */
        List<Spawned> spawn(int firing, int room);
    }

    private final CombatantSheet caster;
    private Integer remainingRounds;
    private final Rule rule;
    /** Each creature's own Duração. */
    private final Integer summonRounds;
    /** "Só podem agir na Rodada seguinte a sua aparição" — its creatures wait for the next Rodada boundary. */
    private final boolean actsNextRodada;
    /** Whether its creatures leave when it ends (Festim dos Mortos, Cuspidor de Pragas). */
    private final boolean summonsLeaveWithIt;
    /** Whether it ends with the Cena de Combate ({@code Scene#endCombat}). */
    private final boolean endsWithCombat;
    /** The sub-group its uncontrolled creatures stand in — hostile to everyone else. */
    private final UUID hostileGroup = UUID.randomUUID();
    private final List<CombatantSheet> spawned = new ArrayList<>();
    private int firings;

    SummonSpawner(final CombatantSheet caster, final Integer remainingRounds, final Rule rule,
                  final Integer summonRounds, final boolean actsNextRodada, final boolean summonsLeaveWithIt,
                  final boolean endsWithCombat) {
        this.caster = caster;
        this.remainingRounds = remainingRounds;
        this.rule = rule;
        this.summonRounds = summonRounds;
        this.actsNextRodada = actsNextRodada;
        this.summonsLeaveWithIt = summonsLeaveWithIt;
        this.endsWithCombat = endsWithCombat;
    }

    /** The one-creature form a {@code Supplier} makes — Totem de Gaea: one controlled creature, while there's room. */
    static Rule single(final Supplier<? extends CombatantSheet> spawn) {
        return (firing, room) -> room > 0 ? List.of(new Spawned(spawn.get(), false)) : List.of();
    }

    /** What it brings now; counts the firing. */
    List<Spawned> fire(final int room) {
        List<Spawned> brought = rule.spawn(firings++, room);
        brought.forEach(creature -> spawned.add(creature.sheet()));
        return brought;
    }

    /** Every creature it brought — what leaves with it when {@link #isSummonsLeaveWithIt()}. */
    public List<CombatantSheet> getSpawned() {
        return List.copyOf(spawned);
    }

    /** One Rodada passes; whether it is now spent. */
    boolean tick() {
        if (remainingRounds == null) {
            return false;
        }
        remainingRounds--;
        return remainingRounds <= 0;
    }
}
