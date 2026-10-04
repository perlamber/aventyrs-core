package org.aventyrs.core.skill;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ReactionsServiceImpl;
import org.aventyrs.core.item.ItemCatalog;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.UtilityItem;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.attention.AttentionCompetencyAbility;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemCompetencyAbility;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararCompetencyAbility;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraCompetencyAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 3 of the Habilidades de Competência plan: limited uses and Reações (core 0.0.102). */
class CompetencyUsesTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet holding(final SkillType skill, final int graduation,
                                          final SkillCompetencyAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK);
        if (graduation > 0) {
            builder.skill(skill, CharacterSkill.builder().skill(skill.newSkillInstance())
                    .graduation(SkillGraduation.builder().graduationValue(graduation).build()).build());
        }
        for (SkillCompetencyAbility ability : abilities) {
            builder.skillCompetencyAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private static CombatantAction rolled(final SkillType skill) {
        return new CombatantAction(skill, null, null, ActionCost.ofActionPoints(1), 1,
                ActionOutcome.from(InteractionResult.builder().build()));
    }

    // The ledger

    @Test
    void aRoundScopedUseResetsEachRodada() {
        CharacterSheet sheet = holding(SkillType.ESQUIVA_E_APARAR, 1, EsquivaEApararCompetencyAbility.RECUO_RAPIDO);
        assertEquals(1, CompetencyUses.remaining(sheet, EsquivaEApararCompetencyAbility.RECUO_RAPIDO));

        CompetencyUses.use(sheet, EsquivaEApararCompetencyAbility.RECUO_RAPIDO);

        assertEquals(0, CompetencyUses.remaining(sheet, EsquivaEApararCompetencyAbility.RECUO_RAPIDO));
        assertThrows(IllegalOperationException.class,
                () -> CompetencyUses.use(sheet, EsquivaEApararCompetencyAbility.RECUO_RAPIDO));
        sheet.startNewRound();
        assertEquals(1, CompetencyUses.remaining(sheet, EsquivaEApararCompetencyAbility.RECUO_RAPIDO));
    }

    @Test
    void saltoPoderosoHasOneUsePerCenaPlusOneAtTheFifthAndTenthGraduacao() {
        assertEquals(1, CompetencyUses.remaining(holding(SkillType.ATLETISMO, 1, AtletismoCompetencyAbility.SALTO_PODEROSO),
                AtletismoCompetencyAbility.SALTO_PODEROSO));
        assertEquals(2, CompetencyUses.remaining(holding(SkillType.ATLETISMO, 5, AtletismoCompetencyAbility.SALTO_PODEROSO),
                AtletismoCompetencyAbility.SALTO_PODEROSO));
        CharacterSheet ten = holding(SkillType.ATLETISMO, 10, AtletismoCompetencyAbility.SALTO_PODEROSO);
        assertEquals(3, CompetencyUses.remaining(ten, AtletismoCompetencyAbility.SALTO_PODEROSO));

        CompetencyUses.use(ten, AtletismoCompetencyAbility.SALTO_PODEROSO);
        ten.startNewRound();
        assertEquals(2, CompetencyUses.remaining(ten, AtletismoCompetencyAbility.SALTO_PODEROSO), "a Rodada renews nothing");
        ten.endCombat();
        assertEquals(3, CompetencyUses.remaining(ten, AtletismoCompetencyAbility.SALTO_PODEROSO));
    }

    @Test
    void socorroImediatoRenewsAtADescansoLongo() {
        CharacterSheet sheet = holding(SkillType.MEDICINA_E_CURA, 1, MedicinaECuraCompetencyAbility.SOCORRO_IMEDIATO);
        CompetencyUses.use(sheet, MedicinaECuraCompetencyAbility.SOCORRO_IMEDIATO);
        sheet.endCombat();
        assertEquals(0, CompetencyUses.remaining(sheet, MedicinaECuraCompetencyAbility.SOCORRO_IMEDIATO));

        sheet.clearRestCooldowns(RestType.LONGO);

        assertEquals(1, CompetencyUses.remaining(sheet, MedicinaECuraCompetencyAbility.SOCORRO_IMEDIATO));
    }

    @Test
    void anAbilityNotHeldHasNoUses() {
        assertEquals(0, CompetencyUses.remaining(holding(SkillType.ATLETISMO, 1),
                AtletismoCompetencyAbility.SALTO_PODEROSO));
    }

    @Test
    void saltoPoderososMovementPricesDifficultHexesAsOrdinaryGround() {
        org.aventyrs.core.scene.grid.GridPosition mud = new org.aventyrs.core.scene.grid.GridPosition(1, 1);
        org.aventyrs.core.scene.grid.StepRules rules = new org.aventyrs.core.scene.grid.StepRules() {
            @Override
            public boolean canPass(final org.aventyrs.core.scene.grid.GridPosition hex) {
                return true;
            }

            @Override
            public boolean canStop(final org.aventyrs.core.scene.grid.GridPosition hex) {
                return true;
            }

            @Override
            public boolean isDifficult(final org.aventyrs.core.scene.grid.GridPosition hex) {
                return hex.equals(mud);
            }

            @Override
            public int enterCost(final org.aventyrs.core.scene.grid.GridPosition hex) {
                return isDifficult(hex) ? 2 : 1;
            }
        };

        org.aventyrs.core.scene.grid.StepRules leaping = rules.ignoringDifficultTerrainCost();

        assertEquals(1, leaping.enterCost(mud));
        assertTrue(leaping.isDifficult(mud), "still known to be difficult");
    }

    // Instinto de Luther

    @Test
    void instintoDeLutherGrantsItsExtraReacoesForTheChosenRodada() {
        CharacterSheet sheet = holding(SkillType.ATTENTION, 5, AttentionCompetencyAbility.INSTINTO_DE_LUTHER);
        ReactionsServiceImpl reactions = new ReactionsServiceImpl();
        int before = reactions.getTotalReactions(sheet, 1);

        CompetencyUses.use(sheet, AttentionCompetencyAbility.INSTINTO_DE_LUTHER);

        assertEquals(before + 2, reactions.getTotalReactions(sheet, 1), "two at the 5ª Graduação");
        assertEquals(0, CompetencyUses.remaining(sheet, AttentionCompetencyAbility.INSTINTO_DE_LUTHER));
    }

    @Test
    void anAllyOfAFifthGraduacaoLutherTakesOneReacaoOncePerCena() {
        CharacterSheet luther = holding(SkillType.ATTENTION, 5, AttentionCompetencyAbility.INSTINTO_DE_LUTHER);
        CharacterSheet ally = holding(SkillType.ATTENTION, 0);
        SceneContext context = new SceneContext(List.of(luther), List.of(), Map.of());
        int before = new ReactionsServiceImpl().getTotalReactions(ally, 1);

        assertTrue(CompetencyUses.allyInstinctAvailable(ally, context));
        CompetencyUses.useAllyInstinct(ally);

        assertEquals(before + 1, new ReactionsServiceImpl().getTotalReactions(ally, 1));
        assertFalse(CompetencyUses.allyInstinctAvailable(ally, context));
    }

    @Test
    void aLutherBelowTheFifthGraduacaoGivesAlliesNothing() {
        CharacterSheet luther = holding(SkillType.ATTENTION, 4, AttentionCompetencyAbility.INSTINTO_DE_LUTHER);

        assertFalse(CompetencyUses.allyInstinctAvailable(holding(SkillType.ATTENTION, 0),
                new SceneContext(List.of(luther), List.of(), Map.of())));
    }

    // Bom Doutor

    @Test
    void theKitIsInTheStoresUtilidades() {
        assertTrue(ItemCatalog.all().contains(UtilityItem.KIT_DE_PRIMEIROS_SOCORROS));
        assertEquals(ItemCategory.UTILITIES, UtilityItem.KIT_DE_PRIMEIROS_SOCORROS.getCategory());
    }

    @Test
    void bomDoutorTakesOnePaOffTheFirstMedicinaRollOfTheRodadaWithAKit() {
        CharacterSheet sheet = holding(SkillType.MEDICINA_E_CURA, 1, MedicinaECuraCompetencyAbility.BOM_DOUTOR);
        ActionPointsServiceImpl actionPoints = new ActionPointsServiceImpl();
        int plain = cost(actionPoints, sheet);

        sheet.addToInventory(UtilityItem.KIT_DE_PRIMEIROS_SOCORROS.forge());
        assertEquals(plain - 1, cost(actionPoints, sheet));

        sheet.recordAction(rolled(SkillType.MEDICINA_E_CURA));
        assertEquals(plain, cost(actionPoints, sheet), "once per Rodada");
        sheet.startNewRound();
        assertEquals(plain - 1, cost(actionPoints, sheet));
    }

    private static int cost(final ActionPointsServiceImpl actionPoints, final CharacterSheet sheet) {
        return actionPoints.getSkillRollCost(sheet, SkillType.MEDICINA_E_CURA, Set.of(), 1, null).spentActionPoints();
    }

    // Charme Feérico

    @Test
    void charmeFeericoCostsTwoPdAndOnceForEachCreaturePerCena() {
        CharacterSheet holder = holding(SkillType.EMPATIA_SELVAGEM, 1, EmpatiaSelvagemCompetencyAbility.CHARME_FEERICO);
        CharacterSheet wolf = holding(SkillType.ATTENTION, 0);
        CharacterSheet bear = holding(SkillType.ATTENTION, 0);
        int spentBefore = holder.getDeterminationSpent();

        CompetencyUses.charm(holder, wolf);

        assertEquals(spentBefore + CompetencyUses.CHARME_FEERICO_COST, holder.getDeterminationSpent());
        assertFalse(CompetencyUses.canCharm(holder, wolf));
        assertTrue(CompetencyUses.canCharm(holder, bear));
        wolf.endCombat();
        assertTrue(CompetencyUses.canCharm(holder, wolf), "a new Cena");
    }
}
