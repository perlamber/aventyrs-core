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
 * The Habilidades/Supremas gated on {@link CurandeiroSpecialization#MEDICO_DE_GUERRA}.
 */
@Getter
@AllArgsConstructor
public enum MedicoDeGuerraAbility implements AventyrTitleAbility {

    // Requer Médico de Guerra. Passive. Real by construction: no ReactionTrigger fires on a heal — the
    // only Reações this core offers answer attacks and damage — so a heal already provokes none. Should
    // a heal-triggered Reação ever be added, its availability check must consult this constant.
    TRATAMENTO_FURTIVO(
            "Seus efeitos que permitam recuperar PV de seus aliados não permitem Reações de seus inimigos.",
            false, fixed(0), ActionCost.NONE, Optional.empty(),
            Optional.of(CurandeiroSpecialization.MEDICO_DE_GUERRA), 0),

    // Requer Médico de Guerra. 2PD, +1PA. Real through CuraProtetoraInteraction: activated beside the heal
    // it rides on, it grants the activator and the target +4 Defesas for 1 Rodada, sourced so a second
    // activation renews rather than stacks ("efeito não cumulativo"). Pairing it with a Broto-or-higher
    // Magia or a Habilidade de Curandeiro is the caller's.
    CURA_PROTETORA(
            "Suas Magias (Broto ou superior) e Habilidades de Curandeiro, em adição a quaisquer efeitos de " +
            "recuperação de PV também concedem a você e ao alvo Bônus de +4 em suas Defesas por 1 Rodada, " +
            "efeito não cumulativo.",
            false, fixed(2), ActionCost.ofActionPoints(1), Optional.of(CuraProtetoraInteraction.class),
            Optional.of(CurandeiroSpecialization.MEDICO_DE_GUERRA), 0),

    // Requer Médico de Guerra. 2PD, +1PA. Real through EncantoRegenerativoInteraction: activated "em
    // conjunto" with the Magia or Efeito de Encantamento it rides on, the ally it targets heals its own
    // Vigor in PV, sourced as this Habilidade.
    ENCANTO_REGENERATIVO(
            "Suas Magias e Efeitos de Encantamentos lançados sobre um personagem aliado adicionalmente " +
            "recuperam Vigor PV do alvo. Esta Habilidade deve ser utilizada em conjunto com a Conjuração ou " +
            "Ativação do Efeito.",
            false, fixed(2), ActionCost.ofActionPoints(1), Optional.of(EncantoRegenerativoInteraction.class),
            Optional.of(CurandeiroSpecialization.MEDICO_DE_GUERRA), 0),

    // Requer 2 Habilidades de Médico de Guerra. Passive. Real: Curandeiro#upgradesRests makes
    // RestService#applyRest treat each of the holder's Descansos one category higher (Total stays
    // Total); Curandeiro#resolveActivationActionPointReduction/#resolveCastingActionPointReduction take
    // -1PA off every Médico de Guerra activation and every Magia that heals, never below 1PA.
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
