package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

public class MovementServiceImpl implements MovementService {

    private final CharacterSizeService characterSizeService;
    private final ModifierResolver modifierResolver;

    public MovementServiceImpl() {
        this(new CharacterSizeServiceImpl(), new ModifierResolverImpl());
    }

    public MovementServiceImpl(final CharacterSizeService characterSizeService,
                                final ModifierResolver modifierResolver) {
        this.characterSizeService = characterSizeService;
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getMovementBase(final CombatantSheet sheet) {
        return getMovementBase(sheet, sheet.getMovementsTakenThisRound());
    }

    /**
     * The permanent total plus the two here-and-now sources — the sheet's Round-scoped {@code
     * MOVEMENT} TemporaryBonus, and {@code resolveRoundMovementIncrease} across the same four
     * sources {@link #getMovementBase(Character)} already scans for the permanent figure.
     * <b>Floored twice, deliberately.</b> The permanent figure is already clamped at 0 by
     * {@link #getMovementBase(Character)} — it is a stat, and a stat cannot be negative — and
     * the here-and-now sources are then added on top of that valid base, with the sum clamped
     * again. So a character carried below 0 by a malus reads 0 permanently, and a
     * first-movement bonus still lifts that movement above 0 rather than being swallowed by
     * the deficit.
     */
    @Override
    public int getMovementBase(final CombatantSheet sheet, final int movementIndex) {
        // Agarrado/Imobilizado: "não pode realizar movimentos" — a prohibition, so 0 outright
        // rather than a malus that happens to floor there.
        if (sheet.isMovementPrevented(null)) {
            return 0;
        }
        Character character = sheet.getCharacter();
        int total = getMovementBase(character) + sheet.getTemporaryBonus(ModifierType.MOVEMENT);

        for (AttributeAbility ability : character.getAttributeAbilities()) {
            total += ability.resolveRoundMovementIncrease(movementIndex);
        }
        for (SkillCompetencyAbility ability : SkillCompetencyAbility.allFor(character)) {
            total += ability.resolveRoundMovementIncrease(movementIndex);
        }
        for (Feat feat : character.getFeats()) {
            total += feat.resolveRoundMovementIncrease(movementIndex, character);
        }
        for (Item item : character.getEquipment()) {
            total += item.resolveRoundMovementIncrease(movementIndex, character);
        }
        return Math.max(0, total);
    }

    @Override
    public int getMovementBase(final Character character) {
        SizeCategory effectiveSize = characterSizeService.getEffectiveSizeCategory(character);
        int total = effectiveSize.getMovementPerActionPoint();

        List<SkillCompetencyAbility> skillCompetencyAbilities = SkillCompetencyAbility.allFor(character);
        total += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.MOVEMENT);
        total += modifierResolver.sumModifiers(skillCompetencyAbilities, ModifierType.MOVEMENT);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.MOVEMENT);
        }
        // Talentos are not part of any ModifierResolver scan (nothing scans them reflectively),
        // so they get an explicit pass here — the same shape DefenseServiceImpl already uses.
        for (Feat feat : character.getFeats()) {
            total += feat.resolveMovementIncrease(character);
        }
        // Equipment-held bonuses are data, not @Modifier methods — an explicit pass, the same
        // shape DamageServiceImpl/AbstractSkillInteraction use for resolveEnhancementBonus. This
        // is what makes a Pedra do Poder's "Movimento Base +2UD" Efeito Base real.
        for (Item item : character.getEquipment()) {
            total += item.resolveEnhancementBonus(ModifierType.MOVEMENT, null, character);
        }
        return Math.max(0, total);
    }
}
