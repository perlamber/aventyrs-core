package org.aventyrs.core.character.services;

import java.util.List;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitle;

/**
 * The one implementation of both figures — {@code AbstractSkillInteraction} resolves a roll's
 * critical through this class rather than keeping its own copy, so what a sheet screen previews and
 * what the roll then applies cannot drift apart.
 */
public class CriticalServiceImpl implements CriticalService {

    @Override
    public int getLesserCriticalMargin(final CombatantSheet holder, final SkillType skillType,
                                       final AttackSource attackSource, final SceneContext sceneContext,
                                       final CombatantSheet target) {
        int margin = getBaseLesserCriticalMargin(attackSource);
        int widening = sumCriticalMarginIncrease(holder, skillType, attackSource, sceneContext);
        widening -= target == null ? 0 : target.getTotalCriticalResistance(sceneContext);
        return margin - Math.max(0, widening);
    }

    /**
     * The weapon's own authored Margem Crítica Menor, before any widening — {@link
     * Weapon#DEFAULT_LESSER_CRITICAL_MARGIN} for an attack made with anything that is not a weapon
     * (a Magia, an unarmed strike) and for a plain Perícia test.
     */
    @Override
    public int getBaseLesserCriticalMargin(final AttackSource attackSource) {
        return attackSource instanceof Weapon weapon
                ? weapon.getLesserCriticalMargin()
                : Weapon.DEFAULT_LESSER_CRITICAL_MARGIN;
    }

    /**
     * Every "+1 número" the holder currently has for this attack, summed — the four ability/trait
     * scans plus the wielded weapon's own fitted enhancements. The target's Resistência a Críticos
     * is <em>not</em> applied here: {@link #getLesserCriticalMargin} subtracts it, and the roll
     * path needs the two apart so the clamp lands in the right place.
     */
    @Override
    public int sumCriticalMarginIncrease(final CombatantSheet holder, final SkillType skillType,
                                         final AttackSource attackSource, final SceneContext sceneContext) {
        Character character = holder.getCharacter();
        List<SkillCompetencyAbility> skillCompetencyAbilities = SkillCompetencyAbility.allFor(character, holder);
        int total = character.getAttributeAbilities().stream()
                .mapToInt(ability -> ability.resolveCriticalMarginIncrease(skillType, sceneContext))
                .sum();
        total += skillCompetencyAbilities.stream()
                .mapToInt(ability -> ability.resolveCriticalMarginIncrease(skillType, sceneContext))
                .sum();
        total += character.getEgoAdvantages().values().stream()
                .mapToInt(advantage -> advantage.resolveCriticalMarginIncrease(skillType, sceneContext))
                .sum();
        // Talentos are outside every ModifierResolver scan, so they get an explicit fourth pass —
        // the same shape AbstractSkillInteraction's own Talento passes use.
        total += character.getFeats().stream()
                .mapToInt(feat -> feat.resolveCriticalMarginIncrease(skillType, sceneContext, character,
                        attackSource, holder))
                .sum();
        // And a fifth for the wielded weapon's fitted enhancements (OffensiveMasterpiece#DECISIVA/
        // #MITRAL), scoped to the weapon this attack is made with by
        // Item#resolveEnhancementCriticalMarginIncrease.
        if (attackSource instanceof Weapon weapon) {
            total += character.getEquipment().stream()
                    .mapToInt(item -> item.resolveEnhancementCriticalMarginIncrease(weapon, character))
                    .sum();
        }
        // And a sixth for the held Títulos (Senhor da Briga's natural-weapon and Defesa margins).
        total += character.getAllTitles().stream()
                .mapToInt(title -> title.resolveCriticalMarginIncrease(skillType, attackSource, holder))
                .sum();
        return total;
    }

    @Override
    public CriticalDamage getCriticalDamage(final CombatantSheet holder, final SkillType skillType,
                                            final AttackSource attackSource, final CriticalResult criticalResult,
                                            final SkillRoll skillRoll, final SceneContext sceneContext) {
        if (skillType == null || !skillType.isAttackSkill()) {
            return CriticalDamage.NONE;
        }
        Character character = holder.getCharacter();
        boolean baselineReplaced = character.getFeats().stream()
                .anyMatch(feat -> feat.replacesBaselineCriticalAdvantage(skillType, attackSource, character));
        CriticalDamage total = baselineReplaced
                ? CriticalDamage.NONE
                : CriticalDamage.ofFlat(Skill.ADVANTAGE_BONUS);
        for (Feat feat : character.getFeats()) {
            total = total.plus(feat.resolveCriticalDamage(skillType, sceneContext, character, attackSource,
                    criticalResult, skillRoll));
        }
        if (attackSource instanceof Weapon weapon) {
            for (Item item : character.getEquipment()) {
                total = total.plus(item.resolveEnhancementCriticalDamage(weapon, character));
            }
        }
        for (AventyrTitle title : character.getAllTitles()) {
            total = total.plus(title.resolveCriticalDamage(skillType, attackSource, criticalResult, holder));
        }
        return total;
    }
}
