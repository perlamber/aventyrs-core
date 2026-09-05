package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Improvement;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemMasterpiece;
import org.aventyrs.core.item.ItemRarity;
import org.aventyrs.core.item.ItemTemplate;
import org.aventyrs.core.item.ItemForgery;
import org.aventyrs.core.item.ItemSpecification;
import org.aventyrs.core.item.RegaliaDonation;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.profissao.ProfissaoSpecialization;


/**
 * Fabricar e reparar Equipamentos, from the <b>player's</b> side.
 *
 * <p>A GM forges any item in any circumstance straight through the unvalidated {@code
 * AbstractItem.builder()} / {@code ItemTemplate#forge()} path — or, for a complete copy handed
 * over free and marked as such, {@code ItemForgery.donatedByAventyr(specification)}, which
 * deliberately lives on {@code ItemForgery} rather than here: every number this service exists to
 * compute is exactly what a donation skips. See the "Builder-bypassable
 * invariants" convention. A <i>player</i> goes through this service, which prices the work in
 * Pontos de Equipamento, measures it in days/hours, resolves the Grau de Dificuldade from
 * Raridade, and enforces the player-side gates: the right Especialização de Profissão, the
 * minimum Graduação an Obra-Prima needs, and the Categoria de Peso Aprimoramento cap.
 *
 * <p><b>This core never rolls dice</b> (see CLAUDE.md). The rules make fabrication and reparo
 * depend on "rolagens bem-sucedidas" — this service does not roll them. The pure getters
 * ({@link #getFabricationCost}, {@link #getFabricationTimeInDays}, {@link
 * #getFabricationDifficulty}, {@link #assessRepair}, …) hand a caller everything it needs to
 * make the Perícia roll itself (via {@code ProfissaoInteraction} / {@code
 * ConhecimentosInteraction} against the GD returned here); the mutating methods ({@link #forge},
 * {@link #repair}, {@link #installImprovement}) then <b>assume the caller has already determined
 * success</b> and apply the result — the same caller-drives-it contract as {@code
 * DefeatBlessingService} ("after an attack it has determined was fatal") and {@code
 * SkillGraduationService#upgradeGraduation}.
 *
 * <p><b>Which Perícia.</b> The user's rules text names Conhecimentos for fabrication, but the
 * trade Especializações (Metalurgia, Alfaiataria, …) live on {@link ProfissaoSpecialization} and
 * the existing {@code ArtificeFeat}/{@code GoblinFeat} both call equipment-crafting a
 * <i>Profissão</i> roll — so fabrication is modeled as a Profissão (trade) roll. Reparo keeps
 * both: a Profissão (trade) roll for the labour plus the secondary Conhecimentos roll its text
 * names, and {@link RepairAssessment} carries the Conhecimentos GD.
 *
 * <p><b>The PE and time are reported, not deducted</b> — this core keeps no PE economy (the gap
 * {@code ResourcesAdvantage#BARGANHISTA} still cites), so affordability and the passage of days
 * are the caller's to track, exactly as {@code WeaponDrawService} reports Pontos de Ação without
 * a spent-this-Turn pool.
 */
public interface EquipmentCraftingService {

    // ---------------------------------------------------------------- Fabricação — pure figures

    /**
     * What fabricating template costs — "metade do valor referido", i.e. half its Preço in PE,
     * floored, never below 1.
     */
    int getFabricationCost(ItemTemplate template);

    /**
     * The raw Tempo de Produção — "metade de seu valor de compra (custo), medido em dias",
     * floored, never below 1. No Habilidade reduction applied; use {@link
     * #getFabricationTimeInDays(Character, ItemTemplate)} for a specific crafter.
     */
    int getFabricationTimeInDays(ItemTemplate template);

    /**
     * {@link #getFabricationTimeInDays(ItemTemplate)} scaled by every {@code
     * SkillCompetencyAbility#resolveProductionTimeMultiplier} crafter holds (so {@code
     * ProfissaoCompetencyAbility#CONSTRUTOR_EFICIENTE}'s -20% lands), floored, never below 1.
     */
    int getFabricationTimeInDays(Character crafter, ItemTemplate template);

    /**
     * The Grau de Dificuldade to fabricate template as an ordinary (non-Obra-Prima) item —
     * straight from {@code template.getRarity().getFabricationDifficulty()}.
     */
    DifficultyLevel getFabricationDifficulty(ItemTemplate template);

    /**
     * The Grau de Dificuldade to fabricate an Obra-Prima whose <i>own</i> Raridade is {@code
     * masterpieceRarity} — the base fabrication GD for that Raridade, one nível harder ("o Grau
     * de Dificuldade também aumenta, em +1 nível").
     */
    DifficultyLevel getMasterpieceFabricationDifficulty(ItemRarity masterpieceRarity);

    /**
     * The minimum Profissão Graduação to fabricate an Obra-Prima of Raridade {@code
     * masterpieceRarity} — 1 / 3 / 5 / 7 / 10 for Comum … Mítico.
     */
    int getMasterpieceMinimumGraduation(ItemRarity masterpieceRarity);

    // ------------------------------------------------------ Aprimoramentos de Obra-Prima — figures

    /**
     * The Grau de Dificuldade of the Conhecimentos roll to install an Aprimoramento of Raridade
     * {@code improvementRarity} — Comum→Difícil … Mítico→Milagre.
     */
    DifficultyLevel getImprovementInstallDifficulty(ItemRarity improvementRarity);

    /**
     * The cumulative Desvantagem on that Conhecimentos roll — a flat roll malus of {@code -2}
     * ({@code Skill#DISADVANTAGE_MALUS}) for <i>each</i> Aprimoramento item already carries
     * ("para cada outro Aprimoramento que o item já possua você recebe Desvantagem cumulativa").
     * The caller adds this to its roll; 0 for a bare Obra-Prima.
     */
    int getImprovementInstallDisadvantage(Item item);

    // ---------------------------------------------------- Regalia — os Talentos de Artífice, figures

    /**
     * The Grau de Dificuldade of the Profissão (ofício) roll to forge a Regalia of grade —
     * Inimaginável for {@link RegaliaGrade#MENOR}, Milagre for {@link RegaliaGrade#SUPERIOR}/{@link
     * RegaliaGrade#DIVINA}, straight from {@code grade.getCraftingDifficulty()}.
     *
     * <p>The rules add "não é possível reduzir GD desta rolagem com Habilidades, Vantagens ou
     * Efeitos, exceto Habilidades de Artífice". This core carries no GD-reduction source that
     * targets a crafting roll (fabrication/reparo never took one either) and no Artífice
     * Habilidade that would be exempt, so the carve-out is <b>currently exempt from nothing</b> —
     * the same "costs nothing to omit until the mechanism lands" reasoning CLAUDE.md applies to
     * the movement-exempt-from-Reações clause. It becomes real the day a Talento reduces a
     * crafting GD.
     */
    DifficultyLevel getRegaliaCraftingDifficulty(RegaliaGrade grade);

    /** The raw Tempo de Criação — 90 / 145 / 180 days — with no Habilidade de Artífice reduction. */
    int getRegaliaCraftingTimeInDays(RegaliaGrade grade);

    /**
     * {@link #getRegaliaCraftingTimeInDays(RegaliaGrade)} scaled by every {@code
     * SkillCompetencyAbility#resolveProductionTimeMultiplier} crafter holds — "Para critérios de
     * Forja de Equipamentos, as Habilidades dos Artífices Despertos que beneficiam Obras-Primas
     * também beneficiam Regalias" — floored, never below 1. The further "reduzida drasticamente em
     * uma Forja do Olho de Deus" is not modeled: this core has no places (see {@link
     * #forgeRegalia}).
     */
    int getRegaliaCraftingTimeInDays(Character crafter, RegaliaGrade grade);

    /**
     * Whether the crafting roll must land an <b>Acerto Crítico</b> outright ({@link
     * RegaliaGrade#DIVINA} only), not merely reach {@link #getRegaliaCraftingDifficulty}. Reported
     * for the caller to enforce — this core never rolls.
     */
    boolean regaliaCraftingRequiresCriticalResult(RegaliaGrade grade);

    // ------------------------------------------------------------------------ Reparo — pure figures

    /**
     * Everything a caller needs to repair item to pristine — the PE cost, the hours of work, the
     * Conhecimentos GD, and the cumulative Desvantagem — computed against the damage it currently
     * carries. An undamaged item returns an all-zero assessment.
     */
    RepairAssessment assessRepair(Item item);

    // ------------------------------------------------------------------------- Mutating — on success

    /**
     * Forges template into a fresh owned {@link Item} copy on crafter's behalf, assuming crafter
     * has already succeeded the Profissão roll against {@link #getFabricationDifficulty} (or
     * {@link #getMasterpieceFabricationDifficulty} when {@code masterpiece} is non-null).
     *
     * <p>Validates: crafter holds {@code trade} as a Profissão Especialização ({@code
     * CRAFTING_TRADE_NOT_HELD}); and, for an Obra-Prima, that their Profissão Graduação meets
     * {@link #getMasterpieceMinimumGraduation} for the masterpiece's Raridade ({@code
     * MASTERPIECE_GRADUATION_TOO_LOW}).
     *
     * <p>On success: builds the copy from the template, fits {@code masterpiece} when supplied,
     * scales its Dureza by every {@code
     * SkillCompetencyAbility#resolveProducedHardnessMultiplier} crafter holds ({@code
     * AUMENTAR_A_DUREZA}'s +50%), and stamps {@code producedByCharacterId}. The copy is
     * <b>returned, not equipped</b> — the caller adds it to inventory.
     *
     * @param masterpiece the Obra-Prima to fabricate it as, or {@code null} for an ordinary item
     */
    Item forge(Character crafter, ProfissaoSpecialization trade, ItemTemplate template,
               ItemMasterpiece masterpiece) throws IllegalOperationException;

    /**
     * Forges whatever specification describes on crafter's behalf — the general form, and the one
     * to prefer: a copy is more than its {@link ItemTemplate}, and an {@link ItemSpecification}
     * carries the Obra-Prima, the Aprimoramentos and (for a Regalia) the {@link RegaliaGrade} in
     * one value. The two shorter methods here are conveniences over it.
     *
     * <p><b>Delegates to {@link ItemForgery}</b>, which applies every gate — see its javadoc for
     * the full list and each failure's message. Pass {@code donation} only when the specification
     * names a Regalia; an ordinary item costs no Centelhas and ignores it.
     */
    Item forge(Character crafter, ProfissaoSpecialization trade, ItemSpecification specification,
               RegaliaDonation donation) throws IllegalOperationException;

    /**
     * Forges specification — which must name a {@link RegaliaGrade} — into a fresh Regalia copy on
     * crafter's behalf, the payoff of the {@code org.aventyrs.core.feat.ArtificeFeat} ladder. The
     * same call as {@link #forge(Character, ProfissaoSpecialization, ItemSpecification,
     * RegaliaDonation)}, named for what it makes; a specification with no grade is simply an
     * ordinary forge and is not refused here. Assumes
     * crafter has already succeeded the Profissão (ofício) roll against {@link
     * #getRegaliaCraftingDifficulty}, and — for {@link RegaliaGrade#DIVINA} — that it was an
     * Acerto Crítico ({@link #regaliaCraftingRequiresCriticalResult}); this core never rolls.
     *
     * <p><b>Delegates to {@link ItemForgery}</b>, which owns the whole Regalia forge — this is
     * the entry point for a caller already holding this service, nothing more. Validates: crafter
     * holds {@code trade} ({@code CRAFTING_TRADE_NOT_HELD}); some Talento crafter holds permits
     * grade <em>right now</em>, use-conditions included ({@code REGALIA_CRAFTING_NOT_PERMITTED} —
     * see {@code Feat#itsAllowedToCraftRegalia}, and note that possessing a Regalia is a condition
     * of <i>use</i>, never of acquiring the Talento); the Centelha {@code donation} is willing
     * ({@code REGALIA_DONOR_NOT_WILLING} — "Se o personagem doador não for voluntário … a criação
     * da Regalia irá falhar"); and, for {@link RegaliaGrade#DIVINA}, that the donor is a Dragão /
     * Elemental / Abissal / Celestial ({@code REGALIA_DIVINE_DONOR_REQUIRED}).
     *
     * <p>On success: builds the copy from {@code base}, marks it {@code
     * setRegaliaGrade(grade)}, scales its Dureza by the crafter's {@code
     * resolveProducedHardnessMultiplier}s and stamps {@code producedByCharacterId} (exactly as
     * {@link #forge}), and calls {@code Character#recordRegaliaCrafted(grade)} so the crafter's
     * "criação de 3 ou mais Regalias" history advances. The copy is <b>returned, not equipped</b>.
     *
     * <p><b>Not modeled</b>, and deliberately left to the caller/GM: the actual sacrifice of the
     * donor's Centelhas (this core does not track them — see {@link RegaliaGrade#requiresAllCentelhas()});
     * the "reduzida drasticamente em uma Forja do Olho de Deus" location bonus, and the Divina
     * "deve ser feita exclusivamente em uma Forja do Olho de Deus" restriction (this core models
     * no places); and the PE cost (no PE economy — the {@code ResourcesAdvantage#BARGANHISTA} gap).
     */
    Item forgeRegalia(Character crafter, ProfissaoSpecialization trade, ItemSpecification specification,
                      RegaliaDonation donation) throws IllegalOperationException;

    /**
     * Fits improvement to item on crafter's behalf, assuming the Conhecimentos roll against
     * {@link #getImprovementInstallDifficulty} (less {@link #getImprovementInstallDisadvantage})
     * already succeeded.
     *
     * <p>Validates: item is a real forged copy, not a template ({@code CANNOT_MODIFY_TEMPLATE});
     * it is an Obra-Prima ({@code ITEM_NOT_A_MASTERPIECE}); it has a free Aprimoramento slot for
     * its Categoria de Peso ({@code IMPROVEMENT_SLOTS_FULL}); and improvement is not one it
     * already carries ({@code DUPLICATE_IMPROVEMENT} — "Aprimoramentos diferentes"). The
     * per-material compatibility guards ({@code ItemImprovement} wrapper, defensive item,
     * ENCAIXE/CAMUFLADA/CAMADA_DE_REFORCO) are enforced by {@code AbstractItem#addImprovement}
     * beneath this.
     */
    void installImprovement(Character crafter, Item item, Improvement improvement) throws IllegalOperationException;

    /**
     * Repairs up to {@code pointsRequested} of item's lost Dureza on repairer's behalf, assuming
     * repairer has already succeeded both the Profissão (trade) roll and the Conhecimentos roll
     * the reparo demands. Validates repairer holds {@code trade} ({@code CRAFTING_TRADE_NOT_HELD})
     * and item is a real forged copy ({@code CANNOT_MODIFY_TEMPLATE}).
     *
     * <p>The Dureza actually recovered is {@code min(pointsRequested, damage carried)} plus every
     * {@code SkillCompetencyAbility#resolveRepairHardnessBonus} repairer holds ({@code
     * REPARO_MELHORADO}'s +2 / +5), still capped at the damage carried.
     *
     * @return the Dureza points actually recovered
     */
    int repair(Character repairer, ProfissaoSpecialization trade, Item item, int pointsRequested)
            throws IllegalOperationException;

    /**
     * The cost, time and difficulty of repairing one item to pristine.
     *
     * @param pointsRepairable  the damage the item currently carries — what a full repair restores
     * @param equipmentPointCost PE the repair costs: 10% of the item's Preço while under half its
     *                           PV are lost, a third of it once past half — floored, never below 1
     * @param workHours          hours of labour: 1 per lost PV under the half-PV mark, 2 per lost
     *                           PV past it
     * @param difficulty         the Conhecimentos GD, from the item's Raridade — the harder
     *                           Obra-Prima table when the copy is one
     * @param disadvantage       the cumulative roll malus, {@code -2} per fitted Aprimoramento
     * @param severelyDamaged    whether more than half the item's PV are gone (which tier the
     *                           cost and hours use)
     */
    record RepairAssessment(int pointsRepairable, int equipmentPointCost, int workHours,
                            DifficultyLevel difficulty, int disadvantage, boolean severelyDamaged) {

        /** The assessment for an item with no damage — nothing to do. */
        public static RepairAssessment none(final DifficultyLevel difficulty) {
            return new RepairAssessment(0, 0, 0, difficulty, 0, false);
        }
    }
}
