package org.aventyrs.core.ability;

import org.aventyrs.core.character.Resistance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CombatantSheet;

@Getter
@AllArgsConstructor
public enum VigorAbility implements AttributeAbility {

    SOBRE_HUMANO("Seu Multiplicador de Pontos de Vida é aumentado em +1.") {
        @Modifier(ModifierType.LIFE_MULTIPLIER)
        public int lifeMultiplierBonus() {
            return 1;
        }
    },

    SANGUE_DE_GIGANTE("Sua Categoria de Tamanho é aumentada em +1.") {
        @Modifier(ModifierType.SIZE_CATEGORY)
        public int sizeCategoryBonus() {
            return 1;
        }
    },

    // All three branches are real. The rest branch via resolveRestHitPointsBonus — a real Descanso
    // only (RecoveryOrigin#DESCANSO): a Magia healing "como se passasse por um Descanso" doesn't pay
    // it (table ruling, 2026-10-16). The +2PV via resolveHealingReceivedBonus, added in
    // AbstractCombatantSheet#heal for a heal whose HealingSource is a Magia or a Habilidade de Título
    // (never Roubo de Vida, which takes healFromLifeSteal). The +1 Roubo de Vida via
    // resolveLifeStealBonus, which LifeStealService adds once, and only to an already-positive total.
    METABOLISMO_RAPIDO("Descansos Verdadeiros Longos ou superiores permitem que você recupere +3PV adicionais. " +
            "Magias e Habilidades de Títulos Aventyr que te façam recuperar PV recuperam +2PV adicionais (não " +
            "aplicável a efeitos de Roubo de Vida). Personagens sob efeitos de Roubo de Vida têm seu efeito de " +
            "Roubo de Vida aumentado em +1 (não cumulativo).") {
        @Override
        public int resolveRestHitPointsBonus(final RestType restType) {
            return restType.isAtLeast(RestType.LONGO) ? REST_HIT_POINTS_BONUS : 0;
        }

        @Override
        public int resolveLifeStealBonus() {
            return LIFE_STEAL_BONUS;
        }

        @Override
        public int resolveHealingReceivedBonus(final org.aventyrs.core.sheet.HealingSource source) {
            return source != null && source.isMagiaOrTituloAbility() ? SPELL_HEALING_BONUS : 0;
        }
    },
    // The base -1 applies to any Dano Físico regardless of source (even an unknown attacker —
    // source == null just skips the size comparison below, falling back to the base value,
    // never granting the -2 without evidence for it). The -2 branch reads both sides' raw
    // Character#getSizeCategory() directly, same simplification AnoesRacialAbility
    // #ABATEDORES_DE_GIGANTES's own size comparison already makes (not CharacterSizeService's
    // "effective," Sangue-de-Gigante-shifted category).
    RIGIDEZ_DA_MONTANHA("Todo o Dano Físico causado a você é reduzido em -1; se o dano for causado por " +
            "personagens de Categorias de Tamanho inferiores à sua, ao invés disso o dano é reduzido em -2.") {
        @Override
        public int resolveDamageReduction(final DamageType damageType, final CombatantSheet source, final CombatantSheet target) {
            if (damageType != DamageType.FISICO) {
                return 0;
            }
            if (source != null && target != null
                    && source.getCharacter().getSizeCategory().getCategory() < target.getCharacter().getSizeCategory().getCategory()) {
                return Resistance.RDS.instances(ENHANCED_DAMAGE_REDUCTION);
            }
            return Resistance.RDS.instances(BASE_DAMAGE_REDUCTION);
        }
    },

    // Real via resolveHarmfulDurationCap: every harmful effect (TemporaryEffect#isHarmful — a Condição but
    // Escondido, a malus, a continuous-damage effect, a harmful Encantamento) landing with more than 3 counted
    // Rodadas is cut to 3 as it lands. "Efeitos permanentes" are the open-ended ones and those scoped to the
    // combat or a Descanso (table ruling, 2026-10-16); a countdown started later on an already-held effect
    // (Sustained trailing Rodadas) is not re-judged.
    RECUPERACAO_ASSOMBROSA("Os Efeitos Críticos, Correntes de Efeitos, e Efeitos de Magias e de Habilidades " +
            "nocivas que possuam Duração maior do que 3 Rodadas são encerradas em 3 Rodadas. Esta Habilidade não " +
            "afeta efeitos permanentes.") {
        @Override
        public java.util.OptionalInt resolveHarmfulDurationCap() {
            return java.util.OptionalInt.of(HARMFUL_DURATION_CAP);
        }
    };

    /** RECUPERACAO_ASSOMBROSA's ceiling on a harmful effect's Duração, in Rodadas. */
    private static final int HARMFUL_DURATION_CAP = 3;

    /** METABOLISMO_RAPIDO's own extra PV recovered on a Descanso Longo or better. */
    private static final int REST_HIT_POINTS_BONUS = 3;

    /** METABOLISMO_RAPIDO's own extra PV on a heal from a Magia or a Habilidade de Título. */
    private static final int SPELL_HEALING_BONUS = 2;

    /** METABOLISMO_RAPIDO's own flat, non-stacking bonus to an already-active Roubo de Vida. */
    private static final int LIFE_STEAL_BONUS = 1;

    /** RIGIDEZ_DA_MONTANHA's own base Dano Físico reduction. */
    private static final int BASE_DAMAGE_REDUCTION = 1;

    /** RIGIDEZ_DA_MONTANHA's own reduction once the attacker's Categoria de Tamanho is lower. */
    private static final int ENHANCED_DAMAGE_REDUCTION = 2;

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.VIGOR;
    }
}
