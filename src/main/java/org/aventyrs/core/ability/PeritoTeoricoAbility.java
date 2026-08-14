package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillType;

import java.util.Collection;

/**
 * The concrete, already-resolved form of {@link GnoseAbility#PERITO_TEORICO} — one constant
 * per {@link SkillType}, so "which Perícia was chosen" is which constant a character holds,
 * not a separately-persisted value. Grant the matching constant in
 * {@code Character.attributeAbilities} instead of {@code GnoseAbility#PERITO_TEORICO} itself
 * (which stays the catalog/rules-text entry — see its own comment).
 *
 * <p>Whenever a new {@link SkillType} constant is added, a matching constant must be added
 * here too — see CLAUDE.md's "Adding a new Perícia" checklist.
 */
@Getter
@AllArgsConstructor
public enum PeritoTeoricoAbility implements AttributeAbility {

    ATTENTION(SkillType.ATTENTION),
    ARTES(SkillType.ARTES),
    ATLETISMO(SkillType.ATLETISMO),
    DIRIGIR_E_CAVALGAR(SkillType.DIRIGIR_E_CAVALGAR),
    DOMINIO_DO_MANA(SkillType.DOMINIO_DO_MANA),
    ATAQUE_A_DISTANCIA(SkillType.ATAQUE_A_DISTANCIA),
    ATAQUE_CORPO_A_CORPO(SkillType.ATAQUE_CORPO_A_CORPO),
    ESQUIVA_E_APARAR(SkillType.ESQUIVA_E_APARAR),
    EMPATIA_SELVAGEM(SkillType.EMPATIA_SELVAGEM),
    FURTIVIDADE(SkillType.FURTIVIDADE),
    MEDICINA_E_CURA(SkillType.MEDICINA_E_CURA),
    PERSUASAO(SkillType.PERSUASAO),
    PROFISSAO(SkillType.PROFISSAO),
    CONHECIMENTOS(SkillType.CONHECIMENTOS);

    private final SkillType skillType;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.GNOSE;
    }

    @Override
    public String getDescription() {
        return GnoseAbility.PERITO_TEORICO.getDescription();
    }

    /**
     * Mirrors {@link org.aventyrs.core.skill.SkillCompetencyAbility#resolveAttributeDomain}'s
     * shape for this ability's own {@code AttributeAbility}-based, per-constant-fixed-skill
     * case: {@link AttributeDomain#GNOSE} if the character holds the constant matching
     * skillType, else defaultDomain.
     */
    public static AttributeDomain resolveAttributeDomain(final Collection<AttributeAbility> attributeAbilities, final SkillType skillType, final AttributeDomain defaultDomain) {
        return attributeAbilities.stream()
                .filter(PeritoTeoricoAbility.class::isInstance)
                .map(PeritoTeoricoAbility.class::cast)
                .filter(ability -> ability.getSkillType() == skillType)
                .findFirst()
                .map(ability -> AttributeDomain.GNOSE)
                .orElse(defaultDomain);
    }
}
