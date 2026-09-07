package org.aventyrs.core.magic;

import org.aventyrs.core.feat.ElficoFeat;
import org.aventyrs.core.magic.catalog.AliadosDaNaturezaSpell;
import org.aventyrs.core.magic.catalog.RegeneracaoSpell;
import org.aventyrs.core.magic.catalog.VooSpell;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_MIMETIZED_SPELL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MimetizedSpellTest {

    @Test
    void retainsTheCatalogSpellAndUsesOnlyItsExplicitOverrides() {
        Spell catalogSpell = new TestSpell();
        ActivationTime activationOverride = ActivationTime.pa(1);
        SpellDuration durationOverride = SpellDuration.rodadas(2);

        MimetizedSpell mimetized = MimetizedSpell.builder()
                .spell(catalogSpell)
                .determinationPointCost(3)
                .activationTimeOverride(activationOverride)
                .durationOverride(durationOverride)
                .selfOnly(true)
                .build();

        assertSame(catalogSpell, mimetized.getSpell());
        assertEquals(3, mimetized.getDeterminationPointCost());
        assertEquals(activationOverride, mimetized.getEffectiveActivationTime());
        assertEquals(durationOverride, mimetized.getEffectiveDuration());
        assertTrue(mimetized.isSelfOnly());
    }

    @Test
    void preservesActivationAndDurationWhenTheMimicryRuleDoesNotOverrideThem() {
        Spell catalogSpell = new TestSpell();

        MimetizedSpell mimetized = MimetizedSpell.builder()
                .spell(catalogSpell)
                .determinationPointCost(2)
                .build();

        assertEquals(catalogSpell.getActivationTime(), mimetized.getEffectiveActivationTime());
        assertEquals(catalogSpell.getDuration(), mimetized.getEffectiveDuration());
        assertFalse(mimetized.isSelfOnly());
        assertEquals(Range.DISTANCIA_MEDIA, mimetized.getSpell().getTargeting().range());
    }

    @Test
    void rejectsAMissingSpellOrNonPositiveDeterminationCost() {
        IllegalOperationException missingSpell = assertThrows(IllegalOperationException.class,
                () -> MimetizedSpell.builder().determinationPointCost(2).build());
        IllegalOperationException zeroCost = assertThrows(IllegalOperationException.class,
                () -> MimetizedSpell.builder().spell(new TestSpell()).determinationPointCost(0).build());

        assertEquals(INVALID_MIMETIZED_SPELL, missingSpell.getMessage());
        assertEquals(INVALID_MIMETIZED_SPELL, zeroCost.getMessage());
    }

    @Test
    void guardianFeatsDescribeTheirFixedMimetizedSpells() {
        MimetizedSpell animal = ElficoFeat.GUARDIAO_DOS_BOSQUES.getGrantedMimetizedSpells(null).getFirst();
        MimetizedSpell flight = ElficoFeat.GUARDIAO_DAS_NUVENS.getGrantedMimetizedSpells(null).getFirst();
        MimetizedSpell regeneration = ElficoFeat.GUARDIAO_DAS_PROFUNDEZAS
                .getGrantedMimetizedSpells(null).getFirst();

        assertEquals(AliadosDaNaturezaSpell.CATIVAR_ANIMAL, animal.getSpell());
        assertEquals(2, animal.getDeterminationPointCost());
        assertEquals(VooSpell.VOO_LIVRE, flight.getSpell());
        assertEquals(3, flight.getDeterminationPointCost());
        assertEquals(ActivationTime.pa(1), flight.getEffectiveActivationTime());
        assertEquals(SpellDuration.rodadas(2), flight.getEffectiveDuration());
        assertTrue(flight.isSelfOnly());
        assertEquals(RegeneracaoSpell.REGENERACAO, regeneration.getSpell());
        assertEquals(2, regeneration.getDeterminationPointCost());
    }
}
