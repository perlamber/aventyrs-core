package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum DexterityAbility implements AttributeAbility {

    // The "+1UD Movimento Base" clause is real (movementBonus() below) — Movimento Base being
    // per Ponto de Ação, this widens every point spent moving, which is what the clause says.
    // TODO: "seu primeiro movimento em cada Rodada tem a distância aumentada em +2UD" stays
    // unimplemented — it's conditioned on *which* movement in the Rodada this is (the first),
    // which MovementService's single per-Ponto-de-Ação figure has no way to express; needs a
    // per-movement-action tracking concept this core doesn't have.
    PASSOS_LONGOS("Seu Movimento Base aumenta em +1UD. Seu primeiro movimento em cada Rodada tem a distância " +
            "aumentada em +2UD.") {
        @Modifier(ModifierType.MOVEMENT)
        public int movementBonus() {
            return 1;
        }
    },
    //TODO bonus no ataque corpo a corpo e mecanica de investida.
    IMPLACAVEL("Suas investidas não provocam Reações de outros personagens na cena e você pode percorrer até o " +
            "triplo do seu Movimento Base, ao invés do dobro. Adicionalmente você recebe Vantagem em suas jogadas " +
            "de Ataque Corpo-a-Corpo; quando malsucedido, você não recebe o redutor padrão de -2 em suas Defesas."),

    APRESSADO("Em turnos de Rodadas pares você recebe Bônus Variável de +1PA.") {
        /**
         * turnNumber is 0-based — Turn 0 <em>is</em> the first Rodada — so an even Rodada is an
         * odd turnNumber: the 2nd Rodada is Turn 1, the 4th is Turn 3, and so on.
         */
        @Override
        public int resolveActionPointsBonus(final int turnNumber) {
            return turnNumber % 2 == 1 ? EVEN_ROUND_ACTION_POINTS_BONUS : 0;
        }
    },

    LETALIDADE_PROGRESSIVA("A Margem Crítica de seus Ataques à Distância aumenta ao longo do combate: você recebe " +
            "Bônus Variável de +1 em sua Margem Crítica menor na primeira Rodada do combate, então este Bônus " +
            "aumenta para +2 na terceira Rodada e para +3 na quinta Rodada. Estes benefícios são encerrados na " +
            "sexta Rodada do combate.") {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext) {
            if (sceneContext == null || !sceneContext.isCombatScene() || skillType != SkillType.ATAQUE_A_DISTANCIA) {
                return 0;
            }
            int round = sceneContext.getCurrentRound();
            if (round < FIRST_TIER_ROUND || round >= BENEFIT_ENDS_ROUND) {
                return 0;
            }
            if (round >= THIRD_TIER_ROUND) {
                return THIRD_TIER_MARGIN_BONUS;
            }
            if (round >= SECOND_TIER_ROUND) {
                return SECOND_TIER_MARGIN_BONUS;
            }
            return FIRST_TIER_MARGIN_BONUS;
        }
    },
    PRECISAO("Você adquire Vantagem na primeira rolagem de Perícias baseada em Destreza realizada em cada um de " +
            "seus Turnos.") {
        @Override
        public Optional<Integer> resolveFirstRollOfTurnBonus(final AttributeDomain rolledDomain) {
            return rolledDomain == AttributeDomain.DEXTERITY ? Optional.of(Skill.ADVANTAGE_BONUS) : Optional.empty();
        }
    };

    /** APRESSADO's PA bonus on every even Rodada (turnNumber 1, 3, 5, ...). */
    private static final int EVEN_ROUND_ACTION_POINTS_BONUS = 1;

    /** LETALIDADE_PROGRESSIVA's Margem Crítica Menor bonus starting the 1st Round of combate. */
    private static final int FIRST_TIER_ROUND = 1;
    private static final int FIRST_TIER_MARGIN_BONUS = 1;

    /** LETALIDADE_PROGRESSIVA's Margem Crítica Menor bonus starting the 3rd Round of combate. */
    private static final int SECOND_TIER_ROUND = 3;
    private static final int SECOND_TIER_MARGIN_BONUS = 2;

    /** LETALIDADE_PROGRESSIVA's Margem Crítica Menor bonus starting the 5th Round of combate. */
    private static final int THIRD_TIER_ROUND = 5;
    private static final int THIRD_TIER_MARGIN_BONUS = 3;

    /** LETALIDADE_PROGRESSIVA's benefits end once combate reaches its 6th Round. */
    private static final int BENEFIT_ENDS_ROUND = 6;

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.DEXTERITY;
    }
}
