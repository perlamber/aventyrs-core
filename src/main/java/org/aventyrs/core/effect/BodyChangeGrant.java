package org.aventyrs.core.effect;

import java.util.ArrayList;
import java.util.List;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.magic.AlternateSpellVersion;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellBodyChange;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.sheet.TemporaryBonus;

/**
 * Turns a {@link SpellBodyChange} into the round-scoped bonuses it grants, shared by {@link BodyChangeEffect} (the
 * Magia's own {@code Efeito:}) and {@link BodyChangeChain} (a Corrente that changes the body too).
 *
 * <p>Each part is a {@link Blessing} on the target for the Magia's Duração, read where that stat already reads a
 * timed bonus: {@code SIZE_CATEGORY} by {@code CharacterSizeService}'s sheet overload (what draws a token's size),
 * {@code LIFE_MULTIPLIER} by {@code HitPointsService}'s, and the {@code <ATTR>_BONUS} pair on a Perícia roll governed
 * by that Atributo (the documented partial reach of a round-scoped Atributo bonus — PV/PM/PD do not see it).
 *
 * <p><b>Every grant carries the Magia's name as its source</b>, so a second cast of the same Magia on the same target
 * renews the Duração instead of stacking, and a Corrente that replaces the {@code Efeito:} ("Em substituição ao
 * efeito anterior" — Gigantecer) does so simply by granting the same {@code ModifierType} under the same source.
 */
final class BodyChangeGrant {

    private BodyChangeGrant() {
    }

    /** Grants change to target for spell's Duração, chosen being the caster's "Força ou Destreza" pick. */
    static List<Blessing> grant(final CombatantSheet target, final Spell spell, final SpellBodyChange change,
                                final AttributeDomain chosen) {
        int rounds = spell.getDuration() == null ? 0 : spell.getDuration().inRodadas().orElse(0);
        if (rounds <= 0) {
            return List.of();
        }
        String source = sourceOf(spell);
        List<Blessing> granted = new ArrayList<>();
        add(granted, ModifierType.SIZE_CATEGORY, change.sizeCategoryShift(), rounds, source);
        add(granted, ModifierType.LIFE_MULTIPLIER, change.lifeMultiplierIncrease(), rounds, source);
        for (AttributeDomain domain : change.affectedAttributes(chosen)) {
            add(granted, domain.getBonusModifierType(), attributeValue(target, domain, change, source), rounds,
                    source);
        }
        granted.forEach(target::grantBlessing);
        return List.copyOf(granted);
    }

    /** The source every grant of spell carries — its base Magia's name, shared by an Efeito Alternativo. */
    static String sourceOf(final Spell spell) {
        Spell base = spell instanceof AlternateSpellVersion alternate ? alternate.getParent() : spell;
        return base.getName();
    }

    private static void add(final List<Blessing> granted, final ModifierType type, final int value, final int rounds,
                            final String source) {
        if (value != 0) {
            granted.add(new Blessing(type, value, rounds, TargetScope.SINGLE_TARGET, source));
        }
    }

    /**
     * The bonus one Atributo receives. A harmful change stops at {@link SpellBodyChange#MINIMUM_ATTRIBUTE}, judged
     * against the target's total as it stands — minus whatever this same Magia already holds on it, since this grant
     * replaces that one.
     */
    private static int attributeValue(final CombatantSheet target, final AttributeDomain domain,
                                      final SpellBodyChange change, final String source) {
        if (change.attributesSetTo() == null && change.attributeChange() > 0) {
            return change.attributeChange();
        }
        ModifierType type = domain.getBonusModifierType();
        int current = target.getCharacter().getEffectiveAttributeTotal(domain)
                + target.getTemporaryBonus(type) - heldFrom(target, type, source);
        int floor = change.attributesSetTo() != null
                ? Math.max(change.attributesSetTo(), SpellBodyChange.MINIMUM_ATTRIBUTE)
                : SpellBodyChange.MINIMUM_ATTRIBUTE;
        int malus = change.attributesSetTo() != null ? floor - current : change.attributeChange();
        return Math.min(0, Math.max(malus, floor - current));
    }

    private static int heldFrom(final CombatantSheet target, final ModifierType type, final String source) {
        return target.getRunningEffects().stream()
                .filter(TemporaryBonus.class::isInstance)
                .map(TemporaryBonus.class::cast)
                .filter(bonus -> bonus.getType() == type && source.equals(bonus.getSource()))
                .mapToInt(TemporaryBonus::getValue)
                .sum();
    }
}
