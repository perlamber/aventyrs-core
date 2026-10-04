package org.aventyrs.core.skill.empatiaselvagem;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Empatia Selvagem.
 */
@Getter
@AllArgsConstructor
public enum EmpatiaSelvagemCompetencyAbility implements SkillCompetencyAbility {

    // Substitutes Gnose for Carisma — see SkillCompetencyAbility.getSubstituteAttributeDomain().
    // Unlike AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO (scoped to a specific
    // attack/delivery method, hence the resolveSubstituteAttributeDomain(AttackSource) hook)
    // this is unconditional, so it fits the plain mechanism directly; GnoseAbility.PERITO_TEORICO
    // is a different shape entirely (an acquisition-time choice of *which* Perícia, not a fixed
    // one) and has its own AcquiredChoice-driven resolution.
    ACADEMICO_SELVAGEM("Você pode substituir o Atributo Base desta perícia por Gnose.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.GNOSE);
        }
    },

    // TODO: makes the character never a wild creature's primary target unless acting
    // offensively (with an exception for being hunted as food as the sole target) — this is
    // an NPC-targeting-priority/threat-perception override, not a roll bonus, and no such
    // system exists yet (same gap as EmpatiaSelvagemExcellency.LENDA's own creature-
    // disposition override).
    AMAINAR_A_SELVAGERIA("Você não é visto como uma ameaça imediata por criaturas " +
            "selvagens, a menos que tome ações ofensivas você nunca será seus alvos " +
            "primários (exceto se estiver sendo caçado como alimento e for o único alvo)."),

    /**
     * Real (core 0.0.102): {@link CompetencyUses#charm} spends the 2PD and marks the creature for this Cena
     * (table ruling: "uma vez para cada criatura" resets per Cena). The reroll itself is the caller's — this core
     * never rolls dice — offered only on a failed Empatia Selvagem roll, as an Ação Livre.
     */
    CHARME_FEERICO("Você pode usar 2PD para refazer, como Ação Livre, uma rolagem de " +
            "Empatia Selvagem que tenha falhado, mas apenas uma vez para cada criatura."),

    // Substitutes Instinto for Carisma — same mechanism as ACADEMICO_SELVAGEM. A character
    // holding both hands SkillCompetencyAbility#resolveAttributeDomain two candidates; it takes
    // the first match, since the rules name no precedence between two substitutions of the same
    // Perícia (the same call the AtaqueCorpoACorpoCompetencyAbility pair already documents).
    INSTINTO_ANIMAL("Você pode substituir o Atributo Base desta perícia por Instinto.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.INSTINCT);
        }
    },

    // Real (core 0.0.103): skill.CompetencyActions#trainCompanion (GD Difícil) keeps a TrainedCompanion
    // — named, with a Cavaleiro/Peão/Torre SubordinateBenefit — on the Character (lasting, table
    // ruling); #callCompanion commands one per Cena as a Subordinado (⚠️ Cena kept per combat).
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
