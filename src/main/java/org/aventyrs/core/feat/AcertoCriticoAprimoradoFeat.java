package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The acquired, per-character form of {@link AssassinoFeat#ACERTO_CRITICO_APRIMORADO}, carrying
 * the {@link AttackMethod} chosen ("um Tipo de Arma ou Conjuração de Magias"). Grant <em>this</em>
 * in {@code Character#feats} in place of the bare enum constant — the same split {@link
 * FocoEmPericiaFeat} keeps against {@code PeritoFeat#FOCO_EM_PERICIA}.
 *
 * <p>The rules text says "Margem Crítica <b>Menor</b>", and that is exactly what this widens:
 * {@code SkillRoll#getCriticalResult(int)} applies its margin to the Menor tier only, with Acerto
 * Crítico Maior staying a literal triple-6.
 */
@Getter
public final class AcertoCriticoAprimoradoFeat extends AbstractFeat {

    /** ACERTO_CRITICO_APRIMORADO's own stated "+1" to the Margem Crítica Menor. */
    private static final int MARGIN_INCREASE = 1;

    private final AttackMethod chosenMethod;

    public AcertoCriticoAprimoradoFeat(@NonNull final AttackMethod chosenMethod) {
        super(AssassinoFeat.ACERTO_CRITICO_APRIMORADO.getFeatCategory(),
                AssassinoFeat.ACERTO_CRITICO_APRIMORADO.getDescription(),
                AssassinoFeat.ACERTO_CRITICO_APRIMORADO.getFeatRequirements());
        this.chosenMethod = chosenMethod;
    }

    public static AcertoCriticoAprimoradoFeat of(@NonNull final AttackMethod chosenMethod) {
        return new AcertoCriticoAprimoradoFeat(chosenMethod);
    }

    /** The método a character chose, if they hold this Talento. Mirrors {@link FocoEmPericiaFeat#chosenBy}. */
    public static Optional<AttackMethod> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(AcertoCriticoAprimoradoFeat.class::isInstance)
                .map(AcertoCriticoAprimoradoFeat.class::cast)
                .map(AcertoCriticoAprimoradoFeat::getChosenMethod)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return AssassinoFeat.ACERTO_CRITICO_APRIMORADO;
    }

    /**
     * "Sua Margem Crítica Menor com o tipo de arma escolhida, ou das magias que você conjurar, é
     * aumentada em +1" — only on an attack roll delivered with the chosen method.
     */
    @Override
    public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                              final Character character, final AttackSource attackSource) {
        return skillType.isAttackSkill() && chosenMethod.matches(attackSource, character)
                ? MARGIN_INCREASE : 0;
    }
}
