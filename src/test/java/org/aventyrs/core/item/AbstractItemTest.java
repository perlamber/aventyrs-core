package org.aventyrs.core.item;

import org.aventyrs.core.ability.ItemActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractItemTest {

    @Test
    void buildsAOneOffItemWithEveryColumn() {
        AbstractItem item = AbstractItem.builder()
                .name("Adaga Simples")
                .description("Uma lâmina curta e comum.")
                .category(ItemCategory.LIGHT_BLADE)
                .rarity(ItemRarity.COMMON)
                .weightClass(ItemWeightClass.LIGHT)
                .price(2)
                .physicalDefenseBonus(0)
                .magicDefenseBonus(0)
                .hardness(8)
                .castingBonus(1)
                .build();

        assertEquals("Adaga Simples", item.getName());
        assertEquals(ItemType.OFFENSIVE, item.getType());
        assertEquals(2, item.getPrice());
        assertEquals(8, item.getHardness());
        assertEquals(1, item.getCastingBonus());
    }

    @Test
    void grantsNoFavorWhenItCarriesNone() {
        AbstractItem item = AbstractItem.builder().name("Corda").category(ItemCategory.RING).build();

        assertNull(item.getFavor());
        assertFalse(item.grantsFavorTo(null));
        assertEquals(0, item.resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, null));
        assertEquals(List.of(), item.resolveFavorBonuses(null));
    }

    @Test
    void canHoldUniqueForgeStateAndActiveAbilityAsARegalia() {
        AbstractItem item = AbstractItem.builder()
                .name("Anel da Chama")
                .category(ItemCategory.RING)
                .regaliaGrade(RegaliaGrade.MENOR)
                .build();

        Masterpiece masterpiece = new Masterpiece() {
            @Override
            public String getName() {
                return "Obra-prima da Chama";
            }

            @Override
            public String getDescription() {
                return "A chama se acende ao toque.";
            }
        };
        Improvement improvement = new Improvement() {
            @Override
            public String getName() {
                return "Aprimoramento";
            }

            @Override
            public String getDescription() {
                return "Melhora a potência arcana.";
            }

            @Override
            public ItemRarity getRarity() {
                return ItemRarity.COMMON;
            }
        };
        ItemActiveAbility activeAbility = new ItemActiveAbility() {
            @Override
            public String getDescription() {
                return "Lança uma chama";
            }

            @Override
            public int getActionPointCost() {
                return 1;
            }

            @Override
            public int getMagicPointCost() {
                return 2;
            }

            @Override
            public int getDurationInRounds() {
                return 3;
            }

            @Override
            public TemporaryEffect resolveEffect(final Character character) {
                return null;
            }
        };

        item.setMasterpiece(masterpiece);
        item.addImprovement(improvement);
        item.setActiveAbility(activeAbility);

        assertEquals(masterpiece, item.getMasterpiece());
        assertEquals(improvement, item.getImprovements().get(0));
        assertEquals(activeAbility, item.getActiveAbility());
        assertTrue(item.isRegalia());
        assertEquals(activeAbility, ((Item) item).getActiveAbility());
    }

    @Test
    void carriesSeveralAprimoramentosAtOnceAndTheirBonusesStack() {
        AbstractItem armor = AbstractItem.builder()
                .name("Armadura Reforçada")
                .category(ItemCategory.ARMOR)
                .weightClass(ItemWeightClass.HEAVY)
                .hardness(30)
                .build();

        armor.addImprovement(ItemImprovement.of(DefensiveImprovement.RESISTENTE)); // +10 Dureza, -1 item damage
        armor.addImprovement(ItemImprovement.of(DefensiveImprovement.AJUSTADA));   // -5 Dureza

        assertEquals(2, armor.getImprovements().size());
        assertEquals(35, armor.getEffectiveHardness()); // 30 + 10 - 5
        // getImprovement() shim still answers with the first fitted one.
        assertEquals(ItemImprovement.of(DefensiveImprovement.RESISTENTE), armor.getImprovement());
    }

    @Test
    void rejectsAnActiveAbilityForANonRegalia() {
        AbstractItem item = AbstractItem.builder()
                .name("Anel Comum")
                .category(ItemCategory.RING)
                .build();

        assertThrows(IllegalStateException.class, () -> item.setActiveAbility(new ItemActiveAbility() {
            @Override
            public String getDescription() {
                return "Não deveria poder ser usada.";
            }

            @Override
            public int getActionPointCost() {
                return 0;
            }

            @Override
            public int getMagicPointCost() {
                return 0;
            }

            @Override
            public int getDurationInRounds() {
                return 0;
            }

            @Override
            public TemporaryEffect resolveEffect(final Character character) {
                return null;
            }
        }));
        assertFalse(item.isRegalia());
        assertNull(item.getActiveAbility());
    }
}
