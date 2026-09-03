package org.aventyrs.core.feat;

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
        if (sceneContext == null || !chosenMethod.matches(attackSource, character)) {
            return 0;
        }
        Range distanceToTarget = sceneContext.getDistanceTo(sceneContext.getOpposedCharacter());
        boolean atOrBeyondMedia = distanceToTarget != null && !distanceToTarget.isWithin(Range.DISTANCIA_CURTA);
        return atOrBeyondMedia ? Skill.ADVANTAGE_BONUS : 0;
    }
}
