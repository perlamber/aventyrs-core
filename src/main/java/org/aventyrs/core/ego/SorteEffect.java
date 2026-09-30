package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.EgoPointType;

/**
 * What a Ponto de Sorte does to one Perícia roll (2.5 Ego › Sorte; {@code docs/rules/ego.txt}). Carried on the
 * roll ({@code SkillRoll#getSorteEffects()}), paid by {@code EgoPointsService#applySorte}/{@code #rerollWithSorte},
 * and applied wherever the roll is resolved — a plain Perícia roll, an attack ({@code AttackDelivery}) and a
 * defence ({@code AttackReceiver}). A temporary point can never buy a permanent effect.
 *
 * <p>The scene changes ("alterações sutis / drásticas nas cenas narradas") are narrative: spend the point with
 * {@code EgoPointsService#useEgoPointsForEffect} and the Narrador describes the rest.
 */
@Getter
@AllArgsConstructor
public enum SorteEffect {
    /**
     * "Refazer uma rolagem de Perícia; rolagens feitas dessa forma são feitas em Vantagem" — new dice, and the
     * roll gets {@code Skill#ADVANTAGE_BONUS}.
     */
    REROLL_WITH_ADVANTAGE(EgoPointType.TEMPORARY),
    /**
     * "Solicitar ao Narrador reduzir o GD de uma rolagem efetuada contra um PdN." The Narrador's approval is the
     * gate — nothing here checks that the roll is against a PdN. ⚠️ Read as one nível ({@link
     * #DIFFICULTY_REDUCTION_LEVELS}); the text gives no amount. On an attack against a Defesa it is reported
     * unapplied, like every GD reduction there.
     */
    DIFFICULTY_REDUCTION(EgoPointType.TEMPORARY),
    /**
     * "Escolher ser bem-sucedido numa rolagem de Perícia, independente do resultado dos dados. É um Acerto
     * Crítico Menor." Overrides the dice (the Cego 1d6 included), never a rule that is not dice — a Defeito's
     * automatic failure, an attack the defender is immune to, a defence Trava Mental forbids.
     */
    FORCED_SUCCESS(EgoPointType.PERMANENT),
    /**
     * "Desencadear suas Correntes de Efeitos e Efeitos Críticos Maiores em uma rolagem de Perícia bem-sucedida."
     * On a hit (or a successful defence) the Corrente fires whatever the margin and the Efeitos Críticos apply at
     * Maior severity. What forbids them outright still does (Frenesi Assustador's suppression). ⚠️ Read as the
     * effects alone: the roll's own {@code CriticalResult} is unchanged, so Vantagem em Danos and "Sucesso Crítico
     * Maior" triggers (Destino Favorável) are not granted by it.
     */
    UNLEASHED_CRITICALS(EgoPointType.PERMANENT);

    /** What {@link #DIFFICULTY_REDUCTION} eases the GD by — see its javadoc. */
    public static final int DIFFICULTY_REDUCTION_LEVELS = 1;

    private final EgoPointType pointType;
}
