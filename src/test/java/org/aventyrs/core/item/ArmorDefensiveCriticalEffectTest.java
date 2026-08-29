package org.aventyrs.core.item;

import org.aventyrs.core.effect.DefensiveCriticalEffectType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every Armadura's Efeito Crítico Defensivo, against the source catalog's "Atualizando os
 * Equipamentos Defensivos" table ({@code docs/rules/efeitos-criticos.txt} L94–103).
 *
 * <p>Nothing reads this column yet — an Efeito Crítico Defensivo fires on an Acerto Crítico in
 * the wearer's own Defesa roll, a branch neither {@code AttackReceiver} nor {@code
 * EsquivaEApararInteraction} resolves. So there is no behaviour to assert, only that the
 * transcription is right and stays right: exactly the guard {@code ArmorItem}'s Preço and Dureza
 * columns would want if anything checked them.
 */
class ArmorDefensiveCriticalEffectTest {

    @Test
    void everyArmourGrantsTheEffectItsStatBlockAssigns() {
        Map<ArmorItem, DefensiveCriticalEffectType> expected = new EnumMap<>(ArmorItem.class);
        expected.put(ArmorItem.ARMADURA_COMPLETA, DefensiveCriticalEffectType.RETORNO_DE_DANOS);
        expected.put(ArmorItem.ARMADURA_DE_GLADIADOR, DefensiveCriticalEffectType.CONTRA_ATACANTE);
        expected.put(ArmorItem.ARMADURA_DE_JUSTA, DefensiveCriticalEffectType.PROVOCAR);
        expected.put(ArmorItem.COURACA, DefensiveCriticalEffectType.RETORNO_DE_DANOS);
        expected.put(ArmorItem.MEIA_ARMADURA, DefensiveCriticalEffectType.RETORNO_DE_DANOS);
        expected.put(ArmorItem.ROBE_CERIMONIAL, DefensiveCriticalEffectType.SURTO_ARCANO);
        expected.put(ArmorItem.ROBE_DE_GUERRA, DefensiveCriticalEffectType.SURTO_ARCANO);
        expected.put(ArmorItem.ROUPA_PESADA, DefensiveCriticalEffectType.LIBERDADE_DE_ACAO);

        Map<ArmorItem, DefensiveCriticalEffectType> actual = new EnumMap<>(ArmorItem.class);
        for (ArmorItem armor : ArmorItem.values()) {
            actual.put(armor, armor.getDefensiveCriticalEffect());
        }

        assertEquals(expected, actual);
    }

    /**
     * The column is non-null for every Armadura by construction — the source table assigns one
     * to each, so there is no "this armour grants none" case to represent. Contrast {@link
     * ArmorItem#ARMADURA_DE_GLADIADOR}'s {@code null} {@link ItemFavor}, where "Favor: Nenhum"
     * genuinely is absence.
     */
    @Test
    void noArmourIsWithoutADefensiveCriticalEffect() {
        Arrays.stream(ArmorItem.values())
                .forEach(armor -> assertNotNull(armor.getDefensiveCriticalEffect(), armor.name()));
    }

    /**
     * The armours reach five of the nine catalogued effects. Choque de AEther, Faísca de
     * Determinação and Repelir e Suprimir belong to Escudos and Defesas Naturais, neither of
     * which this core models — an expected gap, not an omission.
     *
     * <p>Ímpeto Defensivo is a different case: the source catalogues it (L70–72) but its
     * "Atualizando os Equipamentos Defensivos" table assigns it to <b>no</b> Armadura, Escudo or
     * Defesa Natural at all. So no gear in the ruleset currently grants it. Recorded here so
     * that adding a {@code ShieldItem} does not look like it closed this.
     */
    @Test
    void armoursReachFiveOfTheNineCataloguedEffects() {
        var reachedByArmour = Arrays.stream(ArmorItem.values())
                .map(ArmorItem::getDefensiveCriticalEffect)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                        DefensiveCriticalEffectType.RETORNO_DE_DANOS,
                        DefensiveCriticalEffectType.CONTRA_ATACANTE,
                        DefensiveCriticalEffectType.PROVOCAR,
                        DefensiveCriticalEffectType.SURTO_ARCANO,
                        DefensiveCriticalEffectType.LIBERDADE_DE_ACAO),
                reachedByArmour);

        assertEquals(9, DefensiveCriticalEffectType.values().length);
    }
}
