package org.aventyrs.core.combat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.AssassinoFeat;
import org.aventyrs.core.feat.SaqueRelampagoFeat;
import org.aventyrs.core.feat.WeaponOrSpellChoice;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end: a character holding {@code SaqueRelampagoFeat} attacks twice per Rodada across a
 * Scene round boundary — the "-1 nível" applies only to the first cheap weapon attack each
 * Rodada, and {@code Scene#next()} clearing the action log at the wrap restores it.
 */
class SaqueRelampagoScenarioTest {

    private final FeatService featService = new FeatServiceImpl();

    private static final Weapon SWORD = AbstractWeapon.builder().name("Espada")
            .category(ItemCategory.HEAVY_BLADE).damageBase(DamageBase.of(2, 0))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private int cheapAttackReduction(final CharacterSheet attacker, final int round) {
        SkillRoll roll = new SkillRoll(List.of(3, 3, 3), null,
                org.aventyrs.core.skill.DifficultyLevel.MEDIUM.getBaseValue(), ActionCost.ofActionPoints(1));
        InteractionResult result = new AtaqueCorpoACorpoInteraction().applyTo(attacker, null, roll, null, SWORD);
        attacker.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO,
                result.getGoverningAttributeDomain(), SWORD, ActionCost.ofActionPoints(1), round,
                ActionOutcome.from(result)));
        return result.getDifficultyReduction();
    }

    @Test
    void theReductionAppliesOncePerRodadaAcrossARoundBoundary() throws IllegalOperationException {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .build();
        CharacterSheet hero = CharacterSheet.of(character, new Player());
        hero.accumulateExperience(BigDecimal.valueOf(50));
        featService.grantFeat(character, hero, AssassinoFeat.SAQUE_RAPIDO);
        featService.grantFeat(character, hero, SaqueRelampagoFeat.of(WeaponOrSpellChoice.WEAPONS));

        MonsterSheet foe = GenericMonster.CAPANGA.spawn(new Player());
        Scene scene = new Scene();
        scene.addParticipant(hero, 20);
        scene.addParticipant(foe, 5);

        scene.next();                                   // hero's Turn, Round 0
        int firstOfRoundZero = cheapAttackReduction(hero, 0);
        int secondOfRoundZero = cheapAttackReduction(hero, 0);

        scene.next();                                   // foe
        scene.next();                                   // wraps to Round 1, clears the log
        int firstOfRoundOne = cheapAttackReduction(hero, 1);

        assertEquals(firstOfRoundZero - 1, secondOfRoundZero, "second cheap attack of the Rodada is not eased");
        assertEquals(firstOfRoundZero, firstOfRoundOne, "the fresh Rodada eases the first cheap attack again");
    }
}
