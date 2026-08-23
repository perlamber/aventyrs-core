package org.aventyrs.core.ability;

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

    // Two of the three branches are real now. The rest-recovery branch ("Descansos Verdadeiros
    // Longos ou superiores... +3PV") is real via resolveRestHitPointsBonus below — same
    // "Descanso Verdadeiro" == this codebase's own RestType tiers inference
    // FocusAbility#CANALIZADOR_DE_MANA's identical branch already makes (not confirmed rules
    // text). The "+1 non-cumulative Roubo de Vida" branch is real too, via
    // resolveLifeStealBonus below, now that org.aventyrs.core.sheet.LifeSteal exists —
    // LifeStealService#getTotalLifeSteal only ever adds this once, regardless of how many
    // active LifeSteal effects there are, and only when that total is already positive (this
    // ability doesn't grant Roubo de Vida on its own, only amplifies an already-active one).
    // TODO: the remaining "+2PV on spell/Título Aventyr PV healing" branch is still unbuilt —
    // needs a way to know a given CombatantSheet#heal call came from a spell or Título Aventyr
    // ability specifically (no healing-spell-effect representation exists, and no Título
    // Aventyr concept exists at all) — the identical gap FocusAbility#CANALIZADOR_DE_MANA's own
    // matching branch is still blocked on.
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
                return ENHANCED_DAMAGE_REDUCTION;
            }
            return BASE_DAMAGE_REDUCTION;
        }
    },

    // TODO
    RECUPERACAO_ASSOMBROSA("Os Efeitos Críticos, Correntes de Efeitos, e Efeitos de Magias e de Habilidades " +
            "nocivas que possuam Duração maior do que 3 Rodadas são encerradas em 3 Rodadas. Esta Habilidade não " +
            "afeta efeitos permanentes.");

    /** METABOLISMO_RAPIDO's own extra PV recovered on a Descanso Longo or better. */
    private static final int REST_HIT_POINTS_BONUS = 3;

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
