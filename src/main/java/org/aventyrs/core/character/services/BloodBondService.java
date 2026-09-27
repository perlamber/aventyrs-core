package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.race.Vampiro;

import java.util.Optional;
import java.util.UUID;

/**
 * Laços-de-Sangue — the master-and-progeny relation between Vampiros. The relation is recorded on the
 * progeny's own Raça ({@link Vampiro#getSireId()}); this service reads and creates it.
 *
 * <p>Two Talentos act on it: {@code VampiricoFeat#LACOS_ROMPIDOS} ("Desfaz os Laços-de-Sangue, não
 * precisando mais obedecer ao seu mestre") severs it, and {@code VampiricoFeat#MESTRE_VAMPIRO} ("você
 * agora pode gerar Prole, criando outros Vampiros – Dampiros não recebem este benefício") creates one.
 * What obeying a master means at the table is the GM's; this core answers only who the master is.
 */
public interface BloodBondService {

    /**
     * Who vampire must obey — its recorded master, unless it holds Laços Rompidos. Empty for a
     * non-Vampiro, one with no recorded master, or one who has broken the bond.
     */
    Optional<UUID> getMaster(Character vampire);

    /** Whether vampire is bound to master — {@link #getMaster} is master's id. */
    boolean isBoundTo(Character vampire, Character master);

    /** Whether master may gerar Prole — a Vampiro, not a Dampiro, holding Mestre Vampiro. */
    boolean canSireProgeny(Character master);

    /**
     * Makes a Prole of a character of parentRace: the Vampiro Raça to build their {@code Character}
     * with, of master's own linhagem (⚠️ a reading — the clause names none) and naming master as its
     * sire. Creating the Character itself is the caller's.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code PROGENY_NOT_PERMITTED} when
     *         {@link #canSireProgeny} is false, or {@code INVALID_PARENT_RACE} when parentRace cannot
     *         become that linhagem
     */
    Vampiro sire(Character master, Race parentRace);
}
