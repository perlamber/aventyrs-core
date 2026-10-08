package org.aventyrs.core.sheet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.action.ActionKind;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.Skill;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Condições de Personagem a combatant can be under. The catalogue entry for a condition,
 * the same catalogue-not-instance split {@code org.aventyrs.core.item.Item} draws: a constant
 * here describes what Caído <i>is</i>, while {@link Condition} is one combatant actually being
 * under it, with its own countdown and its own origin.
 *
 * <p>Authored from {@code docs/rules/condicoes-e-maleficios-.txt} (full re-import, core 0.1.5),
 * which builds every Condição out of a handful of <b>Estados de Personagem</b>:
 *
 * <ul>
 *   <li><b>Desprevenido</b> — -4 em Defesas ({@link #DESPREVENIDO});</li>
 *   <li><b>Fraqueza</b> — Desvantagem em rolagens de Perícias e Danos ({@link #FRAQUEZA});</li>
 *   <li><b>Desacordado</b> — no actions for 2 Rodadas, and applies Caído ({@link #DESACORDADO});</li>
 *   <li><b>Favorecido</b> — Vantagem on named Perícias. Never held by anyone: it is what a
 *   Condição grants <i>whoever attacks its holder</i>, so it is {@link #getAttackerFavours()},
 *   not a constant.</li>
 * </ul>
 *
 * <p><b>An Estado is binary</b> (table ruling, 2026-10-07): two Condições conferring Fraqueza
 * cost -2 once, never -4. That falls out of {@link #getImplied()} with no extra work, because
 * {@code AbstractCombatantSheet#activeConditionOrigins} keys the active set by type. Different
 * Estados still combine — Caído is Desprevenido <i>and</i> Fraqueza. "Pronto", the default
 * Condição, has no constant: it is the empty set.
 *
 * <p><b>Malefícios are the harmful majority, not the whole enum.</b> {@link #ESCONDIDO} is a
 * state a character puts <i>themselves</i> in, and {@link #PETRIFICADO} is an Encantamento;
 * {@link #isMaleficio()} answers the one clause that asks about the category
 * ({@code VidaSpell#CORPO_FECHADO}'s "todos os Malefícios"). Estados count as Malefícios.
 *
 * <p>{@link #POSSESSAO} is authored from {@code docs/rules/magias.txt} — three Magias name it
 * as a Malefício, and the conditions file does not list it. <b>Coma</b> is {@code
 * CharacterStatus#COMMA} (a PV tier) and <b>Encantamento</b> is {@code MagicType#ENCANTAMENTO};
 * neither is a constant here. <b>Desarmado</b> was dropped by the full re-import (table ruling:
 * a disarmed fighter simply holds no weapon, and attacks with an Ataque Desarmado).
 *
 * <p><b>Three ways a condition reaches the rules engine</b>, and a constant may use any mix:
 *
 * <ul>
 *   <li><b>{@link #getEffects()}</b> — typed numeric maluses ({@link ConditionEffect}), summed by
 *   {@code CombatantSheet#getConditionBonus} into whichever service already reads that {@link
 *   ModifierType}. Only the Estados carry any: every Condição reaches its numbers through the
 *   Estados it implies.</li>
 *   <li><b>{@link #getImplied()}</b> — Estados (or Condições) this one confers, each optionally
 *   scoped to a distance band from the condition's origin. Resolved transitively, so removing
 *   Caído removes the Desprevenido it brought without any separate bookkeeping.</li>
 *   <li><b>The boolean gates</b> ({@link #preventsMovement()} and friends) — for a clause that
 *   forbids something outright rather than taxing it.</li>
 * </ul>
 *
 * <p><b>Fraqueza is a flat {@link Skill#DISADVANTAGE_MALUS}</b> (-2) on every Perícia roll —
 * defensive ones included (table ruling: "rolagens de Perícia" covers Esquiva e Aparar) — and
 * on every dano roll.
 */
@Getter
@AllArgsConstructor
public enum ConditionType {

    /**
     * "Estados de Fraqueza apenas enquanto estiver à Distância Curta da origem de seu medo." The
     * mildest rung of the fear ladder, and the one the other two decay into.
     */
    ABALADO("Estados de Fraqueza apenas enquanto estiver à Distância Curta da origem de seu medo. "
            + "Condição permanece ativa por 2 Rodadas, a menos que a origem do efeito diga o "
            + "contrário.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return Map.of(FRAQUEZA, Range.DISTANCIA_CURTA);
        }

        @Override
        public int getFearRank() {
            return 1;
        }
    },

    /**
     * "Cumulativo com efeitos de Abalado. Área de Efeito de Abalado aumenta para Distância Média.
     * Estado de Desprevenido enquanto em Distância Curta da origem do medo … Ao fim da duração
     * alvo se torna Abalado."
     */
    ASSUSTADO("Cumulativo com efeitos de Abalado. Área de Efeito de Abalado aumenta para Distância "
            + "Média. Estado de Desprevenido enquanto em Distância Curta da origem do medo. "
            + "Condição permanece ativa por 2 Rodadas, a menos que a origem do efeito diga o "
            + "contrário. Ao fim da duração alvo se torna Abalado.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return Map.of(FRAQUEZA, Range.DISTANCIA_MEDIA, DESPREVENIDO, Range.DISTANCIA_CURTA);
        }

        @Override
        public ConditionType getDecaysTo() {
            return ABALADO;
        }

        @Override
        public int getFearRank() {
            return 2;
        }
    },

    /**
     * "Área de Efeito de Abalado aumenta para Distância Longa … Área de Efeito de Assustado
     * (Estado de Desprevenido) aumenta para Distância Média. Enquanto em Distância Curta da origem
     * do medo suas ações são restritas a fugir até sair do alcance de Abalado."
     */
    APAVORADO("Cumulativo com efeitos de Abalado. Área de Efeito de Abalado aumenta para Distância "
            + "Longa. Cumulativo com efeitos de Assustado. Área de Efeito de Assustado (Estado de "
            + "Desprevenido) aumenta para Distância Média. Enquanto em Distância Curta da origem "
            + "do medo suas ações são restritas a fugir até sair do alcance de Abalado. Condição "
            + "permanece ativa por 2 Rodadas, a menos que a origem do efeito diga o contrário. Ao "
            + "fim da Duração alvo se torna Assustado.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return Map.of(FRAQUEZA, Range.DISTANCIA_LONGA, DESPREVENIDO, Range.DISTANCIA_MEDIA);
        }

        /** "Restritas a fugir": only moving away (or escaping a hold to do so) is left. */
        @Override
        public boolean refuses(final ActionKind kind) {
            return kind != ActionKind.MOVEMENT && kind != ActionKind.DEFENCE && !kind.isEscape();
        }

        /** Only while within Curta of the fear's origin (always, for a sourceless fear). */
        @Override
        public Range getRestrictionRange() {
            return Range.DISTANCIA_CURTA;
        }

        @Override
        public ConditionType getDecaysTo() {
            return ASSUSTADO;
        }

        @Override
        public int getFearRank() {
            return 3;
        }
    },

    /**
     * "Estado de Desprevenido. Efeitos conforme descrição da maldição." The Desprevenido is the
     * condition's own; whatever else a Maldição does is supplied by whatever inflicted it (a
     * {@link Condition}'s extra effects, or the Maldição's own effect class).
     */
    AMALDICOADO("Estado de Desprevenido. Efeitos conforme descrição da maldição. Pode aplicar "
            + "Negação de Ego, Fraqueza, Negação de Efeitos de Cura, aplicar Defeitos e infligir "
            + "danos.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(DESPREVENIDO);
        }
    },

    /** Estado — "Redutor de -4 em Defesas." The most-implied entry in the catalogue. */
    DESPREVENIDO("Redutor de -4 em Defesas.") {
        @Override
        public List<ConditionEffect> getEffects() {
            return List.of(new ConditionEffect(ModifierType.DEFESAS, DESPREVENIDO_DEFENSE_MALUS, null));
        }

        @Override
        public boolean isEstado() {
            return true;
        }
    },

    /**
     * Estado — "Desvantagem em suas rolagens de Perícias e Danos." Taxes every Perícia roll,
     * Esquiva e Aparar included, and every dano roll.
     */
    FRAQUEZA("Desvantagem em suas rolagens de Perícias e Danos.") {
        @Override
        public List<ConditionEffect> getEffects() {
            return List.of(
                    new ConditionEffect(ModifierType.SKILL_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, null),
                    new ConditionEffect(ModifierType.DAMAGE_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, null));
        }

        @Override
        public boolean isEstado() {
            return true;
        }
    },

    /**
     * Estado — "Personagem não pode realizar ações por 2 Rodadas, aplica o Malefício Caído." The
     * Caído is applied <i>alongside</i> it by {@code AbstractCombatantSheet#applyCondition}, not
     * implied: it outlives the Desacordado and ends only by Levantar-se.
     */
    DESACORDADO("Personagem não pode realizar ações por 2 Rodadas, aplica o Malefício Caído.") {
        @Override
        public boolean isEstado() {
            return true;
        }

        /** "Não pode realizar ações" — every one; a defence is not an action. */
        @Override
        public boolean refuses(final ActionKind kind) {
            return kind != ActionKind.DEFENCE;
        }

        @Override
        public boolean defendsUnlessCriticalFailure() {
            return true;
        }
    },

    /**
     * "Cercar um personagem exige a presença de 1+ Categoria de Tamanho personagens (mínimo 2)
     * adjacentes … Alvo do cerco recebe o Estado Desprevenido. Personagens flanqueados se tornam
     * Favorecidos em Perícias de Ataque." The Favorecido lands on the attackers (Vantagem on the
     * attack <i>roll</i> — it used to be the dano roll).
     */
    // TODO: whether a combatant is surrounded is geometry this core does not do — the client
    //  derives it from token positions (plan Phase 5, scene.grid.Flanking).
    FLANQUEADO("Refere-se a personagens cercados. Cercar um personagem exige a presença de 1+ "
            + "Categoria de Tamanho personagens (mínimo 2) adjacentes, posicionado em direções "
            + "opostas, triangular, quadrangular ou circular. Alvo do cerco recebe o Estado "
            + "Desprevenido. Personagens flanqueados se tornam Favorecidos em Perícias de Ataque.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(DESPREVENIDO);
        }

        @Override
        public Set<AttackerFavour> getAttackerFavours() {
            return EnumSet.of(AttackerFavour.ATTACK_ROLL);
        }
    },

    /**
     * "Estados de Desprevenido e Fraqueza. Movimento Base reduzido à metade. Permanece nesta
     * Condição até levantar-se … Personagens que ataquem alvos caídos se tornam Favorecidos em
     * Perícias de Ataque e Esquiva e Aparar." Open-ended: Levantar-se ends it.
     */
    // TODO: Levantar-se (1PA, provoking Defender o Perímetro) is the plan's StandUpService (Phase 2).
    CAIDO("Estados de Desprevenido e Fraqueza. Movimento Base reduzido à metade. Permanece nesta "
            + "Condição até levantar-se. Levantar-se indica alternar entre Caído e Pronto, tem o "
            + "Tempo de Ação de 1PA e permite a Reação Defender o Perímetro para personagens "
            + "qualificados. Personagens que ataquem alvos caídos se tornam Favorecidos em "
            + "Perícias de Ataque e Esquiva e Aparar.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(DESPREVENIDO, FRAQUEZA);
        }

        @Override
        public Set<AttackerFavour> getAttackerFavours() {
            return EnumSet.of(AttackerFavour.ATTACK_ROLL, AttackerFavour.DEFENCE_AGAINST_HOLDER);
        }

        @Override
        public boolean halvesMovementBase() {
            return true;
        }
    },

    /**
     * "Não pode realizar Ações de Movimentos (como andar, correr, investir, reposicionar etc.).
     * Estado de Desprevenido. Permanece nesta Condição até Libertar-se do Agarrão." The {@link
     * Condition#getSource()} is the captor.
     */
    AGARRADO("Não pode realizar Ações de Movimentos (como andar, correr, investir, reposicionar "
            + "etc.). Estado de Desprevenido. Permanece nesta Condição até Libertar-se do Agarrão. "
            + "Libertar-se do Agarrão indica alternar entre Agarrado e Pronto, esta ação requer "
            + "uma rolagem de Perícia de Ataque resistida e que não inflige danos e não desencadeia "
            + "Efeitos Críticos e Correntes de Efeitos.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(DESPREVENIDO);
        }

        @Override
        public boolean preventsMovement() {
            return true;
        }
    },

    /**
     * "Estado de Desprevenido. Não pode realizar Ações, exceto Libertar-se da Imobilização …
     * Personagens que ataquem alvos Imobilizados se tornam Favorecidos em Perícias de Ataque."
     * Comes only from specific effects, never from a plain Agarrar.
     */
    IMOBILIZADO("Estado de Desprevenido. Não pode realizar Ações, exceto Libertar-se da "
            + "Imobilização. Permanece nesta Condição até Libertar-se da Imobilização. Libertar-se "
            + "da Imobilização indica alternar entre Imobilizado e Pronto, esta ação requer uma "
            + "rolagem de Furtividade. Personagens que ataquem alvos Imobilizados se tornam "
            + "Favorecidos em Perícias de Ataque.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(DESPREVENIDO);
        }

        @Override
        public Set<AttackerFavour> getAttackerFavours() {
            return EnumSet.of(AttackerFavour.ATTACK_ROLL);
        }

        /** "Não pode realizar Ações, exceto Libertar-se da Imobilização"; a defence is not an action. */
        @Override
        public boolean refuses(final ActionKind kind) {
            return kind != ActionKind.ESCAPE_IMMOBILIZATION && kind != ActionKind.DEFENCE;
        }

        @Override
        public boolean defendsUnlessCriticalFailure() {
            return true;
        }
    },

    /**
     * "Estado de Fraqueza. Ocupa o mesmo espaço que o personagem que o devorou … Apenas Armas
     * Naturais ou Armas leves podem ser utilizados enquanto devorado." Being swallowed also blocks
     * re-arming ({@link #preventsArming()}) — nothing you dropped is reachable from inside.
     */
    // TODO: the escape (GD = the devourer's DF -2 níveis, min Médio), Meio-Dano inside, the
    //  2×Vigor single-hit release and "não pode ser afetado por efeitos externos" are the plan's
    //  Phase 3 (DevourService). Bocarra keeps its own accumulated-damage escape.
    DEVORADO("Estado de Fraqueza. Ocupa o mesmo espaço que o personagem que o devorou. Não pode "
            + "ser afetado por efeitos externos, mesmo que possuam Área de Efeito. Permanece nesta "
            + "Condição até Libertar-se da Predação. Apenas Armas Naturais ou Armas leves podem "
            + "ser utilizados enquanto devorado.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(FRAQUEZA);
        }

        @Override
        public boolean preventsArming() {
            return true;
        }

        @Override
        public boolean restrictsAttacksToLightWeapons() {
            return true;
        }
    },

    /**
     * "O tempo de todas as ações aumentam em +1PA. Não pode realizar Ações Livres e Reações.
     * Personagens que ataquem alvos Confusos se tornam Favorecidos em Perícias de Ataque."
     */
    CONFUSO("O tempo de todas as ações aumentam em +1PA. Não pode realizar Ações Livres e Reações. "
            + "Personagens que ataquem alvos Confusos se tornam Favorecidos em Perícias de "
            + "Ataque.") {
        @Override
        public Set<AttackerFavour> getAttackerFavours() {
            return EnumSet.of(AttackerFavour.ATTACK_ROLL);
        }

        @Override
        public boolean refuses(final ActionKind kind) {
            return kind == ActionKind.FREE_ACTION || kind == ActionKind.REACTION;
        }

        @Override
        public int getActionPointSurcharge() {
            return 1;
        }
    },

    /**
     * "Estado de Desprevenido … Deve rolar 1d6 sempre que efetuar uma rolagem de Perícia Física …
     * Personagens que ataquem alvos Cegos se tornam Favorecidos em Perícias de Ataque e Esquiva e
     * Aparar." The 1d6 is the caller's to throw ({@code SkillRoll#withBlindCheck}), judged by
     * {@code CombatantSheet#getBlindCheckThreshold}.
     */
    // The reach-based thresholds and the Física-only scope are real (core 0.1.5, BlindCheck.Reach).
    // TODO: "Falha automaticamente em rolagens de Atenção para fins visuais" — a roll does not say
    //  what it is *for*, so a visual Atenção cannot be told from a hearing one.
    // TODO: a non-attack Perícia "que afete outros personagens … ou o cenário" is read as personal:
    //  a roll does not name what it affects.
    CEGO("Estado de Desprevenido. Falha automaticamente em rolagens de Atenção para fins visuais. "
            + "Deve rolar 1d6 sempre que efetuar uma rolagem de Perícia Física (baseada em Força ou "
            + "Destreza). Perícias de efeitos pessoal falham com resultados 2 ou menos, "
            + "independentemente de sucessos na rolagem de Perícia. Perícias que afetem outros "
            + "personagens adjacentes ou o cenário falham com resultados 3 ou menos, "
            + "independentemente de sucessos na rolagem de Perícia. Perícias que afetem outros "
            + "personagens além de adjacente falham com resultados 5 ou menos, independentemente "
            + "de sucessos na rolagem de Perícia. Personagens que ataquem alvos Cegos se tornam "
            + "Favorecidos em Perícias de Ataque e Esquiva e Aparar.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(DESPREVENIDO);
        }

        @Override
        public Set<AttackerFavour> getAttackerFavours() {
            return EnumSet.of(AttackerFavour.ATTACK_ROLL, AttackerFavour.DEFENCE_AGAINST_HOLDER);
        }
    },

    /**
     * "Efeitos de Cura e de recuperação de Bônus Bases são reduzidos à zero. Duração conforme
     * origem da condição."
     */
    FERIDAS_DOLOROSAS("Efeitos de Cura e de recuperação de Bônus Bases são reduzidos à zero. "
            + "Duração conforme origem da condição.") {
        @Override
        public boolean preventsHealing() {
            return true;
        }

        @Override
        public boolean preventsResourceRecovery() {
            return true;
        }
    },

    /** "Personagem não podem ativar Habilidades de Aventyrs ou de Monstros e não podem Conjurar Magias." */
    SILENCIO("Personagem não podem ativar Habilidades de Aventyrs ou de Monstros e não podem "
            + "Conjurar Magias. Duração conforme origem da condição.") {
        @Override
        public boolean preventsAbilityActivation() {
            return true;
        }

        @Override
        public boolean preventsSpellCasting() {
            return true;
        }
    },

    /**
     * "Estado de Fraqueza e sofre Dano Natural contínuo. Quantidade de danos conforme origem do
     * efeito … Pode aplicar Redução de Multiplicadores de Bônus Bases …" Every magnitude belongs to
     * whatever inflicted it, carried on the held {@link Condition}'s extra effects — Espinhos
     * Venenos de Gaea's, Inocular Veneno's and Veneno Vampírico's -1 Multiplicador de PV each.
     */
    // The continuous Dano Natural is real (core 0.1.5) through a held Poisoning, which carries the
    // Veneno's own figure. A bare Condition(ENVENENADO, …) is the Fraqueza alone.
    ENVENENADO("Estado de Fraqueza e sofre Dano Natural contínuo. Quantidade de danos conforme "
            + "origem do efeito. Efeitos adicionais conforme descrição do veneno. Pode aplicar "
            + "Redução de Multiplicadores de Bônus Bases, Redução Temporário de Egos, Redução de "
            + "PD, Redução de PM, Redutores de Pontos de Ação.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(FRAQUEZA);
        }
    },

    /**
     * The hidden state a character enters by using Furtividade — a Condição rather than a
     * Malefício, and the one entry here that helps its holder.
     *
     * <p><b>The one condition with a magnitude</b>, and so the one held as a subclass: {@link
     * Hidden} carries the Furtividade total an observer's Atenção must reach, plus whoever has
     * already reached it. Applied and lifted through {@code
     * org.aventyrs.core.character.services.HidingService}, never by hand.
     *
     * <p>No {@link ConditionEffect}s and no implications: what being hidden is <i>worth</i>
     * belongs to whichever trait says so ({@code AssassinoFeat#ESCUDO_DE_SOMBRAS}).
     */
    ESCONDIDO("Estado de ocultação obtido ao utilizar a Perícia Furtividade.") {
        @Override
        public boolean isMaleficio() {
            return false;
        }
    },

    /**
     * "Estado de Fraqueza. Efeitos adicionais conforme descrição da doença." The rest — redutores
     * em Atributos, Desacordado, Feridas Dolorosas, Silêncio, propagation — belongs to the disease.
     */
    // Propagation is real (core 0.1.5): a held Disease that spreads, and DiseaseService for the
    // adjacent 1d6. The infecting creature is immune to its own disease (applyCondition).
    // ⚠️ "Monstros capazes de adoecer" is read as the infecting creature itself: a MonsterSheet keeps
    //  no template identity, so another creature of the same kind is not recognised as immune.
    DOENTE("Estado de Fraqueza. Efeitos adicionais conforme descrição da doença. Pode aplicar "
            + "Redutores Temporários em Atributos, Estado de Desacordado, Feridas Dolorosas e "
            + "Silêncio.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(FRAQUEZA);
        }
    },

    /**
     * Being possessed — a Malefício named by {@code VidaSpell#EXORCIZAR} and {@code
     * VidaSpell#CORPO_FECHADO}, authored from the Fantasma's <i>Possessão Furiosa</i> in {@code
     * docs/rules/magias.txt} (the conditions file does not list it).
     */
    // TODO: "sempre ataca o aliado mais próximo" is forced attack targeting nothing redirects.
    // TODO: "Magias que não sejam Raciais" over-prohibits — nothing classifies a Magia as Racial.
    POSSESSAO("O alvo é possuído por 2 Rodadas e sempre ataca o aliado mais próximo. Personagens "
            + "possuídos desta forma não podem Conjurar Magias que não sejam Raciais ou ativar "
            + "Habilidades Aventyr, mas são beneficiados por efeitos ativos ou passivos.") {
        @Override
        public boolean preventsAbilityActivation() {
            return true;
        }

        @Override
        public boolean preventsSpellCasting() {
            return true;
        }
    },

    /**
     * Petrificação — Olhar de Lacerto's Olhar Petrificador: "o alvo é petrificado por 2 Rodadas,
     * sendo incapaz de realizar ações … A petrificação é um efeito de Encantamento." Not a
     * Malefício: it is an Encantamento, held as a {@link Petrification}.
     */
    PETRIFICADO("O alvo é petrificado, sendo incapaz de realizar ações. A petrificação é um efeito "
            + "de Encantamento.") {
        /** "Incapaz de realizar ações" — every one; a defence is not an action. */
        @Override
        public boolean refuses(final ActionKind kind) {
            return kind != ActionKind.DEFENCE;
        }

        @Override
        public boolean isMaleficio() {
            return false;
        }
    };

    /**
     * Implications that always hold, with no proximity scope — {@link Map#of} rejects a null
     * value, and {@code null} is exactly how {@link #getImplied()} spells "always".
     */
    private static Map<ConditionType, Range> alwaysImplies(final ConditionType... implied) {
        if (implied.length == 1) {
            return Collections.singletonMap(implied[0], null);
        }
        Map<ConditionType, Range> map = new EnumMap<>(ConditionType.class);
        for (ConditionType type : implied) {
            map.put(type, null);
        }
        return Collections.unmodifiableMap(map);
    }

    /** Desprevenido's own stated "Redutor de -4 em Defesas". */
    public static final int DESPREVENIDO_DEFENSE_MALUS = -4;

    /** "Condição permanece ativa por 2 Rodadas" — the fear ladder's stated default. */
    public static final int DEFAULT_FEAR_DURATION_IN_ROUNDS = 2;

    /** Desacordado's "não pode realizar ações por 2 Rodadas". */
    public static final int DESACORDADO_DURATION_IN_ROUNDS = 2;

    /**
     * The next rung of the fear ladder after current — Frenesi Assustador's "ativações posteriores do
     * Frenesi Assustador podem progredir a Condição para Assustado e Apavorado". {@code null} (no
     * fear yet) starts at Abalado; Apavorado is the top and stays there.
     */
    public static ConditionType escalateFear(final ConditionType current) {
        if (current == null) {
            return ABALADO;
        }
        return switch (current) {
            case ABALADO -> ASSUSTADO;
            case ASSUSTADO, APAVORADO -> APAVORADO;
            default -> throw new IllegalArgumentException("Not a fear rung: " + current);
        };
    }

    private final String description;

    /**
     * One typed numeric malus this condition imposes, optionally scoped to a proximity band
     * around the condition's origin. {@code within} is {@code null} for an effect that always
     * applies; otherwise the effect counts only while the holder is at that {@link Range} or
     * closer to {@link Condition#getSource()}.
     *
     * <p>Data, not a {@code @Modifier} method, for the same reason {@code ItemBonus} is: a
     * shared class cannot vary the compile-time-fixed {@code ModifierType} of an annotation.
     */
    public record ConditionEffect(ModifierType type, int value, Range within) {

        /**
         * "Perde -N Multiplicador de Pontos de Vida" — the magnitude every authored Veneno states
         * (Espinhos Venenos de Gaea, Inocular Veneno, Veneno Vampírico), carried on the held
         * Envenenado as an extra effect.
         */
        public static ConditionEffect lifeMultiplierLoss(final int multipliers) {
            return new ConditionEffect(ModifierType.LIFE_MULTIPLIER, -multipliers, null);
        }
    }

    /**
     * What a Condição makes its holder's attackers <b>Favorecidos</b> in — the outward half of
     * an Estado nobody holds. Read off the <em>target</em>'s sheet.
     */
    public enum AttackerFavour {
        /** Vantagem on the attack roll against the holder (Flanqueado, Caído, Imobilizado, Confuso, Cego). */
        ATTACK_ROLL,
        /**
         * Vantagem on the attacker's own Esquiva e Aparar against the holder's attacks, for as long
         * as the holder stays under this Condição (Caído, Cego) — granted to anyone who attacked it
         * while it did (table ruling, 2026-10-07).
         */
        DEFENCE_AGAINST_HOLDER
    }

    /**
     * Whether this is a Malefício — a state inflicted on its holder — rather than one they chose.
     * {@code true} for everything but {@link #ESCONDIDO} and {@link #PETRIFICADO}. Read it through
     * {@link #maleficios()} rather than filtering by hand.
     */
    public boolean isMaleficio() {
        return true;
    }

    /**
     * Whether this is an Estado de Personagem — a building block Condições confer — rather than a
     * Condição. Estados are binary: conferred twice, they apply once.
     */
    public boolean isEstado() {
        return false;
    }

    /** Every Malefício — what "todos os Malefícios" means. */
    public static Set<ConditionType> maleficios() {
        return Arrays.stream(values())
                .filter(ConditionType::isMaleficio)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Typed numeric maluses this condition imposes. Empty unless a constant overrides it. */
    public List<ConditionEffect> getEffects() {
        return List.of();
    }

    /**
     * Conditions this one confers, each mapped to the {@link Range} within which it applies —
     * {@code null} meaning always. Resolved transitively by {@code
     * CombatantSheet#getActiveConditions}.
     */
    public Map<ConditionType, Range> getImplied() {
        return Map.of();
    }

    /**
     * The condition this one turns into when its duration runs out — the fear ladder's "Ao fim da
     * duração alvo se torna Abalado". {@code null} for a condition that simply ends.
     */
    public ConditionType getDecaysTo() {
        return null;
    }

    /**
     * This rung's place on the fear ladder — Abalado 1, Assustado 2, Apavorado 3 — and 0 for a
     * condition that is not a fear. A combatant holds one rung at a time and the strongest wins
     * (table ruling, 2026-10-07), compared by this.
     */
    public int getFearRank() {
        return 0;
    }

    /** Whether this is one of the three fear rungs. */
    public boolean isFear() {
        return getFearRank() > 0;
    }

    /**
     * What this condition makes <b>whoever attacks its holder</b> Favorecido in. Empty unless a
     * constant overrides it.
     */
    public Set<AttackerFavour> getAttackerFavours() {
        return Set.of();
    }

    /** Whether this condition halves its holder's Movimento Base — Caído. */
    public boolean halvesMovementBase() {
        return false;
    }

    /**
     * Whether this condition refuses an action of kind. By default the four standing prohibitions
     * below answer it ({@link #preventsMovement()} → {@link ActionKind#MOVEMENT}, and so on); a
     * Condição that forbids "Ações" outright overrides this instead. Never consulted for {@link
     * ActionKind#DEFENCE} by any constant authored today — a defence is not an Ação.
     */
    public boolean refuses(final ActionKind kind) {
        return switch (kind) {
            case MOVEMENT -> preventsMovement();
            case ABILITY_ACTIVATION -> preventsAbilityActivation();
            case SPELL_CAST -> preventsSpellCasting();
            case ARMING -> preventsArming();
            default -> false;
        };
    }

    /**
     * The distance band from the condition's origin within which {@link #refuses} holds — {@code
     * null} for always. Apavorado's "enquanto em Distância Curta da origem do medo".
     */
    public Range getRestrictionRange() {
        return null;
    }

    /** Pontos de Ação added to every action's price — Confuso's "+1PA". */
    public int getActionPointSurcharge() {
        return 0;
    }

    /**
     * Whether its holder's Esquiva e Aparar succeeds unless it is a Falha Crítica — Imobilizado and
     * Desacordado, which may not act but still defend (table ruling, 2026-10-07).
     */
    public boolean defendsUnlessCriticalFailure() {
        return false;
    }

    /** Whether this condition forbids movement outright — "não pode realizar movimentos". */
    public boolean preventsMovement() {
        return false;
    }

    /**
     * Whether this condition allows only an Arma Natural or an {@link
     * org.aventyrs.core.item.ItemWeightClass#LIGHT} weapon — Devorado. Read through {@code
     * CombatantSheet#canAttackWith(Weapon)}.
     */
    public boolean restrictsAttacksToLightWeapons() {
        return false;
    }

    /**
     * Whether this condition stops its holder getting a weapon into their hands at all — Devorado.
     * Covers both {@code CombatantSheet#rearm(Weapon)} and {@code WeaponDrawService#draw}.
     */
    public boolean preventsArming() {
        return false;
    }

    /** Whether this condition forbids recovering Pontos de Vida by any means. */
    public boolean preventsHealing() {
        return false;
    }

    /**
     * Whether this condition forbids recovering the other Bônus Bases — PM and PD — by any means:
     * Feridas Dolorosas' "recuperação de Bônus Bases são reduzidos à zero".
     */
    public boolean preventsResourceRecovery() {
        return false;
    }

    /** Whether this condition forbids activating Habilidades de Aventyr or de Monstro. */
    public boolean preventsAbilityActivation() {
        return false;
    }

    /** Whether this condition forbids Conjurar Magias. */
    public boolean preventsSpellCasting() {
        return false;
    }
}
