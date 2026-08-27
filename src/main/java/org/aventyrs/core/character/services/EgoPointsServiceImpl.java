package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointSpend;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DIE_ROLL;

public class EgoPointsServiceImpl implements EgoPointsService {

    @Override
    public int getExtraSessionRecovery(final Character character, final EgoDomain domain) {
        EgoAdvantage advantage = character.getEgoAdvantage(domain);
        return advantage == null ? 0 : advantage.resolveExtraSessionEgoRecovery();
    }

    /**
     * Takes the sheet alone, deriving its {@link Character} via {@link
     * CombatantSheet#getCharacter()} — the same preference {@code DamageService}'s own methods
     * follow, rather than asking a caller to pass both. Typed as {@link CombatantSheet}, not
     * {@code CharacterSheet}: nothing here spends experience, so there's no reason a foe
     * couldn't recover between sessions too.
     */
    @Override
    public void applySessionRecovery(final CombatantSheet sheet, final EgoDomain chosenDomain) {
        sheet.recoverTemporaryEgoPoints(chosenDomain, SESSION_TEMPORARY_RECOVERY);
        for (EgoDomain domain : EgoDomain.values()) {
            sheet.recoverTemporaryEgoPoints(domain, getExtraSessionRecovery(sheet.getCharacter(), domain));
        }
    }

    /**
     * Delegates to the single-sheet overload once per entry — the per-sheet method already
     * handles the baseline point and every domain's Vantagem extra, so there is no arithmetic
     * here to get wrong.
     */
    @Override
    public void applySessionRecovery(@NonNull final Map<CombatantSheet, EgoDomain> chosenDomains) {
        chosenDomains.forEach(this::applySessionRecovery);
    }

    @Override
    public int getSpendRecovery(final Character character, final EgoPointSpend spend, final int rolledValue) {
        if (rolledValue < MIN_DIE_FACE || rolledValue > MAX_DIE_FACE) {
            throw new IllegalOperationException(INVALID_DIE_ROLL);
        }
        EgoAdvantage advantage = character.getEgoAdvantage(spend.getDomain());
        return advantage == null ? 0 : advantage.resolveEgoSpendRecovery(spend, rolledValue);
    }

    /**
     * Spends first, then resolves the recovery against the <em>completed</em> spend — the order
     * matters: {@code getType()} and the actually-spent {@code getValue()} are both facts only
     * the finished spend knows, and a spend that took nothing earns nothing.
     */
    @Override
    public List<Blessing> getSpendBlessings(final Character character, final EgoPointSpend spend) {
        EgoAdvantage advantage = character.getEgoAdvantage(spend.getDomain());
        return advantage == null ? List.of() : advantage.resolveEgoSpendBlessings(spend);
    }

    @Override
    public EgoPointSpend useEgoPointsForEffect(final CombatantSheet sheet, final EgoDomain domain,
                                               final EgoPointType type, final int amount,
                                               final int rolledValue) {
        EgoPointSpend spend = sheet.spendEgoPoints(domain, type, amount);
        int recovered = getSpendRecovery(sheet.getCharacter(), spend, rolledValue);
        if (recovered > 0) {
            sheet.heal(recovered);
            sheet.recoverMagicPoints(recovered);
            sheet.recoverDeterminationPoints(recovered);
        }
        // Granted straight to the spender: who used the points is unambiguous, so there is no
        // recipient for a caller to resolve — see EgoAdvantage#resolveEgoSpendBlessings.
        for (Blessing blessing : getSpendBlessings(sheet.getCharacter(), spend)) {
            sheet.grantTemporaryBonus(blessing.getModifierType(), blessing.getValue(), blessing.getRounds());
        }
        return spend;
    }
}
