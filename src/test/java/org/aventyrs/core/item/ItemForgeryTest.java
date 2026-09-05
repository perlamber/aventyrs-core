package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.feat.ArtesMarciaisFeat;
import org.aventyrs.core.feat.ArtificeFeat;
import org.aventyrs.core.ability.ItemActiveAbility;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.services.EquipmentCraftingServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.profissao.ProfissaoCompetencyAbility;
import org.aventyrs.core.skill.profissao.ProfissaoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Regalia forge itself — what {@link ItemForgery} refuses, what it produces, and the
 * acquisition-versus-use split it exists to enforce. The ladder's own two halves are pinned in
 * {@code ArtificeFeatTest}; {@code EquipmentCraftingServiceImplTest} covers the service that
 * delegates here.
 */
class ItemForgeryTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static AbstractItem regalia(final RegaliaGrade grade) {
        return AbstractItem.builder().name("Regalia de Teste").category(ItemCategory.RING)
                .regaliaGrade(grade).build();
    }

    /** A JOALHEIRO of graduation holding feats, owning a Regalia of ownedGrade (null for none). */
    private static Character crafter(final int graduation, final RegaliaGrade ownedGrade,
                                     final ArtificeFeat... feats) {
        List<SkillSpecialization> trade = List.of(ProfissaoSpecialization.JOALHERIA);
        CharacterSkill profissao = CharacterSkillFixture.blank(CharacterSkillFixture.PROFISSAO_1)
                .specializations(trade)
                .build();
        profissao.increaseGraduation(graduation);
        Character crafter = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.PROFISSAO, profissao)
                .feats(new ArrayList<>(List.of(feats)))
                .equipment(new ArrayList<>())
                .build();
        if (ownedGrade != null) {
            crafter.equip(regalia(ownedGrade));
        }
        return crafter;
    }

    private static ItemTemplate base() {
        return new ItemTemplate() {
            @Override public String getName() { return "Anel de Prata"; }
            @Override public String getDescription() { return "Um anel liso."; }
            @Override public ItemCategory getCategory() { return ItemCategory.RING; }
            @Override public ItemRarity getRarity() { return ItemRarity.RARE; }
            @Override public ItemWeightClass getWeightClass() { return ItemWeightClass.LIGHT; }
            @Override public int getPrice() { return 100; }
            @Override public int getPhysicalDefenseBonus() { return 0; }
            @Override public int getMagicDefenseBonus() { return 0; }
            @Override public int getHardness() { return 8; }
            @Override public int getCastingBonus() { return 0; }
            @Override public ItemFavor getFavor() { return null; }
        };
    }

    private static ItemForgery forgeryOf(final Character crafter, final RegaliaGrade grade) {
        return ItemForgery.by(crafter, ProfissaoSpecialization.JOALHERIA,
                ItemSpecification.regalia(base(), grade), RegaliaDonation.willingDonor());
    }

    @Test
    void forgingProducesAGradedCopyStampedWithItsMakerAndAdvancesTheHistory()
            throws IllegalOperationException {
        Character crafter = crafter(7, RegaliaGrade.MENOR, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        Item forged = forgeryOf(crafter, RegaliaGrade.MENOR).forge();

        assertTrue(forged.isRegalia());
        assertEquals(RegaliaGrade.MENOR, forged.getRegaliaGrade());
        assertEquals("Anel de Prata", forged.getName());
        assertEquals(crafter.getId(), ((AbstractItem) forged).getProducedByCharacterId());
        assertEquals(1, crafter.getRegaliasCrafted(RegaliaGrade.MENOR));
    }

    /** The whole point of the permission: the Talento is held, the Regalia is not. */
    @Test
    void aTalentoWithoutTheRegaliaInHandPermitsNothing() {
        Character crafter = crafter(7, null, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);
        ItemForgery forgery = forgeryOf(crafter, RegaliaGrade.MENOR);

        assertFalse(forgery.isPermitted());
        assertThrows(IllegalOperationException.class, forgery::forge);

        crafter.equip(regalia(RegaliaGrade.MENOR));

        assertTrue(forgery.isPermitted());
    }

    /** …and the mirror: the Regalia is in hand, the Talento was never acquired. */
    @Test
    void aRegaliaWithoutTheTalentoPermitsNothingEither() {
        Character crafter = crafter(10, RegaliaGrade.DIVINA);

        assertFalse(forgeryOf(crafter, RegaliaGrade.MENOR).isPermitted());
        assertThrows(IllegalOperationException.class, () -> forgeryOf(crafter, RegaliaGrade.MENOR).forge());
    }

    @Test
    void aTalentoOnlyPermitsItsOwnGrade() {
        Character crafter = crafter(10, RegaliaGrade.DIVINA, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        assertTrue(forgeryOf(crafter, RegaliaGrade.MENOR).isPermitted());
        assertFalse(forgeryOf(crafter, RegaliaGrade.SUPERIOR).isPermitted());
        assertFalse(forgeryOf(crafter, RegaliaGrade.DIVINA).isPermitted());
    }

    @Test
    void anUnrelatedTalentoPermitsNothing() {
        Character crafter = crafter(10, RegaliaGrade.DIVINA);
        crafter.grantFeat(ArtesMarciaisFeat.ARTISTA_MARCIAL);

        assertFalse(forgeryOf(crafter, RegaliaGrade.MENOR).isPermitted());
    }

    @Test
    void theTradeMustBeOneTheCrafterActuallyHolds() {
        Character crafter = crafter(7, RegaliaGrade.MENOR, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        assertThrows(IllegalOperationException.class, () -> ItemForgery.by(crafter,
                ProfissaoSpecialization.METALURGIA, ItemSpecification.regalia(base(), RegaliaGrade.MENOR),
                RegaliaDonation.willingDonor()).forge());
    }

    @Test
    void anUnwillingDonorFailsTheCreation() {
        Character crafter = crafter(7, RegaliaGrade.MENOR, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        assertThrows(IllegalOperationException.class, () -> ItemForgery.by(crafter,
                ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(base(), RegaliaGrade.MENOR),
                new RegaliaDonation(false, null)).forge());
    }

    @Test
    void aDivinaDemandsADragonElementalAbyssalOrCelestialDonor() throws IllegalOperationException {
        Character crafter = crafter(10, RegaliaGrade.DIVINA, ArtificeFeat.ARTESAO_DE_REGALIAS_DIVINAS);

        assertThrows(IllegalOperationException.class, () -> forgeryOf(crafter, RegaliaGrade.DIVINA).forge());
        assertThrows(IllegalOperationException.class, () -> ItemForgery.by(crafter,
                ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(base(), RegaliaGrade.DIVINA),
                RegaliaDonation.willingDivineDonor(CreatureType.HUMANOIDE)).forge());

        Item divina = ItemForgery.by(crafter, ProfissaoSpecialization.JOALHERIA,
                ItemSpecification.regalia(base(), RegaliaGrade.DIVINA),
                RegaliaDonation.willingDivineDonor(CreatureType.DRAGAO)).forge();

        assertEquals(RegaliaGrade.DIVINA, divina.getRegaliaGrade());
    }

    /** A Regalia is forged like any other item, so AUMENTAR_A_DUREZA scales it too: 8 -> 12. */
    @Test
    void theCraftersHardnessAbilitiesScaleTheForgedCopy() throws IllegalOperationException {
        List<SkillSpecialization> trade = List.of(ProfissaoSpecialization.JOALHERIA);
        CharacterSkill profissao = CharacterSkillFixture.blank(CharacterSkillFixture.PROFISSAO_1)
                .specializations(trade).build();
        profissao.increaseGraduation(7);
        Character crafter = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.PROFISSAO, profissao)
                .feats(new ArrayList<>(List.of(ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR)))
                .equipment(new ArrayList<>())
                .skillCompetencyAbility(ProfissaoCompetencyAbility.AUMENTAR_A_DUREZA)
                .build();
        crafter.equip(regalia(RegaliaGrade.MENOR));

        Item forged = forgeryOf(crafter, RegaliaGrade.MENOR).forge();

        assertEquals(12, forged.getEffectiveHardness());
    }

    /** Building a forgery mutates nothing; only {@code forge()} does. */
    @Test
    void describingAForgeChangesNothingUntilItIsForged() throws IllegalOperationException {
        Character crafter = crafter(7, RegaliaGrade.MENOR, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);
        ItemForgery forgery = forgeryOf(crafter, RegaliaGrade.MENOR);

        forgery.validate();
        assertEquals(0, crafter.getRegaliasCrafted(RegaliaGrade.MENOR));

        forgery.forge();
        assertEquals(1, crafter.getRegaliasCrafted(RegaliaGrade.MENOR));
    }

    // ------------------------------------------------------- the specification, beyond the base

    @Test
    void aSpecificationsMasterpieceAndImprovementsReachTheForgedCopy() throws IllegalOperationException {
        Character crafter = crafter(7, null);
        ItemSpecification specification = ItemSpecification.builder()
                .base(ArmorItem.ARMADURA_COMPLETA)
                .masterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA))
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .improvement(ItemImprovement.of(DefensiveImprovement.AJUSTADA))
                .build();

        Item forged = ItemForgery.by(crafter, ProfissaoSpecialization.JOALHERIA, specification).forge();

        assertSame(DefensiveMasterpiece.REFORCADA,
                ((ItemMasterpiece) forged.getMasterpiece()).getDefinition());
        assertEquals(2, forged.getImprovements().size());
        assertNull(forged.getRegaliaGrade());
    }

    @Test
    void anAprimoramentoNeedsAnObraPrimaToHostIt() {
        Character crafter = crafter(7, null);
        ItemSpecification noMasterpiece = ItemSpecification.builder()
                .base(ArmorItem.ARMADURA_COMPLETA)
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .build();

        assertThrows(IllegalOperationException.class,
                () -> ItemForgery.by(crafter, ProfissaoSpecialization.JOALHERIA, noMasterpiece).forge());
    }

    /** A Leve armor holds one Aprimoramento; asking for two is refused before any is fitted. */
    @Test
    void theWeightClassCapsHowManyAprimoramentosOneForgeMayFit() {
        Character crafter = crafter(7, null);
        ItemSpecification overloaded = ItemSpecification.builder()
                .base(ArmorItem.ROUPA_PESADA)
                .masterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA))
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .improvement(ItemImprovement.of(DefensiveImprovement.AJUSTADA))
                .build();

        assertEquals(1, ArmorItem.ROUPA_PESADA.getWeightClass().getMaximumImprovements());
        assertThrows(IllegalOperationException.class,
                () -> ItemForgery.by(crafter, ProfissaoSpecialization.JOALHERIA, overloaded).forge());
    }

    @Test
    void theSameAprimoramentoCannotBeFittedTwice() {
        Character crafter = crafter(7, null);
        ItemSpecification repeated = ItemSpecification.builder()
                .base(ArmorItem.ARMADURA_COMPLETA)
                .masterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA))
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .build();

        assertThrows(IllegalOperationException.class,
                () -> ItemForgery.by(crafter, ProfissaoSpecialization.JOALHERIA, repeated).forge());
    }

    /** The Obra-Prima's Raridade sets the Profissão Graduação its maker needs: Rara asks for 5. */
    @Test
    void anObraPrimaBeyondTheCraftersGraduationIsRefused() throws IllegalOperationException {
        ItemSpecification rare = ItemSpecification.builder()
                .base(ArmorItem.ARMADURA_COMPLETA)
                .masterpiece(ItemMasterpiece.of(DefensiveMasterpiece.BANHADA_EM_OURO))
                .build();

        assertThrows(IllegalOperationException.class, () -> ItemForgery.by(crafter(4, null),
                ProfissaoSpecialization.JOALHERIA, rare).forge());

        Item forged = ItemForgery.by(crafter(5, null), ProfissaoSpecialization.JOALHERIA, rare).forge();
        assertSame(DefensiveMasterpiece.BANHADA_EM_OURO,
                ((ItemMasterpiece) forged.getMasterpiece()).getDefinition());
    }

    /** An ordinary item needs no permission and no Centelha donor at all. */
    @Test
    void anOrdinaryForgeNeedsNeitherPermissionNorDonation() throws IllegalOperationException {
        Character crafter = crafter(1, null);
        ItemForgery forgery = ItemForgery.by(crafter, ProfissaoSpecialization.JOALHERIA,
                ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA));

        assertTrue(forgery.isPermitted());
        assertFalse(forgery.forge().isRegalia());
    }

    // ------------------------------------------------------------------- Aventyr's own donations

    @Test
    void aDonatedRegaliaSkipsEveryGateAndIsMarkedAsAventyrs() throws IllegalOperationException {
        ItemForgery donation = ItemForgery.donatedByAventyr(
                ItemSpecification.regalia(base(), RegaliaGrade.DIVINA));

        assertTrue(donation.isDonation());
        assertTrue(donation.isPermitted());

        Item gift = donation.forge();

        assertTrue(gift.isDonatedByAventyr());
        assertEquals(RegaliaGrade.DIVINA, gift.getRegaliaGrade());
        assertNull(((AbstractItem) gift).getProducedByCharacterId());
    }

    /** No crafter means nobody's Regalia history moves and nobody's abilities scale it. */
    @Test
    void aDonationAdvancesNoHistoryAndIsScaledByNobody() throws IllegalOperationException {
        Character wouldBeCrafter = crafter(10, RegaliaGrade.DIVINA, ArtificeFeat.ARTESAO_DE_REGALIAS_DIVINAS);

        Item gift = ItemForgery.donatedByAventyr(
                ItemSpecification.regalia(base(), RegaliaGrade.DIVINA)).forge();

        assertEquals(0, wouldBeCrafter.getRegaliasCrafted(RegaliaGrade.DIVINA));
        assertEquals(8, gift.getEffectiveHardness());
    }

    /** A donation carries its Obra-Prima and Aprimoramentos too — and skips their gates as well. */
    @Test
    void aDonationStillCarriesWhateverTheSpecificationDescribes() throws IllegalOperationException {
        Item gift = ItemForgery.donatedByAventyr(ItemSpecification.builder()
                .base(ArmorItem.ROUPA_PESADA)
                .masterpiece(ItemMasterpiece.magistral(DefenseType.PHYSICAL))
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .improvement(ItemImprovement.of(DefensiveImprovement.AJUSTADA))
                .regaliaGrade(RegaliaGrade.MENOR)
                .build()).forge();

        // Two Aprimoramentos on a Leve armor: refused for a crafter, waved through for Aventyr.
        assertEquals(2, gift.getImprovements().size());
        assertTrue(gift.isRegalia());
        assertTrue(gift.isDonatedByAventyr());
    }

    /** A forged copy is nobody's gift — the marker is not the default. */
    @Test
    void aCraftedCopyIsNotMarkedAsDonated() throws IllegalOperationException {
        Character crafter = crafter(7, RegaliaGrade.MENOR, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        Item forged = forgeryOf(crafter, RegaliaGrade.MENOR).forge();

        assertFalse(forged.isDonatedByAventyr());
        assertFalse(ArmorItem.ARMADURA_COMPLETA.isDonatedByAventyr());
    }

    // ------------------------------------------------- assembling the piece, and what it costs

    @Test
    void aBareForgeIsWorthTheBasePrecoAndCostsHalfOfIt() {
        ItemForgery forgery = ItemForgery.by(crafter(7, null), ProfissaoSpecialization.JOALHERIA,
                ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA));

        assertEquals(ArmorItem.ARMADURA_COMPLETA.getPrice(), forgery.getTotalValue());
        assertEquals(ArmorItem.ARMADURA_COMPLETA.getPrice() / 2, forgery.getForgingCost());
    }

    /**
     * Each decision is its own commissioned work and re-totals the piece. Every catalog Preço
     * modifier is 0 today, so what this pins is the arithmetic and the recompute, not a figure
     * that moves — see {@link ItemForgery}'s own javadoc.
     */
    @Test
    void eachDecisionRePricesThePieceFromTheWholeSpecification() {
        ItemForgery forgery = ItemForgery.by(crafter(7, null), ProfissaoSpecialization.JOALHERIA,
                ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA));
        int base = ArmorItem.ARMADURA_COMPLETA.getPrice();

        forgery.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA))
                .addImprovement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .addImprovement(ItemImprovement.of(DefensiveImprovement.AJUSTADA));

        int expected = base
                + DefensiveMasterpiece.REFORCADA.getPriceModifier()
                + DefensiveImprovement.RESISTENTE.getPriceModifier()
                + DefensiveImprovement.AJUSTADA.getPriceModifier();
        assertEquals(expected, forgery.getTotalValue());
        assertEquals(expected / 2, forgery.getForgingCost());
    }

    /** The total is recomputed, never accumulated: dropping the Obra-Prima drops its price too. */
    @Test
    void clearingADecisionGivesItsPriceBack() {
        ItemForgery forgery = ItemForgery.by(crafter(7, null), ProfissaoSpecialization.JOALHERIA,
                ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA));
        int bare = forgery.getTotalValue();

        forgery.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.BANHADA_EM_OURO));
        forgery.setMasterpiece(null);

        assertEquals(bare, forgery.getTotalValue());
        assertNull(forgery.getSpecification().getMasterpiece());
    }

    /** A specification handed in already assembled is priced the same as one built up by hand. */
    @Test
    void aPreAssembledSpecificationIsPricedOnConstruction() {
        ItemSpecification assembled = ItemSpecification.builder()
                .base(ArmorItem.ARMADURA_COMPLETA)
                .masterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA))
                .improvement(ItemImprovement.of(DefensiveImprovement.RESISTENTE))
                .build();

        ItemForgery byHand = ItemForgery.by(crafter(7, null), ProfissaoSpecialization.JOALHERIA,
                        ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA))
                .setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA))
                .addImprovement(ItemImprovement.of(DefensiveImprovement.RESISTENTE));

        assertEquals(byHand.getTotalValue(), ItemForgery.by(crafter(7, null),
                ProfissaoSpecialization.JOALHERIA, assembled).getTotalValue());
    }

    /** Half of the summed total, once — never a halving per part, and never below 1. */
    @Test
    void theCostIsHalvedOnceOverTheWholeTotalAndNeverReachesZero() {
        ItemTemplate freeItem = new ItemTemplate() {
            @Override public String getName() { return "Galho"; }
            @Override public String getDescription() { return "Um galho seco."; }
            @Override public ItemCategory getCategory() { return ItemCategory.CLUB; }
            @Override public ItemRarity getRarity() { return ItemRarity.COMMON; }
            @Override public ItemWeightClass getWeightClass() { return ItemWeightClass.LIGHT; }
            @Override public int getPrice() { return 0; }
            @Override public int getPhysicalDefenseBonus() { return 0; }
            @Override public int getMagicDefenseBonus() { return 0; }
            @Override public int getHardness() { return 1; }
            @Override public int getCastingBonus() { return 0; }
            @Override public ItemFavor getFavor() { return null; }
        };

        ItemForgery forgery = ItemForgery.by(crafter(7, null), ProfissaoSpecialization.JOALHERIA,
                ItemSpecification.of(freeItem));

        assertEquals(0, forgery.getTotalValue());
        assertEquals(1, forgery.getForgingCost());
    }

    /** Aventyr charges nothing — though what it gives is still worth what it is worth. */
    @Test
    void aDonationCostsNothingWhileStillBeingWorthSomething() {
        ItemForgery donation = ItemForgery.donatedByAventyr(
                ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA));

        assertEquals(ArmorItem.ARMADURA_COMPLETA.getPrice(), donation.getTotalValue());
        assertEquals(0, donation.getForgingCost());
    }

    /** A bare template's forging cost agrees with the service's own "metade do Preço". */
    @Test
    void theForgingCostAgreesWithTheServicesFabricationCost() {
        ItemForgery forgery = ItemForgery.by(crafter(7, null), ProfissaoSpecialization.JOALHERIA,
                ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA));

        assertEquals(new EquipmentCraftingServiceImpl().getFabricationCost(ArmorItem.ARMADURA_COMPLETA),
                forgery.getForgingCost());
    }

    // --------------------------------------------------------------- an item ability on a Regalia

    @Test
    void anActiveAbilityBoundByTheForgeReachesTheRegalia() throws IllegalOperationException {
        Character crafter = crafter(7, RegaliaGrade.MENOR, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);
        ItemActiveAbility ability = testAbility();

        Item forged = forgeryOf(crafter, RegaliaGrade.MENOR).setActiveAbility(ability).forge();

        assertSame(ability, forged.getActiveAbility());
        assertTrue(forged.isRegalia());
    }

    /** Only a Regalia can host one — asked for on an ordinary item, the forge refuses. */
    @Test
    void anActiveAbilityOnAnOrdinaryItemIsRefused() {
        ItemForgery forgery = ItemForgery.by(crafter(7, null), ProfissaoSpecialization.JOALHERIA,
                        ItemSpecification.of(ArmorItem.ARMADURA_COMPLETA))
                .setActiveAbility(testAbility());

        assertThrows(IllegalOperationException.class, forgery::forge);
    }

    private static ItemActiveAbility testAbility() {
        return new ItemActiveAbility() {
            @Override public String getDescription() { return "Uma habilidade de teste."; }
            @Override public int getActionPointCost() { return 1; }
            @Override public int getMagicPointCost() { return 0; }
            @Override public int getDurationInRounds() { return 1; }
            @Override public TemporaryEffect resolveEffect(final Character character) {
                return new TemporaryBonus(ModifierType.PHYSICAL_DEFENSE, 1, 1);
            }
        };
    }
}
