package org.aventyrs.core.item;

import java.util.List;
import java.util.Map;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.FreeActionsService;
import org.aventyrs.core.character.services.FreeActionsServiceImpl;
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.character.services.ReactionsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.persuasao.PersuasaoInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefensiveMasterpieceTest {

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();
    private final ReactionsService reactionsService = new ReactionsServiceImpl();
    private final FreeActionsService freeActionsService = new FreeActionsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneCatalogEntryForEveryDefensiveMasterpiece() {
        assertEquals(15, DefensiveMasterpiece.values().length);
        assertEquals(ItemRarity.MYTHIC, DefensiveMasterpiece.COURO_DE_DRAGAO.getRarity());
        assertEquals(ItemRarity.MYTHIC, DefensiveMasterpiece.ESPIRITO_UMBRAL.getRarity());
    }

    @Test
    void reforcadaReplacesItsOwnDfBonusWhenDexterityRequirementIsMet() {
        AbstractItem item = defensiveItem(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA));

        assertEquals(1, defenseService.getTotalDefense(characterWith(Map.of(), item), DefenseType.PHYSICAL));
        assertEquals(2, defenseService.getTotalDefense(characterWith(Map.of(AttributeDomain.DEXTERITY, 3), item),
                DefenseType.PHYSICAL));
        assertEquals(0, defenseService.getTotalDefense(characterWith(Map.of(AttributeDomain.DEXTERITY, 3), item),
                DefenseType.MAGIC));
    }

    @Test
    void runicaReplacesItsOwnDmBonusWhenStrengthRequirementIsMet() {
        AbstractItem item = defensiveItem(ItemMasterpiece.of(DefensiveMasterpiece.RUNICA));

        assertEquals(1, defenseService.getTotalDefense(characterWith(Map.of(), item), DefenseType.MAGIC));
        assertEquals(2, defenseService.getTotalDefense(characterWith(Map.of(AttributeDomain.STRENGTH, 3), item),
                DefenseType.MAGIC));
    }

    @Test
    void magistralAppliesOnlyToItsChosenDefenseWhenBothRequirementsAreMet() {
        AbstractItem item = defensiveItem(ItemMasterpiece.magistral(DefenseType.MAGIC));

        assertEquals(0, defenseService.getTotalDefense(characterWith(
                Map.of(AttributeDomain.DEXTERITY, 3), item), DefenseType.MAGIC));
        Character character = characterWith(Map.of(AttributeDomain.DEXTERITY, 3, AttributeDomain.GNOSE, 3), item);
        assertEquals(0, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(3, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void equilibradaAndAdamantinaGrantTheirDamageReductionOnlyWhenRequirementsAreMet() {
        AbstractItem equilibrada = defensiveItem(ItemMasterpiece.of(DefensiveMasterpiece.EQUILIBRADA));
        AbstractItem adamantina = defensiveItem(ItemMasterpiece.of(DefensiveMasterpiece.ADAMANTINA));

        assertEquals(0, damageService.getTotalDamageReduction(characterWith(Map.of(AttributeDomain.STRENGTH, 3), equilibrada)));
        assertEquals(1, damageService.getTotalDamageReduction(characterWith(
                Map.of(AttributeDomain.STRENGTH, 3, AttributeDomain.GNOSE, 3), equilibrada)));
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, damageService.getTotalDamageReduction(
                characterWith(Map.of(AttributeDomain.VIGOR, 3), adamantina)));
    }

    @Test
    void mitralAndAdamantinaAdjustWeightClassInOppositeDirectionsWithinItsBounds() {
        AbstractItem mitral = AbstractItem.builder()
                .name("Armadura de Mitral")
                .category(ItemCategory.ARMOR)
                .weightClass(ItemWeightClass.LIGHT)
                .build();
        mitral.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.MITRAL));
        AbstractItem adamantina = AbstractItem.builder()
                .name("Armadura de Adamantina")
                .category(ItemCategory.ARMOR)
                .weightClass(ItemWeightClass.HEAVY)
                .build();
        adamantina.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.ADAMANTINA));

        assertEquals(ItemWeightClass.LIGHT, mitral.getEffectiveWeightClass());
        assertEquals(ItemWeightClass.HEAVY, adamantina.getEffectiveWeightClass());
    }

    @Test
    void sobMedidaGrantsTheSelectedCounterOnlyWhenFocusRequirementIsMet() {
        AbstractItem reactionsItem = defensiveItem(ItemMasterpiece.sobMedida(ModifierType.REACTIONS));
        AbstractItem freeActionsItem = defensiveItem(ItemMasterpiece.sobMedida(ModifierType.FREE_ACTIONS));

        assertEquals(1, reactionsService.getTotalReactions(characterWith(Map.of(), reactionsItem)));
        assertEquals(2, reactionsService.getTotalReactions(characterWith(Map.of(AttributeDomain.FOCUS, 3), reactionsItem)));
        assertEquals(1, freeActionsService.getTotalFreeActions(characterWith(Map.of(), freeActionsItem)));
        assertEquals(2, freeActionsService.getTotalFreeActions(
                characterWith(Map.of(AttributeDomain.FOCUS, 3), freeActionsItem)));
    }

    @Test
    void banhadaEmOuroGrantsPersuasaoAdvantageWithoutLeakingToOtherSkills() {
        AbstractItem item = defensiveItem(ItemMasterpiece.of(DefensiveMasterpiece.BANHADA_EM_OURO));
        Character character = characterWith(Map.of(AttributeDomain.FOCUS, 3, AttributeDomain.CHARISMA, 2), item);

        assertEquals(Skill.ADVANTAGE_BONUS, item.getMasterpiece().resolveBonus(
                SkillType.PERSUASAO.getRollBonusType(), SkillType.PERSUASAO, character));
        assertEquals(0, item.getMasterpiece().resolveBonus(
                SkillType.ATLETISMO.getRollBonusType(), SkillType.ATLETISMO, character));
        assertEquals(Skill.ADVANTAGE_BONUS, new PersuasaoInteraction().applyTo(
                CharacterSheet.of(character, new Player())).getSkillRollBonus());
    }

    @Test
    void defensiveMasterpiecesCannotBeAttachedToNonDefensiveItems() {
        AbstractItem item = AbstractItem.builder().name("Anel").category(ItemCategory.RING).build();

        assertThrows(IllegalArgumentException.class,
                () -> item.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.REFORCADA)));
    }

    @Test
    void dyospirosRequiresAMediumOrHeavyShield() {
        AbstractItem lightShield = AbstractItem.builder()
                .name("Escudo Leve")
                .category(ItemCategory.SHIELD)
                .weightClass(ItemWeightClass.LIGHT)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> lightShield.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.DYOSPIROS)));

        AbstractItem mediumShield = AbstractItem.builder()
                .name("Escudo Médio")
                .category(ItemCategory.SHIELD)
                .weightClass(ItemWeightClass.MEDIUM)
                .build();
        mediumShield.setMasterpiece(ItemMasterpiece.of(DefensiveMasterpiece.DYOSPIROS));
        assertEquals(DefensiveMasterpiece.DYOSPIROS, ((ItemMasterpiece) mediumShield.getMasterpiece()).getDefinition());
    }

    @Test
    void creationChoiceIsRequiredOnlyForTheTwoConfigurableMasterpieces() {
        assertThrows(IllegalArgumentException.class, () -> ItemMasterpiece.of(DefensiveMasterpiece.MAGISTRAL));
        assertThrows(IllegalArgumentException.class, () -> ItemMasterpiece.of(DefensiveMasterpiece.SOB_MEDIDA));
        assertThrows(IllegalArgumentException.class, () -> ItemMasterpiece.sobMedida(ModifierType.MOVEMENT));
        assertTrue(DefensiveMasterpiece.COURO_DE_MONSTRO.getFavorDescription().contains("Habilidade Monstruosa"));
        assertFalse(DefensiveMasterpiece.GELO_VERDADEIRO.getAdditionalEffects().isBlank());
    }

    private AbstractItem defensiveItem(final ItemMasterpiece masterpiece) {
        AbstractItem item = AbstractItem.builder().name("Item de teste").category(ItemCategory.ARMOR).build();
        item.setMasterpiece(masterpiece);
        return item;
    }

    private Character characterWith(final Map<AttributeDomain, Integer> attributes, final Item... equipment) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(attributes))
                .equipment(List.of(equipment))
                .build();
    }
}
