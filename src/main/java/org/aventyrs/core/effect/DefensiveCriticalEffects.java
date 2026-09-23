package org.aventyrs.core.effect;

import lombok.NonNull;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Which Efeitos Críticos Defensivos a defender's Acerto Crítico on a Defesa triggers — "Apenas
 * Armaduras e Escudos recebem Efeitos Críticos Defensivos e seus efeitos são cumulativos".
 */
public final class DefensiveCriticalEffects {

    private DefensiveCriticalEffects() {
    }

    /**
     * Every Efeito Crítico Defensivo defender's equipment and Títulos grant, in that order:
     * <ol>
     *   <li>each worn Armadura/Escudo's own ({@link Item#getDefensiveCriticalEffect()}) — cumulative,
     *   so an Armadura and an Escudo both fire;</li>
     *   <li>"Corpo Exposto (Sem armaduras ou escudos): Liberdade de Ação" when neither is worn;</li>
     *   <li>each held Título's additions ({@code AventyrTitle#resolveAdditionalDefensiveCriticalEffects}).</li>
     * </ol>
     *
     * <p>TODO the other four Defesas Naturais rows (Espinhos, Cascos e Conchas, Pele Escamosa ou
     * Emplumada, Pele Escorregadia): no Raça or Forma authors a natural defence as data yet.
     */
    public static List<DefensiveCriticalEffectType> grantedTo(@NonNull final CombatantSheet defender) {
        List<DefensiveCriticalEffectType> granted = new ArrayList<>();
        boolean armoured = false;
        for (Item item : defender.getCharacter().getEquipment()) {
            if (item.isDestroyed()) {
                continue;
            }
            if (item.getCategory() == ItemCategory.ARMOR || item.getCategory() == ItemCategory.SHIELD) {
                armoured = true;
            }
            DefensiveCriticalEffectType type = item.getDefensiveCriticalEffect();
            if (type != null) {
                granted.add(type);
            }
        }
        if (!armoured) {
            granted.add(DefensiveCriticalEffectType.LIBERDADE_DE_ACAO);
        }
        defender.getCharacter().getAllTitles()
                .forEach(title -> granted.addAll(title.resolveAdditionalDefensiveCriticalEffects(defender)));
        return granted;
    }
}
