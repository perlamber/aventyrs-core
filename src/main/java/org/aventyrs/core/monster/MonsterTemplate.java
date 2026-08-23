package org.aventyrs.core.monster;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.race.Monstruoso;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A foe's stat block, and the thing that turns it into a playable {@link MonsterSheet}.
 *
 * <p>Two ways to get one, mirroring the {@code Item}/{@code AbstractItem}/{@code ArmorItem} split
 * this codebase already uses for catalogs:
 *
 * <ul>
 *   <li>{@link AbstractMonsterTemplate} — <b>fill in the form</b> for a unique monster. Its
 *   builder <i>is</i> the form: name it, set its Attributes, give it Perícias, done.</li>
 *   <li>{@link GenericMonster} — <b>a generic monster on-scene</b>, when a Narrador needs an
 *   opponent right now and not a designed one. Ready archetypes at a few threat tiers.</li>
 * </ul>
 *
 * <h2>Why the numbers are authored rather than derived</h2>
 *
 * A foe never rolls, so its Defesas and the Grau de Dificuldade its attacks present are fixed
 * values — and they're written on the stat block, not recomputed from its Destreza and
 * Graduações the way a player's defence roll is. That keeps a stat block readable and tunable by
 * hand, at the cost of the numbers being free to drift from the foe's Attributes; nothing here
 * checks them against each other, deliberately.
 *
 * <h2>Uncapped by construction</h2>
 *
 * {@link #getAttributeBases()} and {@link #getSkillGraduations()} are subject to no ceiling. That
 * needs no special mechanism: this core's caps have only ever been enforced on the XP-spending
 * services ({@code CharacterAttributeService#upgradeBase}, {@code
 * SkillGraduationService#upgradeGraduation}), never on construction. And a monster can't reach
 * those services at all, since they take a {@code CharacterSheet} — see {@code
 * org.aventyrs.core.sheet.CombatantSheet}.
 */
public interface MonsterTemplate {

    /** The single shared {@code Race} every foe carries — see {@link Monstruoso}. */
    Monstruoso MONSTER_RACE = new Monstruoso();

    /** The {@code ActionProfile} a foe uses unless its stat block says otherwise. */
    ActionProfile DEFAULT_ACTION_PROFILE = ActionProfile.REFLEXOS_RAPIDOS;

    String getName();

    /** Attribute bases, uncapped. Any domain omitted keeps {@code AttributeValue}'s own default. */
    Map<AttributeDomain, Integer> getAttributeBases();

    /** Trained Perícias and their Graduações, uncapped. */
    Map<SkillType, Integer> getSkillGraduations();

    List<AttributeAbility> getAttributeAbilities();

    List<SkillCompetencyAbility> getSkillCompetencyAbilities();

    /** What it's wearing or wielding — feeds RD, DF/DM columns and Favores exactly as a player's does. */
    List<Item> getEquipment();

    SizeCategory getSizeCategory();

    /** Its DF: the target number a player's Ataque roll must reach to land a physical attack. */
    int getPhysicalDefense();

    /** Its DM: the same, for a magical attack. */
    int getMagicDefense();

    /** The GD its own attacks present to a defender's Esquiva e Aparar roll. */
    DifficultyLevel getAttackDifficulty();

    /** A flat modifier on top of {@link #getAttackDifficulty()}'s threshold. */
    int getAttackBonus();

    default int getLifeMultiplier() {
        return HitPointsService.DEFAULT_LIFE_MULTIPLIER;
    }

    default int getManaMultiplier() {
        return MagicPointsService.DEFAULT_MANA_MULTIPLIER;
    }

    default int getDeterminationMultiplier() {
        return DeterminationPointsService.DEFAULT_DETERMINATION_MULTIPLIER;
    }

    /**
     * Builds a fresh, independent foe from this stat block — a new {@link Character} wrapped in a
     * new {@link MonsterSheet}, with its own identity, its own resource pools and its own
     * mutable state. Spawning the same template twice gives two foes that can be damaged
     * separately.
     *
     * <p>That independence needs care in one place: {@code SkillGraduation} is mutable and
     * {@code CharacterSkill#increaseGraduation} mutates it in place, so each spawn builds its own
     * {@code SkillGraduation} instances rather than sharing the template's. The equipment and
     * ability lists are safe to share — those are catalog constants.
     */
    default MonsterSheet spawn() {
        Map<SkillType, CharacterSkill> skills = new EnumMap<>(SkillType.class);
        for (Map.Entry<SkillType, Integer> entry : getSkillGraduations().entrySet()) {
            skills.put(entry.getKey(), CharacterSkill.builder()
                    .skill(entry.getKey().newSkillInstance())
                    .graduation(SkillGraduation.builder().graduationValue(entry.getValue()).build())
                    .build());
        }

        Character monster = Character.builder()
                .name(getName())
                .race(MONSTER_RACE)
                .attributes(CharacterAttributes.of(getAttributeBases()))
                .egos(CharacterEgos.builder().build())
                .actionProfile(DEFAULT_ACTION_PROFILE)
                .skills(skills)
                .attributeAbilities(getAttributeAbilities())
                .skillCompetencyAbilities(getSkillCompetencyAbilities())
                .equipment(new ArrayList<>(getEquipment()))
                .feats(new ArrayList<>())
                .sizeCategory(getSizeCategory())
                .lifeMultiplier(getLifeMultiplier())
                .manaMultiplier(getManaMultiplier())
                .determinationMultiplier(getDeterminationMultiplier())
                .build();

        return MonsterSheet.of(monster, getPhysicalDefense(), getMagicDefense(),
                getAttackDifficulty(), getAttackBonus());
    }
}
