package org.aventyrs.core.magic.invocation;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.SUMMON_LIMIT_REACHED;

/**
 * The "número máximo de invocações" (Festim dos Mortos) — a caster's <b>Foco</b> (table ruling, 2026-10-14; Carisma
 * stays the Subordinado cap). It counts every invocação the caster controls in the Scene, nature summons included, and
 * every summon Magia is refused at it; only Festim dos Mortos goes past, its excess rising uncontrolled (core
 * 0.1.6.24).
 *
 * <p>⚠️ Readings: a summon already marked to leave at the caster's Turn end (an older one of a replaced exclusivity
 * group) doesn't count, and neither do those this very cast replaces.
 */
public final class SummonLimit {

    private SummonLimit() {
    }

    /** caster's limit — their effective Foco. */
    public static int of(@NonNull final CombatantSheet caster) {
        return Math.max(0, caster.getAttributeTotal(AttributeDomain.FOCUS));
    }

    /** How many more invocações caster may control here, before a cast of exclusivityGroup replaces any. */
    public static int remaining(@NonNull final Scene scene, @NonNull final CombatantSheet caster,
                                final String exclusivityGroup) {
        return scene.remainingSummonRoom(caster, exclusivityGroup);
    }

    /**
     * Refuses a cast that would leave caster controlling more than their limit.
     *
     * @throws IllegalOperationException ({@code SUMMON_LIMIT_REACHED})
     */
    public static void requireRoom(@NonNull final Scene scene, @NonNull final CombatantSheet caster, final int adding,
                                   final String exclusivityGroup) {
        if (adding > remaining(scene, caster, exclusivityGroup)) {
            throw new IllegalOperationException(SUMMON_LIMIT_REACHED);
        }
    }

    /** Whether summon counts against its caster's limit. */
    public static boolean counts(@NonNull final SceneSummon summon) {
        return !summon.isUncontrolled() && !summon.isReplaced();
    }
}
