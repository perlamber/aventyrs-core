package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellAlternateEffect;
import org.aventyrs.core.magic.SpellBodyChange;
import org.aventyrs.core.magic.SpellChainKind;
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
 * <h2>Growing and shrinking are real; the Atributo half reaches the roll path only</h2>
 *
 * The six Magias that change a body's size author a {@link SpellBodyChange} (core 0.0.99), applied
 * by {@code effect.BodyChangeEffect} as round-scoped bonuses lasting the Magia's Duração: the
 * Categoria de Tamanho shift is read by {@code CharacterSizeService}'s sheet overload (a token is
 * drawn at it), the Multiplicador de PV by {@code HitPointsService}'s, and Força/Destreza as
 * {@code STRENGTH_BONUS}/{@code DEXTERITY_BONUS} — which reach a Perícia roll governed by that
 * Atributo and nothing else (CLAUDE.md, "Permanent Attribute bonuses" row). Gigantecer and
 * Espremer are buildable Correntes ({@code SpellChainKind}).
 *
 * <p>"Este efeito não reduz Atributos à um total de zero ou menos" is {@link
 * SpellBodyChange#MINIMUM_ATTRIBUTE}, applied when the effect lands.
 *
 * <p>Still prose: Rearranjo Corporal, Murcha-Corpo and Infla-Músculos (none changes size — the
 * last two would be one {@code bodyChange} line each), Titânecer's Armada Ôgrica (a pick per
 * target), Dracônecer's Draconato and Enfadecer's Boneca de Porcelana.
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
            // The Reação-to-PA direction: this parent is one of the catalog's five Reação Magias,
            // and its second version trades that back for a Ponto de Ação. Which is why an
            // ActivationTime override has to be fully replaceable rather than a one-way widening.
            .alternateEffect(SpellAlternateEffect.builder()
                    .name("Rearranjo Estendido")
                    .activationTime(ActivationTime.pa(1))
                    .duration(SpellDuration.rodadas(1))
                    .build())
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
            .bodyChange(SpellBodyChange.builder().attributeChange(-2).build())
            // TODO the extra PM that buys +2 Rodadas each: SpellCastRequest carries no extra-PM figure.
            // TODO the Corrente Alternativa (Fraqueza Momentânea): a Desvantagem on the next Turn's Força/Destreza
            // rolls has no timed carrier scoped to a governing Atributo, and "amaldiçoado" names no Malefício.
            .effectChainKind(SpellChainKind.ESPREMER)
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
            .bodyChange(SpellBodyChange.builder().attributeChange(2).attributeChoice(true).build())
            .effectChainKind(SpellChainKind.GIGANTECER)
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
            .bodyChange(SpellBodyChange.builder().sizeCategoryShift(-2).attributeChange(-3).build())
            .secondaryEffectDescription("Aura do Encolhimento: O Alcance desta magia é alterado para Pessoal, a GD "
                    + "para Difícil e a Duração aumentada para +2 Minutos. Você e até dois aliados adjacentes tem a "
                    + "Categoria de Tamanho reduzida em -2.")
            // Narrows a dual Pessoal/Toque reach to Pessoal alone, so alternateTargeting drops. The caster is the
            // target; "até dois aliados adjacentes" are its additional targets, picked by the caller (adjacency is
            // geometry). Size only — the version states no Atributo change.
            .alternateEffect(SpellAlternateEffect.builder()
                    .name("Aura do Encolhimento")
                    .targeting(SpellTargeting.PESSOAL)
                    .castingDifficultyLevel(DifficultyLevel.HARD)
                    .duration(SpellDuration.minutos(2))
                    .maxAdditionalTargets(2)
                    .bodyChange(SpellBodyChange.builder().sizeCategoryShift(-2).build())
                    .build())
            .criticalEffectType(CriticalEffectType.DILACERAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * +2 Força e Destreza, +2 Multiplicador de PV and +2 Categoria de Tamanho for 3 Rodadas. TODO Armada Ôgrica (its
     * Efeito Alternativo): Ogrificar's benefits on the caster and two adjacent allies, each with their own "Força ou
     * Destreza" pick — a cast carries one pick, not one per target.
     */
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
            .bodyChange(SpellBodyChange.builder().attributeChange(2).lifeMultiplierIncrease(2).sizeCategoryShift(2)
                    .build())
            .secondaryEffectDescription("Armada Ôgrica: Você e mais dois aliados adjacentes, recebem os benefícios "
                    + "de Ogrificar. Os bônus concedidos podem ser escolhidos individualmente, este efeito não ativa "
                    + "a Corrente de Efeitos – Gigantecer.")
            .alternateEffect(SpellAlternateEffect.named("Armada Ôgrica"))
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .alternateTargeting(SpellTargeting.TOQUE)
            .build()),

    /**
     * "tem sua Força e Destrezas reduzidas à 1" sets an Atributo to a value rather than modifying it, resolved as the
     * malus that lands it there when the effect is applied ({@code SpellBodyChange#attributesSetTo}). ⚠️ So the
     * target's total is read once: a bonus it gains during the Rodada is not cancelled. TODO Boneca de Porcelana — no
     * timed "loses its RD and RM" carrier, and halved healing has no timed form either.
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
            .bodyChange(SpellBodyChange.builder().attributesSetTo(1).sizeCategoryShift(-3).build())
            .effectChainDescription("Boneca de Porcelana: Adicionalmente aos efeitos anteriores, o alvo desta magia "
                    + "perde sua RD e RM, e efeitos de Cura que ele receberia são reduzidos à metade.")
            .criticalEffectType(CriticalEffectType.DILACERAR)
            .duration(SpellDuration.rodadas(1))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MUITO_CURTA))
            .build()),

    /**
     * +3 Força e Destreza, +2 Categoria de Tamanho and +2 Multiplicador de PV for 3 Rodadas. TODO its Corrente
     * (Draconato) grants a Movimento Base de Voo equal to the target's ground speed — the figure MovementService
     * already falls back to — but a Magia's timed effect cannot grant a {@code MovementMode}; its "RD e RM" names no
     * figure.
     */
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
            .bodyChange(SpellBodyChange.builder().attributeChange(3).lifeMultiplierIncrease(2).sizeCategoryShift(2)
                    .build())
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
