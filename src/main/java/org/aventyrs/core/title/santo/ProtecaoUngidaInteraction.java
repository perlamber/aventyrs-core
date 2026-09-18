package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD;

/**
 * Proteção Ungida's activation (3PD, 2PA) — blesses the Armadura or Escudo its holder is using for
 * 3 Rodadas, during which "todo o dano que seria causado a você é reduzido à metade".
 *
 * <p>The Meio-Dano is granted to the activator as a {@link ModifierType#HALF_DAMAGE} {@code
 * Blessing}, which {@code DamageServiceImpl} reads for real from the sheet — applied here rather
 * than reported, since the recipient is the activator and nobody else (the split {@code
 * GritoDeGuerraVulcanoInteraction} sits on the other side of).
 *
 * <p><b>Which item is blessed is deliberately not recorded.</b> Every mechanical consequence the
 * rules text names belongs to the wearer, not to the copy, and the gate below already enforces the
 * "que você esteja utilizando" requirement at activation. Marking the {@code Item} would add
 * per-copy state nothing reads.
 *
 * <p>TODO the "Armadura e Escudo Ungido ao mesmo tempo" clause — halving the Duração of harmful
 * Encantamentos/Maldições — stays unmodelled twice over: nothing classifies a held effect as an
 * Encantamento or a Maldição, and {@code TemporaryEffect#tick} only ever decrements by 1 and is
 * package-private, so no caller can halve a running Duração.
 */
public class ProtecaoUngidaInteraction extends AbstractTitleAbilityInteraction {

    static final int DURATION_IN_ROUNDS = 3;

    private final HitPointsService hitPointsService;

    public ProtecaoUngidaInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public ProtecaoUngidaInteraction(final DeterminationPointsService determinationPointsService) {
        super(SantoAbility.PROTECAO_UNGIDA, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    /** "Abençoa um item do tipo Armadura ou Escudo <b>que você esteja utilizando</b>". */
    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        boolean wearsOne = request.getActivator().getCharacter().getEquipment().stream()
                .map(Item::getCategory)
                .anyMatch(category -> category == ItemCategory.ARMOR || category == ItemCategory.SHIELD);
        if (!wearsOne) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD);
        }
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        // HALF_DAMAGE is read as a flag, so the value is 1 rather than a magnitude.
        activator.grantBlessing(new Blessing(ModifierType.HALF_DAMAGE, 1, DURATION_IN_ROUNDS,
                TargetScope.SELF, SantoAbility.PROTECAO_UNGIDA.name()));
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
