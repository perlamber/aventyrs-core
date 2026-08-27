package org.aventyrs.core.race;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Aquan (Elemental: Água) race can do under each rule-set — see {@link
 * AbstractMesticoRace}'s own javadoc for the shared Mestiço Mortal/Físico Mortal mechanism and
 * validation every Mestiço Elemental shares.
 *
 * <p>Two traits are mechanically real: {@link #getFixedAttributeBonuses()} (+2 Destreza, or +3
 * if {@code parentRace} also grants Destreza; -1 Vigor, unenforced floor of 1 per the rules
 * text's "até o mínimo de 1" — same restraint as every other unvalidated prerequisite in this
 * codebase) and Categoria de Tamanho inherited from {@code parentRace.getBaseSizeCategory()} with
 * no offset ("tem por referência de tamanho a Raça de seus parentes mortais").
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Idiomas/Longevidade</b> (idiomas do parente mortal; ~50% mais longevo) — same "no
 *   Language/age concept" gaps as every other race.</li>
 *   <li><b>2 Talentos adicionais</b> (Mobilidade, substituível por Talento Racial do parente se
 *   houver; + 1 Talento Elemental) — same "no Feat catalog" gap as every other race;
 *   "Mobilidade"/"Elemental" map to {@link org.aventyrs.core.feat.FeatCategory#MOBILIDADE}/{@link
 *   org.aventyrs.core.feat.FeatCategory#ELEMENTAL} directly.</li>
 *   <li><b>Treinamento em Conhecimentos + Especialização Cosmologia</b> — {@code
 *   ConhecimentosSpecialization.COSMOLOGIA} already exists as data, blocked only on {@link
 *   Race}'s usual "no hook for granting starting Perícia training" gap.</li>
 *   <li><b>Corpo Maleável</b> (1PD, Ação Livre, swap Força↔Destreza on Perícia and Dano rolls
 *   for 1 Rodada, 2 se em água) — the *unconditional* Força/Destreza substitution mechanism
 *   already exists ({@code SkillCompetencyAbility#getSubstituteAttributeDomain()}), but this is
 *   an *activated*, PD-costed, Ação-Livre-triggered swap, not an unconditional one — same "no
 *   standalone activation trigger outside of a roll" gap {@code Fada}/{@code Furia}'s own
 *   Feromônio Encantador cites — plus an environment concept (em água ou não) this core doesn't
 *   track either.</li>
 *   <li><b>Água é Vida</b> (+2PV em efeitos de Cura; mimetizar Regeneração Maior a 2PD) — needs
 *   a Magia entity ({@code org.aventyrs.core.magic.SpellCastingService}'s own "No Magia
 *   entity/list exists yet") plus a "Cura effect" modifier hook this core doesn't have.</li>
 *   <li><b>Dependência Aquática</b> (+1/-1 Multiplicador de PV conforme contato com água,
 *   Movimento Base de Natação, +2UD Movimento) — same environment-tracking gap as Corpo
 *   Maleável; {@code MovementService} now aggregates a real "Movimento Base" stat (same fix
 *   {@code Pequenino}'s own Ligeiro/Sempre Veloz cites) for the flat +2UD clause, but Aquan
 *   still has no {@code *RacialAbility} catalog constant to carry it through, and swimming is
 *   a different sub-stat that stat doesn't track (same gap {@code AtletismoCompetencyAbility
 *   .ANFIBIO} cites).</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "imprevisíveis... mas normalmente Neutros" is advisory, not a hard rule.
 */
public class Aquan extends AbstractMesticoRace {

    private static final AttributeDomain PRIMARY_ATTRIBUTE = AttributeDomain.DEXTERITY;
    private static final AttributeDomain REDUCED_ATTRIBUTE = AttributeDomain.VIGOR;
    private static final int PRIMARY_BONUS = 2;
    private static final int PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT = 3;
    private static final int REDUCED_BONUS = -1;

    public Aquan(@NonNull final Race parentRace) {
        this(parentRace, List.of(), List.of());
    }

    public Aquan(@NonNull final Race parentRace, @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities,
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
