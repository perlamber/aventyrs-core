package org.aventyrs.core.effect;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;

/**
 * Amenizar — "O alvo recebe RA para resistir a todos os ataques sofridos": one instance of
 * Resistência Absoluta ({@link #ABSOLUTE_REDUCTION}, "Cada instância reduz … em -2") as a
 * {@code ABSOLUTE_DAMAGE_REDUCTION} Blessing, read by {@code DamageServiceImpl} off the sheet — for
 * 2 Rodadas (Maior) or this one (Menor). A Magia's beneficial critical: the "target" is whoever the
 * Magia was cast on.
 */
public class Amenizar extends AbstractCriticalEffect {

    /** One instance of Resistência Absoluta. */
    static final int ABSOLUTE_REDUCTION = 2;


    public Amenizar(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.AMENIZAR;
    }

    @Override
    protected String majorDescription() {
        return "O alvo recebe RA para resistir a todos os ataques sofridos nas próximas 2 Rodadas.";
    }

    @Override
    protected String minorDescription() {
        return "O alvo recebe RA para resistir a todos os ataques sofridos nesta Rodada.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        target.grantBlessing(new Blessing(ModifierType.ABSOLUTE_DAMAGE_REDUCTION, ABSOLUTE_REDUCTION,
                pick(2, 1), TargetScope.SELF, getType().name()));
        return InteractionResult.builder();
    }
}
