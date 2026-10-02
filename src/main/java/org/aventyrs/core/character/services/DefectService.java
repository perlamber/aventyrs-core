package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.defect.Defect;
import org.aventyrs.core.defect.DefectSeverity;
import org.aventyrs.core.defect.HeldDefect;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Defeitos after character creation ({@code docs/rules/defeitos-e-qualidades.txt}, "Adquirindo Defeitos" and
 * "Superando um Defeito") — creation itself is {@code CharacterCreationService#applyDefectsAndQualities}.
 *
 * <p><b>Both are progression, and neither checks the lock.</b> Like every progression service here, the
 * caller checks {@code Campaign#requireProgressionAllowed} for {@link #overcome}; imposing a Defeito is a
 * Narrador's act ("cicatrizes de batalhas ou de aventuras") and may happen mid-Sessão. What Superar also
 * needs — "Contexto, Oportunidade e EXP" — is two parts narrative the table judges; this core charges the EXP.
 */
public interface DefectService {

    /**
     * The Narrador imposes defect at severity during play: no limit on how many, and <b>no Benefício de
     * Superação</b> ("Defeitos adquiridos durante a campanha não devem fornecer Benefícios de Superação").
     * choices answer {@link Defect#resolveChoices} as at creation. If the character already holds that Defeito
     * in force, it is <b>changed</b> to the new gravidade and choices — a limb lost after a Dano Permanente —
     * keeping whether it came from creation and its Superação (a table ruling, 2026-09-29: never a second copy). Opposing Qualidades are kept; the Defeito's effect overrides theirs (Sobreposição).
     *
     * @return the Defeito as now held
     * @throws IllegalOperationException {@code INVALID_DEFECT_SELECTION} if the choices don't answer the Defeito's
     */
    HeldDefect grantDefect(Character character, Defect defect, DefectSeverity severity, List<Object> choices)
            throws IllegalOperationException;

    /** What Superar costs: "Defeitos Leves custam 3 EXP, Moderados 5 EXP, Graves 7 EXP" — the same for any origin. */
    BigDecimal getOvercomeCost(DefectSeverity severity);

    /**
     * Superar: spends {@link #getOvercomeCost} of sheet's unused EXP and ends the Defeito's effects. It stays on
     * record as overcome ({@link HeldDefect#overcome()}), so what its Benefício de Superação gave is kept (a table
     * ruling, 2026-09-29).
     *
     * @return the Defeito as now held, overcome
     * @throws IllegalOperationException {@code DEFECT_NOT_HELD} if no Defeito of that kind is in force, or {@code
     *         NOT_ENOUGH_EXPERIENCE} — in either case nothing changes
     */
    HeldDefect overcome(Character character, CharacterSheet sheet, Defect defect) throws IllegalOperationException;
}
