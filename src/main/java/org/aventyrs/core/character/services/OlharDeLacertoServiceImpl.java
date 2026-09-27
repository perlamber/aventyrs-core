package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Gorgona;
import org.aventyrs.core.race.OlharDeLacerto;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Petrification;
import org.aventyrs.core.sheet.TargetScope;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.OLHAR_DE_LACERTO_NOT_HELD;

public class OlharDeLacertoServiceImpl implements OlharDeLacertoService {

    private final DeterminationPointsService determinationPointsService;
    private final HitPointsService hitPointsService;

    public OlharDeLacertoServiceImpl() {
        this(new DeterminationPointsServiceImpl(), new HitPointsServiceImpl());
    }

    public OlharDeLacertoServiceImpl(@NonNull final DeterminationPointsService determinationPointsService,
                                     @NonNull final HitPointsService hitPointsService) {
        this.determinationPointsService = determinationPointsService;
        this.hitPointsService = hitPointsService;
    }

    @Override
    public boolean hasGaze(@NonNull final CombatantSheet sheet) {
        Character character = sheet.getCharacter();
        return character != null && character.getRace() instanceof Gorgona
                && !sheet.getRacialTraitSuppression().suppressesInnateTraits();
    }

    @Override
    public Gaze declare(@NonNull final CombatantSheet gorgona, @NonNull final CombatantSheet target) {
        if (!hasGaze(gorgona)) {
            throw new IllegalOperationException(OLHAR_DE_LACERTO_NOT_HELD);
        }
        Character character = gorgona.getCharacter();
        if (determinationPointsService.getCurrentDeterminationPoints(character, gorgona) < DETERMINATION_POINT_COST) {
            throw new IllegalOperationException(NOT_ENOUGH_DETERMINATION_POINTS);
        }
        gorgona.spendDeterminationPoints(DETERMINATION_POINT_COST);
        int rangeSteps = character.getFeats().stream()
                .mapToInt(feat -> feat.resolveAttackRangeIncrease(character, OlharDeLacerto.INSTANCE))
                .sum();
        boolean petrifying = gorgona.isInForm(FormType.MONSTRUOSA);
        int attempts = petrifying ? target.incrementCombatCounter(attemptKey(gorgona)) - 1 : 0;
        return new Gaze(ACTION_POINT_COST, OlharDeLacerto.BASE_RANGE.increasedBy(rangeSteps),
                petrifying ? 2 : 1,
                character.getEffectiveAttributeTotal(AttributeDomain.CHARISMA) / 2,
                OlharDeLacerto.DAMAGE, petrifying, attempts);
    }

    @Override
    public boolean applyHit(@NonNull final CombatantSheet gorgona, @NonNull final CombatantSheet target,
                            @NonNull final Gaze gaze) {
        target.grantBlessing(new Blessing(ModifierType.ACTION_POINTS, ACTION_POINT_MALUS, MALUS_ROUNDS,
                TargetScope.SELF, OlharDeLacerto.INSTANCE.name()));
        if (!gaze.petrifying()) {
            return false;
        }
        boolean fallen = hitPointsService.getMaxHitPoints(target.getCharacter(), target) - target.getDamageTaken() <= 0;
        return target.applyEnchantment(new Petrification(gorgona, fallen ? null : PETRIFICATION_ROUNDS));
    }

    /** "Um mesmo alvo" — attempts are counted on the target, per Górgona. */
    private static Object attemptKey(final CombatantSheet gorgona) {
        return OlharDeLacerto.class.getName() + "#" + gorgona.getId();
    }
}
