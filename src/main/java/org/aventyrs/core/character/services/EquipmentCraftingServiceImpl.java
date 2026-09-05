package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.Improvement;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemForgery;
import org.aventyrs.core.item.ItemSpecification;
import org.aventyrs.core.item.RegaliaDonation;
import org.aventyrs.core.item.ItemMasterpiece;
import org.aventyrs.core.item.ItemRarity;
import org.aventyrs.core.item.ItemTemplate;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.profissao.ProfissaoSpecialization;

import static org.aventyrs.core.util.TranslatableMessages.CANNOT_MODIFY_TEMPLATE;
import static org.aventyrs.core.util.TranslatableMessages.CRAFTING_TRADE_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.DUPLICATE_IMPROVEMENT;
import static org.aventyrs.core.util.TranslatableMessages.IMPROVEMENT_SLOTS_FULL;
import static org.aventyrs.core.util.TranslatableMessages.ITEM_NOT_A_MASTERPIECE;
import static org.aventyrs.core.util.TranslatableMessages.MASTERPIECE_GRADUATION_TOO_LOW;

public class EquipmentCraftingServiceImpl implements EquipmentCraftingService {

    @Override
    public int getFabricationCost(final ItemTemplate template) {
        return atLeastOne(template.getPrice() / 2);
    }

    @Override
    public int getFabricationTimeInDays(final ItemTemplate template) {
        return atLeastOne(template.getPrice() / 2);
    }

    @Override
    public int getFabricationTimeInDays(final Character crafter, final ItemTemplate template) {
        double factor = SkillCompetencyAbility.allFor(crafter).stream()
                .mapToDouble(SkillCompetencyAbility::resolveProductionTimeMultiplier)
                .reduce(1.0, (a, b) -> a * b);
        return atLeastOne((int) Math.floor(getFabricationTimeInDays(template) * factor));
    }

    @Override
    public DifficultyLevel getFabricationDifficulty(final ItemTemplate template) {
        return template.getRarity().getFabricationDifficulty();
    }

    @Override
    public DifficultyLevel getMasterpieceFabricationDifficulty(final ItemRarity masterpieceRarity) {
        return masterpieceRarity.getFabricationDifficulty().harder(1);
    }

    @Override
    public int getMasterpieceMinimumGraduation(final ItemRarity masterpieceRarity) {
        return masterpieceRarity.getMinimumMasterpieceGraduation();
    }

    @Override
    public DifficultyLevel getImprovementInstallDifficulty(final ItemRarity improvementRarity) {
        return improvementRarity.getImprovementInstallDifficulty();
    }

    @Override
    public int getImprovementInstallDisadvantage(final Item item) {
        return Skill.DISADVANTAGE_MALUS * item.getImprovements().size();
    }

    @Override
    public DifficultyLevel getRegaliaCraftingDifficulty(final RegaliaGrade grade) {
        return grade.getCraftingDifficulty();
    }

    @Override
    public int getRegaliaCraftingTimeInDays(final RegaliaGrade grade) {
        return grade.getCraftingTimeInDays();
    }

    @Override
    public int getRegaliaCraftingTimeInDays(final Character crafter, final RegaliaGrade grade) {
        double factor = SkillCompetencyAbility.allFor(crafter).stream()
                .mapToDouble(SkillCompetencyAbility::resolveProductionTimeMultiplier)
                .reduce(1.0, (a, b) -> a * b);
        return atLeastOne((int) Math.floor(grade.getCraftingTimeInDays() * factor));
    }

    @Override
    public boolean regaliaCraftingRequiresCriticalResult(final RegaliaGrade grade) {
        return grade.requiresCriticalResult();
    }

    @Override
    public RepairAssessment assessRepair(final Item item) {
        int damage = item.getDamageTaken();
        boolean isMasterpiece = item.getMasterpiece() != null;
        DifficultyLevel difficulty = isMasterpiece
                ? item.getRarity().getMasterpieceRepairDifficulty()
                : item.getRarity().getRepairDifficulty();
        if (damage <= 0) {
            return RepairAssessment.none(difficulty);
        }
        boolean severe = damage * 2 > item.getEffectiveHardness();
        int price = item.getPrice();
        int peCost = atLeastOne(severe ? price / 3 : price / 10);
        int hours = damage * (severe ? 2 : 1);
        int disadvantage = getImprovementInstallDisadvantage(item);
        return new RepairAssessment(damage, peCost, hours, difficulty, disadvantage, severe);
    }

    @Override
    public Item forge(final Character crafter, final ProfissaoSpecialization trade,
                      final ItemTemplate template, final ItemMasterpiece masterpiece)
            throws IllegalOperationException {
        return forge(crafter, trade, ItemSpecification.builder().base(template).masterpiece(masterpiece).build(),
                null);
    }

    @Override
    public Item forge(final Character crafter, final ProfissaoSpecialization trade,
                      final ItemSpecification specification, final RegaliaDonation donation)
            throws IllegalOperationException {
        return ItemForgery.by(crafter, trade, specification, donation).forge();
    }

    @Override
    public Item forgeRegalia(final Character crafter, final ProfissaoSpecialization trade,
                             final ItemSpecification specification, final RegaliaDonation donation)
            throws IllegalOperationException {
        return forge(crafter, trade, specification, donation);
    }

    @Override
    public void installImprovement(final Character crafter, final Item item, final Improvement improvement)
            throws IllegalOperationException {
        AbstractItem copy = asForgedCopy(item);
        if (copy.getMasterpiece() == null) {
            throw new IllegalOperationException(ITEM_NOT_A_MASTERPIECE);
        }
        if (copy.getImprovements().size() >= copy.getWeightClass().getMaximumImprovements()) {
            throw new IllegalOperationException(IMPROVEMENT_SLOTS_FULL);
        }
        if (copy.getImprovements().stream().anyMatch(fitted -> fitted.equals(improvement))) {
            throw new IllegalOperationException(DUPLICATE_IMPROVEMENT);
        }
        copy.addImprovement(improvement);
    }

    @Override
    public int repair(final Character repairer, final ProfissaoSpecialization trade, final Item item,
                      final int pointsRequested) throws IllegalOperationException {
        requireTrade(repairer, trade);
        AbstractItem copy = asForgedCopy(item);
        int bonus = SkillCompetencyAbility.allFor(repairer).stream()
                .mapToInt(ability -> ability.resolveRepairHardnessBonus(repairer))
                .sum();
        return copy.repair(Math.max(0, pointsRequested) + bonus);
    }

    private void requireTrade(final Character character, final ProfissaoSpecialization trade)
            throws IllegalOperationException {
        CharacterSkill profissao = character.getSkills().get(SkillType.PROFISSAO);
        if (profissao == null || !profissao.getSpecializations().contains(trade)) {
            throw new IllegalOperationException(CRAFTING_TRADE_NOT_HELD);
        }
    }

    private AbstractItem asForgedCopy(final Item item) throws IllegalOperationException {
        if (!(item instanceof AbstractItem copy)) {
            throw new IllegalOperationException(CANNOT_MODIFY_TEMPLATE);
        }
        return copy;
    }

    private int profissaoGraduation(final Character character) {
        CharacterSkill profissao = character.getSkills().get(SkillType.PROFISSAO);
        return profissao == null ? 0 : profissao.getGraduation().getGraduationValue();
    }

    private static int atLeastOne(final int value) {
        return Math.max(1, value);
    }
}
