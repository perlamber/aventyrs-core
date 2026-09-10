package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * An {@link Item}'s Favor — the real, mechanical benefit it grants, but only to a wielder who
 * meets its {@link ItemRequirements}. Unlike the item's flat DF/DM/Dureza/Conjuração columns,
 * which apply to anyone carrying it, everything here is conditional on {@link
 * #isGrantedTo(Character)}.
 *
 * <p><b>The bonuses are data, not prose.</b> A Favor grants a bonus of any sort, the same way a
 * {@code SkillCompetencyAbility} does — so it carries a list of {@link ItemBonus}es typed by
 * {@link ModifierType} (e.g. Armadura Completa's own "Dano de Corte sofrido é reduzido em -2"
 * is {@code DAMAGE_REDUCTION} 2, RD being fully mechanically real — see {@code
 * org.aventyrs.core.character.services.DamageService#getTotalDamageReduction}), resolved via
 * {@link #resolveBonus(ModifierType, Character)}. This is *data* rather than a set of
 * {@code @Modifier}-annotated methods, unlike every ability enum: {@code @Modifier}'s
 * {@code ModifierType} is a compile-time-fixed annotation value, so a single shared {@code
 * ItemFavor} class could never vary which type a given item grants — the identical limitation
 * CLAUDE.md documents in "A ModifierType per skill". {@code getDescription()} stays alongside
 * as the rules text, the same single-source-of-truth convention every ability enum follows for
 * its own description.
 *
 * <p>A Favor clause with no {@code ModifierType} to express it yet simply contributes no
 * {@link ItemBonus} and lives on in {@link #getDescription()} until the mechanism it needs
 * exists — same "TODO the application, not the arithmetic" discipline used elsewhere.
 *
 * <p>{@code additionalEffects} is the item's "Efeitos Adicionais" line — further effects some
 * items grant on top of the Favor itself, once that same requirement is met. Most items have
 * none ("Efeitos Adicionais: Nenhum"), which is exactly {@code null} here — same
 * stays-{@code null}-when-not-applicable convention as every optional field on {@code
 * org.aventyrs.core.sheet.InteractionResult}; use {@link #hasAdditionalEffects()} rather than
 * comparing against an empty string. It stays free text: unlike the Favor's own bonus, what an
 * Efeito Adicional does varies too widely per item to have a shared shape yet.
 */
@Getter
@Builder
public class ItemFavor {

    private final String description;
    private final ItemRequirements requirements;

    /**
     * Every bonus this Favor grants, never {@code null} — empty for a Favor whose effect this
     * core can't express as a {@link ModifierType} yet.
     */
    @Singular("bonus")
    private final List<ItemBonus> bonuses;

    private final String additionalEffects;

    /**
     * Whether character meets this Favor's own requirement — so its {@link #getBonuses()},
     * {@link #getDescription()} and any {@link #getAdditionalEffects()} all apply to them. A
     * Favor with no {@link ItemRequirements} at all is granted unconditionally.
     */
    public boolean isGrantedTo(final Character character) {
        return requirements == null || requirements.isMetBy(character);
    }

    /**
     * How much of modifierType this Favor currently grants character — the sum of every
     * matching {@link ItemBonus} (additive, the same convention every other bonus source in
     * this core follows), or 0 when the requirement isn't met or nothing of that type is
     * granted. A bonus carrying a non-{@link FavorCondition#NONE} {@code condition} is excluded
     * on this path — there is no sheet to judge it against, which reads as "cannot tell"; use
     * {@link #resolveBonus(ModifierType, CombatantSheet)} for those.
     */
    public int resolveBonus(final ModifierType modifierType, final Character character) {
        return resolveBonus(modifierType, character, null);
    }

    /**
     * As {@link #resolveBonus(ModifierType, Character)}, but able to judge a bonus's {@link
     * ItemBonus#condition()} against the wielder's live {@link CombatantSheet} — the Escudos'
     * "se não realizou ação ofensiva nesta Rodada" bonuses only resolve here.
     */
    public int resolveBonus(final ModifierType modifierType, final CombatantSheet sheet) {
        return resolveBonus(modifierType, sheet == null ? null : sheet.getCharacter(), sheet);
    }

    private int resolveBonus(final ModifierType modifierType, final Character character,
                             final CombatantSheet sheet) {
        if (character == null || !isGrantedTo(character)) {
            return 0;
        }
        return bonuses.stream()
                .filter(bonus -> bonus.modifierType() == modifierType)
                .filter(bonus -> bonus.condition().isMetBy(sheet))
                .mapToInt(ItemBonus::value)
                .sum();
    }

    /**
     * Every bonus this Favor grants character right now — its {@link #getBonuses()} list when
     * the requirement is met (minus any conditioned bonus, which this path can't judge), empty
     * otherwise. For a caller applying all of them at once rather than asking one {@link
     * ModifierType} at a time.
     */
    public List<ItemBonus> resolveBonuses(final Character character) {
        if (!isGrantedTo(character)) {
            return List.of();
        }
        return bonuses.stream().filter(bonus -> bonus.condition() == FavorCondition.NONE).toList();
    }

    /**
     * Every bonus this Favor grants the wielder of sheet right now — {@link #resolveBonuses(Character)}
     * plus whichever conditioned bonuses currently hold.
     */
    public List<ItemBonus> resolveBonuses(final CombatantSheet sheet) {
        if (sheet == null || !isGrantedTo(sheet.getCharacter())) {
            return List.of();
        }
        return bonuses.stream().filter(bonus -> bonus.condition().isMetBy(sheet)).toList();
    }

    /** Whether this Favor carries an "Efeitos Adicionais" line beyond the Favor itself. */
    public boolean hasAdditionalEffects() {
        return additionalEffects != null;
    }
}
