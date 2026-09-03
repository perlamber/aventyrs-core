package org.aventyrs.core.skill.attention;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Atenção.
 */
@Getter
@AllArgsConstructor
public enum AttentionCompetencyAbility implements SkillCompetencyAbility {

    /**
     * "Você é sempre bem-sucedido, dispensando rolagens… cuja GD seja igual ou inferior à Média."
     * <b>The GD ceiling is real</b>, through {@link
     * SkillCompetencyAbility#resolveAutomaticSuccess}: the roll now states the target it was made
     * against, so "igual ou inferior à Média" is a comparison this ability can make.
     */
    // TODO: "em Distância Muito Curta" is not enforced. Range exists, but SceneContext measures
    //  distance between CombatantSheets and the thing being perceived is an in-fiction
    //  occurrence, not a combatant — so there is nothing to measure to. Granted at any distance,
    //  which is wider than the clause; the alternative is granting nothing.
    PERCEPCAO_DE_FOXM("Você é sempre bem-sucedido, dispensando rolagens, para perceber " +
            "ocorrências em Distância Muito Curta cuja GD seja igual ou inferior à Média.") {
        @Override
        public boolean resolveAutomaticSuccess(final SkillType skillType, final int targetValue,
                                                final SceneContext sceneContext) {
            return skillType == SkillType.ATTENTION
                    && targetValue <= DifficultyLevel.MEDIUM.getBaseValue();
        }
    },

    // Two halves. The substitution — Gnose for Instinto — is real, riding
    // SkillCompetencyAbility.getSubstituteAttributeDomain().
    // TODO: "nunca se perde em labirintos (não-mágicos)" needs a navigation/exploration system
    // to be exempt from; this core tracks no location beyond a Cena's participants and the
    // distances between them (org.aventyrs.core.scene.SceneContext), so there is no
    // getting-lost state for perfect backtracking to override.
    ALMA_DE_SHERLOCK("Você pode substituir o Atributo Base desta perícia por Gnose e nunca " +
            "se perde em labirintos (não-mágicos), sempre sabendo como retornar ao início.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.GNOSE);
        }
    },

    // TODO: lets you sense magical auras (Regalias, Encantamentos, Maldições) on people and
    // objects — no magic-aura/enchantment-detection system exists yet.
    INTUICAO_DE_SCULLY("Você consegue perceber com um dos seus sentidos auras mágicas de " +
            "Regalias, Encantamentos e Maldições em pessoas e objetos."),

    // TODO: Vantagem on every Perícia roll (not just Atenção's own) made outside Cenas de
    // Combate and on Rodada 0 — same structural cross-skill-Vantagem gap as
    // AtletismoExcellency.LENDA/FurtividadeCompetencyAbility.ACAO_SURPRESA (would need to
    // apply across every <Skill>Interaction, not just AttentionInteraction), plus no
    // Cena-de-Combate-state or Rodada-0-detection exists yet.
    ARDIL_DE_MARPLE("Você recebe Vantagem em rolagens de Perícias efetuadas fora de Cenas " +
            "de Combate e em Rodadas 0 (zero)."),

    // TODO: grants an extra Reação usable in a single Rodada of the character's choosing
    // each Cena — not the same shape as ReactionsService's flat always-available total, this
    // is a single-use-in-one-chosen-Rodada bonus, needing Cena-scoped usage tracking. Also
    // needs a graduation-crossing-a-threshold trigger for the +1 additional use at 5/10
    // Graduações (same gap as ArtesExcellency.FOCADO/LENDA), and — at 5+ Graduações — a way
    // to grant this same benefit to every ally (a cross-character effect), none of which
    // exist yet.
    INSTINTO_DE_LUTHER("Personagens com esta Competência podem prever as ações de outros, " +
            "a cada Cena você recebe uma Reação adicional para utilizar em apenas uma " +
            "Rodada, a sua escolha. A quantidade de Reações adicionais que você recebe " +
            "aumenta em mais uma ao alcançar a 5ª e 10ª Graduação. Se tiver ao menos 5 " +
            "Graduações, todos os seus aliados recebem uma Reação adicional para usar em " +
            "uma Rodada a escolha deles.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATTENTION;
    }
}
