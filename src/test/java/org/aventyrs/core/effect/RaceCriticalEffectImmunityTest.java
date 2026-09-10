package org.aventyrs.core.effect;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.race.Troll;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The player-facing half of Efeito Crítico immunity: a {@code Race}'s own Anatomia clause,
 * reaching {@link CriticalEffect#applicableTo} through {@code
 * AbstractCombatantSheet#getCriticalEffectImmunities()}. {@code CriticalEffectImmunityTest}
 * covers the foe-facing half, where the set is authored on a {@code MonsterTemplate} instead.
 *
 * <p>{@code Troll}'s Anatomia Vegetal is the only race granting any today, and only one of its
 * three — {@link CriticalEffectType#SANGRAMENTO} — has an implementation to filter, which is
 * precisely why the immunity is keyed on the enum rather than on the effect's class.
 */
class RaceCriticalEffectImmunityTest {

    private CharacterSheet sheetOf(final Race race) {
        Character character = race.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(race)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void aCharacterSheetReportsItsRacesOwnImmunities() {
        assertEquals(Set.of(CriticalEffectType.ATORDOANTE,
                        CriticalEffectType.FERIDA_PROFUNDA,
                        CriticalEffectType.SANGRAMENTO),
                sheetOf(new Troll()).getCriticalEffectImmunities());
    }

    @Test
    void aCharacterSheetOfARaceGrantingNoneStaysVulnerableToEverything() {
        assertTrue(sheetOf(new Human()).getCriticalEffectImmunities().isEmpty());
    }

    @Test
    void sangramentoIsFilteredOutAgainstATrollAndKeptAgainstAHuman() {
        List<CriticalEffect> effects = List.of(new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR));

        assertTrue(CriticalEffect.applicableTo(sheetOf(new Troll()), effects).isEmpty());
        assertEquals(effects, CriticalEffect.applicableTo(sheetOf(new Human()), effects));
    }
}
