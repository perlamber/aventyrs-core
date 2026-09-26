package org.aventyrs.core.monster;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.race.Monstruoso;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A <b>fixed</b> stat block — Defesas and GDs stated outright — and the thing that turns it into a
 * playable {@link MonsterSheet}.
 *
 * <p>Since 0.0.59 an authored monster is built by the rules instead, through {@link
 * MonsterBlueprint}, which derives these numbers. A fixed stat block remains for what those rules
 * don't cover: summons (their numbers follow the Conjurador) and hand-built foes in tests.
 *
 * <p>Two ways to get one, mirroring the {@code Item}/{@code AbstractItem}/{@code ArmorItem} split
 * this codebase already uses for catalogs:
 *
 * <ul>
 *   <li>{@link AbstractMonsterTemplate} — <b>fill in the form</b> for a unique monster. Its
 *   builder <i>is</i> the form: name it, set its Attributes, give it Perícias, done.</li>
 *   <li>{@link SummonedMonsterTemplate} — an invocação, whose numbers come from its Conjurador.</li>
 * </ul>
 *
 * <h2>Why the numbers are authored rather than derived</h2>
 *
 * A foe never rolls, so its Defesas and the Grau de Dificuldade it presents on each Perícia are
 * fixed values — and they're written on the stat block, not recomputed from its Destreza and
 * Graduações the way a player's defence roll is. The GDs are one per Perícia it knows ({@link
 * #getSkillDifficulties()}) plus a general one for everything else ({@link
 * #getGeneralDifficulty()}). That keeps a stat block readable and tunable by
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

    /**
     * The {@code ActionProfile} a foe uses unless its stat block says otherwise — deliberately
     * the one profile with no numeric effect on any of PA/Reações/Ações Livres, so a foe whose
     * stat block is silent about its Perfil de Ação gets exactly the counters it authored.
     * Consciência Defensiva's own clause is about which movements provoke Reações, not how many
     * a creature has, and this core has no movement-triggers-Reação mechanism at all (see
     * {@link ActionProfile#CONSCIENCIA_DEFENSIVA}) — so it costs a foe nothing either.
     */
    ActionProfile DEFAULT_ACTION_PROFILE = ActionProfile.CONSCIENCIA_DEFENSIVA;

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

    /**
     * The GD it presents on any Perícia its stat block names no GD for — "a general GD for other
     * tasks". What a foe never authored anything for rolls against: its attacks when it has no
     * attack entry, its Atenção against a hider, its Furtividade against a watcher.
     *
     * <p>{@link #getSkillDifficulties()} overrides it Perícia by Perícia; {@link
     * #getSkillDifficulty(SkillType)} is the one lookup every consumer goes through.
     */
    SkillDifficulty getGeneralDifficulty();

    /**
     * The GD it presents on each Perícia it knows — the Perícias whose GD its stat block states
     * outright. A Perícia absent from this map falls back to {@link #getGeneralDifficulty()}.
     *
     * <p>Keyed separately from {@link #getSkillGraduations()} on purpose: the GD is authored, not
     * derived from a Graduação, exactly as its Defesas are not derived from its Destreza — see this
     * interface's own javadoc. An entry here needs no Graduação and a Graduação needs no entry.
     */
    default Map<SkillType, SkillDifficulty> getSkillDifficulties() {
        return Map.of();
    }

    /**
     * The GD it presents on skillType — its own entry in {@link #getSkillDifficulties()}, or
     * {@link #getGeneralDifficulty()} when it has none. An attack presents {@code
     * getSkillDifficulty(ATAQUE_CORPO_A_CORPO)} (or {@code ATAQUE_A_DISTANCIA}) to a defender's
     * Esquiva e Aparar roll, {@link SkillType#FURTIVIDADE} is how well it hides, and {@link
     * SkillType#ATTENTION}, flattened, is {@link #getPerception()}.
     */
    default SkillDifficulty getSkillDifficulty(@NonNull final SkillType skillType) {
        return getSkillDifficulties().getOrDefault(skillType, getGeneralDifficulty());
    }

    /**
     * The fixed Atenção value this creature presents when someone tries to hide from it — its
     * {@link SkillType#ATTENTION} GD ({@link #getSkillDifficulty(SkillType)}) flattened by {@link
     * SkillDifficulty#getValue()}. {@code
     * org.aventyrs.core.character.services.HidingService#resolveDetection} reads this in place of
     * an Atenção roll and compares it to the hider's concealment: <b>a foe never rolls</b>.
     *
     * <p>A foe that authored no Atenção entry spots at its general GD, Médio's 18 by default.
     */
    default int getPerception() {
        return getSkillDifficulty(SkillType.ATTENTION).getValue();
    }

    /**
     * Its Pontos de Ação. Authored like the four combat numbers and for the same reason — a stat
     * block states "Possuem 2 Pontos de Ação" outright rather than deriving it. Defaults to
     * {@link ActionPointsService#DEFAULT_ACTION_POINTS}, the same 3 every character starts with,
     * so no existing template changes.
     *
     * <p>This is the fixed counter only; {@code ActionPointsService#getMaxActionPoints} still
     * layers the {@code ACTION_POINTS} modifier scan and the {@code ActionProfile} adjustment on
     * top, identically to a player's.
     */
    default int getActionPoints() {
        return ActionPointsService.DEFAULT_ACTION_POINTS;
    }

    /**
     * Especializações held per Perícia — the bracketed tag a stat block writes beside a Perícia,
     * e.g. {@code Ataque Corpo-a-Corpo [Primal]}. A Perícia absent from this map simply has none.
     *
     * <p>Keyed separately from {@link #getSkillGraduations()} rather than folded into it because
     * the overwhelming majority of foes have Graduações and no Especializações, and a single map
     * of pairs would force every one of them to spell out an empty list. An entry here for a
     * Perícia with no Graduação is silently ignored — {@link #spawn()} iterates the Graduações.
     *
     * <p>These are real: a held, matching Especialização named as a roll's {@code
     * SkillRoll#getRequestedAbility()} thresholds against {@code DifficultyLevel#getExpertValue()}
     * instead of the base value.
     */
    default Map<SkillType, List<SkillSpecialization>> getSkillSpecializations() {
        return Map.of();
    }

    /**
     * Whether this creature is a Morto-Vivo. Narrow on purpose: it exists because clauses like
     * the Zumbi's "Vantagem em rolagens de Perícias de Ataque efetuadas contra personagens
     * vivos" need to tell the living from the not, and this core has no such classification —
     * {@code org.aventyrs.core.race.CreatureType} is an essence/anatomy axis (HUMANOIDE, FEERICO,
     * RENASCIDO, DRAGAO …), none of whose values is about being alive.
     *
     * <p>So "living" is resolved as "not a foe whose stat block said this" — which is exact for
     * every combatant this core can currently produce (a {@code CharacterSheet} is always
     * living), and would need revisiting the day a player character can be undead, or a
     * construct/elemental needs to count as non-living without being a Morto-Vivo. A general
     * anatomy/vitality tag on {@code Character} is the real fix; this is deliberately not it.
     */
    default boolean isUndead() {
        return false;
    }

    /** Efeitos Críticos this creature's anatomy shrugs off — see {@link CriticalEffectType}. */
    default Set<CriticalEffectType> getCriticalEffectImmunities() {
        return Set.of();
    }

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
     *
     * @param gm the Player running this foe — see {@link MonsterSheet#getPlayer()}.
     */
    default MonsterSheet spawn(@NonNull final Player gm) {
        Map<SkillType, CharacterSkill> skills = new EnumMap<>(SkillType.class);
        for (Map.Entry<SkillType, Integer> entry : getSkillGraduations().entrySet()) {
            skills.put(entry.getKey(), CharacterSkill.builder()
                    .skill(entry.getKey().newSkillInstance())
                    .specializations(getSkillSpecializations().getOrDefault(entry.getKey(), List.of()))
                    .graduation(SkillGraduation.builder().graduationValue(entry.getValue()).build())
                    .build());
        }

        Character monster = Character.builder()
                .name(getName())
                .race(MONSTER_RACE)
                .attributes(CharacterAttributes.of(getAttributeBases()))
                .egos(CharacterEgos.builder().build())
                .actionProfile(DEFAULT_ACTION_PROFILE)
                .actionPoints(getActionPoints())
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

        return MonsterSheet.of(monster, gm, this);
    }
}
