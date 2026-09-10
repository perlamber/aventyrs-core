package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShieldItemTest {

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DamageService damageService = new DamageServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedShield() {
        assertEquals(6, ShieldItem.values().length);
    }

    @Test
    void everyConstantIsANamedShieldThatGrantsADefensiveCriticalEffect() {
        Arrays.stream(ShieldItem.values()).forEach(shield -> {
            assertEquals(ItemCategory.SHIELD, shield.getCategory());
            assertEquals(ItemType.DEFENSIVE, shield.getType());
            assertFalse(shield.getName().isBlank());
            assertTrue(shield instanceof CriticallyDefensiveItem);
            assertNotNull(shield.getDefensiveCriticalEffect());
        });
    }

    @Test
    void escudoDeCorpoCarriesEveryColumnOfItsRulesText() {
        ShieldItem shield = ShieldItem.ESCUDO_DE_CORPO;

        assertEquals("Escudo de Corpo", shield.getName());
        assertEquals(ItemWeightClass.HEAVY, shield.getWeightClass());
        assertEquals(ItemRarity.RARE, shield.getRarity());
        assertEquals(9, shield.getPrice());
        assertEquals(2, shield.getPhysicalDefenseBonus());
        assertEquals(2, shield.getMagicDefenseBonus());
        assertEquals(20, shield.getHardness());
        assertEquals(-2, shield.getCastingBonus());
    }

    @Test
    void aConditionedFavorBonusResolvesToZeroOnThePlainCharacterPath() {
        Character character = characterWithStrength(3);

        assertEquals(0, ShieldItem.ESCUDO_MEDIO.resolveFavorBonus(ModifierType.DEFESAS, character));
        assertEquals(0, ShieldItem.ESCUDO_MEDIO.resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, character));
    }

    @Test
    void escudoMedioDefesaBonusAppliesOnlyWhileNoOffensiveActionWasTakenThisRound() {
        CharacterSheet sheet = sheetWieldingWithStrength(ShieldItem.ESCUDO_MEDIO, 3);

        // Flat DF column 2 + conditional DEFESAS 1 while the wielder has not attacked.
        assertEquals(2 + 1, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));

        sheet.recordAction(attackAction());

        assertEquals(2, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    @Test
    void escudoMedioDamageReductionAppliesOnlyWhileNoOffensiveActionWasTakenThisRound() {
        CharacterSheet sheet = sheetWieldingWithStrength(ShieldItem.ESCUDO_MEDIO, 3);

        assertEquals(1, damageService.getTotalDamageReduction(sheet, DamageType.FISICO, sheet));

        sheet.recordAction(attackAction());

        assertEquals(0, damageService.getTotalDamageReduction(sheet, DamageType.FISICO, sheet));
    }

    @Test
    void escudoMedioConditionalBonusRequiresForca3() {
        CharacterSheet sheet = sheetWieldingWithStrength(ShieldItem.ESCUDO_MEDIO, 2);

        assertEquals(2, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
        assertEquals(0, damageService.getTotalDamageReduction(sheet, DamageType.FISICO, sheet));
    }

    @Test
    void bracadeirasGrantsAConditionalMagicDefenseFromItsEfeitoAdicional() {
        CharacterSheet sheet = sheetWieldingWith(ShieldItem.BRACADEIRAS, AttributeDomain.DEXTERITY, 3);

        assertEquals(1 + 1, defenseService.getTotalDefense(sheet, DefenseType.MAGIC));

        sheet.recordAction(attackAction());

        assertEquals(1, defenseService.getTotalDefense(sheet, DefenseType.MAGIC));
    }

    /** Bracelete Arcano / Repulsor need an activation path this core lacks. */
    @Test
    void grantsNoBonusForItsActivatedEffectClauses() {
        assertTrue(ShieldItem.BRACELETE_ARCANO.getFavor().getBonuses().isEmpty());
        assertTrue(ShieldItem.REPULSOR.getFavor().getBonuses().isEmpty());
        assertTrue(ShieldItem.BRACELETE_ARCANO.getFavor().hasAdditionalEffects());
    }

    private static CombatantAction attackAction() {
        return new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH,
                null, null, 1, null);
    }

    private static CharacterSheet sheetWieldingWithStrength(final ShieldItem shield, final int base) {
        return sheetWieldingWith(shield, AttributeDomain.STRENGTH, base);
    }

    private static CharacterSheet sheetWieldingWith(final ShieldItem shield, final AttributeDomain domain,
                                                    final int base) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(domain, base)))
                .equipment(List.of(shield))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static Character characterWithStrength(final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.STRENGTH, base)))
                .build();
    }
}
