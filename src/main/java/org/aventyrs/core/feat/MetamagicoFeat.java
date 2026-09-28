package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.character.services.CharacterSkillServiceImpl;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.magic.ActivationType;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.FreeSpellPick;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellFamiliarity;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The Talentos Metamágicos — {@code FeatCategory#METAMAGICO}, the tree that governs how deep
 * into an Árvore de Magia a Conjurador can reach.
 *
 * <h2>The cap ladder</h2>
 *
 * Four of these Talentos form a prerequisite chain, each raising {@code
 * SpellService#getMaxBranchLevel} by exactly one rung from {@code BranchLevel#SEMENTE}:
 *
 * <pre>
 * ARCANISTA               → BROTO
 * ARCANISTA_EXPERIENTE    → MUDA
 * MESTRE_ARCANISTA        → EMERGENTE
 * DESAFIADOR_DA_REALIDADE → FLORESCENTE
 * </pre>
 *
 * Summing one rung apiece is only correct <em>because</em> they chain: each names the previous
 * as its {@code requiredFeat}, so they can't be acquired out of order or in isolation. The
 * ladder is complete — holding all four reaches exactly {@code BranchLevel#FLORESCENTE}. If a
 * fifth rung is ever added, it grants +1 like the rest; never compensate for a missing rung by
 * granting +2 somewhere.
 *
 * <h2>The "ao custo de N exp" clauses</h2>
 *
 * Each ladder rung also lets its holder learn extra Magias of the tier it unlocks, "de outras
 * Árvores que você conheça", at a stated exp cost — 1 (Broto), 2 (Muda), 3 (Emergente), 5
 * (Florescente). Those four figures <em>are</em> {@code SpellService.ACQUISITION_EXPERIENCE_COST}
 * now, and {@code SpellService#grantSpell} spends them for real, so the cost half of these
 * clauses is live: with the cap raised (by the same rung), a further Broto/Muda/… from an Árvore
 * the Conjurador already conhece goes through the ordinary climb/branch gates at exactly that
 * price.
 *
 * <h2>The free picks, and the Árvores conhecidas</h2>
 *
 * The "Escolha 2 Árvores [...], você aprende a conjurar as magias do tipo X destas árvores" half
 * of each rung is {@code Feat#resolveFreeSpellPicks} — two picks of the rung it unlocks — and
 * {@link #ARCANISTA}'s "Escolha uma quantidade de árvores de magia igual ao seu Conhecimento
 * Metamágico" is {@code Feat#resolveKnownTreeCapacity}. <b>Neither records a choice.</b> The
 * trees a Conjurador conhece and the ramificação they took are already derived from {@code
 * Character#getSpells()}, so "the 2 trees I chose" is simply the first two Árvores holding a Magia
 * of that rung, and {@code SpellService#getAcquisitionCost} waives exactly those. A separate
 * choice record could only disagree with the spell list — the same reason the branch gate keeps
 * no "chosen branch" field. The caller picks through {@code SpellService#learnTree}/{@code
 * #getFreeSpellOptions}/{@code #grantSpell}, and picking one of a diverging Árvore's two Magias
 * <em>is</em> choosing its ramificação.
 *
 * <h2>What "Conhecimento: Metamágico" costs this catalog</h2>
 *
 * Most of these Talentos require training or Graduações in <i>Conhecimento: Metamágico</i>, which
 * in this core is {@code ConhecimentosSpecialization#METAMAGICO} — a <b>Especialização</b> of
 * {@link SkillType#CONHECIMENTOS}, not a {@code SkillType} of its own. Every such prerequisite is
 * modeled as the Graduação floor on Conhecimentos alone and <b>under-constrains</b>: a character
 * trained in Conhecimentos without the Metamágico Especialização passes. {@code
 * FeatRequirements#requiredSkillTraits} can express the Especialização now ({@code
 * SobrevivenciaFeat} uses it); adding it here is a separate decision, since it would tighten
 * every character already holding these Talentos.
 *
 * <p>{@link #ARCANISTA} loses more than that — its Pré-requisito names <b>two</b> Perícias
 * (Conhecimento: Metamágico <i>and</i> Domínio do Mana) and {@code FeatRequirements} holds one
 * {@code requiredSkillType}, so only the Conhecimentos half is enforced.
 */
public enum MetamagicoFeat implements Feat {
    // Real: the first rung of the cap ladder (Semente + Broto); the DM bonus, which is
    // unconditional ("permanentemente") and pure arithmetic off Domínio do Mana's Graduação; the
    // Árvores conhecidas — as many as Conhecimento Metamágico, resolved live so a raised Graduação
    // opens another (SpellService#getOpenTreeSlots), each opened by SpellService#learnTree with
    // its Semente — and the 2 free Brotos, one per Árvore (see the class javadoc).
    // Ruling (table, 2026-09-25): "Conhecimento Metamágico" is the Conhecimentos roll value,
    // Graduação + Gnose, not the Graduação alone.
    // Real: "RM para resistir aos efeitos de Magias que você conheça" — one RM instance (table
    // ruling, 2026-09-27) against a hit DamageInteraction#fromSpell marks as a Magia the holder has
    // learned or may mimetize (SpellFamiliarity), through Feat#resolveSpellMagicReduction.
    ARCANISTA(
            "Você consegue conjurar magias do tipo Semente e Broto. Escolha uma quantidade de "
                    + "árvores de magia igual ao seu Conhecimento Metamágico, você conhece estas árvores de "
                    + "magias e sabe utilizar todas as magias do tipo Semente das árvores de magias "
                    + "escolhidas. Ao adquirir novas graduações novas Árvores de Magia também podem ser "
                    + "escolhidas, e suas magias do tipo Semente são automaticamente aprendidas. Em seguida "
                    + "Escolha 2 das Árvores de Magia que você conheça, você aprende a conjurar as Magias "
                    + "Brotos destas árvores. Você pode aprender novas Magias Brotos ao longo da história, "
                    + "de outras Árvores que você conheça, ao custo de 1 exp. Você recebe permanentemente "
                    + "Bônus em sua DM igual a metade de suas Graduações em Domínio do Mana, você recebe RM "
                    + "para resistir aos efeitos de Magias que você conheça.",
            // Under-constrained: the Domínio do Mana half of "Treinamento em 'Conhecimento:
            // Metamágico' e 'Domínio do Mana'" has no second slot to live in — see class javadoc.
            () -> FeatRequirements.builder()
                    .requiredSkillType(SkillType.CONHECIMENTOS)
                    .requiredSkillGraduation(MetamagicoFeat.TRAINED)
                    .build()) {

        @Override
        public int resolveBranchLevelIncrease(final Character character) {
            return ONE_RUNG;
        }

        /** "Escolha uma quantidade de árvores de magia igual ao seu Conhecimento Metamágico". */
        @Override
        public int resolveKnownTreeCapacity(final Character character) {
            return conhecimentoMetamagico(character);
        }

        @Override
        public List<FreeSpellPick> resolveFreeSpellPicks(final Character character) {
            return List.of(new FreeSpellPick(BranchLevel.BROTO, FREE_PICKS_PER_RUNG));
        }

        /** "RM para resistir aos efeitos de Magias que você conheça" — one instance. */
        @Override
        public int resolveSpellMagicReduction(final Spell spell, final CombatantSheet holder) {
            return SpellFamiliarity.canCast(holder.getCharacter(), spell) ? DamageService.DEFAULT_DAMAGE_REDUCTION : 0;
        }

        /** "Bônus em sua DM igual a metade de suas Graduações em Domínio do Mana", rounded down. */
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return defenseType == DefenseType.MAGIC
                    // An effect, so it reads the Perícia a Título may stand in for it (Domínio da Cura).
                    ? character.getEffectiveGraduation(SkillType.DOMINIO_DO_MANA) / 2
                    : 0;
        }
    },

    // Real: the second rung of the cap ladder (Muda).
    //
    // Barreira Mágica is real — BarreiraMagicaActiveAbility, triggered through
    // ActiveAbilityService#activate: 1PA + 3PM for a DEFESAS TemporaryBonus over 2 Rodadas, with
    // Resfriamento 1 (the catalog's first stated one, and what ActiveAbility#getCooldownRounds
    // was added for). The two rungs above *replace* the +2 rather than adding to it, so the
    // figure is resolved from the holder's held Talentos at activation and only this constant
    // grants the ability — see that class. "Podem ter a Duração estendida por quaisquer efeitos que
    // aumente a Duração de Magias" is real too: SpellDurationService#resolveNonSpellDurationIncrease,
    // which only an extension scoped to no kind of Magia answers (a Poderosa weapon).
    // Note: "Magias aprendidas desta forma são sempre do mesmo ramo da magia de nível anterior" is
    // already true by construction, and stricter: Spell#isEligible's branch gate refuses the
    // opposite ramificação outright. Nothing to build; noted so the clause isn't re-derived.
    ARCANISTA_EXPERIENTE(
            "Escolha 2 Árvores de Magia que você conheça, nas quais você seja capaz de conjurar "
                    + "magias do tipo Broto, você aprende a conjurar as magias do tipo Muda destas árvores. "
                    + "Magias aprendidas desta forma são sempre do mesmo ramo da magia de nível anterior. "
                    + "Você pode aprender novas Mudas ao longo da história, de outras árvores que você "
                    + "conheça magias do tipo Broto, ao custo de 2 exp. Você também pode gastar PM para "
                    + "criar Barreiras Mágicas que o protege de ataques, esta ação consome 1PA e 3PM, você "
                    + "recebe Bônus de +2 em suas Defesas. Barreiras criadas desta forma tem Duração de 2 "
                    + "Rodadas, podem ter a Duração estendida por quaisquer efeitos que aumente a Duração "
                    + "de Magias, e Resfriamento 1.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.ARCANISTA)
                    .requiredSkillType(SkillType.CONHECIMENTOS)
                    .requiredSkillGraduation(3)
                    .build()) {
        private final ActiveAbility barreiraMagica = new BarreiraMagicaActiveAbility();

        @Override
        public int resolveBranchLevelIncrease(final Character character) {
            return ONE_RUNG;
        }

        @Override
        public List<FreeSpellPick> resolveFreeSpellPicks(final Character character) {
            return List.of(new FreeSpellPick(BranchLevel.MUDA, FREE_PICKS_PER_RUNG));
        }

        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(barreiraMagica);
        }
    },

    // Real: the third rung of the cap ladder (Emergente).
    //
    // The Barreira Mágica upgrade is real, both halves: holding this rung raises the Barreira the
    // holder creates from +2 to +3 Defesas (a replacement, not a sum — see
    // BarreiraMagicaActiveAbility, which reads the rungs rather than each rung granting its own),
    // and "+1 às Defesas de seus aliados adjacentes" is scanned by DefenseService off each
    // recipient's own SceneContext while the Barreira runs (the best adjacent Barreira counts).
    MESTRE_ARCANISTA(
            "Escolha 2 Árvores de Magia que você conheça, nas quais você seja capaz de conjurar "
                    + "magias do tipo Muda, você aprende a conjurar as magias do tipo Emergentes destas "
                    + "árvores. Magias aprendidas desta forma são sempre do mesmo ramo da magia de nível "
                    + "anterior. Você pode aprender novas magias Emergentes ao longo da história, de outras "
                    + "árvores que você conheça magias do tipo Broto, ao custo de 3 exp. Barreiras Mágicas "
                    + "criadas por você agora concedem Bônus de +3 em suas Defesas e de +1 às Defesas de "
                    + "seus aliados adjacentes.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.ARCANISTA_EXPERIENTE)
                    .requiredSkillType(SkillType.CONHECIMENTOS)
                    .requiredSkillGraduation(6)
                    .build()) {

        @Override
        public int resolveBranchLevelIncrease(final Character character) {
            return ONE_RUNG;
        }

        @Override
        public List<FreeSpellPick> resolveFreeSpellPicks(final Character character) {
            return List.of(new FreeSpellPick(BranchLevel.EMERGENTE, FREE_PICKS_PER_RUNG));
        }
    },

    // Real: the top rung of the cap ladder (Florescente).
    //
    // Same as its predecessor, both halves real: the Barreira becomes +5 Defesas, and +3 to each
    // adjacent ally's Defesas while it runs.
    DESAFIADOR_DA_REALIDADE(
            "Escolha 2 Árvores de Magia que você conheça, nas quais você seja capaz de conjurar "
                    + "magias do tipo Emergente, você aprende a conjurar as magias do tipo Florescente destas "
                    + "árvores. Magias aprendidas desta forma são sempre do mesmo ramo da magia de nível "
                    + "anterior. Você pode aprender novas magias Florescentes ao longo da história, de outras "
                    + "árvores que você conheça magias do tipo Broto, ao custo de 5 exp. Suas Barreiras "
                    + "Mágicas lhe concedem Bônus de +5 em suas Defesas, e de +3 às Defesas de seus aliados "
                    + "adjacentes.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.MESTRE_ARCANISTA)
                    .requiredSkillType(SkillType.CONHECIMENTOS)
                    .requiredSkillGraduation(9)
                    .build()) {

        @Override
        public int resolveBranchLevelIncrease(final Character character) {
            return ONE_RUNG;
        }

        @Override
        public List<FreeSpellPick> resolveFreeSpellPicks(final Character character) {
            return List.of(new FreeSpellPick(BranchLevel.FLORESCENTE, FREE_PICKS_PER_RUNG));
        }
    },

    // Real, both halves: BarreiraMagicaActiveAbility#getActionPointCost(Character) makes the Barreira
    // an Ação Livre for its holder, and while one runs (BarreiraMagicaActiveAbility#isActiveOn) the GD
    // to resist a Magia the holder can cast (SpellFamiliarity) drops 1 nível — applied by
    // AttackReceiver, reported unapplied by AttackDelivery (a flat Defesa has no níveis).
    ARTESAO_DE_BARREIRAS(
            "Você pode conjurar Barreiras Mágicas como Ação Livre. Enquanto estiver com uma "
                    + "Barreira Mágica ativa a GD para resistir às magias que você também seja capaz de "
                    + "conjurar é reduzida em 1 nível.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.ARCANISTA_EXPERIENTE)
                    .build()) {
        @Override
        public int resolveSpellResistanceDifficultyReduction(final Spell spell, final CombatantSheet holder) {
            return BarreiraMagicaActiveAbility.isActiveOn(holder)
                    && SpellFamiliarity.canCast(holder.getCharacter(), spell) ? 1 : 0;
        }
    },

    // Real when the caster opts in on SpellCastRequest#activatedFeats: -1PA off the cast
    // (floored at 1PA, PA casts only), and Desvantagem on its Conjuração roll and its damage
    // (Feat#resolveCastingRollBonus), and on its healing ("e Cura mágica" —
    // Feat#resolveSpellHealingBonus, carried to SpellHealingEffect as SpellEffectContext#healingBonus).
    CONJURACAO_RAPIDA(
            "Você pode optar por receber Desvantagem nas rolagens de Perícia de Conjuração, Dano "
                    + "e Cura mágica de uma magia. Se o fizer o tempo de conjuração da desta magia será "
                    + "reduzido em -1PA.",
            () -> FeatRequirements.builder()
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(4)
                    .build()) {
        @Override
        public int resolveCastingActionPointReduction(final Spell spell, final Character character,
                                                      final int currentRound,
                                                      final List<CombatantAction> actionsThisRound,
                                                      final java.util.Set<Feat> activatedFeats) {
            return activatedFeats.contains(this) ? 1 : 0;
        }

        @Override
        public int resolveCastingRollBonus(final Spell spell, final Character character,
                                           final java.util.Set<Feat> activatedFeats) {
            return activatedFeats.contains(this) ? Skill.DISADVANTAGE_MALUS : 0;
        }

        @Override
        public int resolveSpellDamageBonus(final Spell spell, final Character character,
                                           final java.util.Set<Feat> activatedFeats) {
            return activatedFeats.contains(this) ? Skill.DISADVANTAGE_MALUS : 0;
        }

        @Override
        public int resolveSpellHealingBonus(final Spell spell, final Character character,
                                            final java.util.Set<Feat> activatedFeats) {
            return activatedFeats.contains(this) ? Skill.DISADVANTAGE_MALUS : 0;
        }
    },

    // Real, through magic.SpellStorageService: one Semente or Broto held on the sheet
    // (CombatantSheet#getStoredSpells), released as an Ação Livre, dissipated at the next Descanso.
    ARMAZENAR_MAGIA(
            "Você pode conjurar e \"guardar\" uma Magia do Tipo Semente ou Broto para soltá-la "
                    + "como uma Ação Livre posteriormente. Apenas uma magia pode ser armazenada desta forma "
                    + "por vez e ela será automaticamente dissipada se não for utilizada até seu próximo "
                    + "Descanso.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.CONJURACAO_RAPIDA)
                    .build()),

    // Real, through magic.SpellStorageService: two Sementes/Brotos or one Muda, released as an Ação
    // Livre or a Reação (the Reação spent on the sheet's ledger).
    ARMAZENAR_MAGIA_SUPERIOR(
            "Você pode conjurar e \"guardar\" uma Magia do Tipo Semente ou Broto adicional (para um "
                    + "total de 2 Magias armazenadas) ou armazenar uma única magia do tipo Muda. Você pode "
                    + "descarregar suas magias armazenadas como Ação Livre ou Reação.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.ARMAZENAR_MAGIA)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(7)
                    .build()),

    // Real when the caster opts in on SpellCastRequest#activatedFeats, for a PA-activated Magia
    // only ("não afeta magias conjuradas como Ação Livre ou Reação"): -1PA (floored at 1PA), and
    // SpellCastingResult#getEffectDelayRounds reports 1. ⚠️ The delay is reported, not scheduled:
    // the caller holds the effect back a Rodada.
    // Note: the targeting half — a long-range or area Magia having its direction/target point
    // fixed at cast time, and landing there whether or not a valid target remains — is the first
    // real consumer for the per-cast aim that AreaOfEffect deliberately does not carry (see its
    // javadoc, and the "Área de Efeito" gap-catalog row). It wants exactly the
    // AreaFootprint.covering(AreaOfEffect, origin, towards) shape that row describes, plus
    // somewhere to store the resolved aim between the cast and the effect landing.
    PROCRASTINAR_CONJURACAO(
            "Você pode fazer com que suas magias iniciem seu efeito 1 Rodada após a conjuração, "
                    + "magias conjuradas desta forma tem o Tempo de Conjuração reduzido em -1PA (mínimo "
                    + "1PA). Este Talento não afeta magias conjuradas como Ação Livre ou Reação. Magias de "
                    + "longo alcance ou de área deve ter sua direção ou local alvo escolhido no momento da "
                    + "conjuração inicial, sendo direcionadas ao ponto especificado mesmo se não tiver um "
                    + "alvo válido.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.CONJURACAO_RAPIDA)
                    .build()) {
        @Override
        public int resolveCastingActionPointReduction(final Spell spell, final Character character,
                                                      final int currentRound,
                                                      final List<CombatantAction> actionsThisRound,
                                                      final java.util.Set<Feat> activatedFeats) {
            return activatedFeats.contains(this) ? 1 : 0;
        }

        @Override
        public int resolveCastEffectDelayRounds(final Spell spell, final Character character,
                                                final java.util.Set<Feat> activatedFeats) {
            boolean paCast = spell.getActivationTime() != null
                    && spell.getActivationTime().type() == ActivationType.PONTOS_DE_ACAO;
            return activatedFeats.contains(this) && paCast ? 1 : 0;
        }
    },

    // The mimicry is real, through ArvoresMimetizadasFeat: two chosen Árvores, their Semente
    // (free) and their Broto at 2PD.
    // Real: "+3 em DM para resistir aos efeitos de magias que seja capaz de conjurar" —
    // Feat#resolveSpellDefenseBonus against an incoming Magia the holder can cast (SpellFamiliarity),
    // replaced by Assombrosa's +5 rather than summed with it. Deliberately not an unconditional DM
    // bonus; contrast ARCANISTA's own DM clause, which really is unconditional.
    APTIDAO_MAGICA_AMPLA(
            "Escolha duas Árvores de Magia, você pode mimetizar as magias Sementes e Broto destas "
                    + "árvores. Magias Broto conjuradas com este talento utilizam 2PD, ao invés de 1PM. Você "
                    + "recebe Bônus de +3 em DM para resistir aos efeitos de magias que seja capaz de "
                    + "conjurar.",
            () -> FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(4)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(MetamagicoFeat.TRAINED)
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(new FeatChoice<>(MagicTree.class, 2, Arrays.asList(MagicTree.values())));
        }

        @Override
        public int resolveSpellDefenseBonus(final DefenseType defenseType, final Spell spell,
                                            final CombatantSheet holder) {
            boolean replaced = holder.getCharacter().getFeats().stream()
                    .anyMatch(feat -> feat.catalogEntry() == APTIDAO_MAGICA_ASSOMBROSA);
            return !replaced && resistsCastable(defenseType, spell, holder) ? AMPLA_SPELL_DEFENSE_BONUS : 0;
        }
    },

    // The mimicry is real, through MagiasMimetizadasEscolhidasFeat: the chosen Mudas of Ampla's
    // Árvores, at 3PD each. Real: the +5 DM against a Magia the holder can cast, replacing Ampla's +3.
    APTIDAO_MAGICA_ASSOMBROSA(
            "Escolha uma magia Muda de cada Árvore de Magia conhecida através do talento 'Aptidão "
                    + "Mágica Ampla', você é capaz de mimetizar as magias escolhidas ao custo de 3PD cada. "
                    + "Seu Bônus em DM para resistir às magias que você é capaz de conjurar aumenta para +5.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.APTIDAO_MAGICA_AMPLA)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(4)
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            List<Spell> options = MagiasMimetizadasEscolhidasFeat.optionsFor(holder, BranchLevel.MUDA);
            int picks = ArvoresMimetizadasFeat.chosenBy(holder, APTIDAO_MAGICA_AMPLA).map(java.util.Set::size).orElse(0);
            return options.isEmpty() ? List.of() : List.of(new FeatChoice<>(Spell.class, Math.min(picks, options.size()), options));
        }

        @Override
        public int resolveSpellDefenseBonus(final DefenseType defenseType, final Spell spell,
                                            final CombatantSheet holder) {
            return resistsCastable(defenseType, spell, holder) ? ASSOMBROSA_SPELL_DEFENSE_BONUS : 0;
        }
    },

    // The mimicry is real, through MagiasMimetizadasEscolhidasFeat: the chosen Emergentes of
    // Ampla's Árvores, at 5PD. ⚠️ "Que você conheça uma Muda de seu ramo" is not validated on the pick.
    // Real: the GD to resist a Magia the holder can cast drops 1 nível, the way ARTESAO_DE_BARREIRAS'
    // does, but with no Barreira needed.
    APTIDAO_MAGICA_SUPREMA(
            "Escolha uma magia Emergente de cada Árvore de Magia conhecida através do talento "
                    + "'Aptidão Mágica Ampla', e que você seja conheça uma Muda de seu ramo, você é capaz de "
                    + "mimetizar as magias escolhidas ao custo de 5PD. A GD para resistir às magias que você "
                    + "também é capaz de conjurar é reduzida em 1 nível.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.APTIDAO_MAGICA_ASSOMBROSA)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(7)
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            List<Spell> options = MagiasMimetizadasEscolhidasFeat.optionsFor(holder, BranchLevel.EMERGENTE);
            int picks = ArvoresMimetizadasFeat.chosenBy(holder, APTIDAO_MAGICA_AMPLA).map(java.util.Set::size).orElse(0);
            return options.isEmpty() ? List.of() : List.of(new FeatChoice<>(Spell.class, Math.min(picks, options.size()), options));
        }

        @Override
        public int resolveSpellResistanceDifficultyReduction(final Spell spell, final CombatantSheet holder) {
            return SpellFamiliarity.canCast(holder.getCharacter(), spell) ? 1 : 0;
        }
    },

    // Real, both halves: the Mana Multiplier increase is summed by MagicPointsService
    // #getManaMultiplier, and the Descanso recovery by RestService#getRecoveredMagicPoints. The
    // Título count reads Character#getAllTitles().size() — a Título Aventyr is "Desperto" simply
    // by being held, the same confirmed reading ArtesMarciaisFeat#ARTISTA_MARCIAL relies on.
    MENTE_EXPANDIDA(
            "Você consegue conjurar mais magias do que o normal, seu multiplicador de PM é "
                    + "aumentado em 1. Sua recuperação de PM também aumenta, a cada Descanso você recupera "
                    + "+2PM, e então +1PM para cada Título Aventyr que tenha desperto.",
            () -> FeatRequirements.builder()
                    .requiredSkillType(SkillType.CONHECIMENTOS)
                    .requiredSkillGraduation(MetamagicoFeat.TRAINED)
                    .build()) {

        @Override
        public int resolveManaMultiplierIncrease(final Character character) {
            return MANA_MULTIPLIER_INCREASE;
        }

        /** "a cada Descanso" — every Descanso, whatever its type, unlike FocusAbility's LONGO gate. */
        @Override
        public int resolveRestMagicPointsBonus(final RestType restType, final Character character) {
            return BASE_REST_MANA_RECOVERY + character.getAllTitles().size();
        }
    },

    // Real, both halves: -1PM off SpellCastingResult#getManaCost (Feat#resolveManaCostReduction,
    // floored at 1), and off the Barreira Mágica — the one Talento Metamágico effect priced in PM —
    // through BarreiraMagicaActiveAbility#getMagicPointCost(Character).
    ENGENHEIRO_DO_MANA(
            "O Custo de Mana para Conjurar Magias e ativar efeitos de Talentos Metamágicos é "
                    + "reduzido em -1PM (mínimo 1PM).",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.MENTE_EXPANDIDA)
                    .build()) {
        @Override
        public int resolveManaCostReduction(final Spell spell, final CombatantSheet caster,
                                            final CombatantSheet target, final SceneContext casterContext) {
            return 1;
        }
    },

    // The mimicry is real, through MagiasMimetizadasEscolhidasFeat: one chosen Florescente of Ampla's
    // Árvores, at 5PD.
    // Real: the immunity (Feat#isImmuneToSpell) to a Magia the holder can cast while their current
    // PD is at least 10 — table ruling (2026-09-27): "reserva de Bônus Bases" is the current PD, and
    // immune means defended outright, with no damage and no effects.
    APTIDAO_MAGICA_DRACONICA(
            "Escolha uma magia Florescente de uma das Árvores de Magias conhecidas através do "
                    + "talento ‘Aptidão Mágica Ampla’, você é capaz de mimetizar a magia escolhida ao custo "
                    + "de 5PD. Enquanto tiver ao menos 10PD em sua reserva de Bônus Bases você é imune a "
                    + "Magias que você é capaz de conjurar.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.APTIDAO_MAGICA_SUPREMA)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(9)
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            List<Spell> options = MagiasMimetizadasEscolhidasFeat.optionsFor(holder, BranchLevel.FLORESCENTE);
            int picks = 1;
            return options.isEmpty() ? List.of() : List.of(new FeatChoice<>(Spell.class, Math.min(picks, options.size()), options));
        }

        @Override
        public boolean isImmuneToSpell(final Spell spell, final CombatantSheet holder) {
            return SpellFamiliarity.canCast(holder.getCharacter(), spell)
                    && DETERMINATION_POINTS_SERVICE.getCurrentDeterminationPoints(holder.getCharacter(), holder)
                            >= DRACONICA_DETERMINATION_RESERVE;
        }
    };

    /** Aptidão Mágica Ampla's "+3 em DM para resistir aos efeitos de magias que seja capaz de conjurar". */
    private static final int AMPLA_SPELL_DEFENSE_BONUS = 3;

    /** Aptidão Mágica Assombrosa's "aumenta para +5". */
    private static final int ASSOMBROSA_SPELL_DEFENSE_BONUS = 5;

    /** Aptidão Mágica Dracônica's "ao menos 10PD em sua reserva". */
    private static final int DRACONICA_DETERMINATION_RESERVE = 10;

    private static final DeterminationPointsService DETERMINATION_POINTS_SERVICE =
            new DeterminationPointsServiceImpl();

    /** Whether a DM roll against spell is one the holder resists as a Magia they can cast. */
    private static boolean resistsCastable(final DefenseType defenseType, final Spell spell,
                                           final CombatantSheet holder) {
        return defenseType == DefenseType.MAGIC && SpellFamiliarity.canCast(holder.getCharacter(), spell);
    }

    /** One rung of {@link BranchLevel}'s ladder — what each cap-raising Talento grants. */
    private static final int ONE_RUNG = 1;

    /** "Escolha 2 Árvores de Magia" — the free picks every cap-ladder rung hands out. */
    private static final int FREE_PICKS_PER_RUNG = 2;

    private static final CharacterSkillService CHARACTER_SKILL_SERVICE = new CharacterSkillServiceImpl();

    /** "Treinamento em" a Perícia — the lowest Graduação that counts as trained. */
    private static final int TRAINED = 1;

    /** MENTE_EXPANDIDA's "seu multiplicador de PM é aumentado em 1". */
    private static final int MANA_MULTIPLIER_INCREASE = 1;

    /** MENTE_EXPANDIDA's flat "+2PM" before any Título Aventyr Desperto is counted. */
    private static final int BASE_REST_MANA_RECOVERY = 2;

    private final String description;

    /**
     * Held as a {@link Supplier} rather than a plain field because five of these Talentos name a
     * <em>sibling</em> constant as their {@code requiredFeat}, and Java forbids referencing an
     * enum constant from another constant's constructor arguments. Deferring construction to the
     * first {@link #getFeatRequirements()} call sidesteps that with no forward-reference dance.
     */
    private final Supplier<FeatRequirements> featRequirements;

    MetamagicoFeat(final String description, final Supplier<FeatRequirements> featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.METAMAGICO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements.get();
    }

    /**
     * Conhecimento Metamágico as ARCANISTA counts it: the Conhecimentos roll value — Graduação
     * plus the Gnose total — or zero when the Perícia is untrained. The Especialização carries no
     * Graduação of its own, so the Perícia's is the only one there is.
     */
    static int conhecimentoMetamagico(final Character character) {
        CharacterSkill conhecimentos = character.getSkills().get(SkillType.CONHECIMENTOS);
        if (conhecimentos == null || conhecimentos.getGraduation().getGraduationValue() < TRAINED) {
            return 0;
        }
        return CHARACTER_SKILL_SERVICE.getValueForRoll(conhecimentos, character.getAttributes(), character.getRace());
    }

    private static int graduationOf(final Character character, final SkillType skillType) {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        return characterSkill == null ? 0 : characterSkill.getGraduation().getGraduationValue();
    }
}
