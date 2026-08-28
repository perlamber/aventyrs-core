package org.aventyrs.core.magic;

import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_TARGETING;

/**
 * A Magia's reach in full — a {@link SpellReach} together with the parameters that particular
 * reach requires. {@link SpellReach} alone can't describe a Magia (it can't say <em>how far</em>
 * a {@code DISTANCIA} carries, or what shape an {@code AREA_DE_EFEITO} covers), and separate
 * {@code Range}/{@code AreaOfEffect} columns on {@link Spell} would let a Magia state a Toque at
 * Distância Longa. Pairing them here makes both problems go away at once.
 *
 * <h2>Which parameters each reach takes</h2>
 *
 * <pre>
 * reach            range                              area
 * ---------------  ---------------------------------  ---------
 * PESSOAL          absent                             absent
 * TOQUE            absent (Adjacente is implied)      absent
 * DISTANCIA        required                           absent
 * AREA_DE_EFEITO   optional — see below               required
 * </pre>
 *
 * Every row is enforced by the canonical constructor, which throws {@link
 * IllegalOperationException} ({@code INVALID_SPELL_TARGETING}) on any other combination — the
 * same cross-field pairing check {@code org.aventyrs.core.character.DamageBonus} applies to its
 * own type/element pair, and for the same reason: the combination is authored data, and a
 * meaningless one is a mistake rather than a value worth carrying.
 *
 * <p>{@code null} means "not applicable to this reach", the convention {@code
 * DamageBonus#element}, {@code org.aventyrs.core.scene.SceneContext}'s {@code terrainType} and
 * every {@code InteractionResult} field already follow. {@code TOQUE} deliberately stores no
 * {@link Range#ADJACENTE}: the constant already implies it, and storing it too would be authored
 * redundancy that could disagree with itself.
 *
 * <h2>An Área de Efeito's range is its origin</h2>
 *
 * For {@link SpellReach#AREA_DE_EFEITO} alone, {@code range} answers a different question than
 * it does for {@code DISTANCIA} — not "how far can this reach a target" but "how far away may
 * its centre be placed". Absent, the area is centred on the Conjurador themselves ({@link
 * #isCenteredOnCaster()}).
 *
 * <p>Which is why an <b>emanation</b> — a {@link org.aventyrs.core.scene.AreaShape#LINHA} or
 * {@link org.aventyrs.core.scene.AreaShape#CONE}, both of which radiate outward from the
 * Conjurador — may never carry a range: it has no centre to place. That's one clause in the
 * validation below, and relaxing it is deleting that clause, should a Magia ever author a cone
 * originating away from its caster.
 *
 * <h2>Not modelled here</h2>
 *
 * <b>A Conjurador is never damaged by their own Magia.</b> That rule is universal, so it is
 * deliberately not a column — there is no {@code excludesCaster} flag here, on {@link
 * AreaOfEffect}, or on {@link Spell}. It belongs to targeting resolution, which doesn't exist
 * yet, and couldn't be written today regardless: nothing resolves a Magia's target set, and
 * {@link Spell} carries no damage column at all ({@code getPrimaryEffectDescription} is prose),
 * so "does this Magia apply damage" isn't yet an answerable question. Whoever builds targeting
 * resolution has to implement it there.
 *
 * <p>Which hexes an area actually covers isn't resolved either — an emanation additionally needs
 * a <em>facing</em>, and that's chosen per cast by whoever aims the Magia, never a property of
 * the Magia itself.
 */
public record SpellTargeting(SpellReach reach, Range range, AreaOfEffect area) {

    /** Affects only the Conjurador — see {@link SpellReach#PESSOAL}. */
    public static final SpellTargeting PESSOAL = new SpellTargeting(SpellReach.PESSOAL, null, null);

    /** A single target the Conjurador must touch — see {@link SpellReach#TOQUE}. */
    public static final SpellTargeting TOQUE = new SpellTargeting(SpellReach.TOQUE, null, null);

    public SpellTargeting {
        if (reach == null || !isLegalCombination(reach, range, area)) {
            throw new IllegalOperationException(INVALID_SPELL_TARGETING);
        }
    }

    private static boolean isLegalCombination(final SpellReach reach, final Range range, final AreaOfEffect area) {
        return switch (reach) {
            case PESSOAL, TOQUE -> range == null && area == null;
            case DISTANCIA -> range != null && area == null;
            // An emanation radiates from the Conjurador, so it has no centre to place at a Range.
            case AREA_DE_EFEITO -> area != null && !(area.isEmanation() && range != null);
        };
    }

    /** A single target up to range away. */
    public static SpellTargeting distancia(final Range range) {
        return new SpellTargeting(SpellReach.DISTANCIA, range, null);
    }

    /** An area centred on the Conjurador — the only form an emanation can take. */
    public static SpellTargeting areaDeEfeito(final AreaOfEffect area) {
        return new SpellTargeting(SpellReach.AREA_DE_EFEITO, null, area);
    }

    /** An area whose centre is placed up to range away, which only a CIRCULO can be. */
    public static SpellTargeting areaDeEfeito(final Range range, final AreaOfEffect area) {
        return new SpellTargeting(SpellReach.AREA_DE_EFEITO, range, area);
    }

    /** Whether this Magia affects an area rather than a single target. */
    public boolean isAreaOfEffect() {
        return reach == SpellReach.AREA_DE_EFEITO;
    }

    /** Whether this Magia's area originates on the Conjurador rather than at a placed centre. */
    public boolean isCenteredOnCaster() {
        return isAreaOfEffect() && range == null;
    }
}
