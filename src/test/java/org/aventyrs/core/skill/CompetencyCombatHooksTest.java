package org.aventyrs.core.skill;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.CriticalServiceImpl;
import org.aventyrs.core.effect.AbrirDefesas;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaCompetencyAbility;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility;
import org.aventyrs.core.skill.furtividade.FurtividadeCompetencyAbility;
import org.aventyrs.core.skill.persuasao.PersuasaoCompetencyAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 1 of the Habilidades de Competência plan: the combat hooks (core 0.0.100). */
class CompetencyCombatHooksTest {

    private static final SceneContext COMBAT =
            new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet sheetHolding(final SkillCompetencyAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK);
        for (SkillCompetencyAbility ability : abilities) {
            builder.skillCompetencyAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private static Weapon weapon(final ItemWeightClass weightClass, final SkillType skill) {
        return AbstractWeapon.builder().name("arma").category(ItemCategory.HEAVY_BLADE).weightClass(weightClass)
                .damageBase(DamageBase.of(1, 1)).skillType(skill).build();
    }

    // Mirar na Cabeça

    @Test
    void mirarNaCabecaAddsAVantagemToARangedCrit() {
        CriticalServiceImpl criticals = new CriticalServiceImpl();
        CriticalDamage plain = criticals.getCriticalDamage(sheetHolding(), SkillType.ATAQUE_A_DISTANCIA, null,
                CriticalResult.ACERTO_CRITICO_MENOR, null, null);
        CriticalDamage aimed = criticals.getCriticalDamage(sheetHolding(AtaqueADistanciaCompetencyAbility.MIRAR_NA_CABECA),
                SkillType.ATAQUE_A_DISTANCIA, null, CriticalResult.ACERTO_CRITICO_MENOR, null, null);

        assertEquals(plain.flatBonus() + Skill.ADVANTAGE_BONUS, aimed.flatBonus());
    }

    @Test
    void mirarNaCabecaLeavesAMeleeCritAlone() {
        assertEquals(CriticalDamage.NONE, AtaqueADistanciaCompetencyAbility.MIRAR_NA_CABECA
                .resolveCriticalDamage(SkillType.ATAQUE_CORPO_A_CORPO, null));
    }

    // Abrir Defesas

    @Test
    void abrirDefesasChainsDesprevenidoOnAMeleeCrit() {
        CharacterSheet attacker = sheetHolding();
        assertInstanceOf(AbrirDefesas.class, AtaqueCorpoACorpoCompetencyAbility.ABRIR_DEFESAS
                .resolveCriticalHitEffects(SkillType.ATAQUE_CORPO_A_CORPO, attacker).get(0));
        assertTrue(AtaqueCorpoACorpoCompetencyAbility.ABRIR_DEFESAS
                .resolveCriticalHitEffects(SkillType.ATAQUE_A_DISTANCIA, attacker).isEmpty());
    }

    @Test
    void abrirDefesasLeavesTheTargetDesprevenidoForOneRodada() {
        CharacterSheet target = sheetHolding();

        new AbrirDefesas(sheetHolding()).applyTo(target);

        assertTrue(target.hasCondition(ConditionType.DESPREVENIDO, null));
        target.tickTemporaryEffects();
        assertFalse(target.hasCondition(ConditionType.DESPREVENIDO, null));
    }

    // Acuidade

    @Test
    void acuidadeCostsADesvantagemOnAHeavyWeaponsDano() {
        Optional<DamageBonus> heavy = AtaqueCorpoACorpoCompetencyAbility.ACUIDADE.resolveDamageBonus(
                SkillType.ATAQUE_CORPO_A_CORPO, null, null, null,
                weapon(ItemWeightClass.HEAVY, SkillType.ATAQUE_CORPO_A_CORPO), null);

        assertEquals(Skill.DISADVANTAGE_MALUS, heavy.orElseThrow().getValue());
    }

    @Test
    void acuidadeLeavesALighterWeaponAlone() {
        assertTrue(AtaqueCorpoACorpoCompetencyAbility.ACUIDADE.resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO,
                null, null, null, weapon(ItemWeightClass.MEDIUM, SkillType.ATAQUE_CORPO_A_CORPO), null).isEmpty());
        assertTrue(AtaqueCorpoACorpoCompetencyAbility.ACUIDADE.resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO,
                null, null, null, null, null).isEmpty());
    }

    // Morte Oculta

    @Test
    void morteOcultaAddsTwoDanoWhileEscondido() {
        CharacterSheet holder = sheetHolding(FurtividadeCompetencyAbility.MORTE_OCULTA);
        assertTrue(morteOculta(holder).isEmpty(), "not hidden yet");

        holder.applyCondition(new Condition(ConditionType.ESCONDIDO, null));

        assertEquals(2, morteOculta(holder).orElseThrow().getValue());
    }

    @Test
    void morteOcultaGrowsAtTheFifthAndTenthGraduacao() {
        assertEquals(3, morteOculta(hiddenAtFurtividade(5)).orElseThrow().getValue());
        assertEquals(4, morteOculta(hiddenAtFurtividade(10)).orElseThrow().getValue());
    }

    private static Optional<DamageBonus> morteOculta(final CharacterSheet holder) {
        return FurtividadeCompetencyAbility.MORTE_OCULTA.resolveDamageBonus(SkillType.ATAQUE_A_DISTANCIA, null,
                null, holder.getCharacter(), null, holder);
    }

    private static CharacterSheet hiddenAtFurtividade(final int graduation) {
        CharacterSkill furtividade = CharacterSkillFixture.blank(CharacterSkillFixture.FURTIVIDADE_1).build();
        furtividade.increaseGraduation(graduation - furtividade.getGraduation().getGraduationValue());
        CharacterSheet sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.FURTIVIDADE, furtividade)
                .skillCompetencyAbility(FurtividadeCompetencyAbility.MORTE_OCULTA)
                .build(), new Player());
        sheet.applyCondition(new Condition(ConditionType.ESCONDIDO, null));
        return sheet;
    }

    // Ação Surpresa

    @Test
    void acaoSurpresaGrantsVantagemOnEveryPericiaWhileHiddenInCombat() {
        CharacterSheet holder = sheetHolding(FurtividadeCompetencyAbility.ACAO_SURPRESA);
        holder.applyCondition(new Condition(ConditionType.ESCONDIDO, null));

        assertEquals(Optional.of(Skill.ADVANTAGE_BONUS), FurtividadeCompetencyAbility.ACAO_SURPRESA
                .resolveConditionalRollBonus(SkillType.PERSUASAO, COMBAT, null, holder));
        assertTrue(FurtividadeCompetencyAbility.ACAO_SURPRESA
                .resolveConditionalRollBonus(SkillType.PERSUASAO, new SceneContext(List.of(), List.of(), Map.of()),
                        null, holder).isEmpty(), "outside combat");
    }

    @Test
    void acaoSurpresaReachesARealRoll() {
        CharacterSheet holder = sheetHolding(FurtividadeCompetencyAbility.ACAO_SURPRESA);
        int before = SkillType.PERSUASAO.newInteraction().applyTo(holder, COMBAT, null).getSkillRollBonus();

        holder.applyCondition(new Condition(ConditionType.ESCONDIDO, null));
        InteractionResult hidden = SkillType.PERSUASAO.newInteraction().applyTo(holder, COMBAT, null);

        assertEquals(before + Skill.ADVANTAGE_BONUS, hidden.getSkillRollBonus());
    }

    // Fintar Aprimorado

    @Test
    void aFintaTakesOnePaOffTheNextAttackAndEndsWithIt() {
        CharacterSheet holder = sheetHolding(PersuasaoCompetencyAbility.FINTAR_APRIMORADO);
        ActionPointsServiceImpl actionPoints = new ActionPointsServiceImpl();
        int plain = actionPoints.getAttackCost(holder, SkillType.ATAQUE_CORPO_A_CORPO, null, Set.of(), 1)
                .spentActionPoints();

        PersuasaoCompetencyAbility.FINTAR_APRIMORADO.resolveSuccessBlessings(SkillType.PERSUASAO, null, null)
                .forEach(holder::grantBlessing);

        assertEquals(plain - 1, actionPoints.getAttackCost(holder, SkillType.ATAQUE_CORPO_A_CORPO, null, Set.of(), 1)
                .spentActionPoints());
        assertTrue(holder.hasEffectFrom(PersuasaoCompetencyAbility.FINTAR_APRIMORADO.name()));

        holder.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, null, null,
                ActionCost.ofActionPoints(plain - 1), 1, ActionOutcome.from(InteractionResult.builder().build())));

        assertFalse(holder.hasEffectFrom(PersuasaoCompetencyAbility.FINTAR_APRIMORADO.name()),
                "\"sua próxima rolagem\" — spent by the first attack");
        assertEquals(plain, actionPoints.getAttackCost(holder, SkillType.ATAQUE_CORPO_A_CORPO, null, Set.of(), 1)
                .spentActionPoints());
    }

    @Test
    void aFintaSurvivesAPericiaRollThatIsNotAnAttack() {
        CharacterSheet holder = sheetHolding(PersuasaoCompetencyAbility.FINTAR_APRIMORADO);
        PersuasaoCompetencyAbility.FINTAR_APRIMORADO.resolveSuccessBlessings(SkillType.PERSUASAO, null, null)
                .forEach(holder::grantBlessing);

        holder.recordAction(new CombatantAction(SkillType.ATTENTION, null, null,
                ActionCost.ofActionPoints(2), 1, ActionOutcome.from(InteractionResult.builder().build())));

        assertTrue(holder.hasEffectFrom(PersuasaoCompetencyAbility.FINTAR_APRIMORADO.name()));
    }
}
