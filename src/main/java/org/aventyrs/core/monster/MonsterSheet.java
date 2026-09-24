package org.aventyrs.core.monster;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.sheet.AbstractCombatantSheet;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A foe's sheet — a {@link CombatantSheet} plus the stat-block numbers a foe presents <i>because
 * it never rolls</i>.
 *
 * <p>This game's dice are always rolled by the player, so a foe contributes fixed values in both
 * directions of an exchange: a Grau de Dificuldade the player's Esquiva e Aparar roll must beat
 * when the foe attacks, and a Defesa the player's Ataque roll must beat when the foe is attacked.
 * The Defesas and the GDs — one per Perícia it knows, plus a general one — are the entire
 * difference between this class and {@link CharacterSheet} —
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
     * The GD it presents on any Perícia with no entry of its own in {@link #getSkillDifficulties()}
     * — see {@link MonsterTemplate#getGeneralDifficulty()}.
     */
    private final SkillDifficulty generalDifficulty;

    /**
     * The GD it presents per Perícia it knows, unmodifiable — see {@link
     * MonsterTemplate#getSkillDifficulties()}. {@link #getSkillDifficulty(SkillType)} is the lookup.
     */
    private final Map<SkillType, SkillDifficulty> skillDifficulties;

    /**
     * Whether this foe is a Morto-Vivo — see {@link MonsterTemplate#isUndead()} for what this
     * narrow flag does and does not claim to model.
     */
    private final boolean undead;

    private final Set<CriticalEffectType> criticalEffectImmunities;

    /**
     * The GM running this foe — required, same as {@link CharacterSheet#getPlayer()}. A foe is
     * never a fixed catalog constant once it's playing in a Cena: someone at the table owns the
     * stat block instance, spends its Turn, edits its sheet. {@code Character#getPlayer()} stays
     * unused/nullable ("nobody plays" a Character); this is the sheet-level ownership that
     * actually matters, exactly the split {@link CharacterSheet} already draws.
     */
    @NonNull
    private final Player player;

    private MonsterSheet(final Character character, final Player player, final int physicalDefense, final int magicDefense,
                         final SkillDifficulty generalDifficulty, final Map<SkillType, SkillDifficulty> skillDifficulties,
                         final boolean undead, final Set<CriticalEffectType> criticalEffectImmunities) {
        super(character);
        this.player = player;
        this.physicalDefense = physicalDefense;
        this.magicDefense = magicDefense;
        this.generalDifficulty = generalDifficulty;
        this.skillDifficulties = Map.copyOf(skillDifficulties);
        this.undead = undead;
        this.criticalEffectImmunities = Set.copyOf(criticalEffectImmunities);
    }

    /**
     * A foe assembled from its Defesas and a single GD it presents on every Perícia — ordinary
     * anatomy, no immunities, no per-Perícia entries. The overload to reach for when hand-building a
     * sheet in a test or a caller that has the numbers but no template.
     */
    public static MonsterSheet of(@NonNull final Character character, @NonNull final Player player,
                                  final int physicalDefense, final int magicDefense,
                                  @NonNull final DifficultyLevel generalDifficulty, final int generalBonus) {
        return of(character, player, physicalDefense, magicDefense,
                SkillDifficulty.of(generalDifficulty, generalBonus), Map.of());
    }

    /**
     * Same as {@link #of(Character, Player, int, int, DifficultyLevel, int)}, but with a known id
     * instead of a freshly minted one — for reconstructing a foe from persisted state whose
     * identity already exists, mirroring {@code CharacterSheet.of(Character, Player, UUID)}.
     */
    public static MonsterSheet of(final Character character, final Player player, final int physicalDefense, final int magicDefense,
                                  final DifficultyLevel generalDifficulty, final int generalBonus, @NonNull final UUID id) {
        MonsterSheet sheet = of(character, player, physicalDefense, magicDefense, generalDifficulty, generalBonus);
        sheet.restoreId(id);
        return sheet;
    }

    /**
     * A foe with its general GD and its per-Perícia GDs — ordinary anatomy, no immunities. The form
     * a caller restoring a persisted foe reaches for, since it holds the numbers but no template.
     */
    public static MonsterSheet of(@NonNull final Character character, @NonNull final Player player,
                                  final int physicalDefense, final int magicDefense,
                                  @NonNull final SkillDifficulty generalDifficulty,
                                  @NonNull final Map<SkillType, SkillDifficulty> skillDifficulties) {
        return new MonsterSheet(character, player, physicalDefense, magicDefense, generalDifficulty,
                skillDifficulties, false, Set.of());
    }

    /**
     * The full foe — every authored number <i>and</i> every anatomy fact, read straight off the
     * stat block. This is what {@link MonsterTemplate#spawn(Player)} calls.
     *
     * <p>It takes the template rather than growing the positional overloads above further. The
     * anatomy halves ({@code undead}, the immunity set) are exactly as authored as the combat
     * numbers, so the alternative was a signature nobody could read at a call site — and the
     * template is already in hand wherever a complete foe is being built.
     */
    public static MonsterSheet of(@NonNull final Character character, @NonNull final Player player, @NonNull final MonsterTemplate template) {
        return new MonsterSheet(character, player, template.getPhysicalDefense(), template.getMagicDefense(),
                template.getGeneralDifficulty(), template.getSkillDifficulties(),
                template.isUndead(), template.getCriticalEffectImmunities());
    }

    /** {@link #of(Character, Player, MonsterTemplate)} with a known id — the persistence-restore path. */
    public static MonsterSheet of(final Character character, final Player player, final MonsterTemplate template, @NonNull final UUID id) {
        MonsterSheet sheet = of(character, player, template);
        sheet.restoreId(id);
        return sheet;
    }

    /**
     * The GD it presents on skillType — its own entry, or {@link #getGeneralDifficulty()}. Exactly
     * {@link MonsterTemplate#getSkillDifficulty(SkillType)}, read off the sheet: an attack fills
     * {@code IncomingAttack}'s {@code difficultyLevel}/{@code attackBonus} from {@code
     * getSkillDifficulty(ATAQUE_CORPO_A_CORPO)} (or {@code ATAQUE_A_DISTANCIA}).
     */
    public SkillDifficulty getSkillDifficulty(@NonNull final SkillType skillType) {
        return skillDifficulties.getOrDefault(skillType, generalDifficulty);
    }

    /**
     * The fixed Atenção it presents to anyone hiding from it — its {@link SkillType#ATTENTION} GD
     * flattened. Read by {@code org.aventyrs.core.character.services.HidingService#resolveDetection}
     * in place of an Atenção roll. See {@link MonsterTemplate#getPerception()}.
     */
    public int getPerception() {
        return getSkillDifficulty(SkillType.ATTENTION).getValue();
    }

    @Override
    public Set<CriticalEffectType> getCriticalEffectImmunities() {
        return criticalEffectImmunities;
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
