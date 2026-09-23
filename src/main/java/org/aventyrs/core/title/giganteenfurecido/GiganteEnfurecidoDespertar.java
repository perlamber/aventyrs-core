package org.aventyrs.core.title.giganteenfurecido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Gigante Enfurecido's Despertar — unlike Santo's and Senhor da Briga's, an <b>activated</b> one:
 * "Custo de Ativação: 1 Ponto Temporário de Autocontrole, Tempo de Ativação: 2PA … Você pode entrar
 * em um estado de Frenesi profundo". So it is a trait like any other, activated through {@code
 * AventyrTitle#activateAbility}, rather than a scanned passive.
 *
 * <p>Its own enum because it is <b>held by every Gigante Enfurecido from Awakening</b>, never
 * acquired: {@link GiganteEnfurecido#getAllAbilities()} always includes it, while {@code
 * getAbilities()} — what every "Requer N Habilidades" prerequisite counts — never does. Kept out of
 * {@link GiganteEnfurecidoAbility} so nothing offers it for acquisition.
 */
@Getter
@AllArgsConstructor
public enum GiganteEnfurecidoDespertar implements AventyrTitleAbility {

    // Real through FrenesiInteraction. "Bônus Variável de +2 em 'Força' e 'Iniciativa'" are the
    // sheet.Frenzy's STRENGTH_BONUS/INITIATIVE, which CombatantSheet#getTemporaryBonus folds in, so the
    // Força total (and with it the melee dano) and the Iniciativa see them. The Duração is
    // "2+ Metade do seu 'Vigor'" (+2 Rodadas as Título Primário), counted at the holder's Turn start.
    // "não pode ser interrompida voluntariamente" — only GiganteEnfurecido#endFrenzyVoluntarily, gated
    // on Uno com a Ira, ends it early. The concentration block is Frenzy#isConcentrationBlocked, read by
    // CombatantSheet#isSkillUsePrevented (Gnose Perícias, Domínio do Mana) and isSpellCastingPrevented
    // (Conjurar and Mimetizar). The Autocontrole spent comes back "1 a cada 2 horas" through
    // CombatantSheet#oweHourlyEgoRecovery/passHours. At 0 Autocontrole the holder is compelled to
    // attack the nearest creature (CombatantSheet#isCompelledToAttackNearest — the geometry is the
    // caller's).
    FRENESI(
            "Você pode entrar em um estado de Frenesi profundo, se o fizer você adquire temporariamente " +
            "Bônus Variável de +2 em 'Força' e 'Iniciativa'. Esta Habilidade permanece ativa por uma " +
            "quantidade de Rodadas igual 2+ Metade do seu 'Vigor' e não pode ser interrompida " +
            "voluntariamente, enquanto em Frenesi você não pode utilizar Perícias que exijam contração ou " +
            "raciocínio, você também perde a capacidade de Conjurar ou Mimetizar Magias. Pontos de " +
            "Autocontrole perdidos durante o 'Frenesi' são recuperados 1 a cada 2 horas passadas fora " +
            "deste estado, caso a 'Autocontrole' chegue a zero você perde a capacidade de distinguir " +
            "inimigos e aliados (nunca sofrendo outros efeitos de Autocontrole nulo), sempre atacando a " +
            "criatura mais próxima de você.",
            fixed(0), EgoCost.autocontrole(1), ActionCost.ofActionPoints(2),
            Optional.of(FrenesiInteraction.class));

    private final String description;
    private final PDCost PDCost;
    private final EgoCost egoCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
}
