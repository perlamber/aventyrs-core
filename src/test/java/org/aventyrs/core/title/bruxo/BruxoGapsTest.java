package org.aventyrs.core.title.bruxo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.invocation.SummonEnhancement;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.summon.NatureSummon;
import org.aventyrs.core.monster.summon.NatureSummonKind;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.DamageScopeEffect;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.subordinate.SubordinateBenefits;
import org.aventyrs.core.subordinate.SubordinateServiceImpl;
import org.aventyrs.core.subordinate.Subordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Bruxo clauses first reported and later closed (core 0.1.3). */
class BruxoGapsTest {

    private final Player gm = new Player();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void theWingsGrantFlightForTheRolledDieAndTheBonus() {
        SummonEnhancement enhancement = SummonEnhancement.builder().flightBonusRounds(2).build();
        Blessing wings = enhancement.wings(4).orElseThrow();
        MonsterSheet summon = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).withEnhancement(enhancement).spawn(gm);

        summon.grantBlessing(wings);

        assertEquals(6, wings.getRounds());
        assertTrue(summon.getTemporaryBonus(ModifierType.GRANTS_FLIGHT) > 0);
        assertTrue(SummonEnhancement.NONE.wings(4).isEmpty());
    }

    @Test
    void theCometRollsAgainstTheHigherOfGdAndDm() {
        assertEquals(DifficultyLevel.MEDIUM.getBaseValue(),
                SummonEnhancement.cometTargetValue(DifficultyLevel.MEDIUM, 3));
        assertEquals(40, SummonEnhancement.cometTargetValue(DifficultyLevel.MEDIUM, 40));
    }

    @Test
    void anUnequippedVictimTakesNoEquipmentDamage() {
        assertEquals(0, SummonEnhancement.damageEquipment(CharacterFixture.blank(CharacterFixture.BLANK).build(), 5));
    }

    @Test
    void chamasDoNortePierceAFireImmunityByHalf() {
        DamageService damage = new DamageServiceImpl();
        MonsterSheet burning = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA)
                .withEnhancement(SummonEnhancement.builder().fireAttacks(true).build()).spawn(gm);
        MonsterSheet plain = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(gm);
        DamageDescriptor fire = new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO);

        assertEquals(5, damage.applyDamage(immuneToFire(), null, fire, burning, 10, true));
        assertEquals(0, damage.applyDamage(immuneToFire(), null, fire, plain, 10, true));
    }

    @Test
    void dismissingTheFamiliarReleasesOnlyItsSubordinado() {
        Bruxo bruxo = new Bruxo(List.of(BruxoSpecialization.ILUMINADO), List.of(BruxoAbility.FAMILIAR_MAIOR),
                Map.of(), java.util.Set.of(),
                new Familiar("Corvo", CreatureType.ABISSAL, SubordinateBenefit.TORRE_DEFESAS));
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(org.aventyrs.core.character.CharacterAttributes.builder()
                        .charisma(org.aventyrs.core.character.AttributeValue.builder()
                                .domain(org.aventyrs.core.character.AttributeDomain.CHARISMA).base(3).build())
                        .build())
                .build();
        character.grantTitle(bruxo, TitleSlot.PRIMARY);
        CharacterSheet holder = CharacterSheet.of(character, gm);
        SubordinateServiceImpl subordinates = new SubordinateServiceImpl();
        subordinates.command(holder, Subordinate.of(SubordinateBenefit.CAVALEIRO_ATTACK, false, "Outro"), null);
        subordinates.command(holder, new Subordinate(SubordinateBenefit.TORRE_DEFESAS, false, Bruxo.FAMILIAR_SOURCE,
                java.util.UUID.randomUUID(), null), null);

        assertTrue(bruxo.dismissFamiliar(holder, subordinates));

        assertEquals(List.of(SubordinateBenefit.CAVALEIRO_ATTACK),
                SubordinateBenefits.of(holder).stream().map(Subordinate::getBenefit).toList());
        assertFalse(bruxo.dismissFamiliar(holder, subordinates));
        assertTrue(bruxo.getFamiliar().isPresent());
    }

    private CharacterSheet immuneToFire() {
        CharacterSheet sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), gm);
        sheet.applyEffect(new DamageScopeEffect(DamageScopeEffect.Kind.IMMUNE, DamageScope.element(ElementalType.FOGO),
                null, "test"));
        return sheet;
    }
}
