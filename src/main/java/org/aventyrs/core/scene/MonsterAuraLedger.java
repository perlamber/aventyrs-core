package org.aventyrs.core.scene;

import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionWard;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.util.DiceRoller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * What every holder's {@link MonsterAura}s are granting right now, and how long each has stood empty (core 0.1.7.4).
 * {@link Scene} owns one and delegates {@code refreshMonsterAuras}/{@code pulseMonsterAuras} to it.
 *
 * <p>The grant half is {@link Scene#refreshProjectedAuras}'s revoke-then-grant, keyed by holder id, aimed at the
 * aura's {@link AuraReach} rather than allies only, and with two additions the 20261015 auras need: a figure that
 * keeps biting for {@link MonsterAura#exitLingerRounds} after its recipient leaves, and the holder's own bonus while
 * an enemy is inside ({@link MonsterAura#holderActionPointsWhileOccupied}).
 */
final class MonsterAuraLedger {

    private final Map<UUID, List<Grant>> grantsByHolder = new HashMap<>();
    private final Map<String, Integer> emptyPulses = new HashMap<>();

    /** One live grant, kept so it can be taken back by reference. */
    private record Grant(CombatantSheet recipient, MonsterAura aura, TemporaryEffect effect, AuraBonus bonus) {
    }

    /** Who aura covers right now, from holder's own distances. The holder itself only for a self-and-allies aura. */
    static List<CombatantSheet> covered(final CombatantSheet holder, final MonsterAura aura,
                                        final SceneContext holderContext) {
        List<CombatantSheet> candidates = new ArrayList<>();
        candidates.add(holder);
        candidates.addAll(holderContext.getAllies());
        candidates.addAll(holderContext.getEnemies());
        candidates.addAll(holderContext.getNeutrals());
        Set<CombatantSheet> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<CombatantSheet> covered = new ArrayList<>();
        for (CombatantSheet candidate : candidates) {
            if (!seen.add(candidate) || !aura.reaches(holder, candidate, holderContext)) {
                continue;
            }
            if (candidate == holder) {
                covered.add(candidate);
                continue;
            }
            Range distance = holderContext.getDistanceTo(candidate);
            if (distance != null && distance.isWithin(aura.radius())) {
                covered.add(candidate);
            }
        }
        return covered;
    }

    List<CombatantSheet> refresh(final CombatantSheet holder, final SceneContext holderContext) {
        List<Grant> previous = grantsByHolder.remove(holder.getId());
        if (previous != null) {
            previous.forEach(grant -> grant.recipient().removeEffect(grant.effect()));
        }
        if (holderContext == null) {
            return List.of();
        }
        List<Grant> now = new ArrayList<>();
        List<CombatantSheet> everyone = new ArrayList<>();
        for (MonsterAura aura : holder.resolveMonsterAuras()) {
            List<CombatantSheet> covered = covered(holder, aura, holderContext);
            for (CombatantSheet recipient : covered) {
                for (AuraBonus bonus : aura.whileInside()) {
                    TemporaryBonus granted = TemporaryBonus.openEnded(bonus.type(), bonus.value(), aura.source());
                    recipient.applyEffect(granted);
                    now.add(new Grant(recipient, aura, granted, bonus));
                }
                if (!aura.wardedConditions().isEmpty()) {
                    ConditionWard ward = new ConditionWard(aura.wardedConditions(), aura.source());
                    recipient.applyEffect(ward);
                    now.add(new Grant(recipient, aura, ward, null));
                }
                everyone.add(recipient);
            }
            if (aura.holderActionPointsWhileOccupied() != 0
                    && covered.stream().anyMatch(holderContext.getEnemies()::contains)) {
                TemporaryBonus own = TemporaryBonus.openEnded(org.aventyrs.core.modifier.ModifierType.ACTION_POINTS,
                        aura.holderActionPointsWhileOccupied(), aura.source() + " (emissor)");
                holder.applyEffect(own);
                now.add(new Grant(holder, aura, own, null));
            }
            if (previous != null && aura.exitLingerRounds() > 0) {
                linger(holder, aura, previous, covered);
            }
        }
        grantsByHolder.put(holder.getId(), now);
        return everyone;
    }

    /** "recuperados 1 Rodada após os personagens deixarem a Área" — what someone who left keeps for a while. */
    private static void linger(final CombatantSheet holder, final MonsterAura aura, final List<Grant> previous,
                               final List<CombatantSheet> stillCovered) {
        for (Grant grant : previous) {
            if (grant.bonus() == null || grant.recipient() == holder || !grant.aura().source().equals(aura.source())
                    || stillCovered.contains(grant.recipient())) {
                continue;
            }
            grant.recipient().applyEffect(new TemporaryBonus(grant.bonus().type(), grant.bonus().value(),
                    aura.exitLingerRounds(), aura.source() + " (residual)"));
        }
    }

    /** Takes back everything holder's auras grant — it left the Scene or stopped emitting. */
    void revokeFrom(final CombatantSheet holder) {
        List<Grant> granted = grantsByHolder.remove(holder.getId());
        if (granted != null) {
            granted.forEach(grant -> grant.recipient().removeEffect(grant.effect()));
        }
    }

    /** Takes back whatever anyone's aura grants recipient — it is leaving the Scene. */
    void revokeTo(final CombatantSheet recipient) {
        grantsByHolder.values().forEach(grants -> grants.removeIf(grant -> {
            if (!grant.recipient().getId().equals(recipient.getId())) {
                return false;
            }
            grant.recipient().removeEffect(grant.effect());
            return true;
        }));
    }

    /**
     * Each of holder's auras' pulse for round — see {@link Scene#pulseMonsterAuras}. Also ends an aura that has
     * stood empty of other characters for {@link MonsterAura#emptyRoundsToEnd} pulses.
     */
    List<AuraPulseResult> pulse(final CombatantSheet holder, final SceneContext holderContext, final int round,
                                final DiceRoller roller, final ToIntFunction<CombatantSheet> distanceUd) {
        if (holderContext == null) {
            return List.of();
        }
        List<AuraPulseResult> results = new ArrayList<>();
        for (MonsterAura aura : holder.resolveMonsterAuras()) {
            if (aura.emptyRoundsToEnd() > 0 && endsForStandingEmpty(holder, aura, holderContext)) {
                continue;
            }
            AuraPulse pulse = aura.pulse();
            if (pulse == null || !pulse.firesOn(round)) {
                continue;
            }
            int rolled = pulse.dice() > 0 && roller != null ? roller.rollD6(pulse.dice()) : 0;
            List<AuraHit> hits = new ArrayList<>();
            for (CombatantSheet target : covered(holder, aura, holderContext)) {
                int ud = target == holder ? 0 : distanceOf(target, holderContext, distanceUd);
                int heal = pulse.healAsRest() == null ? 0
                        : new RestServiceImpl().getRecoveredHitPoints(target.getCharacter(), pulse.healAsRest(), org.aventyrs.core.rest.RecoveryOrigin.EQUIVALENTE);
                hits.add(new AuraHit(target, target == holder ? 0 : pulse.damageAt(rolled, ud), pulse.descriptor(),
                        heal, target == holder ? List.of() : pulse.conditions(), pulse.conditionRounds(),
                        target == holder ? 0 : pulse.autocontroleLoss(),
                        target != holder && pulse.possessAtZeroAutocontrole(), target == holder ? 0 : pulse.pushUd()));
            }
            results.add(new AuraPulseResult(aura, rolled, hits, pulse.lifeSteal()));
        }
        return results;
    }

    private boolean endsForStandingEmpty(final CombatantSheet holder, final MonsterAura aura,
                                         final SceneContext holderContext) {
        String key = holder.getId() + "|" + aura.source();
        boolean anyoneElse = MonsterAuraLedger.covered(holder, MonsterAura.builder().source(aura.source())
                        .radius(aura.radius()).reach(AuraReach.ALL_OTHERS).build(), holderContext).stream()
                .anyMatch(sheet -> sheet != holder);
        if (anyoneElse) {
            emptyPulses.remove(key);
            return false;
        }
        int empty = emptyPulses.merge(key, 1, Integer::sum);
        if (empty < aura.emptyRoundsToEnd()) {
            return false;
        }
        emptyPulses.remove(key);
        holder.removeEffectsFrom(aura.source());
        revokeFrom(holder);
        return true;
    }

    private static int distanceOf(final CombatantSheet target, final SceneContext holderContext,
                                  final ToIntFunction<CombatantSheet> distanceUd) {
        if (distanceUd != null) {
            return distanceUd.applyAsInt(target);
        }
        Range band = holderContext.getDistanceTo(target);
        return band == null || band.getMaxUnidadesDeDistancia() == null ? 0 : band.getMaxUnidadesDeDistancia();
    }
}
