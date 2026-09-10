package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * POLIMORFISMO (Encantamento/Natural) — nine Magias, the catalog's joint-largest tree, diverging
 * at Broto into a shrinking line and a growing line that never converge.
 *
 * <p>It is the tree with the most {@code Pessoal ou Toque} entries: five of its nine are
 * dual-reach, authoring both a {@code targeting} and an {@code alternateTargeting}.
 *
 * <h2>Attribute changes here are round-scoped, which {@code AttributeValue} cannot hold</h2>
 *
 * Almost every effect is a temporary Força/Destreza swing. TODO {@code AttributeValue} has only
 * {@code base}, {@code racialBonus} and {@code variable}, all permanent, and none is ever summed
 * through a {@code ModifierType} — so a Rodada-scoped Attribute bonus has no representation at
 * all. Size changes fare better: {@code CharacterSizeService#getEffectiveSizeCategory} resolves a
 * shift for real, and the PV multiplier is a real {@code Character} field.
 *
 * <p>"Este efeito não reduz Atributos à um total de zero ou menos" recurs as a floor of 1 rather
 * than the 0 every {@code <Stat>Service} clamps at, and would need its own clamp wherever the
 * round-scoped mechanism eventually lands.
 */
public enum PolimorfismoSpell implements AuthoredSpell {

    /**
     * <b>The catalog's only GD conditioned on whose side the target is on</b> — "Fácil (13|14)
     * para alvos aliados ou pessoal, DM para alvo para inimigos". That is not quite the "ou DM do
     * Alvo (maior)" floor the other entries use: a floor takes whichever is <em>higher</em>, while
     * this picks by allegiance. Authored as the floor anyway, since it produces the same answer in
     * every case a floor is asked about and there is no ally/enemy discriminator on a Magia; the
     * exact clause is here rather than lost.
     */
    REARRANJO_CORPORAL(SpellData.builder()
            .name("Rearranjo Corporal")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.REACAO)
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.EASY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Lança um raio em uma criatura viva, cujos efeitos são capazes de modificar brevemente o "
                    + "corpo do alvo, o atrapalhando ou auxiliando em sua ação atual.")
            .primaryEffectDescription("O conjurador consegue alterar momentaneamente partes do corpo do alvo, com "
                    + "seu alvo, por exemplo fazendo encolher um braço para que um inimigo erre seu ataque, ou que "
                    + "um aliado cresça alguns centímetros para alcançar um lugar mais alto, concedendo assim "
                    + "Vantagem ou Desvantagem na rolagem de Perícia do alvo. "
                    + "Se o Alvo for um PDN a Dificuldade para superá-lo em sua Perícia ativa é reduzida em 2. "
                    + "Rearranjo Corporal lançado por um conjurador específico não é capaz de afetar o mesmo alvo "
                    + "duas vezes, até que o alvo passe por seu próximo descanso.")
            .secondaryEffectDescription("Rearranjo Estendido: Ao invés de Reação, o tempo de execução da magia muda "
                    + "para 1PA. Se o fizer a duração da magia muda para 1 rodada.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
            .build()),

    MURCHA_CORPO(SpellData.builder()
            .name("Murcha-Corpo")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.POLIMORFISMO_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Um personagem afetado por esta magia tem sua força e agilidade provisoriamente drenada.")
            .primaryEffectDescription("O Alvo desta magia recebe Redutor variável de -2 em Força ou Destreza. Este "
                    + "efeito não reduz Atributos à um total de zero ou menos.")
            .effectChainDescription("Murcha-Almas: Em substituição ao efeito anterior o alvo sofre Redutor de -2 em "
                    + "Força e Destreza.")
            .criticalEffectType(CriticalEffectType.DILACERAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MEDIA))
            .build()),

    /** Its Perícia line reads "Domínio de Mana", one of four such spellings of <i>Domínio do Mana</i>. */
    INFLA_MUSCULOS(SpellData.builder()
            .name("Infla-Músculos")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.POLIMORFISMO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("O alvo do conjurador tem sua força física aumentada.")
            .primaryEffectDescription("Um personagem tocado pelo conjurador adquire temporariamente Bônus de +1 em "
                    + "Força ou 1 de Destreza.")
            .effectChainDescription("Inflar o Ego: Em substituição ao efeito anterior o alvo recebe Bônus de +2 em "
                    + "Força ou Destreza.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * The one Magia in the catalog carrying <b>two</b> Corrente lines — {@code Corrente de Efeitos
     * – Espremer} and {@code Corrente de Efeitos Alternativa – Fraqueza Momentânea}. Both are
     * transcribed into the single {@code effectChainDescription}, headed as the document heads
     * them, since the field is prose rather than a list and nothing resolves either.
     */
    SERRA_PERNAS(SpellData.builder()
            .name("Serra-Pernas")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.POLIMORFISMO_PRINCIPAL)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Um raio que, ao acertar o alvo, reduz seu tamanho e capacidades físicas.")
            .primaryEffectDescription("O conjurador lança de suas mãos um raio que, ao afetar o alvo, concede "
                    + "redutor variável de -2 em Força e Destreza. Esta magia não reduz Atributos à um total de zero "
                    + "ou menos. "
                    + "O conjurador desta magia pode gastar PM adicional em sua conjuração, aumentado a sua duração "
                    + "em 2 rodadas para cada PM gasto desta maneira.")
            .effectChainDescription("Espremer: Em adicional aos efeitos anteriores, o alvo desta magia tem sua "
                    + "Categoria de Tamanho reduzida em 1 número. "
                    + "Corrente de Efeitos Alternativa – Fraqueza Momentânea: Em seu próximo Turno o alvo sofre "
                    + "Desvantagem em suas rolagens de Perícias baseadas em Força e Destreza. Adicionalmente o alvo "
                    + "é amaldiçoado durante toda a Duração desta magia.")
            .criticalEffectType(CriticalEffectType.DILACERAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MEDIA))
            .build()),

    OGRIFICAR(SpellData.builder()
            .name("Ogrificar")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.POLIMORFISMO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Ao modificar o corpo de seu alvo, esta magia concede ao mesmo maior força ou agilidade.")
            .primaryEffectDescription("Alvo tocado adquire bônus variável de +2 em Força ou Destreza.")
            .effectChainDescription("Gigantecer: Em substituição ao efeito anterior o alvo recebe Bônus de +2 em "
                    + "Força e Destreza, a Categoria de Tamanho do alvo aumenta em +1.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /** Its Perícia line reads "Ataque Corpo-a-corpo", the document's one lowercase spelling of it. */
    TOQUE_DE_NANICOLINA(SpellData.builder()
            .name("Toque de Nanicolina")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.POLIMORFISMO_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Criaturas tocadas por esta magia tem seu tamanho bastante reduzido, além de perder a "
                    + "grande parte de suas capacidades físicas.")
            .primaryEffectDescription("O toque do conjurador com essa magia reduz em -2 a Categoria de Tamanho do "
                    + "Alvo, que adicionalmente sofre Redutor -3 em Força e Destreza.")
            .secondaryEffectDescription("Aura do Encolhimento: O Alcance desta magia é alterado para Pessoal, a GD "
                    + "para Difícil e a Duração aumentada para +2 Minutos. Você e até dois aliados adjacentes tem a "
                    + "Categoria de Tamanho reduzida em -2.")
            .criticalEffectType(CriticalEffectType.DILACERAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /** "seu multiplicador de PV … aumentadas em +2" is a real {@code Character#lifeMultiplier} change, unlike the Attribute half. */
    TITANECER(SpellData.builder()
            .name("Titânecer")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.POLIMORFISMO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("O alvo desta magia adquire grande Força, Resistência e Agilidade, e tem seu tamanho "
                    + "aumentado, assumindo as características dos lendários Titãs.")
            .primaryEffectDescription("O alvo recebe Bônus de +2 em Força e Destreza, seu multiplicador de PV e sua "
                    + "Categoria de Tamanho aumentadas em +2.")
            .secondaryEffectDescription("Armada Ôgrica: Você e mais dois aliados adjacentes, recebem os benefícios "
                    + "de Ogrificar. Os bônus concedidos podem ser escolhidos individualmente, este efeito não ativa "
                    + "a Corrente de Efeitos – Gigantecer.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "tem sua Força e Destrezas reduzidas à 1" sets an Attribute to a value rather than
     * modifying it — a shape no bonus mechanism in this core has, quite apart from Attribute
     * changes being permanent-only.
     */
    ENFADECER(SpellData.builder()
            .name("Enfadecer")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.POLIMORFISMO_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Uma magia poderosa, capaz de reduzir ao mínimo as capacidades físicas de seu alvo, ao "
                    + "mesmo que tempo que reduz drasticamente seu tamanho.")
            .primaryEffectDescription("Personagens tocados por esta magia tem sua Força e Destrezas reduzidas à 1, "
                    + "sua Categoria de Tamanho é reduzida em 3. "
                    + "A aparência da criatura muda ligeiramente, transparecendo inocência, fofura e delicadeza.")
            .effectChainDescription("Boneca de Porcelana: Adicionalmente aos efeitos anteriores, o alvo desta magia "
                    + "perde sua RD e RM, e efeitos de Cura que ele receberia são reduzidos à metade.")
            .criticalEffectType(CriticalEffectType.DILACERAR)
            .duration(SpellDuration.rodadas(1))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MUITO_CURTA))
            .build()),

    /** TODO its Corrente grants a Movimento Base de Voo equal to the target's ground speed; vertical/flight movement is a different sub-stat from {@code ModifierType#MOVEMENT}. */
    DRACONECER(SpellData.builder()
            .name("Dracônecer")
            .branchLevel(BranchLevel.FLORESCENTE)
            .branch(MagicBranch.POLIMORFISMO_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Um personagem tocado por esta magia se torna fisicamente tão poderoso e resistente quanto "
                    + "um Dragão.")
            .primaryEffectDescription("O alvo recebe Bônus +3 em Força e Destreza, sua Categoria de Tamanho e "
                    + "multiplicador de PV aumentados em +2.")
            .effectChainDescription("Draconato: Adicionalmente aos efeitos anteriores, o alvo desta magia recebe "
                    + "asas e capacidade de voar com Movimento Base de Voo igual à sua velocidade em terra. O corpo "
                    + "dele é coberto por escamas de Dragão, que lhe fornecem RD e RM.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build());

    private final SpellData data;

    PolimorfismoSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.POLIMORFISMO;
    }
}
