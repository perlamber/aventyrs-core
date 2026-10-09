package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.ego.SocialClass;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointSpend;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DIE_ROLL;
import static org.aventyrs.core.util.TranslatableMessages.MONSTER_EGO_EFFECTS_EXHAUSTED;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EGO_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.RESOURCES_NOT_RECOVERED_BY_SESSION;

public class EgoPointsServiceImpl implements EgoPointsService {

    @Override
    public int getExtraSessionRecovery(final Character character, final EgoDomain domain) {
        return character.getEgoAdvantages(domain).stream()
                .mapToInt(EgoAdvantage::resolveExtraSessionEgoRecovery)
                .sum();
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
        if (chosenDomain == EgoDomain.RECURSOS) {
            throw new IllegalOperationException(RESOURCES_NOT_RECOVERED_BY_SESSION);
        }
        sheet.recoverTemporaryEgoPoints(chosenDomain, SESSION_TEMPORARY_RECOVERY);
        for (EgoDomain domain : EgoDomain.values()) {
            if (domain != EgoDomain.RECURSOS) {
                sheet.recoverTemporaryEgoPoints(domain, getExtraSessionRecovery(sheet.getCharacter(), domain));
            }
        }
    }

    /**
     * Delegates to the single-sheet overload once per entry — the per-sheet method already
     * handles the baseline point and every domain's Vantagem extra, so there is no arithmetic
     * here to get wrong.
     */
    @Override
    public void applySessionRecovery(@NonNull final Map<CombatantSheet, EgoDomain> chosenDomains) {
        // Refused before anyone recovers, so a bad entry can't leave the table half-applied.
        if (chosenDomains.containsValue(EgoDomain.RECURSOS)) {
            throw new IllegalOperationException(RESOURCES_NOT_RECOVERED_BY_SESSION);
        }
        chosenDomains.forEach(this::applySessionRecovery);
    }

    @Override
    public int spendResourcesForEquipmentPoints(@NonNull final CombatantSheet sheet, @NonNull final EgoPointType type,
                                                final int amount) {
        int permanentBefore = sheet.getPermanentEgoPoints(EgoDomain.RECURSOS);
        // No Vantagem de Recursos reacts to a spend with a die (only Autocontrole's does), so any legal face.
        EgoPointSpend spend = useEgoPointsForEffect(sheet, EgoDomain.RECURSOS, type, amount, MIN_DIE_FACE);
        int bonus = sheet.getCharacter().getFeats().stream()
                .mapToInt(feat -> feat.resolveResourcesPointValueBonus(sheet.getCharacter()))
                .sum();
        int gained = 0;
        for (int point = 0; point < spend.getValue(); point++) {
            // A permanent point is priced at the row it was spent from, then lowers the next one's.
            int row = type == EgoPointType.PERMANENT ? permanentBefore - point : permanentBefore;
            gained += SocialClass.of(row).getPointValue(type) + bonus;
        }
        if (gained > 0) {
            sheet.grantEquipmentPoints(gained);
        }
        return gained;
    }

    @Override
    public org.aventyrs.core.skill.SkillRoll applySorte(@NonNull final CombatantSheet sheet,
                                                        @NonNull final org.aventyrs.core.skill.SkillRoll roll,
                                                        @NonNull final org.aventyrs.core.ego.SorteEffect effect) {
        org.aventyrs.core.skill.SkillRoll marked = roll.withSorte(effect);
        paySorte(sheet, effect.getPointType());
        return marked;
    }

    @Override
    public org.aventyrs.core.skill.SkillRoll rerollWithSorte(@NonNull final CombatantSheet sheet,
                                                             @NonNull final org.aventyrs.core.skill.SkillRoll roll,
                                                             @NonNull final List<Integer> newDice) {
        // Built first, so dice that are not three d6 faces are refused before a point is spent.
        org.aventyrs.core.skill.SkillRoll rerolled = roll.rerolledWithSorte(newDice);
        paySorte(sheet, EgoPointType.TEMPORARY);
        return rerolled;
    }

    /** One Sorte point of type — see {@link #payForEffect}. */
    private void paySorte(final CombatantSheet sheet, final EgoPointType type) {
        payForEffect(sheet, EgoDomain.SORTE, type);
    }

    @Override
    public void payForEffect(@NonNull final CombatantSheet sheet, @NonNull final EgoDomain domain,
                             @NonNull final EgoPointType type) {
        if (!sheet.isPdn()) {
            int held = type == EgoPointType.PERMANENT ? sheet.getPermanentEgoPoints(domain) : sheet.getTemporaryEgoPoints(domain);
            if (held < 1) {
                throw new IllegalOperationException(NOT_ENOUGH_EGO_POINTS);
            }
        }
        // No Vantagem reacts to these spends with a die Determinação Heroica can't take as its lowest face.
        useEgoPointsForEffect(sheet, domain, type, 1, MIN_DIE_FACE);
    }

    @Override
    public void grantPdnCompensation(@NonNull final java.util.Collection<CombatantSheet> playerCharacters,
                                     @NonNull final EgoDomain domain) {
        playerCharacters.forEach(pc -> pc.receiveTemporaryEgoPoints(domain, PDN_COMPENSATION, 1));
    }

    @Override
    public int grantTemporaryByNarrator(@NonNull final CombatantSheet sheet, @NonNull final EgoDomain domain,
                                        final int amount) {
        return sheet.receiveTemporaryEgoPoints(domain, NARRATOR_GRANT, amount);
    }

    @Override
    public Character grantPermanentByNarrator(@NonNull final Character character, @NonNull final EgoDomain domain,
                                              final int amount) {
        if (amount <= 0) {
            return character;
        }
        return character.toBuilder().egos(character.getEgos().withVariableBonus(domain, amount)).build();
    }

    @Override
    public Character sacrificePermanent(@NonNull final Character character, @NonNull final EgoDomain domain,
                                        final int amount) {
        if (amount <= 0) {
            return character;
        }
        if (character.getEgos().getEgo(domain).getTotal() < amount) {
            throw new IllegalOperationException(org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EGO_POINTS);
        }
        return character.toBuilder().egos(character.getEgos().withVariableBonus(domain, -amount)).build();
    }

    @Override
    public int getSpendRecovery(final Character character, final EgoPointSpend spend, final int rolledValue) {
        if (rolledValue < MIN_DIE_FACE || rolledValue > MAX_DIE_FACE) {
            throw new IllegalOperationException(INVALID_DIE_ROLL);
        }
        return character.getEgoAdvantages(spend.getDomain()).stream()
                .mapToInt(advantage -> advantage.resolveEgoSpendRecovery(spend, rolledValue))
                .sum();
    }

    /**
     * Spends first, then resolves the recovery against the <em>completed</em> spend — the order
     * matters: {@code getType()} and the actually-spent {@code getValue()} are both facts only
     * the finished spend knows, and a spend that took nothing earns nothing.
     */
    @Override
    public List<Blessing> getSpendBlessings(final Character character, final EgoPointSpend spend) {
        return character.getEgoAdvantages(spend.getDomain()).stream()
                .flatMap(advantage -> advantage.resolveEgoSpendBlessings(spend).stream())
                .toList();
    }

    @Override
    public EgoPointSpend useEgoPointsForEffect(final CombatantSheet sheet, final EgoDomain domain,
                                               final EgoPointType type, final int amount,
                                               final int rolledValue) {
        // "Monstros comuns podem utilizar no máximo dois Efeitos de Ego por Cena" — refused before a
        // point is spent, counted only once the spend went through.
        EgoPointSpend spend;
        if (sheet.isPdn()) {
            // A PdN spends nothing: only an Exemplar may use an Efeito de Ego, and the PJs are owed a point instead.
            if (sheet instanceof MonsterSheet monster) {
                monster.checkEgoEffectAvailable();
            } else if (!sheet.isIntelligent()) {
                // "quando inteligentes" — a foe held as a plain sheet the Narrador did not mark (core 0.0.84).
                throw new IllegalOperationException(MONSTER_EGO_EFFECTS_EXHAUSTED);
            }
            sheet.recordPdnEgoUse(domain, type);
            spend = new EgoPointSpend(domain, type, Math.max(0, amount));
        } else {
            spend = sheet.spendEgoPoints(domain, type, amount);
        }
        int recovered = getSpendRecovery(sheet.getCharacter(), spend, rolledValue);
        if (recovered > 0) {
            // Only the PV half is a heal effect the fallen-character limits reach.
            sheet.heal(recovered, HealingSource.egoSpend(sheet.getCharacter().getEgoAdvantage(domain), sheet));
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
