package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.DefeatBlessingService;
import org.aventyrs.core.character.services.DefeatBlessingServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.effect.Sangramento;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.profissao.ProfissaoSpecialization;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AssassinoFeat#GOLPE_DE_FINALIZACAO}'s "7 ou mais graduações" half, judged as a player
 * meets it: a character who walks three Talentos de Assassino, pays the XP for the fourth, has
 * Graduação 7 in the Perícia de Ataque used, and lands a critical against a target already at or
 * below half PV that they would not have landed before. The objective is the {@link
 * CriticalResult} the Interaction reports, never the hook's own return value — see the
 * {@code testing-a-feat} skill.
 *
 * <p>The Vantagem-em-Danos half of the same constant is covered end-to-end in {@code
 * DamageBonusSummingTest}.
 */
class AssassinoFeatTest {

    private final FeatService featService = new FeatServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    private static CharacterSkill meleeAt(final int graduationValue) {
        return CharacterSkill.builder()
                .skill(new AtaqueCorpoACorpo())
                .graduation(SkillGraduation.builder().graduationValue(graduationValue).build())
                .build();
    }

    private static CharacterSkill furtividadeAt(final int graduationValue) {
        return CharacterSkill.builder()
                .skill(new org.aventyrs.core.skill.furtividade.Furtividade())
                .graduation(SkillGraduation.builder().graduationValue(graduationValue).build())
                .build();
    }

    /**
     * Dexterity 2 clears {@code SAQUE_RAPIDO}; the melee Graduação is set directly (builders
     * bypass the Graduação cap, as every other Feat test relies on).
     */
    private static Character.CharacterBuilder assassin(final int meleeGraduation) {
        return character()
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(2).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeAt(meleeGraduation));
    }

    /**
     * Walks {@code SAQUE_RAPIDO} → {@code TROCA_DE_ARMA_VELOZ} → {@code ACERTO_CRITICO_APRIMORADO}
     * through the service, so {@code GOLPE_DE_FINALIZACAO}'s "3 outros Talentos de Assassino" is
     * really met, then returns the funded sheet ready for the fourth grant.
     */
    private CharacterSheet threeAssassinTalentosDeep(final Character character) throws IllegalOperationException {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        featService.grantFeat(character, sheet, AssassinoFeat.SAQUE_RAPIDO);
        featService.grantFeat(character, sheet, AssassinoFeat.TROCA_DE_ARMA_VELOZ);
        featService.grantFeat(character, sheet, AcertoCriticoAprimoradoFeat.of(AttackMethod.LIGHT_BLADE));
        return sheet;
    }

    private static SceneContext opposedBy(final CombatantSheet opposed) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false, opposed);
    }

    private static CharacterSheet healthyTarget() {
        return CharacterSheet.of(character().build(), new Player());
    }

    private static CharacterSheet targetBelowHalfHitPoints() {
        CharacterSheet target = healthyTarget();
        int max = new HitPointsServiceImpl().getMaxHitPoints(target.getCharacter());
        target.applyDamage(max / 2 + 1);
        return target;
    }

    /** Two 5s: a critical once the Margem Crítica Menor is widened by 1, a plain result otherwise. */
    private static SkillRoll twoFives() {
        return new SkillRoll(List.of(5, 5, 1));
    }

    private static CriticalResult meleeCriticalAgainst(final CharacterSheet attacker, final CombatantSheet target) {
        return SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(attacker, opposedBy(target), twoFives())
                .getCriticalResult();
    }

    // ---------- catalog shape ----------

    @Test
    void everyConstantBelongsToTheAssassinoTree() {
        for (AssassinoFeat feat : AssassinoFeat.values()) {
            assertEquals(FeatCategory.ASSASSINO, feat.getFeatCategory(), feat.name());
        }
    }

    @Test
    void everyConstantHasADescriptionAndRequirements() {
        for (AssassinoFeat feat : AssassinoFeat.values()) {
            assertFalse(feat.getDescription().isBlank(), feat.name());
            assertNotNull(feat.getFeatRequirements(), feat.name());
        }
    }

    @Test
    void theTreeHasEveryTalentoTheCatalogAuthors() {
        assertEquals(16, AssassinoFeat.values().length);
    }

    // ---------- what changes once they hold GOLPE_DE_FINALIZACAO ----------

    @Test
    void golpeDeFinalizacaoWidensTheCriticalMarginOnAFinishingBlowOnceHeld() throws IllegalOperationException {
        Character assassin = assassin(7).build();
        CharacterSheet sheet = threeAssassinTalentosDeep(assassin);
        CharacterSheet wounded = targetBelowHalfHitPoints();

        assertEquals(CriticalResult.NONE, meleeCriticalAgainst(sheet, wounded), "not yet held");

        featService.grantFeat(assassin, sheet, AssassinoFeat.GOLPE_DE_FINALIZACAO);

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, meleeCriticalAgainst(sheet, wounded));
    }

    @Test
    void acquiringGolpeDeFinalizacaoSpendsExactlyItsRaceCost() throws IllegalOperationException {
        Character assassin = assassin(7).build();
        CharacterSheet sheet = threeAssassinTalentosDeep(assassin);
        BigDecimal before = sheet.getUnUsedExperience();
        int cost = assassin.getRace().getNewFeatCost(FeatCategory.ASSASSINO);

        featService.grantFeat(assassin, sheet, AssassinoFeat.GOLPE_DE_FINALIZACAO);

        assertEquals(before.subtract(BigDecimal.valueOf(cost)), sheet.getUnUsedExperience());
    }

    // ---------- what must not change ----------

    /** A Golpe de Finalização is a wounded target — a healthy one gets no widened margin. */
    @Test
    void golpeDeFinalizacaoDoesNothingAgainstAHealthyTarget() throws IllegalOperationException {
        Character assassin = assassin(7).build();
        CharacterSheet sheet = threeAssassinTalentosDeep(assassin);
        featService.grantFeat(assassin, sheet, AssassinoFeat.GOLPE_DE_FINALIZACAO);

        assertEquals(CriticalResult.NONE, meleeCriticalAgainst(sheet, healthyTarget()));
    }

    /** Below Graduação 7 in the Perícia de Ataque used, only the Vantagem half applies. */
    @Test
    void golpeDeFinalizacaoLeavesTheCriticalMarginAloneBelowSevenGraduacoes() throws IllegalOperationException {
        Character assassin = assassin(6).build();
        CharacterSheet sheet = threeAssassinTalentosDeep(assassin);
        featService.grantFeat(assassin, sheet, AssassinoFeat.GOLPE_DE_FINALIZACAO);

        assertEquals(CriticalResult.NONE, meleeCriticalAgainst(sheet, targetBelowHalfHitPoints()));
    }

    /** With no opposed combatant (a bonuses-only preview) there is no target to measure. */
    @Test
    void golpeDeFinalizacaoWidensNothingWithNoOpposedCombatant() {
        Character assassin = assassin(7).build();
        assassin.grantFeat(AssassinoFeat.GOLPE_DE_FINALIZACAO);

        int widening = AssassinoFeat.GOLPE_DE_FINALIZACAO.resolveCriticalMarginIncrease(
                SkillType.ATAQUE_CORPO_A_CORPO, null, assassin);

        assertEquals(0, widening);
    }

    /** Every other constant in the tree widens no Margem Crítica at all. */
    @Test
    void noOtherAssassinoConstantWidensTheCriticalMargin() {
        Character assassin = assassin(7).build();
        SceneContext vsWounded = opposedBy(targetBelowHalfHitPoints());

        for (AssassinoFeat feat : AssassinoFeat.values()) {
            if (feat == AssassinoFeat.GOLPE_DE_FINALIZACAO) {
                continue;
            }
            assertEquals(0,
                    feat.resolveCriticalMarginIncrease(SkillType.ATAQUE_CORPO_A_CORPO, vsWounded, assassin),
                    feat.name());
        }
    }
    // ---------- SAQUE_RAPIDO: the price of the quick draw ----------

    private static AbstractWeapon dagger() {
        return AbstractWeapon.builder()
                .name("Adaga").category(ItemCategory.LIGHT_BLADE)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(1, 2)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    private int meleeRollBonus(final CharacterSheet sheet) {
        return SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, null, new SkillRoll(List.of(3, 3, 3))).getSkillRollBonus();
    }

    private CharacterSheet quickDrawAssassin(final Weapon weapon) throws IllegalOperationException {
        Character character = assassin(1).equipment(new ArrayList<>(List.of(weapon)))
                .drawnWeapons(new ArrayList<>()).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        featService.grantFeat(character, sheet, AssassinoFeat.SAQUE_RAPIDO);
        sheet.startTurn(0);
        return sheet;
    }

    /** Drawing this Turn costs the Turn's first Ataque roll a Desvantagem. */
    @Test
    void drawingThisTurnCostsTheFirstAtaqueRollADesvantagem() throws IllegalOperationException {
        AbstractWeapon dagger = dagger();
        CharacterSheet sheet = quickDrawAssassin(dagger);
        int undrawn = meleeRollBonus(sheet);

        sheet.drawWeapon(dagger);

        assertEquals(undrawn + Skill.DISADVANTAGE_MALUS, meleeRollBonus(sheet));
    }

    /** A character who began the Turn already armed pays nothing — the malus is the draw's price. */
    @Test
    void beingAlreadyArmedCostsNothing() throws IllegalOperationException {
        AbstractWeapon dagger = dagger();
        CharacterSheet sheet = quickDrawAssassin(dagger);
        sheet.drawWeapon(dagger);
        int drawnThisTurn = meleeRollBonus(sheet);

        // A fresh Turn: still holding the blade, but no draw happened in it.
        sheet.startTurn(1);

        assertEquals(drawnThisTurn - Skill.DISADVANTAGE_MALUS, meleeRollBonus(sheet));
    }

    /** Only the *first* Ataque roll of the Turn pays it. */
    @Test
    void onlyTheFirstAtaqueRollOfTheTurnPaysTheDesvantagem() throws IllegalOperationException {
        AbstractWeapon dagger = dagger();
        CharacterSheet sheet = quickDrawAssassin(dagger);
        sheet.drawWeapon(dagger);
        int first = meleeRollBonus(sheet);

        sheet.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO,
                AttributeDomain.STRENGTH, null, ActionCost.ofActionPoints(1), 0, null));

        assertEquals(first - Skill.DISADVANTAGE_MALUS, meleeRollBonus(sheet));
    }

    /** Scoped to Perícias de Ataque — an Atletismo roll is untouched. */
    @Test
    void theDesvantagemDoesNotReachANonAttackPericia() throws IllegalOperationException {
        AbstractWeapon dagger = dagger();
        CharacterSheet sheet = quickDrawAssassin(dagger);
        int before = SkillType.ATLETISMO.newInteraction()
                .applyTo(sheet, null, new SkillRoll(List.of(3, 3, 3))).getSkillRollBonus();

        sheet.drawWeapon(dagger);

        assertEquals(before, SkillType.ATLETISMO.newInteraction()
                .applyTo(sheet, null, new SkillRoll(List.of(3, 3, 3))).getSkillRollBonus());
    }

    // ---------- ESCUDO_DE_SOMBRAS: +3 Defesas and RDS while Escondido ----------

    private CharacterSheet shadowAssassin() throws IllegalOperationException {
        Character character = assassin(1)
                .skill(SkillType.FURTIVIDADE, furtividadeAt(2))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        featService.grantFeat(character, sheet, MobilidadeFeat.MOVIMENTO_FURTIVO);  // "Corrida Furtiva" prereq
        featService.grantFeat(character, sheet, AssassinoFeat.ESCUDO_DE_SOMBRAS);
        return sheet;
    }

    @Test
    void escudoDeSombrasGrantsThreeToBothDefesasWhileEscondido() throws IllegalOperationException {
        DefenseService defenseService = new DefenseServiceImpl();
        CharacterSheet sheet = shadowAssassin();
        int physicalBefore = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null);
        int magicBefore = defenseService.getTotalDefense(sheet, DefenseType.MAGIC, null);

        sheet.applyCondition(new Condition(ConditionType.ESCONDIDO, null));

        assertEquals(physicalBefore + 3, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null));
        assertEquals(magicBefore + 3, defenseService.getTotalDefense(sheet, DefenseType.MAGIC, null));
    }

    @Test
    void escudoDeSombrasGrantsThreeRdsWhileEscondido() throws IllegalOperationException {
        DamageService damageService = new DamageServiceImpl();
        CharacterSheet sheet = shadowAssassin();
        int before = damageService.getTotalDamageReduction(sheet, DamageType.FISICO, null);

        sheet.applyCondition(new Condition(ConditionType.ESCONDIDO, null));

        assertEquals(before + 3, damageService.getTotalDamageReduction(sheet, DamageType.FISICO, null));
    }

    /** Out in the open it grants nothing — and neither does a Character-only caller with no sheet. */
    @Test
    void escudoDeSombrasGrantsNothingWhileVisible() throws IllegalOperationException {
        DefenseService defenseService = new DefenseServiceImpl();
        CharacterSheet sheet = shadowAssassin();
        int hidden;
        sheet.applyCondition(new Condition(ConditionType.ESCONDIDO, null));
        hidden = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null);

        sheet.removeCondition(ConditionType.ESCONDIDO);

        assertEquals(hidden - 3, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null));
        assertEquals(hidden - 3, defenseService.getTotalDefense(sheet.getCharacter(), DefenseType.PHYSICAL));
    }

    // ---------- defeat-trigger Blessings ----------

    private final DefeatBlessingService defeatBlessingService = new DefeatBlessingServiceImpl();

    private static CharacterSheet holding(final AssassinoFeat... feats) {
        Character character = character().build();
        for (AssassinoFeat feat : feats) {
            character.grantFeat(feat);
        }
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void sangueQuenteGrantsAnActionPointsBlessingOnAnyDefeat() {
        CharacterSheet attacker = holding(AssassinoFeat.SANGUE_QUENTE);
        int baseline = new org.aventyrs.core.action.ActionPointsServiceImpl()
                .getMaxActionPoints(attacker, 0);

        List<Blessing> granted = defeatBlessingService.applyDefeatBlessings(attacker, holding(), false);

        assertEquals(1, granted.size());
        assertEquals(ModifierType.ACTION_POINTS, granted.get(0).getModifierType());
        assertEquals(1, granted.get(0).getValue());   // 1 + 0 titles
        assertEquals(baseline + 1, new org.aventyrs.core.action.ActionPointsServiceImpl()
                .getMaxActionPoints(attacker, 0));
    }

    @Test
    void violenciaDescomunalGrantsMovementOnlyOnACriticalDefeat() {
        CharacterSheet attacker = holding(AssassinoFeat.VIOLENCIA_DESCOMUNAL);

        assertTrue(defeatBlessingService.applyDefeatBlessings(attacker, holding(), false).isEmpty());
        List<Blessing> viaCrit = defeatBlessingService.applyDefeatBlessings(attacker, holding(), true);
        assertEquals(ModifierType.MOVEMENT, viaCrit.get(0).getModifierType());
        assertEquals(0, viaCrit.get(0).getValue());   // 0 Bruto titles on a blank build
    }

    @Test
    void arcanismoAvassaladorGrantsACumulativeConjuracaoBonusOnCriticalEliminations() {
        CharacterSheet attacker = holding(AssassinoFeat.ARCANISMO_AVASSALADOR);

        defeatBlessingService.applyDefeatBlessings(attacker, holding(), true);
        defeatBlessingService.applyDefeatBlessings(attacker, holding(), true);

        assertEquals(2, attacker.getTemporaryBonus(ModifierType.DOMINIO_DO_MANA_ROLL_BONUS));
        assertTrue(defeatBlessingService.applyDefeatBlessings(attacker, holding(), false).isEmpty());
    }

    @Test
    void noOtherAssassinoConstantGrantsADefeatBlessing() {
        Character attacker = character().build();
        CharacterSheet defeated = holding();
        for (AssassinoFeat feat : AssassinoFeat.values()) {
            if (feat == AssassinoFeat.SANGUE_QUENTE || feat == AssassinoFeat.VIOLENCIA_DESCOMUNAL
                    || feat == AssassinoFeat.ARCANISMO_AVASSALADOR) {
                continue;
            }
            assertTrue(feat.resolveDefeatBlessings(attacker, defeated, true).isEmpty(), feat.name());
        }
    }

    // ---------- ABRIR_FERIDAS: Sangramento on a critical hit ----------

    @Test
    void abrirFeridasAddsSangramentoToTheChainOfACriticalHit() throws IllegalOperationException {
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(5).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeAt(3))
                .build();
        character.grantFeat(AssassinoFeat.ABRIR_FERIDAS);
        CharacterSheet attacker = CharacterSheet.of(character, new Player());

        DeliveredAttackResult result = new AttackDelivery().resolve(DeliveredAttack.from(foe, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(new SkillRoll(List.of(6, 6, 6)))   // triple-6: Acerto Crítico Maior
                .build());

        assertTrue(result.getCriticalEffectTriggered());
        assertTrue(chainContainsSangramento(result));
    }

    @Test
    void withoutAbrirFeridasACriticalHitHasNoSangramento() {
        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(5).build())
                        .build())
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeAt(3))
                .build();
        CharacterSheet attacker = CharacterSheet.of(character, new Player());

        DeliveredAttackResult result = new AttackDelivery().resolve(DeliveredAttack.from(foe, DefenseType.PHYSICAL)
                .attacker(attacker)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackRoll(new SkillRoll(List.of(6, 6, 6)))
                .build());

        assertFalse(chainContainsSangramento(result));
    }

    private static boolean chainContainsSangramento(final DeliveredAttackResult result) {
        org.aventyrs.core.sheet.Interaction<CombatantSheet> next = result.getAttackResult().getNextInteraction();
        while (next != null) {
            if (next instanceof Sangramento) {
                return true;
            }
            next = next.getNextInteraction();
        }
        return false;
    }

    // ---------- prerequisite gaps ----------

    @Test
    void saqueRelampagoAcceptsAFocoFiveBuildWithNeitherDestrezaNorSaqueRapido() {
        Character focoBuild = character()
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(5).build())
                        .build())
                .build();

        assertTrue(AssassinoFeat.SAQUE_RELAMPAGO.isEligible(focoBuild));
    }

    @Test
    void saqueRelampagoStillRejectsABuildWithNeitherBranch() {
        assertFalse(AssassinoFeat.SAQUE_RELAMPAGO.isEligible(character().build()));
    }

    @Test
    void acertoCriticoArcanoNeedsTheMagiasChoiceOfAcertoCriticoAprimorado() throws IllegalOperationException {
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        featService.grantFeat(character, sheet, AssassinoFeat.SAQUE_RAPIDO);
        featService.grantFeat(character, sheet, SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS));

        character.grantFeat(AcertoCriticoAprimoradoFeat.of(AttackMethod.LIGHT_BLADE));
        assertFalse(AssassinoFeat.ACERTO_CRITICO_ARCANO.isEligible(character), "chose a weapon, not Magias");

        character.getFeats().removeIf(AcertoCriticoAprimoradoFeat.class::isInstance);
        character.grantFeat(AcertoCriticoAprimoradoFeat.of(AttackMethod.OFFENSIVE_MAGIC));
        assertTrue(AssassinoFeat.ACERTO_CRITICO_ARCANO.isEligible(character));
    }

    // ---------- SAQUE_RELAMPAGO's Vantagem rider (WEAPONS branch) ----------

    @Test
    void saqueRelampagoRiderGrantsVantagemOnTheFirstAttackAfterDrawingTheFirstWeaponOfTheCena()
            throws IllegalOperationException {
        AbstractWeapon dagger = dagger();
        Character character = assassin(3).equipment(new ArrayList<>(List.of(dagger)))
                .drawnWeapons(new ArrayList<>()).build();
        character.grantFeat(SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS));
        CharacterSheet sheet = CharacterSheet.of(character, new Player());

        new Scene().addParticipant(sheet, 10);   // startNewScene
        sheet.startTurn(0);
        int undrawn = riderRollBonus(sheet, dagger);

        sheet.drawWeapon(dagger);

        assertEquals(undrawn + Skill.ADVANTAGE_BONUS, riderRollBonus(sheet, dagger));
    }

    @Test
    void saqueRelampagoRiderIsSpentAfterTheFirstQualifyingAttackOfTheCena() throws IllegalOperationException {
        AbstractWeapon dagger = dagger();
        Character character = assassin(3).equipment(new ArrayList<>(List.of(dagger)))
                .drawnWeapons(new ArrayList<>()).build();
        character.grantFeat(SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS));
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        new Scene().addParticipant(sheet, 10);
        sheet.startTurn(0);
        sheet.drawWeapon(dagger);
        int withRider = riderRollBonus(sheet, dagger);

        sheet.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH,
                dagger, ActionCost.ofActionPoints(1), 0, null));

        assertEquals(withRider - Skill.ADVANTAGE_BONUS, riderRollBonus(sheet, dagger));
    }

    private int riderRollBonus(final CharacterSheet sheet, final Weapon weapon) {
        return SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, null, new SkillRoll(List.of(3, 3, 3)), null, weapon).getSkillRollBonus();
    }

    // ---------- ACERTO_CRITICO_RELAMPAGO's Margem Crítica ----------

    @Test
    void acertoCriticoRelampagoWidensTheMarginByTwoOnTheOpeningAttackOfTheCena() throws IllegalOperationException {
        AbstractWeapon dagger = dagger();
        Character character = assassin(3).equipment(new ArrayList<>(List.of(dagger)))
                .drawnWeapons(new ArrayList<>()).build();
        character.grantFeat(AcertoCriticoAprimoradoFeat.of(AttackMethod.LIGHT_BLADE));
        character.grantFeat(AssassinoFeat.ACERTO_CRITICO_RELAMPAGO);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        new Scene().addParticipant(sheet, 10);
        sheet.startTurn(0);
        sheet.drawWeapon(dagger);

        // 4+4+1: two dice at >=4 — a critical only once the Menor margin is widened by 2.
        CriticalResult opener = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction()
                .applyTo(sheet, opposedBy(healthyTarget()), new SkillRoll(List.of(4, 4, 1)), null, dagger)
                .getCriticalResult();

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, opener);
    }

    @Test
    void acertoCriticoRelampagoRequiresAWeaponChoiceOfAcertoCriticoAprimorado() throws IllegalOperationException {
        Character character = character()
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(100));
        featService.grantFeat(character, sheet, AssassinoFeat.SAQUE_RAPIDO);
        featService.grantFeat(character, sheet, SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS));
        character.grantFeat(AcertoCriticoAprimoradoFeat.of(AttackMethod.OFFENSIVE_MAGIC));

        assertFalse(AssassinoFeat.ACERTO_CRITICO_RELAMPAGO.isEligible(character), "chose Magias, not a weapon");
    }

    @Test
    void especialistaTecnologicoNeedsProfissaoFourAndTheMecanicaEspecializacao() {
        CharacterSkill profissao = CharacterSkill.builder()
                .skill(new org.aventyrs.core.skill.profissao.Profissao())
                .graduation(SkillGraduation.builder().graduationValue(4).build())
                .specializations(new ArrayList<>(List.of(ProfissaoSpecialization.MECANICA)))
                .build();
        Character qualified = character()
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeAt(4))
                .skill(SkillType.PROFISSAO, profissao)
                .primaryTitle(new org.aventyrs.core.title.santo.Santo(List.of(), List.of()))
                .build();
        assertTrue(AssassinoFeat.ESPECIALISTA_TECNOLOGICO.isEligible(qualified));

        CharacterSkill noSpec = CharacterSkill.builder()
                .skill(new org.aventyrs.core.skill.profissao.Profissao())
                .graduation(SkillGraduation.builder().graduationValue(4).build())
                .build();
        Character missingSpec = character()
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, meleeAt(4))
                .skill(SkillType.PROFISSAO, noSpec)
                .primaryTitle(new org.aventyrs.core.title.santo.Santo(List.of(), List.of()))
                .build();
        assertFalse(AssassinoFeat.ESPECIALISTA_TECNOLOGICO.isEligible(missingSpec));
    }
}
