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

    // Vantagem on Ataque rolls against a target 2+ Categorias de Tamanho larger — the rules
    // text covers every "Perícia de Ataque" (see SkillType#isAttackSkill()), not just one, so
    // getSkillType() below stays a single representative value (ATAQUE_A_DISTANCIA — resolving
    // "which one" doesn't otherwise matter: resolveAttackRollBonus is scanned unconditionally
    // across every held ability, with no per-skillType filter) while matchesSkillType() is
    // overridden so an explicit SkillRoll#requestedAbility naming this ability validates
    // against either attack SkillType, not just the representative one. Actually wired into
    // AtaqueADistanciaInteraction only for now — AtaqueCorpoACorpoInteraction doesn't yet take
    // an attackTarget parameter the way AtaqueADistanciaInteraction does (see that class's own
    // javadoc for why FRIEZA needed one), so the melee side's automatic bonus isn't wired yet,
    // independent of this SkillType-matching fix.
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

        @Override
        public boolean matchesSkillType(final SkillType requestedSkillType) {
            return requestedSkillType != null && requestedSkillType.isAttackSkill();
        }
    };

    private static final int MIN_SIZE_CATEGORIES_DIFFERENCE = 2;

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }
}
