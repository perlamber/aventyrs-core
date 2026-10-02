package org.aventyrs.core.character.services;

import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Whether thief may steal life from victim at all (core 0.0.90) — {@code Troll}'s "imunes a efeitos de Roubo de Vida
 * de personagens que não tenham Anatomia Vegetal" ({@code Race#isImmuneToLifeStealFrom}), silent while a Forma
 * suppresses the victim's innate racial traits. Asked by every stage that heals a thief off a hit: {@code
 * effect.RouboDeVida}, {@code effect.OferendaMaldita}, {@code effect.MagicaeMortis} and {@code sheet.VampiricVenom}.
 */
public final class LifeStealGuard {

    private LifeStealGuard() {
    }

    /** Whether thief's Roubo de Vida reaches victim. A {@code null} on either side allows it. */
    public static boolean allows(final CombatantSheet victim, final CombatantSheet thief) {
        if (victim == null || thief == null || victim.getCharacter().getRace() == null) {
            return true;
        }
        return victim.getRacialTraitSuppression().suppressesInnateTraits()
                || !victim.getCharacter().getRace().isImmuneToLifeStealFrom(thief.getCharacter());
    }
}
