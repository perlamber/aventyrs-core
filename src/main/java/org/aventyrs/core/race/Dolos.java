package org.aventyrs.core.race;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Dólos (Elemental: Vento) race can do under each rule-set — see {@link
 * AbstractMesticoRace}'s own javadoc for the shared Mestiço Mortal/Físico Mortal mechanism and
 * validation every Mestiço Elemental shares.
 *
 * <p>Two traits are mechanically real: {@link #getFixedAttributeBonuses()} (+2 Carisma, or +3
 * if {@code parentRace} also grants Carisma; -1 Vigor, unenforced floor of 1) and Categoria de
 * Tamanho inherited from {@code parentRace.getBaseSizeCategory()} shifted by -1 ("a sua
 * Categoria de Tamanho é igual a de seus parentes não-elementais -1").
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Idiomas/Longevidade</b> (idiomas do parente mortal; mesma expectativa de vida) — same
 *   "no Language/age concept" gaps as every other race.</li>
 *   <li><b>Talentos</b> (Assassino, substituível por Talento Racial do parente se houver; +1
 *   Talento Elemental) — same "no Feat catalog" gap; "Assassino"/"Elemental" map to {@link
 *   org.aventyrs.core.feat.FeatCategory#ASSASSINO}/{@link
 *   org.aventyrs.core.feat.FeatCategory#ELEMENTAL} directly. The source text's own wording
 *   ("O segundo Talento adicional deve ser escolhido dentre os Talentos Elementais.
 *   Adicionalmente recebem 1 Talento Elemental") reads as citing a Talento Elemental twice —
 *   flagged as an apparent redundancy in the rules text, not resolved by guessing which one is
 *   correct (same "get the source text before modeling" discipline {@code Gnomo}'s own
 *   "duas vs. três Perícias" inconsistency already applies).</li>
 *   <li><b>Treinamento em Persuasão OU Conhecimentos</b> (+ Especialização Cosmologia se
 *   Conhecimentos escolhido) — same "no hook for granting starting Perícia training" gap as
 *   every other race, compounded here by a creation-time choice between *which* Perícia to
 *   train (not just which Especialização) that this core also has no hook for.</li>
 *   <li><b>Como o Vento</b> (+2UD Movimento Base; Movimento Base de Voo por 1PA+3PD, duração
 *   1d6+Carisma Rodadas normalmente ou 1 Rodada em cenas estressantes) — {@code
 *   MovementService} now aggregates a real "Movimento Base" stat (same fix {@code Pequenino}'s
 *   own Ligeiro/Sempre Veloz cites), but Dolos still has no {@code *RacialAbility} catalog
 *   constant to carry the flat +2UD through it, flight is a different sub-stat that stat
 *   doesn't track (same gap {@code AtletismoCompetencyAbility.ALPINISTA_VELOZ}/{@code ANFIBIO}
 *   cite for climbing/swimming), and this whole clause still has no activated-ability-outside-
 *   a-roll trigger (same gap {@code Aquan}'s own Corpo Maleável cites) — this core also
 *   deliberately never rolls its own dice (the "1d6" duration is a caller's job, per the
 *   {@code skill} package-info) nor distinguishes a "cena estressante" from any other.</li>
 *   <li><b>Saúde Forte em Corpo Frágil</b> (-1 Multiplicador de PV; imunidade a doenças
 *   mundanas) — the PV-multiplier malus hits the same {@code HitPointsServiceImpl
 *   #getLifeMultiplier} "doesn't scan {@code race.getRacialAbilities()}" gap {@code Colosso}'s
 *   own Resistente ao AEther cites; disease immunity needs a status-effect/disease concept this
 *   core doesn't have at all.</li>
 *   <li><b>Forma do Vendaval</b> (1PA+3PD, 1 Rodada, imune a ataques/habilidades não-mágicos
 *   sem dano, mas incapaz de usar Perícias exceto Atenção/Conhecimentos) — same activation-
 *   trigger gap as Como o Vento, plus an "immune to non-magical, non-damaging effects" and a
 *   "Perícia usage restricted to a named subset for its duration" concept, neither of which
 *   exist.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "entre Neutros e Malignos" is advisory, not a hard rule.
 */
public class Dolos extends AbstractMesticoRace {

    private static final AttributeDomain PRIMARY_ATTRIBUTE = AttributeDomain.CHARISMA;
    private static final AttributeDomain REDUCED_ATTRIBUTE = AttributeDomain.VIGOR;
    private static final int PRIMARY_BONUS = 2;
    private static final int PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT = 3;
    private static final int REDUCED_BONUS = -1;
    private static final int SIZE_CATEGORY_OFFSET = -1;

    public Dolos(@NonNull final Race parentRace) {
        this(parentRace, List.of(), List.of());
    }

    public Dolos(@NonNull final Race parentRace, @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities,
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
