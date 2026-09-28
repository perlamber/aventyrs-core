package org.aventyrs.core.feat;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.combat.IncomingAttackResult;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.Dilacerar;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillInteractionFactory;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistancia;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.ACTIVATED_FEAT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.DEFENSE_SUBSTITUTION_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.FEAT_ACTIVATION_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_SKILL_ROLL;
import static org.aventyrs.core.util.TranslatableMessages.REROLL_NOT_GRANTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Duelista Talentos a player opts into on one attack (core 0.0.70): the activation gate, the
 * lowest-die reroll, what an attack costs, the trades between an attack and its dano, the follow-up
 * attacks, and the three riders AttackDelivery/AttackReceiver report. Every figure is read off the
 * service or orchestrator a caller reads it from, as a delta against the same roll without the
 * Talento.
 */
class DuelistaActivationTest {

    /** 11 — a plain hit against a Defesa of 5, short of any critical. */
    private static final List<Integer> ORDINARY = List.of(4, 4, 3);

    /** 15 — a critical only once the Margem Crítica Menor is widened by 2. */
    private static final List<Integer> FIFTEEN = List.of(6, 5, 4);

    /** 17 — an Acerto Crítico Menor at the default margin. */
    private static final List<Integer> SEVENTEEN = List.of(6, 6, 5);

    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static AbstractWeapon weapon(final String name, final ItemCategory category, final ItemWeightClass weight,
                                         final SkillType skill) {
        return AbstractWeapon.builder()
                .name(name)
                .category(category)
                .weightClass(weight)
                .damageBase(DamageBase.of(1, 0))
                .skillType(skill)
                .build();
    }

    private static AbstractWeapon adaga() {
        return weapon("Adaga", ItemCategory.LIGHT_BLADE, ItemWeightClass.LIGHT, SkillType.ATAQUE_CORPO_A_CORPO);
    }

    private static AbstractWeapon espada() {
        return weapon("Espada Longa", ItemCategory.HEAVY_BLADE, ItemWeightClass.MEDIUM, SkillType.ATAQUE_CORPO_A_CORPO);
    }

    private static AbstractWeapon arco() {
        return weapon("Arco Curto", ItemCategory.BOW, ItemWeightClass.MEDIUM, SkillType.ATAQUE_A_DISTANCIA);
    }

    private static CharacterSkill trained(final org.aventyrs.core.skill.Skill skill, final int graduation) {
        return CharacterSkill.builder()
                .skill(skill)
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .build();
    }

    /** A duelist holding feats, with weapons drawn, Força/Vigor/Gnose 4 and 4 Graduações in both attacks. */
    private static CharacterSheet duelist(final List<Feat> feats, final List<Weapon> drawn) {
        return duelist(feats, drawn, 4);
    }

    private static CharacterSheet duelist(final List<Feat> feats, final List<Weapon> drawn, final int meleeGraduation) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.STRENGTH, 4, AttributeDomain.VIGOR, 4,
                        AttributeDomain.GNOSE, 4)))
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), meleeGraduation))
                .skill(SkillType.ATAQUE_A_DISTANCIA, trained(new AtaqueADistancia(), 4))
                .equipment(new ArrayList<>(drawn))
                .drawnWeapons(new ArrayList<>(drawn))
                .feats(new ArrayList<>(feats))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static CharacterSheet foe() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build(),
                new Player());
    }

    private static SkillRoll roll(final List<Integer> dice, final Feat... activated) {
        return new SkillRoll(dice, null, null, null, null, Set.of(activated));
    }

    private static InteractionResult attack(final CombatantSheet attacker, final SkillType skill, final SkillRoll roll,
                                            final Weapon weapon) {
        return SkillInteractionFactory.create(skill).applyTo(attacker, null, roll, null, weapon);
    }

    private static int damageBonus(final InteractionResult result) {
        return result.getDamageBonus() == null ? 0 : result.getDamageBonus().getValue();
    }

    private static CombatantAction attackAction(final Weapon weapon, final Boolean hit, final CombatantSheet target,
                                                final Feat... activated) {
        return new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH, weapon,
                ActionCost.ofActionPoints(2), 0, hit == null ? null : new ActionOutcome(hit, 0, CriticalResult.NONE, null),
                target == null ? null : target.getId(), Set.of(activated));
    }

    private DeliveredAttack.DeliveredAttackBuilder delivery(final CombatantSheet attacker, final CombatantSheet defender,
                                                            final Weapon weapon, final SkillRoll roll, final int defense) {
        return DeliveredAttack.builder()
                .attacker(attacker)
                .defender(defender)
                .attackSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .attackSource(weapon)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(defense)
                .attackRoll(roll);
    }

    private int baseAttackPrice(final CombatantSheet sheet) {
        return actionPointsService.getSkillRollCost(sheet.getCharacter(), 0);
    }

    private ActionCost price(final CombatantSheet sheet, final SkillType skill, final Weapon weapon,
                             final Feat... activated) {
        return actionPointsService.getAttackCost(sheet, skill, weapon, Set.of(activated), 0);
    }

    // ---------- the activation gate and the reroll ----------

    @Test
    void theRerollReplacesTheLowestDieAndRemembersIt() {
        SkillRoll rerolled = new SkillRoll(List.of(5, 1, 3)).rerollingLowestDie(2);

        assertEquals(10, rerolled.getTotal());
        assertEquals(1, rerolled.getRerolledFromFace());
        assertTrue(rerolled.isRerolled());
    }

    /** "O novo resultado será utilizado, mesmo que seja inferior ao anterior" — and only once. */
    @Test
    void aRerollStandsEvenLowerAndCannotBeRepeated() {
        SkillRoll rerolled = new SkillRoll(List.of(5, 4, 3)).rerollingLowestDie(1);

        assertEquals(10, rerolled.getTotal());
        assertEquals(INVALID_SKILL_ROLL, assertThrows(IllegalOperationException.class,
                () -> rerolled.rerollingLowestDie(6)).getMessage());
    }

    @Test
    void lutadorNatoMakesAMeleeRerollLegal() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.LUTADOR_NATO), List.of(adaga));
        SkillRoll rerolled = roll(List.of(1, 4, 4), DuelistaFeat.LUTADOR_NATO).rerollingLowestDie(6);

        InteractionResult plain = attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, roll(List.of(6, 4, 4)), adaga);
        InteractionResult withReroll = attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, rerolled, adaga);

        assertEquals(plain.getSkillRollBonus(), withReroll.getSkillRollBonus());
        assertEquals(plain.getCriticalResult(), withReroll.getCriticalResult());
    }

    @Test
    void aRerollNoActivatedTalentoPaysForIsRefused() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.LUTADOR_NATO), List.of(adaga));

        assertEquals(REROLL_NOT_GRANTED, assertThrows(IllegalOperationException.class,
                () -> attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO,
                        roll(List.of(1, 4, 4)).rerollingLowestDie(6), adaga)).getMessage());
    }

    @Test
    void activatingATalentoNotHeldIsRefused() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(), List.of(adaga));

        assertEquals(ACTIVATED_FEAT_NOT_HELD, assertThrows(IllegalOperationException.class,
                () -> attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY, DuelistaFeat.ATAQUE_RAPIDO), adaga))
                .getMessage());
    }

    /** "Um Ataque Corpo-a-Corpo" — not a shot. */
    @Test
    void lutadorNatoCannotBeSpentOnARangedAttack() {
        Weapon arco = arco();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.LUTADOR_NATO), List.of(arco));

        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> attack(duelist, SkillType.ATAQUE_A_DISTANCIA, roll(ORDINARY, DuelistaFeat.LUTADOR_NATO), arco))
                .getMessage());
    }

    @Test
    void miraImpecavelIsTheRangedTwin() {
        Weapon arco = arco();
        CharacterSheet archer = duelist(List.of(ArtilhariaFeat.MIRA_IMPECAVEL), List.of(arco));

        attack(archer, SkillType.ATAQUE_A_DISTANCIA,
                roll(List.of(1, 4, 4), ArtilhariaFeat.MIRA_IMPECAVEL).rerollingLowestDie(6), arco);
        assertEquals(ActionCost.ofActionPoints(baseAttackPrice(archer) + 1),
                price(archer, SkillType.ATAQUE_A_DISTANCIA, arco, ArtilhariaFeat.MIRA_IMPECAVEL));
    }

    // ---------- what an attack costs ----------

    @Test
    void aPlainAttackCostsWhatAPericiaRollCosts() {
        CharacterSheet duelist = duelist(List.of(), List.of(adaga()));

        assertEquals(ActionCost.ofActionPoints(baseAttackPrice(duelist)),
                price(duelist, SkillType.ATAQUE_CORPO_A_CORPO, adaga()));
    }

    @Test
    void lutadorNatoAddsOnePontoDeAcao() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.LUTADOR_NATO), List.of(adaga));

        assertEquals(ActionCost.ofActionPoints(baseAttackPrice(duelist)),
                price(duelist, SkillType.ATAQUE_CORPO_A_CORPO, adaga));
        assertEquals(ActionCost.ofActionPoints(baseAttackPrice(duelist) + 1),
                price(duelist, SkillType.ATAQUE_CORPO_A_CORPO, adaga, DuelistaFeat.LUTADOR_NATO));
    }

    /** "ao tempo de 1PA" replaces the price; Lutador Nato's +1PA still lands on top of it. */
    @Test
    void ataqueRepentinoCostsOnePontoDeAcao() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.ATAQUE_REPENTINO, DuelistaFeat.LUTADOR_NATO),
                List.of(adaga));

        assertEquals(ActionCost.ofActionPoints(1),
                price(duelist, SkillType.ATAQUE_CORPO_A_CORPO, adaga, DuelistaFeat.ATAQUE_REPENTINO));
        assertEquals(ActionCost.ofActionPoints(2), price(duelist, SkillType.ATAQUE_CORPO_A_CORPO, adaga,
                DuelistaFeat.ATAQUE_REPENTINO, DuelistaFeat.LUTADOR_NATO));
    }

    /** "o primeiro ataque que fizer a cada Rodada, com a arma escolhida … -1PA (mínimo 1PA)". */
    @Test
    void dominarArmasDiscountsTheFirstAttackWithTheChosenWeaponOnly() {
        Weapon adaga = adaga();
        Weapon espada = espada();
        CharacterSheet master = duelist(List.of(EspecialistaEmArmaFeat.of(AttackMethod.LIGHT_BLADE),
                DuelistaFeat.DOMINAR_ARMAS), List.of(adaga, espada));
        int base = baseAttackPrice(master);

        assertEquals(ActionCost.ofActionPoints(base), price(master, SkillType.ATAQUE_CORPO_A_CORPO, espada));
        master.recordAction(attackAction(espada, true, null));
        assertEquals(ActionCost.ofActionPoints(base - 1), price(master, SkillType.ATAQUE_CORPO_A_CORPO, adaga));
        master.recordAction(attackAction(adaga, true, null));
        assertEquals(ActionCost.ofActionPoints(base), price(master, SkillType.ATAQUE_CORPO_A_CORPO, adaga));
    }

    @Test
    void dominarArmasIsNotForWhoeverChoseOffensiveMagic() {
        CharacterSheet magus = duelist(List.of(EspecialistaEmArmaFeat.of(AttackMethod.OFFENSIVE_MAGIC)), List.of(), 6);
        CharacterSheet swordsman = duelist(List.of(EspecialistaEmArmaFeat.of(AttackMethod.LIGHT_BLADE)), List.of(), 6);

        assertFalse(DuelistaFeat.DOMINAR_ARMAS.isEligible(magus.getCharacter(), magus));
        assertTrue(DuelistaFeat.DOMINAR_ARMAS.isEligible(swordsman.getCharacter(), swordsman));
    }

    // ---------- trading the attack against its dano ----------

    @Test
    void ataqueConcentradoTradesTheAttackForTheDano() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.ATAQUE_CONCENTRADO), List.of(adaga));
        InteractionResult plain = attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), adaga);
        InteractionResult focused = attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO,
                roll(ORDINARY, DuelistaFeat.ATAQUE_CONCENTRADO), adaga);

        assertEquals(Skill.DISADVANTAGE_MALUS, focused.getSkillRollBonus() - plain.getSkillRollBonus());
        assertEquals(Skill.ADVANTAGE_BONUS, damageBonus(focused) - damageBonus(plain));
        assertNull(focused.getExtraDamageDice());
    }

    /** "Se utilizar este Talento ao mesmo tempo que Lutador Nato, ao invés da Vantagem … +1d6." */
    @Test
    void withLutadorNatoTheDanoGainsADieInsteadOfTheVantagem() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.ATAQUE_CONCENTRADO, DuelistaFeat.LUTADOR_NATO),
                List.of(adaga));
        InteractionResult plain = attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), adaga);
        InteractionResult both = attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO,
                roll(ORDINARY, DuelistaFeat.ATAQUE_CONCENTRADO, DuelistaFeat.LUTADOR_NATO), adaga);

        assertEquals(damageBonus(plain), damageBonus(both));
        assertEquals(1, both.getExtraDamageDice());
    }

    @Test
    void ataqueRapidoTradesTheDanoForTheAttack() {
        Weapon arco = arco();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.ATAQUE_RAPIDO), List.of(arco));
        InteractionResult plain = attack(duelist, SkillType.ATAQUE_A_DISTANCIA, roll(ORDINARY), arco);
        InteractionResult quick = attack(duelist, SkillType.ATAQUE_A_DISTANCIA,
                roll(ORDINARY, DuelistaFeat.ATAQUE_RAPIDO), arco);

        assertEquals(Skill.ADVANTAGE_BONUS, quick.getSkillRollBonus() - plain.getSkillRollBonus());
        assertEquals(Skill.DISADVANTAGE_MALUS, damageBonus(quick) - damageBonus(plain));
    }

    @Test
    void merelyHoldingTheTradesChangesNothing() {
        Weapon adaga = adaga();
        CharacterSheet holder = duelist(List.of(DuelistaFeat.ATAQUE_CONCENTRADO, DuelistaFeat.ATAQUE_RAPIDO,
                DuelistaFeat.FORCA_EXCESSIVA, DuelistaFeat.COMBATER_COM_2_ARMAS), List.of(adaga));
        CharacterSheet control = duelist(List.of(), List.of(adaga));

        InteractionResult held = attack(holder, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), adaga);
        InteractionResult none = attack(control, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), adaga);
        assertEquals(none.getSkillRollBonus(), held.getSkillRollBonus());
        assertEquals(damageBonus(none), damageBonus(held));
    }

    // ---------- Lutar Engajado's dano reroll ----------

    @Test
    void lutarEngajadoRerollsTheDanoOnlyWithLutadorNatoAgainstAnAdjacentTarget() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.LUTAR_ENGAJADO, DuelistaFeat.LUTADOR_NATO),
                List.of(adaga));
        CharacterSheet near = foe();
        CharacterSheet far = foe();
        SceneContext context = new SceneContext(List.of(), List.of(near, far),
                Map.of(near, Range.ADJACENTE, far, Range.DISTANCIA_CURTA));
        var melee = SkillInteractionFactory.create(SkillType.ATAQUE_CORPO_A_CORPO);

        assertEquals(1, melee.applyTo(duelist, context, roll(ORDINARY, DuelistaFeat.LUTADOR_NATO), near, adaga)
                .getDamageLowestDieRerolls());
        assertNull(melee.applyTo(duelist, context, roll(ORDINARY, DuelistaFeat.LUTADOR_NATO), far, adaga)
                .getDamageLowestDieRerolls());
        assertNull(melee.applyTo(duelist, context, roll(ORDINARY), near, adaga).getDamageLowestDieRerolls());
    }

    // ---------- Combater com 2 Armas ----------

    @Test
    void thePairCostsThreeAndItsSecondSwingNothingMore() {
        Weapon adaga = adaga();
        Weapon espada = espada();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.COMBATER_COM_2_ARMAS), List.of(adaga, espada));

        assertEquals(ActionCost.ofActionPoints(3),
                price(duelist, SkillType.ATAQUE_CORPO_A_CORPO, adaga, DuelistaFeat.COMBATER_COM_2_ARMAS));
        duelist.recordAction(attackAction(adaga, true, null, DuelistaFeat.COMBATER_COM_2_ARMAS));
        assertEquals(ActionCost.NONE,
                price(duelist, SkillType.ATAQUE_CORPO_A_CORPO, espada, DuelistaFeat.COMBATER_COM_2_ARMAS));
    }

    /** "um com cada uma de suas armas" — the second swing must be the other weapon. */
    @Test
    void theSecondSwingMustUseTheOtherWeapon() {
        Weapon adaga = adaga();
        Weapon espada = espada();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.COMBATER_COM_2_ARMAS), List.of(adaga, espada));
        duelist.recordAction(attackAction(adaga, true, null, DuelistaFeat.COMBATER_COM_2_ARMAS));

        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO,
                        roll(ORDINARY, DuelistaFeat.COMBATER_COM_2_ARMAS), adaga)).getMessage());
        attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY, DuelistaFeat.COMBATER_COM_2_ARMAS), espada);
    }

    @Test
    void oneDrawnWeaponCannotFightWithTwo() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.COMBATER_COM_2_ARMAS), List.of(adaga));

        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO,
                        roll(ORDINARY, DuelistaFeat.COMBATER_COM_2_ARMAS), adaga)).getMessage());
    }

    /** Desvantagem on both rolls; on the dano too unless one weapon is leve (table ruling). */
    @Test
    void theDanoPenaltyFallsOnlyWhenNeitherWeaponIsLight() {
        Weapon adaga = adaga();
        CharacterSheet lightPair = duelist(List.of(DuelistaFeat.COMBATER_COM_2_ARMAS), List.of(adaga, espada()));
        Weapon espada = espada();
        CharacterSheet heavyPair = duelist(List.of(DuelistaFeat.COMBATER_COM_2_ARMAS), List.of(espada, espada()));

        InteractionResult light = attack(lightPair, SkillType.ATAQUE_CORPO_A_CORPO,
                roll(ORDINARY, DuelistaFeat.COMBATER_COM_2_ARMAS), adaga);
        InteractionResult lightPlain = attack(lightPair, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), adaga);
        InteractionResult heavy = attack(heavyPair, SkillType.ATAQUE_CORPO_A_CORPO,
                roll(ORDINARY, DuelistaFeat.COMBATER_COM_2_ARMAS), espada);
        InteractionResult heavyPlain = attack(heavyPair, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), espada);

        assertEquals(Skill.DISADVANTAGE_MALUS, light.getSkillRollBonus() - lightPlain.getSkillRollBonus());
        assertEquals(0, damageBonus(light) - damageBonus(lightPlain));
        assertEquals(Skill.DISADVANTAGE_MALUS, damageBonus(heavy) - damageBonus(heavyPlain));
    }

    // ---------- Defesa com 2 Armas' "por 1 Rodada" ----------

    /** Both weapons swung on the holder's Turn: zero until that holder's next Turn begins. */
    @Test
    void theZeroedDefenseOutlastsTheRodadaUntilTheHoldersNextTurn() {
        Weapon adaga = adaga();
        Weapon sabre = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.DEFESA_COM_2_ARMAS), List.of(adaga, sabre));
        CharacterSheet control = duelist(List.of(), List.of(adaga, sabre));
        var defenses = new org.aventyrs.core.character.services.DefenseServiceImpl();

        duelist.startTurn(0);
        duelist.recordAction(attackAction(adaga, true, null));
        duelist.recordAction(attackAction(sabre, true, null));
        duelist.finishTurn();
        duelist.startNewRound();
        assertEquals(0, defenses.getTotalDefense(duelist, DefenseType.PHYSICAL)
                - defenses.getTotalDefense(control, DefenseType.PHYSICAL));

        // 4 Graduações: the +2/+4 rung, idle again.
        duelist.startTurn(1);
        assertEquals(4, defenses.getTotalDefense(duelist, DefenseType.PHYSICAL)
                - defenses.getTotalDefense(control, DefenseType.PHYSICAL));
    }

    // ---------- Ataque Repentino ----------

    @Test
    void ataqueRepentinoHalvesTheWholeAttackOncePerTurn() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.ATAQUE_REPENTINO), List.of(adaga));

        DeliveredAttackResult sudden = attackDelivery.resolve(delivery(duelist, foe(), adaga,
                roll(ORDINARY, DuelistaFeat.ATAQUE_REPENTINO), 5).build());
        assertTrue(sudden.isEveryTargetHalved());
        assertFalse(attackDelivery.resolve(delivery(duelist, foe(), adaga, roll(ORDINARY), 5).build())
                .isEveryTargetHalved());

        duelist.recordAction(sudden.getRecordedAction());
        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> attackDelivery.resolve(delivery(duelist, foe(), adaga,
                        roll(ORDINARY, DuelistaFeat.ATAQUE_REPENTINO), 5).build())).getMessage());
        duelist.startTurn(1);
        attackDelivery.resolve(delivery(duelist, foe(), adaga, roll(ORDINARY, DuelistaFeat.ATAQUE_REPENTINO), 5).build());
    }

    @Test
    void theRecordedActionNamesItsTargetAndWhatWasSpent() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.ATAQUE_RAPIDO), List.of(adaga));
        CharacterSheet target = foe();

        CombatantAction action = attackDelivery.resolve(delivery(duelist, target, adaga,
                roll(ORDINARY, DuelistaFeat.ATAQUE_RAPIDO), 5).build()).getRecordedAction();

        assertEquals(target.getId(), action.targetId());
        assertEquals(Set.of(DuelistaFeat.ATAQUE_RAPIDO), action.activatedFeats());
    }

    // ---------- Ataque Giratório and Um-Dois ----------

    @Test
    void ataqueGiratorioIsOncePerRodada() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.ATAQUE_GIRATORIO), List.of(adaga));
        attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY, DuelistaFeat.ATAQUE_GIRATORIO), adaga);
        duelist.recordAction(attackAction(adaga, true, null, DuelistaFeat.ATAQUE_GIRATORIO));

        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO,
                        roll(ORDINARY, DuelistaFeat.ATAQUE_GIRATORIO), adaga)).getMessage());
        duelist.startNewRound();
        attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY, DuelistaFeat.ATAQUE_GIRATORIO), adaga);
    }

    @Test
    void umDoisFollowsOnlyALandedAtaqueRapido() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.ATAQUE_RAPIDO, DuelistaFeat.ATAQUE_CONCENTRADO,
                DuelistaFeat.UM_DOIS), List.of(adaga));
        SkillRoll oneTwo = roll(ORDINARY, DuelistaFeat.UM_DOIS, DuelistaFeat.ATAQUE_CONCENTRADO);

        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, oneTwo, adaga)).getMessage());
        duelist.recordAction(attackAction(adaga, false, null, DuelistaFeat.ATAQUE_RAPIDO));
        assertThrows(IllegalOperationException.class,
                () -> attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, oneTwo, adaga));
        duelist.recordAction(attackAction(adaga, true, null, DuelistaFeat.ATAQUE_RAPIDO));

        attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, oneTwo, adaga);
        assertEquals(ActionCost.ofActionPoints(1), price(duelist, SkillType.ATAQUE_CORPO_A_CORPO, adaga,
                DuelistaFeat.UM_DOIS, DuelistaFeat.ATAQUE_CONCENTRADO));
    }

    // ---------- Explorar Pontos Fracos' second attack ----------

    @Test
    void onlyTheSecondAttackOnOneTargetWidensTheMargin() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.EXPLORAR_PONTOS_FRACOS), List.of(adaga));
        CharacterSheet target = foe();
        CharacterSheet other = foe();
        var melee = SkillInteractionFactory.create(SkillType.ATAQUE_CORPO_A_CORPO);

        assertEquals(CriticalResult.NONE, melee.applyTo(duelist, null, roll(FIFTEEN), target, adaga).getCriticalResult());
        duelist.recordAction(attackAction(adaga, true, target));
        duelist.recordAction(attackAction(adaga, true, other));
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR,
                melee.applyTo(duelist, null, roll(FIFTEEN), target, adaga).getCriticalResult());
        duelist.recordAction(attackAction(adaga, true, target));
        assertEquals(CriticalResult.NONE, melee.applyTo(duelist, null, roll(FIFTEEN), target, adaga).getCriticalResult());
    }

    // ---------- the riders the orchestrators report ----------

    @Test
    void forcaExcessivaAddsHalfVigorAndBillsTheHit() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.FORCA_EXCESSIVA), List.of(adaga));

        InteractionResult plain = attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), adaga);
        InteractionResult excessive = attack(duelist, SkillType.ATAQUE_CORPO_A_CORPO,
                roll(ORDINARY, DuelistaFeat.FORCA_EXCESSIVA), adaga);
        assertEquals(2, damageBonus(excessive) - damageBonus(plain));

        DeliveredAttackResult hit = attackDelivery.resolve(delivery(duelist, foe(), adaga,
                roll(ORDINARY, DuelistaFeat.FORCA_EXCESSIVA), 5).build());
        DeliveredAttackResult miss = attackDelivery.resolve(delivery(duelist, foe(), adaga,
                roll(ORDINARY, DuelistaFeat.FORCA_EXCESSIVA), 99).build());
        assertEquals(2, hit.getLockedSelfDamage());
        assertEquals(0, miss.getLockedSelfDamage());

        duelist.payWithVitality(hit.getLockedSelfDamage());
        duelist.heal(10);
        assertEquals(2, duelist.getDamageTaken());
    }

    /** Metade da Gnose (2) of the critical heals only with Roubo de Vida or a Descanso Verdadeiro. */
    @Test
    void feridasArdentesLocksTheCriticalsGnoseShare() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.EXPLORAR_PONTOS_FRACOS, DuelistaFeat.FERIDAS_ARDENTES),
                List.of(adaga));
        CharacterSheet victim = foe();

        DeliveredAttackResult critical = attackDelivery.resolve(delivery(duelist, victim, adaga,
                roll(SEVENTEEN), 5).build());
        ((DamageInteraction) critical.getAttackResult().getNextInteraction()).applyTo(victim, 5, true);

        assertEquals(5, victim.getDamageTaken());
        victim.heal(10);
        assertEquals(2, victim.getDamageTaken());
        victim.healFromLifeSteal(10);
        assertEquals(0, victim.getDamageTaken());
    }

    @Test
    void withoutFeridasArdentesTheCriticalHealsLikeAnyOther() {
        Weapon adaga = adaga();
        CharacterSheet duelist = duelist(List.of(DuelistaFeat.EXPLORAR_PONTOS_FRACOS), List.of(adaga));
        CharacterSheet victim = foe();

        DeliveredAttackResult critical = attackDelivery.resolve(delivery(duelist, victim, adaga,
                roll(SEVENTEEN), 5).build());
        ((DamageInteraction) critical.getAttackResult().getNextInteraction()).applyTo(victim, 5, true);
        victim.heal(10);

        assertEquals(0, victim.getDamageTaken());
    }

    /**
     * Golpe Trovejante: past the Corrente threshold, a critical applies the natural Efeito Crítico
     * twice. Three 6s, so Dilacerar is the Maior, which throws no dice of its own.
     */
    @Test
    void maestriaEmArmaDoublesTheNaturalCriticalEffectPastTheCorrenteThreshold() {
        Weapon claws = NaturalWeapon.GARRAS_AFIADAS;
        CharacterSheet master = duelist(List.of(EspecialistaEmArmaFeat.of(AttackMethod.NATURAL_WEAPON),
                DuelistaFeat.DOMINAR_ARMAS, DuelistaFeat.MAESTRIA_EM_ARMA), List.of());
        CharacterSheet plain = duelist(List.of(EspecialistaEmArmaFeat.of(AttackMethod.NATURAL_WEAPON),
                DuelistaFeat.DOMINAR_ARMAS), List.of());

        List<Integer> sixes = List.of(6, 6, 6);
        assertEquals(2, dilacerarsIn(attackDelivery.resolve(delivery(master, foe(), claws, roll(sixes), 0).build())));
        assertEquals(1, dilacerarsIn(attackDelivery.resolve(delivery(plain, foe(), claws, roll(sixes), 0).build())));
        int total = attackDelivery.resolve(delivery(master, foe(), claws, null, 0).build()).getAttackTotal() + 18;
        assertEquals(1, dilacerarsIn(attackDelivery.resolve(delivery(master, foe(), claws, roll(sixes), total - 1)
                .build())));
    }

    private static long dilacerarsIn(final DeliveredAttackResult result) {
        long count = 0;
        Interaction<CombatantSheet> stage = result.getAttackResult().getNextInteraction();
        while (stage != null) {
            if (stage instanceof Dilacerar) {
                count++;
            }
            stage = stage.getNextInteraction();
        }
        return count;
    }

    // ---------- Defender-se Atacando ----------

    private static IncomingAttack.IncomingAttackBuilder incoming(final CombatantSheet defender, final DefenseType type,
                                                                  final SkillRoll roll) {
        return IncomingAttack.builder()
                .defender(defender)
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .defenseType(type)
                .defenseSkill(SkillType.ATAQUE_CORPO_A_CORPO)
                .defenseRoll(roll);
    }

    @Test
    void aHeldCounterDamagesTheWeaponByForca() {
        CharacterSheet defender = duelist(List.of(DuelistaFeat.DEFENDER_SE_ATACANDO), List.of(adaga()));

        IncomingAttackResult result = new AttackReceiver().resolve(incoming(defender, DefenseType.PHYSICAL,
                roll(List.of(6, 6, 5), DuelistaFeat.DEFENDER_SE_ATACANDO)).build());

        assertTrue(result.getDefended());
        assertEquals(4, result.getCounterWeaponDamage());
        assertFalse(result.isAttackerDamageAdvantage());
        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, result.getRecordedAction().skill());
    }

    @Test
    void aFailedCounterHandsTheAttackerVantagemOnItsDano() {
        CharacterSheet defender = duelist(List.of(DuelistaFeat.DEFENDER_SE_ATACANDO), List.of(adaga()));

        IncomingAttackResult result = new AttackReceiver().resolve(incoming(defender, DefenseType.PHYSICAL,
                roll(List.of(1, 2, 2), DuelistaFeat.DEFENDER_SE_ATACANDO)).build());

        assertFalse(result.getDefended());
        assertTrue(result.isAttackerDamageAdvantage());
        assertNull(result.getCounterWeaponDamage());
    }

    @Test
    void theCounterIsOncePerRodada() {
        CharacterSheet defender = duelist(List.of(DuelistaFeat.DEFENDER_SE_ATACANDO), List.of(adaga()));
        AttackReceiver receiver = new AttackReceiver();
        defender.recordAction(receiver.resolve(incoming(defender, DefenseType.PHYSICAL,
                roll(List.of(6, 6, 5), DuelistaFeat.DEFENDER_SE_ATACANDO)).build()).getRecordedAction());

        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> receiver.resolve(incoming(defender, DefenseType.PHYSICAL,
                        roll(List.of(6, 6, 5), DuelistaFeat.DEFENDER_SE_ATACANDO)).build())).getMessage());
    }

    /** The Defesa Mágica needs the Superior, and never against an Encantamento or a Maldição. */
    @Test
    void theMagicDefenseNeedsTheSuperiorAndNeverStopsACurse() {
        SkillRoll counter = roll(List.of(6, 6, 5), DuelistaFeat.DEFENDER_SE_ATACANDO);
        CharacterSheet basic = duelist(List.of(DuelistaFeat.DEFENDER_SE_ATACANDO), List.of());
        CharacterSheet superior = duelist(List.of(DuelistaFeat.DEFENDER_SE_ATACANDO,
                DuelistaFeat.DEFENDER_SE_ATACANDO_SUPERIOR), List.of());
        AttackReceiver receiver = new AttackReceiver();

        assertEquals(DEFENSE_SUBSTITUTION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> receiver.resolve(incoming(basic, DefenseType.MAGIC, counter).build())).getMessage());
        assertTrue(receiver.resolve(incoming(superior, DefenseType.MAGIC, counter).build()).getDefended());
        assertThrows(IllegalOperationException.class, () -> receiver.resolve(incoming(superior, DefenseType.MAGIC,
                counter).enchantmentOrCurse(true).build()));
    }

    @Test
    void aSubstitutedDefenseMustSpendTheTalento() {
        CharacterSheet defender = duelist(List.of(DuelistaFeat.DEFENDER_SE_ATACANDO), List.of());

        assertEquals(DEFENSE_SUBSTITUTION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> new AttackReceiver().resolve(incoming(defender, DefenseType.PHYSICAL, roll(List.of(6, 6, 5)))
                        .build())).getMessage());
    }

    // ---------- Cego's 1d6, and Combater às Cegas ----------

    @Test
    void aBlindRollerThrowsTheD6AndAFailureMisses() {
        Weapon adaga = adaga();
        CharacterSheet blind = duelist(List.of(), List.of(adaga));
        blind.applyCondition(new Condition(ConditionType.CEGO, 2));

        InteractionResult reported = attack(blind, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), adaga);
        assertEquals(3, reported.getBlindCheckThreshold());
        assertNull(reported.getBlindCheckFailed());

        assertFalse(attackDelivery.resolve(delivery(blind, foe(), adaga, roll(ORDINARY).withBlindCheck(3), 0).build())
                .getHit());
        assertTrue(attackDelivery.resolve(delivery(blind, foe(), adaga, roll(ORDINARY).withBlindCheck(4), 0).build())
                .getHit());
    }

    @Test
    void combaterAsCegasSparesMeleeButNotRangedTheD6() {
        CharacterSheet blind = duelist(List.of(DuelistaFeat.COMBATER_AS_CEGAS), List.of(adaga(), arco()));
        blind.applyCondition(new Condition(ConditionType.CEGO, 2));

        assertNull(attack(blind, SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), adaga()).getBlindCheckThreshold());
        assertEquals(4, attack(blind, SkillType.ATAQUE_A_DISTANCIA, roll(ORDINARY), arco()).getBlindCheckThreshold());
    }

    @Test
    void aSightedRollerThrowsNoD6() {
        assertNull(attack(duelist(List.of(), List.of(adaga())), SkillType.ATAQUE_CORPO_A_CORPO, roll(ORDINARY), adaga())
                .getBlindCheckThreshold());
    }
}
