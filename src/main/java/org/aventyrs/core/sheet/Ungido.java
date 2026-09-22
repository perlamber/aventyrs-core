package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.item.ItemCategory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Which kinds of Equipamento are currently Ungido on their wearer — the record {@code
 * SantoAbility#PROTECAO_UNGIDA} keeps so that "Enquanto utilizar uma Armadura <b>e</b> um Escudo
 * Ungido ao mesmo tempo" is a question this core can answer.
 *
 * <p><b>Categories, not copies.</b> Which particular Armadura was blessed still is not recorded
 * and still does not matter: every mechanical consequence the rules text names lands on the
 * wearer, and the activation already gated on them using one. What the dual-blessing mode changed
 * is that *how many kinds* are blessed now matters, which a single flag could not carry.
 *
 * <p>The Meio-Dano the blessing grants stays an ordinary {@link ModifierType#HALF_DAMAGE}
 * {@link Blessing} rather than living here — {@code DamageServiceImpl} reads that as it reads
 * every other source, and nothing about it needs to know which item it came from. This effect
 * carries only the part that a bonus cannot: the set.
 */
@Getter
public class Ungido extends TemporaryEffect {

    private final Set<ItemCategory> blessedCategories;

    public Ungido(final Integer remainingRounds, final Set<ItemCategory> blessedCategories) {
        super(remainingRounds);
        this.blessedCategories = EnumSet.copyOf(blessedCategories);
    }

    /** One blessing at a time — re-anointing replaces what was blessed rather than adding to it. */
    @Override
    public boolean isCumulative() {
        return false;
    }

    /** Whether an item of this kind is currently Ungido. */
    public boolean blesses(final ItemCategory category) {
        return blessedCategories.contains(category);
    }

    /**
     * "Enquanto utilizar uma Armadura e um Escudo Ungido ao mesmo tempo" — the state the dual
     * blessing exists to reach, and the gate on the Duração-halving clause.
     */
    public boolean blessesBoth() {
        return blesses(ItemCategory.ARMOR) && blesses(ItemCategory.SHIELD);
    }
}
