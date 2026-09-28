package org.aventyrs.core.monster.model;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.monster.MonsterBlueprint;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.MonstrousAbilitySelection;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;

import java.util.List;

/** Shared scaffolding for the per-Modelo tests: a monster holding exactly the Habilidades under test. */
public final class MonsterTestKit {

    private MonsterTestKit() {
    }

    /**
     * A monster at powerDegree holding selections, and the Modelos they belong to. Every Atributo at
     * base 1 unless the caller builds its own; not validated, since a test often holds a Habilidade
     * the budget wouldn't allow at that Grau de Poder.
     */
    public static MonsterBlueprint.MonsterBlueprintBuilder blueprint(final int powerDegree,
                                                                     final MonstrousAbilitySelection... selections) {
        MonsterBlueprint.MonsterBlueprintBuilder builder = MonsterBlueprint.builder().name("Teste").powerDegree(powerDegree);
        for (MonstrousAbilitySelection selection : selections) {
            builder.ability(selection);
        }
        List.of(selections).stream().map(selection -> selection.ability().getModel()).distinct().forEach(builder::model);
        return builder;
    }

    public static MonsterSheet spawn(final int powerDegree, final MonstrousAbilitySelection... selections) {
        return blueprint(powerDegree, selections).build().spawn(new Player());
    }

    /** The held Efeito Ativo named name — fails the test when absent. */
    public static ActiveAbility held(final CombatantSheet sheet, final String name) {
        return sheet.getCharacter().getActiveAbilities().stream()
                .filter(ability -> ability instanceof MonstrousActiveAbility monstrous && monstrous.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Efeito Ativo named " + name));
    }

    /** What 10 raw damage of this kind becomes on sheet, by the real mitigation path. */
    public static int finalDamage(final CombatantSheet sheet, final DamageType type, final ElementalType element) {
        DamageDescriptor descriptor = element == null ? new DamageDescriptor(type) : new DamageDescriptor(type, element);
        return new DamageServiceImpl().calculateFinalDamage(sheet, null, descriptor, null, 10, false);
    }
}
