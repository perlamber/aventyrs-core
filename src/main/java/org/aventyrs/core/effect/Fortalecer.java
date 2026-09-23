package org.aventyrs.core.effect;

import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;

/**
 * Fortalecer — "Item afetado concede RD e RM ao seu usuário por 2 Rodadas": one instance of each
 * (RD {@code DamageService#DEFAULT_DAMAGE_REDUCTION}, RM {@link #MAGIC_REDUCTION}, "Cada instância
 * reduz o Dano Mágico … em -2") as Blessings on the target, the item's user.
 *
 * <p>TODO the Maior's "Item afetado recupera todos os seus PV perdidos nesta Cena": an item keeps one
 * running damage total with no record of when it was taken, so "nesta Cena" cannot be told apart.
 */
public class Fortalecer extends AbstractCriticalEffect {

    /** One instance of Resistência à Magias. */
    static final int MAGIC_REDUCTION = 2;
    static final int ROUNDS = 2;


    public Fortalecer(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.FORTALECER;
    }

    @Override
    protected String majorDescription() {
        return "Item afetado recupera todos os seus PV perdidos nesta Cena, e concede RD e RM ao seu usuário por 2 Rodadas.";
    }

    @Override
    protected String minorDescription() {
        return "Item afetado concede RD e RM ao seu usuário por 2 Rodadas.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        String source = getType().name();
        target.grantBlessing(new Blessing(ModifierType.DAMAGE_REDUCTION, DamageService.DEFAULT_DAMAGE_REDUCTION,
                ROUNDS, TargetScope.SELF, source));
        target.grantBlessing(new Blessing(ModifierType.MAGIC_REDUCTION, MAGIC_REDUCTION, ROUNDS, TargetScope.SELF,
                source));
        return InteractionResult.builder();
    }
}
