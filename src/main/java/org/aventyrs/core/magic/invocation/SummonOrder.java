package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.monster.summon.LacertoPower;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;

/**
 * One creature an {@link InvocationOrder.Summons} places (core 0.1.7.6) — the figures a server needs to stand it in
 * the Cena, gathered from a {@link InvocationPlan} creature or an {@link UndeadPlan}.
 *
 * @param kind                     the creature's kind — a {@code NatureSummonKind} or {@code UndeadSummonKind} name
 * @param name                     its display name
 * @param conjuradorManaGraduation the caster's Domínio do Mana Graduação its stat block scales with
 * @param powers                   Lacerto's powers, rolled at the cast; empty for the rest
 * @param exclusivityGroup         the group a newer summon of which sends this one away, or {@code null}
 * @param rounds                   its Duração in Rodadas (a Concentração's trailing ones when concentration), or
 *                                 {@code null} for none
 * @param concentration            whether its caster sustains it by Concentração
 * @param enhancement              what the caster's Títulos gave this creature — {@link SummonEnhancement#NONE}
 * @param corpse                   the body it rises from, or {@code null}
 * @param bodySize                 that body's Categoria de Tamanho, or {@code null}
 * @param butcher                  whether it is an Açougueiro Carniçal
 */
public record SummonOrder(String kind, String name, int conjuradorManaGraduation, List<LacertoPower> powers,
                          String exclusivityGroup, Integer rounds, boolean concentration,
                          SummonEnhancement enhancement, CombatantSheet corpse, SizeCategory bodySize,
                          boolean butcher) {

    public SummonOrder {
        powers = powers == null ? List.of() : List.copyOf(powers);
        enhancement = enhancement == null ? SummonEnhancement.NONE : enhancement;
    }
}
