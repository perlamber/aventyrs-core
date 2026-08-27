package org.aventyrs.core.race;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Flaminídeo (Elemental: Fogo) race can do under each rule-set — see {@link
 * AbstractMesticoRace}'s own javadoc for the shared Mestiço Mortal/Físico Mortal mechanism and
 * validation every Mestiço Elemental shares.
 *
 * <p>Two traits are mechanically real: {@link #getFixedAttributeBonuses()} (+2 Instinto, or +3
 * if {@code parentRace} also grants Instinto; -1 Foco, unenforced floor of 1) and Categoria de
 * Tamanho inherited from {@code parentRace.getBaseSizeCategory()} with no offset ("tem por
 * referência de tamanho a Raça de seus parentes mortais").
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Idiomas/Longevidade</b> (idiomas do parente mortal; mesma expectativa de vida) — same
 *   "no Language/age concept" gaps as every other race.</li>
 *   <li><b>Talentos</b> (Escudeiro, substituível por Talento Racial do parente se houver; +1
 *   Talento Elemental) — same "no Feat catalog" gap; "Escudeiro"/"Elemental" map to {@link
 *   org.aventyrs.core.feat.FeatCategory#ESCUDEIRO}/{@link
 *   org.aventyrs.core.feat.FeatCategory#ELEMENTAL} directly. Same apparent "Talento Elemental"
 *   cited twice redundancy as {@code Dolos}' own text.</li>
 *   <li><b>Treinamento em Conhecimentos + Especialização Cosmologia</b> — same "no hook for
 *   granting starting Perícia training" gap as every other race.</li>
 *   <li><b>Conjuração Limitada</b> (só pode conjurar magias Divinas, Elementais de Fogo/
 *   Eletricidade/Magma, ou Primordiais) — needs a Magia entity with a school/element
 *   classification, which doesn't exist ({@code SpellCastingService}'s own "No Magia entity/
 *   list exists yet").</li>
 *   <li><b>Chamas da Justiça</b> (Vantagem em toda rolagem de Perícia contra Monstros e
 *   personagens Malignos) — needs a target-classification concept (creature type, alinhamento)
 *   this core's roll machinery has no way to check per-roll; same "doesn't track what a roll is
 *   *for*" gap documented across this codebase, here scoped to the *target's* classification
 *   rather than the roll's purpose.</li>
 *   <li><b>Aventyr Incandescente</b> (1d6 extra ao ativar Habilidade de Título com Custo, 5-6
 *   concede aura +2PV a aliados adjacentes por 2 Rodadas, Margem Crítica Menor +1 e Corrente de
 *   Efeitos "Aventyr Incandescente" contra Monstros/Malignos) — {@code
 *   org.aventyrs.core.title.AventyrTitleAbility#getPDCost()} now models "Habilidade de Título
 *   com Custo" for real (see {@code Santo}'s own catalog), but this trait still needs an
 *   "ao ativar" trigger hook (no mechanism anywhere fires when a Título ability is actually
 *   activated), this core deliberately never rolls its own dice, and Corrente de Efeitos is
 *   an entirely unbuilt system (same gap {@code AutocontroleAdvantage#RESOLUTO} already
 *   cites).</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "na grande maioria... Bondosos" is advisory, not a hard rule.
 */
public class Flaminideo extends AbstractMesticoRace {

    private static final AttributeDomain PRIMARY_ATTRIBUTE = AttributeDomain.INSTINCT;
    private static final AttributeDomain REDUCED_ATTRIBUTE = AttributeDomain.FOCUS;
    private static final int PRIMARY_BONUS = 2;
    private static final int PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT = 3;
    private static final int REDUCED_BONUS = -1;

    public Flaminideo(@NonNull final Race parentRace) {
        this(parentRace, List.of(), List.of());
    }

    public Flaminideo(@NonNull final Race parentRace, @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities,
                       @NonNull final List<AttributeAbility> inheritedAttributeAbilities) {
        super(parentRace, inheritedRacialAbilities, inheritedAttributeAbilities);
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        int primaryBonus = parentGrants(PRIMARY_ATTRIBUTE) ? PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT : PRIMARY_BONUS;
        return Map.of(PRIMARY_ATTRIBUTE, primaryBonus, REDUCED_ATTRIBUTE, REDUCED_BONUS);
    }

    @Override
    protected int getSizeCategoryOffset() {
        return 0;
    }
}
