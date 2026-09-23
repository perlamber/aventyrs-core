package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.services.CriticalService;
import org.aventyrs.core.character.services.CriticalServiceImpl;
import org.aventyrs.core.character.services.DamageBaseService;
import org.aventyrs.core.character.services.DamageBaseServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Senhor da Briga's passives, exercised through the real services that scan a held Título — never
 * by calling the {@code resolve*} hooks directly.
 */
class SenhorDaBrigaScansTest {

    private static final Weapon FIST = NaturalWeapon.ATAQUE_DESARMADO;

    private final DamageBaseService damageBaseService = new DamageBaseServiceImpl();
    private final DefenseService defenseService = new DefenseServiceImpl();
    private final CriticalService criticalService = new CriticalServiceImpl();

    @BeforeEach
    void setup() {
        SenhorDaBrigaFixtures.loadTemplates();
    }

    private static SenhorDaBriga title(final List<SenhorDaBrigaSpecialization> specializations,
                                       final List<org.aventyrs.core.title.AventyrTitleAbility> abilities) {
        return new SenhorDaBriga(specializations, abilities);
    }

    private int defense(final CharacterSheet sheet) {
        return defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);
    }

    @Test
    void asTituloPrimarioItScalesUpANaturalWeaponsDanoBaseByOne() {
        CharacterSheet primary = SenhorDaBrigaFixtures.holder(title(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet secondary = SenhorDaBrigaFixtures.holder(title(List.of(), List.of()), TitleSlot.SECONDARY);
        CharacterSheet none = SenhorDaBrigaFixtures.combatant();

        assertEquals(damageBaseService.getDamageBase(none.getCharacter(), FIST).scaledUp(1),
                damageBaseService.getDamageBase(primary.getCharacter(), FIST));
        assertEquals(damageBaseService.getDamageBase(none.getCharacter(), FIST),
                damageBaseService.getDamageBase(secondary.getCharacter(), FIST));
    }

    @Test
    void theDanoBaseIncreaseIsForNaturalWeaponsOnly() {
        CharacterSheet primary = SenhorDaBrigaFixtures.holder(title(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet none = SenhorDaBrigaFixtures.combatant();
        Weapon sword = SenhorDaBrigaFixtures.sword();

        assertEquals(damageBaseService.getDamageBase(none.getCharacter(), sword),
                damageBaseService.getDamageBase(primary.getCharacter(), sword));
    }

    @Test
    void asTituloPrimarioItAddsTwoDefesasWhileArmedOnlyWithNaturalWeapons() {
        CharacterSheet primary = SenhorDaBrigaFixtures.holder(title(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet secondary = SenhorDaBrigaFixtures.holder(title(List.of(), List.of()), TitleSlot.SECONDARY);
        int baseline = defense(SenhorDaBrigaFixtures.combatant());

        assertEquals(baseline + SenhorDaBriga.PRIMARY_DEFESAS_BONUS, defense(primary));
        assertEquals(baseline, defense(secondary));

        SenhorDaBrigaFixtures.drawSword(primary);
        assertEquals(baseline, defense(primary));
    }

    @Test
    void fantasmaDoRingueStacksItsThreeConditionalDefesas() {
        SenhorDaBriga fantasma = title(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), List.of());
        CharacterSheet unarmored = SenhorDaBrigaFixtures.holder(fantasma, TitleSlot.SECONDARY);
        int baseline = defense(SenhorDaBrigaFixtures.combatant());

        // +1 always, +1 natural weapons only, +3 no Equipamento Defensivo.
        assertEquals(baseline + 5, defense(unarmored));

        CharacterSheet armoredCombatant = SenhorDaBrigaFixtures.combatant();
        armoredCombatant.getCharacter().equip(SenhorDaBrigaFixtures.armor());
        unarmored.getCharacter().equip(SenhorDaBrigaFixtures.armor());
        // The armour's own column counts for both; only the +3 goes.
        assertEquals(defense(armoredCombatant) + 2, defense(unarmored));
    }

    @Test
    void fantasmaDoRingueLosesItsNaturalWeaponsBonusWithASwordDrawn() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(
                title(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), List.of()), TitleSlot.SECONDARY);
        int baseline = defense(SenhorDaBrigaFixtures.combatant());

        SenhorDaBrigaFixtures.drawSword(holder);

        assertEquals(baseline + 4, defense(holder));
    }

    @Test
    void campeaoDaTavernaStacksOneDefesaPerCritUntilCombatEnds() {
        SenhorDaBriga campeao = title(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                List.of(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA));
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(campeao, TitleSlot.SECONDARY);
        Scene scene = new Scene();
        scene.addParticipant(holder, 10);
        scene.startCombat();
        int before = defense(holder);

        campeao.recordCriticalHit(holder);
        campeao.recordCriticalHit(holder);
        assertEquals(before + 2, defense(holder));

        scene.endCombat();
        assertEquals(before, defense(holder));
    }

    @Test
    void recordingACritWithoutCampeaoDoesNothing() {
        SenhorDaBriga title = title(List.of(), List.of());
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title);

        assertEquals(0, title.recordCriticalHit(holder));
    }

    @Test
    void punhoInigualavelAndCampeaoWidenANaturalWeaponsMargin() {
        CharacterSheet none = SenhorDaBrigaFixtures.combatant();
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                List.of(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA)));
        int baseline = criticalService.sumCriticalMarginIncrease(none, SkillType.ATAQUE_CORPO_A_CORPO, FIST, null);

        assertEquals(baseline + 3,
                criticalService.sumCriticalMarginIncrease(holder, SkillType.ATAQUE_CORPO_A_CORPO, FIST, null));
        assertEquals(criticalService.sumCriticalMarginIncrease(none, SkillType.ATAQUE_CORPO_A_CORPO,
                        SenhorDaBrigaFixtures.sword(), null),
                criticalService.sumCriticalMarginIncrease(holder, SkillType.ATAQUE_CORPO_A_CORPO,
                        SenhorDaBrigaFixtures.sword(), null));
    }

    @Test
    void fantasmaDoRingueWidensTheDefesaRollsMargin() {
        CharacterSheet none = SenhorDaBrigaFixtures.combatant();
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(
                title(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), List.of()));

        assertEquals(criticalService.sumCriticalMarginIncrease(none, SkillType.ESQUIVA_E_APARAR, null, null) + 2,
                criticalService.sumCriticalMarginIncrease(holder, SkillType.ESQUIVA_E_APARAR, null, null));
    }

    @Test
    void campeaoAddsTwoToAMinorCritAndADieToAMajorOne() {
        CharacterSheet holder = SenhorDaBrigaFixtures.holder(title(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                List.of(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA)));

        assertEquals(new CriticalDamage(0, Skill.ADVANTAGE_BONUS + 2), criticalService.getCriticalDamage(holder,
                SkillType.ATAQUE_CORPO_A_CORPO, FIST, CriticalResult.ACERTO_CRITICO_MENOR, null, null));
        assertEquals(new CriticalDamage(1, Skill.ADVANTAGE_BONUS), criticalService.getCriticalDamage(holder,
                SkillType.ATAQUE_CORPO_A_CORPO, FIST, CriticalResult.ACERTO_CRITICO_MAIOR, null, null));
    }
}
