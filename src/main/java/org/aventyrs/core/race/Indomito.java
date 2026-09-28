package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.StartingFeatSlot;
import org.aventyrs.core.item.NaturalWeapon;
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
 * <p>Ferocidade de Lacerto is real as a state ({@link #resolveLacertoFerocityRound}), Garras
 * Afiadas as an Arma Natural ({@link #getGrantedNaturalWeapons()}); so are {@link
 * #getFixedAttributeBonuses()} (+1 Força for every Indômito, plus the chosen {@link Tribo}'s own — a second +1 Força and a +1
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
 *   <li><b>Ferocidade de Lacerto</b>'s forced targeting and concentration block — the state itself
 *   is real ({@link #resolveLacertoFerocityRound}, entered, declined and ended through {@code
 *   LacertoFerocityService}; RD and the two Vantagens reach play through {@code
 *   sheet.LacertoFerocity}), but "atacam sempre o personagem mais próximo" is the "Forced attack
 *   targeting" gap for an attacker nobody provoked, and "Perícias que exijam concentração" is not a
 *   classification any {@code SkillType} carries. ⚠️ "Turno de combate" is read as the combat
 *   Rodada ({@code BestialFeat#ACEITAR_A_LACERTO}'s own wording for the mimicked state).</li>
 *   <li><b>Monstros em Potencial</b>'s change of kind (an Indômito stops counting as Monstruoso and
 *   becomes a Monstro after 3 Talentos Monstruosos, 2 for an Impuro) — counting them is trivial,
 *   but {@link CreatureType} has only the three constants and no MONSTRO, deliberately (see its own
 *   javadoc), and {@link #getCreatureType()} takes no {@link Character}. Its other half — "para cada
 *   dois Talentos Monstruosos … adiantam em 1 Rodada os efeitos de Ferocidade de Lacerto" — is real,
 *   in {@link #resolveLacertoFerocityRound}.</li>
 *   <li><b>Visão no Escuro</b> (8m, monochromatic) — no vision/senses concept exists in this
 *   core.</li>
 *   <li><b>Idiomas</b> (Silvestre + Continental) — same "no Language/Idioma concept exists" gap
 *   as every other race.</li>
 *   <li><b>Longevidade</b> (~80 anos) — same "no age/lifespan concept" gap as every other race.</li>
 *   <li><b>Treinamento adicional em Ataque Corpo-a-Corpo com uma Habilidade de Competência, mais a
 *   Perícia tribal com uma Especialização adicional, mais 1 Talento</b> (Sobrevivência ou
 *   Monstruoso) — the Talento is built ({@link #getStartingFeatSlots()}); {@link Race} still has no
 *   hook to grant starting Perícia training/abilities. The per-tribe Perícia is recorded on each
 *   {@link Tribo} constant even so, since it is exact authored data waiting only on the hook.</li>
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

    /** "A partir do quarto turno de combate." */
    public static final int FEROCITY_ROUND = 4;

    /** "Impuros são afetados por estas características no terceiro turno de combate." */
    public static final int IMPURO_FEROCITY_ROUND = 3;

    /** Monstros em Potencial: "para cada dois Talentos Monstruosos … adiantam em 1 Rodada". */
    private static final int MONSTROUS_FEATS_PER_ROUND = 2;

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

    /**
     * Ferocidade de Lacerto: "a partir do quarto turno de combate", the third for an {@link
     * Tribo#IMPURO}, brought forward one Rodada for every two Talentos Monstruosos held (Monstros em
     * Potencial). Never before the first Rodada.
     */
    @Override
    public Integer resolveLacertoFerocityRound(final Character character) {
        int round = tribo == Tribo.IMPURO ? IMPURO_FEROCITY_ROUND : FEROCITY_ROUND;
        long monstrous = character == null ? 0 : character.getFeats().stream()
                .filter(feat -> feat.getFeatCategory() == FeatCategory.MONSTRUOSO)
                .count();
        return Math.max(1, round - (int) (monstrous / MONSTROUS_FEATS_PER_ROUND));
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

    /** Garras Afiadas: "Possuem Garras Afiadas como armas naturais." */
    @Override
    public List<NaturalWeapon> getGrantedNaturalWeapons() {
        return List.of(NaturalWeapon.GARRAS_AFIADAS);
    }

    @Override
    public List<StartingFeatSlot> getStartingFeatSlots() {
        return List.of(StartingFeatSlot.race(FeatCategory.SOBREVIVENCIA, FeatCategory.MONSTRUOSO));
    }
}
