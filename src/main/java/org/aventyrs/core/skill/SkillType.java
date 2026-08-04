package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.ModifierType;

import java.util.function.Supplier;

/**
 * Identifies each Perícia, used to key a Character's trained skills for O(1) lookup instead
 * of filtering a list. One constant per concrete {@link Skill} implementation, carrying a
 * reference to that skill's {@link SkillExcellency} enum so code holding just a
 * {@code CharacterSkill} (and thus a {@code SkillType}) can resolve its unlocked Excelência
 * tiers generically — see
 * {@code org.aventyrs.core.character.services.ReactionsServiceImpl#getTotalReactions} — a
 * factory for a fresh instance of that same {@link Skill}, used by {@link
 * AbstractSkillInteraction#findCharacterSkill} to build the untrained fallback CharacterSkill
 * generically instead of every concrete {@code <Skill>Interaction} hardcoding its own
 * {@code new <Skill>()} — and this Perícia's own {@link ModifierType}, for bonuses scoped to
 * just this one Perícia's rolls (see {@link #getRollBonusType()}).
 */
@Getter
@AllArgsConstructor
public enum SkillType {
    ATTENTION(AttentionExcellency.class, Attention::new, ModifierType.ATTENTION_ROLL_BONUS),
    ARTES(ArtesExcellency.class, Artes::new, ModifierType.ARTES_ROLL_BONUS),
    ATLETISMO(AtletismoExcellency.class, Atletismo::new, ModifierType.ATLETISMO_ROLL_BONUS),
    DIRIGIR_E_CAVALGAR(DirigirECavalgarExcellency.class, DirigirECavalgar::new, ModifierType.DIRIGIR_E_CAVALGAR_ROLL_BONUS),
    DOMINIO_DO_MANA(DominioDoManaExcellency.class, DominioDoMana::new, ModifierType.DOMINIO_DO_MANA_ROLL_BONUS),
    ATAQUE_A_DISTANCIA(AtaqueADistanciaExcellency.class, AtaqueADistancia::new, ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS),
    ATAQUE_CORPO_A_CORPO(AtaqueCorpoACorpoExcellency.class, AtaqueCorpoACorpo::new, ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS),
    ESQUIVA_E_APARAR(EsquivaEApararExcellency.class, EsquivaEAparar::new, ModifierType.ESQUIVA_E_APARAR_ROLL_BONUS),
    EMPATIA_SELVAGEM(EmpatiaSelvagemExcellency.class, EmpatiaSelvagem::new, ModifierType.EMPATIA_SELVAGEM_ROLL_BONUS),
    FURTIVIDADE(FurtividadeExcellency.class, Furtividade::new, ModifierType.FURTIVIDADE_ROLL_BONUS),
    MEDICINA_E_CURA(MedicinaECuraExcellency.class, MedicinaECura::new, ModifierType.MEDICINA_E_CURA_ROLL_BONUS),
    PERSUASAO(PersuasaoExcellency.class, Persuasao::new, ModifierType.PERSUASAO_ROLL_BONUS),
    PROFISSAO(ProfissaoExcellency.class, Profissao::new, ModifierType.PROFISSAO_ROLL_BONUS),
    CONHECIMENTOS(ConhecimentosExcellency.class, Conhecimentos::new, ModifierType.CONHECIMENTOS_ROLL_BONUS);

    private final Class<? extends SkillExcellency> excellencyClass;
    private final Supplier<Skill> skillFactory;

    /**
     * This Perícia's own {@link ModifierType} — for a bonus that should apply only to this
     * one Perícia's rolls, as opposed to {@code ModifierType#SKILL_ROLL_BONUS}, which every
     * Perícia's roll includes. {@link AbstractSkillInteraction} sums both.
     */
    private final ModifierType rollBonusType;

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
