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
import org.aventyrs.core.sheet.Ungido;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_PD_AMOUNT;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD;

/**
 * Proteção Ungida's activation — blesses the Armadura or Escudo its holder is using for 3 Rodadas,
 * during which "todo o dano que seria causado a você é reduzido à metade".
 *
 * <p><b>Two modes, priced apart.</b> 2PD/2PA blesses one item; 3PD/3PA blesses an Armadura and an
 * Escudo at once, and is open only to a Santo holding any Suprema ("Se possuir quaisquer Suprema
 * de Santo você poderá, ao custo de 3PA e 3PD, ungir uma Armadura e Escudo simultaneamente").
 * That is why the {@code PDCost} is {@code variable(2)} rather than fixed: the player names which
 * mode they are paying for, and {@link #validate} refuses 3PD without a Suprema, refuses anything
 * above 3, and refuses the dual mode to someone not actually wearing both. The extra PA is
 * reported, never deducted, like every other Tempo de Ativação here.
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
 * package-private, so no caller can halve a running Duração. V19's 3PA/3PD dual-blessing mode is
 * the other half of that same clause and is unmodelled with it — see {@code
 * SantoAbility#PROTECAO_UNGIDA}'s own comment for why it is not a Variável PDCost today.
 */
import java.util.EnumSet;
import java.util.Set;

public class ProtecaoUngidaInteraction extends AbstractTitleAbilityInteraction {

    static final int DURATION_IN_ROUNDS = 3;

    /** "ao custo de 3PA e 3PD, ungir uma Armadura e Escudo simultaneamente". */
    static final int DUAL_DETERMINATION_COST = 3;

    private final HitPointsService hitPointsService;

    public ProtecaoUngidaInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public ProtecaoUngidaInteraction(final DeterminationPointsService determinationPointsService) {
        super(SantoAbility.PROTECAO_UNGIDA, determinationPointsService);
        this.hitPointsService = new HitPointsServiceImpl();
    }

    /**
     * "Abençoa um item do tipo Armadura ou Escudo <b>que você esteja utilizando</b>", plus the
     * dual mode's own two gates: only a Santo with a Suprema may pay 3PD, and only someone
     * actually wearing both has both to bless.
     */
    @Override
    protected void validate(final TitleAbilityActivationRequest request) {
        Set<ItemCategory> worn = blessableCategoriesWorn(request.getActivator());
        if (worn.isEmpty()) {
            throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD);
        }
        Integer requested = request.getDeterminationPoints();
        if (requested != null && requested > DUAL_DETERMINATION_COST) {
            throw new IllegalOperationException(INVALID_PD_AMOUNT);
        }
        if (requested != null && requested == DUAL_DETERMINATION_COST) {
            if (!holdsASuprema(request.getActivator())) {
                throw new IllegalOperationException(INVALID_PD_AMOUNT);
            }
            if (worn.size() < 2) {
                throw new IllegalOperationException(TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD);
            }
        }
    }

    /** Which of the two blessable kinds the activator is actually using. */
    private Set<ItemCategory> blessableCategoriesWorn(final CombatantSheet activator) {
        Set<ItemCategory> worn = EnumSet.noneOf(ItemCategory.class);
        for (Item item : activator.getCharacter().getEquipment()) {
            if (item.getCategory() == ItemCategory.ARMOR || item.getCategory() == ItemCategory.SHIELD) {
                worn.add(item.getCategory());
            }
        }
        return worn;
    }

    /** "Se possuir quaisquer Suprema de Santo" — any held Suprema, from any of its catalogues. */
    private boolean holdsASuprema(final CombatantSheet activator) {
        return activator.getCharacter().getAllTitles().stream()
                .flatMap(title -> title.getAllAbilities().stream())
                .anyMatch(AventyrTitleAbility::isSupreme);
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        Set<ItemCategory> worn = blessableCategoriesWorn(activator);
        // At the base price one item is blessed, and which one is the player's business — this
        // core records the kind, not the copy, so with both worn it takes the Armadura. The dual
        // mode blesses everything the validate above confirmed they are wearing.
        Set<ItemCategory> blessed = determinationPoints >= DUAL_DETERMINATION_COST
                ? worn
                : EnumSet.of(worn.contains(ItemCategory.ARMOR) ? ItemCategory.ARMOR : ItemCategory.SHIELD);
        activator.applyEffect(new Ungido(DURATION_IN_ROUNDS, blessed));

        // HALF_DAMAGE is read as a flag, so the value is 1 rather than a magnitude.
        activator.grantBlessing(new Blessing(ModifierType.HALF_DAMAGE, 1, DURATION_IN_ROUNDS,
                TargetScope.SELF, SantoAbility.PROTECAO_UNGIDA.name()));
        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
