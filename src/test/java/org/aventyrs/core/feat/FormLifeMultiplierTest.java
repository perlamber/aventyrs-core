package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.race.Fada;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Multiplicador de PV uplift a Forma carries — the last of Phase 5's four Forma deltas.
 *
 * <p>Two clauses, reaching the same total by deliberately different routes, which is why both are
 * tested here rather than each beside its own Talento: {@code FeericoFeat#ANCIENTEFORME}'s "+2 para
 * cada Título Aventyr Desperto" arrives as a {@code LIFE_MULTIPLIER} {@code TemporaryBonus}
 * (its Duração is fixed, so a countdown is safe), while {@code FormaMetamorfica#CAVALO_DE_CHIFRES}'
 * flat +1 is resolved <em>from</em> the worn shape (enterable through the bare mutator, where a
 * countdown could lapse mid-transformation). {@code HitPointsService#getLifeMultiplier(Character,
 * CombatantSheet)} sums both.
 *
 * <p>What needs guarding beyond the arithmetic is the <b>consequence</b>: max PV moves while
 * transformed and current PV follows it, because damage on the sheet is not re-scaled. That is a
 * derivation the rules text does not address, and the tests state it outright so a later reading
 * has something to disagree with.
 */
class FormLifeMultiplierTest {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Vigor 4, so one point of multiplier is worth a visible 4 PV. */
    private static Character.CharacterBuilder withVigor() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(4).build())
                        .build());
    }

    /** A Dríade with Ancienteforme and the stated number of Títulos Despertos. */
    private static Character anciente(final TitleSlot... slots) {
        Character character = withVigor().race(new Fada()).build();
        for (TitleSlot slot : slots) {
            character.grantTitle(new Santo(List.of(), List.of()), slot);
        }
        character.grantFeat(FeericoFeat.DRIADE);
        character.grantFeat(FeericoFeat.ANCIENTEFORME);
        return character;
    }

    private static ActiveAbility ancienteformeOf(final Character character) {
        return character.getActiveAbilities().stream()
                .filter(ability -> ability.resolveGrantedForm(character) == FormType.ANCIENTE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Ancienteforme transformation"));
    }

    // ---------- Ancienteforme: the TemporaryBonus route ----------

    /** "Para cada Título Aventyr Desperto … Multiplicador de PV … aumenta em +2." */
    @Test
    void ancienteformeRaisesTheMultiplierByTwoPerTitulo() throws IllegalOperationException {
        Character character = anciente(TitleSlot.PRIMARY, TitleSlot.SECONDARY);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = hitPointsService.getLifeMultiplier(character, sheet);

        activeAbilityService.activate(character, sheet, ancienteformeOf(character), 0);

        assertEquals(before + 4, hitPointsService.getLifeMultiplier(character, sheet),
                "two Títulos at +2 each");
    }

    /** One Título is worth half as much — the figure scales, it is not a flat grant. */
    @Test
    void theUpliftScalesWithTheTituloCount() throws IllegalOperationException {
        Character oneTitle = anciente(TitleSlot.PRIMARY);
        CharacterSheet oneSheet = CharacterSheet.of(oneTitle, new Player());
        int oneBefore = hitPointsService.getLifeMultiplier(oneTitle, oneSheet);
        activeAbilityService.activate(oneTitle, oneSheet, ancienteformeOf(oneTitle), 0);

        assertEquals(oneBefore + 2, hitPointsService.getLifeMultiplier(oneTitle, oneSheet));
    }

    /** And the multiplier is what scales max PV — the reason any of this matters. */
    @Test
    void theUpliftRaisesMaxHitPointsByVigorTimesTheIncrease() throws IllegalOperationException {
        Character character = anciente(TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = hitPointsService.getMaxHitPoints(character, sheet);
        int vigor = character.getEffectiveAttributeTotal(AttributeDomain.VIGOR, sheet);

        activeAbilityService.activate(character, sheet, ancienteformeOf(character), 0);

        assertTrue(vigor > 0, "the fixture must have Vigor for this to mean anything");
        assertEquals(before + vigor * 2, hitPointsService.getMaxHitPoints(character, sheet));
    }

    /**
     * <b>The uplift is Forma-scoped both ways.</b> It lands only on the sheet-aware overload —
     * the {@code Character}-only one is the "out of any shape" figure that character creation and
     * every sheet-less caller reads — and it lapses with the Duração, restoring the original.
     */
    @Test
    void theUpliftIsInvisibleToTheSheetlessOverloadAndLapsesWithTheDuracao()
            throws IllegalOperationException {
        Character character = anciente(TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int untransformed = hitPointsService.getMaxHitPoints(character);

        activeAbilityService.activate(character, sheet, ancienteformeOf(character), 0);

        assertNotEquals(untransformed, hitPointsService.getMaxHitPoints(character, sheet));
        assertEquals(untransformed, hitPointsService.getMaxHitPoints(character),
                "PV creation and every Character-only caller still read the untransformed figure");

        for (int rodada = 0; rodada < 4; rodada++) {
            sheet.finishTurn();
            sheet.startNewRound();
        }

        assertEquals(untransformed, hitPointsService.getMaxHitPoints(character, sheet),
                "the Duração lapsed and the multiplier came back down");
    }

    /**
     * <b>The consequence, stated outright.</b> Damage lives on the sheet and is not re-scaled, so
     * current PV = max − damage: transforming widens the headroom and un-transforming narrows it
     * again. A character damaged while an Anciente can therefore drop a {@link CharacterStatus}
     * tier the moment the shape ends.
     *
     * <p>⚠️ This is a <b>derivation</b>. The rules state what the multiplier becomes and say
     * nothing about accumulated damage when it falls back; re-scaling the damage, or clamping the
     * drop so a Forma can never kill its holder, are equally inventable. None is invented — if the
     * ruling ever lands, this is the test that should fail first.
     */
    @Test
    void currentHitPointsFollowTheMaximumUpAndBackDown() throws IllegalOperationException {
        Character character = anciente(TitleSlot.PRIMARY, TitleSlot.SECONDARY);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int baselineCurrent = hitPointsService.getCurrentHitPoints(character, sheet);

        activeAbilityService.activate(character, sheet, ancienteformeOf(character), 0);
        int transformedCurrent = hitPointsService.getCurrentHitPoints(character, sheet);

        assertTrue(transformedCurrent > baselineCurrent, "a bigger tree holds more life");

        // Damage taken as an Anciente stays on the sheet as a raw number.
        sheet.applyDamage(transformedCurrent - baselineCurrent + 1);

        assertEquals(baselineCurrent - 1, hitPointsService.getCurrentHitPoints(character, sheet));

        sheet.enterForm(null);

        assertTrue(hitPointsService.getCurrentHitPoints(character, sheet) < baselineCurrent - 1,
                "the headroom the Forma lent is taken back, and the damage is not re-scaled");
    }

    // ---------- Cavalo de Chifres: the resolve-from-the-Forma route ----------

    /**
     * "Multiplicador de PV +1", flat and un-scaled by Títulos — and resolved from the shape rather
     * than scheduled, so it holds for as long as the holder is a horse however they got there.
     */
    @Test
    void cavaloDeChifresRaisesTheMultiplierByOneWhileWorn() {
        Character character = withVigor()
                .race(new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human())).build();
        character.grantFeat(new MetamorfoseDraculeaFeat(EnumSet.of(
                FormaMetamorfica.CAVALO_DE_CHIFRES, FormaMetamorfica.MORCEGO_ATROZ)));
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = hitPointsService.getLifeMultiplier(character, sheet);

        sheet.enterForm(FormType.CAVALO_DE_CHIFRES);
        assertEquals(before + 1, hitPointsService.getLifeMultiplier(character, sheet));

        // The other chosen shape names no multiplier — the grant belongs to the row, not the Talento.
        sheet.enterForm(FormType.MORCEGO_ATROZ);
        assertEquals(before, hitPointsService.getLifeMultiplier(character, sheet));

        sheet.enterForm(null);
        assertEquals(before, hitPointsService.getLifeMultiplier(character, sheet));
    }

    /** A shape the holder never picked is not theirs, here as everywhere else on this Talento. */
    @Test
    void anUnchosenCavaloGrantsNoMultiplier() {
        Character character = withVigor()
                .race(new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human())).build();
        character.grantFeat(new MetamorfoseDraculeaFeat(EnumSet.of(
                FormaMetamorfica.SERPENTE_ESPINHOSA, FormaMetamorfica.MORCEGO_ATROZ)));
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = hitPointsService.getLifeMultiplier(character, sheet);

        sheet.enterForm(FormType.CAVALO_DE_CHIFRES);

        assertEquals(before, hitPointsService.getLifeMultiplier(character, sheet));
    }

    /** Draconato names no Multiplicador de PV — the control that keeps the Uplift column honest. */
    @Test
    void draconatoRaisesNoLifeMultiplier() {
        Character character = withVigor()
                .race(new org.aventyrs.core.race.NascidoDoDragao(
                        new Human(), org.aventyrs.core.magic.ElementalType.FOGO))
                .build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        character.grantFeat(DraconicoFeat.DRACONATO);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        int before = hitPointsService.getLifeMultiplier(character, sheet);

        sheet.enterForm(FormType.DRACONATO);

        assertEquals(before, hitPointsService.getLifeMultiplier(character, sheet));
    }
}
