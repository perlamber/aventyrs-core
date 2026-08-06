package org.aventyrs.core.race;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Agástias (Elemental: Eletricidade ou Magma) race can do under each rule-set
 * — see {@link AbstractMesticoRace}'s own javadoc for the shared Mestiço Mortal/Físico Mortal
 * mechanism and validation every Mestiço Elemental shares.
 *
 * <p>Unlike the other 5 Mestiços Elementais, Agástias splits into two named linhagens
 * ("Agástias Vulcanos" e "Agástias Trovejantes") whose *reduced* Atributo differs — Vulcanos
 * take -1 Foco, Trovejantes take -1 Força — a third creation-time choice on top of {@code
 * parentRace} itself, captured via {@link Linhagem} rather than a second subclass (it's not a
 * new race, just which half of the same one).
 *
 * <p>Two traits are mechanically real: {@link #getFixedAttributeBonuses()} (+2 Gnose, or +3 if
 * {@code parentRace} also grants Gnose; -1 Foco ou -1 Força per {@link #getLinhagem()},
 * unenforced floor of 1) and Categoria de Tamanho inherited from {@code
 * parentRace.getBaseSizeCategory()} with no offset ("herdam a Categoria de Tamanho de suas
 * contrapartes mortais").
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Idiomas/Longevidade</b> (idiomas do parente mortal; mesma expectativa de vida) — same
 *   "no Language/age concept" gaps as every other race.</li>
 *   <li><b>2 Talentos adicionais</b> (Especialista, substituível por Talento Racial do parente
 *   se houver; +1 Talento Elemental) — same "no Feat catalog" gap as every other race;
 *   "Especialista" has no exact-name match in {@link FeatCategory} — closest is {@link
 *   FeatCategory#PERITO}, an inference, not confirmed (flagged rather than guessed, same "get
 *   the source text before modeling" discipline {@code Range}'s own history taught this
 *   codebase); "Elemental" maps to {@link FeatCategory#ELEMENTAL} directly.</li>
 *   <li><b>Treinamento em Conhecimentos + Especialização Cosmologia</b> — same "no hook for
 *   granting starting Perícia training" gap as every other race.</li>
 *   <li><b>Engenhosidade Divinal</b> (ao criar itens/Equipamentos: -1 GD ou metade do tempo de
 *   criação) — needs an Item/Equipamento entity plus a crafting-time/GD-resolution system,
 *   neither of which exist (same gap {@code ProfissaoCompetencyAbility#FORJA_VULCANA} cites).</li>
 *   <li><b>Especialistas Naturais</b> (-0.5 EXP em Talentos de Especialista; -0.5 EXP na
 *   Graduação de Conhecimentos e Profissão até a 3ª) — two separate gaps: the Feat-cost half
 *   hits the same int-vs-fractional mismatch already flagged on {@code Elfo}'s Conexão com o
 *   Mana, and the Graduação-cost half needs {@code SkillGraduationService#getUpgradeCost} to
 *   take a {@link Race} at all, which it doesn't (same gap Humanos'/{@code Pequenino}'s own
 *   Aprendizado Rápido cite).</li>
 *   <li><b>Magia é Ciência</b> (-0.5 EXP para aprender novas magias) — needs a Magia entity plus
 *   a spell-*learning*-cost system, neither of which exist (same gap {@code Satiro}'s own
 *   Herança Druídica cites).</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "comumente Neutros" is advisory, not a hard rule.
 */
@Getter
public class Agastias extends AbstractMesticoRace {

    public enum Linhagem {
        VULCANO,
        TROVEJANTE
    }

    private static final AttributeDomain PRIMARY_ATTRIBUTE = AttributeDomain.GNOSE;
    private static final int PRIMARY_BONUS = 2;
    private static final int PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT = 3;
    private static final int REDUCED_BONUS = -1;

    private final Linhagem linhagem;

    public Agastias(@NonNull final Race parentRace, @NonNull final Linhagem linhagem) {
        this(parentRace, linhagem, List.of(), List.of());
    }

    public Agastias(@NonNull final Race parentRace, @NonNull final Linhagem linhagem,
                     @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities,
                     @NonNull final List<AttributeAbility> inheritedAttributeAbilities) {
        super(parentRace, inheritedRacialAbilities, inheritedAttributeAbilities);
        this.linhagem = linhagem;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        int primaryBonus = parentGrants(PRIMARY_ATTRIBUTE) ? PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT : PRIMARY_BONUS;
        AttributeDomain reducedAttribute = linhagem == Linhagem.VULCANO ? AttributeDomain.FOCUS : AttributeDomain.STRENGTH;
        return Map.of(PRIMARY_ATTRIBUTE, primaryBonus, reducedAttribute, REDUCED_BONUS);
    }

    @Override
    protected int getSizeCategoryOffset() {
        return 0;
    }
}
