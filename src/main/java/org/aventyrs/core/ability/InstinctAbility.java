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

    // TODO: activated ability (2PD cost, once per Scene) letting you act before Initiative at
    // scene start. The 2PD spend itself is already mechanically possible
    // (CharacterSheet#spendDeterminationPoints), but two real pieces are still missing: a
    // once-per-Cena usage tracker (this core has none anywhere — FurtividadeExcellency's own
    // "três vezes por Cena" TODO cites the identical gap), and a way to insert an action
    // *before* Initiative at a Scene's start — Scene's turn order (activeEntries/
    // pendingEntries/startNewRound) only ever lets a participant join the normal
    // Initiative-ordered rotation, never act ahead of it.
    PARANOIA_SAUDAVEL("Ao custo de 2PD e apenas uma vez a cada Cena, você pode optar por fazer uma ação com " +
            "Tempo de até 2PA, Ação Livre ou Reação no início da cena. Esta Vantagem deve ser ativada no início " +
            "da Cena, antes da ação do primeiro personagem, ignorando as Iniciativas."),

    OBSTINADO("Seu multiplicador de PD aumenta em +1.") {
        @Modifier(ModifierType.DETERMINATION_MULTIPLIER)
        public int determinationMultiplierBonus() {
            return 1;
        }
    },

    // Real via resolveRestDeterminationPointsBonus below, for the one PD-recovery mechanism
    // that actually exists (RestServiceImpl#getRecoveredDeterminationPoints) — the rules
    // text's broader "efeitos que te permitam recuperar PD" is written to cover any future
    // PD-recovery effect too, but Rest is the only one this core has today.
    SUPERMOTIVADO("Efeitos que te permitam recuperar PD o fazem recuperar 1PD adicional. Descansos longos ou " +
            "superiores adicionalmente te permitem recuperar 2PD adicionais (totalizando 3PD adicionais).") {
        @Override
        public int resolveRestDeterminationPointsBonus(final RestType restType) {
            return BASE_REST_DETERMINATION_BONUS + (restType.isAtLeast(RestType.LONGO) ? EXTRA_LONGO_DETERMINATION_BONUS : 0);
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
