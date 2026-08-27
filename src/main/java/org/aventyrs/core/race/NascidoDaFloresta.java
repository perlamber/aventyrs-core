package org.aventyrs.core.race;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_PARENT_RACE;

/**
 * Defines what the Nascido da Floresta race can do under each rule-set — a rare Feérico/
 * Humanoide crossbred, structurally identical to the 6 Mestiços Elementais ({@link
 * AbstractMesticoRace}'s own javadoc documents the shared Mestiço Mortal/Físico Mortal
 * mechanism this race reuses verbatim, both traits' rules text matching word-for-word), except
 * the chosen {@code parentRace} must specifically be {@link CreatureType#HUMANOIDE} ("você deve
 * escolher uma raça Humanoide que não seja mestiça") — narrower than the Elementais' "Feérica,
 * Humanoide ou Monstruosa" — enforced as one extra check after {@code super(...)}, the same
 * "delegate the shared checks, add one more" shape {@code AbstractMesticoRace}'s own {@code
 * parentRace.isMestico()} rejection already gives every subclass for free.
 *
 * <p>Because {@code parentRace} can only ever be Humanoide, {@link #getCreatureType()} — left at
 * {@code AbstractMesticoRace}'s own delegating default — always resolves to {@code
 * CreatureType.HUMANOIDE} too, the same outcome {@code MeioElfo} reaches by fixing the value
 * directly instead (its constructor enforces the identical Humanoide-only constraint on its own
 * {@code parentRace}).
 *
 * <p>Two traits are mechanically real:
 * <ul>
 *   <li><b>{@link #getFixedAttributeBonuses()}</b> — +1 Carisma, +1 Foco, unconditionally (this
 *   race's own Feérico nature, not derived from {@code parentRace} at all — unlike the
 *   Elementais' "+2, ou +3 se o parente conceder" shape, nothing here reads {@code
 *   parentRace.getFixedAttributeBonuses()}, so {@code parentGrants(...)} is never called).</li>
 *   <li><b>Categoria de Tamanho</b> ("herdam a Categoria de Tamanho de seus parentes
 *   não-feéricos") — {@link #getSizeCategoryOffset()} is 0, same shape as Agástias/Aquan/
 *   Flaminídeo.</li>
 * </ul>
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Idiomas</b> (Arcano dialeto Feérico, ou os idiomas do parente não-feérico à escolha)
 *   — same "no Language/Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> ("vida curta, menos de meio século") — same "no age/lifespan concept"
 *   gap as every other race; purely narrative today.</li>
 *   <li><b>2 Talentos adicionais</b> (Metamágicos, Feéricos ou Elementais) — same "no Feat
 *   catalog, no {@code Character.feats} list, no hook for granting extra Talento slots" gap as
 *   every other race's free Talentos; {@code FeatCategory#METAMAGICO}/{@code #FERRICO}/{@code
 *   #ELEMENTAL} already exist as categories to eventually restrict the grant to.</li>
 *   <li><b>Considerados Elementais para pré-requisitos de Talentos</b> — an unenforced-
 *   prerequisite classification, same restraint this codebase already applies to every "Requer N
 *   Graduações"-style clause; nothing to build even once a Feat catalog exists, since
 *   prerequisite legality is never validated here.</li>
 *   <li><b>Especialização/Habilidade de Competência adicional herdada do parente</b> (limitado a
 *   1, se o parente concede) — same "{@link Race} has no hook for granting starting Perícia
 *   training" gap as every other race's free Especializações (e.g. {@code Elfo}'s Origem
 *   Mística, {@code MeioElfo}'s own identical clause).</li>
 *   <li><b>Conexão com o Mana</b> (Talentos Metamágicos custam 2.5 EXP) — identical trait/name to
 *   {@code Elfo}'s/{@code Furia}'s/{@code MeioElfo}'s own Conexão com o Mana; same
 *   int-vs-fractional mismatch already flagged there ({@link
 *   #getNewFeatCost(org.aventyrs.core.feat.FeatCategory)} returns a plain {@code int}).</li>
 *   <li><b>Domínio da Natureza</b> (Magias conjuradas contam como Naturais além de seus tipos,
 *   exceto Primordiais/Umbrais que ficam impossíveis de aprender; custam 0.5 EXP a menos) —
 *   needs a Magia entity with a concrete catalog and spell-type classification ({@code
 *   org.aventyrs.core.magic.SpellCastingService}'s own "No Magia entity/list exists yet"), plus
 *   a spell-*learning*-cost system this core has no equivalent of — same missing pieces {@code
 *   Furia}'s own Magia Natural cites, plus another 0.5-EXP fractional-cost case beyond what
 *   {@code getNewFeatCost} alone would need.</li>
 *   <li><b>Imunidade a Magias</b> (immune to most spells, Primordiais/Umbrais excepted; still
 *   affected by indirect magical effects like enchanted weapons) — same missing Encantamento/
 *   spell-*type*-classification concept {@code Furia}'s own Imunidade a Encantamentos cites,
 *   broadened here to "most magic" rather than just Encantamentos specifically — doesn't change
 *   which piece is missing, only how much of it this trait would eventually need classified.</li>
 *   <li><b>Feromônio Encantador de Humanoides</b> (2PD, Ação Livre, reduz o GD em -1 nível de
 *   rolagens de Persuasão ou Artes contra alvos em Distância Curta, por 1 Rodada; um efeito de
 *   Encantamento) — a GD-reduction variant of {@code Fada}'s/{@code Furia}'s own Feromônio
 *   Encantador (which grants Vantagem, a roll bonus, instead of reducing the target's GD) — the
 *   variance doesn't change which piece is missing: spending PD ({@code
 *   org.aventyrs.core.sheet.CombatantSheet#spendDeterminationPoints}), proximity ({@code
 *   org.aventyrs.core.scene.SceneContext#getDistanceTo}/{@code Range#DISTANCIA_CURTA}), and a
 *   Rodada-scoped duration ({@code CombatantSheet#grantTemporaryBonus}'s {@code rounds}
 *   parameter) are each individually real, but nothing in this core can combine them into a
 *   standalone, opt-in Ação Livre activation — every existing trigger for a granted effect is the
 *   *result* of a skill roll ({@code ArtesCompetencyAbility#DOM_BARDICO}), not a standalone
 *   activation outside of any roll. Nor do {@code PersuasaoInteraction}/{@code ArtesInteraction}
 *   take an {@code attackTarget}-style parameter to check "against a target at Distância Curta"
 *   against, the same gap {@code Furia}'s/{@code Satiro}'s own Feromônio Encantador already
 *   cites. Being an "efeito de Encantamento" also needs the same missing Encantamento-effect
 *   classification Imunidade a Magias above needs.</li>
 * </ul>
 *
 * <p>None of the Características Raciais above fit {@code SkillCompetencyAbility}'s shape well
 * enough today to catalog in a {@code NascidoDaFlorestaRacialAbility} enum — so this race grants
 * no fixed racial abilities of its own; {@link #getRacialAbilities()} (inherited from {@link
 * AbstractMesticoRace}) only ever returns whichever up-to-2 abilities were inherited from {@code
 * parentRace} via Mestiço Humanoide.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "Neutros ou Bondosos... apenas quando passam por experiências traumáticas" is advisory, not a
 * hard rule.
 */
public class NascidoDaFloresta extends AbstractMesticoRace {

    private static final int SIZE_CATEGORY_OFFSET = 0;

    public NascidoDaFloresta(@NonNull final Race parentRace) {
        this(parentRace, List.of(), List.of());
    }

    public NascidoDaFloresta(@NonNull final Race parentRace,
                              @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities,
                              @NonNull final List<AttributeAbility> inheritedAttributeAbilities) {
        super(parentRace, inheritedRacialAbilities, inheritedAttributeAbilities);
        if (parentRace.getCreatureType() != CreatureType.HUMANOIDE) {
            throw new IllegalOperationException(INVALID_PARENT_RACE);
        }
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.CHARISMA, 1, AttributeDomain.FOCUS, 1);
    }

    @Override
    protected int getSizeCategoryOffset() {
        return SIZE_CATEGORY_OFFSET;
    }
}
