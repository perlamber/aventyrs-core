package org.aventyrs.core.effect;

import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.magic.catalog.SpellCatalog;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RestType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEffectFactoryTest {

    @Test
    void aHealingMagiaYieldsAHealingEffect() {
        SpellEffect effect = SpellEffectFactory
                .create(VidaSpell.REVIGORAR, SpellEffectContext.FRIENDLY).orElseThrow();

        assertInstanceOf(HealingEffect.class, effect);
        assertSame(VidaSpell.REVIGORAR, effect.getSpell());
    }

    @Test
    void aCleansingMagiaYieldsADefensiveEffect() {
        SpellEffect effect = SpellEffectFactory
                .create(VidaSpell.EXORCIZAR, SpellEffectContext.FRIENDLY).orElseThrow();

        assertInstanceOf(DefensiveEffect.class, effect);
        assertSame(VidaSpell.EXORCIZAR, effect.getSpell());
    }

    @Test
    void aMagiaAuthoringNoEffectColumnYieldsNothing() {
        assertTrue(SpellEffectFactory.create(new TestSpell(), SpellEffectContext.FRIENDLY).isEmpty());
    }

    /**
     * The invariant that makes a selector unnecessary: each version authors at most one effect, so
     * choosing the version — {@code SpellCastRequest#isUseAlternateVersion()} — is the whole of
     * choosing the effect. Pinned here rather than guarded by a runtime throw, so an authoring
     * mistake stops the build instead of a cast.
     */
    @Test
    void noVersionAuthorsMoreThanOneEffect() {
        SpellCatalog.all().forEach(spell -> {
            assertTrue(SpellEffectFactory.authoredKinds(spell, SpellEffectContext.FRIENDLY).size() <= 1,
                    spell.getName() + " authors more than one effect kind");
            spell.getAlternateVersion().ifPresent(alternate ->
                    assertTrue(SpellEffectFactory.authoredKinds(alternate, SpellEffectContext.FRIENDLY).size() <= 1,
                            alternate.getName() + " authors more than one effect kind"));
        });
    }

    /**
     * The two versions of one Magia carry separate effect columns, which is why the version flag
     * settles which effect a cast uses — Revigorar heals a Descanso Longo, Benção Bifurcada a
     * Mínimo, and neither inherits the other's.
     */
    @Test
    void eachVersionBuildsItsOwnEffectAndNeverItsSiblings() {
        SpellHealingEffect base = (SpellHealingEffect) SpellEffectFactory
                .create(VidaSpell.REVIGORAR, SpellEffectContext.FRIENDLY).orElseThrow();
        SpellHealingEffect alternate = (SpellHealingEffect) SpellEffectFactory
                .create(VidaSpell.REVIGORAR.getAlternateVersion().orElseThrow(),
                        SpellEffectContext.FRIENDLY).orElseThrow();

        assertEquals(RestType.LONGO, base.getHealing().restEquivalent());
        assertEquals(RestType.MINIMO, alternate.getHealing().restEquivalent());
    }

    /**
     * A second version that authors no effect of its own yields none, rather than falling back to
     * its parent's — Fonte da Juventude is deliberately unauthored, and inheriting Nova
     * Rejuvenescedora's healing would silently give it the wrong Descanso tier.
     */
    @Test
    void aVersionAuthoringNoEffectDoesNotInheritItsParents() {
        assertTrue(SpellEffectFactory.create(VidaSpell.NOVA_REJUVENESCEDORA, SpellEffectContext.FRIENDLY)
                .isPresent());
        assertTrue(SpellEffectFactory.create(
                VidaSpell.NOVA_REJUVENESCEDORA.getAlternateVersion().orElseThrow(),
                SpellEffectContext.FRIENDLY).isEmpty());
    }

    @Test
    void theContextReachesTheEffectItBuilds() {
        SpellHealingEffect friendly = (SpellHealingEffect) SpellEffectFactory
                .create(VidaSpell.NOVA_REJUVENESCEDORA, SpellEffectContext.FRIENDLY).orElseThrow();
        SpellHealingEffect hostile = (SpellHealingEffect) SpellEffectFactory
                .create(VidaSpell.NOVA_REJUVENESCEDORA, SpellEffectContext.HOSTILE).orElseThrow();

        assertEquals(false, friendly.isHostileTarget());
        assertEquals(true, hostile.isHostileTarget());
    }

    @Test
    void anAlternateVersionGoesThroughTheSamePathWithNoSpecialCasing() {
        Spell bencaoBifurcada = VidaSpell.REVIGORAR.getAlternateVersion().orElseThrow();

        SpellEffect effect = SpellEffectFactory
                .create(bencaoBifurcada, SpellEffectContext.FRIENDLY).orElseThrow();

        assertInstanceOf(HealingEffect.class, effect);
        assertSame(bencaoBifurcada, effect.getSpell());
    }

    @Test
    void authoredKindsReportsEveryCategoryAMagiaActuallyHas() {
        assertEquals(List.of(SpellEffectKind.HEALING),
                SpellEffectFactory.authoredKinds(VidaSpell.REVIGORAR, SpellEffectContext.FRIENDLY));
        assertEquals(List.of(SpellEffectKind.DEFENSIVE),
                SpellEffectFactory.authoredKinds(VidaSpell.EXORCIZAR, SpellEffectContext.FRIENDLY));
        assertEquals(List.of(), SpellEffectFactory.authoredKinds(new TestSpell(), SpellEffectContext.FRIENDLY));
    }
}
