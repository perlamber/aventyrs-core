package org.aventyrs.core.title.bruxo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * The Habilidades/Supremas gated on {@link BruxoSpecialization#ORACULO_ABISSAL}. Each teaches a
 * type-filtered Misticismo and changes the holder's invocations. "Habilidades ou Suprema de Oráculo
 * Abissal" in the scalings counts these constants alone ({@link Bruxo#getAbilityCount}).
 */
@Getter
@AllArgsConstructor
public enum OraculoAbissalAbility implements MisticismoTeacher {

    // Requer Oráculo Abissal. Passive. Misticismo real; the attack changes are read by
    // Bruxo#resolveSummonEnhancement.
    MALDICAO_DAS_CHAMAS_DO_NORTE(
            "Você aprende um novo Misticismo, escolhido entre magias do tipo Elemental: Fogo ou Profana. Rolagens " +
            "de Ataque de suas invocações são efetuadas contra a DM de seus alvos, o tipo de dano causado muda para " +
            "Dano Mágico Elemental: Fogo, inimigos imunes a fogo ainda sofrem metade dos danos. Como efeito " +
            "adicional o ataque de suas invocações recebe a Corrente de Efeitos – Fogo Vivo: Este ataque aplica o " +
            "Efeito Crítico Menor de Inflamar.",
            false, MisticismoFilter.elementOr(ElementalType.FOGO, MagicType.PROFANA), 0),

    // Requer Oráculo Abissal. Passive. Misticismo real; the RDS is read by
    // Bruxo#resolveSummonEnhancement.
    MALDICAO_DO_DESERTO_DO_OESTE(
            "Você aprende um novo Misticismo, escolhido entre magias do tipo Elemental: Terra ou Profana. Suas " +
            "invocações recebem RDS 2, a Redução de Danos Sofridos aumenta em +1 para cada Habilidades ou Suprema " +
            "de Oráculo Abissal que você possuir.",
            false, MisticismoFilter.elementOr(ElementalType.TERRA, MagicType.PROFANA), 0),

    // Requer Oráculo Abissal. Passive. Misticismo real; the critical changes are read by
    // Bruxo#resolveSummonEnhancement.
    BENCAO_DOS_VULCOES_DO_NOROESTE(
            "Você aprende um novo Misticismo, escolhido entre magias do tipo Elemental: Magma ou Profana. Suas " +
            "invocações recebem Cataclismo como Efeito Crítico adicional em seus ataques, também recebem +1d6 de " +
            "Dano Físico Elemental: Magma em suas rolagens de Dano Crítico. A Margem Crítica Menor dos ataques de " +
            "suas invocações é aumentada em +1, então em +1 para cada 2 Habilidades ou Suprema de Oráculo Abissal " +
            "que você possuir.",
            false, MisticismoFilter.elementOr(ElementalType.MAGMA, MagicType.PROFANA), 0),

    // Requer 2 Habilidades de Oráculo Abissal. Passive. Misticismo real; the arrival burst is reported and
    // the rest is read by Bruxo#resolveSummonEnhancement.
    BENCAO_DOS_RAIOS_DO_NORDESTE(
            "Você aprende um novo Misticismo, escolhido entre magias do tipo Elemental: Eletricidade ou Profana. " +
            "Suas invocações surgem com efeitos relampejantes, infligindo 1d6 pontos de Dano Mágico Elemental: " +
            "Eletricidade aos inimigos adjacentes e todos os seus equipamentos (efeito não cumulativo, caso " +
            "múltiplas invocações afetem um mesmo personagem). Criaturas invocadas desta forma podem agir no Turno " +
            "em que foram invocadas e recebem +2PA, adicionalmente seus ataques infligem danos adicionais igual ao " +
            "número de Habilidades de Oráculo Abissal que você possuir e causam danos Sagrados em substituição a " +
            "quaisquer danos Elementais.",
            true, MisticismoFilter.elementOr(ElementalType.ELETRICIDADE, MagicType.PROFANA), 2);

    private final String description;
    private final boolean supreme;
    private final MisticismoFilter filter;
    private final int requiredOtherAbilities;

    @Override
    public Optional<MisticismoFilter> getMisticismoFilter() {
        return Optional.of(filter);
    }

    @Override
    public PDCost getPDCost() {
        return fixed(0);
    }

    @Override
    public ActionCost getActionPointCost() {
        return ActionCost.NONE;
    }

    @Override
    public Optional<AventyrTitleSpecialization> getRequiredSpecialization() {
        return Optional.of(BruxoSpecialization.ORACULO_ABISSAL);
    }

    @Override
    public Optional<Class<? extends Interaction>> getInteractionClass() {
        return Optional.empty();
    }
}
