package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.skill.Skill;

@Getter
@AllArgsConstructor
public enum InstinctAbility implements AttributeAbility {

    // Real: "Desperto" == holding a Título Aventyr in any of the three slots (Character
    // #getAllTitles() non-empty) — not a per-roll player choice between the two clauses, but
    // two mutually exclusive states of the character itself: no Título yet grants the flat
    // Vantagem, acquiring one switches the character over to the GD reduction instead (never
    // both at once). See resolveAttributeDomainRollBonus/resolveAttributeDomainDifficultyReduction
    // below.
    SENTIR_A_INTENCAO("Você recebe Vantagem em suas rolagens de Perícias baseadas em Instinto, se tiver ao menos " +
            "1 Título Aventyr Desperto; ao invés disso, você reduz a GD da rolagem em -1 nível.") {
        @Override
        public int resolveAttributeDomainRollBonus(final AttributeDomain rolledDomain, final Character character) {
            return rolledDomain == AttributeDomain.INSTINCT && character.getAllTitles().isEmpty()
                    ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public int resolveAttributeDomainDifficultyReduction(final AttributeDomain rolledDomain, final Character character) {
            return rolledDomain == AttributeDomain.INSTINCT && !character.getAllTitles().isEmpty()
                    ? SENTIR_A_INTENCAO_DIFFICULTY_REDUCTION : 0;
        }
    },

    // Real: a Rodada 0.0 before Rodada 0 (table ruling, 2026-10-16). resolvePreInitiativeActionCost prices it;
    // character.services.PreInitiativeService pays it once per combat ("uma vez a cada Cena" is per combat, the
    // house reading — skill.UseWindow); Scene#beginPreInitiativeTurn/#permitsPreInitiativeAction hold the one
    // action of Tempo ≤ 2PA, Ação Livre or Reação, ignoring Iniciativa. The API orders the takers and the client
    // offers it as combat starts.
    PARANOIA_SAUDAVEL("Ao custo de 2PD e apenas uma vez a cada Cena, você pode optar por fazer uma ação com " +
            "Tempo de até 2PA, Ação Livre ou Reação no início da cena. Esta Vantagem deve ser ativada no início " +
            "da Cena, antes da ação do primeiro personagem, ignorando as Iniciativas.") {
        @Override
        public java.util.OptionalInt resolvePreInitiativeActionCost() {
            return java.util.OptionalInt.of(2);
        }
    },

    OBSTINADO("Seu multiplicador de PD aumenta em +1.") {
        @Modifier(ModifierType.DETERMINATION_MULTIPLIER)
        public int determinationMultiplierBonus() {
            return 1;
        }
    },

    // Real for both halves: a real Descanso pays 1PD (+2 more from a Longo) via
    // resolveRestDeterminationPointsBonus, and an effect recovering PD "como se passasse por um
    // Descanso" (a Magia de cura, an Autocontrole spend, Medicina) pays the 1PD via
    // resolveRestEquivalentDeterminationPointsBonus — "Efeitos que te permitam recuperar PD" names
    // every recovery effect, while the Longo half names a Descanso. A PD recovery that isn't
    // Descanso-shaped (Oprimir's steal, a Título's grant) still reaches neither.
    SUPERMOTIVADO("Efeitos que te permitam recuperar PD o fazem recuperar 1PD adicional. Descansos longos ou " +
            "superiores adicionalmente te permitem recuperar 2PD adicionais (totalizando 3PD adicionais).") {
        @Override
        public int resolveRestDeterminationPointsBonus(final RestType restType) {
            return BASE_REST_DETERMINATION_BONUS + (restType.isAtLeast(RestType.LONGO) ? EXTRA_LONGO_DETERMINATION_BONUS : 0);
        }

        @Override
        public int resolveRestEquivalentDeterminationPointsBonus(final RestType restType) {
            return BASE_REST_DETERMINATION_BONUS;
        }
    },

    // Real via org.aventyrs.core.character.services.TitleAbilityService#grantTitleAbility,
    // now that AventyrTitle#grantAbility can mutate an already-held Título's ability list
    // post-acquisition (the piece this was blocked on — a Título's abilities used to be fixed
    // at construction). The one-time grant is tracked on Character#isCentelhaSuperiorSelected();
    // TitleAbilityService#getAvailableSupremaSlots reports how many Supremas a given Título
    // can still receive right now, factoring both that flag and the Título's own current count.
    // grantTitleAbility itself grants any Título ability, not just Supremas — CENTELHA_SUPERIOR's
    // own gate only applies to the one case it actually covers: a Suprema that would exceed the
    // normal one-per-Título allotment.
    CENTELHA_SUPERIOR("Você pode adquirir uma Suprema adicional, mas apenas de um de seus Títulos Aventyr, ao " +
            "custo de 5 EXP.");

    /** SENTIR_A_INTENCAO's own GD reduction (in níveis) once a Título Aventyr is held. */
    private static final int SENTIR_A_INTENCAO_DIFFICULTY_REDUCTION = 1;

    /** SUPERMOTIVADO's own extra PD recovered on any Rest. */
    private static final int BASE_REST_DETERMINATION_BONUS = 1;

    /** SUPERMOTIVADO's own additional extra PD recovered on a Descanso Longo or better. */
    private static final int EXTRA_LONGO_DETERMINATION_BONUS = 2;

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.INSTINCT;
    }
}
