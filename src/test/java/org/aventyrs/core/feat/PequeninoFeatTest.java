package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link PequeninoFeat#SILENCIO_PRE_SURPRESA} — Vantagem on the first Rolagem de Furtividade of
 * each Cena, read off {@code CombatantSheet#getActionsThisCena()}.
 */
class PequeninoFeatTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet silentPequenino() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).feats(new ArrayList<>()).build();
        character.grantFeat(PequeninoFeat.SILENCIO_PRE_SURPRESA);
        return CharacterSheet.of(character, new Player());
    }

    private static int rollBonus(final CharacterSheet sheet, final SkillType skillType) {
        return skillType.newInteraction()
                .applyTo(sheet, null, new SkillRoll(List.of(3, 3, 3)))
                .getSkillRollBonus();
    }

    private static CombatantAction furtividadeAction() {
        return new CombatantAction(SkillType.FURTIVIDADE, AttributeDomain.DEXTERITY, null,
                ActionCost.ofActionPoints(1), 0, null);
    }

    @Test
    void grantsVantagemOnTheFirstFurtividadeRollOfTheCena() {
        CharacterSheet sheet = silentPequenino();
        int first = rollBonus(sheet, SkillType.FURTIVIDADE);

        sheet.recordAction(furtividadeAction());

        assertEquals(first - Skill.ADVANTAGE_BONUS, rollBonus(sheet, SkillType.FURTIVIDADE));
    }

    @Test
    void theVantagemComesBackWhenANewCenaBegins() {
        CharacterSheet sheet = silentPequenino();
        int first = rollBonus(sheet, SkillType.FURTIVIDADE);
        sheet.recordAction(furtividadeAction());

        sheet.startNewScene();

        assertEquals(first, rollBonus(sheet, SkillType.FURTIVIDADE));
    }

    @Test
    void aRollOfAnotherPericiaDoesNotConsumeTheFurtividadeVantagem() {
        CharacterSheet sheet = silentPequenino();
        int first = rollBonus(sheet, SkillType.FURTIVIDADE);

        sheet.recordAction(new CombatantAction(SkillType.ATLETISMO, AttributeDomain.STRENGTH, null,
                ActionCost.ofActionPoints(1), 0, null));

        assertEquals(first, rollBonus(sheet, SkillType.FURTIVIDADE));
    }

    @Test
    void doesNotReachANonFurtividadePericia() {
        CharacterSheet sheet = silentPequenino();
        CharacterSheet plain = CharacterSheet.of(
                CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).feats(new ArrayList<>()).build(),
                new Player());

        assertEquals(rollBonus(plain, SkillType.ATLETISMO), rollBonus(sheet, SkillType.ATLETISMO));
    }
}
