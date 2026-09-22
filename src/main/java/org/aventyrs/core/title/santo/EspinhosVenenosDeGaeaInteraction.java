package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

/**
 * Espinhos Venenos de Gaea's activation (2PA, and a PV cost equal to the activator's Vigor) —
 * "Enquanto ativo os Espinhos Venenos de Gaea causam danos aos personagens que lhe atacarem
 * Corpo-a-Corpo, Vigor pontos de Dano Físico Elemental: Natural", for 2 Rodadas.
 *
 * <p><b>The thorns are a state, not an event.</b> The activation grants the holder a round-scoped
 * {@link ModifierType#RETALIATION_DAMAGE} {@code Blessing} worth their Vigor, and {@code
 * AttackDelivery}/{@code AttackReceiver} read it off the defender whenever an Ataque Corpo-a-Corpo
 * is resolved against them, reporting a {@code combat.Retaliation}. Modelling it as a timed bonus
 * rather than a listener is what let it use machinery that already exists: the Duração is 2
 * Rodadas, which is exactly what a {@code TemporaryBonus} counts.
 *
 * <p><b>Reported, not dealt.</b> This core computes damage only ever to a target from an attacker,
 * so the caller closes the loop with an ordinary {@code DamageService#applyDamage} against the
 * attacker's own sheet — which is the right place for it anyway, since the attacker's RD/RA should
 * judge the thorns like any other incoming damage. The same division of labour {@code
 * Teleportation} uses.
 *
 * <p>The same figure is both price and effect: Vigor in PV to activate, Vigor in damage to
 * whoever swings at you.
 *
 * <p><b>"Atacarem", not "acertarem"</b> — the thorns answer the attack, so they are reported
 * whether or not it landed. Only the Malefício half is conditional on damage actually being dealt
 * ("Personagem que lhe infligirem danos <i>adicionalmente</i> perdem…"), which is why {@code
 * Retaliation} keeps the two apart and leaves that call to the caller.
 *
 * <p>The Veneno half is real: {@code ConditionType#ENVENENADO} now carries a {@code
 * LIFE_MULTIPLIER} -1, read by {@code HitPointsService#getLifeMultiplier(Character,
 * CombatantSheet)}, so an attacker who lands a hit and takes the Malefício really does lose
 * maximum PV for 2 Rodadas.
 *
 * <p>TODO "Pontos de Vida perdidos desta forma" has no locked-PV subtype, the same gap {@code
 * SantoAbility#PROTETOR_DA_VIDA_E_DA_MORTE} cites — though note this clause, unlike its siblings,
 * never actually says the PV are locked.
 */
public class EspinhosVenenosDeGaeaInteraction extends AbstractTitleAbilityInteraction {

    /** "Duração do Efeito: 2 Rodadas." */
    static final int DURATION_IN_ROUNDS = 2;

    private final HitPointsService hitPointsService;

    public EspinhosVenenosDeGaeaInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public EspinhosVenenosDeGaeaInteraction(final DeterminationPointsService determinationPointsService) {
        super(AbracadoPelaEscuridaoAbility.ESPINHOS_VENENOS_DE_GAEA, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    /** "O Custo de Ativação desta Habilidade é igual ao seu próprio Vigor em PV." */
    @Override
    protected int resolveHitPointCost(final TitleAbilityActivationRequest request) {
        return AbracadoPelaEscuridaoAbility.ESPINHOS_VENENOS_DE_GAEA
                .resolveVigorPvCost(request.getActivator().getCharacter());
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        // "Vigor pontos de Dano" — the same Vigor total the PV cost above charges.
        int thorns = AbracadoPelaEscuridaoAbility.ESPINHOS_VENENOS_DE_GAEA
                .resolveVigorPvCost(activator.getCharacter());

        activator.grantBlessing(new Blessing(ModifierType.RETALIATION_DAMAGE, thorns, DURATION_IN_ROUNDS,
                TargetScope.SELF, AbracadoPelaEscuridaoAbility.ESPINHOS_VENENOS_DE_GAEA.name()));

        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
