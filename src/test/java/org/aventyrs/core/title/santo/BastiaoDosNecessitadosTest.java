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
 * Bastião dos Necessitados' ally-facing half — "Seus aliados, que tenham menos quantidade de PV
 * atuais que você, recebem RDS igual a 1+ Metade das Habilidades de Santo que você possuir" —
 * resolved by scanning the damaged character's own adjacent allies for holders, rather than by
 * anyone granting them a {@code TemporaryBonus}.
 *
 * <p><b>RDS is RD</b>, not RA, so these read {@code getTotalDamageReduction}. V19 also gave the
 * figure a formula, so the holder is built with a stated number of Habilidades throughout.
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

    /**
     * A sheet whose owner holds Santo with Bastião dos Necessitados plus two more Habilidades —
     * 3 in all, so "1+ Metade das Habilidades" is 2 outward and half of that, 1, inward. Chosen so
     * both halves are non-zero and distinguishable from each other.
     */
    private CharacterSheet bastiaoHolderSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        character.grantTitle(new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS,
                SantoAbility.PROTECAO_UNGIDA, SantoAbility.GUARDA_VIDAS)), TitleSlot.PRIMARY);
        return CharacterSheet.of(character, new Player());
    }

    /** "1+ Metade das Habilidades de Santo que você possuir", for the 3-Habilidade holder above. */
    private static final int GRANTED_RDS = 2;

    /** "metade do valor capaz de fornecer". */
    private static final int SELF_RDS = GRANTED_RDS / 2;

    private int damageReduction(final CombatantSheet target, final SceneContext context) {
        return damageService.getTotalDamageReduction(target, null, null, context);
    }

    /** A context for whoever is taking the damage, with ally placed at the given band. */
    private SceneContext contextWithAllyAt(final CombatantSheet ally, final Range range) {
        return new SceneContext(List.of(ally), List.of(), Map.of(ally, range));
    }

    @Test
    void adjacentAllyWithBastiaoGrantsDamageReductionToALowerPvAlly() {
        CharacterSheet holder = bastiaoHolderSheet();
        CharacterSheet wounded = plainSheet();
        wounded.applyDamage(5);

        assertEquals(GRANTED_RDS, damageReduction(wounded, contextWithAllyAt(holder, Range.ADJACENTE)));
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

        assertEquals(GRANTED_RDS, damageReduction(wounded, contextWithAllyAt(holder, Range.ADJACENTE)));

        assertEquals(0, damageReduction(wounded, contextWithAllyAt(holder, Range.DISTANCIA_CURTA)));
    }

    /** "apenas aqueles com menos PV que você" — a healthier ally is not covered. */
    @Test
    void theGrantIsWithheldFromAnAllyWithMorePvThanTheHolder() {
        CharacterSheet holder = bastiaoHolderSheet();
        holder.applyDamage(5);
        CharacterSheet healthier = plainSheet();

        assertEquals(0, damageReduction(healthier, contextWithAllyAt(holder, Range.ADJACENTE)));
    }

    /** Equal PV is not "menos PV", so neither direction of the clause fires. */
    @Test
    void theGrantIsWithheldFromAnAllyOnExactlyEqualPv() {
        CharacterSheet holder = bastiaoHolderSheet();
        holder.applyDamage(5);
        CharacterSheet equallyWounded = plainSheet();
        equallyWounded.applyDamage(5);

        assertEquals(0, damageReduction(equallyWounded, contextWithAllyAt(holder, Range.ADJACENTE)));
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

        // The wounded one has an adjacent ally on higher PV: its self-facing half is inert
        // (it is protecting nobody), but the healthier neighbour's ally-facing half covers it.
        assertEquals(GRANTED_RDS, damageReduction(woundedHolder,
                contextWithAllyAt(healthierHolder, Range.ADJACENTE)));

        // The healthier one has an adjacent ally on lower PV: its own self-facing half fires at
        // half the outward figure, while the wounded neighbour grants it nothing outward.
        assertEquals(SELF_RDS, damageReduction(healthierHolder,
                contextWithAllyAt(woundedHolder, Range.ADJACENTE)));
    }

    /** An ally holding no Título at all grants nothing, adjacency notwithstanding. */
    @Test
    void anAdjacentAllyWithoutTheAbilityGrantsNothing() {
        CharacterSheet plainAlly = plainSheet();
        CharacterSheet wounded = plainSheet();
        wounded.applyDamage(5);

        assertEquals(0, damageReduction(wounded, contextWithAllyAt(plainAlly, Range.ADJACENTE)));
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

        assertEquals(0, damageReduction(wounded, null));
    }

    /** Sanity-check the PV arithmetic the tiers above depend on. */
    @Test
    void blankFixtureMaxHitPointsAreWhatTheseTestsAssume() {
        assertEquals(BLANK_MAX_HIT_POINTS,
                new org.aventyrs.core.character.services.HitPointsServiceImpl()
                        .getMaxHitPoints(plainSheet().getCharacter()));
    }
}
