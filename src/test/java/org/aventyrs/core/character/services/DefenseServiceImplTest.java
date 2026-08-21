package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefenseServiceImplTest {

    private final DefenseService defenseService = new DefenseServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A test-only ability granting the broad, undifferentiated DEFESAS type. */
    private static class BothDefensesAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.VIGOR;
        }

        @Override
        public String getDescription() {
            return "Test-only +2 Defesas source, applying to both DF and DM.";
        }

        @Modifier(ModifierType.DEFESAS)
        public int bonus() {
            return 2;
        }
    }

    /** A test-only ability granting DF alone. */
    private static class PhysicalOnlyAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.VIGOR;
        }

        @Override
        public String getDescription() {
            return "Test-only +4 DF source.";
        }

        @Modifier(ModifierType.PHYSICAL_DEFENSE)
        public int bonus() {
            return 4;
        }
    }

    /** A test-only ability applying the standard -2 Defesas penalty, to prove it isn't clamped. */
    private static class DefesasPenaltyAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.VIGOR;
        }

        @Override
        public String getDescription() {
            return "Test-only -2 Defesas malus.";
        }

        @Modifier(ModifierType.DEFESAS)
        public int malus() {
            return -2;
        }
    }

    private Character.CharacterBuilder characterWithStrength(final int strengthBase) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(strengthBase).build())
                        .build());
    }

    @Test
    void aDefesasAbilityAppliesToBothPhysicalAndMagicDefense() {
        Character character = characterWithStrength(0)
                .attributeAbility(new BothDefensesAbility())
                .build();

        assertEquals(2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(2, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void aScopedAbilityAppliesOnlyToItsOwnDefense() {
        Character character = characterWithStrength(0)
                .attributeAbility(new PhysicalOnlyAbility())
                .build();

        assertEquals(4, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(0, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void broadAndScopedSourcesCombineAdditively() {
        Character character = characterWithStrength(0)
                .attributeAbility(new BothDefensesAbility())
                .attributeAbility(new PhysicalOnlyAbility())
                .build();

        assertEquals(2 + 4, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(2, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void anEquippedItemContributesItsFlatDefenseColumns() {
        Character character = characterWithStrength(0)
                .equipment(List.of(ArmorItem.ARMADURA_COMPLETA))
                .build();

        assertEquals(ArmorItem.ARMADURA_COMPLETA.getPhysicalDefenseBonus(),
                defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(ArmorItem.ARMADURA_COMPLETA.getMagicDefenseBonus(),
                defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    /**
     * ROUPA_PESADA has no DF/DM columns at all — its whole contribution is its Favor's single
     * undifferentiated {@code DEFESAS 2}, which must therefore reach both Defesas.
     */
    @Test
    void anItemFavorsDefesasBonusAppliesToBothDefensesOnceItsRequisitosAreMet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .equipment(List.of(ArmorItem.ROUPA_PESADA))
                .build();

        assertEquals(2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(2, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void anItemFavorContributesNothingWhenItsRequisitosArentMet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(1).build())
                        .build())
                .equipment(List.of(ArmorItem.ROUPA_PESADA))
                .build();

        assertEquals(0, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(0, defenseService.getTotalDefense(character, DefenseType.MAGIC));
    }

    @Test
    void aTemporaryDefesasBonusCountsOnlyOnTheSheetOverload() {
        Character character = characterWithStrength(0).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.grantTemporaryBonus(ModifierType.DEFESAS, 2, 2);

        assertEquals(0, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
        assertEquals(2, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
        assertEquals(2, defenseService.getTotalDefense(sheet, DefenseType.MAGIC));
    }

    @Test
    void aScopedTemporaryBonusReachesOnlyItsOwnDefense() {
        Character character = characterWithStrength(0).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.grantTemporaryBonus(ModifierType.MAGIC_DEFENSE, 3, 2);

        assertEquals(0, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
        assertEquals(3, defenseService.getTotalDefense(sheet, DefenseType.MAGIC));
    }

    /**
     * Unlike Reações/RD/RA, a Defesa isn't a spendable resource — it's a comparison value, so a
     * net-negative total is a valid (if dire) state and must survive rather than clamp to 0.
     */
    @Test
    void aNetNegativeTotalIsNotClampedAtZero() {
        Character character = characterWithStrength(0)
                .attributeAbility(new DefesasPenaltyAbility())
                .build();

        assertEquals(-2, defenseService.getTotalDefense(character, DefenseType.PHYSICAL));
    }

    @Test
    void everySourceCombinesOnOneCharacter() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .attributeAbility(new BothDefensesAbility())
                .attributeAbility(new PhysicalOnlyAbility())
                .equipment(List.of(ArmorItem.ARMADURA_COMPLETA))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.grantTemporaryBonus(ModifierType.DEFESAS, 2, 2);

        // 2 (DEFESAS ability) + 4 (PHYSICAL_DEFENSE ability) + 5 (armor DF column) + 2 (blessing).
        assertEquals(2 + 4 + ArmorItem.ARMADURA_COMPLETA.getPhysicalDefenseBonus() + 2,
                defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }
}
