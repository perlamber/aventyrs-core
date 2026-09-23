package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleAttackModifiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Chamar pra Briga and Punho de Ferro — the two passives the caller drives per attack. */
class SenhorDaBrigaPassivesTest {

    private static final NaturalWeapon FIST = NaturalWeapon.ATAQUE_DESARMADO;

    @BeforeEach
    void setup() {
        SenhorDaBrigaFixtures.loadTemplates();
    }

    private static SenhorDaBriga holding(final SenhorDaBrigaAbility ability) {
        return new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL), List.of(ability));
    }

    private static CharacterSheet armedFoe() {
        CharacterSheet foe = SenhorDaBrigaFixtures.combatant();
        SenhorDaBrigaFixtures.drawSword(foe);
        return foe;
    }

    @Test
    void chamarPraBrigaProvokesAnArmedFoeHitInTwoConsecutiveRodadas() {
        SenhorDaBriga title = holding(SenhorDaBrigaAbility.CHAMAR_PRA_BRIGA);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet foe = armedFoe();

        assertFalse(title.resolveChamarPraBriga(holder, foe, FIST, 0));
        assertTrue(title.resolveChamarPraBriga(holder, foe, FIST, 1));

        assertEquals(holder, foe.getForcedTargeting().orElseThrow().getEnchanter());
    }

    @Test
    void chamarPraBrigaNeedsTheRodadasToBeConsecutive() {
        SenhorDaBriga title = holding(SenhorDaBrigaAbility.CHAMAR_PRA_BRIGA);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet foe = armedFoe();

        title.resolveChamarPraBriga(holder, foe, FIST, 0);

        assertFalse(title.resolveChamarPraBriga(holder, foe, FIST, 2));
    }

    @Test
    void chamarPraBrigaIgnoresAnUnarmedFoeAndAWeaponHit() {
        SenhorDaBriga title = holding(SenhorDaBrigaAbility.CHAMAR_PRA_BRIGA);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet unarmed = SenhorDaBrigaFixtures.combatant();
        CharacterSheet armed = armedFoe();

        title.resolveChamarPraBriga(holder, unarmed, FIST, 0);
        assertFalse(title.resolveChamarPraBriga(holder, unarmed, FIST, 1));
        title.resolveChamarPraBriga(holder, armed, SenhorDaBrigaFixtures.sword(), 0);
        assertFalse(title.resolveChamarPraBriga(holder, armed, SenhorDaBrigaFixtures.sword(), 1));
    }

    @Test
    void chamarPraBrigaAffectsATargetOncePerCombat() {
        SenhorDaBriga title = holding(SenhorDaBrigaAbility.CHAMAR_PRA_BRIGA);
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);
        CharacterSheet foe = armedFoe();
        Scene scene = new Scene();
        scene.addParticipant(holder, 10);
        scene.addParticipant(foe, 5);
        scene.startCombat();

        title.resolveChamarPraBriga(holder, foe, FIST, 0);
        assertTrue(title.resolveChamarPraBriga(holder, foe, FIST, 1));
        assertFalse(title.resolveChamarPraBriga(holder, foe, FIST, 2));

        scene.endCombat();
        scene.startCombat();
        title.resolveChamarPraBriga(holder, foe, FIST, 0);
        assertTrue(title.resolveChamarPraBriga(holder, foe, FIST, 1));
    }

    @Test
    void punhoDeFerroCheapensTheFirstNaturalAttackOfAnOddRodadaAndHitsHarderInAnEvenOne() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(holding(SenhorDaBrigaAbility.PUNHO_DE_FERRO));
        SceneContext first = SenhorDaBrigaFixtures.context(0, Map.of());
        SceneContext second = SenhorDaBrigaFixtures.context(1, Map.of());

        TitleAttackModifiers odd = TitleAttackModifiers.resolve(holder, FIST, null, first);
        TitleAttackModifiers even = TitleAttackModifiers.resolve(holder, FIST, null, second);

        assertEquals(1, odd.actionPointReduction());
        assertEquals(0, odd.extraDamageDice());
        assertEquals(0, even.actionPointReduction());
        assertEquals(1, even.extraDamageDice());
    }

    @Test
    void punhoDeFerroIsSpentByTheRodadasFirstNaturalAttack() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(holding(SenhorDaBrigaAbility.PUNHO_DE_FERRO));
        holder.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, null, FIST,
                ActionCost.ofActionPoints(2), 0, null));

        assertTrue(TitleAttackModifiers.resolve(holder, FIST, null, SenhorDaBrigaFixtures.context(0, Map.of())).isNone());
    }
}
