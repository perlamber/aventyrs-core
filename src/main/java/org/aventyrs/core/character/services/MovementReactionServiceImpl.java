package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillType;

import java.util.ArrayList;
import java.util.List;

public class MovementReactionServiceImpl implements MovementReactionService {

    private final AttackRangeService attackRangeService;

    public MovementReactionServiceImpl() {
        this(new AttackRangeServiceImpl());
    }

    public MovementReactionServiceImpl(final AttackRangeService attackRangeService) {
        this.attackRangeService = attackRangeService;
    }

    @Override
    public List<CombatantSheet> getProvokedReactors(@NonNull final CombatantSheet mover,
                                                     final SceneContext sceneContext,
                                                     final Manoeuvre manoeuvre) {
        if (sceneContext == null || isExempt(mover.getCharacter(), manoeuvre)) {
            return List.of();
        }
        List<CombatantSheet> reactors = new ArrayList<>();
        for (CombatantSheet enemy : sceneContext.getEnemies()) {
            Range distance = sceneContext.getDistanceTo(enemy);
            if (distance != null && threatens(enemy, distance)) {
                reactors.add(enemy);
            }
        }
        return List.copyOf(reactors);
    }

    /**
     * Whether any Habilidade de Atributo the mover holds exempts <em>this</em> manoeuvre. Scanned
     * across {@code Character#getAttributeAbilities()}, which folds in the ones a Talento granted.
     *
     * <p>There is no {@code Feat} pass. No Talento states a movement-Reação exemption that is
     * expressible yet: {@code MobilidadeFeat#MOVIMENTO_ACROBATICO}'s "sem provocar Reações" is
     * scoped to the Reposicionar manoeuvre, which is not a {@link Manoeuvre} constant, and {@code
     * ActionProfile#CONSCIENCIA_DEFENSIVA}'s is scoped to a movement's first 2UD, a distance
     * nothing records. Add the pass with the first clause that can actually use it.
     */
    private boolean isExempt(final Character mover, final Manoeuvre manoeuvre) {
        return mover.getAttributeAbilities().stream()
                .anyMatch((AttributeAbility ability) -> ability.exemptsFromMovementReactions(manoeuvre));
    }

    /**
     * Whether this enemy threatens a combatant standing distance away: do they have anything in
     * hand that is swung as an Ataque Corpo-a-Corpo and reaches that far?
     *
     * <p>Drawn weapons <b>plus</b> Armas Naturais, and the split matters. A sheathed blade threatens
     * nobody — "utilizando uma arma" means drawn throughout this core ({@code
     * Character#drawnWeapons} is the in-hand subset of the equipment list) — while an Arma Natural
     * cannot be sheathed at all, so it always counts. Read through {@code
     * CombatantSheet#getNaturalWeapons()} rather than the {@code Character} view, so a Forma that
     * suppresses its holder's natural weapons or grants its own is already reflected.
     */
    private boolean threatens(final CombatantSheet enemy, final Range distance) {
        Character character = enemy.getCharacter();
        List<Weapon> inHand = new ArrayList<>(character.getDrawnWeapons());
        inHand.addAll(enemy.getNaturalWeapons());
        return inHand.stream().anyMatch(weapon -> reaches(character, weapon, distance));
    }

    /**
     * The Ataque-à-Distância exclusion and the reach test, in that order.
     *
     * <p>The reach comes from {@link AttackRangeService#getEffectiveRange(Character, Weapon)} — the
     * <em>band</em> form, which is the right one here twice over: this is an {@code isWithin}
     * question rather than a measurement (see that service's own note on the two forms), and a
     * {@code SceneContext} holds distances as bands to begin with. So a Lança outreaches an adaga
     * for this purpose, and a larger enemy outreaches a smaller one holding the same weapon.
     */
    private boolean reaches(final Character enemy, final Weapon weapon, final Range distance) {
        return weapon.getAttackSkillType() == SkillType.ATAQUE_CORPO_A_CORPO
                && distance.isWithin(attackRangeService.getEffectiveRange(enemy, weapon));
    }
}
