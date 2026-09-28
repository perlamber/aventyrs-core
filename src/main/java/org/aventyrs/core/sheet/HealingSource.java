package org.aventyrs.core.sheet;

import lombok.NonNull;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

/**
 * Which heal effect a recovery comes from — what {@link CombatantSheet#heal(int, HealingSource)}
 * needs to apply the fallen-character limits:
 *
 * <ul>
 *   <li><b>Coma</b>: "personagens em Coma recuperam no máximo 1PV de cada efeito de cura
 *       diferente" — {@link #key()} is what makes two heals "the same effect", and a key already
 *       used during the current Coma recovers nothing more. A real Descanso is the one exception
 *       ({@link #repeatableInComa()}): 1PV each time, as often as the Narrador grants one.</li>
 *   <li><b>Dead</b>: no heal reaches a dead character unless an Ability says so — the healer's
 *       Títulos are asked ({@code AventyrTitle#claimRevival}), which is why {@link #healer()}
 *       and what the heal is ({@link #spell()}, {@link #titleAbility()}) travel with it.</li>
 * </ul>
 *
 * <p><b>Spells are keyed by name, not by the {@code Spell} object.</b> {@code
 * AlternateSpellVersion} is a plain class with no {@code equals}, so object identity would make
 * every cast of an Efeito Alternativo a brand-new effect. A Corrente de Efeitos shares its Magia's
 * key ({@link #spellChain}): Revigorar and its Sobrecura are one heal effect, not two.
 *
 * @param key              what makes two heals the same effect
 * @param repeatableInComa whether the key may heal again while its target is still in Coma
 * @param healer           who is healing, or {@code null} when nobody is (a Descanso, a Regeneração)
 * @param spell            the Magia this heal is, or {@code null}
 * @param titleAbility     the Habilidade de Título this heal is, or {@code null}
 * @param relayed          the healer's half already resolved elsewhere ({@link #relay}), for a heal
 *                         applied on a sheet whose client holds no real copy of the healer — or {@code
 *                         null}, the ordinary case, where {@link #healer()}'s Títulos are asked directly
 */
public record HealingSource(@NonNull Object key, boolean repeatableInComa, CombatantSheet healer,
                            Spell spell, AventyrTitleAbility titleAbility, RelayedHealer relayed) {

    /** The ordinary, unrelayed form. */
    public HealingSource(final Object key, final boolean repeatableInComa, final CombatantSheet healer,
                         final Spell spell, final AventyrTitleAbility titleAbility) {
        this(key, repeatableInComa, healer, spell, titleAbility, null);
    }

    /**
     * This heal with its healer's half resolved now, on the healer's real sheet, for a target whose
     * real sheet lives elsewhere: the Títulos' healing bonus, whether they would lift the Coma cap,
     * and — only when targetIsDead — a revival permission, <b>spent here</b> (a Curar os Mortos charge)
     * so the other side can honour it without holding the charge. The result names no healer.
     *
     * @param targetIsDead what the healer's side knows of the target — its broadcast status tier
     */
    public HealingSource relay(final boolean targetIsDead) {
        if (healer == null) {
            return new HealingSource(key, repeatableInComa, null, spell, titleAbility, RelayedHealer.NONE);
        }
        List<AventyrTitle> titles = healer.getCharacter().getAllTitles();
        int bonus = titles.stream().mapToInt(title -> title.resolveHealingBonus(this, null)).sum();
        boolean bypass = titles.stream().anyMatch(title -> title.grantsComaHealingBypass(this));
        Integer window = null;
        if (targetIsDead) {
            for (AventyrTitle title : titles) {
                OptionalInt claimed = title.claimRevivalWindow(this);
                if (claimed.isPresent()) {
                    window = claimed.getAsInt();
                    break;
                }
            }
        }
        return new HealingSource(key, repeatableInComa, null, spell, titleAbility,
                new RelayedHealer(bonus, bypass, window));
    }

    /** A Magia's own heal — keyed by the Magia's name. */
    public static HealingSource spell(@NonNull final Spell spell, final CombatantSheet caster) {
        return new HealingSource(spellKey(spell), false, caster, spell, null, null);
    }

    /**
     * A Corrente de Efeitos riding on parentSpell — the same key as the Magia's own heal, so the two
     * together are one effect. A {@code null} parentSpell (a Corrente built without its Magia)
     * falls back to keying by chain alone.
     */
    public static HealingSource spellChain(@NonNull final Object chain, final Spell parentSpell,
                                           final CombatantSheet caster) {
        return parentSpell == null
                ? new HealingSource(chain, false, caster, null, null)
                : new HealingSource(spellKey(parentSpell), false, caster, parentSpell, null);
    }

    /** A Habilidade de Título's heal — keyed by the ability constant. */
    public static HealingSource titleAbility(@NonNull final AventyrTitleAbility ability,
                                             final CombatantSheet activator) {
        return new HealingSource(ability, false, activator, null, ability);
    }

    /** A real Descanso — the one heal that stays repeatable in Coma, 1PV at a time. */
    public static HealingSource rest(@NonNull final RestType restType) {
        return new HealingSource(restType, true, null, null, null);
    }

    /**
     * The PV an Ego-point spend gives back. Keyed by the Vantagem that pays it, or by the spender's
     * own sheet class when the domain has none.
     */
    public static HealingSource egoSpend(final EgoAdvantage advantage, final CombatantSheet spender) {
        return new HealingSource(advantage == null ? EgoAdvantage.class : advantage, false, spender, null, null);
    }

    /** One {@link Regeneration} grant — keyed by the instance, so its whole budget is one effect. */
    public static HealingSource regeneration(@NonNull final Regeneration regeneration) {
        return new HealingSource(regeneration, false, null, null, null);
    }

    /** Whether this heal is a Magia whose tree carries any of types. */
    public boolean isSpellOfType(final MagicType... types) {
        return spell != null && Arrays.stream(types).anyMatch(type -> spell.getTree().hasMagicType(type));
    }

    private static String spellKey(final Spell spell) {
        return "spell:" + spell.getName();
    }
}
