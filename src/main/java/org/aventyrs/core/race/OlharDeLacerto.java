package org.aventyrs.core.race;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;

/**
 * Olhar de Lacerto — the Górgona's gaze, as the {@link AttackSource} its attack is made with: "Esta
 * ação exige uma rolagem de Ataque Corpo-a-Corpo, em até Distância Muito Curta, efetuada contra a
 * DM do alvo, o alvo sofre 1d6+Metade do Carisma pontos de Dano Mágico Elemental: Terra".
 *
 * <p>An attack source rather than a Talento flag so that every clause scoped to it reaches the roll
 * through the hooks that already take one — {@code GorgonaFeat#CABELO_SERPENTINO}'s Vantagem, {@code
 * GorgonaFeat#MARCA_DA_MALDICAO}'s Alcance. Declared, paid and landed through {@code
 * OlharDeLacertoService}; the attack itself is an ordinary {@code DeliveredAttack} naming this
 * source and the target's DM.
 */
public enum OlharDeLacerto implements AttackSource {

    INSTANCE;

    /** "Em até Distância Muito Curta." */
    public static final Range BASE_RANGE = Range.DISTANCIA_MUITO_CURTA;

    /** "Dano Mágico Elemental: Terra." */
    public static final DamageDescriptor DAMAGE = new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.TERRA);

    @Override
    public SkillType getAttackSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }
}
