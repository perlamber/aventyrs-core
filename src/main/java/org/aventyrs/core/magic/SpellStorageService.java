package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;

/**
 * Holding a cast Magia back and releasing it later — {@code MetamagicoFeat#ARMAZENAR_MAGIA} ("uma
 * Magia do Tipo Semente ou Broto … soltá-la como uma Ação Livre posteriormente. Apenas uma magia
 * pode ser armazenada desta forma por vez") and {@code #ARMAZENAR_MAGIA_SUPERIOR} ("uma Magia …
 * adicional (para um total de 2 Magias armazenadas) ou … uma única magia do tipo Muda … como Ação
 * Livre ou Reação").
 *
 * <p>The caster casts through {@link SpellCastingService} as usual — paying its costs — and then
 * {@link #store}s the result instead of applying it; {@link #release} hands it back for the caller
 * to apply. Every held Magia dissipates at the next Descanso. Neither step spends the release
 * action: like every {@code ActionCost} here it is reported, not deducted — except a Reação, which
 * {@link #release} spends on the sheet's ledger.
 */
public interface SpellStorageService {

    /**
     * Holds cast of spell back on caster.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code SPELL_STORAGE_NOT_GRANTED}
     *         without Armazenar Magia, {@code SPELL_STORAGE_FULL} when the Magia is too deep or no
     *         room is left
     */
    StoredSpell store(CombatantSheet caster, Spell spell, SpellCastingResult cast);

    /**
     * Releases the held Magia spell with the action releaseCost, returning it for the caller to
     * apply. An Ação Livre always; a Reação only with Armazenar Magia Superior.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code STORED_SPELL_NOT_HELD},
     *         {@code STORED_SPELL_RELEASE_COST_NOT_PERMITTED}
     */
    StoredSpell release(CombatantSheet caster, Spell spell, ActionCost releaseCost);

    /** What caster is holding back right now. */
    default List<StoredSpell> getStoredSpells(final CombatantSheet caster) {
        return caster.getStoredSpells();
    }
}
