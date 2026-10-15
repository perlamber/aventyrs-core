package org.aventyrs.core.monster.generator;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.skill.SkillType;

import static org.aventyrs.core.character.AttributeDomain.CHARISMA;
import static org.aventyrs.core.character.AttributeDomain.DEXTERITY;
import static org.aventyrs.core.character.AttributeDomain.FOCUS;
import static org.aventyrs.core.character.AttributeDomain.GNOSE;
import static org.aventyrs.core.character.AttributeDomain.INSTINCT;
import static org.aventyrs.core.character.AttributeDomain.STRENGTH;
import static org.aventyrs.core.character.AttributeDomain.VIGOR;

/**
 * The recipe {@link MonsterGenerator} follows for a plain monster of one {@link MonsterModel}: the
 * Mestre's quick "conjure a Mutante GP 12" (table ruling, 2026-10-15). One per Modelo, named after it,
 * so a preset's constant name is also the default token portrait's file name on a client.
 *
 * <p>⚠️ Every figure here is <b>authored</b>, not read off the rules. The manual gives the budgets
 * and never a stock stat line, so each preset leans the way its Modelo's own text describes it.
 * <ul>
 *   <li>{@link #getAttributeBases()} spends exactly {@code MonsterBlueprint.ATTRIBUTE_POINTS} above base 1.</li>
 *   <li>The Perícias, Talento trees and Ego order are what the generator fills first. The shapes
 *   come from the Modelo descriptions and {@code SampleMonster}.</li>
 * </ul>
 *
 * <p>A preset names no equipment and no {@code CreatureType}. The API's stored blueprint carries
 * neither, and a conjured monster must survive that round trip unchanged, exactly like one the
 * monster editor authors. A Habilidade that grants an Arma Natural still does.
 */
@Getter
public enum ConjurePreset {

    ABENCOADO_DE_CIRENEIA(MonsterModel.ABENCOADO_DE_CIRENEIA, "Fera de Cireneia", SizeCategory.ZERO,
            bases(3, 2, 5, 1, 3, 2, 1),
            List.of(SkillType.ATAQUE_CORPO_A_CORPO, SkillType.ESQUIVA_E_APARAR, SkillType.FURTIVIDADE, SkillType.ATTENTION),
            List.of(SkillType.ATLETISMO),
            List.of(FeatCategory.MONSTRUOSO, FeatCategory.MOBILIDADE, FeatCategory.ASSASSINO),
            List.of(EgoDomain.INICIATIVA, EgoDomain.SORTE, EgoDomain.AUTOCONTROLE, EgoDomain.RECURSOS)),

    ALMA_ELEMENTAL(MonsterModel.ALMA_ELEMENTAL, "Alma Elemental", SizeCategory.ZERO,
            bases(3, 1, 3, 4, 3, 2, 1),
            List.of(SkillType.ATAQUE_CORPO_A_CORPO, SkillType.ESQUIVA_E_APARAR, SkillType.DOMINIO_DO_MANA, SkillType.ATTENTION),
            List.of(SkillType.CONHECIMENTOS),
            List.of(FeatCategory.MONSTRUOSO, FeatCategory.METAMAGICO),
            List.of(EgoDomain.AUTOCONTROLE, EgoDomain.INICIATIVA, EgoDomain.SORTE, EgoDomain.RECURSOS)),

    ASPECTO_HUMANOIDE(MonsterModel.ASPECTO_HUMANOIDE, "Humanoide Monstruoso", SizeCategory.ZERO,
            bases(3, 4, 3, 1, 2, 2, 2),
            List.of(SkillType.ATAQUE_CORPO_A_CORPO, SkillType.ESQUIVA_E_APARAR, SkillType.ATTENTION, SkillType.PERSUASAO),
            List.of(SkillType.FURTIVIDADE, SkillType.ATLETISMO),
            List.of(FeatCategory.MONSTRUOSO, FeatCategory.DUELISTA, FeatCategory.ARTE_MARCIAL),
            List.of(EgoDomain.AUTOCONTROLE, EgoDomain.RECURSOS, EgoDomain.INICIATIVA, EgoDomain.SORTE)),

    MUTANTE_MONSTRUOSO(MonsterModel.MUTANTE_MONSTRUOSO, "Mutante Monstruoso", SizeCategory.ZERO,
            bases(4, 3, 3, 1, 3, 2, 1),
            List.of(SkillType.ATAQUE_CORPO_A_CORPO, SkillType.ESQUIVA_E_APARAR, SkillType.FURTIVIDADE, SkillType.ATTENTION),
            List.of(SkillType.ATLETISMO),
            List.of(FeatCategory.MONSTRUOSO, FeatCategory.SOBREVIVENCIA),
            List.of(EgoDomain.AUTOCONTROLE, EgoDomain.SORTE, EgoDomain.INICIATIVA, EgoDomain.RECURSOS)),

    BESTA_ALADA(MonsterModel.BESTA_ALADA, "Besta Alada", SizeCategory.ZERO,
            bases(3, 2, 5, 1, 3, 1, 2),
            List.of(SkillType.ATAQUE_CORPO_A_CORPO, SkillType.ESQUIVA_E_APARAR, SkillType.ATTENTION),
            List.of(SkillType.ATLETISMO, SkillType.FURTIVIDADE),
            List.of(FeatCategory.MONSTRUOSO, FeatCategory.MOBILIDADE),
            List.of(EgoDomain.INICIATIVA, EgoDomain.SORTE, EgoDomain.AUTOCONTROLE, EgoDomain.RECURSOS)),

    BROTOS_DE_MAPINGUARI(MonsterModel.BROTOS_DE_MAPINGUARI, "Broto de Mapinguari", SizeCategory.ZERO,
            bases(4, 3, 2, 1, 4, 2, 1),
            List.of(SkillType.ATAQUE_CORPO_A_CORPO, SkillType.ATTENTION, SkillType.ESQUIVA_E_APARAR),
            List.of(SkillType.EMPATIA_SELVAGEM),
            List.of(FeatCategory.MONSTRUOSO, FeatCategory.SOBREVIVENCIA),
            List.of(EgoDomain.AUTOCONTROLE, EgoDomain.SORTE, EgoDomain.INICIATIVA, EgoDomain.RECURSOS)),

    ESCOLHIDO_DE_MACARIA(MonsterModel.ESCOLHIDO_DE_MACARIA, "Escolhido de Macária", SizeCategory.ZERO,
            bases(3, 4, 2, 2, 2, 2, 2),
            List.of(SkillType.ATAQUE_CORPO_A_CORPO, SkillType.ESQUIVA_E_APARAR, SkillType.ATTENTION),
            List.of(SkillType.DOMINIO_DO_MANA, SkillType.FURTIVIDADE),
            List.of(FeatCategory.MONSTRUOSO, FeatCategory.ASSASSINO),
            List.of(EgoDomain.AUTOCONTROLE, EgoDomain.INICIATIVA, EgoDomain.SORTE, EgoDomain.RECURSOS));

    private final MonsterModel model;

    /** What a conjured monster is called when the Mestre names none. */
    private final String defaultName;

    private final SizeCategory sizeCategory;

    /** Each Atributo's base, in {@link AttributeDomain} declaration order. */
    private final Map<AttributeDomain, Integer> attributeBases;

    /** Trained always, in the order the upgrades reach them. */
    private final List<SkillType> primarySkills;

    /** Trained only once the primary Perícias reach their Categoria's ceiling with upgrades left over. */
    private final List<SkillType> secondarySkills;

    /** The trees a Talento is drawn from, preferred first. Anything outside them is never picked. */
    private final List<FeatCategory> featCategories;

    /** The order Ego points are dealt in, one at a time. */
    private final List<EgoDomain> egoOrder;

    ConjurePreset(final MonsterModel model, final String defaultName, final SizeCategory sizeCategory,
                  final Map<AttributeDomain, Integer> attributeBases,
                  final List<SkillType> primarySkills, final List<SkillType> secondarySkills,
                  final List<FeatCategory> featCategories, final List<EgoDomain> egoOrder) {
        this.model = model;
        this.defaultName = defaultName;
        this.sizeCategory = sizeCategory;
        this.attributeBases = attributeBases;
        this.primarySkills = primarySkills;
        this.secondarySkills = secondarySkills;
        this.featCategories = featCategories;
        this.egoOrder = egoOrder;
    }

    /** The preset for model. Every Modelo has exactly one. */
    public static ConjurePreset of(final MonsterModel model) {
        for (ConjurePreset preset : values()) {
            if (preset.model == model) {
                return preset;
            }
        }
        throw new IllegalArgumentException("No ConjurePreset for " + model);
    }

    private static Map<AttributeDomain, Integer> bases(final int vigor, final int strength, final int dexterity,
                                                       final int focus, final int instinct, final int gnose,
                                                       final int charisma) {
        Map<AttributeDomain, Integer> bases = new java.util.EnumMap<>(AttributeDomain.class);
        bases.put(VIGOR, vigor);
        bases.put(STRENGTH, strength);
        bases.put(DEXTERITY, dexterity);
        bases.put(FOCUS, focus);
        bases.put(INSTINCT, instinct);
        bases.put(GNOSE, gnose);
        bases.put(CHARISMA, charisma);
        return java.util.Collections.unmodifiableMap(bases);
    }
}
