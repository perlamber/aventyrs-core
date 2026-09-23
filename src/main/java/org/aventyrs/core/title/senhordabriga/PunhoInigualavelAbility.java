package org.aventyrs.core.title.senhordabriga;

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
 * The Habilidades/Suprema gated on {@link SenhorDaBrigaSpecialization#PUNHO_INIGUALAVEL}
 * specifically — every constant names that Especialização as its {@code requiredSpecialization},
 * and {@link #GRANDE_MESTRE_DAS_BRIGAS}'s "2 Habilidades de Punho Inigualável" is counted only
 * against siblings of this enum.
 *
 * <p>Each Habilidade carries a "Grande Mestre das Brigas" upgrade that applies while its holder
 * also holds that Suprema — read through {@link SenhorDaBriga#holdsGrandeMestreDasBrigas()}.
 */
@Getter
@AllArgsConstructor
public enum PunhoInigualavelAbility implements AventyrTitleAbility {

    // Requer Especialização Punho Inigualável. Real through ImpactoElementalInteraction: the element
    // is chosen once and held on the Título (SenhorDaBriga#chooseImpactoElementalElement — "depois
    // de escolhido não é possível mudar o Elemento"); the 1PD grants a combat-scoped budget of
    // 2 + other Punho Inigualável Habilidades/Supremas held, in attacks
    // (CombatantSheet#grantEnhancedAttacksForCombat — "cancelada ao final da Cena mesmo se ainda
    // houver quantidade de ataques disponíveis"). While charges remain, each Arma Natural attack
    // gets Vantagem (SenhorDaBriga#resolveAttackRollBonus) and is typed Dano Físico Elemental of the
    // element (SenhorDaBriga#resolveAttackModifiers); the caller spends one per attack through
    // TitleAttackModifiers#consumeCharges. Grande Mestre das Brigas: rolled against DM (a
    // DefenseType.MAGIC the caller rolls against) and typed Dano Mágico of the element.
    // TODO Grande Mestre's "seu Efeito Crítico é alterado para Cataclismo" is reported as
    // TitleAttackModifiers#criticalEffectOverride, but CriticalEffectType.CATACLISMO has no class and
    // no weapon's own Efeito Crítico is applied either, so nothing changes at the table yet.
    IMPACTO_ELEMENTAL(
            "Escolha um Elemento entre Fogo, Magma, Terra, Água, Gelo, Ar, Eletricidade ou Natural, " +
            "depois de escolhido não é possível mudar o Elemento. Você recebe Vantagem em suas rolagens " +
            "de Perícia de Ataque com Armas Naturais e o tipo de dano causado muda para Dano Físico " +
            "Elemental do tipo escolhido. A Duração de Impacto Elemental é igual à 2+ o número de outras " +
            "Habilidades e Supremas de Punho Inigualável que você possuir, a Habilidade é cancelada ao " +
            "final da Cena mesmo se ainda houver quantidade de ataques disponíveis. Grande Mestre das " +
            "Brigas: Enquanto Impacto Elemental estiver ativo seus ataques com Armas Naturais são rolados " +
            "contra a DM de seus alvos, causam danos mágicos e seu Efeito Crítico é alterado para " +
            "Cataclismo.",
            false, fixed(1), ActionCost.ofActionPoints(2), Optional.of(ImpactoElementalInteraction.class),
            Optional.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL), 0),

    // Requer Especialização Punho Inigualável. Real through RolamentoOfensivoInteraction: requires a
    // target enemy at Distância Curta (the activator's own SceneContext), and reports how far the
    // roll carries (InteractionResult#getTeleportation — 2UD, 3UD under Grande Mestre) for the
    // caller to move the token, since this core does no geometry. Grande Mestre's "Vantagem em
    // rolagens de Perícia de Ataque com Armas Naturais por 1 Rodada" is a one-Rodada
    // CombatantSheet#openActivationWindow read by SenhorDaBriga#resolveAttackRollBonus.
    // TODO "Bônus de +3 em suas Defesas para resistir aos ataques do inimigo que você se aproximou":
    // a Defesa bonus scoped to one attacker has no carrier — a Blessing reaches every attack alike,
    // and DefenseService never learns who is attacking.
    ROLAMENTO_OFENSIVO(
            "Você pode rolar 2UD em direção a um inimigo em Distância Curta. Você recebe Bônus de +3 em " +
            "suas Defesas para resistir aos ataques do inimigo que você se aproximou por 1 Rodada. " +
            "Grande Mestre das Brigas: A distância de seu Rolamento Ofensivo muda para 3UD e você recebe " +
            "Vantagem em rolagens de Perícia de Ataque com Armas Naturais por 1 Rodada.",
            false, fixed(1), ActionCost.FREE_ACTION, Optional.of(RolamentoOfensivoInteraction.class),
            Optional.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL), 0),

    // Requer Especialização Punho Inigualável. Real through AgarrarEDerrubarInteraction: the 2PD
    // grants a one-attack budget (CombatantSheet#grantEnhancedAttacks) — "Como parte da Ativação
    // desta Habilidade você deve fazer um ataque com uma de suas Armas Naturais" — and while it
    // remains, an Arma Natural attack gets Vantagem (SenhorDaBriga#resolveAttackRollBonus) and the
    // effect.AgarrarEDerrubar Corrente (SenhorDaBriga#resolveAttackModifiers), which knocks the
    // target Caído when it is at most two Categorias de Tamanho larger. Grande Mestre das Brigas'
    // "Uma vez por Rodada você pode desferir um ataque com Arma Natural contra um alvo Caído como
    // Ação Livre" is a permission the caller offers, answered by
    // SenhorDaBriga#grantsFreeAttackAgainstFallen — this core charges no PA either way.
    AGARRAR_E_DERRUBAR(
            "Sua especialidade é derrubar seus oponentes para ampliar suas vantagens em combate. Como " +
            "parte da Ativação desta Habilidade você deve fazer um ataque com uma de suas Armas " +
            "Naturais, você recebe Vantagem nesta rolagem de Perícia. Como efeito adicional este ataque " +
            "recebe a Corrente de Efeitos – Agarrar e Derrubar: O Alvo do seu ataque adicionalmente é " +
            "derrubado, recebendo a condição Caído. Apenas personagens com até o máximo duas Categorias " +
            "de Tamanho superior à sua podem ser alvos desta Corrente de Efeitos. Grande Mestre das " +
            "Brigas: Uma vez por Rodada você pode desferir um ataque com Arma Natural contra um alvo " +
            "Caído como Ação Livre.",
            false, fixed(2), ActionCost.ofActionPoints(3), Optional.of(AgarrarEDerrubarInteraction.class),
            Optional.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL), 0),

    // Requer 2 Habilidades de Punho Inigualável — the Especialização requirement is an inference from
    // the class-level statement, not text repeated on this constant, the same reading
    // AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA takes. Passive: "Suas Habilidades de Punho
    // Inigualável recebem os benefícios de Grande Mestre das Brigas" — a flag, read through
    // SenhorDaBriga#holdsGrandeMestreDasBrigas by each Habilidade above.
    GRANDE_MESTRE_DAS_BRIGAS(
            "Suas Habilidades de Punho Inigualável recebem os benefícios de Grande Mestre das Brigas.",
            true, fixed(0), ActionCost.NONE, Optional.empty(),
            Optional.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL), 2);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<AventyrTitleSpecialization> requiredSpecialization;
    private final int requiredOtherAbilities;
}
