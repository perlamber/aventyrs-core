package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;

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
    },

    // Vantagem on Conhecimentos rolls made under the Natureza Especialização, while this
    // Scene's terrain is Mountain or Cave (see TerrainType/Scene#getTerrainType/
    // SceneContext#isTerrain). Unlike ABATEDORES_DE_GIGANTES above, this ability itself is
    // never the requestedAbility of a roll — ConhecimentosSpecialization#NATUREZA is — so it
    // only fires once a roll both requests (and is therefore validated by
    // AbstractSkillInteraction as actually holding) that Especialização *and* the Scene
    // reports one of these two terrains; getSkillType() below is overridden to CONHECIMENTOS,
    // its actual own Perícia, unlike the enum-level ATAQUE_A_DISTANCIA default.
    FILHOS_DA_MONTANHA("Você recebe Vantagem em rolagens de Conhecimentos: Natureza " +
            "enquanto estiver em terrenos montanhosos ou em cavernas.") {
        @Override
        public SkillType getSkillType() {
            return SkillType.CONHECIMENTOS;
        }

        @Override
        public Optional<Integer> resolveConditionalRollBonus(final SceneContext sceneContext, final SkillTrait requestedAbility) {
            if (sceneContext == null || !sceneContext.isTerrain(TerrainType.MOUNTAIN, TerrainType.CAVE)) {
                return Optional.empty();
            }
            if (requestedAbility != ConhecimentosSpecialization.NATUREZA) {
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
