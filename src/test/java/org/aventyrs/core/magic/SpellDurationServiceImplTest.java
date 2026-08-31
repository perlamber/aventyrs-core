package org.aventyrs.core.magic;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.DefensiveImprovement;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemImprovement;
import org.aventyrs.core.magic.catalog.OcultacaoSpell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellDurationServiceImplTest {

    private final SpellDurationService durationService = new SpellDurationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void encantadoraAddsOneRoundToAnEnchantmentInItsCasterEquipment() {
        Character caster = characterWith(ItemImprovement.of(DefensiveImprovement.ENCANTADORA));

        assertEquals(25, durationService.resolveDurationInRounds(OcultacaoSpell.OCULTAR_SE_NAS_SOMBRAS, caster)
                .orElseThrow());
    }

    @Test
    void encantadoraAddsOneRoundToACurseAndNotToOtherMagicTypes() {
        Character caster = characterWith(ItemImprovement.of(DefensiveImprovement.ENCANTADORA));
        Spell curse = spellWith(MagicType.MALDICAO, SpellDuration.rodadas(3));
        Spell elemental = spellWith(MagicType.ELEMENTAL, SpellDuration.rodadas(3));

        assertEquals(4, durationService.resolveDurationInRounds(curse, caster).orElseThrow());
        assertEquals(3, durationService.resolveDurationInRounds(elemental, caster).orElseThrow());
    }

    @Test
    void resolvesTargetAttributeDurationsBeforeAddingEncantadoraBonus() {
        Character caster = characterWith(ItemImprovement.of(DefensiveImprovement.ENCANTADORA));
        Character target = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.VIGOR, 4)))
                .build();
        Spell spell = spellWith(MagicType.ENCANTAMENTO,
                SpellDuration.targetAttribute(AttributeDomain.VIGOR, DurationUnit.RODADA));

        assertTrue(durationService.resolveDurationInRounds(spell, caster).isEmpty());
        assertEquals(5, durationService.resolveDurationInRounds(spell, caster, target).orElseThrow());
    }

    @Test
    void encantadoraDoesNotTurnAnInstantaneousSpellIntoARoundLongEffect() {
        Character caster = characterWith(ItemImprovement.of(DefensiveImprovement.ENCANTADORA));
        Spell spell = spellWith(MagicType.ENCANTAMENTO, SpellDuration.INSTANTANEA);

        assertEquals(0, durationService.resolveDurationInRounds(spell, caster).orElseThrow());
    }

    private Character characterWith(final ItemImprovement improvement) {
        AbstractItem item = AbstractItem.builder().name("Item de teste").category(ItemCategory.ARMOR).build();
        item.setImprovement(improvement);
        return CharacterFixture.blank(CharacterFixture.BLANK).equipment(List.of(item)).build();
    }

    private Spell spellWith(final MagicType type, final SpellDuration duration) {
        return new TestSpell() {
            @Override
            public MagicType getPrimaryType() {
                return type;
            }

            @Override
            public SpellDuration getDuration() {
                return duration;
            }
        };
    }
}
