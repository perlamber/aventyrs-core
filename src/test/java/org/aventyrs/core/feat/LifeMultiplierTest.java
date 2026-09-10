package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.race.Orc;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Multiplicador de PV is a <b>sum</b> across every held Talento, not a first-wins pick —
 * {@code HitPointsServiceImpl#getLifeMultiplier} walks {@code Character#getFeats()} and adds each
 * {@code Feat#resolveLifeMultiplierIncrease} on top of the {@code @Modifier} scan and the
 * Character's own base.
 *
 * <p>Worth pinning directly rather than leaving to each Talento's own test: the catalog has ten
 * clauses granting this multiplier and more will land, so what needs guarding is the arithmetic
 * that combines them — that a negative one subtracts, that a per-Título one tracks the count, and
 * that several at once stack.
 */
class LifeMultiplierTest {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();
    private final MagicPointsService magicPointsService = new MagicPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
    }

    /** An Orc — Terra nas Veias is race-locked — with a Vigor worth measuring PV against. */
    private static Character orc() {
        return character().race(new Orc())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(3).build())
                        .build())
                .build();
    }

    private static void awaken(final Character character, final TitleSlot... slots) {
        for (TitleSlot slot : slots) {
            character.grantTitle(new Santo(List.of(), List.of()), slot);
        }
    }

    /** {@code OrquicoFeat#TERRA_NAS_VEIAS} — "+1 para cada Título Aventyr desperto". */
    @Test
    void aPerTituloMultiplierTracksTheTituloCount() {
        Character orc = orc();
        int base = hitPointsService.getLifeMultiplier(orc);
        orc.grantFeat(OrquicoFeat.TERRA_NAS_VEIAS);

        assertEquals(base, hitPointsService.getLifeMultiplier(orc), "no Título, no bonus");

        awaken(orc, TitleSlot.PRIMARY);
        assertEquals(base + 1, hitPointsService.getLifeMultiplier(orc));

        awaken(orc, TitleSlot.SECONDARY);
        assertEquals(base + 2, hitPointsService.getLifeMultiplier(orc));
    }

    /**
     * {@code DestinoFeat#ABDICADOR} moves the other way — "+1 para cada Título ainda não Desperto",
     * i.e. the three {@link TitleSlot}s minus the filled ones. The catalog's only inverse
     * multiplier, and the case most likely to be broken by a careless change to the count.
     */
    @Test
    void abdicadorMovesInverselyWithTheTituloCount() {
        Character character = character().build();
        int base = hitPointsService.getLifeMultiplier(character);
        character.grantFeat(DestinoFeat.ABDICADOR);

        assertEquals(base + 3, hitPointsService.getLifeMultiplier(character), "three slots, none filled");

        awaken(character, TitleSlot.PRIMARY);
        assertEquals(base + 2, hitPointsService.getLifeMultiplier(character));

        awaken(character, TitleSlot.SECONDARY, TitleSlot.TERTIARY);
        assertEquals(base, hitPointsService.getLifeMultiplier(character), "all three awakened, nothing owed");
    }

    /** The same clause grants Pontos de Magia too — one sentence, two multipliers. */
    @Test
    void abdicadorRaisesTheManaMultiplierByTheSameCount() {
        Character character = character().build();
        int base = magicPointsService.getManaMultiplier(character);
        character.grantFeat(DestinoFeat.ABDICADOR);

        assertEquals(base + 3, magicPointsService.getManaMultiplier(character));

        awaken(character, TitleSlot.PRIMARY);
        assertEquals(base + 2, magicPointsService.getManaMultiplier(character));
    }

    /**
     * Several at once sum, and a negative one genuinely subtracts — {@code
     * MonstruosoFeat#OSSOS_OCOS}'s −1 is the catalog's only malus here, and the thing a
     * first-wins or max-wins implementation would silently drop.
     */
    @Test
    void multipliersFromSeveralTalentosSumIncludingTheNegativeOne() {
        Character orc = orc();
        int base = hitPointsService.getLifeMultiplier(orc);
        awaken(orc, TitleSlot.PRIMARY);

        orc.grantFeat(OrquicoFeat.TERRA_NAS_VEIAS);          // +1 per Título → +1
        assertEquals(base + 1, hitPointsService.getLifeMultiplier(orc));

        orc.grantFeat(SobrevivenciaFeat.VITALIDADE);         // flat +1 → +2
        assertEquals(base + 2, hitPointsService.getLifeMultiplier(orc));

        orc.grantFeat(MonstruosoFeat.OSSOS_OCOS);            // −1 → +1
        assertEquals(base + 1, hitPointsService.getLifeMultiplier(orc),
                "the malus subtracts rather than being ignored");
    }

    /** And the multiplier is what scales max PV — the reason any of this matters. */
    @Test
    void theMultiplierScalesMaxHitPointsByVigor() {
        Character orc = orc();
        awaken(orc, TitleSlot.PRIMARY);
        int before = hitPointsService.getMaxHitPoints(orc);
        int vigor = orc.getEffectiveAttributeTotal(AttributeDomain.VIGOR);

        orc.grantFeat(OrquicoFeat.TERRA_NAS_VEIAS);

        assertTrue(vigor > 0, "fixture must have Vigor for this to mean anything");
        assertEquals(before + vigor, hitPointsService.getMaxHitPoints(orc),
                "one more multiplier is one more Vigor's worth of PV");
    }
}
