package org.aventyrs.core.item;

import org.aventyrs.core.race.CreatureType;

import java.util.Set;

/**
 * The Centelha donation powering a Regalia forge — "um personagem sacrifique voluntariamente uma
 * de suas Centelhas, despejando seu sangue sobre o equipamento". One of {@link ItemForgery}'s
 * inputs.
 *
 * <p><b>A caller assertion, not tracked state.</b> This core does not model a character's
 * Centelhas at all (the gap {@code DestinoFeat#FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH} cites), so
 * nothing here can verify that a donor has one to give, nor deduct it — the forge only refuses
 * a donation the caller itself declares unwilling or unqualified. How many Centelhas the grade
 * costs is on {@link RegaliaGrade#requiresAllCentelhas()}, equally unenforced.
 *
 * @param willing   whether the donor consents and does not recant mid-process — a forge with
 *                  {@code false} fails ({@code REGALIA_DONOR_NOT_WILLING}), per "Se o personagem
 *                  doador não for voluntário … a criação da Regalia irá falhar"
 * @param donorType the donor's {@link CreatureType} — only consulted for a {@link
 *                  RegaliaGrade#DIVINA} forge, which requires {@link #isDivineDonor()}; may be
 *                  {@code null} for a Menor/Superior donation
 */
public record RegaliaDonation(boolean willing, CreatureType donorType) {

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

    /** Whether the donor's essence qualifies to power a {@link RegaliaGrade#DIVINA}. */
    public boolean isDivineDonor() {
        return donorType != null && DIVINE_DONORS.contains(donorType);
    }
}
