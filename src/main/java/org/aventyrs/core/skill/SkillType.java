package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Identifies each Perícia, used to key a Character's trained skills for O(1) lookup instead
 * of filtering a list. One constant per concrete {@link Skill} implementation, carrying a
 * reference to that skill's {@link SkillExcellency} enum so code holding just a
 * {@code CharacterSkill} (and thus a {@code SkillType}) can resolve its unlocked Excelência
 * tiers generically — see
 * {@code org.aventyrs.core.character.services.ReactionsServiceImpl#getTotalReactions}.
 */
@Getter
@AllArgsConstructor
public enum SkillType {
    ATTENTION(AttentionExcellency.class),
    ARTES(ArtesExcellency.class),
    ATLETISMO(AtletismoExcellency.class),
    DIRIGIR_E_CAVALGAR(DirigirECavalgarExcellency.class),
    DOMINIO_DO_MANA(DominioDoManaExcellency.class),
    ATAQUE_A_DISTANCIA(AtaqueADistanciaExcellency.class),
    ATAQUE_CORPO_A_CORPO(AtaqueCorpoACorpoExcellency.class),
    ESQUIVA_E_APARAR(EsquivaEApararExcellency.class),
    EMPATIA_SELVAGEM(EmpatiaSelvagemExcellency.class),
    FURTIVIDADE(FurtividadeExcellency.class),
    MEDICINA_E_CURA(MedicinaECuraExcellency.class),
    PERSUASAO(PersuasaoExcellency.class);

    private final Class<? extends SkillExcellency> excellencyClass;
}
