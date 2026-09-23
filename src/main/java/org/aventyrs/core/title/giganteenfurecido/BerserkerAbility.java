package org.aventyrs.core.title.giganteenfurecido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * The Habilidades/Supremas gated on Especialização 'Berserker'. Their "Requer N Habilidades" clauses
 * name that Especialização — the default scope.
 */
@Getter
@AllArgsConstructor
public enum BerserkerAbility implements AventyrTitleAbility {

    // Requer Especialização 'Berserker'. A Reação on ReactionTrigger#ADJACENT_ENEMY_ATTACKS_OTHER, real
    // through RetaliacaoFuriosaInteraction: it grants one retaliation attack against that enemy, whose
    // dano GiganteEnfurecido#resolveAttackModifiers fixes "independente da arma" as a
    // TitleAttackModifiers.DamageOverride — 1d6+Metade da Força, or 2d6+Força at 0 PV or below. "apenas
    // 1 vez por Rodada para cada inimigo" is a per-Rodada, per-enemy mark.
    RETALIACAO_FURIOSA(
            "Sempre que um Personagem Inimigo adjacente atacar outros personagens que não você, você se sentirá " +
            "desprezado e irritado. Você pode desferir um ataque contra estes inimigos, o dano causado será " +
            "1d6+Metade da Força, independente da arma que esteja utilizando. Se seus PV forem menores ou " +
            "iguais a zero este dano muda para 2d6+Força integral. Este Habilidade só pode ser ativada apenas 1 " +
            "vez por Rodada para cada inimigo, mesmo que você possua outras Reações disponíveis.",
            false, fixed(0), EgoCost.NONE, ActionCost.REACTION,
            Optional.of(RetaliacaoFuriosaInteraction.class), 0) {
        @Override
        public ReactionTrigger getReactionTrigger() {
            return ReactionTrigger.ADJACENT_ENEMY_ATTACKS_OTHER;
        }

        /** An adjacent enemy, not yet retaliated against this Rodada. */
        @Override
        public boolean isReactionAvailable(final ReactionContext context) {
            return RetaliacaoFuriosaInteraction.isAvailableAgainst(context.getReactor(), context.getAttacker(),
                    context.getReactorContext());
        }
    },

    // Requer Especialização 'Berserker'. Real through FrenesiAssustadorInteraction: after a kill or an
    // Acerto Crítico — which the caller reports through GiganteEnfurecido#recordTriumph — enemies in
    // Distância Curta with more temporary Autocontrole than the activator (a foe with no pool counts
    // 0) climb one rung of the fear ladder for 2 Rodadas, as an Encantamento (sheet.FrightfulCondition).
    // Their "incapazes de desferir Efeitos Críticos Menores e não podem desencadear Correntes" while the
    // activator is at 0 PV or below is CombatantSheet#isMinorCriticalAndChainSuppressed, read by
    // AttackDelivery. "Tempo de Ativação: Nenhum" is an Ação Livre.
    FRENESI_ASSUSTADOR(
            "Esta Habilidade só pode ser ativada ao derrotar um inimigo (reduzindo seus PV à zero ou menos) ou " +
            "após desferir um Acerto Crítico, mas é limitada a uma Ativação por Rodada. Inimigos em Distância " +
            "Curta com valores de 'Autocontrole' maiores do que o seu ficam assustados com sua presença e " +
            "recebem a Condição Abalado por 2 Rodadas (Efeito de Encantamento), ativações posteriores do " +
            "Frenesi Assustador podem progredir a Condição para Assustado e Apavorado, renovando a Duração da " +
            "Condição. Enquanto seus PV forem menores ou iguais a zero os inimigos Abalados, Assustados e " +
            "Apavorados se tornam incapazes de desferir Efeitos Críticos Menores e não podem desencadear " +
            "Correntes de Efeitos.",
            false, fixed(2), EgoCost.NONE, ActionCost.FREE_ACTION,
            Optional.of(FrenesiAssustadorInteraction.class), 0),

    // Requer Especialização 'Berserker'. Passive, real through FrenesiInteraction: every Frenesi of its
    // holder carries RA (one instance) and Frenzy#isScornsDamage, which makes CombatantSheet
    // #getTemporaryBonus report Meio-Dano and heal() halve every cure but Roubo de Vida while the holder
    // is at 0 PV or below.
    DESPREZAR_DANOS(
            "Enquanto estiver em Frenesi você recebe RA. Enquanto seus PV forem menores ou iguais à zero, você " +
            "reduz danos (Efeito de Meio-Dano) e Efeitos de Cura (exceto Roubo de Vida) pela metade.",
            false, fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(), 0),

    // Requer 2 Habilidades de 'Berserker'. A Reação on ReactionTrigger#SELF_WOULD_DROP_TO_ZERO_HP, real
    // through FanaticoDeCytInteraction: every current temporary Autocontrole spent (mínimo 1), the
    // incoming hit floored at 1 PV (CombatantSheet#floorNextDamageAt), Roubo de Vida equal to the points
    // for the rest of the Frenesi, and the Frenzy marked fanatic — no death from negative PV while the
    // Frenesi runs at 0 Autocontrole (HitPointsService#getStatus stops at Coma), everyone an enemy, and
    // every Magia from someone else hostile (SpellCastingResult#getMustOvercomeMagicDefense).
    FANATICO_DE_CYT(
            "Enquanto estiver em Frenesi, se um ataque ou efeito for reduzir seus PV para zero ou menos você " +
            "pode gastar temporariamente todos os seus pontos de Autocontrole atuais (mínimo 1), se o fizer " +
            "você ficará com 1PV. Você recebe Roubo de Vida igual à quantidade de Pontos de Autocontrole " +
            "utilizados e não poderá morrer em decorrência de PV negativos enquanto seu Frenesi estiver ativo e " +
            "seu Autocontrole continuar zerado. Neste estado você vê todos os outros personagens como " +
            "inimigos, sem exceções, sempre atacando quem estiver mais próximo de você. Magias Conjuradas por " +
            "terceiros sempre contam como magias hostis, mesmo as magias benéficas - como as de cura - devem " +
            "superar sua DM para lhe afetar.",
            true, fixed(0), EgoCost.autocontrole(1), ActionCost.REACTION,
            Optional.of(FanaticoDeCytInteraction.class), 2) {
        @Override
        public ReactionTrigger getReactionTrigger() {
            return ReactionTrigger.SELF_WOULD_DROP_TO_ZERO_HP;
        }

        /** "Enquanto estiver em Frenesi", with at least the "mínimo 1" point to spend. */
        @Override
        public boolean isReactionAvailable(final ReactionContext context) {
            return context.getReactor().getOwnFrenzy().isPresent()
                    && context.getReactor().getTemporaryEgoPoints(EgoDomain.AUTOCONTROLE) >= 1;
        }
    };

    /** Retaliação Furiosa's and Frenesi Assustador's reach. */
    static final Range ADJACENT = Range.ADJACENTE;

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final EgoCost egoCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final int requiredOtherAbilities;

    @Override
    public Optional<AventyrTitleSpecialization> getRequiredSpecialization() {
        return Optional.of(GiganteEnfurecidoSpecialization.BERSERKER);
    }
}
