package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.item.Item;

import java.util.List;
import java.util.Map;

public class ReactionsServiceImpl implements ReactionsService {

    private final ModifierResolver modifierResolver;

    public ReactionsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public ReactionsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalReactions(final Character character) {
        return Math.max(0, permanentReactions(character));
    }

    @Override
    public int getTotalReactions(final CombatantSheet sheet, final int turnNumber) {
        return getTotalReactions(sheet, turnNumber, null);
    }

    @Override
    public int getTotalReactions(final CombatantSheet sheet, final int turnNumber, final SceneContext sceneContext) {
        Character character = sheet.getCharacter();
        int timed = sheet.getTemporaryBonus(ModifierType.REACTIONS);
        // Mestre Escudeiro: "Você pode fazer Reações mesmo quando o efeito impedir Reações".
        boolean ignoresPrevention = character.getFeats().stream().anyMatch(feat -> feat.ignoresReactionPrevention(character));
        if (ignoresPrevention) {
            timed -= sheet.getTemporaryMalus(ModifierType.REACTIONS);
        }
        // Iniciativa a Zero's Reflexo Lento: "incapaz de fazer Reações" (core 0.0.82).
        if (sheet.hasEgoSetback(org.aventyrs.core.ego.EgoSetback.REFLEXO_LENTO)) {
            return 0;
        }
        // Preso a Imaginação: no Reações at all in an odd Rodada.
        if (!ignoresPrevention && character.getFeats().stream().anyMatch(feat -> feat.preventsReactions(character, sceneContext))) {
            return 0;
        }
        int baseline = permanentReactions(character) + timed;
        // Analista Tático: "Enquanto você for o último a agir você recebe uma … Reação adicional".
        for (org.aventyrs.core.feat.Feat feat : character.getFeats()) {
            baseline += feat.resolveReactionsIncrease(character, sceneContext);
        }
        return Math.max(0, character.getActionProfile().adjustReactions(baseline, turnNumber, sceneContext));
    }

    /**
     * The fixed counter plus the three-source {@code REACTIONS} scan — unclamped, so the
     * {@link org.aventyrs.core.action.ActionProfile} adjustment and any sheet-level
     * {@code TemporaryBonus} still see a genuine deficit rather than a floored 0.
     */
    private int permanentReactions(final Character character) {
        int total = character.getReactions();
        total += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.REACTIONS);
        total += modifierResolver.sumModifiers(SkillCompetencyAbility.allFor(character), ModifierType.REACTIONS);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += modifierResolver.sumModifiers(unlockedExcellencies, ModifierType.REACTIONS);
        }
        for (Item item : character.getEquipment()) {
            total += item.resolveEnhancementBonus(ModifierType.REACTIONS, null, character);
        }
        return total;
    }
}
