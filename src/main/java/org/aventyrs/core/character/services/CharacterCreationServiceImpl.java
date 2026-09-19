package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCatalog;
import org.aventyrs.core.feat.StartingFeatSlot;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.FEAT_REQUIRES_CHOICE;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_ATTRIBUTE_POINT_ALLOCATION;
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
        final List<StartingFeatSlot> slots = getStartingFeatSlots(character.getRace());
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
}
