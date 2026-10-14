package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.util.DiceRoller;

public class FallServiceImpl implements FallService {

    /** "até o próximo efeito de Descanso" — any tier. */
    static final RestType PAINFUL_WOUNDS_UNTIL = RestType.MINIMO;
    /** Dealt as curse damage, the depth {@code RealExecution} uses for "morte instantânea". */
    private static final int LETHAL_DAMAGE_MULTIPLIER = 2;

    private final CharacterSizeService characterSizeService;
    private final DamageService damageService;
    private final HitPointsService hitPointsService;

    public FallServiceImpl() {
        this(new CharacterSizeServiceImpl(), new DamageServiceImpl(), new HitPointsServiceImpl());
    }

    public FallServiceImpl(final CharacterSizeService characterSizeService, final DamageService damageService,
                           final HitPointsService hitPointsService) {
        this.characterSizeService = characterSizeService;
        this.damageService = damageService;
        this.hitPointsService = hitPointsService;
    }

    @Override
    public int getFallResistance(@NonNull final CombatantSheet sheet) {
        int category = characterSizeService.getEffectiveSizeCategory(sheet).getCategory();
        return Math.max(MINIMUM_FALL_RESISTANCE, FALL_RESISTANCE_PER_SIZE_CATEGORY * category);
    }

    @Override
    public FallResult preview(@NonNull final CombatantSheet sheet, final int heightUd) {
        return measure(sheet, heightUd, 0);
    }

    @Override
    public FallResult fall(@NonNull final CombatantSheet sheet, final int heightUd, final DiceRoller dice,
                           final SceneContext sceneContext) {
        FallResult measured = measure(sheet, heightUd, 0);
        if (measured.extraDice() > 0 && dice == null && !measured.lethal()) {
            throw new IllegalArgumentException("A fall of " + heightUd + "UD needs " + measured.extraDice() + "d6");
        }
        FallResult result = measured.lethal() || measured.extraDice() == 0 ? measured
                : measure(sheet, heightUd, dice.rollD6(measured.extraDice()));
        if (result.lethal()) {
            sheet.applyCurseDamage(LETHAL_DAMAGE_MULTIPLIER
                    * hitPointsService.getMaxHitPoints(sheet.getCharacter(), sheet));
        } else if (result.damage() > 0) {
            damageService.applyDamage(sheet, sceneContext, DamageType.QUEDA, null, result.damage(), false);
        }
        if (result.painfulWounds() && !result.lethal()) {
            sheet.removeCondition(ConditionType.FERIDAS_DOLOROSAS);
            sheet.applyEffectUntilRest(new Condition(ConditionType.FERIDAS_DOLOROSAS, null), PAINFUL_WOUNDS_UNTIL);
        }
        sheet.applyCondition(new Condition(ConditionType.CAIDO, null));
        return result;
    }

    private FallResult measure(final CombatantSheet sheet, final int heightUd, final int rolledDice) {
        if (heightUd < 0) {
            throw new IllegalArgumentException("A fall height is never negative: " + heightUd);
        }
        int resistance = getFallResistance(sheet);
        // "infligido apenas quando a altura for igual ou superior à Resistência à Quedas".
        int excess = heightUd >= resistance ? heightUd - resistance : 0;
        int extraDice = excess / EXCESS_PER_EXTRA_DIE;
        return new FallResult(heightUd, resistance, excess, extraDice, excess / 2 + rolledDice,
                extraDice > 0, excess > LETHAL_EXCESS);
    }
}
