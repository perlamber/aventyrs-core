package org.aventyrs.core.title.giganteenfurecido;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Frenzy;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.FRENZY_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_ACTIVATION_LIMIT_REACHED;

/**
 * Frenesi Esmeralda: "Enquanto em Frenesi você pode ativar esta Suprema, se o fizer você Bônus de +2 em
 * Força, Vantagem em Rolagens de Atenção e Conjuração de Magias Ofensivas …, seu Movimento Base aumenta
 * em +2UD, mas você sofre Redutor de -4 em Defesas." Carried on the Frenzy, so it all ends with it. The
 * Conjuração half is read by {@code SpellCastingServiceImpl} off {@link FrenzyMode#FRENESI_ESMERALDA},
 * since only a damaging Magia earns it.
 */
public class FrenesiEsmeraldaInteraction extends AbstractTitleAbilityInteraction {

    static final int STRENGTH_BONUS = 2;
    static final int MOVEMENT_BONUS = 2;
    static final int DEFESAS_MALUS = -4;

    public FrenesiEsmeraldaInteraction() {
        super(GiganteEnfurecidoAbility.FRENESI_ESMERALDA);
    }

    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        Frenzy frenzy = request.getActivator().getOwnFrenzy()
                .orElseThrow(() -> new IllegalOperationException(FRENZY_REQUIRED));
        if (frenzy.hasMode(FrenzyMode.FRENESI_ESMERALDA)) {
            throw new IllegalOperationException(TITLE_ABILITY_ACTIVATION_LIMIT_REACHED);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        Frenzy frenzy = request.getActivator().getOwnFrenzy().orElseThrow();
        frenzy.addMode(FrenzyMode.FRENESI_ESMERALDA);
        frenzy.addBonus(ModifierType.STRENGTH_BONUS, STRENGTH_BONUS);
        frenzy.addBonus(ModifierType.ATTENTION_ROLL_BONUS, Skill.ADVANTAGE_BONUS);
        frenzy.addBonus(ModifierType.MOVEMENT, MOVEMENT_BONUS);
        frenzy.addBonus(ModifierType.DEFESAS, DEFESAS_MALUS);
        return InteractionResult.builder().build();
    }
}
