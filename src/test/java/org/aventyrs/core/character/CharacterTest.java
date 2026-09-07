package org.aventyrs.core.character;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void alignmentDefaultsToNeutralWhenNotSet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertEquals(Alignment.NEUTRAL, character.getAlignment());
    }

    @Test
    void sexoIsNullByDefault() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertNull(character.getSexo());
    }

    @Test
    void builderAssignsSexoAndAlignment() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .sexo(Character.Sexo.FEMININO)
                .alignment(Alignment.EVIL)
                .build();

        assertEquals(Character.Sexo.FEMININO, character.getSexo());
        assertEquals(Alignment.EVIL, character.getAlignment());
    }

    @Test
    void deityIsNullByDefault() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertNull(character.getDeity());
    }

    @Test
    void builderAssignsDeity() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .deity(Deity.LUZ_PRIMORDIAL)
                .build();

        assertEquals(Deity.LUZ_PRIMORDIAL, character.getDeity());
    }

    @Test
    void hasNoTitlesByDefault() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertNull(character.getPrimaryTitle());
        assertNull(character.getSecondaryTitle());
        assertNull(character.getTertiaryTitle());
        assertTrue(character.getAllTitles().isEmpty());
    }

    @Test
    void grantTitleFillsTheRequestedSlot() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        Santo santo = new Santo(List.of(), List.of());

        character.grantTitle(santo, TitleSlot.SECONDARY);

        assertEquals(santo, character.getSecondaryTitle());
        assertNull(character.getPrimaryTitle());
        assertNull(character.getTertiaryTitle());
    }

    @Test
    void grantTitleOverwritesWhateverAlreadyOccupiedTheSlot() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        Santo first = new Santo(List.of(), List.of());
        Santo second = new Santo(List.of(), List.of());
        character.grantTitle(first, TitleSlot.PRIMARY);

        character.grantTitle(second, TitleSlot.PRIMARY);

        assertEquals(second, character.getPrimaryTitle());
    }

    @Test
    void getAllTitlesListsOnlyTheFilledSlotsPrimaryFirst() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        Santo primary = new Santo(List.of(), List.of());
        Santo tertiary = new Santo(List.of(), List.of());
        character.grantTitle(tertiary, TitleSlot.TERTIARY);
        character.grantTitle(primary, TitleSlot.PRIMARY);

        assertEquals(List.of(primary, tertiary), character.getAllTitles());
    }

    @Test
    void equipmentDefaultsToAFreshEmptyMutableListPerBuild() {
        Character first = CharacterFixture.blank(CharacterFixture.BLANK).equipment(new java.util.ArrayList<>()).build();
        Character second = CharacterFixture.blank(CharacterFixture.BLANK).equipment(new java.util.ArrayList<>()).build();

        first.equip(ArmorItem.ARMADURA_COMPLETA);

        assertEquals(List.of(ArmorItem.ARMADURA_COMPLETA), first.getEquipment());
        assertTrue(second.getEquipment().isEmpty());
    }

    @Test
    void equipAppendsAndUnequipRemovesOneOccurrence() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .equipment(new java.util.ArrayList<>())
                .build();

        character.equip(ArmorItem.COURACA);
        character.equip(ArmorItem.COURACA);
        assertEquals(2, character.getEquipment().size());

        assertTrue(character.unequip(ArmorItem.COURACA));
        assertEquals(List.of(ArmorItem.COURACA), character.getEquipment());

        assertTrue(character.unequip(ArmorItem.COURACA));
        assertTrue(character.getEquipment().isEmpty());
    }

    @Test
    void unequipReportsFalseForAnItemThatWasNeverEquipped() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .equipment(new java.util.ArrayList<>())
                .build();

        assertFalse(character.unequip(ArmorItem.COURACA));
    }

    @Test
    void effectiveAttributeTotalIsTheBareTotalWithNoFeatGrant() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(4).variable(1).build())
                        .build())
                .build();

        assertEquals(5, character.getEffectiveAttributeTotal(AttributeDomain.GNOSE));
    }

    @Test
    void effectiveAttributeTotalAddsEveryHeldFeatsAttributeBonus() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .gnose(AttributeValue.builder().domain(AttributeDomain.GNOSE).base(5).build())
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .feats(new java.util.ArrayList<>())
                .build();
        character.grantFeat(new org.aventyrs.core.feat.ConselheiroDeGuerraYmirianoFeat(
                org.aventyrs.core.ability.StrengthAbility.DESTRUIDOR_DE_MUROS));

        assertEquals(6, character.getEffectiveAttributeTotal(AttributeDomain.GNOSE));
        assertEquals(3, character.getEffectiveAttributeTotal(AttributeDomain.STRENGTH), "untouched Atributo");
    }

    @Test
    void attributeAbilitiesFoldInFeatGrantedOnesButAcquiredListDoesNot() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new java.util.ArrayList<>())
                .build();
        character.grantFeat(new org.aventyrs.core.feat.ConselheiroDeGuerraYmirianoFeat(
                org.aventyrs.core.ability.StrengthAbility.SUBJUGAR));

        assertTrue(character.getAttributeAbilities()
                .contains(org.aventyrs.core.ability.StrengthAbility.SUBJUGAR));
        assertTrue(character.getAcquiredAttributeAbilities().isEmpty());
    }
}
