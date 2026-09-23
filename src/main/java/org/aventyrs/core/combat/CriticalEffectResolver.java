package org.aventyrs.core.combat;

import org.aventyrs.core.effect.CriticalEffect;
import org.aventyrs.core.effect.CriticalEffectContext;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.CriticalEffects;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.TitleAttackModifiers;
import org.aventyrs.core.util.DiceRoller;

import java.util.ArrayList;
import java.util.List;

/**
 * The Efeitos Críticos an attack carries <b>by identity</b>, built — the half both {@link
 * AttackDelivery} and {@link AttackReceiver} share, beside the ready-made {@code CriticalEffect}s a
 * caller or a Talento hands them.
 *
 * <p>Where they come from, in order:
 * <ol>
 *   <li>the attack source's own — a weapon's {@code Weapon#getCriticalEffect()} column ("Sangramento
 *   (17)") or a Magia's {@code Spell#getCriticalEffectType()} — unless a held Título replaces it
 *   ({@code TitleAttackModifiers#criticalEffectOverride}: Impacto Elemental's Cataclismo), repeated
 *   once more per {@code AventyrTitle#resolveExtraNaturalCriticalEffectApplications} (Finalização);</li>
 *   <li>each held Título's additions ({@code AventyrTitle#resolveAdditionalCriticalEffects}:
 *   Punho Inigualável's Guilhotina);</li>
 *   <li>what the request names ({@code EmpoweredAttack#additionalCriticalEffect}, folded in by the
 *   caller).</li>
 * </ol>
 * On a hit that is <em>not</em> critical, only Finalização's repetitions fire — "se este ataque não
 * for um Acerto Crítico este ataque aplica o Efeito Crítico Menor de sua Arma Natural" — at Menor.
 *
 * <p>An identity {@link CriticalEffects#create} cannot build (no mechanism, or no dice supplied) is
 * reported in {@link Resolved#unapplied()} rather than dropped silently.
 */
final class CriticalEffectResolver {

    /** What was built, and what could not be. */
    record Resolved(List<CriticalEffect> effects, List<CriticalEffectType> unapplied) {

        static final Resolved NONE = new Resolved(List.of(), List.of());
    }

    private CriticalEffectResolver() {
    }

    /**
     * The typed Efeitos Críticos of one landed attack.
     *
     * @param critical the attack's critical — an Acerto on {@link AttackDelivery}'s path, the
     *                 defender's Falha on {@link AttackReceiver}'s — or {@code null}/{@code NONE}
     * @param hit      whether the attack landed at all
     */
    static Resolved resolve(final CombatantSheet attacker, final AttackSource attackSource, final SkillType attackSkill,
                            final CriticalResult critical, final boolean hit,
                            final List<CriticalEffectType> requested, final DiceRoller dice) {
        return resolve(attacker, attackSource, attackSkill, critical, hit, requested, dice, true);
    }

    /**
     * {@link #resolve(CombatantSheet, AttackSource, SkillType, CriticalResult, boolean, List, DiceRoller)}
     * with build {@code false}: only {@link Resolved#unapplied()} is filled, and nothing is constructed —
     * so no die an effect throws at construction is consumed by a mere report.
     */
    static Resolved resolve(final CombatantSheet attacker, final AttackSource attackSource, final SkillType attackSkill,
                            final CriticalResult critical, final boolean hit,
                            final List<CriticalEffectType> requested, final DiceRoller dice, final boolean build) {
        if (!hit) {
            return Resolved.NONE;
        }
        boolean isCritical = critical != null && critical != CriticalResult.NONE;
        CriticalEffectType natural = naturalEffectOf(attacker, attackSource);
        int extra = attacker == null ? 0 : attacker.getCharacter().getAllTitles().stream()
                .mapToInt(title -> title.resolveExtraNaturalCriticalEffectApplications(attacker, attackSource))
                .sum();

        List<CriticalEffectType> types = new ArrayList<>();
        CriticalResult severity;
        if (isCritical) {
            severity = critical;
            if (natural != null) {
                for (int i = 0; i <= extra; i++) {
                    types.add(natural);
                }
            }
            if (attacker != null) {
                for (AventyrTitle title : attacker.getCharacter().getAllTitles()) {
                    types.addAll(title.resolveAdditionalCriticalEffects(attackSkill, attackSource, attacker));
                }
            }
            if (requested != null) {
                types.addAll(requested);
            }
        } else {
            if (natural == null || extra == 0) {
                return Resolved.NONE;
            }
            severity = CriticalResult.ACERTO_CRITICO_MENOR;
            for (int i = 0; i < extra; i++) {
                types.add(natural);
            }
        }

        CriticalEffectContext context = CriticalEffectContext.of(attacker, attackSource, severity, dice);
        List<CriticalEffect> built = new ArrayList<>();
        List<CriticalEffectType> unapplied = new ArrayList<>();
        for (CriticalEffectType type : types) {
            if (!CriticalEffects.canCreate(type, context)) {
                unapplied.add(type);
            } else if (build) {
                CriticalEffects.create(type, context).ifPresent(built::add);
            }
        }
        return new Resolved(built, unapplied);
    }

    /** The source's own Efeito Crítico, or the one a held Título replaces it with. */
    private static CriticalEffectType naturalEffectOf(final CombatantSheet attacker, final AttackSource attackSource) {
        if (attacker != null) {
            CriticalEffectType override = TitleAttackModifiers.resolve(attacker, attackSource, null, null)
                    .criticalEffectOverride();
            if (override != null) {
                return override;
            }
        }
        if (attackSource instanceof Weapon weapon) {
            return weapon.getCriticalEffect();
        }
        if (attackSource instanceof Spell spell) {
            return spell.getCriticalEffectType();
        }
        return null;
    }
}
