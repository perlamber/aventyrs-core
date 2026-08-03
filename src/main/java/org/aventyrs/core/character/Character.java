package org.aventyrs.core.character;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.ability.AcquiredChoice;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.services.FreeActionsService;
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;

@Builder(toBuilder = true) @Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Character {
    /**
     * A unique, stable identifier for this Character — independent of any specific
     * {@link org.aventyrs.core.sheet.CharacterSheet} wrapping it (see that class's own
     * {@code id}), e.g. so {@link org.aventyrs.core.scene.Scene} can tell participants apart
     * without relying on object-reference equality.
     */
    @Builder.Default
    protected UUID id = UUID.randomUUID();

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

    /** Trained Perícias, keyed by {@link SkillType} for O(1) lookup instead of filtering a list. */
    @NonNull
    @Singular
    protected Map<SkillType, CharacterSkill> skills;

    @NonNull
    @Singular
    protected List<AttributeAbility> attributeAbilities;

    /** Habilidades de Competência acquired from trained Perícias (e.g. ArtesCompetencyAbility). */
    @NonNull
    @Singular
    protected List<SkillCompetencyAbility> skillCompetencyAbilities;

    /**
     * Values chosen when acquiring an ability whose rules require picking one — e.g. which
     * Perícia {@code GnoseAbility.PERITO_TEORICO} applies to. The ability instance itself still lives
     * in {@link #attributeAbilities}/{@link #skillCompetencyAbilities} as normal — this is
     * purely the extra "what did they pick" data, looked up via
     * {@link org.aventyrs.core.character.services.AbilityChoiceService#getChoiceFor}.
     */
    @NonNull
    @Singular
    protected List<AcquiredChoice<?>> abilityChoices;

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

    /**
     * A temporary PA bonus (or malus), gained and spent much like temporary Ego points —
     * on top of the fixed {@link #actionPoints} counter and any
     * {@link org.aventyrs.core.modifier.ModifierType#ACTION_POINTS} ability/feat bonus.
     * Changing it produces a new {@code Character} via {@link #toBuilder()}, same as every
     * other field here; this project doesn't track session-based recovery for it.
     */
    @Builder.Default
    protected int temporaryActionPointsBonus = 0;

    @Builder.Default
    protected SizeCategory sizeCategory = SizeCategory.ZERO;

    @Builder.Default
    CharacterStatus status = CharacterStatus.CLEAN;

    /**
     * The character's own fixed Reação counter — what they have when no external influence
     * (abilities'/competencies'/excellencies' {@link org.aventyrs.core.modifier.ModifierType#REACTIONS}
     * bonus) applies. {@value ReactionsService#DEFAULT_REACTIONS} by default, lowered to 0 or
     * raised to 2 by some Talentos/Habilidades. See {@link ReactionsService#getTotalReactions}
     * for the fully-modified total.
     */
    @Builder.Default
    protected int reactions = ReactionsService.DEFAULT_REACTIONS;

    /**
     * The character's own fixed Ação Livre counter — what they have when no external
     * influence (abilities'/competencies'/excellencies'
     * {@link org.aventyrs.core.modifier.ModifierType#FREE_ACTIONS} bonus) applies.
     * {@value FreeActionsService#DEFAULT_FREE_ACTIONS} by default. Unlike {@link #reactions},
     * an Ação Livre may be spent on the character's own Turn. See
     * {@link FreeActionsService#getTotalFreeActions} for the fully-modified total.
     */
    @Builder.Default
    protected int freeActions = FreeActionsService.DEFAULT_FREE_ACTIONS;
}
