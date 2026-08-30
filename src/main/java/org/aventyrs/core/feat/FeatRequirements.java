package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Deity;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleArchetype;

import lombok.Builder;

/**
 * Every prerequisite a {@link Feat} can name, as a flat record of independent clauses — an
 * unset clause never blocks eligibility, and every set one must hold at once. Checked by
 * {@link Feat#isEligible}.
 *
 * <p>A Talento's Pré-requisito is one of only two places in this core where a "Requer N …"
 * clause is real, enforced data rather than an unenforced comment (the other is {@code
 * org.aventyrs.core.title.AventyrTitleAbility}) — see CLAUDE.md's "Possession is validated;
 * eligibility mostly isn't" restraint for why everything else stays prose.
 *
 * @param attributeDomain           which Attribute {@code requiredAttributeValue} tests, unset
 *                                  to skip. Tests {@code base}, not {@code getTotal()} —
 *                                  acquiring a Talento is gated on what the character
 *                                  personally invested in, deliberately unlike {@code
 *                                  org.aventyrs.core.item.ItemRequirements}.
 * @param requiredAttributeValue    minimum {@code base} of {@code attributeDomain}.
 * @param requiredSkillType         which Perícia {@code requiredSkillGraduation} tests, unset
 *                                  to skip. An untrained Perícia reads as Graduação 0.
 * @param requiredSkillGraduation   minimum Graduação in {@code requiredSkillType}. A clause
 *                                  reading merely "Treinamento em X", with no number, is a
 *                                  Graduação of 1.
 * @param requiredFeat              a Talento that must already be held, unset to skip.
 * @param requiredSkillCompetencyAbility a Habilidade de Competência that must already be held,
 *                                  unset to skip — "4 Graduações em Dirigir e Cavalgar e a
 *                                  Habilidade de Competência Ginete". Resolved through {@code
 *                                  SkillCompetencyAbility#allFor}, so a Habilidade granted by
 *                                  the holder's Race counts the same as an acquired one.
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
 * @param requiredCreatureType      when set, the holder's {@code Race#getCreatureType()} must be
 *                                  this — "Apenas personagens de raça Feérica", "apenas
 *                                  criaturas Monstruosas". Distinct from {@code requiredRace}
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
 */
@Builder
public record FeatRequirements (
        AttributeDomain attributeDomain,
        int requiredAttributeValue,
        SkillType requiredSkillType,
        int requiredSkillGraduation,
        Feat requiredFeat,
        SkillCompetencyAbility requiredSkillCompetencyAbility,
        int requiredAwakenedTitles,
        TitleArchetype requiredTitleArchetype,
        Class<? extends Race> requiredRace,
        CreatureType requiredCreatureType,
        Deity requiredDeity,
        FeatCategory requiredFeatCategory,
        int requiredFeatCategoryCount
) {}
