package org.aventyrs.core.title.curandeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * The Habilidades/Supremas gated on {@link CurandeiroSpecialization#MEDICO_DE_GUERRA}. None is real
 * yet; each names the system it waits on.
 */
@Getter
@AllArgsConstructor
public enum MedicoDeGuerraAbility implements AventyrTitleAbility {

    // Requer Médico de Guerra. Passive.
    // TODO: "não permitem Reações de seus inimigos" — nothing fires or suppresses a Reação (the
    //  movement-triggered Reações gap in CLAUDE.md).
    TRATAMENTO_FURTIVO(
            "Seus efeitos que permitam recuperar PV de seus aliados não permitem Reações de seus inimigos.",
            false, fixed(0), ActionCost.NONE, Optional.empty(),
            Optional.of(CurandeiroSpecialization.MEDICO_DE_GUERRA), 0),

    // Requer Médico de Guerra. 2PD, +1PA.
    // TODO: "+4 em suas Defesas por 1 Rodada" to healer and target — expressible as a DEFESAS Blessing,
    //  but nothing ties an activation to the heal it rides on, and "Broto ou superior" needs the heal's
    //  Magia rung checked at that moment.
    CURA_PROTETORA(
            "Suas Magias (Broto ou superior) e Habilidades de Curandeiro, em adição a quaisquer efeitos de " +
            "recuperação de PV também concedem a você e ao alvo Bônus de +4 em suas Defesas por 1 Rodada, " +
            "efeito não cumulativo.",
            false, fixed(2), ActionCost.ofActionPoints(1), Optional.empty(),
            Optional.of(CurandeiroSpecialization.MEDICO_DE_GUERRA), 0),

    // Requer Médico de Guerra. 2PD, +1PA.
    // TODO: "Efeitos de Encantamentos lançados sobre um personagem aliado adicionalmente recuperam Vigor
    //  PV" — an Encantamento-typed Magia is not a sheet.Enchantment and reaches no door this could hang
    //  off (the Efeito de Encantamento row in CLAUDE.md).
    ENCANTO_REGENERATIVO(
            "Suas Magias e Efeitos de Encantamentos lançados sobre um personagem aliado adicionalmente " +
            "recuperam Vigor PV do alvo. Esta Habilidade deve ser utilizada em conjunto com a Conjuração ou " +
            "Ativação do Efeito.",
            false, fixed(2), ActionCost.ofActionPoints(1), Optional.empty(),
            Optional.of(CurandeiroSpecialization.MEDICO_DE_GUERRA), 0),

    // Requer 2 Habilidades de Médico de Guerra. Passive.
    // TODO: "seus Descansos contam como uma Categoria superior" — RestService#applyRest has no hook
    //  upgrading the RestType it was given.
    // TODO: "-1PA" permanently on healing activations — the same missing activation-time hook as
    //  CurandeiroAbility#CURANDEIRO_VELOZ.
    DOUTOR_DE_ELDUR(
            "Sempre que descansar, seus Descansos contam como uma Categoria superior. O Tempo de Ativação " +
            "Habilidades de Médico de Guerra e de Conjuração de Magias que permitam a recuperação de PV tem " +
            "seu Tempo de Ação reduzidos permanentemente em -1PA.",
            true, fixed(0), ActionCost.NONE, Optional.empty(),
            Optional.of(CurandeiroSpecialization.MEDICO_DE_GUERRA), 2);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<AventyrTitleSpecialization> requiredSpecialization;
    private final int requiredOtherAbilities;
}
