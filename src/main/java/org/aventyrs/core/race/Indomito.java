package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.skill.SkillType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines what the Indômitos (the felinos Apedemak, Bastet and Sacmis, plus the Impuros born
 * between them) can do under each rule-set. Like {@code Aviano}/{@code Ogro}, an Indômito carries
 * one acquisition-time choice — its {@link Tribo} — held as a constructor field feeding this
 * object's own {@link #getFixedAttributeBonuses()}.
 *
 * <p><b>Impuro is a {@link Tribo}, not a Mestiço.</b> It is born of two Indômito parents of
 * different tribes, so there is no second {@link Race} to choose and nothing for {@code
 * isMestico()} to guard against chaining — the whole Mestiço apparatus ({@code parentRace},
 * inherited Características, a size offset read off a parent) has nothing to operate on here.
 * Its distinctness is entirely in its own bonus row and its own Ferocidade timing.
 *
 * <p>Two of this race's traits are mechanically real today: {@link #getFixedAttributeBonuses()}
 * (+1 Força for every Indômito, plus the chosen {@link Tribo}'s own — a second +1 Força and a +1
 * Vigor for an {@link Tribo#IMPURO}) and {@link #getCreatureType()} ({@link
 * CreatureType#MONSTRUOSO}, "na linha tênue entre um ser monstruoso e um monstro verdadeiro").
 * Categoria de Tamanho 0 needs no override — {@link
 * org.aventyrs.core.character.SizeCategory#ZERO} is already {@link Character}'s own default.
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>The Impuro's -1 Autocontrole</b> — Autocontrole is an {@code EgoDomain}, not an {@link
 *   AttributeDomain}, and {@link #getFixedAttributeBonuses()} is the only racial-bonus hook
 *   {@link Race} has. There is no {@code getFixedEgoBonuses()} counterpart, and {@code
 *   CharacterCreationServiceImpl} allocates Egos from the player's own points with no racial
 *   stage at all — so a racial Ego bonus or malus has nowhere to be expressed. This is the first
 *   trait in the codebase to need one; it is the whole shape that is missing, not a reader.</li>
 *   <li><b>Ímpeto do Caçador</b> ("na primeira rodada de combate o Movimento Base aumenta em
 *   +2m") — {@code ModifierType#MOVEMENT} is real and {@code
 *   SceneContext#isWithinFirstCombatRounds(1)} expresses the condition exactly, but the two
 *   cannot meet: {@code MovementService#getMovementBase} takes only a {@link Character}, with no
 *   {@code SceneContext} overload, and a no-arg {@code @Modifier} could not read one anyway.
 *   Granting it flat would raise every Indômito's Movimento Base permanently. Note also that this
 *   clause is written in <b>metres</b> where every other Movimento clause in the ruleset uses UD
 *   — a source-document inconsistency, left as written rather than silently converted.</li>
 *   <li><b>Ferocidade de Lacerto</b> (from the fourth turno de combate — the third for an Impuro
 *   — RD, Vantagem em Ataque Corpo-a-Corpo e Danos, forced targeting of the nearest character, no
 *   Perícias requiring concentration; a temporary Autocontrole point prevents or ends it) — the
 *   round condition <i>is</i> expressible today ({@code !isWithinFirstCombatRounds(3)}), and the
 *   Vantagem half would fit {@code resolveConditionalRollBonus} cleanly. It is withheld anyway,
 *   because the clause is a <i>state that can be declined</i>: nothing tracks whether this
 *   Indômito is currently ferocious, and {@code EgoPointsService#useEgoPointsForEffect} has no
 *   notion of spending a point to suppress an ongoing effect. Granting the bonus on Rodada 4 would
 *   hand it to an Indômito who paid to avoid it — an over-grant this codebase's discipline says to
 *   document rather than make. The remaining halves are separately blocked: RD from a {@code
 *   SkillCompetencyAbility} cannot be conditioned on a {@code SceneContext} (a no-arg {@code
 *   @Modifier} can't see one), forced targeting is the "Forced attack targeting / interception"
 *   gap, and "Perícias que exijam concentração" is not a classification any {@code SkillType}
 *   carries. The text's own "turno de combate" versus "Rodada" wording is a further ambiguity
 *   worth resolving before building this.</li>
 *   <li><b>Monstros em Potencial</b> (an Indômito stops counting as Monstruoso and becomes a
 *   Monstro after 3 Talentos Monstruosos, 2 for an Impuro; every 2 such Talentos also brings
 *   Ferocidade forward by 1 Rodada) — counting them is trivial ({@code character.getFeats()}
 *   filtered by {@code FeatCategory#MONSTRUOSO}), but there is nowhere to put the answer:
 *   {@link CreatureType} has only the three constants and no MONSTRO, deliberately (see its own
 *   javadoc), and {@link #getCreatureType()} takes no {@link Character}, so a creature type that
 *   changes with what its holder acquired cannot be expressed. The Ferocidade half depends on
 *   Ferocidade itself.</li>
 *   <li><b>Visão no Escuro</b> (8m, monochromatic) — no vision/senses concept exists in this
 *   core.</li>
 *   <li><b>Garras Afiadas</b> as an Arma Natural — the usual two-markers-missing gap (no weapon
 *   catalog is authored, and nothing marks a weapon as natural).</li>
 *   <li><b>Idiomas</b> (Silvestre + Continental) — same "no Language/Idioma concept exists" gap
 *   as every other race.</li>
 *   <li><b>Longevidade</b> (~80 anos) — same "no age/lifespan concept" gap as every other race.</li>
 *   <li><b>Treinamento adicional em Ataque Corpo-a-Corpo com uma Habilidade de Competência, mais
 *   a Perícia tribal com uma Especialização adicional, mais 1 Talento</b> (Sobrevivência ou
 *   Monstruoso) — {@link Race} has no hook to grant starting Perícia training/abilities nor a
 *   {@code Feat} at creation. The per-tribe Perícia is recorded on each {@link Tribo} constant
 *   even so, since it is exact authored data waiting only on the hook.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race.
 */
@Getter
public class Indomito implements Race {

    /**
     * The four Indômito lineages. Each carries the Atributo bonuses it adds on top of the +1
     * Força every Indômito receives, and the Perícia its members are additionally trained in.
     *
     * <p>{@code additionalTraining} has <b>no consumer yet</b> — {@link Race} has no hook for
     * granting starting Perícia training — but it is exact authored data, recorded here per this
     * codebase's "can't apply it yet doesn't mean can't compute it yet" discipline rather than
     * left in prose. {@link #SACMIS} is the one lineage whose training is a player's choice
     * between two Perícias ("Conhecimentos ou Medicina e Cura, a escolha do jogador"), so it is
     * the one constant holding a list of more than one; resolving that choice is deferred with
     * the granting hook itself, not guessed at here.
     */
    @Getter
    @AllArgsConstructor
    public enum Tribo {

        /** "Fortes guerreiros" — +1 Instinto; treinados em Esquiva e Aparar. */
        APEDEMAK(Map.of(AttributeDomain.INSTINCT, 1),
                List.of(SkillType.ESQUIVA_E_APARAR)),

        /** "Corpo ágil e certa afinidade com a magia" — +1 Foco; treinados em Furtividade. */
        BASTET(Map.of(AttributeDomain.FOCUS, 1),
                List.of(SkillType.FURTIVIDADE)),

        /** "Estudiosos, portadores das verdades e curandeiros" — +1 Gnose; Conhecimentos ou Medicina e Cura. */
        SACMIS(Map.of(AttributeDomain.GNOSE, 1),
                List.of(SkillType.CONHECIMENTOS,
                        SkillType.MEDICINA_E_CURA)),

        /**
         * The híbridos the tribes cast out — a second +1 Força (for +2 in total) and +1 Vigor;
         * treinados em Atletismo. Their -1 Autocontrole is <b>not</b> here: this map is typed to
         * {@link AttributeDomain}, and Autocontrole is an {@code EgoDomain}. See the class
         * javadoc.
         */
        IMPURO(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.VIGOR, 1),
                List.of(SkillType.ATLETISMO));

        private final Map<AttributeDomain, Integer> attributeBonuses;
        private final List<SkillType> additionalTraining;
    }

    private final Tribo tribo;

    public Indomito(@NonNull final Tribo tribo) {
        this.tribo = tribo;
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTRUOSO;
    }

    /**
     * +1 Força for every Indômito, merged additively with the chosen {@link Tribo}'s own row —
     * so an {@link Tribo#IMPURO} reads as a single {@code STRENGTH -> 2} entry, exactly as its
     * rules text states ("para um total de Força +2"), rather than two separate ones.
     */
    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        final Map<AttributeDomain, Integer> bonuses = new HashMap<>();
        bonuses.merge(AttributeDomain.STRENGTH, 1, Integer::sum);
        tribo.getAttributeBonuses().forEach((domain, value) -> bonuses.merge(domain, value, Integer::sum));
        return Map.copyOf(bonuses);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }
}
