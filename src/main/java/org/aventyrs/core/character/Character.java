package org.aventyrs.core.character;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interactable;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.Player;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;

@Builder(toBuilder = true) @Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Character implements Interactable {
    @NonNull
    protected Player player;

    @NonNull
    protected String name;

    @NonNull
    protected Race race;

    @NonNull
    protected CharacterAttributes attributes;

    @NonNull
    protected CharacterEgos egos;

    /**
     * The Vantagem de Autocontrole chosen at creation, or {@code null} if the character
     * either wasn't eligible (see
     * {@link org.aventyrs.core.character.services.CharacterCreationService#isAutocontroleAdvantageAvailable})
     * or chose not to pick one.
     */
    protected AutocontroleAdvantage autocontroleAdvantage;

    @NonNull
    @Singular
    protected List<CharacterSkill> skills;

    @NonNull
    @Singular
    protected List<AttributeAbility> attributeAbilities;

    @NonNull
    protected ActionProfile actionProfile;

    /**
     * The character's own fixed Pontos de Ação (PA) counter — what they have when no
     * external influence (abilities/feats' {@link org.aventyrs.core.modifier.ModifierType#ACTION_POINTS}
     * bonus, or the {@link ActionProfile}'s Turn adjustment) applies. Permanent resources
     * that grant or remove PA change this value directly, as opposed to the Turn-scoped
     * adjustments {@link ActionPointsService} layers on top of it.
     */
    @Builder.Default
    protected int actionPoints = ActionPointsService.DEFAULT_ACTION_POINTS;

    @Builder.Default
    protected SizeCategory sizeCategory = SizeCategory.ZERO;

    @Builder.Default
    CharacterStatus status = CharacterStatus.CLEAN;

    //TODO implement
    @Override
    public CharacterStatus receiveInteraction(Interaction interaction) {
        return null;
    }

    //TODO implement
    @Override
    public CharacterStatus receiveInteraction() {
        return null;
    }



}
