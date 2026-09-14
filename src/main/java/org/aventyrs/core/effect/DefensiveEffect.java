package org.aventyrs.core.effect;

/**
 * A {@link SpellEffect} that protects its target rather than harming one — a buff, a Bênção, or
 * the lifting of a Malefício.
 *
 * <p>{@link ConditionCleansingEffect} is the concrete form, and it fills Vida's alternativo
 * branch: removing the {@code ConditionType}s a Magia names, deepening rung by rung from Doença
 * and Veneno at Broto to every Malefício at Florescente.
 *
 * <p><b>A duration-bearing buff belongs on a different rail.</b> "Recebe +N por N Rodadas" is a
 * {@code org.aventyrs.core.sheet.Blessing}, granted through {@code CombatantSheet#grantBlessing}
 * and reported on {@code InteractionResult#getBlessings()} — see the {@code granting-a-blessing}
 * skill. A Defensive Effect is the momentary act; the Blessing is what it leaves behind.
 *
 * <p>TODO the outright <i>immunity</i> half of a protective clause has no mechanism — {@code
 * VidaSpell#CORPO_FECHADO}'s "se torna imune à Malefícios enquanto estiver sob efeito" needs the
 * per-condition immunity CLAUDE.md's Malefício-classification gap names as missing. Removal is
 * real; refusing a future Malefício is not.
 */
public interface DefensiveEffect extends SpellEffect {
}
