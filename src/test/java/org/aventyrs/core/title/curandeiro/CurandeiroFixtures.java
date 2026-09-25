package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitleAbility;

import java.util.List;

/** Shared builders for the Curandeiro tests — call {@link #loadTemplates()} in a {@code @BeforeEach}. */
final class CurandeiroFixtures {

    /** Medicina e Cura 4, so Curar os Mortos reaches the dead of the last 4 / 2 = 2 Rodadas. */
    static final int MEDICINA_E_CURA = 4;

    private CurandeiroFixtures() {
    }

    static void loadTemplates() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    static Curandeiro curandeiro(final List<CurandeiroSpecialization> specializations,
                                 final AventyrTitleAbility... abilities) {
        return new Curandeiro(specializations, List.of(abilities));
    }

    /** Autocontrole and Sorte for every combatant here, so an Ego loan has points to move. */
    static final int EGO = 3;

    /** A Médico de Guerra holding abilities, with Medicina e Cura {@link #MEDICINA_E_CURA}. */
    static CharacterSheet holder(final AventyrTitleAbility... abilities) {
        return holder(List.of(CurandeiroSpecialization.MEDICO_DE_GUERRA), TitleSlot.PRIMARY, abilities);
    }

    /** A Curandeiro with specializations, in slot, with Medicina e Cura {@link #MEDICINA_E_CURA}. */
    static CharacterSheet holder(final List<CurandeiroSpecialization> specializations, final TitleSlot slot,
                                 final AventyrTitleAbility... abilities) {
        Character character = withMedicina();
        character.grantTitle(curandeiro(specializations, abilities), slot);
        return CharacterSheet.of(character, new Player());
    }

    /** A combatant with Medicina e Cura {@link #MEDICINA_E_CURA} and no Título. */
    static CharacterSheet medic() {
        return CharacterSheet.of(withMedicina(), new Player());
    }

    /** A blank combatant holding no Título at all. */
    static CharacterSheet bystander() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).egos(egos()).build(), new Player());
    }

    private static Character withMedicina() {
        CharacterSkill medicina = CharacterSkillFixture.blank(CharacterSkillFixture.MEDICINA_E_CURA_1).build();
        medicina.increaseGraduation(MEDICINA_E_CURA);
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.MEDICINA_E_CURA, medicina)
                .egos(egos())
                .build();
    }

    private static CharacterEgos egos() {
        return CharacterEgos.builder()
                .autocontrole(EgoValue.builder().base(EGO).build())
                .sorte(EgoValue.builder().base(EGO).build())
                .build();
    }
}
