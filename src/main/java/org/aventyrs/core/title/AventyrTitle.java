package org.aventyrs.core.title;

import java.util.List;
import java.util.stream.Stream;

/**
 * A held instance of one Título Aventyr (e.g. {@code org.aventyrs.core.title.santo.Santo}) —
 * unlike {@link org.aventyrs.core.race.Race} (one stateless concrete class per race, since
 * every holder of a given race gets identical racial abilities), this is the per-character
 * *held* instance, carrying acquisition-time state (which Especializações/Habilidades were
 * chosen) — the same "instance carries the acquisition-time choice" shape as
 * {@code org.aventyrs.core.ego.MoralHerdadaAbility}/
 * {@code org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility}. "Which Título family
 * this is" is answered by which concrete class implements this interface (e.g. {@code Santo}),
 * not a separate identity enum — deferred until a second Título creates real pressure for one,
 * mirroring {@code org.aventyrs.core.ego.SorteAdvantage}'s own precedent for the same
 * restraint. See CLAUDE.md's "Adding a new Título" section for the full convention.
 *
 * <p>Whether a held instance is the holder's "Título Primário" is deliberately **not** a
 * method here — it's a fact about which of {@link org.aventyrs.core.character.Character
 * #getPrimaryTitle()}/{@code #getSecondaryTitle()}/{@code #getTertiaryTitle()} slot holds this
 * instance, not something the instance reports about itself (an earlier version had a
 * self-reported {@code isPrimaryTitle()} boolean, unenforced against holding more than one; the
 * three-slot shape on {@code Character} makes that structurally impossible instead).
 */
public interface AventyrTitle {

    /** This Título's own name, e.g. "Santo" — matches its rules-text header exactly. */
    String getName();

    /**
     * The base passive granted just for holding this Título, with no activation cost of its
     * own — e.g. Santo's own "Despertar" clause. Lives here, not on a catalog
     * {@link AventyrTitleAbility} constant, because the base effect isn't itself a
     * Habilidade/Especialização — it's a property of holding the Título at all.
     */
    String getBaseEffectDescription();

    /**
     * The additional bonus this Título's base effect grants specifically when held as the
     * holder's Título Primário (see {@code Character#getPrimaryTitle()}) — e.g. Santo's own
     * "Se este for seu Título Primário, aliados adjacentes a você recebem Bônus em Defesas
     * iguais à metade dos Bônus que você receber..." clause. Kept separate from {@link
     * #getBaseEffectDescription()} rather than left concatenated onto it, since "is this Título
     * held as Primário" is a distinct fact a caller resolves separately (via {@code
     * Character#getPrimaryTitle() == title}) from "does this Título's base effect apply at
     * all" (true unconditionally, just by holding it). {@code null} when this Título's base
     * effect has no such clause — not every Título's rules text needs one.
     */
    default String getPrimaryTitleBonusDescription() {
        return null;
    }

    List<AventyrTitleSpecialization> getSpecializations();

    List<AventyrTitleAbility> getAbilities();

    /**
     * Especializações plus Supremas held, combined — pure arithmetic over the two methods
     * above, kept as a shared default so any Título whose own rules text scales off this same
     * count (as Santo's Despertar duration does) doesn't need to re-derive the formula.
     */
    default int getSpecializationAndSupremaCount() {
        return getSpecializations().size()
                + (int) getAbilities().stream().filter(AventyrTitleAbility::isSupreme).count();
    }

    /**
     * Every {@link AventyrTitleAbility} this Título grants, combining {@link #getSpecializations()}
     * (each one an Active Ability in its own right — see that interface's own javadoc) and
     * {@link #getAbilities()} into one list — e.g. for a scanning service that needs to sum a
     * {@code resolve*} hook across every Título trait the holder has, regardless of whether
     * it's cataloged as an Especialização or a Habilidade/Suprema (see {@code
     * DamageServiceImpl}'s own Título-ability RA scan).
     */
    default List<AventyrTitleAbility> getAllAbilities() {
        return Stream.concat(
                getSpecializations().stream().map(AventyrTitleAbility.class::cast),
                getAbilities().stream()
        ).toList();
    }
}
