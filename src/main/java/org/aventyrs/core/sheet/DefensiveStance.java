package org.aventyrs.core.sheet;

import org.aventyrs.core.feat.EscudeiroFeat;
import org.aventyrs.core.item.ItemCategory;

/**
 * Postura Defensiva (acoes-de-combate.txt 20261013, core 0.1.6.19) — "Você recebe Bônus de +2 em Defesas, este Bônus
 * muda para +3 se estiver utilizando um Escudo". Table ruling (2026-10-14): until its holder's own next Turn begins;
 * the Escudo is checked live, so losing it mid-Rodada drops the bonus to +2. Asas Adamantinas count as a Escudo while
 * not flying (the Escudeiro ruling). Read by {@code DefenseService}; taken through {@code DefensiveStanceService}.
 */
public class DefensiveStance extends TemporaryEffect {

    /** "+2 em Defesas". */
    public static final int BONUS = 2;
    /** "+3 se estiver utilizando um Escudo". */
    public static final int SHIELD_BONUS = 3;

    public DefensiveStance() {
        super(1, true);
    }

    @Override
    boolean isCumulative() {
        return false;
    }

    /** What the posture is worth to holder right now. */
    public int bonusFor(final CombatantSheet holder) {
        boolean shield = holder.getCharacter().getEquipment().stream()
                .anyMatch(item -> item.getCategory() == ItemCategory.SHIELD && !item.isDestroyed())
                || EscudeiroFeat.holdsWings(holder.getCharacter()) && !holder.isFlying();
        return shield ? SHIELD_BONUS : BONUS;
    }
}
