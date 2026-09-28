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
 * The Habilidades/Supremas gated on {@link CurandeiroSpecialization#MARTIR_ALTRUISTA}.
 */
@Getter
@AllArgsConstructor
public enum MartirAltruistaAbility implements AventyrTitleAbility {

    // Requer Mártir Altruísta. Passive. Real through Curandeiro#permitsHitPointPayment: a Habilidade de
    // Curandeiro aimed at another character pays its PD in PV (TitleCostPayment#HIT_POINTS among the
    // request's choices), and a single-target Magia Divina/Natural cast on a non-hostile other character
    // reports its PM as SpellCastingResult#getVitalityCost (SpellCastRequest#payManaWithHitPoints). Either
    // way the PV go through CombatantSheet#payWithVitality, locked until a Descanso Verdadeiro.
    TRANSFERIR_VITALIDADE(
            "Você pode utilizar PV em substituição à PM e PD para ativar Habilidades de Curandeiro ou conjurar " +
            "Magias Divinas e Naturais de único alvo, mas apenas quando os alvos forem personagens aliados. PV " +
            "perdidos desta forma podem ser recuperados apenas com Descansos Verdadeiros.",
            false, fixed(0), ActionCost.NONE, Optional.empty(),
            Optional.of(CurandeiroSpecialization.MARTIR_ALTRUISTA), 0),

    // Requer Mártir Altruísta. 1PD, 3PA. Real through TransferirDeterminacaoInteraction: PD moved at the
    // touch (the ally takes what it is missing), or 1 temporary Autocontrole lent until the end of the
    // Cena (CombatantSheet#receiveEgoLoan), returned if unused and lost if used.
    TRANSFERIR_DETERMINACAO(
            "Para ativar esta Habilidade você deve escolher entre transferir qualquer quantidade de PD que você " +
            "possua, ou temporariamente 1 de seus Pontos de Autocontrole, para um aliado, a transferência é " +
            "realizada ao toque. Pontos de Autocontrole transferidos desta forma são perdidos ou devolvidos se " +
            "não forem utilizados até o fim da Cena.",
            false, fixed(1), ActionCost.ofActionPoints(3), Optional.of(TransferirDeterminacaoInteraction.class),
            Optional.of(CurandeiroSpecialization.MARTIR_ALTRUISTA), 0),

    // Requer Mártir Altruísta. 1PD, 3PA. Real through TransferirEssenciaInteraction — the PM/Sorte twin of
    // TRANSFERIR_DETERMINACAO.
    TRANSFERIR_ESSENCIA(
            "Para ativar esta Habilidade você deve escolher entre transferir qualquer quantidade de PM que você " +
            "possua, ou temporariamente 1 de seus Pontos de Sorte, para um aliado, a transferência é realizada " +
            "ao toque. Pontos de Sorte transferidos desta forma são perdidos ou devolvidos se não forem " +
            "utilizados até o fim da Cena.",
            false, fixed(1), ActionCost.ofActionPoints(3), Optional.of(TransferirEssenciaInteraction.class),
            Optional.of(CurandeiroSpecialization.MARTIR_ALTRUISTA), 0),

    // Requer 2 Habilidades de Mártir Altruísta. Passive. Real through Curandeiro#resolveDamageTakenAllyBlessings,
    // granted by DamageService#notifyDamageTaken to every ally the damage site's SceneContext resolves:
    // each hit from an enemy gives them, for 1 Rodada and cumulatively (sourceless Blessings),
    // -1 Nível on Perícia de Ataque and Domínio do Mana rolls and +1d6 dano (reported on the attack roll
    // as InteractionResult#getExtraDamageDice). Withheld in a combat Cena once the holder has dealt damage
    // (CombatantSheet#hasDealtDamageThisScene).
    TRANSFERIR_RANCOR(
            "Após você sofrer Danos de inimigos, a GD das Rolagens de Perícia de Ataque e Domínio do Mana de " +
            "todos os seus aliados presentes na Cena são reduzidas em -1 Nível, adicionalmente os ataques " +
            "deles causam +1d6 pontos de danos. Estes efeitos são cumulativos, mas duram apenas 1 Rodada. Esta " +
            "Habilidade não é ativada em Cenas de Combate em que você causou Danos a outros personagens.",
            true, fixed(0), ActionCost.NONE, Optional.empty(),
            Optional.of(CurandeiroSpecialization.MARTIR_ALTRUISTA), 2);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<AventyrTitleSpecialization> requiredSpecialization;
    private final int requiredOtherAbilities;
}
