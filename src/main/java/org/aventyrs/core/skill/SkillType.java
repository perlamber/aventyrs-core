package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.artes.Artes;
import org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility;
import org.aventyrs.core.skill.artes.ArtesExcellency;
import org.aventyrs.core.skill.artes.ArtesInteraction;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistancia;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaExcellency;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaInteraction;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpo;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoExcellency;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoInteraction;
import org.aventyrs.core.skill.atletismo.Atletismo;
import org.aventyrs.core.skill.atletismo.AtletismoExcellency;
import org.aventyrs.core.skill.atletismo.AtletismoInteraction;
import org.aventyrs.core.skill.attention.Attention;
import org.aventyrs.core.skill.attention.AttentionExcellency;
import org.aventyrs.core.skill.attention.AttentionInteraction;
import org.aventyrs.core.skill.conhecimentos.Conhecimentos;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosExcellency;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosInteraction;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgar;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarExcellency;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarInteraction;
import org.aventyrs.core.skill.dominiodomana.DominioDoMana;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaExcellency;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagem;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemExcellency;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemInteraction;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEAparar;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararExcellency;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararInteraction;
import org.aventyrs.core.skill.furtividade.Furtividade;
import org.aventyrs.core.skill.furtividade.FurtividadeExcellency;
import org.aventyrs.core.skill.furtividade.FurtividadeInteraction;
import org.aventyrs.core.skill.medicinaecura.MedicinaECura;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraExcellency;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraInteraction;
import org.aventyrs.core.skill.persuasao.Persuasao;
import org.aventyrs.core.skill.persuasao.PersuasaoExcellency;
import org.aventyrs.core.skill.persuasao.PersuasaoInteraction;
import org.aventyrs.core.skill.profissao.Profissao;
import org.aventyrs.core.skill.profissao.ProfissaoExcellency;
import org.aventyrs.core.skill.profissao.ProfissaoInteraction;

import java.util.function.Supplier;

import static org.aventyrs.core.util.TranslatableMessages.UNKNOWN_SKILL_TYPE;

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
 * {@code new <Skill>()} — this Perícia's own {@link ModifierType}, for bonuses scoped to
 * just this one Perícia's rolls (see {@link #getRollBonusType()}) — and a factory for a fresh
 * instance of that same Perícia's {@code <Skill>Interaction}, so a caller holding only a
 * {@code SkillType} (e.g. from an incoming API request) can obtain the right concrete
 * {@link AbstractSkillInteraction} without a giant hand-written switch — see
 * {@link SkillInteractionFactory}.
 */
@Getter
@AllArgsConstructor
public enum SkillType {
    ATTENTION(AttentionExcellency.class, Attention::new, ModifierType.ATTENTION_ROLL_BONUS, AttentionInteraction::new),
    ARTES(ArtesExcellency.class, Artes::new, ModifierType.ARTES_ROLL_BONUS, ArtesInteraction::new),
    ATLETISMO(AtletismoExcellency.class, Atletismo::new, ModifierType.ATLETISMO_ROLL_BONUS, AtletismoInteraction::new),
    DIRIGIR_E_CAVALGAR(DirigirECavalgarExcellency.class, DirigirECavalgar::new, ModifierType.DIRIGIR_E_CAVALGAR_ROLL_BONUS, DirigirECavalgarInteraction::new),
    DOMINIO_DO_MANA(DominioDoManaExcellency.class, DominioDoMana::new, ModifierType.DOMINIO_DO_MANA_ROLL_BONUS, DominioDoManaInteraction::new),
    ATAQUE_A_DISTANCIA(AtaqueADistanciaExcellency.class, AtaqueADistancia::new, ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, AtaqueADistanciaInteraction::new),
    ATAQUE_CORPO_A_CORPO(AtaqueCorpoACorpoExcellency.class, AtaqueCorpoACorpo::new, ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, AtaqueCorpoACorpoInteraction::new),
    ESQUIVA_E_APARAR(EsquivaEApararExcellency.class, EsquivaEAparar::new, ModifierType.ESQUIVA_E_APARAR_ROLL_BONUS, EsquivaEApararInteraction::new),
    EMPATIA_SELVAGEM(EmpatiaSelvagemExcellency.class, EmpatiaSelvagem::new, ModifierType.EMPATIA_SELVAGEM_ROLL_BONUS, EmpatiaSelvagemInteraction::new),
    FURTIVIDADE(FurtividadeExcellency.class, Furtividade::new, ModifierType.FURTIVIDADE_ROLL_BONUS, FurtividadeInteraction::new),
    MEDICINA_E_CURA(MedicinaECuraExcellency.class, MedicinaECura::new, ModifierType.MEDICINA_E_CURA_ROLL_BONUS, MedicinaECuraInteraction::new),
    PERSUASAO(PersuasaoExcellency.class, Persuasao::new, ModifierType.PERSUASAO_ROLL_BONUS, PersuasaoInteraction::new),
    PROFISSAO(ProfissaoExcellency.class, Profissao::new, ModifierType.PROFISSAO_ROLL_BONUS, ProfissaoInteraction::new),
    CONHECIMENTOS(ConhecimentosExcellency.class, Conhecimentos::new, ModifierType.CONHECIMENTOS_ROLL_BONUS, ConhecimentosInteraction::new);

    private final Class<? extends SkillExcellency> excellencyClass;
    private final Supplier<Skill> skillFactory;

    /**
     * This Perícia's own {@link ModifierType} — for a bonus that should apply only to this
     * one Perícia's rolls, as opposed to {@code ModifierType#SKILL_ROLL_BONUS}, which every
     * Perícia's roll includes. {@link AbstractSkillInteraction} sums both.
     */
    private final ModifierType rollBonusType;

    private final Supplier<AbstractSkillInteraction> interactionFactory;

    /**
     * A fresh, untrained instance of this Perícia's concrete {@link Skill} — e.g. {@code new
     * Artes()} for {@link #ARTES}.
     */
    public Skill newSkillInstance() {
        return skillFactory.get();
    }

    /**
     * A fresh instance of this Perícia's own {@code <Skill>Interaction} — e.g. {@code new
     * ArtesInteraction()} for {@link #ARTES} — built via its default (no-arg) constructor, the
     * same one every concrete {@code <Skill>Interaction} already exposes. Prefer
     * {@link SkillInteractionFactory#create(SkillType)} at call sites; this method is what it
     * delegates to.
     */
    public AbstractSkillInteraction newInteraction() {
        return interactionFactory.get();
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

    /**
     * Resolves a SkillType by name, matching {@link #name()} case-insensitively — the entry
     * point for a caller that only has a raw String in hand (e.g. an API layer deserializing
     * an incoming {@link SkillRollRequest}) and needs to reach the right constant, rather than
     * falling back to {@link #valueOf(String)}'s case-sensitive, unchecked-exception behavior.
     * A genuine system boundary (input from outside this core), unlike internal invariants
     * this codebase otherwise trusts — throws {@link IllegalOperationException} for a name
     * that doesn't match any constant instead of silently returning {@code null}.
     */
    public static SkillType fromString(final String value) {
        for (SkillType skillType : values()) {
            if (skillType.name().equalsIgnoreCase(value)) {
                return skillType;
            }
        }
        throw new IllegalOperationException(UNKNOWN_SKILL_TYPE);
    }
}
