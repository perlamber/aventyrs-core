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

class ReactionsServiceImplTest {

    private final ReactionsService reactionsService = new ReactionsServiceImpl();

    private static class ReactionsBonusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.INSTINCT;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Reações bonus source.";
        }

        @Modifier(ModifierType.REACTIONS)
        public int bonus() {
            return 1;
        }
    }

    private static class ReactionsBonusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATTENTION;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Reações bonus source.";
        }

        @Modifier(ModifierType.REACTIONS)
        public int bonus() {
            return 1;
        }
    }

    private static class ReactionsMalusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATTENTION;
        }

        @Override
        public String getDescription() {
            return "Test-only -10 Reações malus source.";
        }

        @Modifier(ModifierType.REACTIONS)
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
    void defaultTotalReactionsIsOne() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        assertEquals(1, reactionsService.getTotalReactions(character));
    }

    @Test
    void attributeAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new ReactionsBonusAbility())
                .build();
        assertEquals(2, reactionsService.getTotalReactions(character));
    }

    @Test
    void skillCompetencyAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new ReactionsBonusSkillCompetencyAbility())
                .build();
        assertEquals(2, reactionsService.getTotalReactions(character));
    }

    @Test
    void unlockedExcellencyModifierIsAddedForATrainedSkill() {
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        attentionSkill.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATTENTION, attentionSkill)
                .build();

        assertEquals(2, reactionsService.getTotalReactions(character));
    }

    @Test
    void excellencyNotYetUnlockedGrantsNoBonus() {
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATTENTION, attentionSkill)
                .build();

        assertEquals(1, reactionsService.getTotalReactions(character));
    }

    @Test
    void totalReactionsNeverGoesBelowZero() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new ReactionsMalusSkillCompetencyAbility())
                .build();
        assertEquals(0, reactionsService.getTotalReactions(character));
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
    void perRoundTotalMatchesThePermanentTotalForAProfileWithNoReactionEffect() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA);
        assertEquals(1, reactionsService.getTotalReactions(sheet, 0));
        assertEquals(1, reactionsService.getTotalReactions(sheet, 3));
    }

    @Test
    void reflexosRapidosGrantsAnExtraReactionOnEveryRound() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.REFLEXOS_RAPIDOS);
        assertEquals(2, reactionsService.getTotalReactions(sheet, 0));
        assertEquals(2, reactionsService.getTotalReactions(sheet, 4));
    }

    @Test
    void estrategistaGrantsAnExtraReactionOnlyInsideACenaDeCombate() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.ESTRATEGISTA);
        assertEquals(1, reactionsService.getTotalReactions(sheet, 0));
        assertEquals(2, reactionsService.getTotalReactions(sheet, 0, COMBAT_SCENE));
    }

    @Test
    void aGrantedTemporaryBonusRaisesTheRoundsReactions() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA);
        sheet.grantTemporaryBonus(ModifierType.REACTIONS, 2, 1);
        assertEquals(3, reactionsService.getTotalReactions(sheet, 0));
    }

    @Test
    void aTemporaryBonusLeavesThePermanentTotalUntouched() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA);
        sheet.grantTemporaryBonus(ModifierType.REACTIONS, 2, 1);
        assertEquals(1, reactionsService.getTotalReactions(sheet.getCharacter()));
    }

    @Test
    void anExpiredTemporaryBonusNoLongerCounts() {
        CharacterSheet sheet = sheetWithProfile(ActionProfile.CONSCIENCIA_DEFENSIVA);
        sheet.grantTemporaryBonus(ModifierType.REACTIONS, 2, 1);
        sheet.finishTurn();
        assertEquals(1, reactionsService.getTotalReactions(sheet, 1));
    }

    @Test
    void perRoundTotalNeverGoesBelowZero() {
        CharacterSheet sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .skillCompetencyAbility(new ReactionsMalusSkillCompetencyAbility())
                .build(), new Player());
        assertEquals(0, reactionsService.getTotalReactions(sheet, 0));
    }
}
