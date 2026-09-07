package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.MIMETIZED_SPELL_NOT_HELD;

public class MimetizedSpellCastingServiceImpl implements MimetizedSpellCastingService {

    @Override
    public MimetizedSpellCastingResult cast(final CombatantSheet caster, final MimetizedSpell mimetizedSpell)
            throws IllegalOperationException {
        if (!caster.getCharacter().getMimetizedSpells().contains(mimetizedSpell)) {
            throw new IllegalOperationException(MIMETIZED_SPELL_NOT_HELD);
        }
        caster.spendDeterminationPoints(mimetizedSpell.getDeterminationPointCost());
        return new MimetizedSpellCastingResult(mimetizedSpell);
    }
}
