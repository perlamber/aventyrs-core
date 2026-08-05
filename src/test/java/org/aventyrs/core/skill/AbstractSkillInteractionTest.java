package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.race.Elfos;
import org.aventyrs.core.race.ElfosRacialAbility;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.artes.ArtesCompetencyAbility;
import org.aventyrs.core.skill.attention.AttentionCompetencyAbility;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Demonstrates the extension point {@code AbstractSkillInteraction#applyTo(CharacterSheet,
 * SceneContext)} exists for: a "gang up" style ability whose bonus scales with how many
 * allies are within a given {@link Range}, clamped to a maximum — the shape a real ability
 * like that would take, using a private test-only subclass (no such ability currently exists
 * in the ruleset) the same way {@code DamageServiceImplTest} uses test-only ability classes to
 * exercise a generic mechanism without needing a real named consumer yet. Which skill this
 * hypothetical ability is attached to is arbitrary — Atletismo here — nothing about it is
 * Atletismo-specific.
 */
class AbstractSkillInteractionTest {

    private static final int PER_ALLY_BONUS = 1;
    private static final int MAX_BONUS = 3;

    private static class GangUpBonusInteraction extends AbstractSkillInteraction {
        GangUpBonusInteraction() {
            super(SkillType.ATLETISMO);
        }

        @Override
        public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext) {
            InteractionResult result = super.applyTo(target, sceneContext);
            if (sceneContext == null) {
                return result;
            }
            int gangUpBonus = Math.min(sceneContext.countAlliesWithin(Range.DISTANCIA_CURTA) * PER_ALLY_BONUS, MAX_BONUS);
            return result.toBuilder()
                    .skillRollBonus(result.getSkillRollBonus() + gangUpBonus)
                    .build();
        }
    }

    private final GangUpBonusInteraction interaction = new GangUpBonusInteraction();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private CharacterSheet actingSheet(final int strengthBase) {
        CharacterSkill atletismoSkill = CharacterSkillFixture.blank(CharacterSkillFixture.ATLETISMO_1).build();
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().base(strengthBase).build())
                        .build())
                .skill(SkillType.ATLETISMO, atletismoSkill)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void applyToWithNoSceneContextOmitsTheCountScaledBonus() {
        CharacterSheet sheet = actingSheet(2);

        InteractionResult result = interaction.applyTo(sheet);

        assertEquals(2, result.getSkillRollBonus());
    }

    @Test
    void applyToScalesTheBonusByHowManyAlliesAreWithinRange() {
        CharacterSheet sheet = actingSheet(2);
        CharacterSheet adjacentAlly = newSheet();
        CharacterSheet curtaAlly = newSheet();
        SceneContext sceneContext = new SceneContext(
                List.of(adjacentAlly, curtaAlly), List.of(),
                Map.of(adjacentAlly, Range.ADJACENTE, curtaAlly, Range.DISTANCIA_CURTA));

        InteractionResult result = interaction.applyTo(sheet, sceneContext);

        assertEquals(4, result.getSkillRollBonus()); // 2 (base) + 2 allies * 1
    }

    @Test
    void applyToClampsTheCountScaledBonusAtItsMaximum() {
        CharacterSheet sheet = actingSheet(2);
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        CharacterSheet c = newSheet();
        CharacterSheet d = newSheet();
        SceneContext sceneContext = new SceneContext(
                List.of(a, b, c, d), List.of(),
                Map.of(a, Range.ADJACENTE, b, Range.ADJACENTE, c, Range.ADJACENTE, d, Range.ADJACENTE));

        InteractionResult result = interaction.applyTo(sheet, sceneContext);

        assertEquals(5, result.getSkillRollBonus()); // 2 (base) + clamp(4, 3)
    }

    @Test
    void applyToIgnoresAlliesOutsideTheRelevantRange() {
        CharacterSheet sheet = actingSheet(2);
        CharacterSheet farAlly = newSheet();
        SceneContext sceneContext = new SceneContext(
                List.of(farAlly), List.of(), Map.of(farAlly, Range.DISTANCIA_MUITO_LONGA));

        InteractionResult result = interaction.applyTo(sheet, sceneContext);

        assertEquals(2, result.getSkillRollBonus());
    }

    @Test
    void applyToIgnoresEnemyCountForThisAllyScopedBonus() {
        CharacterSheet sheet = actingSheet(2);
        CharacterSheet adjacentEnemy = newSheet();
        SceneContext sceneContext = new SceneContext(
                List.of(), List.of(adjacentEnemy), Map.of(adjacentEnemy, Range.ADJACENTE));

        InteractionResult result = interaction.applyTo(sheet, sceneContext);

        assertEquals(2, result.getSkillRollBonus());
    }

    /**
     * {@link SkillRoll#getRequestedAbility()} validation applies to every skill generically
     * (it's implemented once, in {@code AbstractSkillInteraction} itself) — exercised here via
     * plain {@link AttentionInteraction} rather than the ATLETISMO-flavored {@code
     * GangUpBonusInteraction} above, since it's unrelated to the SceneContext extension point
     * this file otherwise demonstrates.
     */
    private CharacterSheet attentionSheetHolding(final SkillCompetencyAbility... abilities) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK);
        for (SkillCompetencyAbility ability : abilities) {
            builder.skillCompetencyAbility(ability);
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    @Test
    void applyToWithNoRequestedAbilitySkipsValidationEntirely() {
        CharacterSheet sheet = attentionSheetHolding();
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4));

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertNotNull(result);
    }

    @Test
    void applyToWithARequestedAbilityTheCharacterHoldsSucceeds() {
        CharacterSheet sheet = attentionSheetHolding(AttentionCompetencyAbility.PERCEPCAO_DE_FOXM);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), AttentionCompetencyAbility.PERCEPCAO_DE_FOXM);

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertNotNull(result);
    }

    @Test
    void applyToWithARequestedAbilityTheCharacterLacksThrows() {
        CharacterSheet sheet = attentionSheetHolding();
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), AttentionCompetencyAbility.PERCEPCAO_DE_FOXM);
        AttentionInteraction attentionInteraction = new AttentionInteraction();

        assertThrows(IllegalOperationException.class, () -> attentionInteraction.applyTo(sheet, null, skillRoll));
    }

    @Test
    void applyToWithARequestedAbilityFromADifferentSkillTypeThrowsEvenWhenHeld() {
        // DOM_BARDICO belongs to ARTES, not ATTENTION — holding it doesn't make it a valid
        // request on an AttentionInteraction roll.
        CharacterSheet sheet = attentionSheetHolding(ArtesCompetencyAbility.DOM_BARDICO);
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), ArtesCompetencyAbility.DOM_BARDICO);
        AttentionInteraction attentionInteraction = new AttentionInteraction();

        assertThrows(IllegalOperationException.class, () -> attentionInteraction.applyTo(sheet, null, skillRoll));
    }

    /**
     * A racial ability (see CLAUDE.md's "Racial Abilities reuse SkillCompetencyAbility"
     * section) is held via {@code character.getRace().getRacialAbilities()}, not {@code
     * skillCompetencyAbilities} — {@code validateRequestedAbility} must check both, the same
     * "held" test a {@code SkillCompetencyAbility} would need to pass.
     */
    @Test
    void applyToWithARequestedRacialAbilityTheCharacterHoldsSucceeds() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Elfos())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SkillRoll skillRoll = new SkillRoll(List.of(2, 3, 4), ElfosRacialAbility.SENTIDOS_ABSOLUTOS);

        InteractionResult result = new AttentionInteraction().applyTo(sheet, null, skillRoll);

        assertNotNull(result);
    }
}
