package org.aventyrs.core.race;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Invernal (Elemental: Gelo) race can do under each rule-set — see {@link
 * AbstractMesticoRace}'s own javadoc for the shared Mestiço Mortal/Físico Mortal mechanism and
 * validation every Mestiço Elemental shares.
 *
 * <p>Two traits are mechanically real: {@link #getFixedAttributeBonuses()} (+2 Força, or +3 if
 * {@code parentRace} also grants Força; -1 Carisma, unenforced floor of 1) and Categoria de
 * Tamanho inherited from {@code parentRace.getBaseSizeCategory()} shifted by +1 ("tendo sua
 * Categoria de Tamanho igual à de sua contraparte mortal +1").
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Idiomas/Longevidade</b> (idiomas do parente mortal; curta até a velhice, depois igual
 *   ao parente) — same "no Language/age concept" gaps as every other race; the "curta até a
 *   velhice" clause would also need combat-death tracking this core doesn't have, purely
 *   narrative today either way.</li>
 *   <li><b>2 Talentos adicionais</b> (Duelista, substituível por Talento Racial do parente se
 *   houver; +1 Talento Elemental) — same "no Feat catalog" gap; "Duelista"/"Elemental" map to
 *   {@link org.aventyrs.core.feat.FeatCategory#DUELISTA}/{@link
 *   org.aventyrs.core.feat.FeatCategory#ELEMENTAL} directly.</li>
 *   <li><b>Treinamento em Conhecimentos + Especialização Cosmologia</b> — same "no hook for
 *   granting starting Perícia training" gap as every other race.</li>
 *   <li><b>Carisma Ymiriano</b> (primeiro ataque de cada Turno recebe Corrente de Efeitos
 *   "Carisma Ymiriano": alvo obrigado a atacá-lo por 1 Rodada, efeito de Encantamento, não
 *   repete no mesmo alvo na mesma Cena) — Corrente de Efeitos é sistema inexistente (mesmo gap
 *   já citado em {@code AutocontroleAdvantage#RESOLUTO}), e um efeito de "obrigar a atacar"
 *   precisaria de um sistema de controle de ações do alvo que também não existe.</li>
 *   <li><b>Armamento Gélido</b> (2PA+2PD para tornar uma arma Obra Prima com Material Especial:
 *   Gelo Verdadeiro por 3 Rodadas) — needs an Item/Equipamento entity (same gap {@code
 *   Gigantes}' own Tudo é Frágil and {@code ProfissaoCompetencyAbility#FORJA_VULCANA} cite).</li>
 *   <li><b>Resistência ao Divino</b> (-2, mínimo 1, em efeitos de recuperação de PV exceto
 *   Descansos verdadeiros e Roubo de Vida) — needs a "healing effect" modifier hook this core
 *   doesn't have, scoped to exclude two specific named exceptions this core also has no
 *   classification for.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "normalmente Malignos" is advisory, not a hard rule.
 */
public class Invernal extends AbstractMesticoRace {

    private static final AttributeDomain PRIMARY_ATTRIBUTE = AttributeDomain.STRENGTH;
    private static final AttributeDomain REDUCED_ATTRIBUTE = AttributeDomain.CHARISMA;
    private static final int PRIMARY_BONUS = 2;
    private static final int PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT = 3;
    private static final int REDUCED_BONUS = -1;
    private static final int SIZE_CATEGORY_OFFSET = 1;

    public Invernal(@NonNull final Race parentRace) {
        this(parentRace, List.of(), List.of());
    }

    public Invernal(@NonNull final Race parentRace, @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities,
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
        return SIZE_CATEGORY_OFFSET;
    }
}
