package org.aventyrs.core.effect;

import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DamageReceipt;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * The Damage stage of the Skill -&gt; Damage -&gt; EffectChain -&gt; CriticalEffect
 * pipeline (see {@code org.aventyrs.core.effect} package-info) — computes and applies
 * mitigated damage via {@link DamageService}, then reports the outcome.
 *
 * <p>Mirrors {@code AbstractSkillInteraction}'s cascading-overload shape: the 1-arg
 * {@link #applyTo(CombatantSheet)} required by {@link Interaction} (and the only one
 * {@link CombatantSheet#receiveInteraction} can call) delegates down with defaults — no
 * damage, no {@code SceneContext}, no {@code DamageType}/source, no next stage — so it's a
 * safe no-op when invoked generically. A caller that actually has damage to deal, optionally a
 * {@code SceneContext} (e.g. for {@code InitiativeAdvantage#TORRE_EM_MOVIMENTO}'s
 * Scene-conditioned RA/half-damage — {@code null} the same as every other
 * not-currently-in-a-Scene case elsewhere), optionally this hit's {@link DamageType} and its
 * source (the attacker's own {@link CombatantSheet} — e.g. for {@code
 * VigorAbility#RIGIDEZ_DA_MONTANHA}'s Dano-Físico-only, attacker-size-conditioned RD), and
 * optionally a next stage to hand off to once it exists, calls the 7-arg
 * {@link #applyTo(CombatantSheet, SceneContext, DamageType, CombatantSheet, int, boolean,
 * Interaction)} directly.
 */
public class DamageInteraction implements Interaction<CombatantSheet> {

    private final DamageService damageService;
    private final HitPointsService hitPointsService;
    private Interaction<CombatantSheet> nextInteraction;
    private boolean halfDamage;

    /** Whether this hit's PV loss lands at the next Rodada boundary — see {@link #postponing()}. */
    private boolean postponed;

    public DamageInteraction() {
        this(new DamageServiceImpl());
    }

    public DamageInteraction(final DamageService damageService) {
        this(damageService, new HitPointsServiceImpl());
    }

    public DamageInteraction(final DamageService damageService, final HitPointsService hitPointsService) {
        this.damageService = damageService;
        this.hitPointsService = hitPointsService;
    }

    /**
     * Links nextInteraction as this stage's pre-wired successor — see {@link
     * Interaction#getNextInteraction()}. It's reported only when this hit actually dealt damage,
     * exactly like the per-call {@code nextInteraction} parameter the longest {@code applyTo}
     * overload takes; the two are the same gate, differing only in <i>when</i> the successor is
     * supplied. A stored successor is what lets a whole chain be assembled up front and handed
     * over as one object (see {@code org.aventyrs.core.combat.AttackReceiver}); the parameter
     * form stays for a caller wiring one call at a time. When both are given, the explicit
     * parameter wins.
     */
    public DamageInteraction chainInto(final Interaction<CombatantSheet> nextInteraction) {
        this.nextInteraction = nextInteraction;
        return this;
    }

    /**
     * Marks this stage as dealing Meio-Dano because of <b>the attack</b>, not because of anything
     * the target carries — {@code ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA}'s "os danos
     * no alvo adicional são reduzidos à metade", which {@code
     * org.aventyrs.core.combat.AttackDelivery} sets on the chain head it builds for each
     * <em>additional</em> target and never on the primary one's.
     *
     * <p>A fluent setter rather than a constructor argument or another {@code applyTo} parameter,
     * for the same reason {@link #chainInto} is one: a whole chain is assembled up front and
     * handed over as one object, so anything the head needs to know has to be attachable to it.
     * It is OR'd with the target's own half-damage sources inside {@link DamageService}, so a
     * blow can never be quartered.
     */
    public DamageInteraction halvingDamage() {
        this.halfDamage = true;
        return this;
    }

    /**
     * Marks this hit's Pontos de Vida as lost at the start of the target's <b>next Rodada</b>
     * rather than now — {@code VidaSpell#ALIVIAR_A_DOR}'s Efeito Alternativo <i>Procrastinar
     * Ferimento</i>. A fluent setter for the same reason {@link #halvingDamage} is one: the chain
     * is assembled up front, so anything its head needs to know must be attachable to it.
     *
     * <p><b>Deferred, not cancelled, and this distinction is load-bearing.</b> Everything except
     * the PV loss happens on schedule: the mitigated figure is still reported as {@code
     * resourceLossValue}, the damage-taken trigger still fires, and the chain is still forwarded.
     * Zeroing the figure instead would trip this stage's own {@code finalDamage > 0} gate and
     * silently swallow every Corrente and Efeito Crítico the hit triggered — not just the
     * Sangramento that <i>Estancar</i> is meant to stop.
     *
     * <p>The damage is handed to {@link CombatantSheet#schedulePostponedDamage} already mitigated,
     * so the Rodada boundary applies it raw and the target's RD is not charged twice.
     */
    public DamageInteraction postponing() {
        this.postponed = true;
        return this;
    }

    @Override
    public Interaction<CombatantSheet> getNextInteraction() {
        return nextInteraction;
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        return applyTo(target, null, 0, false, null);
    }

    public InteractionResult applyTo(final CombatantSheet target, final int rawDamage, final boolean ignoreDamageReduction) {
        return applyTo(target, null, rawDamage, ignoreDamageReduction, null);
    }

    /** Same as {@link #applyTo(CombatantSheet, int, boolean)}, but also given sceneContext — see this class's own javadoc. */
    public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext, final int rawDamage, final boolean ignoreDamageReduction) {
        return applyTo(target, sceneContext, rawDamage, ignoreDamageReduction, null);
    }

    public InteractionResult applyTo(final CombatantSheet target, final int rawDamage, final boolean ignoreDamageReduction,
                                      final Interaction<CombatantSheet> nextInteraction) {
        return applyTo(target, null, rawDamage, ignoreDamageReduction, nextInteraction);
    }

    public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext, final int rawDamage, final boolean ignoreDamageReduction,
                                      final Interaction<CombatantSheet> nextInteraction) {
        return applyTo(target, sceneContext, null, null, rawDamage, ignoreDamageReduction, nextInteraction);
    }

    /**
     * Same as {@link #applyTo(CombatantSheet, SceneContext, int, boolean, Interaction)}, but
     * also given this hit's damageType and source, with no next stage — see this class's own
     * javadoc.
     */
    public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext,
                                      final DamageType damageType, final CombatantSheet source,
                                      final int rawDamage, final boolean ignoreDamageReduction) {
        return applyTo(target, sceneContext, damageType, source, rawDamage, ignoreDamageReduction, null);
    }

    /**
     * Applies {@code rawDamage} (mitigated per {@link DamageService#calculateFinalDamage(CombatantSheet,
     * SceneContext, DamageType, CombatantSheet, int, boolean)}) to {@code
     * target}. {@code nextInteraction} is only carried onto the returned {@link
     * InteractionResult#getNextInteraction()} when this hit actually dealt damage (final damage
     * &gt; 0) — a hit fully absorbed by RD/RA has nothing for a downstream {@link EffectChain}/
     * {@link CriticalEffect} to react to, so the chain ends here instead of forwarding into a
     * stage with nothing to work from.
     *
     * <p>Mitigation and application happen in two separate steps here, rather than through
     * {@link DamageService#applyDamage}: this hit's own final damage is needed for {@code
     * resourceLossValue} and the next-stage gate, which that method's "total accumulated so
     * far" return can't supply.
     */
    public InteractionResult applyTo(final CombatantSheet target, final SceneContext sceneContext,
                                      final DamageType damageType, final CombatantSheet source,
                                      final int rawDamage, final boolean ignoreDamageReduction,
                                      final Interaction<CombatantSheet> nextInteraction) {
        int finalDamage = damageService.calculateFinalDamage(target, sceneContext, damageType, source, rawDamage, ignoreDamageReduction, halfDamage);
        // Procrastinar Ferimento: the PV are lost at the next Rodada boundary instead of now. The
        // figure below stays the honest one — everything but the loss itself happens on schedule.
        if (postponed) {
            target.schedulePostponedDamage(finalDamage, source);
        } else {
            target.applyDamage(finalDamage);
        }
        // Mitigation and application are split here rather than run through
        // DamageService#applyDamage (see this method's javadoc), so the victim's own damage-taken
        // reactions have to be fired explicitly — this is the attack path, and skipping it would
        // leave Regeneração Reativa triggering on every path but the real one.
        damageService.notifyDamageTaken(target, finalDamage, source, sceneContext);
        // What an Efeito Crítico further down this chain reads as "o dano deste ataque" —
        // Cataclismo's elemental half, Estilhaçador's half for the items, Oferenda Maldita's steal.
        target.recordDamageReceived(new DamageReceipt(finalDamage, damageType, source));

        InteractionResult.InteractionResultBuilder result = InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(target))
                .resourceLossValue(finalDamage)
                .resourceLossType(ResourceType.HIT_POINTS);
        if (finalDamage > 0) {
            result.nextInteraction(nextInteraction != null ? nextInteraction : this.nextInteraction);
        }
        return result.build();
    }
}
