package org.aventyrs.core.item;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.furtividade.FurtividadeInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefensiveImprovementTest {

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void bencaoDeProtecaoUpgradesDuringItsInitialWindowAndKeepsTheExtensionInItsSceneOnly() {
        AbstractItem protectedItem = defensiveItem(ItemCategory.ARMOR,
                ItemImprovement.of(DefensiveImprovement.BENCAO_DE_PROTECAO));
        CharacterSheet sheet = CharacterSheet.of(characterWith(protectedItem), new Player());
        UUID sceneId = UUID.randomUUID();

        assertEquals(1, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, combatContext(0, sceneId)));
        assertEquals(3, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, combatContext(1, sceneId)));

        damageService.applyDamage(sheet, combatContext(1, sceneId), 1, false);

        assertEquals(5, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, combatContext(1, sceneId)));
        assertEquals(5, defenseService.getTotalDefense(sheet, DefenseType.MAGIC, combatContext(5, sceneId)));
        assertEquals(1, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, combatContext(6, sceneId)));
        assertEquals(3, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, combatContext(1, UUID.randomUUID())));
    }

    @Test
    void bencaoDeProtecaoCannotBeExtendedAfterItsInitialWindow() {
        AbstractItem protectedItem = defensiveItem(ItemCategory.ARMOR,
                ItemImprovement.of(DefensiveImprovement.BENCAO_DE_PROTECAO));
        CharacterSheet sheet = CharacterSheet.of(characterWith(protectedItem), new Player());
        UUID sceneId = UUID.randomUUID();

        damageService.applyDamage(sheet, combatContext(4, sceneId), 1, false);

        assertEquals(1, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, combatContext(5, sceneId)));
    }

    @Test
    void catalogsEveryDefensiveImprovementAndItsAuthoredColumns() {
        assertEquals(17, DefensiveImprovement.values().length);
        assertEquals(1, DefensiveImprovement.AJUSTADA.getPhysicalDefenseBonus());
        assertEquals(1, DefensiveImprovement.BENCAO_SELVAGEM.getAttackBonus());
        assertEquals(1, DefensiveImprovement.BENCAO_YMIRIANA.getDamageBonus());
        assertEquals(1, DefensiveImprovement.ENCANTADORA.getCastingBonus());
    }

    @Test
    void ajustadaAndCamadaDeReforcoContributeTheirCorrectDefenseValues() {
        AbstractItem adjusted = defensiveItem(ItemCategory.ARMOR, ItemImprovement.of(DefensiveImprovement.AJUSTADA));
        AbstractItem physicalReinforcement = defensiveItem(
                ItemCategory.ARMOR, ItemImprovement.camadaDeReforco(DefenseType.PHYSICAL));
        AbstractItem shieldReinforcement = defensiveItem(
                ItemCategory.SHIELD, ItemImprovement.camadaDeReforcoParaEscudo());

        assertEquals(1, defenseService.getTotalDefense(characterWith(adjusted), DefenseType.PHYSICAL));
        assertEquals(0, defenseService.getTotalDefense(characterWith(adjusted), DefenseType.MAGIC));
        assertEquals(1, defenseService.getTotalDefense(characterWith(physicalReinforcement), DefenseType.PHYSICAL));
        assertEquals(0, defenseService.getTotalDefense(characterWith(physicalReinforcement), DefenseType.MAGIC));
        assertEquals(1, defenseService.getTotalDefense(characterWith(shieldReinforcement), DefenseType.PHYSICAL));
        assertEquals(1, defenseService.getTotalDefense(characterWith(shieldReinforcement), DefenseType.MAGIC));
    }

    @Test
    void ajustadaReducesAndResistenteIncreasesTheItemHardnessThatActsAsItsPv() {
        AbstractItem adjusted = defensiveItem(ItemCategory.ARMOR, 6, ItemImprovement.of(DefensiveImprovement.AJUSTADA));
        AbstractItem resistant = defensiveItem(ItemCategory.ARMOR, 6, ItemImprovement.of(DefensiveImprovement.RESISTENTE));
        AbstractItem lowHardnessAdjusted = defensiveItem(ItemCategory.ARMOR, 3,
                ItemImprovement.of(DefensiveImprovement.AJUSTADA));

        assertEquals(1, adjusted.getEffectiveHardness());
        assertEquals(16, resistant.getEffectiveHardness());
        assertEquals(0, lowHardnessAdjusted.getEffectiveHardness());
    }

    @Test
    void ajustadaReducesTheItemWeightClassByOneCategory() {
        AbstractItem adjusted = AbstractItem.builder()
                .name("Item de teste")
                .category(ItemCategory.ARMOR)
                .weightClass(ItemWeightClass.HEAVY)
                .build();
        adjusted.setImprovement(ItemImprovement.of(DefensiveImprovement.AJUSTADA));

        assertEquals(ItemWeightClass.MEDIUM, adjusted.getEffectiveWeightClass());
    }

    @Test
    void ocultaAndCamufladaGrantFurtividadeAdvantage() {
        AbstractItem oculta = defensiveItem(ItemCategory.ARMOR, ItemImprovement.of(DefensiveImprovement.OCULTA));
        AbstractItem camuflada = defensiveItem(ItemCategory.ARMOR, ItemImprovement.of(DefensiveImprovement.CAMUFLADA));

        assertEquals(3, new FurtividadeInteraction().applyTo(CharacterSheet.of(characterWith(oculta), new Player()))
                .getSkillRollBonus());
        assertEquals(3, new FurtividadeInteraction().applyTo(CharacterSheet.of(characterWith(camuflada), new Player()))
                .getSkillRollBonus());
    }

    @Test
    void bencaoElementalRequiresOneConcreteElementAtCreation() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemImprovement.of(DefensiveImprovement.BENCAO_ELEMENTAL));
        assertThrows(IllegalArgumentException.class,
                () -> ItemImprovement.bencaoElemental(ElementalType.TODOS));
    }

    @Test
    void bencaoElementalAddsItsDefenseBonusToTheExplicitDefensePoolAgainstTheChosenElement() {
        AbstractItem blessedItem = defensiveItem(ItemCategory.ARMOR,
                ItemImprovement.bencaoElemental(ElementalType.GELO));
        CharacterSheet sheet = CharacterSheet.of(characterWith(blessedItem), new Player());
        DamageDescriptor iceDamage = new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.GELO);
        DamageDescriptor fireDamage = new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.FOGO);

        assertEquals(2, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null, iceDamage));
        assertEquals(2, defenseService.getTotalDefense(sheet, DefenseType.MAGIC, null, iceDamage));
        assertEquals(0, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL, null, fireDamage));
    }

    @Test
    void bencaoElementalGrantsTwoDamageReductionOnlyAgainstMatchingElementalDamage() {
        AbstractItem blessedItem = defensiveItem(ItemCategory.ARMOR,
                ItemImprovement.bencaoElemental(ElementalType.GELO));
        CharacterSheet sheet = CharacterSheet.of(characterWith(blessedItem), new Player());

        assertEquals(8, damageService.calculateFinalDamage(sheet, null,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.GELO), null, 10, false));
        assertEquals(8, damageService.calculateFinalDamage(sheet, null,
                new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.GELO), null, 10, false));
        assertEquals(10, damageService.calculateFinalDamage(sheet, null,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO), null, 10, false));
    }

    @Test
    void categoryRestrictedImprovementsRejectInvalidItems() {
        assertThrows(IllegalArgumentException.class, () -> defensiveItem(
                ItemCategory.BOOTS, ItemImprovement.of(DefensiveImprovement.ENCAIXE)));
        assertThrows(IllegalArgumentException.class, () -> defensiveItem(
                ItemCategory.SHIELD, ItemImprovement.of(DefensiveImprovement.CAMUFLADA)));
        assertThrows(IllegalArgumentException.class, () -> defensiveItem(
                ItemCategory.SHIELD, ItemImprovement.camadaDeReforco(DefenseType.MAGIC)));
    }

    private AbstractItem defensiveItem(final ItemCategory category, final ItemImprovement improvement) {
        return defensiveItem(category, 0, improvement);
    }

    private AbstractItem defensiveItem(final ItemCategory category, final int hardness, final ItemImprovement improvement) {
        AbstractItem item = AbstractItem.builder().name("Item de teste").category(category).hardness(hardness).build();
        item.setImprovement(improvement);
        return item;
    }

    private Character characterWith(final AbstractItem item) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.DEXTERITY, 3)))
                .equipment(List.of(item))
                .build();
    }

    private SceneContext combatContext(final int round, final UUID sceneId) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, round, false, null, sceneId);
    }
}
