package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;

import java.math.BigDecimal;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;

/**
 * A player character's sheet — a {@link CombatantSheet} plus the three things only a player has:
 * the {@link Player} behind it, an experience wallet, and Fama.
 *
 * <p>Everything a foe also does — damage, shields, Mana/Determinação, temporary Ego points,
 * Efeitos, the Turn lifecycle — lives on {@link AbstractCombatantSheet} and is shared verbatim
 * with {@code org.aventyrs.core.monster.MonsterSheet}. Only what's genuinely player-shaped is
 * here.
 *
 * <p><b>That's what keeps monsters out of the progression system.</b> Experience is spent from
 * this class, so the four services that spend it — {@code CharacterAttributeService#upgradeBase},
 * {@code SkillGraduationService#upgradeGraduation}, {@code FeatService#grantFeat}, {@code
 * TitleAbilityService#grantTitleAbility} — take a {@code CharacterSheet} rather than a {@code
 * CombatantSheet}, and a monster therefore cannot reach them at all. A monster's Attributes and
 * Graduações are deliberately uncapped (see CLAUDE.md's caps section — the builders never
 * validated anything), so the thing actually worth preventing was never "exceeding the cap", it
 * was "levelling up like a character". No {@code isMonster()} flag exists because none is needed:
 * it doesn't compile.
 */
@Getter
public class CharacterSheet extends AbstractCombatantSheet {

    @NonNull
    private final Player player;

    private BigDecimal totalExperience = BigDecimal.ZERO;

    private BigDecimal unUsedExperience = BigDecimal.ZERO;

    private int famaPositiva = 0;

    private int famaNegativa = 0;

    private CharacterSheet(final Character character, final Player player) {
        super(character);
        this.player = player;
    }

    public static CharacterSheet of(@NonNull final Character character, @NonNull final Player player) {
        return new CharacterSheet(character, player);
    }

    /**
     * Same as {@link #of(Character, Player)}, but with a known id instead of a freshly minted one
     * — for reconstructing a sheet from persisted state (e.g. a DTO) whose identity already
     * exists.
     */
    public static CharacterSheet of(final Character character, final Player player, @NonNull final UUID id) {
        CharacterSheet sheet = of(character, player);
        sheet.restoreId(id);
        return sheet;
    }

    /**
     * Consumes the available experience.
     *
     * <p>The subtraction happens only once the result is known to be non-negative — an earlier
     * version subtracted first and checked afterwards, so a rejected spend still silently
     * corrupted the balance.
     *
     * @param expToUse experience to be used
     * @return BigDecimal remaining experience
     * @throws IllegalOperationException in case unUsed experience is lower than consumed
     */
    public BigDecimal useExperience(BigDecimal expToUse) throws IllegalOperationException {
        BigDecimal remainingExperience = unUsedExperience.subtract(expToUse);
        if (remainingExperience.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalOperationException(NOT_ENOUGH_EXPERIENCE);
        }
        return unUsedExperience = remainingExperience;
    }

    public BigDecimal accumulateExperience(BigDecimal experience) {
        unUsedExperience = unUsedExperience.add(experience);
        return totalExperience = totalExperience.add(experience);
    }

    /**
     * Increases Fama Positiva — e.g. an Excelência bonus or a Narrador reward.
     * @return int total Fama Positiva after the increase
     */
    public int increaseFamaPositiva(int amount) {
        return famaPositiva += amount;
    }

    /**
     * Increases Fama Negativa — e.g. an Excelência bonus or a Narrador reward.
     * @return int total Fama Negativa after the increase
     */
    public int increaseFamaNegativa(int amount) {
        return famaNegativa += amount;
    }
}
