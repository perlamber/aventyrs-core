package org.aventyrs.core.skill.empatiaselvagem;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The Habilidades de Competência available to characters trained in Empatia Selvagem.
 */
@Getter
@AllArgsConstructor
public enum EmpatiaSelvagemCompetencyAbility implements SkillCompetencyAbility {

    // TODO: lets this Perícia use Gnose instead of its normal base Attribute (Carisma) — the
    // substitution mechanism itself now exists (see SkillCompetencyAbility
    // .getSubstituteAttributeDomain() / AtaqueCorpoACorpoCompetencyAbility.ACUIDADE), this
    // constant just doesn't override it yet, and EmpatiaSelvagemInteraction doesn't yet
    // resolve/pass it into CharacterSkillService.getValueForRoll's substituteAttributeDomain
    // overload (same remaining wiring gap as FurtividadeCompetencyAbility.LADINO_TEORICO). Unlike
    // AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO (scoped to a specific
    // attack/delivery method) this is unconditional, so it fits the mechanism directly;
    // GnoseAbility.PERITO_TEORICO is a different shape entirely (an acquisition-time choice
    // of *which* Perícia, not a fixed one) and needs its own AcquiredChoice-driven mechanism.
    ACADEMICO_SELVAGEM("Você pode substituir o Atributo Base desta perícia por Gnose."),

    // TODO: makes the character never a wild creature's primary target unless acting
    // offensively (with an exception for being hunted as food as the sole target) — this is
    // an NPC-targeting-priority/threat-perception override, not a roll bonus, and no such
    // system exists yet (same gap as EmpatiaSelvagemExcellency.LENDA's own creature-
    // disposition override).
    AMAINAR_A_SELVAGERIA("Você não é visto como uma ameaça imediata por criaturas " +
            "selvagens, a menos que tome ações ofensivas você nunca será seus alvos " +
            "primários (exceto se estiver sendo caçado como alimento e for o único alvo)."),

    // TODO: spend 2PD as an Ação Livre to reroll a failed Empatia Selvagem roll, once per
    // creature — needs a roll-resolution-vs-DifficultyLevel engine (to know "failed") and a
    // per-creature usage-limit tracker, neither of which exist yet. Spending PD itself is
    // already supported (CombatantSheet#spendDeterminationPoints), but the reroll/once-per
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
