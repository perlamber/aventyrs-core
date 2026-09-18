package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.title.santo.SantoAbility;
import org.aventyrs.core.title.santo.SantoSpecialization;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

    // --- A Título's own Efeito Base ----------------------------------------------------------

    /** Santo's Despertar: "+2 em suas Defesas", before any adjacency or Especialização. */
    @Test
    void aHeldTitulosBaseEffectGrantsItsDefesasOnBothDefenses() {
        Character plain = CharacterFixture.blank(CharacterFixture.BLANK).build();
        int physicalBefore = defenseService.getTotalDefense(plain, DefenseType.PHYSICAL);
        int magicBefore = defenseService.getTotalDefense(plain, DefenseType.MAGIC);

        Character santo = CharacterFixture.blank(CharacterFixture.BLANK).build();
        santo.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        // "suas Defesas" is plural — the broad DEFESAS shape, so both DF and DM, added once each.
        assertEquals(physicalBefore + 2, defenseService.getTotalDefense(santo, DefenseType.PHYSICAL));
        assertEquals(magicBefore + 2, defenseService.getTotalDefense(santo, DefenseType.MAGIC));
    }

    /** "esse Bônus aumenta em +1 para cada aliado adjacente". */
    @Test
    void theBaseEffectGrowsWithEachAdjacentAlly() {
        Character santoCharacter = CharacterFixture.blank(CharacterFixture.BLANK).build();
        santoCharacter.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        int alone = defenseService.getTotalDefense(santoCharacter, DefenseType.PHYSICAL, null);

        CharacterSheet allyOne = plainSheet();
        CharacterSheet allyTwo = plainSheet();
        SceneContext twoAdjacent = new SceneContext(List.of(allyOne, allyTwo), List.of(),
                Map.of(allyOne, Range.ADJACENTE, allyTwo, Range.ADJACENTE));

        assertEquals(alone + 2, defenseService.getTotalDefense(santoCharacter, DefenseType.PHYSICAL, twoAdjacent));
    }

    /** Only *adjacent* allies count — one standing further off adds nothing. */
    @Test
    void anAllyBeyondAdjacenteDoesNotGrowTheBaseEffect() {
        Character santoCharacter = CharacterFixture.blank(CharacterFixture.BLANK).build();
        santoCharacter.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet ally = plainSheet();
        SceneContext farAlly = new SceneContext(List.of(ally), List.of(), Map.of(ally, Range.DISTANCIA_CURTA));

        assertEquals(defenseService.getTotalDefense(santoCharacter, DefenseType.PHYSICAL, null),
                defenseService.getTotalDefense(santoCharacter, DefenseType.PHYSICAL, farAlly));
    }

    /** "+1 para cada ... Especializações e Supremas que você possua". */
    @Test
    void theBaseEffectGrowsWithEspecializacoesAndSupremas() {
        Character bare = CharacterFixture.blank(CharacterFixture.BLANK).build();
        bare.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);

        Character decorated = CharacterFixture.blank(CharacterFixture.BLANK).build();
        // One Especialização + one Suprema; BASTIAO_DOS_NECESSITADOS is a Habilidade and doesn't count.
        decorated.grantTitle(new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS, SantoAbility.GUARDA_VIDAS)), TitleSlot.PRIMARY);

        assertEquals(defenseService.getTotalDefense(bare, DefenseType.PHYSICAL) + 2,
                defenseService.getTotalDefense(decorated, DefenseType.PHYSICAL));
    }

    /**
     * The Character and CombatantSheet paths deliberately don't cascade, so the Título source has
     * to be summed on both — this is the test that would catch it being added to only one.
     */
    @Test
    void theBaseEffectIsSummedOnTheSheetPathToo() {
        Character santoCharacter = CharacterFixture.blank(CharacterFixture.BLANK).build();
        santoCharacter.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = CharacterSheet.of(santoCharacter, new Player());

        assertEquals(defenseService.getTotalDefense(santoCharacter, DefenseType.PHYSICAL),
                defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    @Test
    void aCharacterWithNoTituloGetsNoBaseEffect() {
        Character plain = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet ally = plainSheet();
        SceneContext adjacent = new SceneContext(List.of(ally), List.of(), Map.of(ally, Range.ADJACENTE));

        assertEquals(defenseService.getTotalDefense(plain, DefenseType.PHYSICAL, null),
                defenseService.getTotalDefense(plain, DefenseType.PHYSICAL, adjacent));
    }

    private CharacterSheet plainSheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }
}
