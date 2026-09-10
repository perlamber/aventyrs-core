package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Optional;

/**
 * The acquired, per-character form of {@link AssassinoFeat#SAQUE_RELAMPAGO}, carrying the
 * {@link WeaponOrSpellChoice} chosen ("Escolha entre Armas ou Magias"). Grant <em>this</em> in
 * {@code Character#feats} in place of the bare enum constant — the same split {@link
 * AcertoCriticoAprimoradoFeat} keeps against {@code AssassinoFeat#ACERTO_CRITICO_APRIMORADO}.
 *
 * <p>The "-1 nível" half is applied through {@code DifficultyLevel#easier} on the direct
 * skill-roll path and by {@code AttackReceiver}; on the {@code AttackDelivery} flat-Defesa path
 * it is reported on {@code DeliveredAttackResult#getUnappliedDifficultyReduction()} without being
 * applied, since a foe's authored Defesa has no tier to ease (the {@code AttackDelivery} "Open
 * question").
 *
 * <p>The <b>Vantagem rider</b> ("imediatamente após sacar sua primeira arma, ou a primeira magia
 * conjurada, na Cena de Combate") is granted through {@link Feat#resolveSkillRollBonus}'s {@code
 * CombatantSheet} overload: for {@link WeaponOrSpellChoice#WEAPONS}, when a weapon was drawn this
 * Turn ({@code holder.hasDrawnWeaponThisTurn()}), this is the Turn's first attack roll, and no
 * qualifying attack is yet in {@code holder.getActionsThisCena()}; for {@link
 * WeaponOrSpellChoice#SPELLS}, when this is the first spell attack recorded in the Cena. The
 * spell branch depends on the API recording spell attack rolls in the action log (a plain cast
 * via {@code SpellCastingService} still records nothing).
 */
@Getter
public final class SaqueRelampagoFeat extends AbstractFeat {

    /** SAQUE_RELAMPAGO's stated "-1 nível" to the Perícia de Ataque's GD. */
    private static final int DIFFICULTY_REDUCTION = 1;

    private final WeaponOrSpellChoice chosenMethod;

    public SaqueRelampagoFeat(@NonNull final WeaponOrSpellChoice chosenMethod) {
        super(AssassinoFeat.SAQUE_RELAMPAGO.getFeatCategory(),
                AssassinoFeat.SAQUE_RELAMPAGO.getDescription(),
                AssassinoFeat.SAQUE_RELAMPAGO.getFeatRequirements());
        this.chosenMethod = chosenMethod;
    }

    public static SaqueRelampagoFeat of(@NonNull final WeaponOrSpellChoice chosenMethod) {
        return new SaqueRelampagoFeat(chosenMethod);
    }

    /** The método a character chose, if they hold this Talento. Mirrors {@link AcertoCriticoAprimoradoFeat#chosenBy}. */
    public static Optional<WeaponOrSpellChoice> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(SaqueRelampagoFeat.class::isInstance)
                .map(SaqueRelampagoFeat.class::cast)
                .map(SaqueRelampagoFeat::getChosenMethod)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return AssassinoFeat.SAQUE_RELAMPAGO;
    }

    /**
     * "-1 nível" on the first attack of the chosen method costing at most 1PA (a Reação or Ação
     * Livre spends 0, so both qualify) that this holder makes each Rodada — the round's history
     * is {@code actionsThisRound}, filtered by the same predicate.
     */
    @Override
    public int resolveAttackCostDifficultyReduction(final SkillType skillType, final SceneContext sceneContext,
            final Character character, final AttackSource attackSource, final ActionCost actionCost,
            final List<CombatantAction> actionsThisRound) {
        if (!skillType.isAttackSkill() || actionCost == null) {
            return 0;
        }
        if (actionCost.spentActionPoints() > 1 || !chosenMethod.matches(attackSource)) {
            return 0;
        }
        boolean alreadyQualifiedThisRound = actionsThisRound.stream().anyMatch(this::isQualifyingAttack);
        return alreadyQualifiedThisRound ? 0 : DIFFICULTY_REDUCTION;
    }

    private boolean isQualifyingAttack(final CombatantAction action) {
        return action.skill() != null && action.skill().isAttackSkill()
                && action.cost() != null && action.cost().spentActionPoints() <= 1
                && chosenMethod.matches(action.attackSource());
    }

    /**
     * The Vantagem rider — "imediatamente após sacar sua primeira arma / a primeira magia
     * conjurada, na Cena de Combate". See the class javadoc for the two branches.
     */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                      final SkillTrait requestedAbility, final Character character,
                                      final AttackSource attackSource, final CombatantSheet holder) {
        if (!skillType.isAttackSkill() || holder == null || !chosenMethod.matches(attackSource)) {
            return 0;
        }
        boolean firstOfCena = holder.getActionsThisCena().stream()
                .noneMatch(action -> action.skill() != null && action.skill().isAttackSkill()
                        && chosenMethod.matches(action.attackSource()));
        if (!firstOfCena) {
            return 0;
        }
        boolean opener = chosenMethod == WeaponOrSpellChoice.WEAPONS
                ? holder.hasDrawnWeaponThisTurn() && holder.isFirstAttackRollOfTurn()
                : true;   // a spell's "primeira magia conjurada na Cena" is the first-of-Cena check itself
        return opener ? Skill.ADVANTAGE_BONUS : 0;
    }
}
