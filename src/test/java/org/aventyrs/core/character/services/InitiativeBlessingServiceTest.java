package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InitiativeBlessingServiceTest {

    private final InitiativeBlessingService blessingService = new InitiativeBlessingServiceImpl();

    private static class TestAttributeAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.INSTINCT;
        }

        @Override
        public String getDescription() {
            return "Test-only initiative-won blessing source.";
        }

        @Override
        public List<Blessing> resolveInitiativeBlessings() {
            return List.of(new Blessing(ModifierType.REACTIONS, 1, 1, TargetScope.SELF, "TestAttributeAbility"));
        }
    }

    private static class TestSkillCompetencyAbility implements SkillCompetencyAbility {
        @Override
        public SkillType getSkillType() {
            return SkillType.ATLETISMO;
        }

        @Override
        public String getDescription() {
            return "Test-only initiative-won blessing source.";
        }

        @Override
        public List<Blessing> resolveInitiativeBlessings() {
            return List.of(new Blessing(ModifierType.FREE_ACTIONS, 1, 1, TargetScope.SELF, "TestSkillCompetencyAbility"));
        }
    }

    private static class RaceWithBlessingAbility extends Human {
        @Override
        public List<SkillCompetencyAbility> getRacialAbilities() {
            return List.of(new TestSkillCompetencyAbility());
        }
    }

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void resolvesNothingForACharacterWithNoBlessingSources() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        assertEquals(List.of(), blessingService.resolveBlessings(character));
    }

    @Test
    void resolvesTheEgoAdvantagesOwnBlessing() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO)
                .build();

        List<Blessing> blessings = blessingService.resolveBlessings(character);

        assertEquals(1, blessings.size());
        assertEquals(ModifierType.MOVEMENT, blessings.get(0).getModifierType());
        assertEquals(2, blessings.get(0).getValue());
        assertEquals(TargetScope.SELF_AND_ALLIES, blessings.get(0).getScope());
    }

    @Test
    void resolvesTheAttributeAbilitysOwnBlessing() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new TestAttributeAbility())
                .build();

        List<Blessing> blessings = blessingService.resolveBlessings(character);

        assertEquals(1, blessings.size());
        assertEquals(ModifierType.REACTIONS, blessings.get(0).getModifierType());
    }

    @Test
    void resolvesTheAcquiredSkillCompetencyAbilitysOwnBlessing() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .skillCompetencyAbility(new TestSkillCompetencyAbility())
                .build();

        assertEquals(1, blessingService.resolveBlessings(character).size());
        assertEquals(ModifierType.FREE_ACTIONS, blessingService.resolveBlessings(character).get(0).getModifierType());
    }

    @Test
    void resolvesARacialSkillCompetencyAbilitysOwnBlessing() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new RaceWithBlessingAbility())
                .build();

        assertEquals(1, blessingService.resolveBlessings(character).size());
        assertEquals(ModifierType.FREE_ACTIONS, blessingService.resolveBlessings(character).get(0).getModifierType());
    }

    @Test
    void combinesBlessingsFromAllThreeSources() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO)
                .attributeAbility(new TestAttributeAbility())
                .skillCompetencyAbility(new TestSkillCompetencyAbility())
                .build();

        assertEquals(3, blessingService.resolveBlessings(character).size());
    }
}
