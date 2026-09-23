package org.aventyrs.core.title.giganteenfurecido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.MetamagicoFeat;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * The Habilidades/Supremas gated on Especialização 'Titã Enlouquecido'. Their "Requer N Habilidades"
 * clauses name that Especialização, so {@link AventyrTitleAbility#getRequiredOtherAbilitiesScope()}'s
 * default (this Especialização) is the right reading.
 */
@Getter
@AllArgsConstructor
public enum TitaEnlouquecidoAbility implements AventyrTitleAbility {

    // Requer Especialização 'Titã Enlouquecido'. Real through GritosDeGuerraInteraction; see there for
    // each Grito. "Enquanto estiver em Frenesi" and "Não é possível ativar Gritos de Guerra mais de uma
    // vez na mesma Rodada" are both refused in validate.
    GRITOS_DE_GUERRA(
            "Enquanto estiver em Frenesi você pode dar Gritos de Guerra que motivam seus aliados ou atormentam " +
            "seus inimigos. Ao ativar esta Habilidade escolha um dos efeitos abaixo: Grito de Desdenho: Todo o " +
            "dano causado a você e aos seus aliados, que estejam em Distância Média, é reduzido à metade " +
            "durante 1 Rodadas; Grito Espinhoso: Inimigos em Distância Curta sofrem 1d6 + Metade de seus " +
            "Multiplicador de PV pontos de Dano Físico Primordial, este dano é dobrado em inimigos adjacentes; " +
            "Grito Inspirador: Aliados em Distância Curta recebem os mesmos efeitos de seus Frenesis ativos, " +
            "exceto Gritos de Guerra, por 1 Rodada. Não é possível ativar Gritos de Guerra mais de uma vez na " +
            "mesma Rodada.",
            false, fixed(3), EgoCost.NONE, ActionCost.ofActionPoints(1),
            Optional.of(GritosDeGuerraInteraction.class), 0),

    // Requer Especialização 'Titã Enlouquecido' e o talento 'Arcanista' — the Talento half in
    // #isEligible(AventyrTitle, Character). "Custo de Ativação: +3PA, Tempo de Ativação: Variável,
    // conforme magia": the +3PA land on the empowered cast (SpellCastingServiceImpl adds
    // SpellEmpowerment#ACTION_POINT_SURCHARGE), so the activation itself is an Ação Livre. Real through
    // FrenesiArcanoInteraction: a one-cast SpellEmpowerment budget, "apenas uma vez a cada Rodada e
    // apenas enquanto Titã Enlouquecido estiver Ativo".
    FRENESI_ARCANO(
            "Você pode aumentar o Dano Base de suas magias em +1d6, então magias capazes de infligir 3d6 " +
            "pontos de dano são maximizadas. Alternativamente você pode aumentar a Duração de seus " +
            "Encantamentos e Maldições em +2 Rodadas. Frenesi Arcano pode ser ativado apenas uma vez a cada " +
            "Rodada e apenas enquanto Titã Enlouquecido estiver Ativo.",
            false, fixed(0), EgoCost.NONE, ActionCost.FREE_ACTION,
            Optional.of(FrenesiArcanoInteraction.class), 0) {
        @Override
        public boolean isEligible(final AventyrTitle title, final Character character) {
            return isEligible(title) && character.getFeats().contains(MetamagicoFeat.ARCANISTA);
        }
    },

    // Requer Especialização 'Titã Enlouquecido'. Passive, real through FrenesiInteraction: while Titã
    // Enlouquecido is active in a Frenesi, Categoria de Tamanho +1 more ("para um total de +2") and the
    // PD and PM Multiplicadores +1 (Frenzy's DETERMINATION_MULTIPLIER/MANA_MULTIPLIER).
    COLOSSO_ENFURECIDO(
            "Enquanto Titã Enlouquecido estiver ativo sua Categoria de Tamanho aumenta em +1 (para um total de " +
            "+2) e seus Multiplicadores de PD e PM aumentam em +1.",
            false, fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(), 0),

    // Requer 2 Habilidades de Titã Enlouquecido. Real through CataclismoElementalInteraction: the
    // elements are a multi-choice (TitleAbilityActivationRequest#getChoices), each adding +1PA
    // (ActionCost.dynamic). While it runs — for the rest of the Frenesi — AreaDamage#cataclysm adds a
    // 1-point Mágico Elemental hit per element to every character in Distância Curta (×2 adjacent)
    // after each Magia de Duração instantânea (SpellCastingResult#getAreaDamage) and each Grito de
    // Guerra; and the holder has RE against each chosen element (CombatantSheet
    // #getElementalResistanceInstances, read by DamageServiceImpl).
    CATACLISMO_ELEMENTAL(
            "Ao ativar esta Suprema você precisa escolher entre causar danos Elementais (Fogo, Lava, Terra, " +
            "Água, Frio/Gelo, Vento/Som, Eletricidade e Selvagem). Para cada tipo de dano escolhido Tempo de " +
            "Ativação desta Suprema aumenta em +1PA. Enquanto Cataclismo Elemental estiver ativo suas Magias " +
            "de Duração instantânea e Gritos de Guerra causam, como efeito adicional, dano Mágico Elemental " +
            "igual ao número de Elementos escolhido a todos os personagens em Distância Curta, este dano é " +
            "dobrado em personagens adjacentes. Você também recebe RE para resistir a todos os Elementos " +
            "escolhidos.",
            true, fixed(0), EgoCost.autocontrole(1), ActionCost.dynamic(1),
            Optional.of(CataclismoElementalInteraction.class), 2);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final EgoCost egoCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final int requiredOtherAbilities;

    @Override
    public Optional<AventyrTitleSpecialization> getRequiredSpecialization() {
        return Optional.of(GiganteEnfurecidoSpecialization.TITA_ENLOUQUECIDO);
    }
}
