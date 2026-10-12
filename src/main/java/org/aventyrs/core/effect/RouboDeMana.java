package org.aventyrs.core.effect;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Roubo de Mana on a landed hit — the target loses {@link #getAmount()} PM and the attacker recovers what was
 * actually taken, never more than the target still had. The PM mirror of the Roubo de Determinação ruling
 * (2026-10-01, {@link Oprimir}), adopted for Roubo de Mana on 2026-10-12.
 *
 * <p>{@code AttackDelivery} chains it right behind the damage for a weapon whose Aprimoramento carries it ({@code
 * OffensiveImprovement#BENCAO_ELDURIANA}, {@code #LADRA_DO_AETHER}); like every stage there, only a hit that dealt
 * damage reaches it.
 */
@Getter
public class RouboDeMana extends AbstractEffect {

    private static final MagicPointsService MAGIC_POINTS = new MagicPointsServiceImpl();

    private final CombatantSheet attacker;
    private final int amount;

    public RouboDeMana(@NonNull final CombatantSheet attacker, final int amount) {
        this.attacker = attacker;
        this.amount = amount;
    }

    @Override
    public String getDescription() {
        return "Roubo de Mana " + amount;
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int taken = Math.min(Math.max(0, amount),
                Math.max(0, MAGIC_POINTS.getCurrentMagicPoints(target.getCharacter(), target)));
        if (taken > 0) {
            target.spendMagicPoints(taken);
            attacker.recoverMagicPoints(taken);
        }
        return reportChain(InteractionResult.builder()
                .resourceLossValue(taken)
                .resourceLossType(ResourceType.MAGIC_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
