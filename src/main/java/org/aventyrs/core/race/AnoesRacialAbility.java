package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * Habilidades Raciais granted to every Anão — see {@link Race#getRacialAbilities()} for why
 * these are modeled as ordinary {@link SkillCompetencyAbility} instances rather than a
 * separate type.
 */
@Getter
@AllArgsConstructor
public enum AnoesRacialAbility implements SkillCompetencyAbility {

    // Vantagem on Ataque rolls against a target 2+ Categorias de Tamanho larger. Wired into
    // AtaqueADistanciaInteraction only for now, so getSkillType() is fixed to
    // ATAQUE_A_DISTANCIA — the rules text actually covers every "Perícia de Ataque" (see
    // SkillType#isAttackSkill()), but AtaqueCorpoACorpoInteraction doesn't yet take an
    // attackTarget parameter the way AtaqueADistanciaInteraction does (see that class's own
    // javadoc for why FRIEZA needed one), so the melee side isn't wired yet.
    ABATEDORES_DE_GIGANTES("Você recebe vantagem nas rolagens de Ataque contra criaturas " +
            "que pertençam a 2 ou mais Categorias de Tamanho superiores.") {
        @Override
        public Optional<Integer> resolveAttackRollBonus(final CharacterSheet actor, final CharacterSheet attackTarget) {
            if (actor == null || attackTarget == null) {
                return Optional.empty();
            }
            int sizeDifference = attackTarget.getCharacter().getSizeCategory().getCategory()
                    - actor.getCharacter().getSizeCategory().getCategory();
            if (sizeDifference < MIN_SIZE_CATEGORIES_DIFFERENCE) {
                return Optional.empty();
            }
            return Optional.of(Skill.ADVANTAGE_BONUS);
        }
    };

    private static final int MIN_SIZE_CATEGORIES_DIFFERENCE = 2;

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }
}
