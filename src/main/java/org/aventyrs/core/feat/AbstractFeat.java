package org.aventyrs.core.feat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * A plain, builder-built {@link Feat} for a Talento that needs no constant-specific override —
 * and the extension point that keeps {@code Feat} being sealed from closing this library off.
 *
 * <p>{@code non-sealed} on purpose: {@code Feat} is sealed so {@link FeatCatalog} can enumerate
 * the authored catalog reflectively, but a consumer with a homebrew Talento extends this and
 * gets a first-class {@code Feat} — eligible, grantable, everything. It just won't appear in
 * {@link FeatCatalog}, which lists the authored ruleset by design.
 *
 * <p>The all-args constructor is <b>public</b> for that reason and not by accident: an extension
 * point reachable only from this package would not be one. It is what lets a caller anywhere
 * write {@code new AbstractFeat(category, description, requirements) { ... }} and override a
 * {@code resolve*} hook, which is how a homebrew Talento with real behaviour is written now that
 * {@code Feat} itself cannot be implemented directly.
 */
@Getter
@Builder
@AllArgsConstructor
public non-sealed class AbstractFeat implements Feat {

    private FeatCategory featCategory;
    private String description;
    private FeatRequirements featRequirements;

}
