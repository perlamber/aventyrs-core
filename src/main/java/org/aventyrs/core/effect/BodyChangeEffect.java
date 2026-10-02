package org.aventyrs.core.effect;

import java.util.List;

import lombok.Getter;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellBodyChange;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * The applicable form of a Magia's {@link Spell#getBodyChange()} (core 0.0.99) — Polimorfismo's growing and
 * shrinking: Titânecer, Dracônecer and Ogrificar on one branch, Serra-Pernas, Toque de Nanicolina and Enfadecer on the
 * other. One class for the whole tree, parameterized by the authored column, per {@link SpellEffect}'s
 * one-shape-per-branch rule.
 *
 * <p>Applying it grants each part as a timed bonus on the target (see {@link BodyChangeGrant}) and reports them on
 * {@link InteractionResult#getBlessings()}. Nothing else needs telling: a {@code SIZE_CATEGORY} bonus is what {@code
 * CharacterSizeService#getEffectiveSizeCategory(CombatantSheet)} reads, so the target is that size for exactly the
 * Duração, and back to its own when the bonus runs out.
 *
 * <p>Which subcategory it belongs to depends on the change, not the class: {@link Enhancement} is a {@link
 * DefensiveEffect}, {@link Diminishment} an {@link OffensiveEffect}. {@link SpellBodyChange#isHarmful()} decides — so
 * Aura do Encolhimento, which shrinks the caster and allies, files as a Diminishment though it is cast on friends.
 */
@Getter
public abstract class BodyChangeEffect extends AbstractEffect implements SpellEffect {

    private final Spell spell;
    private final SpellBodyChange bodyChange;

    /** The caster's "Força ou Destreza" pick, or {@code null}. */
    private final AttributeDomain chosenAttribute;

    private BodyChangeEffect(final Spell spell, final SpellBodyChange bodyChange, final AttributeDomain chosenAttribute) {
        this.spell = spell;
        this.bodyChange = bodyChange;
        this.chosenAttribute = chosenAttribute;
    }

    /** The effect change describes for spell — a {@link Diminishment} when harmful, else an {@link Enhancement}. */
    public static BodyChangeEffect of(final Spell spell, final SpellBodyChange change, final AttributeDomain chosen) {
        return change.isHarmful() ? new Diminishment(spell, change, chosen) : new Enhancement(spell, change, chosen);
    }

    @Override
    public String getDescription() {
        return spell.getPrimaryEffectDescription();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        List<Blessing> granted = BodyChangeGrant.grant(target, spell, bodyChange, chosenAttribute);
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target))
                .blessings(granted.isEmpty() ? null : granted))
                .build();
    }

    /** A body change that strengthens or enlarges its target. */
    public static final class Enhancement extends BodyChangeEffect implements DefensiveEffect {
        private Enhancement(final Spell spell, final SpellBodyChange change, final AttributeDomain chosen) {
            super(spell, change, chosen);
        }
    }

    /** A body change that weakens or shrinks its target. */
    public static final class Diminishment extends BodyChangeEffect implements OffensiveEffect {
        private Diminishment(final Spell spell, final SpellBodyChange change, final AttributeDomain chosen) {
            super(spell, change, chosen);
        }
    }
}
