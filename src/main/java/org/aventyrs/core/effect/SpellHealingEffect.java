package org.aventyrs.core.effect;

import lombok.Getter;
import org.aventyrs.core.magic.HealingCondition;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellHealing;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * The applicable form of a Magia's {@link SpellHealing} — one class for a whole healing
 * ramificação, because the Magias of a branch share an effect that only deepens as the tree
 * climbs. Vida's principal branch is the whole of its catalog today, and every rung is a
 * different {@link SpellHealing} rather than a different class:
 *
 * <pre>
 * Semente      Aliviar a Dor        Descanso Mínimo, only if not bleeding
 * Broto        Revigorar            Descanso Longo
 * Muda         Revigorar Maior      Descanso Total
 * Emergente    Nova Rejuvenescedora Descanso Longo, halved for hostiles
 * Florescente  Benção da Luz        every lost PV
 * </pre>
 *
 * <p><b>It heals as much as a Descanso would, but is not one.</b> The amount comes from {@link
 * RestService#getRecoveredHitPoints}, read against the <b>target's</b> own Vigor — "faça com que
 * <i>um alvo</i> recupere PV como se <i>passasse</i> por um Descanso" names the recipient — and is
 * then applied with a bare {@link CombatantSheet#heal}. {@link RestService#applyRest} is
 * deliberately never called: the rules text says "Este é um efeito similar a Descanso e não
 * substitui Descansos reais", and a real Rest also restores PM and PD, settles every {@code
 * PendingEgoRecovery} and clears rest cooldowns.
 *
 * <p>Three behaviours come free from going through {@code heal} rather than touching the pool
 * directly: Feridas Dolorosas refuses the recovery outright, any ongoing {@code Bleeding} is
 * interrupted, and — since the heal is sourced as this Magia ({@link HealingSource#spell}) — a
 * target in Coma recovers at most 1PV from it and a dead one nothing, unless the caster's Títulos
 * say otherwise. That last needs the {@link #caster}; an effect built without one heals the
 * fallen as if no Título could help.
 *
 * <p><b>Whether the target is hostile is the caller's to say.</b> Nova Rejuvenescedora's "Inimigos
 * do conjurador recuperam apenas metade desta quantidade de PV" needs an Área de Efeito footprint
 * resolved into a set of combatants, which this core does not do — so the caller picks the targets
 * and constructs one effect per target, saying which are hostile. The same division {@code
 * DamageInteraction#halvingDamage()} makes for a multi-target attack.
 */
@Getter
public class SpellHealingEffect extends AbstractEffect implements HealingEffect {

    private final Spell spell;
    private final SpellHealing healing;
    private final boolean hostileTarget;
    private final RestService restService;
    private final CombatantSheet caster;

    public SpellHealingEffect(final Spell spell, final SpellHealing healing) {
        this(spell, healing, false, new RestServiceImpl());
    }

    public SpellHealingEffect(final Spell spell, final SpellHealing healing,
                              final boolean hostileTarget) {
        this(spell, healing, hostileTarget, new RestServiceImpl());
    }

    public SpellHealingEffect(final Spell spell, final SpellHealing healing,
                              final boolean hostileTarget, final RestService restService) {
        this(spell, healing, hostileTarget, restService, null);
    }

    /**
     * @param caster who cast the Magia, or {@code null} — whose Títulos may lift the limits on
     *               healing a target in Coma or dead
     */
    public SpellHealingEffect(final Spell spell, final SpellHealing healing,
                              final boolean hostileTarget, final RestService restService,
                              final CombatantSheet caster) {
        this.spell = spell;
        this.healing = healing;
        this.hostileTarget = hostileTarget;
        this.restService = restService;
        this.caster = caster;
    }

    @Override
    public String getDescription() {
        return spell.getPrimaryEffectDescription();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (healing.condition() == HealingCondition.ONLY_IF_NOT_BLEEDING && target.stopBleeding()) {
            // "ao invés disso" — a bleeding target gets the bleeding stopped and no PV back.
            return reportChain(InteractionResult.builder()
                    .resultStatus(resolveStatus(target)))
                    .build();
        }

        int damageBefore = target.getDamageTaken();
        target.heal(resolveAmount(target, damageBefore), HealingSource.spell(spell, caster));
        int recovered = damageBefore - target.getDamageTaken();

        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target))
                .resourceGainValue(recovered)
                .resourceGainType(ResourceType.HIT_POINTS))
                .build();
    }

    /**
     * What this Magia offers this target — every lost PV for a full recovery, otherwise the
     * Descanso-equivalent figure, halved when a hostile target is owed only half.
     *
     * <p>Reported through {@code resourceGainValue} is what was <i>actually</i> recovered rather
     * than what was offered, since a target already at full PV or under Feridas Dolorosas takes
     * none of it — the same distinction {@code org.aventyrs.core.sheet.Regeneration} draws when
     * spending its own budget.
     */
    private int resolveAmount(final CombatantSheet target, final int damageTaken) {
        if (healing.fullRecovery()) {
            return damageTaken;
        }
        int offered = restService.getRecoveredHitPoints(target.getCharacter(),
                healing.restEquivalent());
        return hostileTarget && healing.halvedForHostiles() ? offered / 2 : offered;
    }
}
