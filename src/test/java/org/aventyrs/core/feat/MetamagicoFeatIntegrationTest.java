package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.character.services.SpellService;
import org.aventyrs.core.character.services.SpellServiceImpl;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.Conhecimentos;
import org.aventyrs.core.skill.dominiodomana.DominioDoMana;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * That each real {@link MetamagicoFeat} clause actually reaches the service that consumes it —
 * {@link MetamagicoFeatTest} pins the formulas themselves, these pin the wiring.
 *
 * <p>Every Talento here is acquired the way a player acquires one: through {@link
 * FeatService#grantFeat}, by a character who genuinely satisfies its Pré-requisito and pays its
 * XP. That is deliberate rather than incidental — {@code Character#grantFeat} would be shorter,
 * but it validates nothing, so a Talento whose requirements no character could ever meet would
 * still pass every assertion below. See the {@code testing-a-feat} skill.
 */
class MetamagicoFeatIntegrationTest {

    private final SpellService spellService = new SpellServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final MagicPointsService magicPointsService = new MagicPointsServiceImpl();
    private final RestService restService = new RestServiceImpl();
    private final FeatService featService = new FeatServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static CharacterSkill trained(final Skill skill, final int graduation) {
        return CharacterSkill.builder()
                .skill(skill)
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .build();
    }

    /**
     * A Conjurador trained deeply enough in Conhecimentos to satisfy every rung of the cap
     * ladder (DESAFIADOR_DA_REALIDADE asks for 9), and in Domínio do Mana for ARCANISTA's own DM
     * clause to have a Graduação to halve.
     */
    private static Character caster(final int dominioDoManaGraduation) {
        return character()
                .skill(SkillType.CONHECIMENTOS, trained(new Conhecimentos(), 9))
                .skill(SkillType.DOMINIO_DO_MANA, trained(new DominioDoMana(), dominioDoManaGraduation))
                .build();
    }

    /** An XP wallet deep enough for the whole ladder at {@code Race#BASE_NEW_FEAT_COST} apiece. */
    private static CharacterSheet fundedSheet(final Character character) {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        return sheet;
    }

    /** Acquires each Talento in order, through the service, so every prerequisite is really met. */
    private void acquire(final Character character, final MetamagicoFeat... feats) throws IllegalOperationException {
        CharacterSheet sheet = fundedSheet(character);
        for (MetamagicoFeat feat : feats) {
            featService.grantFeat(character, sheet, feat);
        }
    }

    // ---------- the cap ladder reaches SpellService ----------

    @Test
    void aCharacterWithNoTalentosIsCappedAtSemente() {
        assertEquals(BranchLevel.SEMENTE, spellService.getMaxBranchLevel(caster(0)));
    }

    @Test
    void arcanistaRaisesTheCapToBroto() throws IllegalOperationException {
        Character character = caster(0);
        acquire(character, MetamagicoFeat.ARCANISTA);

        assertEquals(BranchLevel.BROTO, spellService.getMaxBranchLevel(character));
    }

    @Test
    void arcanistaPlusExperienteRaisesTheCapToMuda() throws IllegalOperationException {
        Character character = caster(0);
        acquire(character, MetamagicoFeat.ARCANISTA, MetamagicoFeat.ARCANISTA_EXPERIENTE);

        assertEquals(BranchLevel.MUDA, spellService.getMaxBranchLevel(character));
    }

    @Test
    void threeRungsReachEmergente() throws IllegalOperationException {
        Character character = caster(0);
        acquire(character, MetamagicoFeat.ARCANISTA, MetamagicoFeat.ARCANISTA_EXPERIENTE,
                MetamagicoFeat.MESTRE_ARCANISTA);

        assertEquals(BranchLevel.EMERGENTE, spellService.getMaxBranchLevel(character));
    }

    /**
     * The whole ladder lands exactly on FLORESCENTE — no rung missing, none double-counted. That
     * every rung is acquirable in order, by one character, is half of what this asserts: the
     * chain is only walkable because each rung's Conhecimentos floor (1/3/6/9) is reachable and
     * each names the previous as its requiredFeat.
     */
    @Test
    void allFourRungsReachFlorescenteExactly() throws IllegalOperationException {
        Character character = caster(0);
        acquire(character, MetamagicoFeat.ARCANISTA, MetamagicoFeat.ARCANISTA_EXPERIENTE,
                MetamagicoFeat.MESTRE_ARCANISTA, MetamagicoFeat.DESAFIADOR_DA_REALIDADE);

        assertEquals(BranchLevel.FLORESCENTE, spellService.getMaxBranchLevel(character));
    }

    /** The ladder cannot be climbed out of order — the second rung refuses without the first. */
    @Test
    void aRungIsUnreachableUntilItsPredecessorIsHeld() {
        Character character = caster(0);
        CharacterSheet sheet = fundedSheet(character);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, MetamagicoFeat.ARCANISTA_EXPERIENTE));
        assertEquals(BranchLevel.SEMENTE, spellService.getMaxBranchLevel(character));
    }

    /** Untrained in Conhecimentos, the first rung is out of reach and the cap never moves. */
    @Test
    void anUntrainedCharacterCannotStartTheLadderAtAll() {
        Character character = character().build();
        CharacterSheet sheet = fundedSheet(character);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, MetamagicoFeat.ARCANISTA));
        assertEquals(BranchLevel.SEMENTE, spellService.getMaxBranchLevel(character));
    }

    /** Each rung costs its Race's ordinary price — the ladder is four full-price acquisitions. */
    @Test
    void climbingTheWholeLadderSpendsFourTalentosWorthOfExperience() throws IllegalOperationException {
        Character character = caster(0);
        CharacterSheet sheet = fundedSheet(character);
        int cost = character.getRace().getNewFeatCost(FeatCategory.METAMAGICO);

        featService.grantFeat(character, sheet, MetamagicoFeat.ARCANISTA);
        featService.grantFeat(character, sheet, MetamagicoFeat.ARCANISTA_EXPERIENTE);
        featService.grantFeat(character, sheet, MetamagicoFeat.MESTRE_ARCANISTA);
        featService.grantFeat(character, sheet, MetamagicoFeat.DESAFIADOR_DA_REALIDADE);

        assertEquals(BigDecimal.valueOf(100 - 4L * cost), sheet.getUnUsedExperience());
    }

    // ---------- ARCANISTA's DM reaches DefenseService ----------

    @Test
    void arcanistasMagicDefenseBonusReachesTheDefenseTotal() throws IllegalOperationException {
        Character character = caster(6);
        int before = defenseService.getTotalDefense(character, DefenseType.MAGIC);

        acquire(character, MetamagicoFeat.ARCANISTA);

        assertEquals(before + 3, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void arcanistaLeavesPhysicalDefenseAlone() throws IllegalOperationException {
        Character character = caster(6);
        int before = defenseService.getTotalDefense(character, DefenseType.PHYSICAL);

        acquire(character, MetamagicoFeat.ARCANISTA);

        assertEquals(before, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
    }

    /** The bonus scales off the holder's own Graduação, so it is tested at two points. */
    @Test
    void arcanistasMagicDefenseBonusScalesWithDominioDoManaGraduation() throws IllegalOperationException {
        Character shallow = caster(2);
        Character deep = caster(6);
        int shallowBefore = defenseService.getTotalDefense(shallow, DefenseType.MAGIC);
        int deepBefore = defenseService.getTotalDefense(deep, DefenseType.MAGIC);

        acquire(shallow, MetamagicoFeat.ARCANISTA);
        acquire(deep, MetamagicoFeat.ARCANISTA);

        assertEquals(shallowBefore + 1, defenseService.getTotalDefense(shallow, DefenseType.MAGIC));
        assertEquals(deepBefore + 3, defenseService.getTotalDefense(deep, DefenseType.MAGIC));
    }

    // ---------- MENTE_EXPANDIDA reaches MagicPointsService and RestService ----------

    @Test
    void menteExpandidaRaisesTheResolvedManaMultiplier() throws IllegalOperationException {
        Character character = caster(0);
        int before = magicPointsService.getManaMultiplier(character);

        acquire(character, MetamagicoFeat.MENTE_EXPANDIDA);

        assertEquals(before + 1, magicPointsService.getManaMultiplier(character));
    }

    @Test
    void menteExpandidaRaisesMaxMagicPointsThroughTheMultiplier() throws IllegalOperationException {
        Character character = caster(0);
        int before = magicPointsService.getMaxMagicPoints(character);

        acquire(character, MetamagicoFeat.MENTE_EXPANDIDA);

        int focus = character.getAttributes().getFocus().getTotal();
        assertEquals(before + focus, magicPointsService.getMaxMagicPoints(character));
    }

    @Test
    void menteExpandidaRaisesManaRecoveredOnARest() throws IllegalOperationException {
        Character character = caster(0);
        int before = restService.getRecoveredMagicPoints(character, RestType.LONGO);

        acquire(character, MetamagicoFeat.MENTE_EXPANDIDA);

        assertEquals(before + 2, restService.getRecoveredMagicPoints(character, RestType.LONGO));
    }

    @Test
    void aTalentoWithNoRealClauseChangesNothing() throws IllegalOperationException {
        Character character = caster(6);
        int defense = defenseService.getTotalDefense(character, DefenseType.MAGIC);
        int multiplier = magicPointsService.getManaMultiplier(character);
        int recovery = restService.getRecoveredMagicPoints(character, RestType.LONGO);
        BranchLevel cap = spellService.getMaxBranchLevel(character);

        acquire(character, MetamagicoFeat.CONJURACAO_RAPIDA);

        assertEquals(defense, defenseService.getTotalDefense(character, DefenseType.MAGIC));
        assertEquals(multiplier, magicPointsService.getManaMultiplier(character));
        assertEquals(recovery, restService.getRecoveredMagicPoints(character, RestType.LONGO));
        assertEquals(cap, spellService.getMaxBranchLevel(character));
    }
}
