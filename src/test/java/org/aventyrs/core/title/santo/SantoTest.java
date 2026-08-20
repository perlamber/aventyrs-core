package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SantoTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void requiresNonNullSpecializationsAndAbilities() {
        assertThrows(NullPointerException.class, () -> new Santo(null, List.of()));
        assertThrows(NullPointerException.class, () -> new Santo(List.of(), null));
    }

    @Test
    void reportsItsOwnNameAndBaseEffectDescription() {
        Santo santo = new Santo(List.of(), List.of());

        assertEquals("Santo", santo.getName());
        assertFalse(santo.getBaseEffectDescription().isBlank());
    }

    @Test
    void primaryTitleBonusDescriptionIsSeparateFromTheBaseEffectDescription() {
        Santo santo = new Santo(List.of(), List.of());

        assertFalse(santo.getPrimaryTitleBonusDescription().isBlank());
        assertNotEquals(santo.getBaseEffectDescription(), santo.getPrimaryTitleBonusDescription());
        // The "Se este for seu Título Primário" clause belongs on its own method, not
        // concatenated onto the unconditional base effect text.
        assertFalse(santo.getBaseEffectDescription().contains("Título Primário"));
    }

    @Test
    void getSpecializationsAndGetAbilitiesRoundTripWhatWasPassedIn() {
        Santo santo = new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS));

        List<AventyrTitleSpecialization> specializations = santo.getSpecializations();
        List<AventyrTitleAbility> abilities = santo.getAbilities();

        assertEquals(0, specializations.size());
        assertEquals(List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS), abilities);
    }

    @Test
    void ignoreCriticalEffectDurationIsZeroWithNoSpecializationsOrSupremas() {
        Santo santo = new Santo(List.of(), List.of());

        assertEquals(0, santo.getIgnoreCriticalEffectDurationInRounds());
    }

    @Test
    void ignoreCriticalEffectDurationSumsSpecializationsAndSupremaOnlyAbilities() {
        // 0 held specializations (none passed to this Santo instance) + 1 Suprema
        // (GUARDA_VIDAS) — BASTIAO_DOS_NECESSITADOS is a Habilidade, not a Suprema, so it must
        // not count.
        Santo santo = new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS));

        assertEquals(1, santo.getIgnoreCriticalEffectDurationInRounds());
    }

    @Test
    void getAllAbilitiesCombinesSpecializationsAndAbilities() {
        Santo santo = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS));

        List<AventyrTitleAbility> allAbilities = santo.getAllAbilities();

        assertEquals(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ, SantoAbility.BASTIAO_DOS_NECESSITADOS), allAbilities);
    }

    @Test
    void getAllAbilitiesIsEmptyWhenNeitherIsHeld() {
        Santo santo = new Santo(List.of(), List.of());

        assertEquals(List.of(), santo.getAllAbilities());
    }

    @Test
    void grantAbilityAppendsToTheHeldAbilities() {
        Santo santo = new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS));

        santo.grantAbility(SantoAbility.GUARDA_VIDAS);

        assertEquals(List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS), santo.getAbilities());
        assertEquals(List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS), santo.getAllAbilities());
    }

    @Test
    void grantAbilityWorksEvenWhenConstructedWithAnImmutableAbilitiesList() {
        Santo santo = new Santo(List.of(), List.of());

        santo.grantAbility(SantoAbility.GUARDA_VIDAS);

        assertEquals(List.of(SantoAbility.GUARDA_VIDAS), santo.getAbilities());
    }

    private CharacterSheet newAdjacentAllySheet() {
        Character allyCharacter = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(allyCharacter, new Player());
    }

    @Test
    void defesasBonusIsTwoWithNoAdjacentAlliesOrSpecializationsOrSupremas() {
        Santo santo = new Santo(List.of(), List.of());

        assertEquals(2, santo.getDefesasBonus(null));
    }

    @Test
    void defesasBonusTreatsANullSceneContextAsNoAdjacentAllies() {
        Santo santo = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());

        assertEquals(3, santo.getDefesasBonus(null));
    }

    @Test
    void defesasBonusScalesWithAdjacentAlliesAndSpecializationsAndSupremas() {
        Santo santo = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS));
        CharacterSheet adjacentAlly = newAdjacentAllySheet();
        SceneContext sceneContext = new SceneContext(List.of(adjacentAlly), List.of(), Map.of(adjacentAlly, Range.ADJACENTE));

        // Base 2 + 1 adjacent ally + (1 Especialização + 1 Suprema [GUARDA_VIDAS]) = 5.
        assertEquals(5, santo.getDefesasBonus(sceneContext));
    }

    @Test
    void defesasBonusIgnoresNonAdjacentAllies() {
        Santo santo = new Santo(List.of(), List.of());
        CharacterSheet farAlly = newAdjacentAllySheet();
        SceneContext sceneContext = new SceneContext(List.of(farAlly), List.of(), Map.of(farAlly, Range.DISTANCIA_CURTA));

        assertEquals(2, santo.getDefesasBonus(sceneContext));
    }

    @Test
    void primaryTitleAllyDefesasBonusIsHalfOfTheDefesasBonusRoundedDown() {
        Santo santo = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS));
        CharacterSheet adjacentAlly = newAdjacentAllySheet();
        SceneContext sceneContext = new SceneContext(List.of(adjacentAlly), List.of(), Map.of(adjacentAlly, Range.ADJACENTE));

        // getDefesasBonus == 5 here (see the test above), half rounded down == 2.
        assertEquals(2, santo.getPrimaryTitleAllyDefesasBonus(sceneContext));
    }

    @Test
    void activateAbencoadoPelaLuzThrowsWhenNotHeld() {
        Santo santo = new Santo(List.of(), List.of());
        CharacterSheet target = newAdjacentAllySheet();

        assertThrows(IllegalOperationException.class, () -> santo.activateAbencoadoPelaLuz(target, null, true));
    }

    @Test
    void activateAbencoadoPelaLuzDelegatesToTheInteractionWhenHeld() {
        Santo santo = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());
        CharacterSheet target = newAdjacentAllySheet();
        target.applyDamage(1000);

        InteractionResult result = santo.activateAbencoadoPelaLuz(target, null, true);

        assertNotNull(result.getResourceGainValue());
        assertEquals(1000 - result.getResourceGainValue(), target.getDamageTaken());
    }

    @Test
    void activateGritoDeGuerraVulcanoThrowsWhenNotHeld() {
        Santo santo = new Santo(List.of(), List.of());
        CharacterSheet actor = newAdjacentAllySheet();

        assertThrows(IllegalOperationException.class, () -> santo.activateGritoDeGuerraVulcano(actor, null));
    }

    @Test
    void activateGritoDeGuerraVulcanoDelegatesToTheInteractionWhenHeld() {
        Santo santo = new Santo(List.of(), List.of(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO));
        CharacterSheet actor = newAdjacentAllySheet();

        InteractionResult result = santo.activateGritoDeGuerraVulcano(actor, null);

        assertEquals(3, result.getBlessings().size());
        assertEquals(actor.getCharacter().getStatus(), result.getResultStatus());
    }
}
