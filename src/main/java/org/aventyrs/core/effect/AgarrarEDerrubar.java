package org.aventyrs.core.effect;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.CharacterSizeService;
import org.aventyrs.core.character.services.CharacterSizeServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * The Corrente de Efeitos – Agarrar e Derrubar ({@code PunhoInigualavelAbility#AGARRAR_E_DERRUBAR}):
 * "O Alvo do seu ataque adicionalmente é derrubado, recebendo a condição Caído. Apenas personagens
 * com até o máximo duas Categorias de Tamanho superior à sua podem ser alvos desta Corrente de
 * Efeitos."
 *
 * <p>A Corrente, so it rides the ordinary pipeline: {@code AttackDelivery} runs it only on a hit
 * clearing the defender's Corrente threshold, after the damage. Caído is applied open-ended — the
 * clause names no Duração, and getting up is the target's own action, which is how every other
 * Caído in this core is left too.
 *
 * <p>The size gate compares both combatants' <b>effective</b> Categorias ({@link
 * CharacterSizeService#getEffectiveSizeCategory(CombatantSheet)}), so a Forma or Sacrifício
 * Ymiriano's +2 counts on either side. A target too large is simply not knocked down: the attack
 * itself still landed.
 */
@Getter
public class AgarrarEDerrubar extends AbstractEffect implements EffectChain {

    /** "até o máximo duas Categorias de Tamanho superior à sua". */
    public static final int MAXIMUM_SIZE_DIFFERENCE = 2;

    private final CombatantSheet attacker;
    private final CharacterSizeService sizeService;

    public AgarrarEDerrubar(@NonNull final CombatantSheet attacker) {
        this(attacker, new CharacterSizeServiceImpl());
    }

    public AgarrarEDerrubar(@NonNull final CombatantSheet attacker, @NonNull final CharacterSizeService sizeService) {
        this.attacker = attacker;
        this.sizeService = sizeService;
    }

    @Override
    public String getDescription() {
        return "Agarrar e Derrubar: O Alvo do seu ataque adicionalmente é derrubado, recebendo a condição "
                + "Caído. Apenas personagens com até o máximo duas Categorias de Tamanho superior à sua "
                + "podem ser alvos desta Corrente de Efeitos.";
    }

    /** Whether target is small enough to be knocked down by attacker. */
    public boolean canKnockDown(@NonNull final CombatantSheet target) {
        SizeCategory attackerSize = sizeService.getEffectiveSizeCategory(attacker);
        SizeCategory targetSize = sizeService.getEffectiveSizeCategory(target);
        return targetSize.ordinal() - attackerSize.ordinal() <= MAXIMUM_SIZE_DIFFERENCE;
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (canKnockDown(target)) {
            target.applyCondition(new Condition(ConditionType.CAIDO, null, attacker));
        }
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
