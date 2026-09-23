package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.skill.AttackSource;

import java.util.UUID;

/**
 * Empalar — "A arma fica presa no alvo". Open-ended: it stays until removed, which is {@link
 * CombatantSheet#removeImpalement} — "remover a arma causa 3d6 pontos de dano (este dano não pode
 * ser reduzido por efeitos de redução)" (Maior) / "2d6" (Menor), so the removal is bare PV loss.
 *
 * <p>The price of removing it differs by who does it and by the tier, and is reported, never
 * charged: Maior "1 ação livre, ou 2PA do alvo", Menor "1PA (do usuário ou do alvo)" — {@link
 * #getOwnerRemovalCost()} for the weapon's owner, {@link #getTargetRemovalCost()} for the impaled.
 */
@Getter
public class Impalement extends TemporaryEffect {

    private final AttackSource weapon;
    private final UUID ownerId;
    private final int removalDice;
    private final ActionCost ownerRemovalCost;
    private final ActionCost targetRemovalCost;

    public Impalement(final AttackSource weapon, final UUID ownerId, final int removalDice,
                      final ActionCost ownerRemovalCost, final ActionCost targetRemovalCost) {
        super(null);
        this.weapon = weapon;
        this.ownerId = ownerId;
        this.removalDice = removalDice;
        this.ownerRemovalCost = ownerRemovalCost;
        this.targetRemovalCost = targetRemovalCost;
    }
}
