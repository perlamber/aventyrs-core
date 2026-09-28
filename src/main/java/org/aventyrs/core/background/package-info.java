/**
 * Antecedentes — {@code docs/rules/antecedentes.txt}. Every character holds exactly two, one
 * Naturalidade ({@link org.aventyrs.core.background.OriginBackground}) and one Carreira ({@link
 * org.aventyrs.core.background.CareerBackground}), picked <b>last</b> in creation and never changed.
 *
 * <h2>Picking one</h2>
 *
 * Everything goes through {@code CharacterCreationService}; a UI needs no per-Antecedente knowledge:
 *
 * <ol>
 *   <li>{@code getBackgroundOptions(kind)} — the catalog.</li>
 *   <li>{@link org.aventyrs.core.background.Background#getGraduationGrants()} — the "+1 Graduação"
 *       clauses; a grant whose {@code isChoice()} is true asks for {@code picks} of its options.</li>
 *   <li>{@code getBackgroundTraitGrants(character, background, graduationSkills)} — the
 *       Especializações/Habilidades de Competência owed <em>given those picks</em> (they decide "Se
 *       treinado em X" and "da Perícia escolhida"); again, only {@code isChoice()} grants ask.</li>
 *   <li>{@code getBackgroundBenefitChoices(character, background)} — {@code FeatChoice}s the Benefício
 *       itself needs (Escudeiro's Perícia, Estudioso Arcano's {@code MagicTree}).</li>
 *   <li>{@code applyBackground(character, new AcquiredBackground(background, graduationSkills,
 *       traits, benefitChoices))} returns the new {@code Character}. Apply the Naturalidade first, then
 *       the Carreira against what that returned ({@code applyBackgrounds} does both).</li>
 * </ol>
 *
 * <h2>What holding one does</h2>
 *
 * The creation grants are <b>materialized</b> — Graduações, traits and Ego base are written into the
 * character, so nothing re-derives them. The stored {@link org.aventyrs.core.background.AcquiredBackground}
 * keeps the picks and is what a consumer persists; restoring a character means restoring those records
 * alongside its skills, not re-applying them. The named Benefício ({@code
 * org.aventyrs.core.feat.AntecedenteFeat}) is read live through {@code Character#getFeats()}; the
 * activated ones (Teoria de Tudo, Carteirada, Dar Fuga, Ação Rotineira, Dom da Negociação, Criar
 * Obra-Prima, Retribuição Atroz) are spent on a {@code SkillRoll} exactly like an activated Talento.
 *
 * <h2>Still not modelled</h2>
 *
 * Idiomas (every "Idioma adicional", Comerciante's Linguista), Elduriano's A Todo Vapor (no
 * Tecnológico/Vapor item column), Espião's Kit de Disfarces (no Poção in the catalog), Jullyano's
 * territory bonus (no location), Curandeiro's physical-healing half (no computed Medicina heal), and
 * the narrative Benefícios (Harenai, Eremita, Súdito do Dragão's orientation) — each TODO'd on its
 * {@code AntecedenteFeat} constant.
 */
package org.aventyrs.core.background;
