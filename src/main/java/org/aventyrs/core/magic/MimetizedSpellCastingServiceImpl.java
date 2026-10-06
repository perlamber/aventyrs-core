package org.aventyrs.core.magic;

import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.title.AventyrTitle;

import java.util.OptionalInt;

import static org.aventyrs.core.util.TranslatableMessages.HIT_POINT_PAYMENT_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_HIT_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.SPELL_CASTING_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.MIMETIZED_SPELL_FORM_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.MIMETIZED_SPELL_NOT_HELD;

public class MimetizedSpellCastingServiceImpl implements MimetizedSpellCastingService {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    @Override
    public MimetizedSpellCastingResult cast(final CombatantSheet caster, final MimetizedSpell mimetizedSpell)
            throws IllegalOperationException {
        return cast(caster, mimetizedSpell, false);
    }

    @Override
    public OptionalInt resolveHitPointCost(final CombatantSheet caster, final MimetizedSpell mimetizedSpell) {
        AventyrTitle primary = caster.getCharacter().getPrimaryTitle();
        return caster.getCharacter().getAllTitles().stream()
                .map(title -> title.resolveMimicryHitPointCost(mimetizedSpell, caster, title == primary))
                .filter(OptionalInt::isPresent)
                .findFirst()
                .orElse(OptionalInt.empty());
    }

    @Override
    public MimetizedSpellCastingResult cast(final CombatantSheet caster, final MimetizedSpell mimetizedSpell,
                                            final boolean payWithHitPoints) throws IllegalOperationException {
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
        if (!payWithHitPoints) {
            caster.spendDeterminationPoints(mimetizedSpell.getDeterminationPointCost());
            return new MimetizedSpellCastingResult(mimetizedSpell);
        }
        int hitPoints = resolveHitPointCost(caster, mimetizedSpell)
                .orElseThrow(() -> new IllegalOperationException(HIT_POINT_PAYMENT_NOT_PERMITTED));
        // A PV cost can never be paid down to 0 PV — the refusal every PV price in this core applies.
        if (hitPointsService.getCurrentHitPoints(caster.getCharacter(), caster) <= hitPoints) {
            throw new IllegalOperationException(NOT_ENOUGH_HIT_POINTS);
        }
        caster.payWithVitality(hitPoints);
        caster.getCharacter().getAllTitles()
                .forEach(title -> title.consumeMimicryHitPointPayment(mimetizedSpell, caster));
        return new MimetizedSpellCastingResult(mimetizedSpell, hitPoints);
    }
}
