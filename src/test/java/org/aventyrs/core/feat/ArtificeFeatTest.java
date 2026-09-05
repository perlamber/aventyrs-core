package org.aventyrs.core.feat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.profissao.Profissao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Regalia-crafting ladder, in its two separate halves: what a player must have to
 * <b>acquire</b> each rung ({@link Feat#isEligible} — Profissão Graduação, the rung below, the
 * forged-Regalia history) and what they must have in hand to <b>use</b> it ({@link
 * Feat#itsAllowedToCraftRegalia} — a Regalia in possession). The forge those permissions feed is
 * exercised in {@code ItemForgeryTest} / {@code EquipmentCraftingServiceImplTest}.
 */
class ArtificeFeatTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSkill profissao(final int graduation) {
        return CharacterSkill.builder()
                .skill(new Profissao())
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .build();
    }

    private static AbstractItem regalia(final RegaliaGrade grade) {
        return AbstractItem.builder().name("Regalia de Teste").category(ItemCategory.RING)
                .regaliaGrade(grade).build();
    }

    private static Character.CharacterBuilder crafter(final int profissaoGraduation) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.PROFISSAO, profissao(profissaoGraduation))
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>());
    }

    // ---------------------------------------------------------------- catalog basics

    @Test
    void everyConstantIsAnArtificeTalentoWithARealDescription() {
        assertEquals(3, ArtificeFeat.values().length);
        for (ArtificeFeat feat : ArtificeFeat.values()) {
            assertEquals(FeatCategory.ARTIFICE, feat.getFeatCategory());
            assertFalse(feat.getDescription().isBlank());
        }
    }

    // ------------------------------------------- acquiring a rung: possession is NOT a requirement

    /**
     * The distinction this ladder turns on: "a Regalia em sua posse" is what you need to forge,
     * not what you need to learn. A crafter with Profissão 7 and an empty pack qualifies.
     */
    @Test
    void menorNeedsProfissaoSevenAndNothingElse() {
        Character lowGraduation = crafter(6).build();
        assertFalse(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR.isEligible(lowGraduation));

        Character noRegalia = crafter(7).build();
        assertTrue(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR.isEligible(noRegalia));
    }

    @Test
    void superioresNeedsProfissaoTenTheMenorTalentoAndThreeForgedMenores() {
        Character crafter = crafter(10).build();
        assertFalse(ArtificeFeat.ARTESAO_DE_REGALIAS_SUPERIORES.isEligible(crafter));

        crafter.grantFeat(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);
        crafter.recordRegaliaCrafted(RegaliaGrade.MENOR);
        crafter.recordRegaliaCrafted(RegaliaGrade.MENOR);
        assertFalse(ArtificeFeat.ARTESAO_DE_REGALIAS_SUPERIORES.isEligible(crafter));

        crafter.recordRegaliaCrafted(RegaliaGrade.MENOR);
        assertTrue(ArtificeFeat.ARTESAO_DE_REGALIAS_SUPERIORES.isEligible(crafter));
    }

    @Test
    void divinasNeedsTheSuperioresTalentoAndThreeForgedSuperiores() {
        Character crafter = crafter(10).build();
        crafter.grantFeat(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);
        crafter.grantFeat(ArtificeFeat.ARTESAO_DE_REGALIAS_SUPERIORES);

        assertFalse(ArtificeFeat.ARTESAO_DE_REGALIAS_DIVINAS.isEligible(crafter));

        crafter.recordRegaliaCrafted(RegaliaGrade.SUPERIOR);
        crafter.recordRegaliaCrafted(RegaliaGrade.SUPERIOR);
        crafter.recordRegaliaCrafted(RegaliaGrade.SUPERIOR);
        assertTrue(ArtificeFeat.ARTESAO_DE_REGALIAS_DIVINAS.isEligible(crafter));
    }

    @Test
    void forgedMenoresDoNotCountTowardTheDivinasHistoryGate() {
        Character crafter = crafter(10).build();
        crafter.grantFeat(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);
        crafter.grantFeat(ArtificeFeat.ARTESAO_DE_REGALIAS_SUPERIORES);
        for (int i = 0; i < 5; i++) {
            crafter.recordRegaliaCrafted(RegaliaGrade.MENOR);
        }

        assertEquals(5, crafter.getRegaliasCrafted(RegaliaGrade.MENOR));
        assertEquals(0, crafter.getRegaliasCrafted(RegaliaGrade.SUPERIOR));
        assertFalse(ArtificeFeat.ARTESAO_DE_REGALIAS_DIVINAS.isEligible(crafter));
    }

    @Test
    void aFixtureBuiltCrafterWithNoHistoryReadsZeroForEveryGrade() {
        Character fresh = CharacterFixture.blank(CharacterFixture.BLANK).build();

        for (RegaliaGrade grade : RegaliaGrade.values()) {
            assertEquals(0, fresh.getRegaliasCrafted(grade));
        }
        fresh.recordRegaliaCrafted(RegaliaGrade.MENOR);
        assertEquals(1, fresh.getRegaliasCrafted(RegaliaGrade.MENOR));
    }

    @Test
    void featServiceGrantsMenorToACrafterWhoOwnsNoRegaliaAtAll() throws IllegalOperationException {
        FeatService featService = new FeatServiceImpl();
        Character crafter = crafter(7).build();
        CharacterSheet sheet = CharacterSheet.of(crafter, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(50));

        featService.grantFeat(crafter, sheet, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        assertTrue(crafter.getFeats().contains(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR));
        // …and they still may not forge until one is in hand.
        assertNull(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR.itsAllowedToCraftRegalia(crafter));
    }

    @Test
    void theLadderClimbsOneRungAtATime() {
        Character crafter = crafter(10).build();
        List<ArtificeFeat> ladder = List.of(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR,
                ArtificeFeat.ARTESAO_DE_REGALIAS_SUPERIORES, ArtificeFeat.ARTESAO_DE_REGALIAS_DIVINAS);

        assertTrue(ladder.get(0).isEligible(crafter));
        assertFalse(ladder.get(1).isEligible(crafter));
        assertFalse(ladder.get(2).isEligible(crafter));
    }

    // ------------------------------------------------ using a rung: the permission and its condition

    @Test
    void eachRungPermitsItsOwnGradeOnceARegaliaIsInPossession() {
        Character crafter = crafter(10).build();

        assertNull(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR.itsAllowedToCraftRegalia(crafter));

        crafter.equip(regalia(RegaliaGrade.MENOR));
        assertEquals(RegaliaGrade.MENOR,
                ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR.itsAllowedToCraftRegalia(crafter));
        // A Menor in hand says nothing about the rungs above it.
        assertNull(ArtificeFeat.ARTESAO_DE_REGALIAS_SUPERIORES.itsAllowedToCraftRegalia(crafter));
        assertNull(ArtificeFeat.ARTESAO_DE_REGALIAS_DIVINAS.itsAllowedToCraftRegalia(crafter));
    }

    /** Grades compare by rank: owning a Divina satisfies every rung's own possession condition. */
    @Test
    void aHigherGradeRegaliaSatisfiesTheRungsBelowIt() {
        Character crafter = crafter(10).build();
        crafter.equip(regalia(RegaliaGrade.DIVINA));

        assertEquals(RegaliaGrade.MENOR,
                ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR.itsAllowedToCraftRegalia(crafter));
        assertEquals(RegaliaGrade.SUPERIOR,
                ArtificeFeat.ARTESAO_DE_REGALIAS_SUPERIORES.itsAllowedToCraftRegalia(crafter));
        assertEquals(RegaliaGrade.DIVINA,
                ArtificeFeat.ARTESAO_DE_REGALIAS_DIVINAS.itsAllowedToCraftRegalia(crafter));
    }

    /** Losing the Regalia suspends the permission without touching the Talento. */
    @Test
    void sellingTheRegaliaSuspendsThePermissionButKeepsTheTalento() {
        Character crafter = crafter(7).build();
        crafter.grantFeat(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);
        AbstractItem owned = regalia(RegaliaGrade.MENOR);
        crafter.equip(owned);
        assertEquals(RegaliaGrade.MENOR,
                ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR.itsAllowedToCraftRegalia(crafter));

        crafter.unequip(owned);

        assertTrue(crafter.getFeats().contains(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR));
        assertTrue(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR.isEligible(crafter));
        assertNull(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR.itsAllowedToCraftRegalia(crafter));
    }

    /** Every other Talento in the catalog permits nothing, Regalia in hand or not. */
    @Test
    void noOtherTalentoPermitsForgingARegalia() {
        Character crafter = crafter(10).build();
        crafter.equip(regalia(RegaliaGrade.DIVINA));

        assertNull(ArtesMarciaisFeat.ARTISTA_MARCIAL.itsAllowedToCraftRegalia(crafter));
        assertNull(MobilidadeFeat.VELOCISTA.itsAllowedToCraftRegalia(crafter));
    }
}
