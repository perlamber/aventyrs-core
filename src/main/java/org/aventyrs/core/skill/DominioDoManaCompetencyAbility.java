package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Domínio do Mana. Most of
 * these modify some aspect of Magias (critical margin, duration, damage/healing,
 * concentration) that no {@code Magia}/spellcasting entity or resolution engine can express
 * yet — none of them are expressible for real today; see each constant's TODO. MAGIA_SELVAGEM's
 * unconditional Attribute substitution is the exception — see {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()}.
 */
@Getter
@AllArgsConstructor
public enum DominioDoManaCompetencyAbility implements SkillCompetencyAbility {

    // TODO: +1 to a Magia's Margem Crítica Menor, then +1 more at the 5th and 10th
    // graduation (a graduation-tiered scaling bonus, not a flat one) — no Magia entity
    // exists yet to attach a critical-margin value to. A Perícia-scoped critical-margin
    // concept now exists (see ArtesAprimorarComArteAbility#getCriticalMarginReduction), but
    // this ability's is Magia-scoped, a different axis that concept doesn't cover.
    LETALIDADE_ARCANA("A Margem Crítica Menor de suas Magias é aumentada em +1 número, " +
            "então em +1 ao alcançar a 5ª e 10ª graduação."),

    // Substitutes Foco for Instinto — see SkillCompetencyAbility.getSubstituteAttributeDomain().
    MAGIA_SELVAGEM("Você pode alterar o Atributo Base desta Perícia para Instinto.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.INSTINCT);
        }
    },

    // TODO: +1 Rodada to a Magia's Duração, then +1 more at the 5th and 10th graduation —
    // no Magia/spell-duration-tracking system exists yet.
    CONJURACAO_DURADOURA("A Duração de suas Magias é aumentada em +1 Rodada, então em +1 " +
            "ao alcançar a 5ª e 10ª graduação."),

    // TODO: +2 to a Magia's Dano/Cura effects, then +1 more at the 5th and 10th graduation
    // — no Magia damage/healing-formula system exists yet.
    ARCANISMO_EXPLOSIVA("Efeitos de Danos e Curas de suas Magias são aumentados em +2, " +
            "então em +1 ao alcançar a 5ª e 10ª graduação."),

    // TODO: prevents losing Concentração (to keep a Magia active) after taking Dano — no
    // Concentração system or Dano-triggers-a-concentration-check mechanic exists yet.
    CONCENTRACAO_INABALAVEL("Você não perde a Concentração para manter ativa suas magias " +
            "após sofrer Danos.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.DOMINIO_DO_MANA;
    }
}
