package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.Frenzy;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.EnumSet;
import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.FRENZY_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_LOCKED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_REQUIRED;

/**
 * Cataclismo Elemental, switched on during a Frenesi with its elements — a multi-choice of {@link
 * #ELEMENTS}, each "+1PA" (the reported Tempo is one PA per element). Its "+1 Ponto temporário de
 * Autocontrole" is a Frenesi spend like any other. Once on, it lasts as long as the Frenesi: the
 * damage is {@code AreaDamage#cataclysm}, the RE {@code CombatantSheet#getElementalResistanceInstances}.
 */
public class CataclismoElementalInteraction extends AbstractTitleAbilityInteraction {

    /**
     * "Fogo, Lava, Terra, Água, Frio/Gelo, Vento/Som, Eletricidade e Selvagem" — Lava is MAGMA and
     * Selvagem is NATURAL, the names this core's {@link ElementalType} uses.
     */
    public static final Set<ElementalType> ELEMENTS = EnumSet.of(ElementalType.FOGO, ElementalType.MAGMA,
            ElementalType.TERRA, ElementalType.AGUA, ElementalType.GELO, ElementalType.VENTO,
            ElementalType.ELETRICIDADE, ElementalType.NATURAL);

    public CataclismoElementalInteraction() {
        super(TitaEnlouquecidoAbility.CATACLISMO_ELEMENTAL);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        Frenzy frenzy = request.getActivator().getOwnFrenzy()
                .orElseThrow(() -> new IllegalOperationException(FRENZY_REQUIRED));
        if (frenzy.hasMode(FrenzyMode.CATACLISMO_ELEMENTAL)) {
            throw new IllegalOperationException(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED);
        }
        Set<ElementalType> elements = request.getChoices(ElementalType.class);
        if (elements.isEmpty()) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_REQUIRED);
        }
        if (!ELEMENTS.containsAll(elements)) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_LOCKED);
        }
    }

    @Override
    protected void onEgoSpent(final TitleAbilityActivationRequest request, final EgoCost paid) {
        GiganteEnfurecido.requireHeldBy(request.getActivator()).recordFrenzySpend(request.getActivator(), paid.points());
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        Frenzy frenzy = request.getActivator().getOwnFrenzy().orElseThrow();
        frenzy.addMode(FrenzyMode.CATACLISMO_ELEMENTAL);
        frenzy.setCataclysmElements(request.getChoices(ElementalType.class));
        return InteractionResult.builder().build();
    }
}
