package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.Race;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_ATTRIBUTE_POINT_ALLOCATION;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_EGO_POINT_ALLOCATION;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_RACIAL_BONUS_ALLOCATION;

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
            int racialBonus = fixedBonuses.getOrDefault(domain, 0) + chosenRacialBonusAllocation.getOrDefault(domain, 0);
            assignAttribute(builder, domain, AttributeValue.builder().base(base).racialBonus(racialBonus).build());
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
        switch (domain) {
            case VIGOR -> builder.vigor(value);
            case STRENGTH -> builder.strength(value);
            case DEXTERITY -> builder.dexterity(value);
            case FOCUS -> builder.focus(value);
            case INSTINCT -> builder.instinct(value);
            case GNOSE -> builder.gnose(value);
            case CHARISMA -> builder.charisma(value);
        }
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
    public boolean isAutocontroleAdvantageAvailable(final CharacterEgos egos) {
        return egos.getAutocontrole().getBase() >= AUTOCONTROLE_ADVANTAGE_MIN_BASE;
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
}
