package org.aventyrs.core.monster;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousTraits;
import org.aventyrs.core.monster.model.almaelemental.AlmaElementalAbility;
import org.aventyrs.core.monster.model.bestaalada.BestaAladaAbility;
import org.aventyrs.core.monster.model.brotosdemapinguari.BrotosDeMapinguariAbility;
import org.aventyrs.core.monster.model.mutantemonstruoso.MutanteMonstruosoAbility;
import org.aventyrs.core.monster.model.aspectohumanoide.AspectoHumanoideAbility;
import org.aventyrs.core.monster.model.cireneia.CireneiaAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.function.Supplier;

/**
 * Worked examples of {@link MonsterBlueprint} — legal under {@link MonsterRules#validate}, one per
 * end of the table. An editor offers them as starting points; a test uses them as known numbers.
 *
 * <p>They replace the five fixed-stat-block {@code GenericMonster} archetypes, which authored their
 * GDs and Defesas by hand. Each is a {@link Supplier} of a fresh blueprint so a caller may {@code
 * toBuilder()} it freely.
 */
public enum SampleMonster implements Supplier<MonsterBlueprint> {

    /**
     * Presa, GP 5, Regular. From the rules' own examples ("Goblins Selvagens"). Aspecto Humanoide's
     * two Presa Habilidades are catalog-only — held and counted (+2PV each), not applied.
     *
     * <p>ACaC Fácil+1 (15), Esquiva e Aparar Fácil+2 (16) — so DF/DM 16 — Furtividade Fácil+2 (16),
     * Atenção Muito Fácil+1 (13). PV 39, PD 28, PM 21, 3 PA. Corpo Humanoide mimics its Ataque Corpo-a-Corpo
     * and adds a Talento slot; Máscara Social's Carisma +2 grants Aparência Inofensiva.
     */
    GOBLIN_SELVAGEM(() -> MonsterBlueprint.builder()
            .name("Goblin Selvagem")
            .powerDegree(5)
            .sizeCategory(SizeCategory.MINUS_ONE)
            .attributeBase(AttributeDomain.VIGOR, 3)
            .attributeBase(AttributeDomain.STRENGTH, 3)
            .attributeBase(AttributeDomain.DEXTERITY, 4)
            .attributeBase(AttributeDomain.INSTINCT, 3)
            .attributeBase(AttributeDomain.GNOSE, 2)
            .trainedSkill(SkillType.ATAQUE_CORPO_A_CORPO)
            .trainedSkill(SkillType.ESQUIVA_E_APARAR)
            .trainedSkill(SkillType.FURTIVIDADE)
            .trainedSkill(SkillType.ATTENTION)
            .gnoseUpgrade(SkillType.ATAQUE_CORPO_A_CORPO)
            .gnoseUpgrade(SkillType.FURTIVIDADE)
            .progressionUpgrade(SkillType.ESQUIVA_E_APARAR, 1)
            .model(MonsterModel.ASPECTO_HUMANOIDE)
            .ability(MonstrousAbilitySelection.of(AspectoHumanoideAbility.CORPO_HUMANOIDE, SkillType.ATAQUE_CORPO_A_CORPO.name()))
            .ability(MonstrousAbilitySelection.of(AspectoHumanoideAbility.MASCARA_SOCIAL))
            .build()),

    /**
     * Predador, GP 30, Regular — an Abençoado de Cireneia, whose Habilidades are applied in full.
     *
     * <p>Destreza 5 + Bônus Racial 4 trimmed to the Predador's 8. ACaC Muito Difícil+1 (29),
     * Esquiva e Aparar Muito Difícil+4 (32, Liberdade Selvagem's step included) — so DF/DM 32 —
     * Furtividade Difícil+4 (27), Atenção Muito Fácil+1 (13). PV 54, PD 36, PM 29; 4 PA fixed plus
     * Celeridade's 1; Movimento +3, Iniciativa +2, +1 Reação and +1 Ação Livre.
     */
    PANTERA_DE_CIRENEIA(() -> MonsterBlueprint.builder()
            .name("Pantera de Cireneia")
            .powerDegree(30)
            .sizeCategory(SizeCategory.PLUS_ONE)
            .attributeBase(AttributeDomain.VIGOR, 3)
            .attributeBase(AttributeDomain.STRENGTH, 2)
            .attributeBase(AttributeDomain.DEXTERITY, 5)
            .attributeBase(AttributeDomain.INSTINCT, 3)
            .attributeBase(AttributeDomain.GNOSE, 2)
            .trainedSkill(SkillType.ATAQUE_CORPO_A_CORPO)
            .trainedSkill(SkillType.ESQUIVA_E_APARAR)
            .trainedSkill(SkillType.FURTIVIDADE)
            .trainedSkill(SkillType.ATTENTION)
            .gnoseUpgrade(SkillType.ATAQUE_CORPO_A_CORPO)
            .gnoseUpgrade(SkillType.ESQUIVA_E_APARAR)
            .progressionUpgrade(SkillType.ATAQUE_CORPO_A_CORPO, 3)
            .progressionUpgrade(SkillType.ESQUIVA_E_APARAR, 2)
            .progressionUpgrade(SkillType.FURTIVIDADE, 2)
            .model(MonsterModel.ABENCOADO_DE_CIRENEIA)
            .ability(MonstrousAbilitySelection.of(CireneiaAbility.ATRIBUTOS_APRIMORADOS))
            .ability(MonstrousAbilitySelection.of(CireneiaAbility.MOVIMENTO_APRIMORADO))
            .ability(MonstrousAbilitySelection.of(CireneiaAbility.CELERIDADE))
            .ability(MonstrousAbilitySelection.of(CireneiaAbility.RELAMPEJANTE, CireneiaAbility.REACTION_CHOICE))
            .ability(MonstrousAbilitySelection.of(CireneiaAbility.LIBERDADE_SELVAGEM))
            .egoPoint(EgoDomain.INICIATIVA, 3)
            .egoPoint(EgoDomain.SORTE, 3)
            .build()),

    /** Presa, GP 6, Alma Elemental — a fire salamander: RE, Resistência a Críticos and Instinto against Fogo, and claws. */
    SALAMANDRA(() -> MonsterBlueprint.builder()
            .name("Salamandra")
            .powerDegree(6)
            .sizeCategory(SizeCategory.MINUS_ONE)
            .attributeBase(AttributeDomain.VIGOR, 3)
            .attributeBase(AttributeDomain.DEXTERITY, 4)
            .attributeBase(AttributeDomain.INSTINCT, 3)
            .attributeBase(AttributeDomain.FOCUS, 3)
            .trainedSkill(SkillType.ATAQUE_CORPO_A_CORPO)
            .trainedSkill(SkillType.ESQUIVA_E_APARAR)
            .gnoseUpgrade(SkillType.ATAQUE_CORPO_A_CORPO)
            .progressionUpgrade(SkillType.ESQUIVA_E_APARAR, 1)
            .model(MonsterModel.ALMA_ELEMENTAL)
            .ability(MonstrousAbilitySelection.of(AlmaElementalAbility.SANGUE_ELEMENTAL, ElementalType.FOGO.name()))
            .ability(MonstrousAbilitySelection.of(AlmaElementalAbility.ARMAMENTO_ELEMENTAL, java.util.Map.of(
                    AlmaElementalAbility.WEAPON, java.util.List.of(NaturalWeapon.GARRAS_AFIADAS.name()),
                    MonstrousTraits.ELEMENT, java.util.List.of(ElementalType.FOGO.name()))))
            .build()),

    /** Presa, GP 3, Mutante Monstruoso — a sewer rat grown wrong: RDS, Resistência a Críticos, a second head. */
    RATO_MUTANTE(() -> MonsterBlueprint.builder()
            .name("Rato Mutante")
            .powerDegree(3)
            .sizeCategory(SizeCategory.MINUS_ONE)
            .attributeBase(AttributeDomain.VIGOR, 4)
            .attributeBase(AttributeDomain.DEXTERITY, 4)
            .attributeBase(AttributeDomain.INSTINCT, 3)
            .attributeBase(AttributeDomain.STRENGTH, 3)
            .trainedSkill(SkillType.ATAQUE_CORPO_A_CORPO)
            .trainedSkill(SkillType.FURTIVIDADE)
            .gnoseUpgrade(SkillType.FURTIVIDADE)
            .model(MonsterModel.MUTANTE_MONSTRUOSO)
            .ability(MonstrousAbilitySelection.of(MutanteMonstruosoAbility.FISIOLOGIA_ESTRANHA))
            .ability(MonstrousAbilitySelection.of(MutanteMonstruosoAbility.MEMBROS_MULTIPLOS, "CABECAS"))
            .build()),

    /** Presa, GP 4, Besta Alada — a harpy: Mergulho Atroz from the air, +2 Movimento when flying. */
    HARPIA(() -> MonsterBlueprint.builder()
            .name("Harpia")
            .powerDegree(4)
            .attributeBase(AttributeDomain.VIGOR, 3)
            .attributeBase(AttributeDomain.DEXTERITY, 5)
            .attributeBase(AttributeDomain.INSTINCT, 3)
            .attributeBase(AttributeDomain.CHARISMA, 2)
            .trainedSkill(SkillType.ATAQUE_CORPO_A_CORPO)
            .trainedSkill(SkillType.ESQUIVA_E_APARAR)
            .trainedSkill(SkillType.ATTENTION)
            .gnoseUpgrade(SkillType.ESQUIVA_E_APARAR)
            .progressionUpgrade(SkillType.ATAQUE_CORPO_A_CORPO, 1)
            .model(MonsterModel.BESTA_ALADA)
            .ability(MonstrousAbilitySelection.of(BestaAladaAbility.PLANAR_E_PAIRAR))
            .ability(MonstrousAbilitySelection.of(BestaAladaAbility.DESFILE_DE_SYLPH))
            .build()),

    /** Presa, GP 5, Brotos de Mapinguari — a thorn sprout: Anatomia Vegetal, regenerating half its Instinto each Rodada. */
    BROTO_ESPINHOSO(() -> MonsterBlueprint.builder()
            .name("Broto Espinhoso")
            .powerDegree(5)
            .attributeBase(AttributeDomain.VIGOR, 4)
            .attributeBase(AttributeDomain.STRENGTH, 3)
            .attributeBase(AttributeDomain.INSTINCT, 4)
            .attributeBase(AttributeDomain.GNOSE, 2)
            .trainedSkill(SkillType.ATAQUE_CORPO_A_CORPO)
            .trainedSkill(SkillType.ATTENTION)
            .gnoseUpgrade(SkillType.ATAQUE_CORPO_A_CORPO)
            .gnoseUpgrade(SkillType.ATTENTION)
            .progressionUpgrade(SkillType.ATAQUE_CORPO_A_CORPO, 1)
            .model(MonsterModel.BROTOS_DE_MAPINGUARI)
            .ability(MonstrousAbilitySelection.of(BrotosDeMapinguariAbility.NASCIDO_DAS_LAGRIMAS_DE_FLORA, ElementalType.GELO.name()))
            .build());

    private final Supplier<MonsterBlueprint> source;

    SampleMonster(final Supplier<MonsterBlueprint> source) {
        this.source = source;
    }

    @Override
    public MonsterBlueprint get() {
        return source.get();
    }
}
