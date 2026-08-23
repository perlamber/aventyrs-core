package org.aventyrs.core.monster;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

/**
 * <b>A generic monster, on-scene.</b> Ready-made {@link MonsterTemplate}s for when a Narrador
 * needs an opponent immediately and doesn't want to design one — the counterpart to {@link
 * AbstractMonsterTemplate}'s fill-in-the-form path.
 *
 * <pre>{@code
 * MonsterSheet thug = GenericMonster.CAPANGA.spawn();
 * scene.addParticipant(thug, 11, enemyGroup);
 * }</pre>
 *
 * <p>Three roles at three threat tiers, so a Narrador picks by <i>what the fight needs</i> rather
 * than by monster fiction: something that swings hard ({@link #CAPANGA}, {@link #BRUTAMONTES}),
 * something that stays at range ({@link #ATIRADOR}), something that casts ({@link #CONJURADOR}),
 * and something that ends a session ({@link #ABERRACAO}). Deliberately unnamed as species —
 * these are stand-ins, and a foe with a name and a story belongs in {@link
 * AbstractMonsterTemplate} or a catalog of its own.
 *
 * <p>The numbers rise together across the tiers (Attributes, Graduações, Defesas and attack GD),
 * so a tier is a single dial rather than five. They're authored, not derived — see {@link
 * MonsterTemplate}.
 *
 * <p>None of these carries abilities or equipment: a generic stand-in with a signature trait
 * isn't generic any more. Add those through {@link AbstractMonsterTemplate} when a fight needs
 * them.
 */
@Getter
@AllArgsConstructor
public enum GenericMonster implements MonsterTemplate {

    /** A low-tier melee thug — the body in a room full of them. */
    CAPANGA("Capanga",
            Map.of(AttributeDomain.VIGOR, 2, AttributeDomain.STRENGTH, 3, AttributeDomain.DEXTERITY, 2),
            Map.of(SkillType.ATAQUE_CORPO_A_CORPO, 3, SkillType.ESQUIVA_E_APARAR, 2),
            SizeCategory.ZERO, 13, 11, DifficultyLevel.EASY, 0, 4),

    /** A low-tier ranged attacker, fragile up close. */
    ATIRADOR("Atirador",
            Map.of(AttributeDomain.VIGOR, 2, AttributeDomain.DEXTERITY, 4, AttributeDomain.FOCUS, 2),
            Map.of(SkillType.ATAQUE_A_DISTANCIA, 4, SkillType.ESQUIVA_E_APARAR, 3),
            SizeCategory.ZERO, 12, 11, DifficultyLevel.EASY, 1, 3),

    /** A mid-tier bruiser: slow, heavily armoured, hits hard. */
    BRUTAMONTES("Brutamontes",
            Map.of(AttributeDomain.VIGOR, 6, AttributeDomain.STRENGTH, 6, AttributeDomain.DEXTERITY, 1),
            Map.of(SkillType.ATAQUE_CORPO_A_CORPO, 7, SkillType.ESQUIVA_E_APARAR, 2),
            SizeCategory.PLUS_ONE, 17, 12, DifficultyLevel.MEDIUM, 2, 6),

    /** A mid-tier caster — poor Defesa Física, strong Defesa Mágica. */
    CONJURADOR("Conjurador",
            Map.of(AttributeDomain.VIGOR, 3, AttributeDomain.GNOSE, 6, AttributeDomain.FOCUS, 5),
            Map.of(SkillType.DOMINIO_DO_MANA, 7, SkillType.ATAQUE_A_DISTANCIA, 4, SkillType.ESQUIVA_E_APARAR, 3),
            SizeCategory.ZERO, 13, 19, DifficultyLevel.MEDIUM, 3, 4),

    /**
     * A high-tier horror meant to end a session. Its Vigor of 12 and Graduação of 14 are both
     * far past any player ceiling — which needs no special mechanism, see {@link
     * MonsterTemplate}. Its {@code lifeMultiplier} of 8 is the other half of that: bulk paid for
     * directly rather than by inflating Vigor until every Vigor-governed roll came with it.
     */
    ABERRACAO("Aberração",
            Map.of(AttributeDomain.VIGOR, 12, AttributeDomain.STRENGTH, 10, AttributeDomain.INSTINCT, 8),
            Map.of(SkillType.ATAQUE_CORPO_A_CORPO, 14, SkillType.ESQUIVA_E_APARAR, 9, SkillType.ATTENTION, 8),
            SizeCategory.PLUS_THREE, 24, 22, DifficultyLevel.VERY_HARD, 4, 8);

    private final String name;
    private final Map<AttributeDomain, Integer> attributeBases;
    private final Map<SkillType, Integer> skillGraduations;
    private final SizeCategory sizeCategory;
    private final int physicalDefense;
    private final int magicDefense;
    private final DifficultyLevel attackDifficulty;
    private final int attackBonus;
    private final int lifeMultiplier;

    @Override
    public List<AttributeAbility> getAttributeAbilities() {
        return List.of();
    }

    @Override
    public List<SkillCompetencyAbility> getSkillCompetencyAbilities() {
        return List.of();
    }

    @Override
    public List<Item> getEquipment() {
        return List.of();
    }
}
