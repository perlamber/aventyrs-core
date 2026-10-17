package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;

@Getter
@AllArgsConstructor
public enum StrengthAbility implements AttributeAbility {

    // Real via resolveCarriedWeightClassShift and Character#carriedWeightClassOf, read only where lighter is a
    // benefit: the Destreza burden on Esquiva e Aparar, the two-hand budget (a Médio weapon in one hand), Defesa de
    // Mãos Limpas' and the Duelista's Leve checks. The Categoria Base stays: Especializações, the Aprimoramento cap
    // and a clause rewarding a heavy weapon read the authored column.
    BESTA_DE_CARGA("Equipamentos são considerados uma Categoria de Peso inferiores para você (Pesados são " +
            "considerados Médios, Médios são considerados Leves). Esta Habilidade não muda a Categoria Base do " +
            "Equipamento, mas permite receber os benefícios da mudança de Categoria.") {
        @Override
        public int resolveCarriedWeightClassShift() {
            return 1;
        }
    },

    /**
     * Real. The base rule adds half the attacker's Força to an Ataque Corpo-a-Corpo's dano roll;
     * this upgrades that to the full value on the Rodada's first attack, resolved by {@code
     * AbstractSkillInteraction}'s dano-bonus sum via {@link
     * AttributeAbility#upgradesFirstMeleeAttackOfRoundStrengthScaling()}.
     */
    DESTRUIDOR_DE_MUROS("O primeiro ataque que realizar a cada Rodada utiliza seu valor de Força integral nas " +
            "rolagens de dano, ao invés da metade.") {
        @Override
        public boolean upgradesFirstMeleeAttackOfRoundStrengthScaling() {
            return true;
        }
    },

    /**
     * The moving half is real: the Rodada's first movement (index 0, one Ponto de Ação's worth)
     * ignores Terreno Difícil, judged per movement by {@code MovementTerrainService}. Escalar and
     * nadar have no movement channel of their own here.
     */
    MOVIMENTO_LIVRE("O primeiro Ponto de Ação (PA) que utilizar em cada Rodada para mover-se, escalar ou nadar " +
            "ignora efeitos de Terreno Difícil.") {
        @Override
        public boolean ignoresDifficultTerrain(final int movementIndex) {
            return movementIndex == 0;
        }
    },

    // Real. The roll half via resolveManoeuvreRollBonus: Agarrar is Manoeuvre#AGARRAR, and Empurrar
    // and Derrubar are both the Encontrão (Manoeuvre#ENCONTRAO), which pushes and carries Lançar ao
    // Chão at its Corrente margin (table ruling, 2026-10-16). The dano half via resolveDamageRollBonus,
    // against a target that holds Caído or Desprevenido (Caído implies Desprevenido) on any attack.
    SUBJUGAR("Você recebe Vantagem em suas rolagens de Ataque Corpo-a-corpo para Agarrar, Derrubar ou Empurrar " +
            "outros personagens. Você recebe Vantagem em rolagens de Dano para atacar personagens caídos ou " +
            "desprevenidos.") {
        @Override
        public int resolveManoeuvreRollBonus(final org.aventyrs.core.action.Manoeuvre manoeuvre,
                                             final org.aventyrs.core.skill.SkillType skillType) {
            return (manoeuvre == org.aventyrs.core.action.Manoeuvre.AGARRAR
                    || manoeuvre == org.aventyrs.core.action.Manoeuvre.ENCONTRAO)
                    && skillType == org.aventyrs.core.skill.SkillType.ATAQUE_CORPO_A_CORPO
                    ? org.aventyrs.core.skill.Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public int resolveDamageRollBonus(final org.aventyrs.core.skill.SkillType skillType,
                                          final org.aventyrs.core.sheet.CombatantSheet holder,
                                          final org.aventyrs.core.sheet.CombatantSheet attackTarget,
                                          final org.aventyrs.core.scene.SceneContext sceneContext) {
            return attackTarget != null
                    && attackTarget.hasCondition(org.aventyrs.core.sheet.ConditionType.DESPREVENIDO, sceneContext)
                    ? org.aventyrs.core.skill.Skill.ADVANTAGE_BONUS : 0;
        }
    },

    // Real for objects and for the escudo/armadura damage. The roll Vantagem via the governing-domain
    // resolveManoeuvreRollBonus on a Força-governed Atacar Objetos roll, the dano Vantagem via
    // resolveObjectDamageBonus in ObjectAttackService#strike, and the 2 points via
    // resolveDefensiveEquipmentDamageOnHit, reported by AttackDelivery and dealt by combat.ShieldBreak on the
    // target's real sheet.
    // TODO: "e construtos" — no construto classification exists (CreatureType has none, and the monster
    //  rules define none); objects only until one does (table ruling, 2026-10-16).
    ESTILHACADOR("Você recebe Vantagem em rolagens de Perícias de Ataque (baseadas em Força) e Danos efetuadas " +
            "contra objetos e construtos. Adicionalmente, seus Ataques Corpo-a-Corpo bem-sucedidos contra outros " +
            "personagens causam 2 pontos de dano ao item tipo 'escudo' que o alvo esteja utilizando; caso o " +
            "escudo seja destruído, ou o alvo não utilize um, este dano é causado a um item tipo 'armadura'.") {
        @Override
        public int resolveManoeuvreRollBonus(final org.aventyrs.core.action.Manoeuvre manoeuvre,
                                             final org.aventyrs.core.skill.SkillType skillType,
                                             final AttributeDomain governingDomain) {
            return manoeuvre == org.aventyrs.core.action.Manoeuvre.ATACAR_OBJETOS
                    && skillType.isAttackSkill() && governingDomain == AttributeDomain.STRENGTH
                    ? org.aventyrs.core.skill.Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public int resolveObjectDamageBonus(final AttributeDomain governingDomain) {
            return governingDomain == AttributeDomain.STRENGTH ? org.aventyrs.core.skill.Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public int resolveDefensiveEquipmentDamageOnHit(final org.aventyrs.core.skill.SkillType skillType) {
            return skillType == org.aventyrs.core.skill.SkillType.ATAQUE_CORPO_A_CORPO
                    ? org.aventyrs.core.combat.ShieldBreak.ESTILHACADOR_DAMAGE : 0;
        }
    };

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.STRENGTH;
    }
}
