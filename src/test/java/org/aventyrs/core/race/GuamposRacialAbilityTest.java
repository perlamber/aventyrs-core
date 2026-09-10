package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GuamposRacialAbilityTest {

    private final DamageService damageService = new DamageServiceImpl();

    private Character characterOf(final Race race) {
        return race.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(race)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();
    }

    @Test
    void everyAbilityHasADescription() {
        for (GuamposRacialAbility ability : GuamposRacialAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void vigorDeEponaGrantsAFlatDamageReductionModifier() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();

        assertEquals(1, modifierResolver.sumModifiers(
                GuamposRacialAbility.VIGOR_DE_EPONA, ModifierType.DAMAGE_REDUCTION));
    }

    // The point of this one is the *service*, not the constant: DamageServiceImpl used to scan
    // character.getSkillCompetencyAbilities() alone, which never sees a racial ability, so the
    // grant above would have arrived nowhere. Vigor de Epona is the first racial ability in the
    // codebase to grant RD.
    @Test
    void vigorDeEponaReachesTheDamageServiceThroughTheRace() {
        assertEquals(1, damageService.getTotalDamageReduction(characterOf(new Guampo())));
    }

    @Test
    void aRaceWithoutTheAbilityStillHasNoDamageReduction() {
        assertEquals(0, damageService.getTotalDamageReduction(characterOf(new Human())));
    }

    @Test
    void vigorDeEponaReducesEveryHitByOne() {
        Character guampo = characterOf(new Guampo());

        assertEquals(9, damageService.calculateFinalDamage(guampo, 10, false));
        // ...and is skipped when the attack ignores RD, unlike RA.
        assertEquals(10, damageService.calculateFinalDamage(guampo, 10, true));
    }

    @Test
    void vigorDeEponaGrantsNoAbsoluteDamageReduction() {
        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(characterOf(new Guampo())));
    }
}
