package org.aventyrs.core.title.curandeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Curandeiro's Despertar, "Os dons de um Curandeiro", as an <i>activated</i> trait — always held
 * (through {@link Curandeiro#getAllAbilities()}) but never among {@link Curandeiro#getAbilities()},
 * so it counts toward no "outras Habilidades" prerequisite. The same shape as {@code
 * GiganteEnfurecidoDespertar}. Its Vantagem half is passive and lives on {@link
 * Curandeiro#resolveSkillRollBonus}.
 */
@Getter
@AllArgsConstructor
public enum CurandeiroDespertar implements AventyrTitleAbility {

    // "Custo de Ativação: Nenhum", "Tempo de Ativação: 3PA". Real through DonsDoCurandeiroInteraction:
    // the caller rolls Medicina e Cura against DonsDoCurandeiroInteraction.DIFFICULTY and passes the
    // outcome; a success heals as a Descanso Curto (Longo with Beijo de Boros, outside combat); either
    // way the target cannot be touched again until the Curandeiro's Descanso Longo.
    OS_DONS_DE_UM_CURANDEIRO(
            "Você pode realizar uma rolagem de 'Medicina e Cura', com GD Difícil, em um alvo ferido, se for " +
            "bem-sucedido o alvo recupera PV como se passasse por um Descanso Curto, se Ativada fora de combate " +
            "esta Habilidade pode receber a Corrente de Efeitos – Beijo de Boros: o Alvo recupera PV como se " +
            "passasse por um Descanso Longo. Você não pode afetar um mesmo personagem desta forma até que você " +
            "passe por um Descanso Longo, mesmo que você não tenha sido bem-sucedido em sua primeira tentativa.",
            fixed(0), EgoCost.NONE, ActionCost.ofActionPoints(3), Optional.of(DonsDoCurandeiroInteraction.class));

    private final String description;
    private final PDCost PDCost;
    private final EgoCost egoCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
}
