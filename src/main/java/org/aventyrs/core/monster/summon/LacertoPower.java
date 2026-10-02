package org.aventyrs.core.monster.summon;

import org.aventyrs.core.util.DiceRoller;

import java.util.ArrayList;
import java.util.List;

/**
 * The six powers an Experimento de Lacerto ("1 dos poderes abaixo, escolhido aleatoriamente") or an Orgulho de Lacerto
 * ("2 dos poderes abaixo") is invoked with. Listed in the rules' order, so a 1d6 face is {@code ordinal() + 1}.
 *
 * <p>Rolled when the Magia is cast (table ruling, 2026-10-01) — see {@link #roll}. Each power's figures differ between
 * the two creatures; {@link NatureSummon} reads them per kind. What each one reaches:
 * <ul>
 *   <li>{@link #INOCULAR_VENENO} and {@link #DEVORAR_INTEIRO} — a Corrente de Efeitos on the creature's attacks,
 *       through {@code MonsterTemplate#resolveAttackEffectChains};</li>
 *   <li>{@link #BRUTO} — RD (Experimento) or RA and Vigor +3 (Orgulho), and +1d6 on its attack, as four Dano Base
 *       scale-ups (1d6+3 → 2d6+3, 2d6+0 → 3d6+0);</li>
 *   <li>{@link #MEMBROS_MULTIPLOS} — +1PA in even Rodadas, and the 3PA pair of attacks at Desvantagem as a held {@code
 *       feat.CriaturaFeat#MEMBROS_MULTIPLOS} (core 0.0.97);</li>
 *   <li>{@link #SOPRO_ELEMENTAL} and {@link #AURA_ELEMENTAL} — {@link ElementalDischarge}s ({@code NatureSummon#breath},
 *       {@code #aura}, core 0.0.97), Natural by table ruling. Who stands in the cone or beside the creature is the
 *       caller's geometry; each target's share is dealt through {@code DamageService}.</li>
 * </ul>
 */
public enum LacertoPower {

    INOCULAR_VENENO("Inocular Veneno"),
    SOPRO_ELEMENTAL("Sopro Elemental"),
    AURA_ELEMENTAL("Aura Elemental"),
    DEVORAR_INTEIRO("Devorar Inteiro"),
    BRUTO("Bruto"),
    MEMBROS_MULTIPLOS("Membros Múltiplos");

    private final String displayName;

    LacertoPower(final String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * count distinct powers, each a 1d6 face — a face already drawn is rolled again ("2 dos poderes", read as two
     * different ones).
     */
    public static List<LacertoPower> roll(final int count, final DiceRoller roller) {
        List<LacertoPower> drawn = new ArrayList<>();
        while (drawn.size() < Math.min(count, values().length)) {
            LacertoPower power = values()[Math.floorMod(roller.rollD6() - 1, values().length)];
            if (!drawn.contains(power)) {
                drawn.add(power);
            }
        }
        return List.copyOf(drawn);
    }
}
