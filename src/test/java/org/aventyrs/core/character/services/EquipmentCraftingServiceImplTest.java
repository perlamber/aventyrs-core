package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.character.services.EquipmentCraftingService.RepairAssessment;
import org.aventyrs.core.feat.ArtificeFeat;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.item.DefensiveImprovement;
import org.aventyrs.core.item.DefensiveMasterpiece;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemFavor;
import org.aventyrs.core.item.ItemImprovement;
import org.aventyrs.core.item.ItemMasterpiece;
import org.aventyrs.core.item.ItemRarity;
import org.aventyrs.core.item.ItemSpecification;
import org.aventyrs.core.item.ItemTemplate;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.RegaliaDonation;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.profissao.ProfissaoCompetencyAbility;
import org.aventyrs.core.skill.profissao.ProfissaoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentCraftingServiceImplTest {

    private final EquipmentCraftingService service = new EquipmentCraftingServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    // ------------------------------------------------------------------ helpers

    /** A minimal catalog blueprint, so a test can vary Preço / Dureza / Raridade freely. */
    private static ItemTemplate template(final int price, final int hardness, final ItemRarity rarity,
                                         final ItemWeightClass weightClass) {
        return new ItemTemplate() {
            @Override public String getName() { return "Espada Longa"; }
            @Override public String getDescription() { return ""; }
            @Override public ItemCategory getCategory() { return ItemCategory.HEAVY_BLADE; }
            @Override public ItemRarity getRarity() { return rarity; }
            @Override public ItemWeightClass getWeightClass() { return weightClass; }
            @Override public int getPrice() { return price; }
            @Override public int getPhysicalDefenseBonus() { return 0; }
            @Override public int getMagicDefenseBonus() { return 0; }
            @Override public int getHardness() { return hardness; }
            @Override public int getCastingBonus() { return 0; }
            @Override public ItemFavor getFavor() { return null; }
        };
    }

    private Character crafter(final int profissaoGraduation, final ProfissaoSpecialization trade,
                              final ProfissaoCompetencyAbility... abilities) {
        List<SkillSpecialization> specializations = trade == null ? List.of() : List.of(trade);
        CharacterSkill profissao = CharacterSkillFixture.blank(CharacterSkillFixture.PROFISSAO_1)
                .specializations(specializations)
                .build();
        profissao.increaseGraduation(profissaoGraduation);
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.PROFISSAO, profissao);
        for (ProfissaoCompetencyAbility ability : abilities) {
            builder.skillCompetencyAbility(ability);
        }
        return builder.build();
    }

    private static AbstractItem damagedArmor(final int hardness, final int damage) {
        AbstractItem armor = AbstractItem.builder()
                .name("Armadura de Justa Banhada em Ouro")
                .category(ItemCategory.ARMOR)
                .rarity(ItemRarity.EPIC)
                .weightClass(ItemWeightClass.HEAVY)
                .price(44)
                .hardness(hardness)
                .build();
        armor.setDamageTaken(damage);
        return armor;
    }

    // ------------------------------------------------------------------ fabrication figures

    @Test
    void fabricationCostsAndTakesHalfThePriceInPeAndDaysFlooredAtOne() {
        ItemTemplate espadaLonga = template(7, 24, ItemRarity.COMMON, ItemWeightClass.MEDIUM);

        assertEquals(3, service.getFabricationCost(espadaLonga));
        assertEquals(3, service.getFabricationTimeInDays(espadaLonga));
        assertEquals(1, service.getFabricationCost(template(1, 4, ItemRarity.COMMON, ItemWeightClass.LIGHT)));
    }

    @Test
    void fabricationDifficultyClimbsWithRarity() {
        assertEquals(DifficultyLevel.MEDIUM, service.getFabricationDifficulty(template(4, 4, ItemRarity.COMMON, ItemWeightClass.LIGHT)));
        assertEquals(DifficultyLevel.UNLIKELY, service.getFabricationDifficulty(template(4, 4, ItemRarity.EPIC, ItemWeightClass.LIGHT)));
        assertEquals(DifficultyLevel.UNIMAGINABLE, service.getFabricationDifficulty(template(4, 4, ItemRarity.MYTHIC, ItemWeightClass.LIGHT)));
    }

    @Test
    void masterpieceFabricationIsOneNivelHarderAndGatedOnGraduation() {
        assertEquals(DifficultyLevel.HARD, service.getMasterpieceFabricationDifficulty(ItemRarity.COMMON));
        assertEquals(DifficultyLevel.MIRACLE, service.getMasterpieceFabricationDifficulty(ItemRarity.MYTHIC));
        assertEquals(1, service.getMasterpieceMinimumGraduation(ItemRarity.COMMON));
        assertEquals(3, service.getMasterpieceMinimumGraduation(ItemRarity.UNCOMMON));
        assertEquals(10, service.getMasterpieceMinimumGraduation(ItemRarity.MYTHIC));
    }

    @Test
    void construtorEficienteShavesTwentyPercentOffTheProductionTime() {
        ItemTemplate espadaLonga = template(7, 24, ItemRarity.COMMON, ItemWeightClass.MEDIUM);
        Character efficient = crafter(3, ProfissaoSpecialization.METALURGIA,
                ProfissaoCompetencyAbility.CONSTRUTOR_EFICIENTE);

        assertEquals(3, service.getFabricationTimeInDays(espadaLonga));
        assertEquals(2, service.getFabricationTimeInDays(efficient, espadaLonga)); // floor(3 * 0.8) = 2
    }

    // ------------------------------------------------------------------ forge

    @Test
    void forgeStampsTheProducerAndReturnsAnUnequippedCopy() throws IllegalOperationException {
        Character smith = crafter(3, ProfissaoSpecialization.METALURGIA);
        ItemTemplate espadaLonga = template(7, 24, ItemRarity.COMMON, ItemWeightClass.MEDIUM);

        Item forged = service.forge(smith, ProfissaoSpecialization.METALURGIA, espadaLonga, null);

        assertNotNull(forged);
        assertEquals(24, forged.getHardness());
        assertEquals(smith.getId(), ((AbstractItem) forged).getProducedByCharacterId());
        assertFalse(smith.getEquipment().contains(forged));
    }

    @Test
    void aumentarADurezaScalesTheForgedCopysDurezaByHalf() throws IllegalOperationException {
        Character smith = crafter(3, ProfissaoSpecialization.METALURGIA,
                ProfissaoCompetencyAbility.AUMENTAR_A_DUREZA);
        ItemTemplate espadaLonga = template(7, 24, ItemRarity.COMMON, ItemWeightClass.MEDIUM);

        Item forged = service.forge(smith, ProfissaoSpecialization.METALURGIA, espadaLonga, null);

        assertEquals(36, forged.getHardness()); // floor(24 * 1.5)
    }

    @Test
    void forgeRefusesACrafterWithoutTheTrade() {
        Character wrongTrade = crafter(3, ProfissaoSpecialization.JOALHERIA);
        ItemTemplate espadaLonga = template(7, 24, ItemRarity.COMMON, ItemWeightClass.MEDIUM);

        assertThrows(IllegalOperationException.class,
                () -> service.forge(wrongTrade, ProfissaoSpecialization.METALURGIA, espadaLonga, null));
    }

    @Test
    void forgeRefusesAnObraPrimaBelowTheGraduationFloor() {
        Character novice = crafter(2, ProfissaoSpecialization.METALURGIA); // UNCOMMON needs 3

        assertThrows(IllegalOperationException.class,
                () -> service.forge(novice, ProfissaoSpecialization.METALURGIA,
                        ArmorItem.ARMADURA_COMPLETA, uncommonMasterpiece()));
    }

    @Test
    void forgeFitsTheObraPrimaWhenTheCrafterQualifies() throws IllegalOperationException {
        Character master = crafter(5, ProfissaoSpecialization.METALURGIA);

        Item forged = service.forge(master, ProfissaoSpecialization.METALURGIA,
                ArmorItem.ARMADURA_COMPLETA, uncommonMasterpiece());

        assertNotNull(forged.getMasterpiece());
    }

    private static ItemMasterpiece uncommonMasterpiece() {
        return ItemMasterpiece.of(DefensiveMasterpiece.EQUILIBRADA); // ItemRarity.UNCOMMON
    }

    // ------------------------------------------------------------------ aprimoramento install

    @Test
    void improvementInstallDifficultyAndCumulativeDisadvantage() throws IllegalOperationException {
        assertEquals(DifficultyLevel.HARD, service.getImprovementInstallDifficulty(ItemRarity.COMMON));
        assertEquals(DifficultyLevel.MIRACLE, service.getImprovementInstallDifficulty(ItemRarity.MYTHIC));

        Character master = crafter(5, ProfissaoSpecialization.METALURGIA);
        Item mediumMasterpiece = service.forge(master, ProfissaoSpecialization.METALURGIA,
                ArmorItem.ARMADURA_COMPLETA, uncommonMasterpiece());

        assertEquals(0, service.getImprovementInstallDisadvantage(mediumMasterpiece));
        service.installImprovement(master, mediumMasterpiece, ItemImprovement.of(DefensiveImprovement.RESISTENTE));
        assertEquals(-2, service.getImprovementInstallDisadvantage(mediumMasterpiece));
    }

    @Test
    void installImprovementRespectsTheWeightClassSlotCapAndRejectsDuplicates() throws IllegalOperationException {
        Character master = crafter(5, ProfissaoSpecialization.METALURGIA);
        // ARMADURA_COMPLETA is HEAVY -> 3 slots.
        Item heavyMasterpiece = service.forge(master, ProfissaoSpecialization.METALURGIA,
                ArmorItem.ARMADURA_COMPLETA, uncommonMasterpiece());

        service.installImprovement(master, heavyMasterpiece, ItemImprovement.of(DefensiveImprovement.RESISTENTE));
        assertThrows(IllegalOperationException.class, () -> service.installImprovement(
                master, heavyMasterpiece, ItemImprovement.of(DefensiveImprovement.RESISTENTE))); // duplicate

        service.installImprovement(master, heavyMasterpiece, ItemImprovement.of(DefensiveImprovement.AJUSTADA));
        service.installImprovement(master, heavyMasterpiece, ItemImprovement.of(DefensiveImprovement.ESPINHOSA));
        assertEquals(3, heavyMasterpiece.getImprovements().size());
        assertThrows(IllegalOperationException.class, () -> service.installImprovement(
                master, heavyMasterpiece, ItemImprovement.of(DefensiveImprovement.OCULTA))); // slots full
    }

    @Test
    void installImprovementRefusesANonMasterpiece() throws IllegalOperationException {
        Character master = crafter(5, ProfissaoSpecialization.METALURGIA);
        Item ordinary = service.forge(master, ProfissaoSpecialization.METALURGIA, ArmorItem.ARMADURA_COMPLETA, null);

        assertThrows(IllegalOperationException.class, () -> service.installImprovement(
                master, ordinary, ItemImprovement.of(DefensiveImprovement.RESISTENTE)));
    }

    // ------------------------------------------------------------------ repair

    @Test
    void assessRepairPricesLightDamageAtATenthAndSevereDamageAtAThird() {
        // Armadura de Justa Banhada em Ouro, 44 PE, Dureza 35.
        assertEquals(4, service.assessRepair(damagedArmor(35, 10)).equipmentPointCost());   // 44/10 floored
        assertEquals(14, service.assessRepair(damagedArmor(35, 30)).equipmentPointCost());  // 44/3 floored
    }

    @Test
    void assessRepairChargesOneHourPerPvLightAndTwoHoursPerPvSevere() {
        assertEquals(9, service.assessRepair(damagedArmor(40, 9)).workHours());     // under half -> 1h each
        assertEquals(36, service.assessRepair(damagedArmor(20, 18)).workHours());   // 18 of 20 -> severe, 2h each
    }

    @Test
    void assessRepairReadsItsGdFromRarityAndTheHarderTableForAnObraPrima() {
        AbstractItem plain = damagedArmor(35, 10);
        assertEquals(DifficultyLevel.HARD, service.assessRepair(plain).difficulty()); // EPIC ordinary

        AbstractItem masterwork = damagedArmor(35, 10);
        masterwork.setMasterpiece(uncommonMasterpiece());
        assertEquals(DifficultyLevel.VERY_HARD, service.assessRepair(masterwork).difficulty()); // EPIC masterpiece
    }

    @Test
    void anUndamagedItemNeedsNoRepair() {
        RepairAssessment none = service.assessRepair(damagedArmor(35, 0));
        assertEquals(0, none.equipmentPointCost());
        assertEquals(0, none.workHours());
        assertFalse(none.severelyDamaged());
    }

    @Test
    void repairReducesDamageAndReparoMelhoradoAddsToTheRecovery() throws IllegalOperationException {
        Character mender = crafter(3, ProfissaoSpecialization.METALURGIA,
                ProfissaoCompetencyAbility.REPARO_MELHORADO);
        AbstractItem armor = damagedArmor(35, 20);

        int recovered = service.repair(mender, ProfissaoSpecialization.METALURGIA, armor, 10);

        assertEquals(12, recovered);           // 10 requested + REPARO_MELHORADO's +2
        assertEquals(8, armor.getDamageTaken());
    }

    @Test
    void reparoMelhoradoBecomesPlusFiveAtTenGraduacoes() throws IllegalOperationException {
        Character grandmaster = crafter(10, ProfissaoSpecialization.METALURGIA,
                ProfissaoCompetencyAbility.REPARO_MELHORADO);
        AbstractItem armor = damagedArmor(35, 20);

        assertEquals(15, service.repair(grandmaster, ProfissaoSpecialization.METALURGIA, armor, 10));
    }

    @Test
    void repairNeverRestoresMoreThanTheDamageCarried() throws IllegalOperationException {
        Character mender = crafter(3, ProfissaoSpecialization.METALURGIA,
                ProfissaoCompetencyAbility.REPARO_MELHORADO);
        AbstractItem armor = damagedArmor(35, 3);

        assertEquals(3, service.repair(mender, ProfissaoSpecialization.METALURGIA, armor, 10));
        assertEquals(0, armor.getDamageTaken());
    }

    @Test
    void repairRefusesARepairerWithoutTheTrade() {
        Character wrongTrade = crafter(3, ProfissaoSpecialization.ALFAIATARIA_E_CURTUME);
        AbstractItem armor = damagedArmor(35, 10);

        assertThrows(IllegalOperationException.class,
                () -> service.repair(wrongTrade, ProfissaoSpecialization.METALURGIA, armor, 5));
    }

    @Test
    void aTemplateCannotBeRepairedOrImprovedInPlace() {
        Character master = crafter(5, ProfissaoSpecialization.METALURGIA);

        assertThrows(IllegalOperationException.class, () -> service.repair(
                master, ProfissaoSpecialization.METALURGIA, ArmorItem.ARMADURA_COMPLETA, 1));
        assertThrows(IllegalOperationException.class, () -> service.installImprovement(
                master, ArmorItem.ARMADURA_COMPLETA, ItemImprovement.of(DefensiveImprovement.RESISTENTE)));
    }

    @Test
    void multipleAprimoramentosStackInTheItemAggregation() throws IllegalOperationException {
        Character master = crafter(5, ProfissaoSpecialization.METALURGIA);
        Item armor = service.forge(master, ProfissaoSpecialization.METALURGIA,
                ArmorItem.ARMADURA_COMPLETA, uncommonMasterpiece());

        int baseHardness = armor.getEffectiveHardness();
        service.installImprovement(master, armor, ItemImprovement.of(DefensiveImprovement.RESISTENTE)); // +10 PV
        assertEquals(baseHardness + 10, armor.getEffectiveHardness());
    }

    // ------------------------------------------------------------------ Regalia (Talentos de Artífice)

    /**
     * A crafter holding the JOALHERIA trade, {@code feats}, and — since possessing a Regalia is a
     * condition of <i>using</i> any {@code ArtificeFeat} — a Regalia Divina, which satisfies every
     * rung's own possession condition. {@link #regaliaCrafterOwningNoRegalia} is the counterpart
     * with an empty pack.
     */
    private Character regaliaCrafter(final int graduation, final ArtificeFeat... feats) {
        Character crafter = regaliaCrafterOwningNoRegalia(graduation, feats);
        crafter.equip(AbstractItem.builder().name("Regalia de Teste").category(ItemCategory.RING)
                .regaliaGrade(RegaliaGrade.DIVINA).build());
        return crafter;
    }

    private Character regaliaCrafterOwningNoRegalia(final int graduation, final ArtificeFeat... feats) {
        List<SkillSpecialization> trade = List.of(ProfissaoSpecialization.JOALHERIA);
        CharacterSkill profissao = CharacterSkillFixture.blank(CharacterSkillFixture.PROFISSAO_1)
                .specializations(trade)
                .build();
        profissao.increaseGraduation(graduation);
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.PROFISSAO, profissao)
                .feats(new ArrayList<>(List.of(feats)))
                .equipment(new ArrayList<>())
                .build();
    }

    private static ItemTemplate regaliaBase() {
        return new ItemTemplate() {
            @Override public String getName() { return "Anel de Prata"; }
            @Override public String getDescription() { return ""; }
            @Override public ItemCategory getCategory() { return ItemCategory.RING; }
            @Override public ItemRarity getRarity() { return ItemRarity.RARE; }
            @Override public ItemWeightClass getWeightClass() { return ItemWeightClass.LIGHT; }
            @Override public int getPrice() { return 12; }
            @Override public int getPhysicalDefenseBonus() { return 0; }
            @Override public int getMagicDefenseBonus() { return 0; }
            @Override public int getHardness() { return 8; }
            @Override public int getCastingBonus() { return 0; }
            @Override public ItemFavor getFavor() { return null; }
        };
    }

    @Test
    void regaliaCraftingFiguresComeStraightFromTheGrade() {
        assertEquals(DifficultyLevel.UNIMAGINABLE, service.getRegaliaCraftingDifficulty(RegaliaGrade.MENOR));
        assertEquals(DifficultyLevel.MIRACLE, service.getRegaliaCraftingDifficulty(RegaliaGrade.SUPERIOR));
        assertEquals(90, service.getRegaliaCraftingTimeInDays(RegaliaGrade.MENOR));
        assertEquals(180, service.getRegaliaCraftingTimeInDays(RegaliaGrade.DIVINA));
        assertFalse(service.regaliaCraftingRequiresCriticalResult(RegaliaGrade.SUPERIOR));
        assertTrue(service.regaliaCraftingRequiresCriticalResult(RegaliaGrade.DIVINA));
    }

    @Test
    void construtorEficienteShortensTheRegaliaCraftingTime() {
        List<SkillSpecialization> trade = List.of(ProfissaoSpecialization.JOALHERIA);
        CharacterSkill profissao = CharacterSkillFixture.blank(CharacterSkillFixture.PROFISSAO_1)
                .specializations(trade).build();
        profissao.increaseGraduation(10);
        Character efficient = CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.PROFISSAO, profissao)
                .skillCompetencyAbility(ProfissaoCompetencyAbility.CONSTRUTOR_EFICIENTE)
                .build();

        // CONSTRUTOR_EFICIENTE is a -20% production-time multiplier: 90 -> 72.
        assertEquals(72, service.getRegaliaCraftingTimeInDays(efficient, RegaliaGrade.MENOR));
    }

    @Test
    void forgeRegaliaMarksTheCopyWithItsGradeStampsTheMakerAndAdvancesHistory() throws IllegalOperationException {
        Character crafter = regaliaCrafter(7, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        Item regalia = service.forgeRegalia(crafter, ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(regaliaBase(), RegaliaGrade.MENOR), RegaliaDonation.willingDonor());

        assertTrue(regalia.isRegalia());
        assertEquals(RegaliaGrade.MENOR, regalia.getRegaliaGrade());
        assertEquals(crafter.getId(), ((AbstractItem) regalia).getProducedByCharacterId());
        assertEquals(1, crafter.getRegaliasCrafted(RegaliaGrade.MENOR));
    }

    /**
     * Holding the Talento is only half of it — "a Regalia em sua posse" is a condition of use, so
     * a crafter who owns none is refused even though they legitimately acquired the Talento.
     */
    @Test
    void forgeRegaliaRefusesACrafterHoldingTheTalentoButNoRegalia() {
        Character crafter = regaliaCrafterOwningNoRegalia(7, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        assertThrows(IllegalOperationException.class, () -> service.forgeRegalia(crafter,
                ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(regaliaBase(), RegaliaGrade.MENOR),
                RegaliaDonation.willingDonor()));
    }

    @Test
    void forgeRegaliaRefusesACrafterWithoutTheMatchingArtificeTalento() {
        Character noFeat = regaliaCrafter(10);
        Character wrongRung = regaliaCrafter(10, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        assertThrows(IllegalOperationException.class, () -> service.forgeRegalia(noFeat,
                ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(regaliaBase(), RegaliaGrade.MENOR), RegaliaDonation.willingDonor()));
        // Holds Menor, tries Superior.
        assertThrows(IllegalOperationException.class, () -> service.forgeRegalia(wrongRung,
                ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(regaliaBase(), RegaliaGrade.SUPERIOR), RegaliaDonation.willingDonor()));
    }

    @Test
    void forgeRegaliaRefusesAnUnwillingDonorAndAWrongTrade() {
        Character crafter = regaliaCrafter(7, ArtificeFeat.ARTESAO_DE_REGALIAS_MENOR);

        assertThrows(IllegalOperationException.class, () -> service.forgeRegalia(crafter,
                ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(regaliaBase(), RegaliaGrade.MENOR),
                new RegaliaDonation(false, null)));
        assertThrows(IllegalOperationException.class, () -> service.forgeRegalia(crafter,
                ProfissaoSpecialization.METALURGIA, ItemSpecification.regalia(regaliaBase(), RegaliaGrade.MENOR), RegaliaDonation.willingDonor()));
    }

    @Test
    void forgeRegaliaDivinaRequiresADragonElementalAbyssalOrCelestialDonor() throws IllegalOperationException {
        Character crafter = regaliaCrafter(10, ArtificeFeat.ARTESAO_DE_REGALIAS_DIVINAS);

        assertThrows(IllegalOperationException.class, () -> service.forgeRegalia(crafter,
                ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(regaliaBase(), RegaliaGrade.DIVINA), RegaliaDonation.willingDonor()));
        assertThrows(IllegalOperationException.class, () -> service.forgeRegalia(crafter,
                ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(regaliaBase(), RegaliaGrade.DIVINA),
                RegaliaDonation.willingDivineDonor(CreatureType.HUMANOIDE)));

        Item divina = service.forgeRegalia(crafter, ProfissaoSpecialization.JOALHERIA, ItemSpecification.regalia(regaliaBase(), RegaliaGrade.DIVINA), RegaliaDonation.willingDivineDonor(CreatureType.DRAGAO));
        assertEquals(RegaliaGrade.DIVINA, divina.getRegaliaGrade());
    }
}
