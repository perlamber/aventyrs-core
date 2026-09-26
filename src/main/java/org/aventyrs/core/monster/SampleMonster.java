package org.aventyrs.core.monster;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.monster.model.MonsterModel;
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
     * Atenção Muito Fácil+1 (13). PV 39, PD 28, PM 21, 3 PA.
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
            .ability(MonstrousAbilitySelection.of(AspectoHumanoideAbility.CORPO_HUMANOIDE))
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
