package org.aventyrs.core.item;

import lombok.Builder;
import lombok.Getter;
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
}
