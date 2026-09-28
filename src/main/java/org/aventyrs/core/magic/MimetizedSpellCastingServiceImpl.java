package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.SPELL_CASTING_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.MIMETIZED_SPELL_FORM_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.MIMETIZED_SPELL_NOT_HELD;

public class MimetizedSpellCastingServiceImpl implements MimetizedSpellCastingService {

    @Override
    public MimetizedSpellCastingResult cast(final CombatantSheet caster, final MimetizedSpell mimetizedSpell)
            throws IllegalOperationException {
        // Frenesi: "você também perde a capacidade de Conjurar ou Mimetizar Magias" — and Silêncio.
        if (caster.isSpellCastingPrevented(null)) {
            throw new IllegalOperationException(SPELL_CASTING_PREVENTED);
        }
        if (!caster.getCharacter().getMimetizedSpells().contains(mimetizedSpell)) {
            throw new IllegalOperationException(MIMETIZED_SPELL_NOT_HELD);
        }
        if (mimetizedSpell.getRequiredForm() != null && !caster.isInForm(mimetizedSpell.getRequiredForm())) {
            throw new IllegalOperationException(MIMETIZED_SPELL_FORM_REQUIRED);
        }
        caster.spendDeterminationPoints(mimetizedSpell.getDeterminationPointCost());
        return new MimetizedSpellCastingResult(mimetizedSpell);
    }
}
