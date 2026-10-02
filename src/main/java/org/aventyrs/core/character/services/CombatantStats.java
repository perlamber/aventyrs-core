package org.aventyrs.core.character.services;

import java.util.Map;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.SizeCategory;

/**
 * Everything derived about a combatant that a timed effect can move, read at one moment (core 0.1.0) — the value a
 * caller caching any of it compares to know it must reload. Built by {@link CombatantStatsService#snapshot}.
 *
 * <p>Core stores none of these figures: each is derived on read from the sheet, so a Bônus Variável taking hold or
 * running out changes the next read with no notification of its own. A client holding a resource bar, a status badge
 * or a token size takes a snapshot after anything that may have changed the sheet — a landed Magia, an activation, a
 * Turn's tick — and reloads only what differs. A record, so {@code equals} is that comparison.
 *
 * @param attributes every Atributo's current total, through the sheet (timed bonuses in, suppressed racial bonuses out)
 */
public record CombatantStats(int maxHitPoints, int currentHitPoints, int lifeMultiplier,
                             int maxMagicPoints, int currentMagicPoints, int manaMultiplier,
                             int maxDeterminationPoints, int currentDeterminationPoints, int determinationMultiplier,
                             CharacterStatus status, SizeCategory sizeCategory,
                             Map<AttributeDomain, Integer> attributes) {

    public CombatantStats {
        attributes = Map.copyOf(attributes);
    }

    /** Whether any PV/PM/PD figure or the status differs from other's — what a resource bar or badge must reload. */
    public boolean poolsDifferFrom(final CombatantStats other) {
        return other == null || maxHitPoints != other.maxHitPoints || currentHitPoints != other.currentHitPoints
                || maxMagicPoints != other.maxMagicPoints || currentMagicPoints != other.currentMagicPoints
                || maxDeterminationPoints != other.maxDeterminationPoints
                || currentDeterminationPoints != other.currentDeterminationPoints || status != other.status;
    }

    /** Whether the Categoria de Tamanho differs from other's — what a token must be redrawn for. */
    public boolean sizeDiffersFrom(final CombatantStats other) {
        return other == null || sizeCategory != other.sizeCategory;
    }
}
