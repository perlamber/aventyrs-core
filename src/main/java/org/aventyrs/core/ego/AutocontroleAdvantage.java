package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;

/**
 * The Vantagem de Autocontrole chosen once at character creation — available only to
 * characters whose Autocontrole base reached 3 through the creation-time point
 * distribution (see
 * {@link org.aventyrs.core.character.services.CharacterCreationService#isAutocontroleAdvantageAvailable}).
 * Reaching base 3 any other way (Talentos, Títulos Aventyrs, other Habilidades) never
 * grants access to this choice, and it is never lost if Autocontrole later drops to 2 or
 * below.
 */
@Getter
@AllArgsConstructor
public enum AutocontroleAdvantage implements EgoAdvantage {

    // TODO: +1d6 PV/PM/PD recovery (doubled when the point spent was permanent) whenever
    // Autocontrole points are spent for any effect — no Autocontrole-spending system or
    // dice-based recovery integration exists yet.
    DETERMINACAO_HEROICA("Usar pontos de Autocontrole para qualquer efeito adicionalmente " +
            "recupera +1d6PV, PM e PD; se o ponto for permanente, o valor recuperado é " +
            "dobrado."),

    // TODO: raises the Corrente de Efeitos threshold from 5 to 7 over your Defesas — no
    // Corrente de Efeitos/Defesas system exists yet.
    RESOLUTO("Correntes de Efeitos, para te afetar, precisam superar suas Defesas em 7, ao " +
            "invés de 5."),

    // TODO: grants +1 additional temporary Autocontrole point recovered per game session —
    // no game-session tracking system exists yet.
    MOTIVACAO_DE_MOSES("Você recupera 1 ponto de Autocontrole temporário adicional por " +
            "sessão de jogo.");

    private final String description;

    @Override
    public EgoDomain getEgoDomain() {
        return EgoDomain.AUTOCONTROLE;
    }
}
