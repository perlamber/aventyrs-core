package org.aventyrs.core.skill.attention;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The Habilidades de Competência available to characters trained in Atenção.
 */
@Getter
@AllArgsConstructor
public enum AttentionCompetencyAbility implements SkillCompetencyAbility {

    // TODO: automatic success, skipping the roll entirely, to perceive occurrences at
    // Distância Muito Curta whose GD is Média or lower — the Distância vocabulary itself now
    // exists (org.aventyrs.core.scene.Range), but that only tracks distance *between
    // CharacterSheets* (see SceneContext), not to an arbitrary in-fiction occurrence, so it
    // doesn't directly cover this case; still needs a roll-resolution-vs-DifficultyLevel
    // engine too (to know an occurrence's GD and compare it against Média).
    PERCEPCAO_DE_FOXM("Você é sempre bem-sucedido, dispensando rolagens, para perceber " +
            "ocorrências em Distância Muito Curta cuja GD seja igual ou inferior à Média."),

    // TODO: lets this Perícia use Gnose instead of its normal base Attribute (Instinto), and
    // grants perfect non-magical maze/labyrinth backtracking. The substitution half's
    // mechanism now exists (see SkillCompetencyAbility.getSubstituteAttributeDomain() /
    // AtaqueCorpoACorpoCompetencyAbility.ACUIDADE), this constant just doesn't override it
    // yet, and AttentionInteraction doesn't yet resolve/pass it into
    // CharacterSkillService.getValueForRoll's substituteAttributeDomain overload (same
    // remaining wiring gap as FurtividadeCompetencyAbility.LADINO_TEORICO); the
    // maze/navigation half still needs its own system, unrelated to attribute substitution.
    ALMA_DE_SHERLOCK("Você pode substituir o Atributo Base desta perícia por Gnose e nunca " +
            "se perde em labirintos (não-mágicos), sempre sabendo como retornar ao início."),

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
