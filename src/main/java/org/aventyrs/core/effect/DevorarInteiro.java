package org.aventyrs.core.effect;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.services.DevourService;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Bocarra's Corrente de Efeitos — Devorar Inteiro: "O alvo deste ataque é engolido." Built by {@code
 * DevourService#declareBite} for the one bite it upgrades, and carried on that attack's {@code
 * DeliveredAttack#effectChains}, so it lands only when the shared Corrente threshold is met.
 *
 * <p>Applying it asks {@link DevourService#swallow}: a target too large for the captor, or one that
 * would overfill their stomach, is not swallowed, and the chain then does nothing.
 */
@Getter
public class DevorarInteiro extends AbstractEffect implements EffectChain {

    private final CombatantSheet captor;
    private final DevourService devourService;

    public DevorarInteiro(@NonNull final CombatantSheet captor, @NonNull final DevourService devourService) {
        this.captor = captor;
        this.devourService = devourService;
    }

    @Override
    public String getDescription() {
        return "O alvo deste ataque é engolido. Personagens engolidos desta forma sofrem 1+Metade do "
                + "Vigor do Ogro pontos de dano a cada Rodada e para se soltarem precisam causar danos "
                + "(dobro do vigor do Ogro) até serem regurgitados.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        devourService.swallow(captor, target);
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
