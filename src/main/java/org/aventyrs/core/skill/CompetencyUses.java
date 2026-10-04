package org.aventyrs.core.skill;

import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.util.TranslatableMessages;

import java.util.List;

/**
 * Spends the uses of a limited-use Habilidade de Competência — Salto Poderoso, Instinto de Luther, Socorro Imediato,
 * Recuo Rápido — off the ledger its {@link SkillCompetencyAbility#getUseWindow()} names, and grants what a use
 * grants ({@link SkillCompetencyAbility#resolveActivationBlessings}).
 *
 * <p>What a use <i>does</i> beyond Blessings — a movement that ignores Terreno Difícil, a Perícia roll priced as an
 * Ação Livre, a Reposicionar paid with a Reação — is the caller's half, exactly as for every other trait this
 * core reports rather than applies.
 */
public final class CompetencyUses {

    private CompetencyUses() {
    }

    /** How many uses holder has left of ability right now — 0 for one it does not hold or that is not limited. */
    public static int remaining(final CombatantSheet holder, final SkillCompetencyAbility ability) {
        if (holder == null || ability.getUseWindow() == null
                || !SkillCompetencyAbility.allFor(holder.getCharacter(), holder).contains(ability)) {
            return 0;
        }
        int limit = ability.resolveUseLimit(holder.getCharacter());
        return Math.max(0, limit - spent(holder, ability));
    }

    /**
     * Spends one use and grants its Blessings to holder.
     *
     * @return the Blessings granted, for a caller to log
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NO_USES_LEFT} when none remain
     */
    public static List<Blessing> use(final CombatantSheet holder, final SkillCompetencyAbility ability) {
        if (remaining(holder, ability) <= 0) {
            throw new IllegalOperationException(TranslatableMessages.COMPETENCY_ABILITY_NO_USES_LEFT);
        }
        String key = keyOf(ability);
        switch (ability.getUseWindow()) {
            case ROUND -> holder.spendRoundScopedUse(key);
            case CENA -> holder.incrementCombatCounter(key);
            case LONG_REST -> holder.spendOrdinaryRestScopedUse(key, RestType.LONGO);
        }
        List<Blessing> blessings = ability.resolveActivationBlessings(holder.getCharacter());
        blessings.forEach(holder::grantBlessing);
        return blessings;
    }

    /** Charme Feérico's "2PD". */
    public static final int CHARME_FEERICO_COST = 2;

    /**
     * Whether holder may still spend Charme Feérico on creature this Cena — held, enough PD, and not yet spent on
     * that creature ("apenas uma vez para cada criatura", reset per Cena by table ruling).
     */
    public static boolean canCharm(final CombatantSheet holder, final CombatantSheet creature) {
        return holder != null && creature != null
                && SkillCompetencyAbility.allFor(holder.getCharacter(), holder)
                        .contains(org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemCompetencyAbility.CHARME_FEERICO)
                && new org.aventyrs.core.character.services.DeterminationPointsServiceImpl()
                        .getCurrentDeterminationPoints(holder.getCharacter(), holder) >= CHARME_FEERICO_COST
                && !creature.isAffectedThisCombat(new CharmMark(holder));
    }

    /**
     * Spends Charme Feérico on creature: 2PD, and the creature marked for this Cena. The reroll of the failed
     * Empatia Selvagem roll is the caller's.
     *
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NO_USES_LEFT} when {@link #canCharm} is false
     */
    public static void charm(final CombatantSheet holder, final CombatantSheet creature) {
        if (!canCharm(holder, creature)) {
            throw new IllegalOperationException(TranslatableMessages.COMPETENCY_ABILITY_NO_USES_LEFT);
        }
        holder.spendDeterminationPoints(CHARME_FEERICO_COST);
        creature.markAffectedThisCombat(new CharmMark(holder));
    }

    /** Which holder's Charme a creature has already felt this Cena. */
    private record CharmMark(CombatantSheet holder) {
    }

    /**
     * Whether ally may take Instinto de Luther's allies' half now: some ally of theirs in context holds it with
     * Atenção at {@code AttentionCompetencyAbility#LUTHER_ALLY_GRADUATION}+, and ally has not taken it this Cena.
     */
    public static boolean allyInstinctAvailable(final CombatantSheet ally,
                                                final org.aventyrs.core.scene.SceneContext context) {
        if (ally == null || context == null || allyInstinctTaken(ally)) {
            return false;
        }
        return context.getAllies().stream().anyMatch(other -> other != ally && offersAllyInstinct(other));
    }

    /** Whether ally has already taken Instinto de Luther's allies' half this Cena. */
    public static boolean allyInstinctTaken(final CombatantSheet ally) {
        return ally.getCombatCounter(ALLY_INSTINCT_KEY) > 0;
    }

    /**
     * Takes Instinto de Luther's allies' half: one extra Reação for this Rodada, once per Cena. Which ally holds
     * Luther is the caller's to vouch for — {@link #allyInstinctAvailable} when it holds that ally's real sheet,
     * the holder's own announcement when it holds only a stand-in.
     *
     * @throws IllegalOperationException {@code COMPETENCY_ABILITY_NO_USES_LEFT} when already taken this Cena
     */
    public static Blessing useAllyInstinct(final CombatantSheet ally) {
        if (allyInstinctTaken(ally)) {
            throw new IllegalOperationException(TranslatableMessages.COMPETENCY_ABILITY_NO_USES_LEFT);
        }
        ally.incrementCombatCounter(ALLY_INSTINCT_KEY);
        Blessing blessing = new Blessing(org.aventyrs.core.modifier.ModifierType.REACTIONS, 1, 1,
                org.aventyrs.core.sheet.TargetScope.SELF, ALLY_INSTINCT_KEY);
        ally.grantBlessing(blessing);
        return blessing;
    }

    /** Whether holder qualifies to offer its allies Instinto de Luther's half — held at Atenção 5+. */
    public static boolean offersAllyInstinct(final CombatantSheet holder) {
        return holder != null
                && SkillCompetencyAbility.allFor(holder.getCharacter(), holder)
                        .contains(org.aventyrs.core.skill.attention.AttentionCompetencyAbility.INSTINTO_DE_LUTHER)
                && holder.getCharacter().getEffectiveGraduation(SkillType.ATTENTION)
                        >= org.aventyrs.core.skill.attention.AttentionCompetencyAbility.LUTHER_ALLY_GRADUATION;
    }

    private static final String ALLY_INSTINCT_KEY = "COMPETENCY:ATTENTION:INSTINTO_DE_LUTHER:ALLY";

    /**
     * Restores a persisted rest-scoped use count — what a client reloading {@code getAllRestScopedUses} calls for
     * every entry. One of this class's own keys renews on any Descanso Longo; any other source keeps the
     * Verdadeiro-only reset it was persisted under.
     */
    public static void restoreRestScopedUses(final CombatantSheet sheet, final String source, final int uses) {
        if (source != null && source.startsWith(KEY_PREFIX)) {
            sheet.restoreOrdinaryRestScopedUses(source, uses, RestType.LONGO);
        } else {
            sheet.restoreRestScopedUses(source, uses, RestType.LONGO);
        }
    }

    private static final String KEY_PREFIX = "COMPETENCY:";

    /** The ledger key — one per ability, shared by every window. */
    public static String keyOf(final SkillCompetencyAbility ability) {
        return KEY_PREFIX + ability.getSkillType().name() + ":"
                + (ability instanceof Enum<?> constant ? constant.name() : ability.getClass().getSimpleName());
    }

    private static int spent(final CombatantSheet holder, final SkillCompetencyAbility ability) {
        String key = keyOf(ability);
        return switch (ability.getUseWindow()) {
            case ROUND -> holder.getRoundScopedUses(key);
            case CENA -> holder.getCombatCounter(key);
            case LONG_REST -> holder.getRestScopedUses(key);
        };
    }
}
