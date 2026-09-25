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
 * The Habilidades/Supremas gated on {@link CurandeiroSpecialization#MARTIR_ALTRUISTA}. None is real
 * yet; each names the system it waits on.
 */
@Getter
@AllArgsConstructor
public enum MartirAltruistaAbility implements AventyrTitleAbility {

    // Requer Mártir Altruísta. Passive.
    // TODO: "utilizar PV em substituição à PM e PD" — no cost-substitution hook on a Magia's Mana or a
    //  Habilidade's PD; and "recuperados apenas com Descansos Verdadeiros" needs PV lost this way held
    //  apart from ordinary damage, which ResourcePool does not do.
    TRANSFERIR_VITALIDADE(
            "Você pode utilizar PV em substituição à PM e PD para ativar Habilidades de Curandeiro ou conjurar " +
            "Magias Divinas e Naturais de único alvo, mas apenas quando os alvos forem personagens aliados. PV " +
            "perdidos desta forma podem ser recuperados apenas com Descansos Verdadeiros.",
            false, fixed(0), ActionCost.NONE, Optional.empty(),
            Optional.of(CurandeiroSpecialization.MARTIR_ALTRUISTA), 0),

    // Requer Mártir Altruísta. 1PD, 3PA.
    // TODO: moving PD from one sheet to another is two ordinary spends/recoveries, but "temporariamente 1
    //  de seus Pontos de Autocontrole … perdidos ou devolvidos … até o fim da Cena" needs a Cena-scoped
    //  loan between two Ego pools, which EgoPointPool cannot hold.
    TRANSFERIR_DETERMINACAO(
            "Para ativar esta Habilidade você deve escolher entre transferir qualquer quantidade de PD que você " +
            "possua, ou temporariamente 1 de seus Pontos de Autocontrole, para um aliado, a transferência é " +
            "realizada ao toque. Pontos de Autocontrole transferidos desta forma são perdidos ou devolvidos se " +
            "não forem utilizados até o fim da Cena.",
            false, fixed(1), ActionCost.ofActionPoints(3), Optional.empty(),
            Optional.of(CurandeiroSpecialization.MARTIR_ALTRUISTA), 0),

    // Requer Mártir Altruísta. 1PD, 3PA.
    // TODO: the Mana/Sorte twin of TRANSFERIR_DETERMINACAO, blocked on the same Cena-scoped Ego loan.
    TRANSFERIR_ESSENCIA(
            "Para ativar esta Habilidade você deve escolher entre transferir qualquer quantidade de PM que você " +
            "possua, ou temporariamente 1 de seus Pontos de Sorte, para um aliado, a transferência é realizada " +
            "ao toque. Pontos de Sorte transferidos desta forma são perdidos ou devolvidos se não forem " +
            "utilizados até o fim da Cena.",
            false, fixed(1), ActionCost.ofActionPoints(3), Optional.empty(),
            Optional.of(CurandeiroSpecialization.MARTIR_ALTRUISTA), 0),

    // Requer 2 Habilidades de Mártir Altruísta. Passive.
    // TODO: a damage-taken trigger exists (DamageService#notifyDamageTaken), but it grants Blessings to
    //  the victim alone — nothing reaches "todos os seus aliados presentes na Cena"; the GD reduction
    //  and the +1d6 are further blocked on a Título GD hook and on this core granting no dice; and
    //  "Cenas de Combate em que você causou Danos" needs a per-Cena damage-dealt record.
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
