package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillType;

/**
 * Resolves how many combatants one attack may affect — the one target every attack has, plus
 * whatever extra ones the attacker's Talentos grant.
 *
 * <h2>How many, never which ones</h2>
 *
 * The rules that grant an extra target also constrain <em>which</em> combatants may be picked:
 * {@code ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA}'s "alvos adicionais precisam estar
 * adjacentes ao alvo primário", {@code CavalariaFeat#ATAQUE_EM_ARCO}'s "que estejam adjacentes
 * entre si". That constraint is pairwise geometry between two combatants who are both <em>not</em>
 * the roller, and this core does none: a {@code SceneContext} carries distances measured from its
 * own holder and nothing else, so it cannot answer "is B adjacent to C". Picking the targets is
 * therefore the caller's — in practice the UI's — job, and this service answers only the half that
 * is a property of the attacker: how many they are entitled to.
 *
 * <h2>Where the count comes from — one source today</h2>
 *
 * Every held {@code Feat}'s {@code Feat#resolveAdditionalTargets(SkillType, Character)}, summed on
 * top of {@link #BASE_TARGETS}. Talentos sit outside every {@code ModifierResolver} scan, so this
 * is an explicit pass over {@code Character#getFeats()}, the same shape {@code
 * AttackRangeServiceImpl}/{@code MovementServiceImpl} use. There is no {@code
 * SkillCompetencyAbility}/{@code AttributeAbility} counterpart yet — add one with its first real
 * consumer, not before.
 *
 * <p>Each Talento checks its own conditions inside the hook, so the answer already reflects what
 * the character is currently wielding: {@code ARTE_FLUIDA} contributes nothing while its holder
 * has a non-natural weapon drawn or a Escudo equipped.
 */
public interface AttackTargetingService {

    /** Every attack affects one target before any Talento widens it. */
    int BASE_TARGETS = 1;

    /**
     * The greatest number of targets attacker may declare for one attack made with attackSkill —
     * {@link #BASE_TARGETS} plus every held Talento's own grant. Never below {@link
     * #BASE_TARGETS}: a Talento returning a negative number cannot take away the target an attack
     * inherently has.
     *
     * <p>attackSkill is passed through so a grant can scope itself to one Perícia de Ataque
     * ({@code CavalariaFeat#ATAQUE_EM_ARCO} is Ataque Corpo-a-Corpo only). Nothing here rejects a
     * non-attack Perícia — the usual restraint; {@code org.aventyrs.core.combat.AttackDelivery}
     * is where a roll is refused for not being an attack.
     */
    int getMaximumTargets(Character attacker, SkillType attackSkill);
}
