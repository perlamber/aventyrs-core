package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.CreatureType;

import java.util.Set;

/**
 * The Centelha donation powering a Regalia forge — "um personagem sacrifique voluntariamente uma
 * de suas Centelhas, despejando seu sangue sobre o equipamento". One of {@link ItemForgery}'s
 * inputs.
 *
 * <p><b>Name the donor when they are a {@link Character}</b> ({@link #willingDonor(Character)}): the
 * forge then refuses a donor with too few Centelhas ({@code REGALIA_DONOR_LACKS_CENTELHA}) and, on
 * success, takes them — one for a Menor, all for a Superior ({@link
 * RegaliaGrade#requiresAllCentelhas()}) — through {@link Character#sacrificeCentelhas}. A donation
 * naming no donor stays a caller assertion, checked for willingness and essence only: a Divina's
 * Dragão/Elemental/Abissal/Celestial is usually a foe, not a {@code Character}.
 *
 * @param willing   whether the donor consents and does not recant mid-process — a forge with
 *                  {@code false} fails ({@code REGALIA_DONOR_NOT_WILLING}), per "Se o personagem
 *                  doador não for voluntário … a criação da Regalia irá falhar"
 * @param donorType the donor's {@link CreatureType} — only consulted for a {@link
 *                  RegaliaGrade#DIVINA} forge, which requires {@link #isDivineDonor()}; may be
 *                  {@code null} for a Menor/Superior donation
 * @param donor     the donating character, when there is one — {@code null} for an unnamed donor
 */
public record RegaliaDonation(boolean willing, CreatureType donorType, Character donor) {

    /** A donation naming no donor character. */
    public RegaliaDonation(final boolean willing, final CreatureType donorType) {
        this(willing, donorType, null);
    }

    /** The Dragão / Elemental / Abissal / Celestial essences a Regalia Divina's donor must be. */
    private static final Set<CreatureType> DIVINE_DONORS = Set.of(CreatureType.DRAGAO,
            CreatureType.ELEMENTAL, CreatureType.ABISSAL, CreatureType.CELESTIAL);

    /** A willing donation from an ordinary personagem — for a Menor or Superior forge. */
    public static RegaliaDonation willingDonor() {
        return new RegaliaDonation(true, null);
    }

    /** A willing donation from a Dragão / Elemental / Abissal / Celestial — for a Divina forge. */
    public static RegaliaDonation willingDivineDonor(final CreatureType donorType) {
        return new RegaliaDonation(true, donorType);
    }

    /** A willing donation from donor — whose Centelhas the forge checks and takes. */
    public static RegaliaDonation willingDonor(final Character donor) {
        return new RegaliaDonation(true, null, donor);
    }

    /** Whether the donor's essence qualifies to power a {@link RegaliaGrade#DIVINA}. */
    public boolean isDivineDonor() {
        return donorType != null && DIVINE_DONORS.contains(donorType);
    }
}
