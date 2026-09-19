package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.FormEffect;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.TemporaryEffect;

import static org.aventyrs.core.util.TranslatableMessages.ABILITY_ACTIVATION_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.ABILITY_ON_COOLDOWN;
import static org.aventyrs.core.util.TranslatableMessages.ACTIVE_ABILITY_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_ACTION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_REACTIONS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_FREE_ACTIONS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_HIT_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_MAGIC_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.FORM_NOT_AVAILABLE;

public class ActiveAbilityServiceImpl implements ActiveAbilityService {

    private final ActionPointsService actionPointsService;
    private final MagicPointsService magicPointsService;
    private final HitPointsService hitPointsService;
    private final ReactionsService reactionsService;
    private final FreeActionsService freeActionsService;
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    public ActiveAbilityServiceImpl() {
        this(new ActionPointsServiceImpl(), new MagicPointsServiceImpl(), new HitPointsServiceImpl());
    }

    public ActiveAbilityServiceImpl(final ActionPointsService actionPointsService, final MagicPointsService magicPointsService) {
        this(actionPointsService, magicPointsService, new HitPointsServiceImpl());
    }

    public ActiveAbilityServiceImpl(final ActionPointsService actionPointsService, final MagicPointsService magicPointsService,
                                    final HitPointsService hitPointsService) {
        this(actionPointsService, magicPointsService, hitPointsService,
                new ReactionsServiceImpl(), new FreeActionsServiceImpl());
    }

    /**
     * The full form — the Reação and Ação Livre counters joined the gate when {@code ActionCost}
     * became the Tempo de Ativação type, so an ability priced in either is checked against the
     * right entitlement instead of against Pontos de Ação it never spends.
     */
    public ActiveAbilityServiceImpl(final ActionPointsService actionPointsService, final MagicPointsService magicPointsService,
                                    final HitPointsService hitPointsService, final ReactionsService reactionsService,
                                    final FreeActionsService freeActionsService) {
        this.actionPointsService = actionPointsService;
        this.magicPointsService = magicPointsService;
        this.hitPointsService = hitPointsService;
        this.reactionsService = reactionsService;
        this.freeActionsService = freeActionsService;
    }

    @Override
    public void activate(final Character character, final CombatantSheet characterSheet, final ActiveAbility ability, final int turnNumber) throws IllegalOperationException {
        if (character.getActiveAbilities().stream().noneMatch(held -> held == ability)) {
            throw new IllegalOperationException(ACTIVE_ABILITY_NOT_HELD);
        }
        // Silêncio: "não podem ativar Habilidades de Aventyrs ou de Monstros". Checked before
        // any cost is paid, for the same reason the Magia gate is. No SceneContext reaches this
        // call, and no condition scopes this gate by proximity, so null is exact here.
        if (characterSheet.isAbilityActivationPrevented(null)) {
            throw new IllegalOperationException(ABILITY_ACTIVATION_PREVENTED);
        }
        // Resfriamento — both units at once (Rodadas still owed, or a Descanso still pending),
        // checked alongside the other gates before a single point is spent, so a refused
        // activation costs nothing. Neither is burned down here.
        if (characterSheet.isOnCooldown(ability)) {
            throw new IllegalOperationException(ABILITY_ON_COOLDOWN);
        }
        // A Forma the holder's own Talentos refuse is refused here too — Marca da Maldição locks
        // its holder into one shape, Acolhida por Flora forbids another. Checked before the cost
        // for the same reason every other gate is.
        FormType grantedForm = ability.resolveGrantedForm(character);
        if (grantedForm != null && !characterSheet.canTakeForm(grantedForm)) {
            throw new IllegalOperationException(FORM_NOT_AVAILABLE);
        }
        checkActionCost(characterSheet, ability.getActionPointCost(), turnNumber);
        if (magicPointsService.getCurrentMagicPoints(character, characterSheet) < ability.getMagicPointCost()) {
            throw new IllegalOperationException(NOT_ENOUGH_MAGIC_POINTS);
        }
        if (determinationPointsService.getCurrentDeterminationPoints(character, characterSheet)
                < ability.getDeterminationPointCost()) {
            throw new IllegalOperationException(NOT_ENOUGH_DETERMINATION_POINTS);
        }
        // "consomem 3PV cada" — a Poder Vampírico cannot be activated if paying it would drop the
        // holder to 0 PV or below. Provenance ("recuperados exclusivamente com Roubo de Vida") is
        // not tracked; the loss is plain damage.
        if (ability.getHitPointCost() > 0
                && hitPointsService.getCurrentHitPoints(character, characterSheet) <= ability.getHitPointCost()) {
            throw new IllegalOperationException(NOT_ENOUGH_HIT_POINTS);
        }

        characterSheet.spendMagicPoints(ability.getMagicPointCost());
        characterSheet.spendDeterminationPoints(ability.getDeterminationPointCost());
        if (ability.getHitPointCost() > 0) {
            characterSheet.applyDamage(ability.getHitPointCost());
        }
        for (TemporaryEffect effect : ability.resolveEffects(character)) {
            characterSheet.applyEffect(effect);
        }
        // The Forma, and the countdown that ends it. Entered after the costs are paid, like every
        // other effect; the FormEffect is what returns the holder to their own shape when the
        // Duração lapses (see tickTemporaryEffects) — the one place leaving a Forma is not a
        // caller's call.
        if (grantedForm != null) {
            characterSheet.enterForm(grantedForm);
            characterSheet.applyEffect(new FormEffect(grantedForm, ability.resolveDurationInRounds()));
        }
        // Started only once everything above has succeeded — an activation that threw never
        // happened, so it must not lock the ability out.
        characterSheet.startCooldown(ability, ability.getCooldownRounds());
        characterSheet.startRestCooldown(ability, ability.getReactivationRest());
    }

    /**
     * Refuses an activation its holder isn't entitled to pay for, each Tempo de Ativação against
     * its own counter: Pontos de Ação for a FIXED/DYNAMIC cost (a DYNAMIC one against its
     * minimum — the most the player could then choose is the caller's business), a Reação for a
     * REACTION, an Ação Livre for a FREE_ACTION, and nothing at all for a passive.
     *
     * <p>All three are <b>"entitled to any at all"</b> checks, not live pools: this core keeps no
     * spent-this-Turn ledger for any of them, the same "reported, not deducted" stance {@code
     * ActionCost} and {@code WeaponDrawService} take. A holder with one Reação who has already
     * used it this Rodada still passes here.
     *
     * <p>The sheet overloads, not the Character ones: activating an ability is combat-facing, and
     * the sheet is what carries a granted ACTION_POINTS/REACTIONS/FREE_ACTIONS TemporaryBonus. No
     * SceneContext is threaded through this call yet, so ESTRATEGISTA's combat malus reads as
     * out-of-combat.
     */
    private void checkActionCost(final CombatantSheet characterSheet, final ActionCost cost, final int turnNumber) {
        switch (cost.kind()) {
            case FIXED, DYNAMIC -> {
                if (actionPointsService.getMaxActionPoints(characterSheet, turnNumber) < cost.minimum()) {
                    throw new IllegalOperationException(NOT_ENOUGH_ACTION_POINTS);
                }
            }
            case REACTION -> {
                if (reactionsService.getTotalReactions(characterSheet, turnNumber) < 1) {
                    throw new IllegalOperationException(NOT_ENOUGH_REACTIONS);
                }
            }
            case FREE_ACTION -> {
                if (freeActionsService.getTotalFreeActions(characterSheet, turnNumber) < 1) {
                    throw new IllegalOperationException(NOT_ENOUGH_FREE_ACTIONS);
                }
            }
            case NONE -> {
                // A passive has nothing to pay. Activating one is odd but not this gate's refusal.
            }
        }
    }
}
