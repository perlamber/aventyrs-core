package org.aventyrs.core.monster;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.sheet.AbstractCombatantSheet;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.DifficultyLevel;

import java.util.UUID;

/**
 * A foe's sheet — a {@link CombatantSheet} plus the stat-block numbers a foe presents <i>because
 * it never rolls</i>.
 *
 * <p>This game's dice are always rolled by the player, so a foe contributes fixed values in both
 * directions of an exchange: a Grau de Dificuldade the player's Esquiva e Aparar roll must beat
 * when the foe attacks, and a Defesa the player's Ataque roll must beat when the foe is attacked.
 * Those four numbers are the entire difference between this class and {@link CharacterSheet} —
 * everything else (damage, shields, Mana, Efeitos, inventory, the Turn lifecycle) comes from
 * {@link AbstractCombatantSheet} and behaves identically for both.
 *
 * <p>The numbers are <b>authored on the template</b>, not derived from the foe's own Perícias.
 * A stat block says what a Goblin's DF is; it isn't recomputed from its Destreza and Graduação
 * the way a player's defence roll is. See {@link MonsterTemplate}.
 *
 * <p><b>No experience, no Fama, no {@code Player}</b> — and that isn't just omission, it's what
 * keeps a monster out of the progression system entirely. The four XP-spending services take a
 * {@link CharacterSheet}, so none of them will accept one of these. See {@link CombatantSheet}'s
 * own javadoc for the full reasoning; the short version is that a monster's Attributes and
 * Graduações are deliberately uncapped, so what needed preventing was levelling up, not
 * exceeding a cap.
 */
@Getter
public class MonsterSheet extends AbstractCombatantSheet {

    /**
     * Its DF — the target number a player's Ataque roll must reach to land a physical attack on
     * it. Read by {@code org.aventyrs.core.combat.AttackDelivery} via {@link #getDefense}.
     */
    private final int physicalDefense;

    /** Its DM — the same, for a magical attack. */
    private final int magicDefense;

    /**
     * The Grau de Dificuldade its own attacks present to a defender's Esquiva e Aparar roll —
     * exactly {@code IncomingAttack}'s own {@code difficultyLevel}, so
     * {@code IncomingAttack.from(this, defender, …)} fills it straight off this sheet.
     */
    private final DifficultyLevel attackDifficulty;

    /** The flat modifier on top of {@link #getAttackDifficulty()}'s own threshold. */
    private final int attackBonus;

    private MonsterSheet(final Character character, final int physicalDefense, final int magicDefense,
                         final DifficultyLevel attackDifficulty, final int attackBonus) {
        super(character);
        this.physicalDefense = physicalDefense;
        this.magicDefense = magicDefense;
        this.attackDifficulty = attackDifficulty;
        this.attackBonus = attackBonus;
    }

    public static MonsterSheet of(@NonNull final Character character, final int physicalDefense, final int magicDefense,
                                  @NonNull final DifficultyLevel attackDifficulty, final int attackBonus) {
        return new MonsterSheet(character, physicalDefense, magicDefense, attackDifficulty, attackBonus);
    }

    /**
     * Same as {@link #of}, but with a known id instead of a freshly minted one — for
     * reconstructing a foe from persisted state whose identity already exists, mirroring {@code
     * CharacterSheet.of(Character, Player, UUID)}.
     */
    public static MonsterSheet of(final Character character, final int physicalDefense, final int magicDefense,
                                  final DifficultyLevel attackDifficulty, final int attackBonus, @NonNull final UUID id) {
        MonsterSheet sheet = of(character, physicalDefense, magicDefense, attackDifficulty, attackBonus);
        sheet.restoreId(id);
        return sheet;
    }

    /**
     * Whichever of {@link #getPhysicalDefense()}/{@link #getMagicDefense()} defenseType names —
     * so a caller resolving an attack doesn't branch on the type itself, the same restraint
     * {@link DefenseType} already applies to an item's two Defesa columns.
     */
    public int getDefense(@NonNull final DefenseType defenseType) {
        return defenseType == DefenseType.PHYSICAL ? physicalDefense : magicDefense;
    }
}
