package org.aventyrs.core.feat;

import java.util.Optional;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

/**
 * The acquired, per-character form of {@link ArtilhariaFeat#ATIRADOR_PERFEITO}, carrying the
 * {@link AttackMethod} chosen ("um tipo de arma de Ataque a Distância ou de Arremesso"). Grant
 * <em>this</em> in {@code Character#feats} in place of the bare enum constant — the same split
 * {@link FocoEmPericiaFeat} keeps against {@code PeritoFeat#FOCO_EM_PERICIA}.
 */
@Getter
public final class AtiradorPerfeitoFeat extends AbstractFeat {

    private final AttackMethod chosenMethod;

    public AtiradorPerfeitoFeat(@NonNull final AttackMethod chosenMethod) {
        super(ArtilhariaFeat.ATIRADOR_PERFEITO.getFeatCategory(),
                ArtilhariaFeat.ATIRADOR_PERFEITO.getDescription(),
                ArtilhariaFeat.ATIRADOR_PERFEITO.getFeatRequirements());
        this.chosenMethod = chosenMethod;
    }

    public static AtiradorPerfeitoFeat of(@NonNull final AttackMethod chosenMethod) {
        return new AtiradorPerfeitoFeat(chosenMethod);
    }

    /**
     * The método a character chose, if they hold this Talento. Mirrors {@link
     * EspecialistaEmArmaFeat#chosenBy} — read by {@code ArtilhariaFeat#ABATER_A_CACA}/{@code
     * UM_TIRO_UMA_MORTE}, whose "sempre que utilizar o talento 'Atirador Perfeito'" clause fires
     * under the exact same weapon-type and range condition {@link #matchesConditions} checks.
     */
    public static Optional<AttackMethod> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(AtiradorPerfeitoFeat.class::isInstance)
                .map(AtiradorPerfeitoFeat.class::cast)
                .map(AtiradorPerfeitoFeat::getChosenMethod)
                .findFirst();
    }

    /**
     * Whether an attack made by character with attackSource against the {@code
     * SceneContext#getOpposedCharacter()} meets Atirador Perfeito's own condition — the chosen
     * weapon type, and a target at Distância Média or beyond. Shared with the two dependent
     * Talentos so their "sempre que utilizar Atirador Perfeito" scope can never drift from this.
     */
    public static boolean matchesConditions(final AttackMethod chosenMethod, final Character character,
                                            final SceneContext sceneContext, final AttackSource attackSource) {
        if (sceneContext == null || chosenMethod == null || !chosenMethod.matches(attackSource, character)) {
            return false;
        }
        Range distanceToTarget = sceneContext.getDistanceTo(sceneContext.getOpposedCharacter());
        return distanceToTarget != null && !distanceToTarget.isWithin(Range.DISTANCIA_CURTA);
    }

    @Override
    public Feat catalogEntry() {
        return ArtilhariaFeat.ATIRADOR_PERFEITO;
    }

    /**
     * "Vantagem nas rolagens de ataque sempre que atacar inimigos à Distâncias Médias ou
     * superiores enquanto utilizando armas do tipo escolhido." The target is {@code
     * SceneContext#getOpposedCharacter()} — on an attack roll, the combatant being attacked — the
     * same source {@code Feat#resolveCriticalMarginIncrease}'s own javadoc points to for an
     * opponent-conditioned clause.
     */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                      final SkillTrait requestedAbility, final Character character,
                                      final AttackSource attackSource) {
        return matchesConditions(chosenMethod, character, sceneContext, attackSource)
                ? Skill.ADVANTAGE_BONUS : 0;
    }
}
