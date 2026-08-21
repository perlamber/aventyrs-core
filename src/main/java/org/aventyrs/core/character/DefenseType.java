package org.aventyrs.core.character;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.modifier.ModifierType;

import java.util.function.ToIntFunction;

/**
 * Which of the two Defesas an incoming attack is resolved against — DF (Defesa Física) or DM
 * (Defesa Mágica).
 *
 * <p>A Defesa in this ruleset is <b>not</b> a passive score an attacker rolls against: this
 * game's dice are always rolled by the player, so an incoming attack is resolved as the
 * defender's own Esquiva e Aparar roll against the Grau de Dificuldade the attack presents. What
 * this type selects is therefore <i>which pool of bonuses feeds that roll</i> — which is exactly
 * how this codebase already modeled Defesas before the type existed: {@code
 * org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararExcellency#FOCADO}'s "Defesas +1" and
 * {@code EsquivaEApararCompetencyAbility#MOVIMENTO_DEFENSIVO}'s "+3 em suas Defesas" are both
 * plain {@code ModifierType#SKILL_ROLL_BONUS} methods. DF and DM are two variants of the same
 * roll's bonus, not two new standalone stats.
 *
 * <p>Each constant carries both halves of its own mapping — the scoped {@link ModifierType} and
 * which of an {@link Item}'s two DF/DM columns applies — so {@code
 * org.aventyrs.core.character.services.DefenseService} never branches on the constant itself,
 * the same one-constant-carries-its-own-lookup convention {@code
 * org.aventyrs.core.skill.SkillType} already uses for {@code rollBonusType}/{@code
 * excellencyClass}/{@code interactionFactory}.
 *
 * <p>{@link ModifierType#DEFESAS} remains the broad "applies to both" type and is summed
 * <i>alongside</i> whichever scoped type this names — never instead of it.
 */
@Getter
@AllArgsConstructor
public enum DefenseType {

    /** DF — resisting a physical attack. */
    PHYSICAL(ModifierType.PHYSICAL_DEFENSE, Item::getPhysicalDefenseBonus),

    /** DM — resisting a magical attack. */
    MAGIC(ModifierType.MAGIC_DEFENSE, Item::getMagicDefenseBonus);

    /**
     * The {@link ModifierType} scoped to this Defesa alone. A source wanting to affect both
     * names {@link ModifierType#DEFESAS} instead.
     */
    private final ModifierType modifierType;

    /**
     * Which of an {@link Item}'s two flat Defesa columns ({@link Item#getPhysicalDefenseBonus()}
     * / {@link Item#getMagicDefenseBonus()}) this Defesa reads.
     */
    private final ToIntFunction<Item> itemColumn;

    /** That column's value for item — the flat, unconditional half of an item's contribution. */
    public int columnOf(final Item item) {
        return itemColumn.applyAsInt(item);
    }
}
