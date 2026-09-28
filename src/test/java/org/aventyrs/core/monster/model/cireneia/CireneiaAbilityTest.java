package org.aventyrs.core.monster.model.cireneia;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.FreeActionsService;
import org.aventyrs.core.character.services.FreeActionsServiceImpl;
import org.aventyrs.core.character.services.InitiativeService;
import org.aventyrs.core.character.services.InitiativeServiceImpl;
import org.aventyrs.core.character.services.MovementService;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.character.services.ReactionsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterBlueprint;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.monster.SampleMonster;
import org.aventyrs.core.monster.model.AbilityContext;
import org.aventyrs.core.monster.model.MonstrousActiveAbility;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.monster.MonsterCategory.ABOMINACAO;
import static org.aventyrs.core.monster.MonsterCategory.APEX;
import static org.aventyrs.core.monster.MonsterCategory.DEVIANTE;
import static org.aventyrs.core.monster.MonsterCategory.PREDADOR;
import static org.aventyrs.core.monster.MonsterCategory.PRESA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CireneiaAbilityTest {

    private final ActiveAbilityService activeAbilities = new ActiveAbilityServiceImpl();
    private final ActionPointsService actionPoints = new ActionPointsServiceImpl();
    private final ReactionsService reactions = new ReactionsServiceImpl();
    private final FreeActionsService freeActions = new FreeActionsServiceImpl();
    private final InitiativeService initiative = new InitiativeServiceImpl();
    private final MovementService movement = new MovementServiceImpl();

    private static AbilityContext picked(final MonsterCategory category, final String bonus) {
        return AbilityContext.of(category, Map.of(CireneiaAbility.BONUS_CHOICE, List.of(bonus)));
    }

    private static MonstrousActiveAbility only(final CireneiaAbility ability, final MonsterCategory category) {
        List<ActiveAbility> actives = ability.resolveActiveAbilities(AbilityContext.of(category));
        assertEquals(1, actives.size());
        return (MonstrousActiveAbility) actives.get(0);
    }

    private static ActiveAbility held(final MonsterSheet sheet, final String name) {
        return sheet.getCharacter().getActiveAbilities().stream()
                .filter(ability -> ability instanceof MonstrousActiveAbility monstrous && monstrous.getName().equals(name))
                .findFirst().orElseThrow();
    }

    /** A bare Pantera — same Atributos and Perícias, no Habilidades — to measure each one's delta against. */
    private static MonsterSheet bare() {
        return SampleMonster.PANTERA_DE_CIRENEIA.get().toBuilder().clearAbilities().build().spawn(new Player());
    }

    private static MonsterSheet pantera() {
        return SampleMonster.PANTERA_DE_CIRENEIA.get().spawn(new Player());
    }

    // ---- Atributos Aprimorados ----------------------------------------------------------------

    @Test
    void atributosAprimoradosGrantsBonusRacialDeDestrezaThatGrowsAtPredador() {
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 2), CireneiaAbility.ATRIBUTOS_APRIMORADOS.resolveRacialAttributeBonuses(AbilityContext.of(DEVIANTE)));
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 4), CireneiaAbility.ATRIBUTOS_APRIMORADOS.resolveRacialAttributeBonuses(AbilityContext.of(PREDADOR)));
    }

    @Test
    void impulsoGrowsWithTheCategoria() {
        MonstrousActiveAbility presa = only(CireneiaAbility.ATRIBUTOS_APRIMORADOS, PRESA);
        assertEquals(ActionCost.FREE_ACTION, presa.getActionPointCost());
        assertEquals(2, presa.getDeterminationPointCost());
        assertEquals(3, presa.getDurationInRounds());
        assertTrue(presa.isOncePerScene());
        assertEquals(5, only(CireneiaAbility.ATRIBUTOS_APRIMORADOS, DEVIANTE).getDurationInRounds());
    }

    @Test
    void impulsoRaisesDestrezaAndWithItTheDerivedDefesasOncePerCena() {
        MonsterSheet pantera = pantera();
        int before = pantera.getDefense(DefenseType.PHYSICAL);
        ActiveAbility impulso = held(pantera, "Impulso");

        activeAbilities.activate(pantera.getCharacter(), pantera, impulso, 1);

        // Destreza 8 → 11: half of it goes from 4 to 5
        assertEquals(before + 1, pantera.getDefense(DefenseType.PHYSICAL));
        assertEquals(2, pantera.getDeterminationSpent());
        assertThrows(IllegalOperationException.class, () -> activeAbilities.activate(pantera.getCharacter(), pantera, impulso, 1));

        pantera.beginScene();
        activeAbilities.activate(pantera.getCharacter(), pantera, impulso, 1);
    }

    @Test
    void impulsoAtApexIsPlusSix() {
        MonsterBlueprint apex = SampleMonster.PANTERA_DE_CIRENEIA.get().toBuilder().powerDegree(46).build();
        MonsterSheet sheet = apex.spawn(new Player());
        int before = sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.DEXTERITY, sheet);
        activeAbilities.activate(sheet.getCharacter(), sheet, held(sheet, "Impulso"), 1);
        assertEquals(before + 6, sheet.getCharacter().getEffectiveAttributeTotal(AttributeDomain.DEXTERITY, sheet));
    }

    // ---- Movimento Aprimorado ------------------------------------------------------------------

    @Test
    void movimentoAprimoradoRaisesMovimentoAndIniciativaByCategoria() {
        CireneiaAbility ability = CireneiaAbility.MOVIMENTO_APRIMORADO;
        assertEquals(3, ability.resolveModifier(ModifierType.MOVEMENT, AbilityContext.of(PRESA)));
        assertEquals(5, ability.resolveModifier(ModifierType.MOVEMENT, AbilityContext.of(APEX)));
        assertEquals(0, ability.resolveModifier(ModifierType.INITIATIVE, AbilityContext.of(PRESA)));
        assertEquals(2, ability.resolveModifier(ModifierType.INITIATIVE, AbilityContext.of(DEVIANTE)));
        assertEquals(4, ability.resolveModifier(ModifierType.INITIATIVE, AbilityContext.of(APEX)));
    }

    @Test
    void movimentoAprimoradoReachesTheMovementAndInitiativeServices() {
        MonsterSheet bare = bare();
        MonsterSheet pantera = pantera();
        assertEquals(movement.getMovementBase(bare.getCharacter()) + 3, movement.getMovementBase(pantera.getCharacter()));
        assertEquals(initiative.getTotalInitiative(bare.getCharacter()) + 2, initiative.getTotalInitiative(pantera.getCharacter()));
    }

    @Test
    void investidasGetVantagemAndAlwaysHitFromPredador() {
        CireneiaAbility ability = CireneiaAbility.MOVIMENTO_APRIMORADO;
        assertEquals(Skill.ADVANTAGE_BONUS, ability.resolveChargeAttackBonus(AbilityContext.of(PRESA)));
        assertEquals(Skill.ADVANTAGE_BONUS, ability.resolveChargeDamageBonus(AbilityContext.of(PRESA)));
        assertFalse(ability.chargesAlwaysHit(AbilityContext.of(DEVIANTE)));
        assertTrue(ability.chargesAlwaysHit(AbilityContext.of(PREDADOR)));
    }

    @Test
    void aceleracaoIsPlusFiveIniciativaForTwoRodadas() {
        MonstrousActiveAbility aceleracao = only(CireneiaAbility.MOVIMENTO_APRIMORADO, PRESA);
        assertEquals(ActionCost.ofActionPoints(1), aceleracao.getActionPointCost());
        assertEquals(2, aceleracao.getCooldownRounds());

        MonsterSheet pantera = pantera();
        activeAbilities.activate(pantera.getCharacter(), pantera, held(pantera, "Aceleração"), 1);
        assertEquals(5, pantera.getTemporaryBonus(ModifierType.INITIATIVE));
    }

    // ---- Celeridade ----------------------------------------------------------------------------

    @Test
    void celeridadeAddsPaAndOneMoreAtApex() {
        assertEquals(1, CireneiaAbility.CELERIDADE.resolveModifier(ModifierType.ACTION_POINTS, AbilityContext.of(DEVIANTE)));
        assertEquals(2, CireneiaAbility.CELERIDADE.resolveModifier(ModifierType.ACTION_POINTS, AbilityContext.of(APEX)));
        assertEquals(actionPoints.getMaxActionPoints(bare(), 1) + 1, actionPoints.getMaxActionPoints(pantera(), 1));
    }

    @Test
    void celeridadeMorGetsCheaperAndFasterWithTheCategoria() {
        MonstrousActiveAbility deviante = only(CireneiaAbility.CELERIDADE, DEVIANTE);
        assertEquals(2, deviante.getCooldownRounds());
        assertEquals(3, deviante.getDeterminationPointCost());
        assertEquals(1, only(CireneiaAbility.CELERIDADE, PREDADOR).getCooldownRounds());
        assertEquals(2, only(CireneiaAbility.CELERIDADE, APEX).getDeterminationPointCost());
    }

    @Test
    void celeridadeMorIsTwoMorePaThisTurn() {
        MonsterSheet pantera = pantera();
        int before = actionPoints.getMaxActionPoints(pantera, 1);
        ActiveAbility mor = held(pantera, "Celeridade Mór");
        activeAbilities.activate(pantera.getCharacter(), pantera, mor, 1);
        assertEquals(before + 2, actionPoints.getMaxActionPoints(pantera, 1));
        assertEquals(1, pantera.getRemainingCooldown(mor));
    }

    // ---- Relampejante --------------------------------------------------------------------------

    @Test
    void relampejanteGrantsThePickThenTheOtherThenThePickAgain() {
        CireneiaAbility ability = CireneiaAbility.RELAMPEJANTE;
        String reaction = CireneiaAbility.REACTION_CHOICE;
        assertEquals(1, ability.resolveModifier(ModifierType.REACTIONS, picked(DEVIANTE, reaction)));
        assertEquals(0, ability.resolveModifier(ModifierType.FREE_ACTIONS, picked(DEVIANTE, reaction)));
        assertEquals(1, ability.resolveModifier(ModifierType.FREE_ACTIONS, picked(PREDADOR, reaction)));
        assertEquals(2, ability.resolveModifier(ModifierType.REACTIONS, picked(APEX, reaction)));

        String free = CireneiaAbility.FREE_ACTION_CHOICE;
        assertEquals(1, ability.resolveModifier(ModifierType.FREE_ACTIONS, picked(DEVIANTE, free)));
        assertEquals(0, ability.resolveModifier(ModifierType.REACTIONS, picked(DEVIANTE, free)));
    }

    @Test
    void relampejanteReachesTheReactionAndFreeActionServices() {
        MonsterSheet bare = bare();
        MonsterSheet pantera = pantera();
        assertEquals(reactions.getTotalReactions(bare, 1) + 1, reactions.getTotalReactions(pantera, 1));
        assertEquals(freeActions.getTotalFreeActions(bare, 1) + 1, freeActions.getTotalFreeActions(pantera, 1));
    }

    // ---- Liberdade Selvagem --------------------------------------------------------------------

    @Test
    void liberdadeSelvagemStepsOnlyDestrezaPericias() {
        CireneiaAbility ability = CireneiaAbility.LIBERDADE_SELVAGEM;
        assertEquals(1, ability.resolveSkillLevelShift(SkillType.FURTIVIDADE, AttributeDomain.DEXTERITY, AbilityContext.of(PREDADOR)));
        assertEquals(0, ability.resolveSkillLevelShift(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH, AbilityContext.of(PREDADOR)));
    }

    @Test
    void sonidoIsFreeAtAbominacao() {
        MonstrousActiveAbility predador = only(CireneiaAbility.LIBERDADE_SELVAGEM, PREDADOR);
        assertEquals(ActionCost.ofActionPoints(1), predador.getActionPointCost());
        assertEquals(2, predador.getDeterminationPointCost());
        assertEquals(2, predador.getCooldownRounds());
        assertEquals(1, only(CireneiaAbility.LIBERDADE_SELVAGEM, APEX).getCooldownRounds());
        MonstrousActiveAbility abominacao = only(CireneiaAbility.LIBERDADE_SELVAGEM, ABOMINACAO);
        assertEquals(ActionCost.NONE, abominacao.getActionPointCost());
        assertEquals(0, abominacao.getDeterminationPointCost());
    }

    // ---- Apex ----------------------------------------------------------------------------------

    @Test
    void auraDoParadoxoGivesTheMonsterTwoPa() {
        MonsterBlueprint apex = SampleMonster.PANTERA_DE_CIRENEIA.get().toBuilder().powerDegree(50)
                .ability(MonstrousAbilitySelection.of(CireneiaAbility.AURA_DO_PARADOXO)).build();
        MonsterSheet sheet = apex.spawn(new Player());
        int before = actionPoints.getMaxActionPoints(sheet, 1);
        activeAbilities.activate(sheet.getCharacter(), sheet, held(sheet, "Aura do Paradoxo"), 1);
        assertEquals(before + 2, actionPoints.getMaxActionPoints(sheet, 1));
        assertEquals(3, sheet.getDeterminationSpent());
    }

    @Test
    void ofuscarBecomesPassiveAtAbominacaoSoItHasNoActive() {
        assertEquals(1, CireneiaAbility.OFUSCAR.resolveActiveAbilities(AbilityContext.of(APEX)).size());
        assertEquals(List.of(), CireneiaAbility.OFUSCAR.resolveActiveAbilities(AbilityContext.of(ABOMINACAO)));
    }

    @Test
    void everyActiveSpendsItsDeterminacaoThroughTheSharedService() {
        MonsterSheet pantera = pantera();
        int before = new DeterminationPointsServiceImpl().getCurrentDeterminationPoints(pantera.getCharacter(), pantera);
        activeAbilities.activate(pantera.getCharacter(), pantera, held(pantera, "Celeridade Mór"), 1);
        assertEquals(before - 3, new DeterminationPointsServiceImpl().getCurrentDeterminationPoints(pantera.getCharacter(), pantera));
    }
}
