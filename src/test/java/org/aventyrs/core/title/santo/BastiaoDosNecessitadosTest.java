package org.aventyrs.core.title.santo;

import java.util.List;
import java.util.Map;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bastião dos Necessitados' ally-facing half — "Aliados adjacentes, apenas aqueles com menos PV
 * que você, recebem RA" — resolved by scanning the damaged character's own adjacent allies for
 * holders, rather than by anyone granting them a {@code TemporaryBonus}.
 *
 * <p>{@link #theGrantIsWithheldOnceTheAllyIsNoLongerAdjacent} is the test that pins the design:
 * nothing is revoked anywhere in it, and no code runs between the two assertions except building
 * a new {@link SceneContext}. The answer changes because the question is asked against current
 * state, which is what a granted-and-revoked bonus could not have given without something
 * watching for the move.
 */
class BastiaoDosNecessitadosTest {

    /** Vigor 1, no {@code LIFE_MULTIPLIER} source: {@code 10 + 1 * 4}. */
    private static final int BLANK_MAX_HIT_POINTS = 14;

    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet plainSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    /** A sheet whose owner holds Santo with Bastião dos Necessitados in their primary slot. */
    private CharacterSheet bastiaoHolderSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS)), TitleSlot.PRIMARY);
        return CharacterSheet.of(character, new Player());
    }

    /** A context for whoever is taking the damage, with ally placed at the given band. */
    private SceneContext contextWithAllyAt(final CombatantSheet ally, final Range range) {
        return new SceneContext(List.of(ally), List.of(), Map.of(ally, range));
    }

    @Test
    void adjacentAllyWithBastiaoGrantsAbsoluteDamageReductionToALowerPvAlly() {
        CharacterSheet holder = bastiaoHolderSheet();
        CharacterSheet wounded = plainSheet();
        wounded.applyDamage(5);

        int reduction = damageService.getTotalAbsoluteDamageReduction(
                wounded, contextWithAllyAt(holder, Range.ADJACENTE));

        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, reduction);
    }

    /**
     * The whole point of scanning rather than granting: the bonus stops applying the moment the
     * holder is no longer adjacent, with nothing revoking anything.
     */
    @Test
    void theGrantIsWithheldOnceTheAllyIsNoLongerAdjacent() {
        CharacterSheet holder = bastiaoHolderSheet();
        CharacterSheet wounded = plainSheet();
        wounded.applyDamage(5);

        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalAbsoluteDamageReduction(
                wounded, contextWithAllyAt(holder, Range.ADJACENTE)));

        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(
                wounded, contextWithAllyAt(holder, Range.DISTANCIA_CURTA)));
    }

    /** "apenas aqueles com menos PV que você" — a healthier ally is not covered. */
    @Test
    void theGrantIsWithheldFromAnAllyWithMorePvThanTheHolder() {
        CharacterSheet holder = bastiaoHolderSheet();
        holder.applyDamage(5);
        CharacterSheet healthier = plainSheet();

        int reduction = damageService.getTotalAbsoluteDamageReduction(
                healthier, contextWithAllyAt(holder, Range.ADJACENTE));

        assertEquals(0, reduction);
    }

    /** Equal PV is not "menos PV", so neither direction of the clause fires. */
    @Test
    void theGrantIsWithheldFromAnAllyOnExactlyEqualPv() {
        CharacterSheet holder = bastiaoHolderSheet();
        holder.applyDamage(5);
        CharacterSheet equallyWounded = plainSheet();
        equallyWounded.applyDamage(5);

        int reduction = damageService.getTotalAbsoluteDamageReduction(
                equallyWounded, contextWithAllyAt(holder, Range.ADJACENTE));

        assertEquals(0, reduction);
    }

    /**
     * Both halves are independent hooks, so a Bastião holder standing next to a more-wounded
     * Bastião holder collects their own self-facing RA <i>and</i> the ally-facing RA the other
     * one grants outward.
     */
    @Test
    void theGrantStacksWithTheHoldersOwnSelfFacingHalf() {
        CharacterSheet healthierHolder = bastiaoHolderSheet();
        CharacterSheet woundedHolder = bastiaoHolderSheet();
        woundedHolder.applyDamage(5);

        // The wounded one has an adjacent ally on higher PV: its self-facing half is inert,
        // but the healthier neighbour's ally-facing half covers it.
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalAbsoluteDamageReduction(
                woundedHolder, contextWithAllyAt(healthierHolder, Range.ADJACENTE)));

        // The healthier one has an adjacent ally on lower PV: its own self-facing half fires,
        // while the wounded neighbour grants it nothing outward. Both hooks, one total each.
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalAbsoluteDamageReduction(
                healthierHolder, contextWithAllyAt(woundedHolder, Range.ADJACENTE)));
    }

    /** An ally holding no Título at all grants nothing, adjacency notwithstanding. */
    @Test
    void anAdjacentAllyWithoutTheAbilityGrantsNothing() {
        CharacterSheet plainAlly = plainSheet();
        CharacterSheet wounded = plainSheet();
        wounded.applyDamage(5);

        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(
                wounded, contextWithAllyAt(plainAlly, Range.ADJACENTE)));
    }

    /**
     * No {@code SceneContext} means no adjacency information at all, so the scan contributes
     * nothing — the same "condition not met, never an error" convention every other {@code
     * resolve*} hook follows for a {@code null} context.
     */
    @Test
    void noAllyGrantWithoutASceneContext() {
        CharacterSheet wounded = plainSheet();
        wounded.applyDamage(5);

        assertEquals(0, damageService.getTotalAbsoluteDamageReduction(wounded, null));
    }

    /** Sanity-check the PV arithmetic the tiers above depend on. */
    @Test
    void blankFixtureMaxHitPointsAreWhatTheseTestsAssume() {
        assertEquals(BLANK_MAX_HIT_POINTS,
                new org.aventyrs.core.character.services.HitPointsServiceImpl()
                        .getMaxHitPoints(plainSheet().getCharacter()));
    }
}
