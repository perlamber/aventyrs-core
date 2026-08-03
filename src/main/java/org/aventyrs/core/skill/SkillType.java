package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

/**
 * Identifies each Perícia, used to key a Character's trained skills for O(1) lookup instead
 * of filtering a list. One constant per concrete {@link Skill} implementation, carrying a
 * reference to that skill's {@link SkillExcellency} enum so code holding just a
 * {@code CharacterSkill} (and thus a {@code SkillType}) can resolve its unlocked Excelência
 * tiers generically — see
 * {@code org.aventyrs.core.character.services.ReactionsServiceImpl#getTotalReactions} — plus
 * a factory for a fresh instance of that same {@link Skill}, used by {@link
 * AbstractSkillInteraction#findCharacterSkill} to build the untrained fallback CharacterSkill
 * generically instead of every concrete {@code <Skill>Interaction} hardcoding its own
 * {@code new <Skill>()}.
 */
@Getter
@AllArgsConstructor
public enum SkillType {
    ATTENTION(AttentionExcellency.class, Attention::new),
    ARTES(ArtesExcellency.class, Artes::new),
    ATLETISMO(AtletismoExcellency.class, Atletismo::new),
    DIRIGIR_E_CAVALGAR(DirigirECavalgarExcellency.class, DirigirECavalgar::new),
    DOMINIO_DO_MANA(DominioDoManaExcellency.class, DominioDoMana::new),
    ATAQUE_A_DISTANCIA(AtaqueADistanciaExcellency.class, AtaqueADistancia::new),
    ATAQUE_CORPO_A_CORPO(AtaqueCorpoACorpoExcellency.class, AtaqueCorpoACorpo::new),
    ESQUIVA_E_APARAR(EsquivaEApararExcellency.class, EsquivaEAparar::new),
    EMPATIA_SELVAGEM(EmpatiaSelvagemExcellency.class, EmpatiaSelvagem::new),
    FURTIVIDADE(FurtividadeExcellency.class, Furtividade::new),
    MEDICINA_E_CURA(MedicinaECuraExcellency.class, MedicinaECura::new),
    PERSUASAO(PersuasaoExcellency.class, Persuasao::new),
    PROFISSAO(ProfissaoExcellency.class, Profissao::new),
    CONHECIMENTOS(ConhecimentosExcellency.class, Conhecimentos::new);

    private final Class<? extends SkillExcellency> excellencyClass;
    private final Supplier<Skill> skillFactory;

    /**
     * A fresh, untrained instance of this Perícia's concrete {@link Skill} — e.g. {@code new
     * Artes()} for {@link #ARTES}.
     */
    public Skill newSkillInstance() {
        return skillFactory.get();
    }

    /**
     * Whether this Perícia is one of the Perícias de Ataque the rules text refers to as a
     * group — Ataque à Distância and Ataque Corpo-a-Corpo. The well-typed reference for
     * abilities whose effect branches on that category (e.g.
     * {@link ArtesAprimorarComArteAbility}'s Dano Base branch).
     */
    public boolean isAttackSkill() {
        return this == ATAQUE_A_DISTANCIA || this == ATAQUE_CORPO_A_CORPO;
    }
}
