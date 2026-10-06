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
 * The Habilidades/Supremas gated on {@link BruxoSpecialization#ILUMINADO}. Each teaches a
 * type-filtered Misticismo and changes the holder's invocations. "Habilidades ou Suprema de
 * Iluminado" in the scalings counts these constants alone ({@link Bruxo#getAbilityCount}).
 */
@Getter
@AllArgsConstructor
public enum IluminadoAbility implements MisticismoTeacher {

    // Requer Iluminado. Passive. Misticismo real; the flight clause is read by
    // Bruxo#resolveSummonEnhancement.
    BENCAO_DO_VENTO_DO_LESTE(
            "Você aprende um novo Misticismo, escolhido entre magias do tipo Elemental: Ar ou Divina. Suas " +
            "invocações podem utilizar 3PA para criarem asas mágicas, após isso recebem Movimento Base de Voo por " +
            "1d6 Rodadas, a Duração do voo aumenta em +1 Rodada para cada 2 Habilidades ou Suprema de Iluminado " +
            "que você possuir.",
            false, MisticismoFilter.elementOr(ElementalType.VENTO, MagicType.DIVINA), 0),

    // Requer Iluminado. Passive. Misticismo real; the regeneration is read by
    // Bruxo#resolveSummonEnhancement.
    BENCAO_DO_MAR_DO_SUL(
            "Você aprende um novo Misticismo, escolhido entre magias do tipo Elemental: Água ou Divina. Suas " +
            "invocações são capazes de regenerar as próprias feridas, curando 2PV no final de cada Rodada, a " +
            "regeneração aumenta em +1PV para cada 2 Habilidades ou Suprema de Iluminado que possuir.",
            false, MisticismoFilter.elementOr(ElementalType.AGUA, MagicType.DIVINA), 0),

    // Requer Iluminado. Passive. Misticismo real; the size increase is read by
    // Bruxo#resolveSummonEnhancement.
    MALDICAO_DO_PANTANO_DO_SUDESTE(
            "Você aprende um novo Misticismo, escolhido entre magias do tipo Elemental: Natural ou Divina. Suas " +
            "invocações são maiores, em tamanho, que o normal. A Categoria de Tamanho de suas invocações é " +
            "aumentada em +2, então em +1 para cada duas Habilidades ou Suprema de Iluminado que você possuir.",
            false, MisticismoFilter.elementOr(ElementalType.NATURAL, MagicType.DIVINA), 0),

    // Requer 2 Habilidades de 'Iluminado'. Passive. Misticismo real; the comet and the Aura Profana are
    // reported by Bruxo#resolveSummonEnhancement for the caller to deal.
    MALDICAO_DA_NEVASCA_DO_SUDOESTE(
            "Você aprende um novo Misticismo, escolhido entre magias do tipo Elemental: Gelo ou Divina. Apenas em " +
            "cenários de céu aberto, ao realizar uma invocação você pode substituir a rolagem de Domínio do Mana " +
            "convencional por uma rolagem de Ataque à Distância – Distância Média e Área de Efeito – Adjacências " +
            "(contra GD ou DM dos Alvos, o que for maior), se o fizer um cometa cairá dos céus sobre os alvos, " +
            "infligindo 1d6 pontos de dano Mágico Elemental: Gelo, o dano aumenta em +2 para cada Habilidade de " +
            "Iluminado que possuir. Após a queda do meteoro a camada de gelo explode e de seu interior surge a " +
            "invocação. Criaturas invocadas desta forma emitem uma Aura Profana, personagens adjacentes (exceto " +
            "você) sofrem 3 Pontos de Danos Mágicos Profanos no início de cada Rodada.",
            true, MisticismoFilter.elementOr(ElementalType.GELO, MagicType.DIVINA), 2);

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
        return Optional.of(BruxoSpecialization.ILUMINADO);
    }

    @Override
    public Optional<Class<? extends Interaction>> getInteractionClass() {
        return Optional.empty();
    }
}
