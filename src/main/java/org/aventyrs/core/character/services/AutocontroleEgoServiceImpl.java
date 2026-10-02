package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.ego.AutocontroleDefence;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CenaImmunity;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.sheet.TimedElementalResistance;
import org.aventyrs.core.skill.SkillRoll;

import static org.aventyrs.core.util.TranslatableMessages.NOT_A_RUNNING_EFFECT;

public class AutocontroleEgoServiceImpl implements AutocontroleEgoService {

    /** The source every Autocontrole protection is granted under, so a second one renews the first. */
    static final String PROTECTION_SOURCE = "AUTOCONTROLE";

    private final EgoPointsService egoPointsService;
    private final RestService restService;

    public AutocontroleEgoServiceImpl() {
        this(new EgoPointsServiceImpl(), new RestServiceImpl());
    }

    public AutocontroleEgoServiceImpl(final EgoPointsService egoPointsService, final RestService restService) {
        this.egoPointsService = egoPointsService;
        this.restService = restService;
    }

    @Override
    public void halveMaleficio(@NonNull final CombatantSheet sheet, @NonNull final TemporaryEffect maleficio) {
        requireRunning(sheet, maleficio);
        pay(sheet, EgoPointType.TEMPORARY);
        sheet.halveEffectDuration(maleficio);
    }

    @Override
    public void grantProtection(@NonNull final CombatantSheet sheet, @NonNull final Protection protection,
                                final ElementalType element) {
        if (protection == Protection.RE && element == null) {
            throw new IllegalArgumentException("RE names an element");
        }
        pay(sheet, EgoPointType.TEMPORARY);
        sheet.applyEffect(TemporaryBonus.untilTurnStarts(ModifierType.ABSOLUTE_DAMAGE_REDUCTION,
                DamageService.DEFAULT_DAMAGE_REDUCTION, PROTECTION_RODADAS, PROTECTION_SOURCE));
        switch (protection) {
            case RD -> sheet.applyEffect(TemporaryBonus.untilTurnStarts(ModifierType.DAMAGE_REDUCTION,
                    DamageService.DEFAULT_DAMAGE_REDUCTION, PROTECTION_RODADAS, PROTECTION_SOURCE));
            case RM -> sheet.applyEffect(TemporaryBonus.untilTurnStarts(ModifierType.MAGIC_REDUCTION,
                    DamageService.DEFAULT_DAMAGE_REDUCTION, PROTECTION_RODADAS, PROTECTION_SOURCE));
            case RE -> sheet.applyEffect(new TimedElementalResistance(element, 1, PROTECTION_RODADAS, true));
        }
    }

    @Override
    public int recoverAsLongRest(@NonNull final CombatantSheet sheet) {
        pay(sheet, EgoPointType.TEMPORARY);
        return recover(sheet, RestType.LONGO, restService.getRecoveredHitPoints(sheet.getCharacter(), RestType.LONGO));
    }

    @Override
    public SkillRoll applyDefence(@NonNull final CombatantSheet sheet, @NonNull final SkillRoll defenceRoll,
                                  @NonNull final AutocontroleDefence effect) {
        SkillRoll marked = defenceRoll.withAutocontrole(effect);
        pay(sheet, effect.getPointType());
        return marked;
    }

    @Override
    public void removeMaleficio(@NonNull final CombatantSheet sheet, @NonNull final TemporaryEffect maleficio) {
        requireRunning(sheet, maleficio);
        pay(sheet, EgoPointType.PERMANENT);
        sheet.removeEffect(maleficio);
        sheet.grantCenaImmunity(CenaImmunity.kindOf(maleficio));
    }

    @Override
    public int negateDamageThisRound(@NonNull final CombatantSheet sheet) {
        pay(sheet, EgoPointType.PERMANENT);
        return sheet.negateDamageThisRound();
    }

    @Override
    public int recoverAsTotalRest(@NonNull final CombatantSheet sheet) {
        pay(sheet, EgoPointType.PERMANENT);
        return recover(sheet, RestType.TOTAL, sheet.getDamageTaken());
    }

    /** PM and PD as restType gives, and hitPoints PV — healed as an Ego spend, so the fallen limits apply. */
    private int recover(final CombatantSheet sheet, final RestType restType, final int hitPoints) {
        sheet.recoverMagicPoints(restService.getRecoveredMagicPoints(sheet.getCharacter(), restType));
        sheet.recoverDeterminationPoints(restService.getRecoveredDeterminationPoints(sheet.getCharacter(), restType));
        int before = sheet.getDamageTaken();
        sheet.heal(hitPoints, HealingSource.egoSpend(sheet.getCharacter().getEgoAdvantage(EgoDomain.AUTOCONTROLE), sheet));
        return before - sheet.getDamageTaken();
    }

    private static void requireRunning(final CombatantSheet sheet, final TemporaryEffect effect) {
        if (!sheet.getRunningEffects().contains(effect)) {
            throw new IllegalOperationException(NOT_A_RUNNING_EFFECT);
        }
    }

    /** One Autocontrole point of type — see {@code EgoPointsService#payForEffect} (a PdN spends nothing). */
    private void pay(final CombatantSheet sheet, final EgoPointType type) {
        egoPointsService.payForEffect(sheet, EgoDomain.AUTOCONTROLE, type);
    }
}
