package org.aventyrs.core.skill.profissao;

import org.aventyrs.core.ego.EquipmentPointExpense;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.ExcellencyTier;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

/**
 * Profissão's automatic Excelência bonuses, granted once a character's Profissão graduation
 * reaches each {@link ExcellencyTier}'s threshold.
 */
@Getter
@AllArgsConstructor
public enum ProfissaoExcellency implements SkillExcellency {

    // Real (core 0.1.6.5): -1PE on buying and forging, through resolveEquipmentPointDiscount, floored at 1PE by
    // character.services.EquipmentPointPricing#discounted.
    FOCADO(ExcellencyTier.FOCADO, "Custos de criação e compras de Equipamentos reduzido em " +
            "-1PE (mínimo 1PE).") {
        @Override
        public int resolveEquipmentPointDiscount(final EquipmentPointExpense kind) {
            return creationOrPurchase(kind) ? FOCADO_DISCOUNT : 0;
        }
    },

    PRODIGIO(ExcellencyTier.PRODIGIO, "GD reduzido em -1 nível.") {
        @Override
        public int getDifficultyReduction() {
            return 1;
        }
    },

    // Real (core 0.1.6.5): -2PE more on top of Focado's -1PE, totaling -3PE, still floored at 1PE — per this
    // codebase's convention, "muda para -3PE" is the cumulative total across tiers, not a standalone override (see
    // ArtesExcellency.LENDA, whose old/new rules text confirms this reading). Both tiers are unlocked at Lenda, and
    // EquipmentPointPricing sums them.
    LENDA(ExcellencyTier.LENDA, "Redução dos Custos de criação e Compras de Equipamentos " +
            "muda para -3PE (mínimo 1PE).") {
        @Override
        public int resolveEquipmentPointDiscount(final EquipmentPointExpense kind) {
            return creationOrPurchase(kind) ? LENDA_TOTAL_DISCOUNT - FOCADO_DISCOUNT : 0;
        }
    };

    /** FOCADO's "-1PE". */
    private static final int FOCADO_DISCOUNT = 1;

    /** LENDA's "muda para -3PE" — the total across both tiers. */
    private static final int LENDA_TOTAL_DISCOUNT = 3;

    /** "Custos de criação e compras" — a forge or a store purchase, never an Aprimoramento install. */
    private static boolean creationOrPurchase(final EquipmentPointExpense kind) {
        return kind == EquipmentPointExpense.PURCHASE || kind == EquipmentPointExpense.PRODUCTION;
    }

    private final ExcellencyTier tier;
    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PROFISSAO;
    }
}
