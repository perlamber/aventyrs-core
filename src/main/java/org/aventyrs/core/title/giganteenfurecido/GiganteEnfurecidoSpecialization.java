package org.aventyrs.core.title.giganteenfurecido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

/**
 * Gigante Enfurecido's two Especializações. Neither is activated on its own: each "deve ser ativada em
 * conjunto a o Frenesi do Gigante Enfurecido", so each is a {@link FrenzyMode} the Frenesi request
 * names ({@code TitleAbilityActivationRequest#getChoices(FrenzyMode.class)}), adding its "+1 Ponto
 * temporário de Autocontrole" and "+1PA" to that one activation. Hence {@link ActionCost#NONE} and no
 * Interaction here — the stated extra cost is {@link #getEgoCost()}/{@link #getExtraActionPoints()},
 * which {@link FrenesiInteraction} adds.
 */
@Getter
@AllArgsConstructor
public enum GiganteEnfurecidoSpecialization implements AventyrTitleSpecialization {

    // "Apenas 'Gigantes Enfurecidos' podem adquirir" — enforced by GiganteEnfurecido#grantSpecialization.
    // Real through FrenesiInteraction: Categoria de Tamanho +1 and Multiplicador de PV +1 (Frenzy's
    // SIZE_CATEGORY/LIFE_MULTIPLIER, read by CharacterSizeService and HitPointsService); the
    // concentration block is lifted (Frenzy#liftConcentrationBlock), and "Conjurar Magias e utilizar
    // Perícias baseadas em Gnose tem o tempo de ação aumentado em +1PA" is
    // CombatantSheet#getConcentrationActionPointSurcharge, added by SpellCastingService
    // #resolveActivationTime. "ainda tem inclinações violentas e comportamentos impulsivo" is
    // roleplay, with no mechanic stated.
    TITA_ENLOUQUECIDO(
            "Esta Habilidade deve ser ativada em conjunto a o Frenesi do Gigante Enfurecido. Enquanto em " +
            "Frenesi sua Categoria de Tamanho e seu Multiplicador de PV aumentam ambos em +1. Quando iniciar " +
            "um Frenesi com esta Habilidade ativa você não perde totalmente a capacidade de raciocínio, se " +
            "tornando apto a realizar ações que exijam concentração, como Conjurar Magias e usar Perícias " +
            "diversas, porém ainda tem inclinações violentas e comportamentos impulsivo. Conjurar Magias e " +
            "utilizar Perícias baseadas em Gnose tem o tempo de ação aumentado em +1PA.",
            FrenzyMode.TITA_ENLOUQUECIDO),

    // "Apenas 'Gigantes Enfurecidos' podem adquirir" — enforced by GiganteEnfurecido#grantSpecialization.
    // Real through FrenesiInteraction: Força +1 and Margem Crítica Menor +1 (Frenzy's STRENGTH_BONUS and
    // LESSER_CRITICAL_MARGIN). "Sempre que for alvo de um ataque bem-sucedido e sofrer danos" is
    // GiganteEnfurecido#recordHitTaken, which the caller calls after the damage lands: −2 Defesas and
    // +1 Força stacked onto the Frenzy, "apenas 1 vez para cada inimigo atacante" a Cena
    // (CombatantSheet#markAffectedThisCombat keyed per attacker).
    BERSERKER(
            "Esta Habilidade deve ser ativada em conjunto a o Frenesi do Gigante Enfurecido. Você recebe " +
            "Bônus de +1 em Força e sua Margem Crítica Menor aumenta em +1 número. Sempre que for alvo de um " +
            "ataque bem-sucedido e sofrer danos você recebe Redutor de -2 em suas Defesas e Bônus de +1 em " +
            "Força. A cada Cena esta habilidade pode ser ativada apenas 1 vez para cada inimigo atacante.",
            FrenzyMode.BERSERKER);

    /** Each Especialização's "Tempo de Ativação: +1PA", added to the Frenesi it joins. */
    public static final int EXTRA_ACTION_POINTS = 1;

    private final String description;
    /** The Frenesi mode this Especialização switches on. */
    private final FrenzyMode mode;

    /** "Custo de Ativação: +1 Ponto temporário de Autocontrole" — paid by the Frenesi it joins. */
    @Override
    public EgoCost getEgoCost() {
        return EgoCost.autocontrole(1);
    }

    public int getExtraActionPoints() {
        return EXTRA_ACTION_POINTS;
    }

    @Override
    public PDCost getPDCost() {
        return PDCost.NONE;
    }

    @Override
    public ActionCost getActionPointCost() {
        return ActionCost.NONE;
    }

    @Override
    public Optional<Class<? extends Interaction>> getInteractionClass() {
        return Optional.empty();
    }

    /** The Especialização that switches on mode, if any. */
    public static Optional<GiganteEnfurecidoSpecialization> of(final FrenzyMode mode) {
        for (GiganteEnfurecidoSpecialization specialization : values()) {
            if (specialization.mode == mode) {
                return Optional.of(specialization);
            }
        }
        return Optional.empty();
    }
}
