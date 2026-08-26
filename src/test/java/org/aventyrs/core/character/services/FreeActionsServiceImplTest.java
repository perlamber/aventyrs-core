package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FreeActionsServiceImplTest {

    private final FreeActionsService freeActionsService = new FreeActionsServiceImpl();

    private static class FreeActionsBonusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.STRENGTH;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Ações Livres bonus source.";
        }

        @Modifier(ModifierType.FREE_ACTIONS)
        public int bonus() {
            return 1;
        }
    }

    private static class FreeActionsBonusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Ações Livres bonus source.";
        }

        @Modifier(ModifierType.FREE_ACTIONS)
        public int bonus() {
            return 1;
        }
    }

    private static class FreeActionsMalusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only -10 Ações Livres malus source.";
        }

        @Modifier(ModifierType.FREE_ACTIONS)
        public int malus() {
            return -10;
        }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    @Test
    void defaultTotalFreeActionsIsOne() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        assertEquals(1, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void attributeAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new FreeActionsBonusAbility())
                .build();
        assertEquals(2, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void skillCompetencyAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new FreeActionsBonusSkillCompetencyAbility())
                .build();
        assertEquals(2, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void unlockedExcellencyModifierIsAddedForATrainedSkill() {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        atletismoSkill.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATLETISMO, atletismoSkill)
                .build();

        assertEquals(2, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void excellencyNotYetUnlockedGrantsNoBonus() {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATLETISMO, atletismoSkill)
                .build();

        assertEquals(1, freeActionsService.getTotalFreeActions(character));
    }

    @Test
    void totalFreeActionsNeverGoesBelowZero() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new FreeActionsMalusSkillCompetencyAbility())
                .build();
        assertEquals(0, freeActionsService.getTotalFreeActions(character));
    }

    // --- Per-Round totals: the CombatantSheet overloads -----------------------------------------

    private static final SceneContext COMBAT_SCENE =
            new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false);

    private CharacterSheet sheetWithProfile(final ActionProfile profile) {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .actionProfile(profile)
                .build(), new Player());
    }

    @Test
    void perRoundTotalMatchesThePermanentTotalForAProfileWithNoFreeActionEffect() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA);
        assertEquals(1, freeActionsService.getTotalFreeActions(sheet, 0));
        assertEquals(1, freeActionsService.getTotalFreeActions(sheet, 3));
    }

    @Test
    void movimentoPlanejadoLeavesNoFreeActionOnTheFirstTurn() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.MOVIMENTO_PLANEJADO);
        assertEquals(0, freeActionsService.getTotalFreeActions(sheet, 0));
    }

    @Test
    void movimentoPlanejadoGrantsOneExtraFreeActionFromTheSecondTurnOnward() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.MOVIMENTO_PLANEJADO);
        assertEquals(2, freeActionsService.getTotalFreeActions(sheet, 1));
        assertEquals(2, freeActionsService.getTotalFreeActions(sheet, 7));
    }

    /**
     * "Não pode fazer Ações Livres" is a denial, not a -1 malus: an ability granting an extra
     * Ação Livre doesn't buy its way past the first Turn, because the profile is applied last.
     */
    @Test
    void movimentoPlanejadosFirstTurnDenialOutranksEveryOtherSource() {
        CharacterSheet sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .actionProfile(ActionProfile.MOVIMENTO_PLANEJADO)
                .attributeAbility(new FreeActionsBonusAbility())
                .build(), new Player());
        sheet.grantTemporaryBonus(ModifierType.FREE_ACTIONS, 3, 2);
        assertEquals(0, freeActionsService.getTotalFreeActions(sheet, 0));
        assertEquals(6, freeActionsService.getTotalFreeActions(sheet, 1));
    }

    @Test
    void movimentoPlanejadoTouchesNeitherThePermanentTotalNorReactions() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.MOVIMENTO_PLANEJADO);
        assertEquals(1, freeActionsService.getTotalFreeActions(sheet.getCharacter()));
        assertEquals(1, new ReactionsServiceImpl().getTotalReactions(sheet, 0));
    }

    @Test
    void estrategistaGrantsAnExtraFreeActionOnlyInsideACenaDeCombate() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.ESTRATEGISTA);
        assertEquals(1, freeActionsService.getTotalFreeActions(sheet, 0));
        assertEquals(2, freeActionsService.getTotalFreeActions(sheet, 0, COMBAT_SCENE));
    }

    @Test
    void aGrantedTemporaryBonusRaisesTheRoundsFreeActions() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA);
        sheet.grantTemporaryBonus(ModifierType.FREE_ACTIONS, 2, 1);
        assertEquals(3, freeActionsService.getTotalFreeActions(sheet, 0));
        assertEquals(1, freeActionsService.getTotalFreeActions(sheet.getCharacter()));
    }

    @Test
    void perRoundTotalNeverGoesBelowZero() {
        CharacterSheet sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .actionProfile(ActionProfile.MOVIMENTO_PLANEJADO)
                .skillCompetencyAbility(new FreeActionsMalusSkillCompetencyAbility())
                .build(), new Player());
        assertEquals(0, freeActionsService.getTotalFreeActions(sheet, 1));
    }
}
