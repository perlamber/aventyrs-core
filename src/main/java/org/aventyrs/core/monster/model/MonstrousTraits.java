package org.aventyrs.core.monster.model;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterRules;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.SkillType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Small shared vocabulary for the Modelo enums — the element picks, the Árvores de Magia a
 * Habilidade may teach, "Perícias Físicas", and a timed-bonus supplier. Kept here rather than
 * repeated in six enums.
 */
public final class MonstrousTraits {

    /** Pick id for "Escolha um Elemento". */
    public static final String ELEMENT = "element";

    /** One Instância of Resistência a Críticos (RC), as {@code getTotalCriticalResistance} counts it. */
    public static final int CRITICAL_RESISTANCE_INSTANCE = 2;

    /** One instance of RD/RM/RA — "Cada instância reduz … em -2". */
    public static final int REDUCTION_INSTANCE = 2;

    /** One instance of RDS — "Cada instância reduz … em -1", carried as plain RD. */
    public static final int RDS_INSTANCE = 1;

    private MonstrousTraits() {
    }

    /** Every element a monster may pick — {@link ElementalType#TODOS} is a resistance, never a pick. */
    public static List<String> elementOptions() {
        return Arrays.stream(ElementalType.values()).filter(element -> element != ElementalType.TODOS)
                .map(Enum::name).toList();
    }

    /** The Árvores de Magia matching filter, as pick options. */
    public static List<String> treeOptions(@NonNull final Predicate<MagicTree> filter) {
        return Arrays.stream(MagicTree.values()).filter(filter).map(Enum::name).toList();
    }

    /** "Árvores de Magia Elemental". */
    public static boolean isElemental(final MagicTree tree) {
        return hasType(tree, MagicType.ELEMENTAL);
    }

    /** "Elemental, Divina ou Profana". */
    public static boolean isElementalDivineOrProfane(final MagicTree tree) {
        return isElemental(tree) || hasType(tree, MagicType.DIVINA) || hasType(tree, MagicType.PROFANA);
    }

    /** "Árvores Elemental: Natural" — the Natural school. */
    public static boolean isNatural(final MagicTree tree) {
        return hasType(tree, MagicType.NATURAL);
    }

    private static boolean hasType(final MagicTree tree, final MagicType type) {
        return tree.getMagicType() == type || tree.getSecondaryMagicType().filter(type::equals).isPresent();
    }

    /** Every Magia in the named trees up to and including maxLevel. Unknown names are skipped. */
    public static List<Spell> spellsUpTo(@NonNull final List<String> treeNames, @NonNull final BranchLevel maxLevel) {
        return treeNames.stream()
                .flatMap(name -> Arrays.stream(MagicTree.values()).filter(tree -> tree.name().equals(name)))
                .flatMap(tree -> tree.getSpells().stream())
                .filter(spell -> maxLevel.isAtLeast(spell.getBranchLevel()))
                .distinct()
                .toList();
    }

    /** Every Magia of every tree filter matches, up to maxLevel. */
    public static List<Spell> spellsUpTo(@NonNull final Predicate<MagicTree> filter, @NonNull final BranchLevel maxLevel) {
        return spellsUpTo(treeOptions(filter), maxLevel);
    }

    /** The Perícias governed by domain. */
    public static Set<SkillType> skillsGovernedBy(@NonNull final AttributeDomain domain) {
        Set<SkillType> skills = EnumSet.noneOf(SkillType.class);
        for (SkillType skill : SkillType.values()) {
            if (MonsterRules.governingAttribute(skill) == domain) {
                skills.add(skill);
            }
        }
        return skills;
    }

    /**
     * "Perícias Físicas" — the Perícias of the body: Força, Destreza and Vigor. An inference: the
     * rules never list them, and these three Atributos are the physical ones.
     */
    public static Set<SkillType> physicalSkills() {
        Set<SkillType> skills = EnumSet.noneOf(SkillType.class);
        skills.addAll(skillsGovernedBy(AttributeDomain.STRENGTH));
        skills.addAll(skillsGovernedBy(AttributeDomain.DEXTERITY));
        skills.addAll(skillsGovernedBy(AttributeDomain.VIGOR));
        return skills;
    }

    /** The Perícias a monster attacks and defends with. */
    public static Set<SkillType> attackAndDefenseSkills() {
        return EnumSet.of(SkillType.ATAQUE_CORPO_A_CORPO, SkillType.ATAQUE_A_DISTANCIA, SkillType.ESQUIVA_E_APARAR);
    }

    /** A timed bonus, fresh each call — the shape every {@code MonstrousActiveAbility} effect takes. */
    public static Supplier<TemporaryEffect> bonus(@NonNull final ModifierType type, final int value, final int rounds,
                                                  final String source) {
        return () -> new TemporaryBonus(type, value, rounds, source);
    }

    /** An open-ended bonus — what a while-flying or standing effect is. */
    public static TemporaryEffect standing(@NonNull final ModifierType type, final int value, final String source) {
        return TemporaryBonus.openEnded(type, value, source);
    }
}
