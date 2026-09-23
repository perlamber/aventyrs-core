package org.aventyrs.core.effect;

import java.util.List;
import java.util.Optional;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DamageReceipt;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Estilhaçador — the target's items take half the damage their owner just took: all of them
 * (Maior), or one chosen by a d6 (Menor) — "1: Arma; 2: Armadura; 3: Escudo; 4; Capa; 5: Elmo; 6:
 * Núcleo Tecnológico. Deve ser rolado novamente se o alvo não utilizar um item do tipo escolhido."
 * Each item is damaged through its own {@code Item#applyDamage}, so its own enhancements mitigate.
 *
 * <p>The source prints "Maior" on both lines; the second is read as the Menor tier. This core has no
 * Núcleo Tecnológico, so a 6 always rerolls; a target wearing none of the five kinds loses nothing.
 */
public class Estilhacador extends AbstractCriticalEffect {

    /** Enough rerolls that a target wearing any of the five kinds is all but certain to be hit. */
    static final int MAX_ROLLS = 30;


    public Estilhacador(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.ESTILHACADOR;
    }

    @Override
    protected String majorDescription() {
        return "Todos os itens do alvo sofrem metade do dano sofrido por seu dono.";
    }

    @Override
    protected String minorDescription() {
        return "Item, escolhido aleatoriamente, sofre metade do dano sofrido por seu dono.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        int half = target.getLastDamageReceived().map(DamageReceipt::damage).orElse(0) / 2;
        List<Item> worn = target.getCharacter().getEquipment();
        if (half <= 0 || worn.isEmpty()) {
            return InteractionResult.builder();
        }
        if (getContext().isMajor()) {
            worn.forEach(item -> item.applyDamage(half));
            return InteractionResult.builder();
        }
        for (int attempt = 0; attempt < MAX_ROLLS; attempt++) {
            Optional<Item> hit = itemFor(roll(1), worn);
            if (hit.isPresent()) {
                hit.get().applyDamage(half);
                break;
            }
            if (worn.stream().noneMatch(item -> itemFor(item).isPresent())) {
                break;
            }
        }
        return InteractionResult.builder();
    }

    /** The first worn item of the kind face names, if any. */
    private static Optional<Item> itemFor(final int face, final List<Item> worn) {
        return worn.stream().filter(item -> kindOf(item) == face).findFirst();
    }

    /** Whether item is one of the five kinds the table names at all. */
    private static Optional<Integer> itemFor(final Item item) {
        int kind = kindOf(item);
        return kind == 0 ? Optional.empty() : Optional.of(kind);
    }

    /** The table's face for item — 0 for a kind it doesn't name. */
    private static int kindOf(final Item item) {
        if (item.getType() == ItemType.OFFENSIVE) {
            return 1;
        }
        ItemCategory category = item.getCategory();
        return switch (category) {
            case ARMOR -> 2;
            case SHIELD -> 3;
            case CLOAK -> 4;
            case HELMET -> 5;
            default -> 0;
        };
    }
}
