package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ResourceType;

/**
 * The one rule every PV, PM and PD maximum follows (core 0.1.0): {@code base + Atributo × Multiplicador}, with the
 * Atributo and the Multiplicador each read the same way for all three pools.
 *
 * <ul>
 *   <li><b>The Atributo</b> is {@link Character#getEffectiveAttributeTotal(org.aventyrs.core.character.AttributeDomain,
 *       CombatantSheet)} — so with a sheet in hand it carries a timed Bônus Variável (Titânecer's, a Frenesi's) and
 *       loses a racial bonus a Forma suppresses, for every pool alike. Before this, only PV read it; PM and PD read
 *       the permanent total and missed both.</li>
 *   <li><b>The Multiplicador</b> is the character's own figure plus the pool's traits ({@code @Modifier} scan and
 *       Talentos, which each service supplies), then — only with a sheet — every held Condição carrying the pool's
 *       {@link ResourceType#getMultiplierModifier()} (Envenenado) and every timed bonus of it (Titânecer's +2, Ferida
 *       Profunda's loss, a Choque de AEther's drain). Those transient changes never take it below 1, then {@code
 *       Feat#fixedMultiplier} (Sobreposição) applies last, as it always did.</li>
 * </ul>
 *
 * <p>Nothing is stored: a maximum is derived on every read, so a bonus taking hold or running out moves it at once.
 * What a caller holding a cached figure needs is to <i>notice</i> the move — {@link CombatantStatsService#snapshot}
 * is the value to compare. The pools' spent/damage figures stay fixed when a maximum moves (table ruling,
 * 2026-10-02): current = maximum − spent, so a lapsing bonus lowers current points with it.
 */
final class ResourcePoolFormula {

    private ResourcePoolFormula() {
    }

    /** The pool's Atributo total — sheet-aware when sheet is given. */
    static int attribute(final ResourceType type, final Character character, final CombatantSheet sheet) {
        return character.getEffectiveAttributeTotal(type.getGoverningAttribute(), sheet);
    }

    /**
     * The pool's Multiplicador.
     *
     * @param traitBonus what the holder's traits add on top of {@code character}'s own figure — the {@code @Modifier}
     *                   scan and every Talento's increase, which differ per pool and so are the caller's to sum
     */
    static int multiplier(final ResourceType type, final Character character, final CombatantSheet sheet,
                          final int traitBonus) {
        int permanent = ownMultiplier(type, character) + traitBonus;
        int transientChange = 0;
        if (sheet != null) {
            transientChange += sheet.getConditionBonus(type.getMultiplierModifier(), null);
            transientChange += sheet.getTemporaryBonus(type.getMultiplierModifier());
        }
        // A Condição or a timed loss never takes the figure below 1 — "a stack of Malefícios can never drive a pool to
        // zero by arithmetic alone" — but a creature authored below 1 is not raised to it either.
        int floor = Math.min(1, permanent);
        return Feat.fixedMultiplier(type, character, Math.max(floor, permanent + transientChange));
    }

    private static int ownMultiplier(final ResourceType type, final Character character) {
        return switch (type) {
            case HIT_POINTS -> character.getLifeMultiplier();
            case MAGIC_POINTS -> character.getManaMultiplier();
            case DETERMINATION_POINTS -> character.getDeterminationMultiplier();
        };
    }
}
