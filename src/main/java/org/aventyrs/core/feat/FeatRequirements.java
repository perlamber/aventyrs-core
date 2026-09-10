package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Alignment;
import org.aventyrs.core.character.Deity;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleArchetype;

import lombok.Builder;
import lombok.Singular;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Every prerequisite a {@link Feat} can name. The record itself is one <b>AND group</b> — an unset
 * clause never blocks eligibility, and every set one must hold at once — and {@link #anyOf()}
 * hangs a disjunction off it: when non-empty, at least one of those nested groups must hold
 * <em>as well as</em> every clause on this record. Checked by {@link Feat#isEligible}.
 *
 * <p>A Talento's Pré-requisito is one of only two places in this core where a "Requer N …"
 * clause is real, enforced data rather than an unenforced comment (the other is {@code
 * org.aventyrs.core.title.AventyrTitleAbility}) — see CLAUDE.md's "Possession is validated;
 * eligibility mostly isn't" restraint for why everything else stays prose.
 *
 * <p><b>Some clauses need a {@code CharacterSheet}</b> ({@link #requiredFame()}, {@link
 * #requiredTotalExperience()}) because the value lives there rather than on {@code Character}.
 * Those are checked only by {@link Feat#isEligible(org.aventyrs.core.character.Character,
 * org.aventyrs.core.sheet.CharacterSheet)}; the sheet-less overload <em>skips</em> them, which
 * keeps a preview listing looser than the real gate rather than stricter. {@code
 * FeatService#grantFeat} always has the sheet, so the authoritative check never skips anything.
 *
 * @param attributeDomain           which Attribute {@code requiredAttributeValue} tests, unset
 *                                  to skip. Tests {@code base}, not {@code getTotal()} —
 *                                  acquiring a Talento is gated on what the character
 *                                  personally invested in, deliberately unlike {@code
 *                                  org.aventyrs.core.item.ItemRequirements}.
 * @param requiredAttributeValue    minimum {@code base} of {@code attributeDomain}.
 * @param maximumAttributeDomain    which Attribute {@code maximumAttributeValue} caps, unset to
 *                                  skip — "Força igual ou inferior à 2" ({@code
 *                                  DestinoFeat#APARENCIA_INOFENSIVA}). A separate pair from the
 *                                  minimum above because a Talento naming both names <i>two
 *                                  different</i> Atributos ("Carisma 3 e Força ≤ 2"). One maximum
 *                                  per group, the same single-clause shape the minimum has; no
 *                                  authored Talento names two.
 * @param maximumAttributeValue     the largest {@code base} of {@code maximumAttributeDomain}
 *                                  still eligible. Inclusive ("igual ou inferior").
 * @param requiredAnyAttributeValue when > 0, <i>some</i> Attribute must have at least this
 *                                  {@code base} — "Atributo 3 ou Superior", "Qualquer Atributo
 *                                  com Valor Base 5", which name no particular domain.
 * @param requiredAnyRacialAttributeValue when > 0, some Attribute that actually <i>receives a
 *                                  Bônus Racial</i> ({@code AttributeValue#getRacialBonus() > 0})
 *                                  must have at least this {@code base} — {@code
 *                                  MonstruosoFeat#ALFA}'s "qualquer atributo que receba bônus
 *                                  Racial com valor Base igual à 5". A strictly narrower clause
 *                                  than the one above, not a substitute for it.
 * @param maximumEgoDomain          which Ego {@code maximumEgoValue} caps, unset to skip —
 *                                  "Iniciativa 2 ou inferior" ({@code
 *                                  PeritoFeat#ANALISTA_TATICO}). An {@link EgoDomain}, not an
 *                                  {@link AttributeDomain}: Iniciativa is an Ego in this ruleset.
 *                                  Tests {@code EgoValue#getBase()}, matching the Attribute
 *                                  clauses' invested-value reading.
 * @param maximumEgoValue           the largest {@code base} of {@code maximumEgoDomain} still
 *                                  eligible. Inclusive.
 * @param requiredSkillType         which Perícia {@code requiredSkillGraduation} tests, unset
 *                                  to skip. An untrained Perícia reads as Graduação 0.
 * @param requiredSkillGraduation   minimum Graduação in {@code requiredSkillType}. A clause
 *                                  reading merely "Treinamento em X", with no number, is a
 *                                  Graduação of 1.
 * @param requiredFeats             Talentos that must <b>all</b> already be held — add them one
 *                                  at a time with the builder's {@code requiredFeat(…)}. Plural
 *                                  because four Talentos name two ("Reparação Elemental
 *                                  <i>e</i> Resistência Elemental Superior"). Compared through
 *                                  {@code Feat#catalogEntry()}, so a choice-carrying instance
 *                                  counts as the constant it wraps.
 * @param forbiddenFeats            Talentos that must <b>not</b> be held — the negative form,
 *                                  which is what an exclusion clause needs ("Apenas Pequeninos
 *                                  que não possuam o Talento 'Linhagem de Flora'", and each half
 *                                  of a mutually-exclusive pair naming the other). Also compared
 *                                  through {@code catalogEntry()}. Note this is <em>possession</em>
 *                                  only: an exclusion phrased as a cap on how many of a family may
 *                                  be held at once ({@code ArtesMarciaisFeat}'s Dominar styles)
 *                                  is still an {@code isEligible} override on that enum.
 * @param requiredSkillTraits       Habilidades de Competência and/or Especializações that must
 *                                  all already be held — "4 Graduações em Dirigir e Cavalgar e a
 *                                  Habilidade de Competência Ginete", "a Especialização Pulmão de
 *                                  Aço". Typed as {@link SkillTrait} so both kinds fit one clause;
 *                                  a {@code SkillCompetencyAbility} is resolved through {@code
 *                                  SkillCompetencyAbility#allFor} (so a racially-granted one
 *                                  counts), a {@code SkillSpecialization} against its own
 *                                  Perícia's {@code CharacterSkill#getSpecializations()}.
 * @param requiredAwakenedTitles    how many Títulos Aventyr must be Desperto — i.e. how many of
 *                                  {@code Character}'s three title slots must be filled. This
 *                                  is the gate behind the {@code Aventyr} tag on a Talento's
 *                                  rules-text header: ~110 Talentos carry it, near-universally
 *                                  demanding 1 and occasionally 2. Zero (the default) means the
 *                                  Talento is not Aventyr-tier at all. "Desperto" means simply
 *                                  <i>held</i> — see {@code Character#getAllTitles()}.
 * @param requiredTitleArchetype    when set, the Títulos counted by {@code
 *                                  requiredAwakenedTitles} must additionally be of this {@link
 *                                  TitleArchetype} — "Ter desperto ao menos 1 Título Aventyr
 *                                  <b>Bruto</b>", as the four <i>Centelha Aventyr</i> Talentos
 *                                  demand. Unset means any Título counts.
 * @param requiredRace              when set, the holder's {@code Race} must be an instance of
 *                                  this class — "Apenas personagens da Raça Ogro…". Held as a
 *                                  {@code Class} rather than an enum constant because {@code
 *                                  Race} is an interface implemented by one stateless class per
 *                                  race, with no identity enum to name (see {@code
 *                                  org.aventyrs.core.race.Race}). Tested with {@code
 *                                  isInstance}, so a Mestiço subclass of a named parent race
 *                                  still qualifies.
 * @param forbiddenRace             when set, the holder's {@code Race} must <b>not</b> be an
 *                                  instance of this class — "apenas personagens não-humanos"
 *                                  ({@code MonstruosoFeat#ANATOMIA_INCOMUM}). The mirror of
 *                                  {@code requiredRace}, {@code isInstance} and all, so excluding
 *                                  a parent race excludes its Mestiço subclasses too.
 * @param requiredCreatureType      when set, the holder's {@code
 *                                  Race#getPrerequisiteCreatureType()} must be this — "Apenas
 *                                  personagens de raça Feérica", "apenas criaturas Monstruosas".
 *                                  That hook is {@code getCreatureType()} for every living race
 *                                  and the life-race's type for a {@code RENASCIDO} one (a
 *                                  {@code Vampiro} counts as its raça em vida here). Distinct
 *                                  from {@code requiredRace}
 *                                  and not a substitute for it: a {@link CreatureType} spans
 *                                  many races (Fada, Fúria, Sátiro and Nascido da Floresta are
 *                                  all {@code FEERICO}), which is exactly what these clauses
 *                                  mean. Where rules text names two specific races of one type
 *                                  ("apenas Fadas e Fúrias"), this is <i>looser</i> than
 *                                  written — the safe direction, and noted on the constant.
 * @param requiredDeity             when set, the holder's {@code Character#getDeity()} must be
 *                                  exactly this — "Apenas personagens Orcs <b>Devotos de
 *                                  Epona</b>". A real, enforced clause rather than a comment
 *                                  because {@link Deity} and the field behind it both already
 *                                  exist; unset means devotion is irrelevant, which is every
 *                                  Talento but two today. Note this tests devotion alone, not
 *                                  the Adepto/Fiel/Fundamentalista <i>tier</i> the Talentos de
 *                                  Devoção are split across — that second progression system
 *                                  has no field, which is why those 20 stay unauthored.
 * @param requiredFeatCategory      when set, the holder must already hold {@code
 *                                  requiredFeatCategoryCount} other Talentos of this category —
 *                                  "2 outros Talentos de Destino". The Talento being tested is
 *                                  never itself counted (it is not yet held).
 * @param requiredFeatCategoryCount how many of {@code requiredFeatCategory} are needed.
 * @param craftedRegaliaGrade       when set, the holder must have forged at least {@code
 *                                  craftedRegaliaCount} Regalias of this grade — "ter sido
 *                                  bem-sucedido na criação de 3 ou mais Regalias", read off {@code
 *                                  Character#getRegaliasCrafted(RegaliaGrade)}.
 * @param craftedRegaliaCount       how many Regalias of {@code craftedRegaliaGrade} are needed.
 * @param requiredAlignments        permitted alignments for the holder. An unset or empty set
 *                                  means the Talento has no alignment prerequisite.
 * @param requiredFame              when > 0, the holder's Fama must reach this — "Fama 15"
 *                                  ({@code DestinoFeat#FAVORITISMO_MAIOR}). Read as <b>either</b>
 *                                  Fama reaching the figure: the rules text says plain "Fama"
 *                                  while this core splits it Positiva/Negativa, and the Talento
 *                                  it gates is about being <i>recognised</i>, which notoriety
 *                                  serves as well as renown. Needs a {@code CharacterSheet}.
 * @param requiredTotalExperience   when set, the holder's lifetime EXP must reach this — "EXP
 *                                  total ≥ 15". {@code totalExperience}, not {@code
 *                                  unUsedExperience}: the clause measures how far a character has
 *                                  come, not what they have left to spend. Needs a {@code
 *                                  CharacterSheet}.
 * @param anyOf                     nested requirement groups of which <b>at least one</b> must
 *                                  hold, on top of every clause set on this record — a
 *                                  disjunction ("Destreza 3 e Saque Rápido, <i>ou</i> Foco 5").
 *                                  Add branches with the builder's {@code alternative(…)}. Empty
 *                                  (the default) means no disjunction. Clauses common to every
 *                                  branch stay on the outer record rather than being repeated
 *                                  inside each, which is why this is "AND the outer group" rather
 *                                  than a flat list of whole alternatives. Nesting is checked
 *                                  recursively, so a branch may carry its own {@code anyOf}.
 */
@Builder
public record FeatRequirements (
        AttributeDomain attributeDomain,
        int requiredAttributeValue,
        AttributeDomain maximumAttributeDomain,
        int maximumAttributeValue,
        int requiredAnyAttributeValue,
        int requiredAnyRacialAttributeValue,
        EgoDomain maximumEgoDomain,
        int maximumEgoValue,
        SkillType requiredSkillType,
        int requiredSkillGraduation,
        @Singular Set<Feat> requiredFeats,
        @Singular Set<Feat> forbiddenFeats,
        @Singular Set<SkillTrait> requiredSkillTraits,
        int requiredAwakenedTitles,
        TitleArchetype requiredTitleArchetype,
        Class<? extends Race> requiredRace,
        Class<? extends Race> forbiddenRace,
        CreatureType requiredCreatureType,
        Deity requiredDeity,
        FeatCategory requiredFeatCategory,
        int requiredFeatCategoryCount,
        RegaliaGrade craftedRegaliaGrade,
        int craftedRegaliaCount,
        Set<Alignment> requiredAlignments,
        int requiredFame,
        BigDecimal requiredTotalExperience,
        @Singular("alternative") List<FeatRequirements> anyOf
) {
    public FeatRequirements {
        requiredAlignments = requiredAlignments == null ? Set.of() : Set.copyOf(requiredAlignments);
    }
}
