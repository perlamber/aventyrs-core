package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

/**
 * The Habilidades de Competência available to characters trained in Empatia Selvagem.
 */
@Getter
@AllArgsConstructor
public enum EmpatiaSelvagemCompetencyAbility implements SkillCompetencyAbility {

    // TODO: lets this Perícia use Gnose instead of its normal base Attribute (Carisma) — no
    // Perícia base-Attribute substitution mechanism exists yet (same gap as
    // AtaqueCorpoACorpoCompetencyAbility.ACUIDADE / AtaqueADistanciaCompetencyAbility
    // .ARREMESSO_PODEROSO / AtletismoCompetencyAbility.ACROBATA / GnoseAbility
    // .PERITO_TEORICO / AttentionCompetencyAbility.ALMA_DE_SHERLOCK).
    ACADEMICO_SELVAGEM("Você pode substituir o Atributo Base desta perícia por Gnose."),

    // Note: scoped to calming hostile creatures specifically, but this codebase doesn't
    // track what a roll is *for* (same simplification as AttentionCompetencyAbility
    // .PERCEPCAO_DE_FOXM), so it's implemented as an unconditional flat bonus to every
    // Empatia Selvagem roll rather than silently narrowed.
    AMAINAR_A_SELVAGERIA("Vantagem para acalmar criaturas hostis.") {
        @Modifier(ModifierType.SKILL_ROLL_BONUS)
        public int advantageBonus() {
            return Skill.ADVANTAGE_BONUS;
        }
    },

    // TODO: spend 2PD as an Ação Livre to reroll a failed Empatia Selvagem roll, once per
    // creature — needs a roll-resolution-vs-DifficultyLevel engine (to know "failed") and a
    // per-creature usage-limit tracker, neither of which exist yet. Spending PD itself is
    // already supported (CharacterSheet#spendDeterminationPoints), but the reroll/once-per
    // -creature logic around it isn't.
    CHARME_FEERICO("Você pode usar 2PD para refazer, como Ação Livre, uma rolagem de " +
            "Empatia Selvagem que tenha falhado, mas apenas uma vez para cada criatura."),

    // TODO: lets this Perícia use Instinto instead of its normal base Attribute (Carisma) —
    // same substitution gap as ACADEMICO_SELVAGEM.
    INSTINTO_ANIMAL("Você pode substituir o Atributo Base desta perícia por Instinto."),

    // TODO: an activated ability training a creature (GD Difícil) into a
    // Cavaleiro/Peão/Torre-typed Subordinado, limited to one per Cena — needs a
    // roll-resolution-vs-DifficultyLevel engine, a Subordinado/ally-classification system,
    // and Cena-scoped usage tracking, none of which exist yet.
    ALIADO_DA_NATUREZA("Você pode treinar uma criatura para lhe acompanhar e auxiliar (GD " +
            "Difícil), ele é será considerado um Subordinado do tipo Cavaleiro, Peão ou " +
            "Torre, a sua escolha. Você recebe os benefícios de apenas um animal treinado " +
            "desta forma em cada Cena.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.EMPATIA_SELVAGEM;
    }
}
