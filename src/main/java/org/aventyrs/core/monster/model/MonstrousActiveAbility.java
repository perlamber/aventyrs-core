package org.aventyrs.core.monster.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.List;
import java.util.function.Supplier;

/**
 * An Efeito Ativo of a {@link MonstrousAbility}, as plain data — the price, the Duração, the
 * Resfriamento and the {@link TemporaryEffect}s it applies. Activated through {@code
 * ActiveAbilityService#activate} like any other {@link ActiveAbility}, which pays the PD, checks
 * the Resfriamento and applies the effects.
 *
 * <p>Built fresh per spawn by {@link MonstrousAbility#resolveActiveAbilities}, already resolved for
 * the holder's Categoria (Impulso at Apex simply <i>is</i> a +6), so the instance never has to know
 * which Aprimoramentos apply.
 *
 * <p><b>Effects are fixed values, not formulas of the holder</b> — every monster active built so
 * far grants a stated number. One that scales with an Atributo would override {@link
 * #resolveEffects} instead. They are held as {@link Supplier}s because a {@link TemporaryEffect}
 * is mutable (it ticks down on the sheet): each activation must apply a fresh one, or a second
 * activation would share the first's countdown.
 *
 * <p>An active whose whole effect this core cannot apply (Sonido's "não permite Reações", a
 * Corrente de Efeitos) still exists — it has a price and a Resfriamento, which a table needs
 * tracked — and simply carries no effects.
 */
@Getter
@Builder
public class MonstrousActiveAbility implements ActiveAbility {

    /**
     * The Resfriamento that stands for "Apenas uma vez por Cena" — long enough never to lapse on
     * its own; {@code MonsterSheet#beginScene} is what clears it.
     */
    public static final int ONCE_PER_SCENE = Integer.MAX_VALUE;

    /** Its name as the rules print it, e.g. "Celeridade Mór". */
    @NonNull
    private final String name;

    @NonNull
    private final String description;

    @NonNull
    @Builder.Default
    private final ActionCost actionPointCost = ActionCost.ofActionPoints(1);

    private final int determinationPointCost;

    private final int durationInRounds;

    private final int cooldownRounds;

    @Singular
    private final List<Supplier<TemporaryEffect>> effects;

    /**
     * The dice this active rolls ("Recupera 3d6PV"), or {@code null}. Declared, never rolled here:
     * the caller rolls and activates through {@code ActiveAbilityService#activate(…, List faces)}.
     */
    private final org.aventyrs.core.character.Dice dice;

    /** A clause that must hold to activate ("Apenas enquanto voando"), or {@code null} for none. */
    private final java.util.function.Predicate<org.aventyrs.core.sheet.CombatantSheet> usableWhen;

    /** What activating does beyond its effects — landing, lifting Condições — or {@code null}. */
    private final java.util.function.Consumer<org.aventyrs.core.sheet.CombatantSheet> onActivated;

    /** What the rolled total does to the activator — a heal, typically. Ignored without {@link #dice}. */
    private final java.util.function.ObjIntConsumer<org.aventyrs.core.sheet.CombatantSheet> onRolled;

    @Override
    public int getMagicPointCost() {
        return 0;
    }

    /** Whether it is limited to once per Cena, reset by {@code MonsterSheet#beginScene}. */
    public boolean isOncePerScene() {
        return cooldownRounds == ONCE_PER_SCENE;
    }

    /**
     * Unused: {@link #resolveEffects} is overridden and is what {@code ActiveAbilityService}
     * applies. Returns the first effect, or {@code null} for an active that carries none.
     */
    @Override
    public TemporaryEffect resolveEffect(final Character character) {
        return effects.isEmpty() ? null : effects.get(0).get();
    }

    @Override
    public boolean isUsableBy(final org.aventyrs.core.sheet.CombatantSheet activator) {
        return usableWhen == null || usableWhen.test(activator);
    }

    @Override
    public void applyOnActivation(final org.aventyrs.core.sheet.CombatantSheet activator) {
        if (onActivated != null) {
            onActivated.accept(activator);
        }
    }

    @Override
    public org.aventyrs.core.character.Dice getDice() {
        return dice;
    }

    @Override
    public void applyRolled(final org.aventyrs.core.sheet.CombatantSheet activator, final int rolledTotal) {
        if (onRolled != null) {
            onRolled.accept(activator, rolledTotal);
        }
    }

    @Override
    public List<TemporaryEffect> resolveEffects(final Character character) {
        return effects.stream().map(Supplier::get).toList();
    }
}
