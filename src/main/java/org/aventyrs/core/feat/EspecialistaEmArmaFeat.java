package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The acquired, per-character form of {@link DuelistaFeat#ESPECIALISTA_EM_ARMA}, carrying the
 * {@link AttackMethod} chosen ("um tipo de arma, armas naturais ou magias ofensivas"). Grant
 * <em>this</em> in {@code Character#feats} in place of the bare enum constant — the same split
 * {@link FocoEmPericiaFeat} keeps against {@code PeritoFeat#FOCO_EM_PERICIA}.
 *
 * <p>{@link DuelistaFeat#MAESTRIA_EM_ARMA} and the method-scoped half of {@code
 * DuelistaFeat#DOMINAR_ARMAS} read the choice via {@link #chosenBy}.
 */
@Getter
public final class EspecialistaEmArmaFeat extends AbstractFeat {

    private final AttackMethod chosenMethod;

    public EspecialistaEmArmaFeat(@NonNull final AttackMethod chosenMethod) {
        super(DuelistaFeat.ESPECIALISTA_EM_ARMA.getFeatCategory(),
                DuelistaFeat.ESPECIALISTA_EM_ARMA.getDescription(),
                DuelistaFeat.ESPECIALISTA_EM_ARMA.getFeatRequirements());
        this.chosenMethod = chosenMethod;
    }

    public static EspecialistaEmArmaFeat of(@NonNull final AttackMethod chosenMethod) {
        return new EspecialistaEmArmaFeat(chosenMethod);
    }

    /** The método a character chose, if they hold this Talento. Mirrors {@link FocoEmPericiaFeat#chosenBy}. */
    public static Optional<AttackMethod> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(EspecialistaEmArmaFeat.class::isInstance)
                .map(EspecialistaEmArmaFeat.class::cast)
                .map(EspecialistaEmArmaFeat::getChosenMethod)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return DuelistaFeat.ESPECIALISTA_EM_ARMA;
    }

    /**
     * "Receba vantagem nas rolagens de ataque com a arma escolhida, ou com suas magias ofensivas
     * (que exijam uma rolagem de perícia de Ataque), conforme escolhido." — a Perícia de Ataque
     * roll delivered with attackSource matching the chosen method.
     */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                      final SkillTrait requestedAbility, final Character character,
                                      final AttackSource attackSource) {
        return skillType.isAttackSkill() && chosenMethod.matches(attackSource, character)
                ? Skill.ADVANTAGE_BONUS : 0;
    }
}
