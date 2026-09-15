package org.aventyrs.core.magic;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.catalog.PiromanciaSpell;
import org.aventyrs.core.magic.catalog.PolimorfismoSpell;
import org.aventyrs.core.magic.catalog.TransporteSpell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlternateSpellVersionTest {

    private Spell alternateOf(final Spell parent) {
        return parent.getAlternateVersion().orElseThrow();
    }

    @Test
    void anOverriddenColumnReportsTheDeltasValue() {
        Spell procrastinar = alternateOf(VidaSpell.ALIVIAR_A_DOR);

        assertEquals(ActivationTime.REACAO, procrastinar.getActivationTime());
        assertEquals("Procrastinar Ferimento", procrastinar.getName());
        assertEquals(SpellReach.DISTANCIA, procrastinar.getTargeting().reach());
        assertEquals(Range.DISTANCIA_CURTA, procrastinar.getTargeting().range());
    }

    @Test
    void anUnOverriddenColumnDelegatesToTheParent() {
        Spell procrastinar = alternateOf(VidaSpell.ALIVIAR_A_DOR);

        assertEquals(VidaSpell.ALIVIAR_A_DOR.getCastingDifficultyLevel(),
                procrastinar.getCastingDifficultyLevel());
        assertEquals(VidaSpell.ALIVIAR_A_DOR.getCriticalEffectType(), procrastinar.getCriticalEffectType());
        assertEquals(VidaSpell.ALIVIAR_A_DOR.getManaCost(), procrastinar.getManaCost());
        assertEquals(VidaSpell.ALIVIAR_A_DOR.getDescription(), procrastinar.getDescription());
    }

    @Test
    void itsEfeitoIsTheParentsEfeitoAlternativoProse() {
        Spell procrastinar = alternateOf(VidaSpell.ALIVIAR_A_DOR);

        assertEquals(VidaSpell.ALIVIAR_A_DOR.getSecondaryEffectDescription(),
                procrastinar.getPrimaryEffectDescription());
    }

    @Test
    void theRungTreeAndBranchAlwaysDelegate() {
        Spell alternate = alternateOf(VidaSpell.REVIGORAR);

        assertSame(VidaSpell.REVIGORAR.getBranchLevel(), alternate.getBranchLevel());
        assertSame(VidaSpell.REVIGORAR.getTree(), alternate.getTree());
        assertEquals(VidaSpell.REVIGORAR.getBranch(), alternate.getBranch());
    }

    @Test
    void anAlternateIsMarkedAsOneAndHasNoSecondVersionOfItsOwn() {
        Spell alternate = alternateOf(VidaSpell.ALIVIAR_A_DOR);

        assertTrue(alternate.isAlternateVersion());
        assertFalse(VidaSpell.ALIVIAR_A_DOR.isAlternateVersion());
        assertNull(alternate.getSecondaryEffectDescription());
        assertTrue(alternate.getAlternateVersion().isEmpty());
    }

    @Test
    void theParentsDualReachDoesNotSurviveAnOverriddenOne() {
        // Aliviar a Dor is "Pessoal ou Toque"; its second version states a reach of its own, so
        // the other half of the parent's dual Alcance does not carry over.
        assertTrue(VidaSpell.ALIVIAR_A_DOR.getAlternateTargeting().isPresent());
        assertTrue(alternateOf(VidaSpell.ALIVIAR_A_DOR).getAlternateTargeting().isEmpty());
    }

    @Test
    void aDualReachIsInheritedWhenTheAlternateStatesNone() {
        // Benção Bifurcada overrides the GD, not the Alcance.
        assertEquals(VidaSpell.REVIGORAR.getAlternateTargeting(),
                alternateOf(VidaSpell.REVIGORAR).getAlternateTargeting());
    }

    @Test
    void suppressingTheCriticalEffectReadsAsNoneRatherThanInherited() {
        Spell portalDeFuga = alternateOf(TransporteSpell.PORTAL);

        assertNotNull(TransporteSpell.PORTAL.getCriticalEffectType());
        assertNull(portalDeFuga.getCriticalEffectType(),
                "\"não recebe os benefícios de Efeitos Críticos\"");
    }

    @Test
    void anOverriddenCriticalEffectReplacesTheParents() {
        Spell armamento = alternateOf(PiromanciaSpell.GOLPE_DE_FOGO);

        assertEquals(CriticalEffectType.POTENCIALIZAR, PiromanciaSpell.GOLPE_DE_FOGO.getCriticalEffectType());
        assertEquals(CriticalEffectType.INFLAMAR, armamento.getCriticalEffectType());
    }

    @Test
    void thePericiaFlipTakesTheGdWithIt() {
        Spell maldicaoIgnis = alternateOf(PiromanciaSpell.FOGO_FATUO);

        assertEquals(SkillType.DOMINIO_DO_MANA, PiromanciaSpell.FOGO_FATUO.getAttackSkillType());
        assertEquals(SkillType.ATAQUE_A_DISTANCIA, maldicaoIgnis.getAttackSkillType());
        assertEquals(DifficultyLevel.MEDIUM, maldicaoIgnis.getCastingDifficultyLevel());
        assertFalse(PiromanciaSpell.FOGO_FATUO.isCastingDifficultyFlooredByTargetMagicDefense());
        assertTrue(maldicaoIgnis.isCastingDifficultyFlooredByTargetMagicDefense());
    }

    @Test
    void anActivationOverrideRunsBothDirections() {
        // Vida's parent costs PA and its alternate a Reação; Polimorfismo's runs the other way.
        assertEquals(ActivationTime.REACAO, alternateOf(VidaSpell.ALIVIAR_A_DOR).getActivationTime());
        assertEquals(ActivationType.PONTOS_DE_ACAO,
                PolimorfismoSpell.REARRANJO_CORPORAL.getAlternateVersion().orElseThrow()
                        .getActivationTime().type());
        assertEquals(ActivationTime.REACAO, PolimorfismoSpell.REARRANJO_CORPORAL.getActivationTime());
    }

    @Test
    void anEffectColumnIsTheAlternatesOwnAndNeverInherited() {
        // Benção Bifurcada heals a Descanso Mínimo where its parent heals a Longo.
        assertEquals(RestType.LONGO, VidaSpell.REVIGORAR.getHealing().orElseThrow().restEquivalent());
        assertEquals(RestType.MINIMO,
                alternateOf(VidaSpell.REVIGORAR).getHealing().orElseThrow().restEquivalent());

        // And an alternate that authors none reports none rather than its parent's.
        assertTrue(VidaSpell.NOVA_REJUVENESCEDORA.getHealing().isPresent());
        assertTrue(alternateOf(VidaSpell.NOVA_REJUVENESCEDORA).getHealing().isEmpty());
    }

    @Test
    void aMagiaWithNoEfeitoAlternativoHasNoSecondVersion() {
        assertTrue(VidaSpell.CORPO_FECHADO.getAlternateVersion().isEmpty());
    }

    @Test
    void eligibilityIsTheParentsSoTheAcquisitionGatesStayHonest() {
        AlternateSpellVersion alternate =
                (AlternateSpellVersion) alternateOf(VidaSpell.ALIVIAR_A_DOR);

        assertSame(VidaSpell.ALIVIAR_A_DOR, alternate.getParent());
    }
}
