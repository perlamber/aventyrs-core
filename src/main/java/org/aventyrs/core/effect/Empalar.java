package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Impalement;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Empalar — "A arma fica presa no alvo". Registers a {@code sheet.Impalement} on the target naming
 * the weapon and its owner: removing it costs 3d6 (Maior) / 2d6 (Menor) of irreducible damage, and
 * "1 ação livre, ou 2PA do alvo" (Maior) / "1PA (do usuário ou do alvo)" (Menor) — see {@code
 * CombatantSheet#removeImpalement}. One weapon stuck at a time; a second replaces the first.
 *
 * <p>TODO: nothing stops the owner attacking with the stuck weapon meanwhile — {@code
 * CombatantSheet#canAttackWith} reads no Impalement, and an Arma Natural (Chifres Poderosos) cannot
 * be sheathed to mark it out of hand.
 */
public class Empalar extends AbstractCriticalEffect {

    static final int MAJOR_REMOVAL_DICE = 3;
    static final int MINOR_REMOVAL_DICE = 2;


    public Empalar(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.EMPALAR;
    }

    @Override
    protected String majorDescription() {
        return "A arma fica presa no alvo, remover a arma requer 1 ação livre, ou 2PA do alvo, remover a arma causa 3d6 pontos de dano (este dano não pode ser reduzido por efeitos de redução).";
    }

    @Override
    protected String minorDescription() {
        return "A arma fica presa no alvo, remover a arma requer 1PA (do usuário ou do alvo), remover a arma causa 2d6 pontos de dano (este dano não pode ser reduzido por efeitos de redução).";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        target.getImpalement().ifPresent(target::removeEffect);
        target.applyEffect(new Impalement(getContext().attackSource(),
                attacker() == null ? null : attacker().getId(),
                pick(MAJOR_REMOVAL_DICE, MINOR_REMOVAL_DICE),
                getContext().isMajor() ? ActionCost.FREE_ACTION : ActionCost.ofActionPoints(1),
                ActionCost.ofActionPoints(pick(2, 1))));
        return InteractionResult.builder();
    }
}
