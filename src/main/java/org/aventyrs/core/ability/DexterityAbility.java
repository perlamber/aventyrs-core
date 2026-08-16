package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

@Getter
@AllArgsConstructor
public enum DexterityAbility implements AttributeAbility {

    // The "+1UD Movimento Base" clause is real (movementBonus() below). TODO: "seu primeiro
    // movimento em cada Rodada tem a distância aumentada em +2UD" stays unimplemented — it's
    // conditioned on *which* movement in the Rodada this is (the first), which MovementService's
    // flat per-Turn total has no way to express; needs a per-movement-action tracking concept
    // this core doesn't have.
    PASSOS_LONGOS("Seu Movimento Base aumenta em +1UD. Seu primeiro movimento em cada Rodada tem a distância " +
            "aumentada em +2UD.") {
        @Modifier(ModifierType.MOVEMENT)
        public int movementBonus() {
            return 1;
        }
    },

    IMPLACAVEL("Suas investidas não provocam Reações de outros personagens na cena e você pode percorrer até o " +
            "triplo do seu Movimento Base, ao invés do dobro. Adicionalmente você recebe Vantagem em suas jogadas " +
            "de Ataque Corpo-a-Corpo; quando malsucedido, você não recebe o redutor padrão de -2 em suas Defesas."),

    APRESSADO("Em turnos de Rodadas pares você recebe Bônus Variável de +1PA."),

    LETALIDADE_PROGRESSIVA("A Margem Crítica de seus Ataques à Distância aumenta ao longo do combate: você recebe " +
            "Bônus Variável de +1 em sua Margem Crítica menor na primeira Rodada do combate, então este Bônus " +
            "aumenta para +2 na terceira Rodada e para +3 na quinta Rodada. Estes benefícios são encerrados na " +
            "sexta Rodada do combate."),

    PRECISAO("Você adquire Vantagem na primeira rolagem de Perícias baseada em Destreza realizada em cada um de " +
            "seus Turnos.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.DEXTERITY;
    }
}
