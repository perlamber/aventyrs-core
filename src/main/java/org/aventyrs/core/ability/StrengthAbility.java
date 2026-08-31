package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;

@Getter
@AllArgsConstructor
public enum StrengthAbility implements AttributeAbility {

    BESTA_DE_CARGA("Equipamentos são considerados uma Categoria de Peso inferiores para você (Pesados são " +
            "considerados Médios, Médios são considerados Leves). Esta Habilidade não muda a Categoria Base do " +
            "Equipamento, mas permite receber os benefícios da mudança de Categoria."),

    DESTRUIDOR_DE_MUROS("O primeiro ataque que realizar a cada Rodada utiliza seu valor de Força integral nas " +
            "rolagens de dano, ao invés da metade."),

    MOVIMENTO_LIVRE("O primeiro Ponto de Ação (PA) que utilizar em cada Rodada para mover-se, escalar ou nadar " +
            "ignora efeitos de Terreno Difícil."),

    SUBJUGAR("Você recebe Vantagem em suas rolagens de Ataque Corpo-a-corpo para Agarrar, Derrubar ou Empurrar " +
            "outros personagens. Você recebe Vantagem em rolagens de Dano para atacar personagens caídos ou " +
            "desprevenidos."),

    // TODO: the Vantagem half is scoped to a target classification this core does not carry
    //  (objects and construtos are not a CreatureType), and the shield damage needs a hook on
    //  AttackDelivery: dealing it is expressible now (Item#applyDamage, and Item#isDestroyed is
    //  exactly the "caso o escudo seja destruído" fallback condition), but nothing resolves which
    //  of the target's equipped items is the escudo and rolls the damage onto it.
    ESTILHACADOR("Você recebe Vantagem em rolagens de Perícias de Ataque (baseadas em Força) e Danos efetuadas " +
            "contra objetos e construtos. Adicionalmente, seus Ataques Corpo-a-Corpo bem-sucedidos contra outros " +
            "personagens causam 2 pontos de dano ao item tipo 'escudo' que o alvo esteja utilizando; caso o " +
            "escudo seja destruído, ou o alvo não utilize um, este dano é causado a um item tipo 'armadura'.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.STRENGTH;
    }
}
