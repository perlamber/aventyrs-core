package org.aventyrs.core.ego;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.artes.ArtesInteraction;
import org.aventyrs.core.skill.atletismo.AtletismoInteraction;
import org.aventyrs.core.skill.persuasao.PersuasaoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoralHerdadaAbilityTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet sheetWith(final MoralHerdadaAbility.FamaChoice famaChoice) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.RECURSOS, new MoralHerdadaAbility(famaChoice))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private CharacterSheet blankSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void belongsToRecursosAndSharesTheCatalogDescription() {
        MoralHerdadaAbility ability = new MoralHerdadaAbility(MoralHerdadaAbility.FamaChoice.POSITIVA);

        assertEquals(EgoDomain.RECURSOS, ability.getEgoDomain());
        assertEquals(ResourcesAdvantage.MORAL_HERDADA.getDescription(), ability.getDescription());
        assertFalse(ability.getDescription().isBlank());
    }

    @Test
    void requiresAChosenFamaType() {
        assertThrows(NullPointerException.class, () -> new MoralHerdadaAbility(null));
    }

    @Test
    void grantsTheBaseBonusToArtesRollsWithNoFamaYet() {
        CharacterSheet withAbility = sheetWith(MoralHerdadaAbility.FamaChoice.POSITIVA);
        CharacterSheet blank = blankSheet();

        int withBonus = new ArtesInteraction().applyTo(withAbility).getSkillRollBonus();
        int withoutBonus = new ArtesInteraction().applyTo(blank).getSkillRollBonus();

        assertEquals(MoralHerdadaAbility.BASE_ROLL_BONUS, withBonus - withoutBonus);
    }

    @Test
    void grantsTheBaseBonusToPersuasaoRollsWithNoFamaYet() {
        CharacterSheet withAbility = sheetWith(MoralHerdadaAbility.FamaChoice.POSITIVA);
        CharacterSheet blank = blankSheet();

        int withBonus = new PersuasaoInteraction().applyTo(withAbility).getSkillRollBonus();
        int withoutBonus = new PersuasaoInteraction().applyTo(blank).getSkillRollBonus();

        assertEquals(MoralHerdadaAbility.BASE_ROLL_BONUS, withBonus - withoutBonus);
    }

    @Test
    void grantsNoBonusToAnUnrelatedSkill() {
        CharacterSheet withAbility = sheetWith(MoralHerdadaAbility.FamaChoice.POSITIVA);
        withAbility.increaseFamaPositiva(50);
        CharacterSheet blank = blankSheet();

        int withBonus = new AtletismoInteraction().applyTo(withAbility).getSkillRollBonus();
        int withoutBonus = new AtletismoInteraction().applyTo(blank).getSkillRollBonus();

        assertEquals(0, withBonus - withoutBonus);
    }

    @Test
    void bonusScalesByOnePerTenPointsOfTheChosenFama() {
        CharacterSheet sheet = sheetWith(MoralHerdadaAbility.FamaChoice.POSITIVA);
        sheet.increaseFamaPositiva(25);
        CharacterSheet blank = blankSheet();

        int withBonus = new ArtesInteraction().applyTo(sheet).getSkillRollBonus();
        int withoutBonus = new ArtesInteraction().applyTo(blank).getSkillRollBonus();

        assertEquals(MoralHerdadaAbility.BASE_ROLL_BONUS + 2, withBonus - withoutBonus);
    }

    @Test
    void choosingNegativaScalesWithFamaNegativaNotFamaPositiva() {
        CharacterSheet sheet = sheetWith(MoralHerdadaAbility.FamaChoice.NEGATIVA);
        sheet.increaseFamaPositiva(50);
        sheet.increaseFamaNegativa(10);
        CharacterSheet blank = blankSheet();

        int withBonus = new ArtesInteraction().applyTo(sheet).getSkillRollBonus();
        int withoutBonus = new ArtesInteraction().applyTo(blank).getSkillRollBonus();

        assertEquals(MoralHerdadaAbility.BASE_ROLL_BONUS + 1, withBonus - withoutBonus);
    }

    @Test
    void applyStartingFamaGrantsPositivaEqualToTheCharactersRecursosTotal() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egos(CharacterEgos.builder().recursos(EgoValue.builder().base(3).build()).build())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        MoralHerdadaAbility ability = new MoralHerdadaAbility(MoralHerdadaAbility.FamaChoice.POSITIVA);

        int result = ability.applyStartingFama(character, sheet);

        assertEquals(3, result);
        assertEquals(3, sheet.getFamaPositiva());
        assertEquals(0, sheet.getFamaNegativa());
    }

    @Test
    void applyStartingFamaGrantsNegativaInstead() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egos(CharacterEgos.builder().recursos(EgoValue.builder().base(4).build()).build())
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        MoralHerdadaAbility ability = new MoralHerdadaAbility(MoralHerdadaAbility.FamaChoice.NEGATIVA);

        int result = ability.applyStartingFama(character, sheet);

        assertEquals(4, result);
        assertEquals(4, sheet.getFamaNegativa());
        assertEquals(0, sheet.getFamaPositiva());
    }
}
