package org.aventyrs.core.monster.summon;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.skill.SkillType;

import java.util.EnumMap;
import java.util.Map;

/**
 * The creatures the ALIADOS DA NATUREZA tree invokes, each a stat block from {@code docs/rules/magias.txt}. A
 * {@link NatureSummon} is one of these, parameterized by its Conjurador's Graduação.
 *
 * <p>The Graduações are the first figure of each "Perícia [Especialização] +N +M" line; the second is the Categoria de
 * Tamanho's own modifier, which {@code AbstractSkillInteraction} already applies from {@link #getSizeCategory()}.
 * Defesas are the stat block's "Outras Informações" totals, Bônus Racial included. The Dano Base is what is left of
 * "Danos de Ataques" once the melee ½ Força and the size modifier come off — the two terms the engine adds itself.
 *
 * <p>⚠️ The Anciente's Efeito states "DF +20 e DM +25" and "Bônus igual ao Domínio do Mana … ou +20" while its own
 * stat block says DF +18 / DM +21 and lists Graduações; the stat block is taken.
 */
public enum NatureSummonKind {

    /** Aliados da Natureza's animal. "15PV (Multiplicador de PV x5) e Defesas +3", "1d6+4 (Base 2 + Metade da Força)". */
    ALIADO_DA_NATUREZA("Aliado da Natureza", attributes(4, 3, 1, 1, 3, 1, 2), graduations(4, 2, 2, 2),
            SizeCategory.ZERO, 3, 5, 3, 3, DamageBase.of(1, 2), 0),

    /**
     * Its Efeito Alternativo: "um forte exemplar da sua espécie, recebendo +2 Graduações em suas Perícias e Vigor +1".
     * Everything else is the Aliado's.
     */
    PREDADOR_REGIONAL("Predador Regional", attributes(4, 3, 2, 1, 3, 1, 2), graduations(6, 4, 4, 4),
            SizeCategory.ZERO, 3, 5, 3, 3, DamageBase.of(1, 2), 0),

    /** "25PV (Multiplicador de PV x5) e DF +12 / DM +8", "1d6+7 (Base 3 + Metade da Força + Tamanho)", 1 power. */
    EXPERIMENTO_DE_LACERTO("Experimento de Lacerto", attributes(6, 4, 3, 1, 4, 1, 2), graduations(5, 4, 5, 4),
            SizeCategory.PLUS_ONE, 3, 5, 12, 8, DamageBase.of(1, 3), 1),

    /** "35PV (Multiplicador de PV x5) e DF +18 / DM +18", 4PA, "2d6+5 (Base 0 + Metade da Força + Tamanho)", 2 powers. */
    ORGULHO_DE_LACERTO("Orgulho de Lacerto", attributes(8, 7, 5, 1, 6, 1, 3), graduations(10, 5, 7, 7),
            SizeCategory.PLUS_TWO, 4, 5, 18, 18, DamageBase.of(2, 0), 2),

    /** "70PV (Multiplicador de PV x6) e DF +18 / DM +21", "2d6+7 (Base 1 + Metade da Força + Tamanho)". */
    ANCIENTE("Anciente", attributes(8, 10, 10, 3, 8, 4, 6), graduations(10, 8, 8, 5),
            SizeCategory.PLUS_THREE, 3, 6, 18, 21, DamageBase.of(2, 1), 0);

    private final String displayName;
    private final Map<AttributeDomain, Integer> attributeBases;
    private final Map<SkillType, Integer> skillGraduations;
    private final SizeCategory sizeCategory;
    private final int actionPoints;
    private final int lifeMultiplier;
    private final int physicalDefense;
    private final int magicDefense;
    private final DamageBase damageBase;
    private final int powerCount;

    NatureSummonKind(final String displayName, final Map<AttributeDomain, Integer> attributeBases,
                     final Map<SkillType, Integer> skillGraduations, final SizeCategory sizeCategory,
                     final int actionPoints, final int lifeMultiplier, final int physicalDefense,
                     final int magicDefense, final DamageBase damageBase, final int powerCount) {
        this.displayName = displayName;
        this.attributeBases = attributeBases;
        this.skillGraduations = skillGraduations;
        this.sizeCategory = sizeCategory;
        this.actionPoints = actionPoints;
        this.lifeMultiplier = lifeMultiplier;
        this.physicalDefense = physicalDefense;
        this.magicDefense = magicDefense;
        this.damageBase = damageBase;
        this.powerCount = powerCount;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** A fresh, mutable copy — a {@link NatureSummon} adds its encantamentos onto it. */
    public Map<AttributeDomain, Integer> getAttributeBases() {
        return new EnumMap<>(attributeBases);
    }

    public Map<SkillType, Integer> getSkillGraduations() {
        return skillGraduations;
    }

    public SizeCategory getSizeCategory() {
        return sizeCategory;
    }

    public int getActionPoints() {
        return actionPoints;
    }

    public int getLifeMultiplier() {
        return lifeMultiplier;
    }

    public int getPhysicalDefense() {
        return physicalDefense;
    }

    public int getMagicDefense() {
        return magicDefense;
    }

    public DamageBase getDamageBase() {
        return damageBase;
    }

    /** How many {@link LacertoPower}s it is invoked with — 1 for an Experimento, 2 for an Orgulho, else 0. */
    public int getPowerCount() {
        return powerCount;
    }

    /** Whether it is an animal — every kind but the Anciente, a tree. Totem de Gaea strengthens "animais aliados". */
    public boolean isAnimal() {
        return this != ANCIENTE;
    }

    private static Map<AttributeDomain, Integer> attributes(final int strength, final int dexterity, final int vigor,
                                                            final int gnose, final int instinct, final int focus,
                                                            final int charisma) {
        Map<AttributeDomain, Integer> bases = new EnumMap<>(AttributeDomain.class);
        bases.put(AttributeDomain.STRENGTH, strength);
        bases.put(AttributeDomain.DEXTERITY, dexterity);
        bases.put(AttributeDomain.VIGOR, vigor);
        bases.put(AttributeDomain.GNOSE, gnose);
        bases.put(AttributeDomain.INSTINCT, instinct);
        bases.put(AttributeDomain.FOCUS, focus);
        bases.put(AttributeDomain.CHARISMA, charisma);
        return Map.copyOf(bases);
    }

    /** Ataque Corpo-a-Corpo [Primal], Atenção [Sentidos Apurados], Esquiva e Aparar [Guerreiro Natural], Furtividade [Maestria da Ocultação]. */
    private static Map<SkillType, Integer> graduations(final int attack, final int attention, final int dodge,
                                                       final int stealth) {
        return Map.of(SkillType.ATAQUE_CORPO_A_CORPO, attack, SkillType.ATTENTION, attention,
                SkillType.ESQUIVA_E_APARAR, dodge, SkillType.FURTIVIDADE, stealth);
    }
}
