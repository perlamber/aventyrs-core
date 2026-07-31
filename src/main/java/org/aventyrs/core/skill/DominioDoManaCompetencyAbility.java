package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Domínio do Mana. Every
 * one of these modifies some aspect of Magias (critical margin, duration, damage/healing,
 * casting GD, concentration) — no {@code Magia}/spellcasting entity or resolution engine
 * exists yet, so none of them are expressible for real today; see each constant's TODO.
 */
@Getter
@AllArgsConstructor
public enum DominioDoManaCompetencyAbility implements SkillCompetencyAbility {

    // TODO: +1 to a Magia's Margem Crítica Menor, then +1 more at the 4th, 7th and 10th
    // graduation (a graduation-tiered scaling bonus, not a flat one) — no Magia entity or
    // Margem Crítica (critical-margin) concept exists yet.
    LETALIDADE_ARCANA("A Margem Crítica Menor de suas Magias é aumentada em +1 número, " +
            "então em +1 ao alcançar a 4ª, 7ª e 10ª graduação."),

    // TODO: -1 GD, but not for Domínio do Mana's own roll — casting a Magia is two rolls
    // (see org.aventyrs.core.magic.SpellCastingService): the *delivery* Perícia (e.g. Ataque
    // à Distância) rolled against the target's GD, then Domínio do Mana rolled against the
    // Magia's own GD. Feiticeiro reduces the delivery roll's GD, which
    // SkillCompetencyAbility.getDifficultyReduction() can't model — that hook only ever
    // feeds back into this *same* skill's own Interaction, never another skill's. Awaiting
    // the Magia list (and their GDs) to exist before this can be wired into
    // SpellCastingService. Requires 7 Graduações to acquire — also unenforced (see
    // EsquivaEApararCompetencyAbility.RECUO_RAPIDO for the same convention), since no
    // eligibility-validation service exists for SkillCompetencyAbility.
    FEITICEIRO("Requer 7 Graduações – O GD para Conjuração de suas Magias é reduzido em -1 " +
            "Nível."),

    // TODO: +1 Rodada to a Magia's Duração, then +1 more at the 4th, 7th and 10th graduation
    // — no Magia/spell-duration-tracking system exists yet.
    CONJURACAO_DURADOURA("A Duração de suas Magias é aumentada em +1 Rodada, então em +1 " +
            "ao alcançar a 4ª, 7ª e 10ª graduação."),

    // TODO: +3 to a Magia's Dano/Cura effects, then +1 more at the 4th, 7th and 10th
    // graduation — no Magia damage/healing-formula system exists yet.
    CONJURACAO_EXPLOSIVA("Efeitos de Danos e Curas de suas Magias são aumentados em +3, " +
            "então em +1 ao alcançar a 4ª, 7ª e 10ª graduação."),

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
