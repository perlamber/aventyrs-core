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
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.ego.AutocontroleAdvantage;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;

@Builder(toBuilder = true) @Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Character {
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
}
