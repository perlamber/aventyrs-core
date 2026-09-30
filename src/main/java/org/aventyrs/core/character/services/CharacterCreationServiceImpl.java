package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.background.AcquiredBackground;
import org.aventyrs.core.background.Background;
import org.aventyrs.core.background.GraduationGrant;
import org.aventyrs.core.background.TraitGrant;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatChoice;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.feat.FeatCatalog;
import org.aventyrs.core.feat.StartingFeatSlot;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.FEAT_REQUIRES_CHOICE;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_ATTRIBUTE_POINT_ALLOCATION;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_BACKGROUND_SELECTION;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_DEFECT_SELECTION;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_EGO_POINT_ALLOCATION;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_RACIAL_BONUS_ALLOCATION;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_STARTING_FEAT_SELECTION;

public class CharacterCreationServiceImpl implements CharacterCreationService {

    @Override
    public CharacterAttributes allocateAttributes(final Race race,
                                                    final Map<AttributeDomain, Integer> basePointAllocation,
                                                    final Map<AttributeDomain, Integer> chosenRacialBonusAllocation) throws IllegalOperationException {
        validateBasePointAllocation(basePointAllocation);
        validateRacialBonusAllocation(race, chosenRacialBonusAllocation);

        final Map<AttributeDomain, Integer> fixedBonuses = race.getFixedAttributeBonuses();
        final CharacterAttributes.CharacterAttributesBuilder builder = CharacterAttributes.builder();
        for (AttributeDomain domain : AttributeDomain.values()) {
            int base = 1 + basePointAllocation.getOrDefault(domain, 0);
            // Kept apart rather than summed: which half the race dictated and which half its
            // player directed is information nothing downstream can recover from a total, and
            // racial-trait suppression needs it. AttributeValue#getRacialBonus() still reports
            // the sum for every reader that only wants "how much of this is racial".
            assignAttribute(builder, domain, AttributeValue.builder().domain(domain).base(base)
                    .fixedRacialBonus(fixedBonuses.getOrDefault(domain, 0))
                    .chosenRacialBonus(chosenRacialBonusAllocation.getOrDefault(domain, 0))
                    .build());
        }
        return builder.build();
    }

    private void validateBasePointAllocation(final Map<AttributeDomain, Integer> basePointAllocation) throws IllegalOperationException {
        int totalAssigned = 0;
        for (AttributeDomain domain : AttributeDomain.values()) {
            int assigned = basePointAllocation.getOrDefault(domain, 0);
            if (assigned < 0 || assigned > MAX_STARTING_ATTRIBUTE_BASE - 1) {
                throw new IllegalOperationException(INVALID_ATTRIBUTE_POINT_ALLOCATION);
            }
            totalAssigned += assigned;
        }
        if (totalAssigned != STARTING_ATTRIBUTE_POINTS) {
            throw new IllegalOperationException(INVALID_ATTRIBUTE_POINT_ALLOCATION);
        }
    }

    private void validateRacialBonusAllocation(final Race race, final Map<AttributeDomain, Integer> chosenRacialBonusAllocation) throws IllegalOperationException {
        int totalAssigned = 0;
        for (Map.Entry<AttributeDomain, Integer> entry : chosenRacialBonusAllocation.entrySet()) {
            if (!race.getChoosableAttributes().contains(entry.getKey()) || entry.getValue() < 0) {
                throw new IllegalOperationException(INVALID_RACIAL_BONUS_ALLOCATION);
            }
            totalAssigned += entry.getValue();
        }
        if (totalAssigned != race.getChoosableAttributeBonusPoints()) {
            throw new IllegalOperationException(INVALID_RACIAL_BONUS_ALLOCATION);
        }
    }

    private void assignAttribute(final CharacterAttributes.CharacterAttributesBuilder builder, final AttributeDomain domain, final AttributeValue value) {
        CharacterAttributes.assign(builder, domain, value);
    }

    @Override
    public CharacterEgos allocateEgos(final Map<EgoDomain, Integer> extraPointAllocation) throws IllegalOperationException {
        validateEgoPointAllocation(extraPointAllocation);

        final CharacterEgos.CharacterEgosBuilder builder = CharacterEgos.builder();
        for (EgoDomain domain : EgoDomain.values()) {
            int base = STARTING_EGO_POINTS + extraPointAllocation.getOrDefault(domain, 0);
            assignEgo(builder, domain, EgoValue.builder().base(base).build());
        }
        return builder.build();
    }

    @Override
    public boolean isEgoAdvantageAvailable(final EgoDomain domain, final CharacterEgos egos) {
        return egos.getEgo(domain).getBase() >= EGO_ADVANTAGE_MIN_BASE;
    }

    private void validateEgoPointAllocation(final Map<EgoDomain, Integer> extraPointAllocation) throws IllegalOperationException {
        int totalAssigned = 0;
        for (EgoDomain domain : EgoDomain.values()) {
            int assigned = extraPointAllocation.getOrDefault(domain, 0);
            if (assigned < 0) {
                throw new IllegalOperationException(INVALID_EGO_POINT_ALLOCATION);
            }
            totalAssigned += assigned;
        }
        if (totalAssigned != EXTRA_EGO_POINTS) {
            throw new IllegalOperationException(INVALID_EGO_POINT_ALLOCATION);
        }
    }

    private void assignEgo(final CharacterEgos.CharacterEgosBuilder builder, final EgoDomain domain, final EgoValue value) {
        switch (domain) {
            case AUTOCONTROLE -> builder.autocontrole(value);
            case RECURSOS -> builder.recursos(value);
            case SORTE -> builder.sorte(value);
            case INICIATIVA -> builder.iniciativa(value);
        }
    }

    @Override
    public List<StartingFeatSlot> getStartingFeatSlots(final Race race) {
        final List<StartingFeatSlot> slots = new ArrayList<>(
                Collections.nCopies(DEFAULT_GENERAL_FEAT_SLOTS, StartingFeatSlot.defaultGeneral()));
        slots.addAll(race.getStartingFeatSlots());
        return List.copyOf(slots);
    }

    @Override
    public List<Feat> getStartingFeatOptions(final Character character, final StartingFeatSlot slot, final CharacterSheet sheet) {
        return FeatCatalog.all().stream()
                .filter(feat -> character.getFeats().stream().noneMatch(held -> held.catalogEntry() == feat))
                .filter(feat -> slot.accepts(feat, character, sheet))
                .toList();
    }

    @Override
    public void grantStartingFeats(final Character character, final List<Feat> picks, final CharacterSheet sheet) throws IllegalOperationException {
        final List<StartingFeatSlot> slots = getStartingFeatSlots(character);
        if (picks.size() != slots.size()) {
            throw new IllegalOperationException(INVALID_STARTING_FEAT_SELECTION);
        }
        for (int i = 0; i < slots.size(); i++) {
            final Feat pick = picks.get(i);
            if (!getStartingFeatOptions(character, slots.get(i), sheet).contains(pick.catalogEntry())) {
                throw new IllegalOperationException(INVALID_STARTING_FEAT_SELECTION);
            }
            // Same guard as FeatServiceImpl#grantFeat: a bare choice-carrying constant does nothing.
            if (pick == pick.catalogEntry() && !pick.resolveRequiredChoices(character).isEmpty()) {
                throw new IllegalOperationException(FEAT_REQUIRES_CHOICE);
            }
            FeatServiceImpl.acquire(character, pick);
        }
    }

    // ---- Antecedentes ---------------------------------------------------------------------------

    @Override
    public List<TraitGrant> getBackgroundTraitGrants(final Character character, final Background background,
                                                     final List<SkillType> graduationSkills) throws IllegalOperationException {
        List<SkillType> resolved = resolveGraduationSkills(background, graduationSkills);
        return background.resolveTraitGrants(withGraduations(character, resolved), Set.copyOf(resolved));
    }

    @Override
    public Character applyBackground(final Character character, final AcquiredBackground selection) throws IllegalOperationException {
        Background background = selection.background();
        if (character.getBackground(background.getKind()).isPresent()) {
            throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
        }
        List<SkillType> graduationSkills = resolveGraduationSkills(background, selection.graduationSkills());
        Character trained = withGraduations(character, graduationSkills);
        List<SkillTrait> traits = resolveTraits(background.resolveTraitGrants(trained, Set.copyOf(graduationSkills)),
                selection.traits());
        validateBenefitChoices(background.resolveBenefitChoices(character), selection.benefitChoices());

        Map<SkillType, CharacterSkill> skills = new EnumMap<>(SkillType.class);
        skills.putAll(trained.getSkills());
        List<SkillCompetencyAbility> competencies = new ArrayList<>(trained.getSkillCompetencyAbilities());
        for (SkillTrait trait : traits) {
            if (trait instanceof SkillSpecialization specialization) {
                CharacterSkill skill = skills.get(specialization.getSkillType());
                List<SkillSpecialization> held = new ArrayList<>(skill.getSpecializations());
                held.add(specialization);
                skills.put(specialization.getSkillType(), skill.toBuilder().specializations(List.copyOf(held)).build());
            } else {
                competencies.add((SkillCompetencyAbility) trait);
            }
        }
        CharacterEgos egos = trained.getEgos();
        for (Map.Entry<EgoDomain, Integer> bonus : background.getEgoBonuses().entrySet()) {
            egos = egos.withBaseBonus(bonus.getKey(), bonus.getValue());
        }
        return trained.toBuilder()
                .clearSkills().skills(skills)
                .clearSkillCompetencyAbilities().skillCompetencyAbilities(competencies)
                .egos(egos)
                .background(new AcquiredBackground(background, graduationSkills, traits, selection.benefitChoices()))
                .build();
    }

    /**
     * Every Perícia background's "+1 Graduação" lands on: the fixed ones, plus exactly the picked
     * ones for each choice. submitted may name the fixed ones or leave them out; anything else in it
     * is refused, as is a repeated Perícia.
     */
    private static List<SkillType> resolveGraduationSkills(final Background background, final List<SkillType> submitted) {
        List<SkillType> requested = submitted == null ? List.of() : submitted;
        if (new HashSet<>(requested).size() != requested.size()) {
            throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
        }
        Set<SkillType> remaining = new LinkedHashSet<>(requested);
        List<SkillType> resolved = new ArrayList<>();
        for (GraduationGrant grant : background.getGraduationGrants()) {
            if (!grant.isChoice()) {
                resolved.addAll(grant.options());
                grant.options().forEach(remaining::remove);
                continue;
            }
            List<SkillType> chosen = remaining.stream().filter(grant.options()::contains).toList();
            if (chosen.size() != grant.picks()) {
                throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
            }
            resolved.addAll(chosen);
            chosen.forEach(remaining::remove);
        }
        if (!remaining.isEmpty()) {
            throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
        }
        return List.copyOf(resolved);
    }

    /** The same resolution for traits: fixed grants filled in, every choice answered exactly. */
    private static List<SkillTrait> resolveTraits(final List<TraitGrant> grants, final List<SkillTrait> submitted) {
        if (new HashSet<>(submitted).size() != submitted.size()) {
            throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
        }
        Set<SkillTrait> remaining = new LinkedHashSet<>(submitted);
        List<SkillTrait> resolved = new ArrayList<>();
        for (TraitGrant grant : grants) {
            if (!grant.isChoice()) {
                resolved.addAll(grant.options());
                grant.options().forEach(remaining::remove);
                continue;
            }
            List<SkillTrait> chosen = remaining.stream().filter(grant.options()::contains).toList();
            if (chosen.size() != grant.picks()) {
                throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
            }
            resolved.addAll(chosen);
            chosen.forEach(remaining::remove);
        }
        if (!remaining.isEmpty()) {
            throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
        }
        return List.copyOf(resolved);
    }

    /** Each choice answered in order, exactly its picks, each from its options and none repeated. */
    private static void validateBenefitChoices(final List<FeatChoice<?>> choices, final List<Object> answers) {
        int next = 0;
        for (FeatChoice<?> choice : choices) {
            if (next + choice.picks() > answers.size()) {
                throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
            }
            List<Object> picked = answers.subList(next, next + choice.picks());
            if (new HashSet<>(picked).size() != picked.size() || !choice.options().containsAll(picked)) {
                throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
            }
            next += choice.picks();
        }
        if (next != answers.size()) {
            throw new IllegalOperationException(INVALID_BACKGROUND_SELECTION);
        }
    }

    /** A copy of character with +1 Graduação in each of skills — an untrained one becomes trained at 1. */
    private static Character withGraduations(final Character character, final List<SkillType> skills) {
        Map<SkillType, CharacterSkill> raised = new EnumMap<>(SkillType.class);
        raised.putAll(character.getSkills());
        for (SkillType skillType : skills) {
            CharacterSkill current = raised.get(skillType);
            int graduation = current == null ? 0 : current.getGraduation().getGraduationValue();
            raised.put(skillType, CharacterSkill.builder()
                    .skill(current == null ? skillType.newSkillInstance() : current.getSkill())
                    .specializations(current == null ? List.of() : current.getSpecializations())
                    .graduation(SkillGraduation.builder().graduationValue(graduation + 1).build())
                    .build());
        }
        return character.toBuilder().clearSkills().skills(raised).build();
    }

    // ---- Defeitos e Qualidades ---------------------------------------------------------------------

    @Override
    public List<StartingFeatSlot> getStartingFeatSlots(final Character character) {
        List<StartingFeatSlot> slots = new ArrayList<>(getStartingFeatSlots(character.getRace()));
        int traded = character.getQualities().stream()
                .filter(held -> held.source() == org.aventyrs.core.defect.QualitySource.GENERAL_FEAT_TRADE)
                .mapToInt(held -> held.qualityClass().getGeneralFeatSlotsTraded())
                .sum();
        for (int i = slots.size() - 1; i >= 0 && traded > 0; i--) {
            if (slots.get(i).isGeneralOnly()) {
                slots.remove(i);
                traded--;
            }
        }
        for (org.aventyrs.core.defect.HeldDefect held : character.getDefects()) {
            if (held.superacao() != null && held.superacao().getFeatSlot() != null) {
                slots.add(StartingFeatSlot.defect(
                        held.superacao().getFeatSlot() == org.aventyrs.core.defect.SuperacaoBenefit.FeatSlot.QUALQUER));
            }
        }
        return List.copyOf(slots);
    }

    @Override
    public Character applyDefectsAndQualities(final Character character,
                                              final List<org.aventyrs.core.defect.HeldDefect> defects,
                                              final List<org.aventyrs.core.defect.HeldQuality> qualities) throws IllegalOperationException {
        if (!character.getDefects().isEmpty() || !character.getQualities().isEmpty()) {
            throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
        }
        List<org.aventyrs.core.defect.HeldDefect> heldDefects = defects == null ? List.of() : defects;
        List<org.aventyrs.core.defect.HeldQuality> heldQualities = qualities == null ? List.of() : qualities;
        requireDistinct(heldDefects.stream().map(org.aventyrs.core.defect.HeldDefect::severity).toList());
        requireDistinct(heldDefects.stream().map(org.aventyrs.core.defect.HeldDefect::defect).toList());
        requireDistinct(heldQualities.stream().map(org.aventyrs.core.defect.HeldQuality::quality).toList());
        if (heldQualities.size() > MAX_QUALITIES) {
            throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
        }

        Character working = character;
        List<org.aventyrs.core.defect.HeldDefect> recorded = new ArrayList<>();
        int menorOwed = 0;
        int maiorOwed = 0;
        for (org.aventyrs.core.defect.HeldDefect held : heldDefects) {
            validateChoices(held.defect().resolveChoices(held.severity(), character, held.choices()), held.choices());
            org.aventyrs.core.defect.SuperacaoBenefit benefit = held.superacao();
            if (benefit == null || benefit.getSeverity() != held.severity()) {
                throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
            }
            menorOwed += benefit.getMenorQualities();
            maiorOwed += benefit.getMaiorQualities();
            working = applySuperacaoPick(working, benefit, held.superacaoPicks());
            recorded.add(new org.aventyrs.core.defect.HeldDefect(held.defect(), held.severity(), held.choices(), true,
                    benefit, held.superacaoPicks()));
        }

        int menorFromSuperacao = 0;
        int maiorFromSuperacao = 0;
        int traded = 0;
        for (org.aventyrs.core.defect.HeldQuality held : heldQualities) {
            if (heldDefects.stream().anyMatch(defect -> defect.defect() == held.quality().getOpposes())) {
                throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
            }
            validateChoices(held.quality().resolveChoices(held.qualityClass(), character, held.choices()), held.choices());
            if (held.source() == org.aventyrs.core.defect.QualitySource.SUPERACAO) {
                if (held.qualityClass() == org.aventyrs.core.defect.QualityClass.MAIOR) {
                    maiorFromSuperacao++;
                } else {
                    menorFromSuperacao++;
                }
            } else {
                traded += held.qualityClass().getGeneralFeatSlotsTraded();
            }
            working = applyQualityGrant(working, held);
        }
        if (menorFromSuperacao != menorOwed || maiorFromSuperacao != maiorOwed) {
            throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
        }
        // No Qualidade without a Defeito (a table ruling), and never more General slots than there are.
        if (traded > 0 && (heldDefects.isEmpty() || traded > getTradableGeneralFeatSlots(character.getRace()))) {
            throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
        }
        return working.toBuilder().defects(recorded).qualities(heldQualities).build();
    }

    private static void requireDistinct(final List<?> values) {
        if (new HashSet<>(values).size() != values.size()) {
            throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
        }
    }

    /**
     * answers against choices, in order: each choice's picks from its options, none repeated — a
     * free-text choice ({@code Defect#FREE_TEXT}, no options) takes any non-blank String. Shared with
     * {@link DefectServiceImpl}, which validates a Defeito imposed during play the same way.
     */
    static void validateChoices(final List<FeatChoice<?>> choices, final List<Object> answers) {
        int next = 0;
        for (FeatChoice<?> choice : choices) {
            if (next + choice.picks() > answers.size()) {
                throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
            }
            List<Object> picked = answers.subList(next, next + choice.picks());
            boolean freeText = choice.options().isEmpty() && choice.type() == String.class;
            boolean valid = freeText
                    ? picked.stream().allMatch(answer -> answer instanceof String text && !text.isBlank())
                    : new HashSet<>(picked).size() == picked.size() && choice.options().containsAll(picked);
            if (!valid) {
                throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
            }
            next += choice.picks();
        }
        if (next != answers.size()) {
            throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
        }
    }

    /** The Superação's own pick, applied — see {@link org.aventyrs.core.defect.SuperacaoBenefit.Pick}. */
    private static Character applySuperacaoPick(final Character character,
                                                final org.aventyrs.core.defect.SuperacaoBenefit benefit,
                                                final List<Object> picks) {
        org.aventyrs.core.defect.SuperacaoBenefit.Pick pick = benefit.getPick();
        if (pick == org.aventyrs.core.defect.SuperacaoBenefit.Pick.NONE) {
            if (!picks.isEmpty()) {
                throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
            }
            return character;
        }
        if (picks.size() != 1) {
            throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
        }
        Object value = picks.get(0);
        boolean trained = value instanceof SkillType skill && graduationOf(character, skill) >= 1;
        switch (pick) {
            case UNTRAINED_SKILL -> {
                if (!(value instanceof SkillType skill) || trained) {
                    throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
                }
                return withGraduations(character, List.of(skill));
            }
            case TRAINED_SKILL -> {
                if (!trained) {
                    throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
                }
                return withGraduations(character, List.of((SkillType) value));
            }
            case ANY_SKILL -> {
                if (!(value instanceof SkillType skill)) {
                    throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
                }
                return withGraduations(character, List.of(skill));
            }
            case EGO_ADVANTAGE -> {
                if (!(value instanceof org.aventyrs.core.ego.EgoAdvantage advantage)
                        || character.getEgoAdvantages().containsKey(advantage.getEgoDomain())) {
                    throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
                }
                return character.toBuilder().egoAdvantage(advantage.getEgoDomain(), advantage).build();
            }
            case COMPETENCY_ABILITY -> {
                if (!(value instanceof SkillCompetencyAbility ability)
                        || graduationOf(character, ability.getSkillType()) < 1
                        || !org.aventyrs.core.skill.SkillTraitCatalog.competencyAbilitiesOf(ability.getSkillType()).contains(ability)
                        || SkillCompetencyAbility.allFor(character).contains(ability)) {
                    throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
                }
                return character.toBuilder().skillCompetencyAbility(ability).build();
            }
            default -> throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
        }
    }

    /**
     * A Qualidade's creation grant: Privilegiado's "1 Ponto permanente de Recursos" and Precognição's "1
     * ponto permanente de Iniciativa + uma Especialização ou Habilidade de Competência de Atenção" — Ego
     * base, as the Antecedentes' Ego points are. An Especialização needs Atenção trained to sit on.
     */
    private static Character applyQualityGrant(final Character character, final org.aventyrs.core.defect.HeldQuality held) {
        if (held.qualityClass() != org.aventyrs.core.defect.QualityClass.MAIOR) {
            return character;
        }
        return switch (held.quality()) {
            case DESTINADO_A_FORTUNA -> character.toBuilder()
                    .egos(character.getEgos().withBaseBonus(EgoDomain.RECURSOS, 1)).build();
            case SEXTO_SENTIDO -> {
                Character.CharacterBuilder builder = character.toBuilder()
                        .egos(character.getEgos().withBaseBonus(EgoDomain.INICIATIVA, 1));
                SkillTrait trait = held.choice(SkillTrait.class).orElseThrow(
                        () -> new IllegalOperationException(INVALID_DEFECT_SELECTION));
                if (trait instanceof SkillSpecialization specialization) {
                    CharacterSkill atencao = character.getSkills().get(SkillType.ATTENTION);
                    if (atencao == null || atencao.getGraduation().getGraduationValue() < 1) {
                        throw new IllegalOperationException(INVALID_DEFECT_SELECTION);
                    }
                    List<SkillSpecialization> specializations = new ArrayList<>(atencao.getSpecializations());
                    specializations.add(specialization);
                    Map<SkillType, CharacterSkill> skills = new EnumMap<>(SkillType.class);
                    skills.putAll(character.getSkills());
                    skills.put(SkillType.ATTENTION, atencao.toBuilder().specializations(List.copyOf(specializations)).build());
                    builder.clearSkills().skills(skills);
                } else {
                    builder.skillCompetencyAbility((SkillCompetencyAbility) trait);
                }
                yield builder.build();
            }
            default -> character;
        };
    }

    private static int graduationOf(final Character character, final SkillType skillType) {
        CharacterSkill skill = character.getSkills().get(skillType);
        return skill == null ? 0 : skill.getGraduation().getGraduationValue();
    }

    @Override
    public int grantStartingEquipmentPoints(@lombok.NonNull final CharacterSheet sheet) {
        return sheet.grantEquipmentPoints(socialClassOf(sheet.getCharacter()).getStartingEquipmentPoints());
    }

    @Override
    public java.util.Optional<org.aventyrs.core.item.ItemStore> getStartingStore(@lombok.NonNull final Character character) {
        return socialClassOf(character).getStartingRarity().map(org.aventyrs.core.item.ItemStore::new);
    }

    /** At creation nothing is spent yet, so the Recursos total (capped at 5) is the permanent points left. */
    private static org.aventyrs.core.ego.SocialClass socialClassOf(final Character character) {
        return org.aventyrs.core.ego.SocialClass.of(character.getEffectiveEgoTotal(EgoDomain.RECURSOS));
    }
}
