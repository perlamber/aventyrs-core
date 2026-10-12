package org.aventyrs.core.item;

import org.aventyrs.core.effect.CriticalEffectType;
import lombok.Builder;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * A plain builder-built {@link Weapon} — {@link AbstractItem} plus the two columns a weapon
 * adds, for a one-off or caller-supplied Equipamento Ofensivo that doesn't belong in a catalog
 * enum.
 * The same role {@code AbstractItem} plays for a non-weapon, and {@code
 * org.aventyrs.core.monster.AbstractMonsterTemplate} plays for a foe.
 *
 * <p>It extends {@code AbstractItem} rather than restating its ten columns, which is why both
 * use {@code @SuperBuilder}: {@code AbstractWeapon.builder()} offers every {@code AbstractItem}
 * field alongside {@code damageBase}/{@code skillType}, and {@code AbstractItem.builder()} is
 * unaffected.
 *
 * <p>Two of the three fields here are {@code @NonNull} — unlike every inherited one, which
 * follows this codebase's usual "a builder is a data holder, not a gatekeeper" restraint. The
 * exception is narrow and deliberate: neither is a questionable value a caller might mean. A
 * {@code null} Dano Base is a {@link NullPointerException} the moment anything asks what the
 * weapon hits for (a weapon that deals bare-fist dano says so, with {@link DamageBase#UNARMED}),
 * and a {@code null} {@code skillType} is one the moment {@code
 * org.aventyrs.core.character.services.DamageBaseService} asks which Perícia's grants apply to
 * a swing — that column is now what selects them, not a caller-supplied argument.
 *
 * <p>{@code range} is the third column and the odd one out: it is a plain {@code
 * @Builder.Default} of {@link Range#ADJACENTE}, not {@code @NonNull}. A weapon that never states
 * an Alcance <em>is</em> a corpo-a-corpo one, so the default is a real answer rather than a
 * stand-in, and the great majority of call sites (every test builder among them) don't consult
 * it — see {@link Weapon#getRange()}. A weapon de Ataque à Distância or de Arremesso sets it.
 */
@Getter
@SuperBuilder
public class AbstractWeapon extends AbstractItem implements Weapon {

    @NonNull
    private DamageBase damageBase;

    @NonNull
    private SkillType skillType;

    @Builder.Default
    private Range range = Range.ADJACENTE;

    /**
     * This copy's own Efeito Crítico — its catalog entry's, carried by {@code
     * AbstractItem#builderFromTemplate} so a forged or bought blade keeps the "Sangramento" its
     * column prints. {@code null} for a one-off weapon that names none.
     */
    private CriticalEffectType criticalEffect;

    /** This copy's own Margem Crítica Menor — its catalog entry's, carried the same way. */
    @Builder.Default
    private int lesserCriticalMargin = Weapon.DEFAULT_LESSER_CRITICAL_MARGIN;

    /** What this copy fires — its catalog entry's {@link Weapon#getAmmunitionKind()}, carried the same way. */
    private AmmunitionKind ammunitionKind;

    /** Its catalog entry's {@link Weapon#getMagazineCapacity()}. */
    private int magazineCapacity;

    /** Its catalog entry's {@link Weapon#getReloadCost()}. */
    private ActionCost reloadCost;

    /** Its catalog entry's {@link Weapon#getFavoredReloadCost()}. */
    private ActionCost favoredReloadCost;

    /**
     * The projectiles loaded in this copy's magazine, in firing order — the next shot is the first. Per-copy state
     * that persists across Cenas (table ruling, 2026-10-12): a Besta stays loaded until it is fired. Always empty for
     * a weapon with no magazine. Filled by {@code AmmunitionService#reload}, emptied by {@link #fireLoaded()}.
     */
    @Builder.Default
    private List<ProjectileItem> loadedProjectiles = new ArrayList<>();

    /** A copy can attack exactly when its catalog entry can — see {@link Weapon#canAttack()}. */
    @Override
    public boolean canAttack() {
        return !(getTemplate() instanceof Weapon template) || template.canAttack();
    }

    /** Loads projectile into the magazine — a plain mutator; capacity is {@code AmmunitionService}'s to check. */
    public void load(final ProjectileItem projectile) {
        loadedProjectiles.add(projectile);
    }

    /** The projectile the next shot fires, or {@code null} while unloaded. */
    public ProjectileItem nextLoaded() {
        return loadedProjectiles.isEmpty() ? null : loadedProjectiles.get(0);
    }

    /** Removes and returns the projectile at the front of the magazine, or {@code null} while unloaded. */
    public ProjectileItem fireLoaded() {
        return loadedProjectiles.isEmpty() ? null : loadedProjectiles.remove(0);
    }
}
