package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Asphyxia;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.util.DiceRoller;

public class AsphyxiaServiceImpl implements AsphyxiaService {

    /** "até o próximo efeito de Descanso" — any tier. */
    static final RestType PAINFUL_WOUNDS_UNTIL = RestType.MINIMO;
    /** Dealt as curse damage, the depth {@code RealExecution} uses for "morte instantânea". */
    private static final int LETHAL_DAMAGE_MULTIPLIER = 2;

    private final DamageService damageService;
    private final HitPointsService hitPointsService;

    public AsphyxiaServiceImpl() {
        this(new DamageServiceImpl(), new HitPointsServiceImpl());
    }

    public AsphyxiaServiceImpl(final DamageService damageService, final HitPointsService hitPointsService) {
        this.damageService = damageService;
        this.hitPointsService = hitPointsService;
    }

    @Override
    public void stopBreathing(@NonNull final CombatantSheet sheet) {
        // "Os Mortos não precisam … respirar" — the Anatomia de Morto-Vivo (core 0.1.6.23).
        if (sheet.hasUndeadAnatomy()) {
            return;
        }
        if (sheet.getAsphyxia().isEmpty()) {
            sheet.setAsphyxia(Asphyxia.STARTED);
        }
    }

    @Override
    public void breathe(@NonNull final CombatantSheet sheet) {
        sheet.setAsphyxia(null);
    }

    @Override
    public boolean isBreathless(@NonNull final CombatantSheet sheet) {
        return sheet.getAsphyxia().isPresent();
    }

    @Override
    public int getRoundsPerInstance(@NonNull final CombatantSheet sheet) {
        return Math.max(1, sheet.getAttributeTotal(AttributeDomain.VIGOR));
    }

    @Override
    public AsphyxiaTick tick(@NonNull final CombatantSheet sheet, final DiceRoller dice,
                             final SceneContext sceneContext) {
        Asphyxia held = sheet.getAsphyxia().orElse(null);
        if (held == null) {
            return AsphyxiaTick.NONE;
        }
        int rounds = held.breathlessRounds() + 1;
        if (rounds % getRoundsPerInstance(sheet) != 0) {
            sheet.setAsphyxia(new Asphyxia(rounds, held.instancesSuffered()));
            return new AsphyxiaTick(rounds, 0, 0, 0, false, false);
        }
        int instance = held.instancesSuffered() + 1;
        if (instance >= LETHAL_INSTANCE) {
            sheet.applyCurseDamage(LETHAL_DAMAGE_MULTIPLIER
                    * hitPointsService.getMaxHitPoints(sheet.getCharacter(), sheet));
            sheet.setAsphyxia(new Asphyxia(rounds, instance));
            return new AsphyxiaTick(rounds, instance, 0, 0, false, true);
        }
        if (dice == null) {
            throw new IllegalArgumentException("Asphyxia instance " + instance + " needs dice");
        }
        int count = Math.min(instance, MAXIMUM_DICE);
        int damage = dice.rollD6(count);
        damageService.applyDamage(sheet, sceneContext, DamageType.ASFIXIA, null, damage, false);
        boolean painful = instance == PAINFUL_WOUNDS_INSTANCE;
        if (painful) {
            sheet.removeCondition(ConditionType.FERIDAS_DOLOROSAS);
            sheet.applyEffectUntilRest(new Condition(ConditionType.FERIDAS_DOLOROSAS, null), PAINFUL_WOUNDS_UNTIL);
        }
        sheet.setAsphyxia(new Asphyxia(rounds, instance));
        return new AsphyxiaTick(rounds, instance, count, damage, painful, false);
    }
}
