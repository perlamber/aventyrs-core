package org.aventyrs.core.scene;

import lombok.Builder;
import lombok.Singular;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.ConditionType;

import java.util.List;

/**
 * What a {@link MonsterAura} does to everyone inside it once per Rodada, at its holder's Turn start (table ruling
 * 2026-10-15) — rolled once for everyone, never per target. Resolved by {@code Scene#pulseMonsterAuras} into {@link
 * AuraHit}s the caller applies.
 *
 * @param dice                   d6 thrown once for the whole pulse (0 for none)
 * @param flat                   added to the dice
 * @param descriptor             the damage's kind, or {@code null} when the pulse deals none
 * @param reductionPerUd         "reduzidos em -1 para cada UD" — counted past the first UD, as {@code
 *                               ElementalDischarge}
 * @param lifeSteal              the holder heals what the pulse dealt (Presença do Ceifeiro)
 * @param conditions             Condições every target takes (Ventos Caóticos' Cego)
 * @param conditionRounds        their Duração
 * @param healAsRest             heal each target as this Descanso would, or {@code null} (Dádiva de Gaea's Curto)
 * @param autocontroleLoss       temporary Autocontrole each target loses (Mestre das Marionetes)
 * @param everyNRounds           fire only on Rodadas divisible by this ("a cada 2 Rodadas"); 1 = every Rodada
 * @param pushUd                 how far each target is pushed back from the holder (reported; the caller moves)
 * @param possessAtZeroAutocontrole a target left at zero Autocontrole is Possuído for {@link #conditionRounds}
 */
@Builder
public record AuraPulse(int dice, int flat, DamageDescriptor descriptor, int reductionPerUd, boolean lifeSteal,
                        @Singular List<ConditionType> conditions, int conditionRounds, RestType healAsRest,
                        int autocontroleLoss, int everyNRounds, int pushUd, boolean possessAtZeroAutocontrole) {

    public AuraPulse {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        everyNRounds = Math.max(1, everyNRounds);
    }

    /** Whether it fires on round. */
    public boolean firesOn(final int round) {
        return round % everyNRounds == 0;
    }

    /** What a target distanceUd away takes from a pulse whose dice came to rolled. */
    public int damageAt(final int rolled, final int distanceUd) {
        if (descriptor == null) {
            return 0;
        }
        return Math.max(0, rolled + flat - reductionPerUd * Math.max(0, distanceUd - 1));
    }
}
