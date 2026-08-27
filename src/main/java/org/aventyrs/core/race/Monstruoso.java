package org.aventyrs.core.race;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;

/**
 * The catch-all {@link Race} every foe built from a {@code
 * org.aventyrs.core.monster.MonsterTemplate} carries.
 *
 * <p>{@code Character#race} is required and is read for real — {@code
 * SkillCompetencyAbility#allFor} concatenates {@link #getRacialAbilities()} into every Perícia
 * roll — so a monster needs one. It doesn't need a <i>distinct</i> one per family, though:
 * unlike a player race, where the race is the character's heritage and carries its innate
 * traits, a monster's traits are authored per stat block. So this stays deliberately empty and
 * every foe shares it, with per-monster abilities supplied through the template's own ability
 * lists (which land on {@code Character#attributeAbilities}/{@code #skillCompetencyAbilities}
 * and are scanned identically).
 *
 * <p>{@link CreatureType#MONSTRUOSO} already existed — it's what a Mestiço race validates a
 * chosen parent against — so this needed no new classification.
 *
 * <p>Everything else on {@link Race} keeps its default, including {@code getNewFeatCost}: a
 * monster never spends XP (see {@code org.aventyrs.core.sheet.CombatantSheet} for why it
 * can't), so that cost is unreachable for one.
 */
public class Monstruoso implements Race {

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTRUOSO;
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }
}
