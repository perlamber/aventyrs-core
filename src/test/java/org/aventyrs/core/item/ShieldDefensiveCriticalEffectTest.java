package org.aventyrs.core.item;

import org.aventyrs.core.effect.DefensiveCriticalEffectType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every Escudo's Efeito Crítico Defensivo, against the source catalog's "Atualizando os
 * Equipamentos Defensivos" table ({@code docs/rules/efeitos-criticos.txt} L104–110). The Escudo
 * twin of {@code ArmorDefensiveCriticalEffectTest} — nothing reads the column yet, so this only
 * guards the transcription.
 */
class ShieldDefensiveCriticalEffectTest {

    @Test
    void everyShieldGrantsTheEffectItsStatBlockAssigns() {
        Map<ShieldItem, DefensiveCriticalEffectType> expected = new EnumMap<>(ShieldItem.class);
        expected.put(ShieldItem.BRACADEIRAS, DefensiveCriticalEffectType.FAISCA_DE_DETERMINACAO);
        expected.put(ShieldItem.BRACELETE_ARCANO, DefensiveCriticalEffectType.CHOQUE_DE_AETHER);
        expected.put(ShieldItem.BROQUEL, DefensiveCriticalEffectType.CONTRA_ATACANTE);
        expected.put(ShieldItem.ESCUDO_MEDIO, DefensiveCriticalEffectType.REPELIR_E_SUPRIMIR);
        expected.put(ShieldItem.ESCUDO_DE_CORPO, DefensiveCriticalEffectType.PROVOCAR);
        expected.put(ShieldItem.REPULSOR, DefensiveCriticalEffectType.CHOQUE_DE_AETHER);

        Map<ShieldItem, DefensiveCriticalEffectType> actual = new EnumMap<>(ShieldItem.class);
        for (ShieldItem shield : ShieldItem.values()) {
            actual.put(shield, shield.getDefensiveCriticalEffect());
        }

        assertEquals(expected, actual);
    }

    @Test
    void noShieldIsWithoutADefensiveCriticalEffect() {
        Arrays.stream(ShieldItem.values())
                .forEach(shield -> assertNotNull(shield.getDefensiveCriticalEffect(), shield.name()));
    }

    /**
     * Armaduras and Escudos together reach eight of the nine catalogued effects. Ímpeto
     * Defensivo is assigned to no gear at all in the source table, and Surto Arcano / Liberdade
     * de Ação / Retorno de Danos come from Armaduras alone.
     */
    @Test
    void armouresAndShieldsTogetherReachEightOfTheNineCataloguedEffects() {
        Set<DefensiveCriticalEffectType> reached = Stream.concat(
                        Arrays.stream(ArmorItem.values()).map(ArmorItem::getDefensiveCriticalEffect),
                        Arrays.stream(ShieldItem.values()).map(ShieldItem::getDefensiveCriticalEffect))
                .collect(Collectors.toSet());

        assertEquals(8, reached.size());
        assertFalse(reached.contains(DefensiveCriticalEffectType.IMPETO_DEFENSIVO));
    }
}
