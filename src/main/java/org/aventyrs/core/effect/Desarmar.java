package org.aventyrs.core.effect;

import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Improvement;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

import java.util.Optional;

/**
 * Desarmar — "Apenas contra Armas e Escudos, o equipamento é derrubado ao chão." A generic Corrente de Efeitos (V19
 * 20261012), carried by the Atacar Objetos / Desarmar manoeuvre of {@code V19 - Ações de Combate}, not yet imported.
 *
 * <p><b>Out of hand, still owned</b> (table ruling, 2026-10-12) — this core models no ground:
 * <ul>
 *   <li>an <b>Arma</b> in hand is sheathed ({@link Character#sheatheWeapon}): it stays in the equipment and is drawn
 *       again through {@code WeaponDrawService} at the ordinary draw cost;</li>
 *   <li>an equipped <b>Escudo</b> goes to the target's inventory, so it grants nothing until it is put back on —
 *       {@code CharacterSheet#equip} at {@code CharacterSheet#getEquipCost} (1PA in combat).</li>
 * </ul>
 * Deliberately <em>not</em> {@link CombatantSheet#disarm}, which unequips a weapon outright and hands it to the caller.
 *
 * <p>The attacker picks the item. With no pick, the first disarmable weapon in hand, else the equipped Escudo. An
 * Arma Natural cannot be dropped ({@link Character#treatsAsNaturalWeapon}), nor anything whose Obra-Prima or
 * Aprimoramento "não pode ser desarmado" — then the Corrente does nothing; the attack itself still landed.
 */
@Getter
public class Desarmar extends AbstractEffect implements EffectChain {

    /** The Arma or Escudo the attacker aimed at; {@code null} lets {@link #targetOf} choose. */
    private final Item chosen;

    public Desarmar(final Item chosen) {
        this.chosen = chosen;
    }

    @Override
    public String getDescription() {
        return "Apenas contra Armas e Escudos, o equipamento é derrubado ao chão.";
    }

    /** What this Corrente would knock out of target's hands — empty when nothing it may reach is held. */
    public Optional<Item> targetOf(final CombatantSheet target) {
        Character character = target.getCharacter();
        if (character == null) {
            return Optional.empty();
        }
        if (chosen != null) {
            return Optional.of(chosen).filter(item -> isDroppable(character, item));
        }
        Optional<Item> weapon = character.getDrawnWeapons().stream()
                .map(Item.class::cast)
                .filter(item -> isDroppable(character, item))
                .findFirst();
        return weapon.isPresent() ? weapon : character.getEquipment().stream()
                .filter(item -> isDroppable(character, item))
                .filter(item -> item.getCategory() == ItemCategory.SHIELD)
                .findFirst();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        Item dropped = targetOf(target).orElse(null);
        if (dropped instanceof Weapon weapon && dropped.getCategory() != ItemCategory.SHIELD) {
            target.getCharacter().sheatheWeapon(weapon);
        } else if (dropped != null && target.getCharacter().unequip(dropped)) {
            target.addToInventory(dropped);
        }
        return reportChain(InteractionResult.builder()
                .droppedItem(dropped)
                .resultStatus(resolveStatus(target)))
                .build();
    }

    /** A weapon in hand or an equipped Escudo, neither an Arma Natural nor proofed against disarming. */
    private static boolean isDroppable(final Character character, final Item item) {
        boolean proofed = (item.getMasterpiece() != null && item.getMasterpiece().preventsDisarming())
                || item.getImprovements().stream().anyMatch(Improvement::preventsDisarming);
        if (proofed) {
            return false;
        }
        if (item.getCategory() == ItemCategory.SHIELD) {
            return character.getEquipment().contains(item);
        }
        return item instanceof Weapon weapon && character.isDrawn(weapon) && !character.treatsAsNaturalWeapon(weapon);
    }
}
