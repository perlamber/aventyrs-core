package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * Habilidades Raciais granted to every Goblin — see {@link Race#getRacialAbilities()} for why
 * these are modeled as ordinary {@link SkillCompetencyAbility} instances rather than a separate
 * type.
 *
 * <p>These two are the exact complement of one another, and that is what makes both safe to
 * grant at once. {@link #PODER_DOS_NUMEROS} fires when an ally is within {@link
 * Range#DISTANCIA_CURTA}; {@link #AUTODESCONFIANCA_EM_COMBATE} fires when none is — the rules
 * text's "por estarem distantes (Distância Média ou Superior)" is that same line drawn from the
 * other side. So the Vantagem and the Desvantagem on a dano roll can never both apply, and
 * {@code AbstractSkillInteraction}'s first-non-empty {@code resolveDamageBonus} lookup never has
 * to break a tie between them.
 */
@Getter
@AllArgsConstructor
public enum GoblinsRacialAbility implements SkillCompetencyAbility {

    /**
     * Real in both halves. The Perícia half is a {@code resolveConditionalRollBonus} rather than
     * a flat {@code @Modifier}, because a no-arg annotated method can't see the {@code
     * SceneContext} this proximity check needs.
     *
     * <p>{@code getSkillType()} stays the enum-level representative value and {@link
     * #matchesSkillType} is widened to every Perícia, the same shape {@code
     * AnoesRacialAbility#ABATEDORES_DE_GIGANTES} uses for its own "every Perícia de Ataque"
     * scope — here the scope is genuinely <i>every</i> Perícia ("recebem Vantagens em suas
     * rolagens de Perícias"). Which constant {@code getSkillType()} reports doesn't otherwise
     * matter: {@code sumConditionalRollBonuses} and {@code resolveDamageBonus} are both scanned
     * with no per-{@code SkillType} filter.
     */
    PODER_DOS_NUMEROS("Enquanto próximo de seus aliados (Distância Curta ou inferior), recebem " +
            "Vantagens em suas rolagens de Perícias, se pelo menos um destes aliados for outro " +
            "Goblin recebem também vantagem em suas rolagens de dano.") {
        @Override
        public Optional<Integer> resolveConditionalRollBonus(final SceneContext sceneContext, final SkillTrait requestedAbility) {
            if (sceneContext == null || !sceneContext.hasAllyWithin(Range.DISTANCIA_CURTA)) {
                return Optional.empty();
            }
            return Optional.of(Skill.ADVANTAGE_BONUS);
        }

        // Overrides the 4-arg overload per the cascading convention, though only sceneContext is
        // read: the clause is about who is standing near the *roller*, not who is being attacked.
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor) {
            if (sceneContext == null || !hasNearbyGoblinAlly(sceneContext)) {
                return Optional.empty();
            }
            return Optional.of(new DamageBonus(Skill.ADVANTAGE_BONUS, DamageType.FISICO));
        }

        @Override
        public boolean matchesSkillType(final SkillType requestedSkillType) {
            return requestedSkillType != null;
        }
    },

    /**
     * The Desvantagem em rolagens de Danos half is real; the Malefício Abalado half is not.
     *
     * <p>Two narrowings of the "nenhum aliado que possa ajudá-los" condition are deliberately
     * unmodeled, and both make this constant apply <i>less</i> often than the rules text says,
     * never more — the safe direction for a malus:
     * <ul>
     *   <li><b>Unconscious allies</b> ("ou inconscientes") still count as help here. Resolving it
     *   needs each nearby ally's {@code CharacterStatus}, which only {@code
     *   HitPointsService#getStatus} can answer — a service an enum constant has no way to reach
     *   without constructing one itself, which no ability in this codebase does.</li>
     *   <li><b>Malefício Abalado</b> — no Malefício taxonomy exists anywhere in this core (the
     *   same gap {@code Withering}/{@code ABRIR_DEFESAS} cite), so there is no condition to
     *   inflict.</li>
     * </ul>
     */
    AUTODESCONFIANCA_EM_COMBATE("Enquanto em combate, se não houver nenhum aliado que possa " +
            "ajudá-los, seja por estarem sozinhos, por estarem distantes (Distância Média ou " +
            "Superior) ou inconscientes, os Goblins recebem o Malefício Abalado e Desvantagem " +
            "em suas rolagens de Danos.") {
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor) {
            if (sceneContext == null || !sceneContext.isCombatScene()) {
                return Optional.empty();
            }
            if (sceneContext.hasAllyWithin(Range.DISTANCIA_CURTA)) {
                return Optional.empty();
            }
            return Optional.of(new DamageBonus(Skill.DISADVANTAGE_MALUS, DamageType.FISICO));
        }

        @Override
        public boolean matchesSkillType(final SkillType requestedSkillType) {
            return requestedSkillType != null;
        }
    };

    private final String description;

    /**
     * Whether at least one ally within {@link Range#DISTANCIA_CURTA} is itself a Goblin — the
     * extra condition on {@link #PODER_DOS_NUMEROS}' dano half. Read off each ally's own {@link
     * Race} instance, so a {@code NascidoDoDragao} whose parente is a Goblin does <i>not</i>
     * count: it is a different Raça, and its own rules text inherits Características, not
     * membership.
     */
    private static boolean hasNearbyGoblinAlly(final SceneContext sceneContext) {
        return sceneContext.getAlliesWithin(Range.DISTANCIA_CURTA).stream()
                .anyMatch(ally -> ally.getCharacter().getRace() instanceof Goblin);
    }

    /**
     * A representative value only — both constants apply to every Perícia, and neither is ever
     * itself a roll's {@code requestedAbility}. See {@link #PODER_DOS_NUMEROS}' own javadoc.
     */
    @Override
    public SkillType getSkillType() {
        return SkillType.FURTIVIDADE;
    }
}
