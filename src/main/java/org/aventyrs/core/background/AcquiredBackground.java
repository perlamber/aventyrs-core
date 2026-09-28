package org.aventyrs.core.background;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Optional;

/**
 * An Antecedente as one character holds it — the constant plus every pick made for it. What
 * {@code Character#getBackgrounds()} stores and a consumer persists.
 *
 * <p>Handed to {@code CharacterCreationService#applyBackground} it is the <em>submission</em>: the
 * player's picks, fixed grants optional. What that call stores back on the character is the
 * <em>normalized</em> record — every Perícia that received a Graduação and every trait received,
 * fixed ones included — so it doubles as the provenance of what the Antecedente gave.
 *
 * @param background       the Antecedente
 * @param graduationSkills the Perícias that received its "+1 Graduação"
 * @param traits           the Especializações/Habilidades de Competência it granted
 * @param benefitChoices   answers to {@link Background#resolveBenefitChoices}, in order
 */
public record AcquiredBackground(@NonNull Background background, List<SkillType> graduationSkills,
                                 List<SkillTrait> traits, List<Object> benefitChoices) {

    public AcquiredBackground {
        graduationSkills = graduationSkills == null ? List.of() : List.copyOf(graduationSkills);
        traits = traits == null ? List.of() : List.copyOf(traits);
        benefitChoices = benefitChoices == null ? List.of() : List.copyOf(benefitChoices);
    }

    /** A submission with no benefit picks. */
    public static AcquiredBackground of(final Background background, final List<SkillType> graduationSkills,
                                        final List<SkillTrait> traits) {
        return new AcquiredBackground(background, graduationSkills, traits, List.of());
    }

    public BackgroundKind kind() {
        return background.getKind();
    }

    /** The first benefit pick of type, if any — e.g. Estudioso Arcano's {@code SpellTree}. */
    public <T> Optional<T> benefitChoice(final Class<T> type) {
        return benefitChoices.stream().filter(type::isInstance).map(type::cast).findFirst();
    }

    /**
     * "A Perícia escolhida" of an Antecedente whose Perícias line is a single choice — Aprendiz,
     * Trombadinha, Negociador, Natureza Longínqua. The Perícia its chosen Graduação went to.
     */
    public Optional<SkillType> chosenSkill() {
        return background.getGraduationGrants().stream()
                .filter(GraduationGrant::isChoice)
                .flatMap(grant -> graduationSkills.stream().filter(grant.options()::contains))
                .findFirst();
    }

    /** The Antecedente character holds as background, if it does. */
    public static Optional<AcquiredBackground> heldBy(final Character character, final Background background) {
        if (character == null) {
            return Optional.empty();
        }
        return character.getBackgrounds().stream()
                .filter(held -> held.background() == background)
                .findFirst();
    }
}
