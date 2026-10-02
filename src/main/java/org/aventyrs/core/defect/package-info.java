/**
 * Defeitos e Qualidades — {@code docs/rules/defeitos-e-qualidades.txt}; plan in {@code
 * docs/plans/defeitos-e-qualidades-plan.md}. Optional for every character: none is required, and a
 * Qualidade is never held without a Defeito.
 *
 * <h2>At creation — after the Egos and Perfil de Ação, before the Habilidades and Talentos</h2>
 *
 * <ol>
 *   <li>Up to three {@link org.aventyrs.core.defect.Defect}s, one per {@link
 *       org.aventyrs.core.defect.DefectSeverity}, each with its {@link
 *       org.aventyrs.core.defect.Defect#resolveChoices choices} (ask again after each answer — a later
 *       one may depend on it) and one {@link org.aventyrs.core.defect.SuperacaoBenefit} of its
 *       gravidade, with that benefit's own {@link org.aventyrs.core.defect.SuperacaoBenefit#getPick() pick}.</li>
 *   <li>Up to three {@link org.aventyrs.core.defect.Quality Qualidades}: exactly the Menor/Maior counts
 *       the chosen benefits grant ({@link org.aventyrs.core.defect.QualitySource#SUPERACAO}), plus any
 *       bought with Talentos Gerais ({@link org.aventyrs.core.defect.QualitySource#GENERAL_FEAT_TRADE},
 *       only beside a Defeito). None may oppose a held Defeito.</li>
 *   <li>{@code CharacterCreationService#applyDefectsAndQualities} validates it all and returns the new
 *       {@code Character}. From there, {@code getStartingFeatSlots(Character)} is the Talentos step's
 *       slot list (traded General slots gone, a Superação's Talento slot added) and {@code
 *       Character#getBonusAttributeAbilitySlots()} the Habilidades step's extra slots.</li>
 * </ol>
 *
 * <p>Each held level's effect is a {@code DefeitoFeat}/{@code QualidadeFeat} constant folded into
 * {@code Character#getFeats()}. <b>As of Phase 1 those effects are not wired yet</b> — see each enum's
 * TODO and the plan's clause table.
 */
package org.aventyrs.core.defect;
