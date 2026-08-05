package org.aventyrs.core.skill.ataqueadistancia;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtaqueADistanciaCompetencyAbilityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet plainSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    @Test
    void everyAbilityBelongsToAtaqueADistancia() {
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            assertEquals(SkillType.ATAQUE_A_DISTANCIA, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, AtaqueADistanciaCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficultyOrGrantsASkillRollBonusYet() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
            assertEquals(0, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }

    @Test
    void friezaResolvesADamageBonusWhenTheAttackTargetIsWithinDistanciaCurta() {
        CharacterSheet attackTarget = plainSheet();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(attackTarget), Map.of(attackTarget, Range.DISTANCIA_CURTA));

        Optional<DamageBonus> bonus = AtaqueADistanciaCompetencyAbility.FRIEZA.resolveDamageBonus(sceneContext, attackTarget);

        assertEquals(Skill.ADVANTAGE_BONUS, bonus.orElseThrow().getValue());
        assertEquals(DamageType.FISICO, bonus.orElseThrow().getType());
    }

    @Test
    void friezaResolvesNoDamageBonusWhenTheAttackTargetIsFar() {
        CharacterSheet attackTarget = plainSheet();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(attackTarget), Map.of(attackTarget, Range.DISTANCIA_LONGA));

        assertEquals(Optional.empty(), AtaqueADistanciaCompetencyAbility.FRIEZA.resolveDamageBonus(sceneContext, attackTarget));
    }

    @Test
    void friezaResolvesNoDamageBonusWithoutASceneContextOrAttackTarget() {
        assertEquals(Optional.empty(), AtaqueADistanciaCompetencyAbility.FRIEZA.resolveDamageBonus(null, plainSheet()));
        assertEquals(Optional.empty(), AtaqueADistanciaCompetencyAbility.FRIEZA
                .resolveDamageBonus(new SceneContext(List.of(), List.of(), Map.of()), null));
    }

    @Test
    void onlyFriezaEverResolvesADamageBonus() {
        CharacterSheet attackTarget = plainSheet();
        SceneContext sceneContext = new SceneContext(List.of(), List.of(attackTarget), Map.of(attackTarget, Range.DISTANCIA_CURTA));

        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            if (ability != AtaqueADistanciaCompetencyAbility.FRIEZA) {
                assertEquals(Optional.empty(), ability.resolveDamageBonus(sceneContext, attackTarget));
            }
        }
    }

    @Test
    void onlyDisparoArcanoSubstitutesTheBaseAttribute() {
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            if (ability == AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO) {
                assertEquals(Optional.of(AttributeDomain.FOCUS), ability.getSubstituteAttributeDomain());
            } else {
                assertEquals(Optional.empty(), ability.getSubstituteAttributeDomain());
            }
        }
    }
}
