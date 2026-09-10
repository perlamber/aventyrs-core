package org.aventyrs.core.skill.ataqueadistancia;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtaqueADistanciaCompetencyAbilityTest {

    private static final Weapon ADAGA_DE_ARREMESSO = AbstractWeapon.builder()
            .name("Adaga de Arremesso")
            .category(ItemCategory.THROWABLE)
            .damageBase(DamageBase.of(1, 2))
            .skillType(SkillType.ATAQUE_A_DISTANCIA)
            .build();

    private static final Weapon ARCO_LONGO = AbstractWeapon.builder()
            .name("Arco Longo")
            .category(ItemCategory.BOW)
            .damageBase(DamageBase.of(2, 0))
            .skillType(SkillType.ATAQUE_A_DISTANCIA)
            .build();

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
    void onlyDisparoArcanoSubstitutesTheBaseAttributeUnconditionally() {
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            if (ability == AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO) {
                assertEquals(Optional.of(AttributeDomain.FOCUS), ability.getSubstituteAttributeDomain());
            } else {
                assertEquals(Optional.empty(), ability.getSubstituteAttributeDomain());
            }
        }
    }

    /**
     * ARREMESSO_PODEROSO must leave the unconditional hook empty — overriding it would hand
     * Força to the bow shot its own clause excludes.
     */
    @Test
    void arremessoPoderosoSubstitutesNothingUnconditionally() {
        assertEquals(Optional.empty(), ARREMESSO_PODEROSO.getSubstituteAttributeDomain());
    }

    @Test
    void arremessoPoderosoSubstitutesForcaForAThrownWeapon() {
        assertEquals(Optional.of(AttributeDomain.STRENGTH),
                ARREMESSO_PODEROSO.resolveSubstituteAttributeDomain(ADAGA_DE_ARREMESSO));
    }

    @Test
    void arremessoPoderosoSubstitutesForcaForAMagia() {
        assertEquals(Optional.of(AttributeDomain.STRENGTH),
                ARREMESSO_PODEROSO.resolveSubstituteAttributeDomain(new TestSpell()));
    }

    /** A bow is fired, not thrown — the half of the Perícia the clause deliberately excludes. */
    @Test
    void arremessoPoderosoSubstitutesNothingForAFiredWeapon() {
        assertEquals(Optional.empty(),
                ARREMESSO_PODEROSO.resolveSubstituteAttributeDomain(ARCO_LONGO));
    }

    /**
     * An Ataque Desarmado and a caller who simply didn't say are both a {@code null} source —
     * neither is a thrown weapon nor a Magia, so both correctly leave the roll on Destreza. The
     * two are not distinguishable, and nothing needs them to be; see CLAUDE.md's gap catalog.
     */
    @Test
    void arremessoPoderosoSubstitutesNothingWithoutAnAttackSource() {
        assertEquals(Optional.empty(), ARREMESSO_PODEROSO.resolveSubstituteAttributeDomain(null));
    }

    /**
     * The delivery-scoped hook defaults to the unconditional one, so DISPARO_ARCANO keeps
     * substituting Foco whatever the attack is made with, and the constants that substitute
     * nothing still substitute nothing under a qualifying source.
     */
    @Test
    void aThrownSourceChangesNoOtherAbilitysSubstitution() {
        AttackSource thrown = ADAGA_DE_ARREMESSO;
        for (AtaqueADistanciaCompetencyAbility ability : AtaqueADistanciaCompetencyAbility.values()) {
            if (ability == ARREMESSO_PODEROSO) {
                continue;
            }
            assertEquals(ability.getSubstituteAttributeDomain(), ability.resolveSubstituteAttributeDomain(thrown));
        }
        assertEquals(Optional.of(AttributeDomain.FOCUS),
                AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO.resolveSubstituteAttributeDomain(thrown));
    }
}
