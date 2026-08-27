/**
 * Títulos Aventyrs and how a Character holds them.
 *
 * <h2>Granting and reading a Título</h2>
 *
 * An {@link org.aventyrs.core.title.AventyrTitle} is a held instance — it carries the chosen
 * Especializações/Habilidades, the same "instance carries the acquisition-time choice" shape
 * as {@code org.aventyrs.core.ego.MoralHerdadaAbility}. A character holds **exactly three**
 * Título slots — Primário/Secundário/Terciário, {@link org.aventyrs.core.character.TitleSlot}
 * — unlike {@link org.aventyrs.core.race.Race} (exactly one field per Character); a slot may
 * be left empty:
 *
 * <pre>{@code
 * Santo santo = new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS));
 * character.grantTitle(santo, TitleSlot.PRIMARY);
 *
 * AventyrTitle primary       = character.getPrimaryTitle();  // null if that slot is empty
 * List<AventyrTitle> all     = character.getAllTitles();     // only the filled slots
 * }</pre>
 *
 * <p>{@link org.aventyrs.core.character.Character#getPrimaryTitle()}/{@code #getSecondaryTitle()}/
 * {@code #getTertiaryTitle()} are plain nullable fields, not the {@code @Singular} immutable-list
 * shape {@code skillCompetencyAbilities}/{@code attributeAbilities} use — see
 * {@code Character#grantTitle(AventyrTitle, TitleSlot)}'s own javadoc for why. Which slot a
 * Título occupies is a fact about the *Character*, not something the held {@code AventyrTitle}
 * instance reports about itself — there is no {@code isPrimaryTitle()} method on {@code
 * AventyrTitle}; "is this the character's Título Primário" is answered by
 * {@code character.getPrimaryTitle() == title}, not by asking the instance.
 *
 * <h2>Adding a new Título</h2>
 *
 * A concrete Título's classes live together in their own subpackage,
 * {@code org.aventyrs.core.title.<titlename>} (e.g. {@link org.aventyrs.core.title.santo}) —
 * mirroring {@code org.aventyrs.core.skill.<skillname>}'s one-subpackage-per-catalog
 * convention. See the project's {@code CLAUDE.md} "Adding a new Título" section (and the
 * {@code adding-a-title} Claude Code skill) for the full checklist; {@code Santo} is the
 * worked reference example.
 */
package org.aventyrs.core.title;
