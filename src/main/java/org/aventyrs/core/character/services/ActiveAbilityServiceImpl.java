package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.FormEffect;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.TemporaryEffect;

import static org.aventyrs.core.util.TranslatableMessages.ABILITY_ACTIVATION_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.ABILITY_ON_COOLDOWN;
import static org.aventyrs.core.util.TranslatableMessages.ACTIVE_ABILITY_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_ACTION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_HIT_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_MAGIC_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.FORM_NOT_AVAILABLE;

public class ActiveAbilityServiceImpl implements ActiveAbilityService {

    private final ActionPointsService actionPointsService;
    private final MagicPointsService magicPointsService;
    private final HitPointsService hitPointsService;
    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    public ActiveAbilityServiceImpl() {
        this(new ActionPointsServiceImpl(), new MagicPointsServiceImpl(), new HitPointsServiceImpl());
    }

    public ActiveAbilityServiceImpl(final ActionPointsService actionPointsService, final MagicPointsService magicPointsService) {
        this(actionPointsService, magicPointsService, new HitPointsServiceImpl());
    }

    public ActiveAbilityServiceImpl(final ActionPointsService actionPointsService, final MagicPointsService magicPointsService,
                                    final HitPointsService hitPointsService) {
        this.actionPointsService = actionPointsService;
        this.magicPointsService = magicPointsService;
        this.hitPointsService = hitPointsService;
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
        // The sheet overload, not the Character one: activating an ability is combat-facing, and
        // the sheet is what carries a granted ACTION_POINTS TemporaryBonus. No SceneContext is
        // threaded through this call yet, so ESTRATEGISTA's combat malus reads as out-of-combat.
        if (actionPointsService.getMaxActionPoints(characterSheet, turnNumber) < ability.getActionPointCost()) {
            throw new IllegalOperationException(NOT_ENOUGH_ACTION_POINTS);
        }
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
}
