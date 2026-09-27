package org.aventyrs.core.magic;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.MetamagicoFeat;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.SPELL_STORAGE_FULL;
import static org.aventyrs.core.util.TranslatableMessages.SPELL_STORAGE_NOT_GRANTED;
import static org.aventyrs.core.util.TranslatableMessages.STORED_SPELL_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.STORED_SPELL_RELEASE_COST_NOT_PERMITTED;

public class SpellStorageServiceImpl implements SpellStorageService {

    /** Armazenar Magia: one Semente or Broto. */
    private static final int BASE_CAPACITY = 1;

    /** Armazenar Magia Superior: "para um total de 2 Magias armazenadas". */
    private static final int SUPERIOR_CAPACITY = 2;

    @Override
    public StoredSpell store(final CombatantSheet caster, final Spell spell, final SpellCastingResult cast) {
        Character character = caster.getCharacter();
        boolean superior = holds(character, MetamagicoFeat.ARMAZENAR_MAGIA_SUPERIOR);
        if (!superior && !holds(character, MetamagicoFeat.ARMAZENAR_MAGIA)) {
            throw new IllegalOperationException(SPELL_STORAGE_NOT_GRANTED);
        }
        List<Spell> held = new ArrayList<>(caster.getStoredSpells().stream().map(StoredSpell::spell).toList());
        held.add(spell);
        if (!fits(held, superior)) {
            throw new IllegalOperationException(SPELL_STORAGE_FULL);
        }
        StoredSpell stored = new StoredSpell(spell, cast);
        caster.storeSpell(stored);
        return stored;
    }

    @Override
    public StoredSpell release(final CombatantSheet caster, final Spell spell, final ActionCost releaseCost) {
        StoredSpell stored = caster.getStoredSpells().stream()
                .filter(candidate -> candidate.spell().equals(spell))
                .findFirst()
                .orElseThrow(() -> new IllegalOperationException(STORED_SPELL_NOT_HELD));
        boolean reaction = releaseCost != null && releaseCost.kind() == ActionCost.Kind.REACTION;
        boolean freeAction = releaseCost != null && releaseCost.kind() == ActionCost.Kind.FREE_ACTION;
        if (!freeAction && !(reaction && holds(caster.getCharacter(), MetamagicoFeat.ARMAZENAR_MAGIA_SUPERIOR))) {
            throw new IllegalOperationException(STORED_SPELL_RELEASE_COST_NOT_PERMITTED);
        }
        caster.removeStoredSpell(stored);
        if (reaction) {
            caster.spendReaction();
        }
        return stored;
    }

    /**
     * Whether held (the new Magia included) is a permitted load: up to the capacity of Sementes and
     * Brotos, or — with Superior — one Muda on its own.
     */
    private static boolean fits(final List<Spell> held, final boolean superior) {
        boolean allShallow = held.stream().allMatch(spell -> !spell.getBranchLevel().isAtLeast(BranchLevel.MUDA));
        if (allShallow) {
            return held.size() <= (superior ? SUPERIOR_CAPACITY : BASE_CAPACITY);
        }
        return superior && held.size() == 1 && held.get(0).getBranchLevel() == BranchLevel.MUDA;
    }

    private static boolean holds(final Character character, final Feat feat) {
        return character.getFeats().stream().anyMatch(held -> held.catalogEntry() == feat);
    }
}
