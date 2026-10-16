package org.aventyrs.core.monster.model;

/**
 * What its holder just activated, as a Habilidade Monstruosa reacting to it sees it (core 0.1.7.3) — Renascido
 * Monstruoso's "Após ativar qualquer Habilidade Monstruosa do Modelo Escolhido de Macária ou Habilidade de Título
 * Aventyr".
 *
 * @param model        the Modelo of the Habilidade Monstruosa whose Efeito Ativo was activated, or {@code null}
 * @param titleAbility whether a Título Aventyr Habilidade was activated instead
 */
public record ActivationTrigger(MonsterModel model, boolean titleAbility) {

    public static ActivationTrigger monstrous(final MonsterModel model) {
        return new ActivationTrigger(model, false);
    }

    public static ActivationTrigger title() {
        return new ActivationTrigger(null, true);
    }
}
