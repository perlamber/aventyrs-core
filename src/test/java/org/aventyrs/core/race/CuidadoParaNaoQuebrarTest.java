package org.aventyrs.core.race;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.GiganteFeat;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Phase I: the Gigante's Cuidado para não Quebrar, and Zelo pelos Frágeis lifting it. */
class CuidadoParaNaoQuebrarTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheetOf(final Race race, final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(race)
                .sizeCategory(race.getBaseSizeCategory())
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static SceneContext adjacentTo(final CombatantSheet ally) {
        return new SceneContext(List.of(ally), List.of(), Map.of(ally, Range.ADJACENTE), TerrainType.values()[0],
                false, 0, false, null);
    }

    private static int bonus(final SkillType skill, final CombatantSheet sheet, final SceneContext context) {
        return skill.newInteraction().applyTo(sheet, context, new SkillRoll(List.of(3, 3, 3))).getSkillRollBonus();
    }

    @Test
    void aFragileAdjacentAllyCostsAVantagemOnForcaAndDestrezaRolls() {
        CharacterSheet giant = sheetOf(new Gigantes());
        SceneContext nearHuman = adjacentTo(sheetOf(new Human()));

        assertEquals(bonus(SkillType.ATLETISMO, giant, null) + Skill.DISADVANTAGE_MALUS,
                bonus(SkillType.ATLETISMO, giant, nearHuman));
        assertEquals(bonus(SkillType.ATTENTION, giant, null), bonus(SkillType.ATTENTION, giant, nearHuman));
    }

    @Test
    void anAllyOnlyOneCategoriaSmallerIsNoConcern() {
        CharacterSheet giant = sheetOf(new Gigantes());
        SceneContext nearOgre = adjacentTo(sheetOf(new Ogro(Ogro.Aptidao.values()[0])));

        assertEquals(bonus(SkillType.ATLETISMO, giant, null), bonus(SkillType.ATLETISMO, giant, nearOgre));
    }

    @Test
    void zeloPelosFrageisLiftsIt() {
        CharacterSheet giant = sheetOf(new Gigantes(), GiganteFeat.ZELO_PELOS_FRAGEIS);

        assertEquals(bonus(SkillType.ATLETISMO, giant, null),
                bonus(SkillType.ATLETISMO, giant, adjacentTo(sheetOf(new Human()))));
    }
}
