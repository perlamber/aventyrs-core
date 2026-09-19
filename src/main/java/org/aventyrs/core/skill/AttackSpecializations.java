package org.aventyrs.core.skill;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaSpecialization;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoSpecialization;

import java.util.Arrays;
import java.util.Optional;

/**
 * Which Especialização de Ataque an {@link AttackSource} is rolled with — the rule that lets a
 * caller start an attack from the <em>weapon</em> and have both halves of the roll follow from it:
 * the Perícia is the weapon's own {@link Weapon#getSkillType()}, and the Especialização is {@link
 * #requiredFor}. A player never picks either; if they hold the one this answers, the roll requests
 * it (and is judged against {@link DifficultyLevel#getExpertValue()}), and otherwise it is a plain
 * roll of the Perícia.
 *
 * <p><b>One Especialização per source, category before weight.</b> The descriptions overlap — a
 * light dagger thrown is both "técnicas de arremesso" and "Categoria Base Leve" — and the reading
 * adopted here is that the more specific clause wins:
 * <ul>
 *   <li>an Arma Natural ({@link Character#treatsAsNaturalWeapon}, so a Talento-reclassified weapon
 *   counts, and {@code NaturalWeapon#ATAQUE_DESARMADO} too) swung corpo-a-corpo → {@link
 *   AtaqueCorpoACorpoSpecialization#PRIMAL}, whatever it weighs. A <em>ranged</em> Arma Natural (an
 *   Arma de Sopro) has no Especialização that names it, so it answers empty;</li>
 *   <li>an {@link ItemCategory#THROWABLE} → {@link AtaqueADistanciaSpecialization#TECNICAS_DE_ARREMESSO};</li>
 *   <li>every other weapon → the Especialização of its own Perícia whose {@code weightClasses}
 *   contains the weapon's <b>base</b> {@link org.aventyrs.core.item.Item#getWeightClass()} — the
 *   "Categoria Base" the descriptions name, not the effective class an Obra-Prima shifts;</li>
 *   <li>a {@link Spell} → {@link AtaqueADistanciaSpecialization#CONJURADOR_DE_LINHA_DE_TRAS} or
 *   {@link AtaqueCorpoACorpoSpecialization#ARCANISTA_DE_LINHA_DE_FRENTE}, by its {@link
 *   Spell#getAttackSkillType()}.</li>
 * </ul>
 *
 * <p><b>{@code ARMAS_TECNOLOGICAS} is never answered</b>, for either Perícia: no Item carries a
 * technological marker (Equipamentos Tecnológicos are {@code <em produção>} stubs in {@code
 * docs/rules/equipamentos-index.md}). Add the branch with the column.
 *
 * <p>{@link AbstractSkillInteraction} enforces the same answer: a roll that requests an
 * Especialização de Ataque while naming an {@code attackSource} it doesn't fit is refused.
 */
public final class AttackSpecializations {

    private AttackSpecializations() {
    }

    /**
     * The Especialização source is rolled with, whether or not character holds it — empty for a
     * {@code null} source, a source whose Perícia is not a Perícia de Ataque, and the cases the
     * class javadoc names. character may be {@code null}, in which case only a weapon whose own
     * category is {@link ItemCategory#NATURAL_WEAPON} counts as natural.
     */
    public static Optional<SkillSpecialization> requiredFor(final AttackSource source, final Character character) {
        if (source instanceof Spell spell) {
            return forSpell(spell.getAttackSkillType());
        }
        if (!(source instanceof Weapon weapon) || weapon.getSkillType() == null) {
            return Optional.empty();
        }
        boolean natural = character == null
                ? weapon.getCategory() == ItemCategory.NATURAL_WEAPON
                : character.treatsAsNaturalWeapon(weapon);
        return switch (weapon.getSkillType()) {
            case ATAQUE_CORPO_A_CORPO -> natural
                    ? Optional.of(AtaqueCorpoACorpoSpecialization.PRIMAL)
                    : byWeight(AtaqueCorpoACorpoSpecialization.values(), weapon.getWeightClass());
            case ATAQUE_A_DISTANCIA -> {
                if (natural) {
                    yield Optional.empty();
                }
                yield weapon.getCategory() == ItemCategory.THROWABLE
                        ? Optional.of(AtaqueADistanciaSpecialization.TECNICAS_DE_ARREMESSO)
                        : byWeight(AtaqueADistanciaSpecialization.values(), weapon.getWeightClass());
            }
            default -> Optional.empty();
        };
    }

    /**
     * {@link #requiredFor}, kept only when character actually holds it — read through {@link
     * Character#getSpecializations(SkillType)}, so a Talento-granted Especialização counts. This is
     * what a caller hands to {@link SkillRoll} as its {@code requestedAbility}: present means an
     * expert roll, empty means a plain roll of the weapon's Perícia.
     */
    public static Optional<SkillSpecialization> heldFor(final AttackSource source, final Character character) {
        if (character == null) {
            return Optional.empty();
        }
        return requiredFor(source, character)
                .filter(specialization -> character.getSpecializations(specialization.getSkillType())
                        .contains(specialization));
    }

    private static Optional<SkillSpecialization> forSpell(final SkillType attackSkillType) {
        if (attackSkillType == SkillType.ATAQUE_A_DISTANCIA) {
            return Optional.of(AtaqueADistanciaSpecialization.CONJURADOR_DE_LINHA_DE_TRAS);
        }
        if (attackSkillType == SkillType.ATAQUE_CORPO_A_CORPO) {
            return Optional.of(AtaqueCorpoACorpoSpecialization.ARCANISTA_DE_LINHA_DE_FRENTE);
        }
        return Optional.empty();
    }

    private static <S extends Enum<S> & SkillSpecialization> Optional<SkillSpecialization> byWeight(
            final S[] specializations, final ItemWeightClass weightClass) {
        if (weightClass == null) {
            return Optional.empty();
        }
        return Arrays.stream(specializations)
                .filter(specialization -> weightClassesOf(specialization).contains(weightClass))
                .<SkillSpecialization>map(specialization -> specialization)
                .findFirst();
    }

    private static java.util.Set<ItemWeightClass> weightClassesOf(final SkillSpecialization specialization) {
        if (specialization instanceof AtaqueCorpoACorpoSpecialization melee) {
            return melee.getWeightClasses();
        }
        if (specialization instanceof AtaqueADistanciaSpecialization ranged) {
            return ranged.getWeightClasses();
        }
        return java.util.Set.of();
    }
}
