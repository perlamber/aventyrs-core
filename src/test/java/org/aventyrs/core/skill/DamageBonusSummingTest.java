package org.aventyrs.core.skill;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.feat.AssassinoFeat;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.aventyrs.core.skill.atletismo.AtletismoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dano-roll bonuses <b>sum across every source</b> rather than the first non-empty one winning,
 * and "Vantagem/Desvantagem em rolagens de Dano" is the same flat ±2 that Vantagem is on a
 * Perícia roll ({@link Skill#ADVANTAGE_BONUS}/{@link Skill#DISADVANTAGE_MALUS}).
 *
 * <p>The four sources are exercised through {@code AbstractSkillInteraction} rather than in
 * isolation — the point of the change is that they combine, which only shows end-to-end.
 */
class DamageBonusSummingTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).build(), new Player());
    }

    private static SceneContext at(final CombatantSheet other, final Range distance) {
        return new SceneContext(List.of(), List.of(other), Map.of(other, distance));
    }

    private DamageBonus meleeDamageBonus(final CharacterSheet roller, final SceneContext context) {
        return new AtaqueCorpoACorpoInteraction().applyTo(roller, context, null).getDamageBonus();
    }

    // ---------- DamageBonus.total, the summing rule itself ----------

    @Test
    void totalIsEmptyWhenThereIsNothingToReport() {
        assertEquals(Optional.empty(), DamageBonus.total(List.of(), 0));
    }

    @Test
    void totalSumsTypedBonusesAndTheFlatModifier() {
        Optional<DamageBonus> total = DamageBonus.total(
                List.of(new DamageBonus(1, DamageType.FISICO), new DamageBonus(3, DamageType.FISICO)), -2);

        assertTrue(total.isPresent());
        assertEquals(2, total.get().getValue());
        assertEquals(DamageType.FISICO, total.get().getType());
    }

    /** An untyped-only total takes FISICO, the established reading of "+N em rolagens de Danos". */
    @Test
    void anUntypedOnlyTotalIsPhysical() {
        Optional<DamageBonus> total = DamageBonus.total(List.of(), Skill.DISADVANTAGE_MALUS);

        assertTrue(total.isPresent());
        assertEquals(Skill.DISADVANTAGE_MALUS, total.get().getValue());
        assertEquals(DamageType.FISICO, total.get().getType());
    }

    /** Mixed types flatten to the first contributor's — documented, and it keeps the pairing valid. */
    @Test
    void mixedTypesTakeTheFirstContributorsType() {
        Optional<DamageBonus> total = DamageBonus.total(
                List.of(new DamageBonus(2, DamageType.MAGICO), new DamageBonus(1, DamageType.FISICO)), 0);

        assertEquals(3, total.orElseThrow().getValue());
        assertEquals(DamageType.MAGICO, total.orElseThrow().getType());
    }

    // ---------- conditions reaching a dano roll ----------

    @Test
    void desarmadoCostsTwoOnADanoRoll() {
        CharacterSheet roller = sheet();
        assertNull(meleeDamageBonus(roller, null));

        roller.applyCondition(new Condition(ConditionType.DESARMADO, 1));

        assertEquals(Skill.DISADVANTAGE_MALUS, meleeDamageBonus(roller, null).getValue());
    }

    @Test
    void caidoCostsTwoOnADanoRollToo() {
        CharacterSheet roller = sheet();
        roller.applyCondition(new Condition(ConditionType.CAIDO, 1));

        assertEquals(Skill.DISADVANTAGE_MALUS, meleeDamageBonus(roller, null).getValue());
    }

    /** Two conditions each costing a Desvantagem stack — they are separate maluses, not one state. */
    @Test
    void twoConditionsEachCostingADesvantagemStack() {
        CharacterSheet roller = sheet();
        roller.applyCondition(new Condition(ConditionType.DESARMADO, 1));
        roller.applyCondition(new Condition(ConditionType.CAIDO, 1));

        assertEquals(Skill.DISADVANTAGE_MALUS * 2, meleeDamageBonus(roller, null).getValue());
    }

    /** The fear ladder's dano malus is proximity-scoped, exactly like its Perícia one. */
    @Test
    void assustadoCostsDanoOnlyWithinFourUdOfTheFearsOrigin() {
        CharacterSheet roller = sheet();
        CharacterSheet fear = sheet();
        roller.applyCondition(new Condition(ConditionType.ASSUSTADO, 2, fear));

        assertEquals(Skill.DISADVANTAGE_MALUS,
                meleeDamageBonus(roller, at(fear, Range.DISTANCIA_CURTA)).getValue());
        assertNull(meleeDamageBonus(roller, at(fear, Range.DISTANCIA_MEDIA)));
    }

    /** Apavorado reaches 8UD where Assustado reaches 4UD — same widening as its Perícia malus. */
    @Test
    void apavoradoCostsDanoOutToEightUd() {
        CharacterSheet roller = sheet();
        CharacterSheet fear = sheet();
        roller.applyCondition(new Condition(ConditionType.APAVORADO, 2, fear));

        assertEquals(Skill.DISADVANTAGE_MALUS,
                meleeDamageBonus(roller, at(fear, Range.DISTANCIA_MEDIA)).getValue());
    }

    /** A dano malus must not leak onto a non-attack Perícia, which has no dano roll at all. */
    @Test
    void aDanoMalusDoesNotReachANonAttackPericia() {
        CharacterSheet roller = sheet();
        roller.applyCondition(new Condition(ConditionType.DESARMADO, 1));

        InteractionResult result = new AtletismoInteraction().applyTo(roller, null, null);

        assertNull(result.getDamageBonus());
    }

    // ---------- Flanqueado: the outward-facing half ----------

    /**
     * "Atacar um personagem Flanqueado garante Vantagem na rolagem de Dano" — the bonus lands on
     * the attacker, so it is the *target's* condition that produces it.
     */
    @Test
    void attackingAFlankedTargetGrantsVantagemOnTheDanoRoll() {
        CharacterSheet attacker = sheet();
        CharacterSheet victim = sheet();
        SceneContext context = at(victim, Range.ADJACENTE);
        InteractionResult before = new AtaqueCorpoACorpoInteraction()
                .applyTo(attacker, context, null, victim, null);
        assertNull(before.getDamageBonus());

        victim.applyCondition(new Condition(ConditionType.FLANQUEADO, 1));

        InteractionResult after = new AtaqueCorpoACorpoInteraction()
                .applyTo(attacker, context, null, victim, null);
        assertEquals(Skill.ADVANTAGE_BONUS, after.getDamageBonus().getValue());
    }

    /** Being Flanqueado costs its holder nothing on their own dano roll — only Defesas. */
    @Test
    void beingFlankedDoesNotCostTheHoldersOwnDanoRoll() {
        CharacterSheet roller = sheet();
        roller.applyCondition(new Condition(ConditionType.FLANQUEADO, 1));

        assertNull(meleeDamageBonus(roller, null));
    }

    /** With no attackTarget there is nobody to be flanked, so nothing is granted. */
    @Test
    void theFlankingBonusNeedsAnAttackTargetToReadItOff() {
        CharacterSheet attacker = sheet();
        CharacterSheet victim = sheet();
        victim.applyCondition(new Condition(ConditionType.FLANQUEADO, 1));

        assertNull(meleeDamageBonus(attacker, at(victim, Range.ADJACENTE)));
    }

    // ---------- a granted TemporaryBonus reaches a dano roll ----------

    @Test
    void aTemporaryDamageRollBonusIsSummedIn() {
        CharacterSheet roller = sheet();
        roller.grantTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS, 3, 1);
        roller.applyCondition(new Condition(ConditionType.DESARMADO, 1));

        // +3 granted, -2 from the condition.
        assertEquals(1, meleeDamageBonus(roller, null).getValue());
    }

    /**
     * Contributions that cancel out report <b>no</b> bonus rather than one of value 0 — the
     * contract {@link DamageBonus#total} documents, so a caller never has to distinguish "no
     * bonus" from "a bonus of nothing".
     */
    @Test
    void contributionsThatCancelOutReportNoBonusAtAll() {
        CharacterSheet roller = sheet();
        roller.grantTemporaryBonus(ModifierType.DAMAGE_ROLL_BONUS, -Skill.DISADVANTAGE_MALUS, 1);
        roller.applyCondition(new Condition(ConditionType.DESARMADO, 1));

        assertNull(meleeDamageBonus(roller, null));
    }

    /** A character with nothing at all still reports no bonus rather than a zero-valued one. */
    @Test
    void aCharacterWithNoDanoSourceReportsNoBonus() {
        assertNull(meleeDamageBonus(sheet(), null));
    }

    private static Character blank() {
        return CharacterFixture.blank(CharacterFixture.BLANK).build();
    }
    // ---------- a Talento reaching a dano roll ----------

    /**
     * {@code AssassinoFeat#GOLPE_DE_FINALIZACAO} — "Vantagem em rolagens de Danos em alvos que já
     * tenham perdido pelo menos a metade de seus PV". The first Talento to reach a dano roll, and
     * the reason {@code Feat#resolveDamageBonus} takes an attackTarget.
     */
    @Test
    void golpeDeFinalizacaoGrantsVantagemOnlyAgainstAWoundedTarget() throws IllegalOperationException {
        Character assassin = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).feats(new ArrayList<>()).build();
        CharacterSheet attacker = CharacterSheet.of(assassin, new Player());
        attacker.accumulateExperience(BigDecimal.valueOf(200));
        assassin.grantFeat(AssassinoFeat.GOLPE_DE_FINALIZACAO);

        CharacterSheet healthy = sheet();
        SceneContext vsHealthy = at(healthy, Range.ADJACENTE);
        assertNull(new AtaqueCorpoACorpoInteraction()
                .applyTo(attacker, vsHealthy, null, healthy, null).getDamageBonus());

        CharacterSheet wounded = sheet();
        wounded.applyDamage(new HitPointsServiceImpl().getMaxHitPoints(wounded.getCharacter()) / 2 + 1);
        SceneContext vsWounded = at(wounded, Range.ADJACENTE);

        assertEquals(Skill.ADVANTAGE_BONUS, new AtaqueCorpoACorpoInteraction()
                .applyTo(attacker, vsWounded, null, wounded, null).getDamageBonus().getValue());
    }

    /** With no target to measure, the Vantagem is withheld rather than granted by default. */
    @Test
    void golpeDeFinalizacaoGrantsNothingWithNoAttackTarget() {
        Character assassin = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).feats(new ArrayList<>()).build();
        assassin.grantFeat(AssassinoFeat.GOLPE_DE_FINALIZACAO);

        assertNull(meleeDamageBonus(CharacterSheet.of(assassin, new Player()), null));
    }
}
