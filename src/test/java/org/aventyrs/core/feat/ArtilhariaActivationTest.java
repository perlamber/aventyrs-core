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
import org.aventyrs.core.character.services.AttackRangeService;
import org.aventyrs.core.character.services.AttackRangeServiceImpl;
import org.aventyrs.core.combat.AttackDelivery;
import org.aventyrs.core.combat.DeliveredAttack;
import org.aventyrs.core.combat.DeliveredAttackResult;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.Dilacerar;
import org.aventyrs.core.effect.RepeatedEffect;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillInteractionFactory;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistancia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.FEAT_ACTIVATION_NOT_PERMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Artilharia Talentos spent on one shot (core 0.0.70): Tiro Longo's Mira-Impecável step, Tiro
 * Rápido's follow-up, Tiro Duplo's and Tiro Múltiplo's extra projectiles, Disparo às Cegas' reach,
 * and Mira Mortal's ranged-choice Pré-requisito — each read off the service or orchestrator a
 * caller reads it from.
 */
class ArtilhariaActivationTest {

    /** 11 — a plain hit against a Defesa of 5. */
    private static final List<Integer> ORDINARY = List.of(4, 4, 3);

    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final AttackRangeService attackRangeService = new AttackRangeServiceImpl();
    private final AttackDelivery attackDelivery = new AttackDelivery();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** An arco at Distância Média, whose own Efeito Crítico is Dilacerar (dice-free at Maior). */
    private static AbstractWeapon arco(final String name) {
        return AbstractWeapon.builder()
                .name(name)
                .category(ItemCategory.BOW)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(DamageBase.of(1, 0))
                .skillType(SkillType.ATAQUE_A_DISTANCIA)
                .range(Range.DISTANCIA_MEDIA)
                .criticalEffect(CriticalEffectType.DILACERAR)
                .build();
    }

    private static CharacterSheet archer(final List<Feat> feats, final List<Weapon> drawn) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.DEXTERITY, 4)))
                .skill(SkillType.ATAQUE_A_DISTANCIA, CharacterSkill.builder()
                        .skill(new AtaqueADistancia())
                        .graduation(SkillGraduation.builder().graduationValue(7).build())
                        .build())
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

    private static InteractionResult shoot(final CombatantSheet archer, final SkillRoll roll, final Weapon weapon) {
        return SkillInteractionFactory.create(SkillType.ATAQUE_A_DISTANCIA).applyTo(archer, null, roll, null, weapon);
    }

    private static CombatantAction shot(final Weapon weapon, final Feat... activated) {
        return new CombatantAction(SkillType.ATAQUE_A_DISTANCIA, AttributeDomain.DEXTERITY, weapon,
                ActionCost.ofActionPoints(2), 0, null, null, Set.of(activated));
    }

    private DeliveredAttackResult deliver(final CombatantSheet archer, final Weapon weapon, final SkillRoll roll) {
        return attackDelivery.resolve(DeliveredAttack.builder()
                .attacker(archer)
                .defender(foe())
                .attackSkill(SkillType.ATAQUE_A_DISTANCIA)
                .attackSource(weapon)
                .defenseType(DefenseType.PHYSICAL)
                .defenseValue(0)
                .attackRoll(roll)
                .build());
    }

    private static long stagesOf(final DeliveredAttackResult result, final Class<?> type) {
        long count = 0;
        Interaction<CombatantSheet> stage = result.getAttackResult().getNextInteraction();
        while (stage != null) {
            Object effect = stage instanceof RepeatedEffect repeated ? repeated.getRepeated() : stage;
            if (type.isInstance(effect)) {
                count++;
            }
            stage = stage.getNextInteraction();
        }
        return count;
    }

    // ---------- Tiro Longo ----------

    /** "+1 nível", and "+1 passo (para o total de +2 níveis)" on a shot spending Mira Impecável. */
    @Test
    void tiroLongoGainsASecondStepWithMiraImpecavel() {
        Weapon arco = arco("Arco Curto");
        Character longShot = archer(List.of(ArtilhariaFeat.TIRO_LONGO, ArtilhariaFeat.MIRA_IMPECAVEL), List.of(arco))
                .getCharacter();

        assertEquals(Range.DISTANCIA_LONGA, attackRangeService.getEffectiveRange(longShot, arco));
        assertEquals(Range.DISTANCIA_MUITO_LONGA,
                attackRangeService.getEffectiveRange(longShot, arco, Set.of(ArtilhariaFeat.MIRA_IMPECAVEL)));
        assertEquals(Range.DISTANCIA_MUITO_LONGA.getMaxUnidadesDeDistancia(),
                attackRangeService.getEffectiveRangeInUnidadesDeDistancia(longShot, arco,
                        Set.of(ArtilhariaFeat.MIRA_IMPECAVEL)));
    }

    @Test
    void withoutTiroLongoMiraImpecavelWidensNothing() {
        Weapon arco = arco("Arco Curto");
        Character archer = archer(List.of(ArtilhariaFeat.MIRA_IMPECAVEL), List.of(arco)).getCharacter();

        assertEquals(attackRangeService.getEffectiveRangeInUnidadesDeDistancia(archer, arco),
                attackRangeService.getEffectiveRangeInUnidadesDeDistancia(archer, arco,
                        Set.of(ArtilhariaFeat.MIRA_IMPECAVEL)));
    }

    // ---------- Tiro Rápido ----------

    @Test
    void tiroRapidoFollowsAShotWithTheSameWeaponForOnePontoDeAcaoInDesvantagem() {
        Weapon arco = arco("Arco Curto");
        CharacterSheet archer = archer(List.of(ArtilhariaFeat.TIRO_RAPIDO), List.of(arco));
        int plain = shoot(archer, roll(ORDINARY), arco).getSkillRollBonus();

        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, assertThrows(IllegalOperationException.class,
                () -> shoot(archer, roll(ORDINARY, ArtilhariaFeat.TIRO_RAPIDO), arco)).getMessage());
        archer.recordAction(shot(arco));

        assertEquals(plain + Skill.DISADVANTAGE_MALUS,
                shoot(archer, roll(ORDINARY, ArtilhariaFeat.TIRO_RAPIDO), arco).getSkillRollBonus());
        assertEquals(ActionCost.ofActionPoints(1), actionPointsService.getAttackCost(archer,
                SkillType.ATAQUE_A_DISTANCIA, arco, Set.of(ArtilhariaFeat.TIRO_RAPIDO), 0));
    }

    @Test
    void tiroRapidoNeedsTheSameWeaponAndIsOncePerRodada() {
        Weapon arco = arco("Arco Curto");
        Weapon besta = arco("Besta Leve");
        CharacterSheet archer = archer(List.of(ArtilhariaFeat.TIRO_RAPIDO), List.of(arco, besta));
        archer.recordAction(shot(besta));
        assertThrows(IllegalOperationException.class,
                () -> shoot(archer, roll(ORDINARY, ArtilhariaFeat.TIRO_RAPIDO), arco));

        archer.recordAction(shot(arco));
        archer.recordAction(shot(arco, ArtilhariaFeat.TIRO_RAPIDO));
        assertThrows(IllegalOperationException.class,
                () -> shoot(archer, roll(ORDINARY, ArtilhariaFeat.TIRO_RAPIDO), arco));
    }

    // ---------- Tiro Duplo and Tiro Múltiplo ----------

    @Test
    void tiroDuploAddsADieAndDesvantagem() {
        Weapon arco = arco("Arco Curto");
        CharacterSheet archer = archer(List.of(ArtilhariaFeat.TIRO_DUPLO), List.of(arco));
        InteractionResult plain = shoot(archer, roll(ORDINARY), arco);
        InteractionResult doubled = shoot(archer, roll(ORDINARY, ArtilhariaFeat.TIRO_DUPLO), arco);

        assertEquals(Skill.DISADVANTAGE_MALUS, doubled.getSkillRollBonus() - plain.getSkillRollBonus());
        assertNull(plain.getExtraDamageDice());
        assertEquals(1, doubled.getExtraDamageDice());
    }

    /** "Correntes de Efeito e Efeitos Críticos aplicam seus efeitos duas vezes." */
    @Test
    void tiroDuploAppliesTheCriticalEffectTwice() {
        Weapon arco = arco("Arco Curto");
        CharacterSheet archer = archer(List.of(ArtilhariaFeat.TIRO_DUPLO), List.of(arco));

        assertEquals(1, stagesOf(deliver(archer, arco, roll(List.of(6, 6, 6))), Dilacerar.class));
        assertEquals(2, stagesOf(deliver(archer, arco, roll(List.of(6, 6, 6), ArtilhariaFeat.TIRO_DUPLO)),
                Dilacerar.class));
    }

    @Test
    void tiroDuploIsOncePerRodadaSharedWithTiroMultiploAndNeverAReacao() {
        Weapon arco = arco("Arco Curto");
        CharacterSheet archer = archer(List.of(ArtilhariaFeat.TIRO_DUPLO, ArtilhariaFeat.TIRO_MULTIPLO), List.of(arco));

        assertThrows(IllegalOperationException.class, () -> shoot(archer,
                new SkillRoll(ORDINARY, null, null, ActionCost.REACTION, null, Set.of(ArtilhariaFeat.TIRO_DUPLO)), arco));
        assertThrows(IllegalOperationException.class, () -> shoot(archer,
                roll(ORDINARY, ArtilhariaFeat.TIRO_DUPLO, ArtilhariaFeat.TIRO_MULTIPLO), arco));
        archer.recordAction(shot(arco, ArtilhariaFeat.TIRO_DUPLO));
        assertThrows(IllegalOperationException.class,
                () -> shoot(archer, roll(ORDINARY, ArtilhariaFeat.TIRO_MULTIPLO), arco));
        archer.startNewRound();
        shoot(archer, roll(ORDINARY, ArtilhariaFeat.TIRO_MULTIPLO), arco);
    }

    /** Table ruling: per projectile — with no Título Desperto, the minimum 2: +2d6, effects thrice. */
    @Test
    void tiroMultiploScalesPerProjectile() {
        Weapon arco = arco("Arco Curto");
        CharacterSheet archer = archer(List.of(ArtilhariaFeat.TIRO_MULTIPLO), List.of(arco));

        assertEquals(2, shoot(archer, roll(ORDINARY, ArtilhariaFeat.TIRO_MULTIPLO), arco).getExtraDamageDice());
        assertEquals(3, stagesOf(deliver(archer, arco, roll(List.of(6, 6, 6), ArtilhariaFeat.TIRO_MULTIPLO)),
                Dilacerar.class));
    }

    // ---------- Disparo às Cegas ----------

    @Test
    void disparoAsCegasSparesTheD6OnlyUpToDistanciaMedia() {
        Weapon arco = arco("Arco Curto");
        CharacterSheet archer = archer(List.of(DuelistaFeat.COMBATER_AS_CEGAS, ArtilhariaFeat.DISPARO_AS_CEGAS),
                List.of(arco));
        archer.applyCondition(new Condition(ConditionType.CEGO, 2));
        CharacterSheet near = foe();
        CharacterSheet far = foe();
        Map<CombatantSheet, Range> distances = Map.of(near, Range.DISTANCIA_MEDIA, far, Range.DISTANCIA_LONGA);

        assertTrue(archer.getBlindCheckThreshold(SkillType.ATAQUE_A_DISTANCIA,
                new SceneContext(List.of(), List.of(near, far), distances, null, false, 0, false, far)).isPresent());
        assertFalse(archer.getBlindCheckThreshold(SkillType.ATAQUE_A_DISTANCIA,
                new SceneContext(List.of(), List.of(near, far), distances, null, false, 0, false, near)).isPresent());
    }

    @Test
    void disparoAsCegasRequiresCombaterAsCegas() {
        assertTrue(ArtilhariaFeat.DISPARO_AS_CEGAS.getFeatRequirements().requiredFeats()
                .contains(DuelistaFeat.COMBATER_AS_CEGAS));
    }

    // ---------- Mira Mortal's Pré-requisito ----------

    /** "Acerto Crítico Aprimorado, com arma escolhida de ataque à distância ou arremesso." */
    @Test
    void miraMortalNeedsARangedAcertoCriticoAprimorado() {
        CharacterSheet bowman = archer(List.of(ArtilhariaFeat.MIRA_IMPECAVEL,
                AcertoCriticoAprimoradoFeat.of(AttackMethod.BOW)), List.of());
        CharacterSheet swordsman = archer(List.of(ArtilhariaFeat.MIRA_IMPECAVEL,
                AcertoCriticoAprimoradoFeat.of(AttackMethod.HEAVY_BLADE)), List.of());

        assertTrue(ArtilhariaFeat.MIRA_MORTAL.isEligible(bowman.getCharacter(), bowman));
        assertFalse(ArtilhariaFeat.MIRA_MORTAL.isEligible(swordsman.getCharacter(), swordsman));
    }
}
