package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillExcellency;

public interface MovementService {

    /**
     * Movimento Base in UD (Unidades de Distância) — how far this character travels for
     * <b>each</b> Ponto de Ação spent moving, <b>not</b> a whole-Turn allowance. It is {@link
     * SizeCategory#getMovementPerActionPoint()} (via {@link CharacterSizeService
     * #getEffectiveSizeCategory}, so a size-shifting ability like Sangue de Gigante is already
     * reflected) plus any {@link org.aventyrs.core.modifier.ModifierType#MOVEMENT} bonus found
     * on attributeAbilities, skillCompetencyAbilities (acquired <b>and</b> racial — see {@link
     * org.aventyrs.core.skill.SkillCompetencyAbility#allFor}; unlike {@link ReactionsService}/
     * {@link InitiativeService}, which predate that fix and still only scan the acquired list,
     * this newer service starts from the corrected combined one), or the unlocked {@link
     * SkillExcellency} tiers of every trained Perícia. Never negative — like Reações/Ações
     * Livres/RD/RA, this is a spendable-resource-like budget, not a signed comparative value
     * like Iniciativa.
     *
     * <p><b>This service deliberately does not multiply by {@link ActionPointsService
     * #getMaxActionPoints}.</b> A character's Pontos de Ação are spent across moving
     * <em>and</em> everything else they do that Turn, and how many go to each is the player's
     * choice at the table — a decision this core has no way to make and no reason to
     * pre-empt. A caller that wants the distance covered by a specific number of Pontos de
     * Ação multiplies this figure by that number itself; one that wants the theoretical
     * maximum multiplies by {@code getMaxActionPoints} for the Turn in question. Neither
     * belongs here, which is also why this takes no {@code turnNumber}: nothing about
     * Movimento Base varies by Turn.
     *
     * <p>This is the <em>permanent</em> total only. A caller wanting what's actually available
     * this Round also adds {@link CombatantSheet#getTemporaryBonus}({@code
     * ModifierType.MOVEMENT}) on top — the same combination {@code AbstractSkillInteraction}
     * already performs for {@code skillRollBonus} — since a Round-scoped grant (e.g. {@code
     * InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}'s +2UD while its holder's group holds
     * initiative) lives on the {@code CombatantSheet}, not the {@code Character}, the same
     * "permanent ability vs. granted-by-someone-else's-action" split every other stat here
     * already draws. Being a per-Ponto-de-Ação figure, such a grant is worth +2UD on
     * <em>every</em> Ponto de Ação spent moving, which is exactly what "seu Movimento Base
     * aumenta em +2UD" says.
     */
    int getMovementBase(Character character);
}
