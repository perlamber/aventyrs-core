package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Atenção.
 */
@Getter
@AllArgsConstructor
public enum AttentionCompetencyAbility implements SkillCompetencyAbility {

    // TODO: grants Vantagem on Atenção rolls to perceive movement through sight, hearing or
    // smell — no Vantagem/Desvantagem roll system exists yet.
    PERCEPCAO_DE_FOXM("Vantagem em rolagens de Atenção para perceber movimentações pelo " +
            "cenário através da visão, audição e olfato."),

    // TODO: lets this Perícia use Gnose instead of its normal base Attribute (Instinto),
    // and grants perfect maze/labyrinth backtracking — no Perícia base-Attribute
    // substitution mechanism or maze/navigation system exists yet (same substitution gap
    // as GnoseAbility.PERITO_TEORICO).
    ALMA_DE_SHERLOCK("Você pode substituir o Atributo Base desta perícia por Gnose; " +
            "adicionalmente, você nunca se perde em labirintos, sempre sabendo como " +
            "retornar ao início."),

    // TODO: lets you sense magical auras (Regalias, Encantamentos, Maldições) on people and
    // objects — no magic-aura/enchantment-detection system exists yet.
    PERCEPCAO_DE_SCULLY("Você consegue perceber com um dos seus sentidos auras mágicas de " +
            "Regalias, Encantamentos e Maldições em pessoas e objetos."),

    // TODO: an Atenção roll (GD Difícil or higher) to anticipate or get hints about things
    // that may happen — no roll-resolution-vs-DifficultyLevel engine exists yet.
    ARDIL_DE_MARPLE("Você confia fortemente na sua intuição e em momentos de suspeitas pode " +
            "fazer rolagens de Atenção (GD Difícil ou superior) para tentar antecipar (ou " +
            "conseguir dicas) de coisas que podem vir a acontecer."),

    // TODO: a roll vs GD Difícil to predict another's actions, granting an extra Reação
    // against them for 2 Rounds (+1 Round if the character has Identificar Padrões) — no
    // roll-resolution-vs-DifficultyLevel engine or Reação/Duration-tracking system exists yet.
    INSTINTO_DE_LUTHER("Personagens com esta Competência podem fazer uma rolagem contra GD " +
            "Difícil para prever as ações de outros; se bem-sucedido, recebem uma Reação " +
            "adicional contra o alvo pelas próximas 2 Rodadas, Duração que aumenta em +1 " +
            "Rodada em Personagens capazes de Identificar Padrões.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATTENTION;
    }
}
