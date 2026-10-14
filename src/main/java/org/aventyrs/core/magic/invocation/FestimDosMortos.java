package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.monster.summon.UndeadSummonKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Festim dos Mortos' Sarcófago Negro, as a pure table (core 0.1.6.25) — what one Rodada's 1d6 brings, so the core
 * {@code Scene} and a server running its own Scene read the same rule.
 *
 * <ul>
 *   <li>"resultados de 1 a 3 indicam que um Zumbi se levantará dele, resultados 4 e 5 indicam a presença de Carniçal,
 *   resultados 6 indicam a presença de um Abantesma" — {@link #kindFor}.</li>
 *   <li>"Se seu número máximo de invocações for superado mortos-vivos continuaram sendo invocados sem obedecer a
 *   nenhum mestre" — a creature beyond the caster's Foco ({@code room} 0) rises uncontrolled.</li>
 *   <li>Horda Faminta: "adicionalmente à criatura invocada aleatoriamente, um zumbi será invocado. Você não possui
 *   controle de nenhum morto-vivo invocado desta forma" — everything uncontrolled, plus a Zumbi.</li>
 * </ul>
 *
 * <p>Its creatures are raised from no corpse, so they leave nothing behind; they "não podem ter a Duração estendida",
 * leave with the sarcophagus ({@link #ROUNDS}), and act from the next Rodada.
 */
public final class FestimDosMortos {

    /** "Duração: 3 Rodadas". */
    public static final int ROUNDS = 3;

    /** "Se você possuir 10 Graduações em Domínio do Mana … GD reduzida em -2 Níveis". */
    public static final int ALLIED_UNDEAD_DIFFICULTY_REDUCTION = 2;

    /** The Graduação that clause asks for. */
    public static final int ALLIED_UNDEAD_TIER = 10;

    /** One creature the sarcophagus brings, and whether it obeys nobody. */
    public record Spawn(UndeadSummonKind kind, boolean uncontrolled) {
    }

    private FestimDosMortos() {
    }

    /**
     * "Se você possuir 10 Graduações em Domínio do Mana, todos os Mortos-Vivos aliados (mas não você mesmo) que estejam
     * em Distância Curta do Sarcófago Negro ou Jazigo Profano efetuam rolagens de Perícia com a GD reduzida em -2
     * Níveis" — the níveis for one roller; whether it is an allied undead within Curta of the sarcophagus is the
     * caller's (positions are).
     */
    public static int alliedUndeadDifficultyReduction(final int casterManaGraduation,
                                                      final boolean alliedUndeadWithinShortRange) {
        return casterManaGraduation >= ALLIED_UNDEAD_TIER && alliedUndeadWithinShortRange
                ? ALLIED_UNDEAD_DIFFICULTY_REDUCTION : 0;
    }

    /** The creature a face of the die names. */
    public static UndeadSummonKind kindFor(final int face) {
        if (face <= 3) {
            return UndeadSummonKind.ZUMBI;
        }
        return face <= 5 ? UndeadSummonKind.CARNICAL : UndeadSummonKind.ABANTESMA;
    }

    /**
     * What one Rodada's face brings.
     *
     * @param room how many more invocações the caster may control
     */
    public static List<Spawn> spawnsFor(final int face, final boolean hordaFaminta, final int room) {
        List<Spawn> spawns = new ArrayList<>();
        spawns.add(new Spawn(kindFor(face), hordaFaminta || room <= 0));
        if (hordaFaminta) {
            spawns.add(new Spawn(UndeadSummonKind.ZUMBI, true));
        }
        return List.copyOf(spawns);
    }
}
