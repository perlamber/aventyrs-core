package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.item.ArmorItem;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Proteção Ungida — 3PD for 3 Rodadas of Meio-Dano, gated on wearing an Armadura or Escudo. */
class ProtecaoUngidaInteractionTest {

    private final ProtecaoUngidaInteraction interaction = new ProtecaoUngidaInteraction();
    private final DamageService damageService = new DamageServiceImpl();
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** A holder wearing equipment, with Força 3 so the Armadura Completa's own Favor is met. */
    private CharacterSheet holderWearing(final Item... equipment) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build())
                        .build())
                .equipment(new ArrayList<>(List.of(equipment)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private int currentPd(final CharacterSheet sheet) {
        return determinationPointsService.getCurrentDeterminationPoints(sheet.getCharacter(), sheet);
    }

    @Test
    void anArmourWearerGetsMeioDanoForThreeRodadas() {
        CharacterSheet holder = holderWearing(ArmorItem.ARMADURA_COMPLETA);
        int pdBefore = currentPd(holder);
        // Before: the Armadura's own Favor gives RD 2, so a 10-damage hit lands 8.
        assertEquals(8, damageService.calculateFinalDamage(holder, null, (DamageType) null, null, 10, false));

        InteractionResult result = interaction.applyTo(holder);

        assertEquals(3, result.getDeterminationPointsSpent());
        assertEquals(pdBefore - 3, currentPd(holder));
        assertEquals(1, holder.getTemporaryBonus(ModifierType.HALF_DAMAGE));
        // (10 - 2) / 2 — the halving lands after the flat reduction.
        assertEquals(4, damageService.calculateFinalDamage(holder, null, (DamageType) null, null, 10, false));
    }

    @Test
    void aShieldAloneAlsoSatisfiesTheRequirement() {
        CharacterSheet holder = holderWearing(ShieldItem.values()[0]);

        interaction.applyTo(holder);

        assertEquals(1, holder.getTemporaryBonus(ModifierType.HALF_DAMAGE));
    }

    @Test
    void theMeioDanoLapsesAfterThreeRodadas() {
        CharacterSheet holder = holderWearing(ArmorItem.ARMADURA_COMPLETA);
        interaction.applyTo(holder);

        for (int rodada = 0; rodada < ProtecaoUngidaInteraction.DURATION_IN_ROUNDS; rodada++) {
            assertEquals(1, holder.getTemporaryBonus(ModifierType.HALF_DAMAGE));
            holder.finishTurn();
        }

        assertEquals(0, holder.getTemporaryBonus(ModifierType.HALF_DAMAGE));
        assertEquals(8, damageService.calculateFinalDamage(holder, null, (DamageType) null, null, 10, false));
    }

    @Test
    void aHolderUsingNeitherArmourNorShieldIsRefusedAndPaysNothing() {
        CharacterSheet holder = holderWearing();
        int pdBefore = currentPd(holder);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> interaction.applyTo(holder));

        assertEquals(TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD, refused.getMessage());
        assertEquals(pdBefore, currentPd(holder));
        assertEquals(0, holder.getTemporaryBonus(ModifierType.HALF_DAMAGE));
    }

    @Test
    void aNonDefensiveItemDoesNotSatisfyTheRequirement() {
        CharacterSheet holder = holderWearing(org.aventyrs.core.item.HelmetItem.values()[0]);

        assertThrows(IllegalOperationException.class, () -> interaction.applyTo(holder));
    }
}
