package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InitiativeServiceImplTest {

    private final InitiativeService initiativeService = new InitiativeServiceImpl();

    private static class InitiativeBonusAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.INSTINCT;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Iniciativa bonus source.";
        }

        @Modifier(ModifierType.INITIATIVE)
        public int bonus() {
            return 1;
        }
    }

    private static class InitiativeBonusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATTENTION;
        }

        @Override
        public String getDescription() {
            return "Test-only +1 Iniciativa bonus source.";
        }

        @Modifier(ModifierType.INITIATIVE)
        public int bonus() {
            return 1;
        }
    }

    private static class InitiativeMalusSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATTENTION;
        }

        @Override
        public String getDescription() {
            return "Test-only -10 Iniciativa malus source.";
        }

        @Modifier(ModifierType.INITIATIVE)
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
    void defaultTotalInitiativeIsTheIniciativaEgoTotal() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        assertEquals(character.getEgos().getIniciativa().getTotal(), initiativeService.getTotalInitiative(character));
    }

    @Test
    void attributeAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new InitiativeBonusAbility())
                .build();
        assertEquals(character.getEgos().getIniciativa().getTotal() + 1, initiativeService.getTotalInitiative(character));
    }

    @Test
    void skillCompetencyAbilityModifierIsAdded() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new InitiativeBonusSkillCompetencyAbility())
                .build();
        assertEquals(character.getEgos().getIniciativa().getTotal() + 1, initiativeService.getTotalInitiative(character));
    }

    @Test
    void trainedSkillWithUnlockedExcellencyScanRunsWithoutError() {
        // No concrete SkillExcellency grants an INITIATIVE bonus yet, so this only exercises
        // the same 3-source scan Reactions/FreeActions use for a trained, Excelência-unlocked
        // skill — once one does, this should start asserting a non-zero bonus like
        // ReactionsServiceImplTest#unlockedExcellencyModifierIsAddedForATrainedSkill does.
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        attentionSkill.increaseGraduation(3);
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATTENTION, attentionSkill)
                .build();

        assertEquals(character.getEgos().getIniciativa().getTotal(), initiativeService.getTotalInitiative(character));
    }

    @Test
    void excellencyNotYetUnlockedGrantsNoBonus() {
        CharacterSkill attentionSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATTENTION_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATTENTION, attentionSkill)
                .build();

        assertEquals(character.getEgos().getIniciativa().getTotal(), initiativeService.getTotalInitiative(character));
    }

    @Test
    void totalInitiativeCanGoBelowZeroSinceItIsNotASpendableResource() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egos(CharacterEgos.builder().iniciativa(EgoValue.builder().base(2).build()).build())
                .skillCompetencyAbility(new InitiativeMalusSkillCompetencyAbility())
                .build();
        assertEquals(-8, initiativeService.getTotalInitiative(character));
    }
}
