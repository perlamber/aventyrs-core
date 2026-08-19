package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end coverage granting a real {@link Santo} to a {@link CharacterFixture}-built
 * Character and exercising it through the real scanning service that consumes it
 * ({@link DamageServiceImpl}), mirroring {@code ArtesAprimorarComArteAbilityTest}'s own
 * convention of not calling {@code resolveAbsoluteDamageReduction} directly.
 */
class SantoIntegrationTest {

    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void grantedTitleRoundTripsThroughCharacter() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        Santo santo = new Santo(List.of(),
                List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS));

        character.grantTitle(santo, TitleSlot.PRIMARY);

        assertEquals(List.of(santo), character.getAllTitles());
        assertEquals(santo, character.getPrimaryTitle());
    }

    @Test
    void ignoreCriticalEffectDurationReflectsTheReallyHeldSpecializationsAndSupremas() {
        Santo santo = new Santo(List.of(),
                List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS));

        // BASTIAO_DOS_NECESSITADOS is a Habilidade (doesn't count), GUARDA_VIDAS is a Suprema
        // (counts) — 0 Especializações + 1 Suprema.
        assertEquals(1, santo.getIgnoreCriticalEffectDurationInRounds());
    }

    @Test
    void damageServiceImplPicksUpBastiaoDosNecessitadosThroughAFullApplyDamageCall() {
        Character holder = CharacterFixture.blank(CharacterFixture.BLANK).build();
        holder.grantTitle(new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS)), TitleSlot.PRIMARY);
        CharacterSheet holderSheet = CharacterSheet.of(holder, new Player());

        Character allyCharacter = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet allySheet = CharacterSheet.of(allyCharacter, new Player());
        allySheet.applyDamage(5);

        SceneContext sceneContext = new SceneContext(List.of(allySheet), List.of(), Map.of(allySheet, Range.ADJACENTE));

        int totalDamageTaken = damageService.applyDamage(holderSheet, sceneContext, 10, false);

        assertEquals(10 - DamageService.DEFAULT_DAMAGE_REDUCTION, totalDamageTaken);
        assertEquals(10 - DamageService.DEFAULT_DAMAGE_REDUCTION, holderSheet.getDamageTaken());
    }
}
