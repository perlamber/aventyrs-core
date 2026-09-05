package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.skill.SkillType;

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
 * price. What the ladder does <em>not</em> yet do is hand out the initial Sementes/Brotos of the
 * chosen trees for free — that is {@link #ARCANISTA}'s own TODO (it needs an
 * acquisition-time-choice class to record the picks and override {@code
 * Feat#grantsFreeSpellAcquisition}).
 *
 * <h2>What "Conhecimento: Metamágico" costs this catalog</h2>
 *
 * Most of these Talentos require training or Graduações in <i>Conhecimento: Metamágico</i>, which
 * in this core is {@code ConhecimentosSpecialization#METAMAGICO} — a <b>Especialização</b> of
 * {@link SkillType#CONHECIMENTOS}, not a {@code SkillType} of its own. {@link FeatRequirements}
 * has no Especialização field, so every such prerequisite is modeled as the Graduação floor on
 * Conhecimentos alone and <b>under-constrains</b>: a character trained in Conhecimentos without
 * the Metamágico Especialização passes. Widening {@code FeatRequirements} with a
 * {@code SkillSpecialization} field would close this for every Talento at once.
 *
 * <p>{@link #ARCANISTA} loses more than that — its Pré-requisito names <b>two</b> Perícias
 * (Conhecimento: Metamágico <i>and</i> Domínio do Mana) and {@code FeatRequirements} holds one
 * {@code requiredSkillType}, so only the Conhecimentos half is enforced.
 */
public enum MetamagicoFeat implements Feat {
    // Real: the first rung of the cap ladder (Semente + Broto), and the DM bonus, which is
    // unconditional ("permanentemente") and pure arithmetic off Domínio do Mana's Graduação.
    //
    // TODO: "conhece N Árvores de Magia" (N = Graduações em Conhecimento Metamágico), with every
    // Semente of uma Árvore conhecida learned automatically, and 2 chosen Árvores whose Brotos it
    // can cast. The foundation is in place now: SpellService#getKnownTrees derives the "Árvores
    // conhecidas" set from the spell list, SpellService#grantSpell spends the per-rung
    // SpellService.ACQUISITION_EXPERIENCE_COST (1/2/3/5 for Broto/Muda/Emergente/Florescente),
    // and Feat#grantsFreeSpellAcquisition waives that cost for a Magia a Talento hands out. Still
    // missing: (1) this Talento is a plain enum constant with nowhere to record the N chosen
    // trees / the 2 Broto trees — it needs the acquisition-time-choice class the adding-a-feat
    // skill describes, which is also where grantsFreeSpellAcquisition gets a real override; (2)
    // no auto-learning path — grantSpell is still the only way a Magia enters the spell list and
    // every call is deliberate, so "automaticamente aprendidas" has no loop to run.
    // TODO: "RM para resistir aos efeitos de Magias que você conheça" — RM (Redução Mágica) is
    // not a concept this core computes at all (same gap Gorgona's javadoc already cites), and
    // the "Magias que você conheça" scope needs an incoming effect to be classified as a
    // specific Magia, which nothing does.
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

        /** "Bônus em sua DM igual a metade de suas Graduações em Domínio do Mana", rounded down. */
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return defenseType == DefenseType.MAGIC
                    ? graduationOf(character, SkillType.DOMINIO_DO_MANA) / 2
                    : 0;
        }
    },

    // Real: the second rung of the cap ladder (Muda).
    //
    // TODO: Barreira Mágica — 1PA + 3PM for +2 Defesas over 2 Rodadas with Resfriamento 1. Three
    // pieces missing: no activation path spends PA and PM together to apply an effect; no
    // Resfriamento/cooldown concept exists anywhere in this core; and "efeitos que aumentem a
    // Duração de Magias" has no hook. The +2 Defesas half alone would be an ordinary Blessing
    // (grantTemporaryBonus(DEFESAS, 2, 2)) once something can trigger it.
    // TODO: "Magias aprendidas desta forma são sempre do mesmo ramo da magia de nível anterior" —
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

        @Override
        public int resolveBranchLevelIncrease(final Character character) {
            return ONE_RUNG;
        }
    },

    // Real: the third rung of the cap ladder (Emergente).
    //
    // TODO: the Barreira Mágica upgrade (+3 own Defesas, +1 to adjacent allies) is blocked on the
    // same Barreira gap ARCANISTA_EXPERIENTE cites — nothing creates a Barreira for this to
    // upgrade. Note each rung *replaces* the previous rung's Barreira figures rather than adding
    // to them (+2 → +3 → +5), which is itself unexpressible until the Barreira exists.
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
    },

    // Real: the top rung of the cap ladder (Florescente).
    //
    // TODO: the Barreira Mágica upgrade (+5 own Defesas, +3 to adjacent allies) is blocked on the
    // same Barreira gap; the ally half additionally needs the outward-facing scan shape
    // AventyrTitleAbility#resolveAllyAbsoluteDamageReduction uses for Bastião dos Necessitados.
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
    },

    // TODO: both halves blocked on the same Barreira Mágica gap ARCANISTA_EXPERIENTE cites —
    // nothing creates a Barreira, so neither making it an Ação Livre nor keying a GD reduction
    // off one being active has anything to attach to. The GD reduction additionally needs the
    // "Magias que você seja capaz de conjurar" scope, which nothing classifies.
    ARTESAO_DE_BARREIRAS(
            "Você pode conjurar Barreiras Mágicas como Ação Livre. Enquanto estiver com uma "
                    + "Barreira Mágica ativa a GD para resistir às magias que você também seja capaz de "
                    + "conjurar é reduzida em 1 nível.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.ARCANISTA_EXPERIENTE)
                    .build()),

    // TODO: the casting-time reduction has a column to read now — Spell#getActivationTime()
    // (ActivationTime.actionPoints()) — but nothing resolves an *effective* cast cost from it:
    // SpellCastingService#castSpell rolls the delivery Perícia and Domínio do Mana and spends
    // nothing, so there is no PA figure to reduce or floor at 1PA — the same missing cost step
    // Guampo's Benção Divina javadoc cites. The Desvantagem half has a constant ready
    // (Skill#DISADVANTAGE_MALUS) but no way to scope it to "this one cast", nor to Dano/Cura rolls
    // specifically; and the trade is opt-in per cast, which is a per-activation choice this core
    // has no shape for.
    CONJURACAO_RAPIDA(
            "Você pode optar por receber Desvantagem nas rolagens de Perícia de Conjuração, Dano "
                    + "e Cura mágica de uma magia. Se o fizer o tempo de conjuração da desta magia será "
                    + "reduzido em -1PA.",
            () -> FeatRequirements.builder()
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(4)
                    .build()),

    // TODO: entirely unbuilt. Storing a cast Magia to release later as an Ação Livre needs a
    // "held charge" slot on CharacterSheet that nothing resembles today, plus a dissipation
    // trigger on the next Descanso — RestService applies recovery but fires no such hook.
    ARMAZENAR_MAGIA(
            "Você pode conjurar e \"guardar\" uma Magia do Tipo Semente ou Broto para soltá-la "
                    + "como uma Ação Livre posteriormente. Apenas uma magia pode ser armazenada desta forma "
                    + "por vez e ela será automaticamente dissipada se não for utilizada até seu próximo "
                    + "Descanso.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.CONJURACAO_RAPIDA)
                    .build()),

    // TODO: same missing Magia-storage mechanism as ARMAZENAR_MAGIA — a cast Magia has nowhere to
    // be held between casting and release, and Spell carries no activation-cost column for
    // "descarregar como Ação Livre ou Reação" to set.
    // Note the storage capacity is expressed in BranchLevel terms (two SEMENTE/BROTO, or one
    // MUDA), which BranchLevel#isAtLeast already compares — only the store itself is missing.
    ARMAZENAR_MAGIA_SUPERIOR(
            "Você pode conjurar e \"guardar\" uma Magia do Tipo Semente ou Broto adicional (para um "
                    + "total de 2 Magias armazenadas) ou armazenar uma única magia do tipo Muda. Você pode "
                    + "descarregar suas magias armazenadas como Ação Livre ou Reação.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.ARMAZENAR_MAGIA)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(7)
                    .build()),

    // TODO: the -1PA reduction reads Spell#getActivationTime() for its base now, but is blocked
    // on the same missing cost step CONJURACAO_RAPIDA cites — SpellCastingService#castSpell spends
    // no PA, so there is nothing to reduce or floor at 1PA (the gap Guampo's javadoc cites).
    // TODO: "iniciem seu efeito 1 Rodada após a conjuração" needs a delayed-effect mechanism.
    // TemporaryEffect counts a Round-scoped effect *down* toward expiry; nothing schedules one to
    // *begin* later, and CharacterSheet#startTurn — the natural trigger — is still a no-op.
    // TODO: "não afeta magias conjuradas como Ação Livre ou Reação" can't be expressed: nothing
    // classifies how a Magia was activated. Same shape as the missing "this one delivered attack"
    // scoping.
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
                    .build()),

    // TODO: mimetizar is its own acquisition-less casting path — casting a Magia you never
    // learned, paid for in PD instead of PM. Nothing models a Magia a character can cast but has
    // not acquired, and no cast path takes an alternative currency.
    // TODO: "+3 em DM para resistir aos efeitos de magias que seja capaz de conjurar" — scoped to
    // what is being resisted, which nothing classifies (same blocker as EVASAO's Área de Efeito
    // scoping). Deliberately NOT granted as an unconditional DM bonus, which would over-apply to
    // every Defesa roll; contrast ARCANISTA's own DM clause, which really is unconditional.
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
                    .build()),

    // TODO: same mimetizar gap as APTIDAO_MAGICA_AMPLA, and the same scoped-DM gap — the +5 here
    // replaces that Talento's +3 rather than stacking, which is itself unexpressible until the
    // scope exists.
    APTIDAO_MAGICA_ASSOMBROSA(
            "Escolha uma magia Muda de cada Árvore de Magia conhecida através do talento 'Aptidão "
                    + "Mágica Ampla', você é capaz de mimetizar as magias escolhidas ao custo de 3PD cada. "
                    + "Seu Bônus em DM para resistir às magias que você é capaz de conjurar aumenta para +5.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.APTIDAO_MAGICA_AMPLA)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(4)
                    .build()),

    // TODO: same mimetizar gap. The GD reduction is scoped the same way ARTESAO_DE_BARREIRAS'
    // is. Note this one also requires already knowing a Muda "de seu ramo" — that half IS
    // expressible against Spell#isEligible's branch gate, but only once Árvores are authored and
    // the mimetizar choice has somewhere to live.
    APTIDAO_MAGICA_SUPREMA(
            "Escolha uma magia Emergente de cada Árvore de Magia conhecida através do talento "
                    + "'Aptidão Mágica Ampla', e que você seja conheça uma Muda de seu ramo, você é capaz de "
                    + "mimetizar as magias escolhidas ao custo de 5PD. A GD para resistir às magias que você "
                    + "também é capaz de conjurar é reduzida em 1 nível.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.APTIDAO_MAGICA_ASSOMBROSA)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(7)
                    .build()),

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

    // TODO: no Mana cost is ever resolved through a service — Spell#getManaCost() (delegating to
    // BranchLevel#getManaCost) is authored data read by nobody, SpellCastingService#castSpell
    // spends no PM (the gap Guampo's javadoc cites), and no Talento Metamágico activation spends
    // PM either (see the Barreira gap). A -1PM floor-1 reduction has no calculation to sit in
    // front of.
    ENGENHEIRO_DO_MANA(
            "O Custo de Mana para Conjurar Magias e ativar efeitos de Talentos Metamágicos é "
                    + "reduzido em -1PM (mínimo 1PM).",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.MENTE_EXPANDIDA)
                    .build()),

    // TODO: same mimetizar gap as the rest of the Aptidão ladder, now at FLORESCENTE depth.
    // TODO: the immunity half is a further stage than any mitigation this core has — the gap
    // catalog records that no damage-nullifying mechanism of any kind exists — and it is
    // additionally scoped to "Magias que você é capaz de conjurar", the same unexpressible scope
    // ARTESAO_DE_BARREIRAS and APTIDAO_MAGICA_AMPLA both cite for their own DM bonuses.
    // TODO: "enquanto tiver ao menos 10PD em sua reserva de Bônus Bases" reads a Determinação
    // reserve threshold; PD is spendable but no hook conditions an effect on how much remains.
    APTIDAO_MAGICA_DRACONICA(
            "Escolha uma magia Florescente de uma das Árvores de Magias conhecidas através do "
                    + "talento ‘Aptidão Mágica Ampla’, você é capaz de mimetizar a magia escolhida ao custo "
                    + "de 5PD. Enquanto tiver ao menos 10PD em sua reserva de Bônus Bases você é imune a "
                    + "Magias que você é capaz de conjurar.",
            () -> FeatRequirements.builder()
                    .requiredFeat(MetamagicoFeat.APTIDAO_MAGICA_SUPREMA)
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(9)
                    .build());

    /** One rung of {@link BranchLevel}'s ladder — what each cap-raising Talento grants. */
    private static final int ONE_RUNG = 1;

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

    private static int graduationOf(final Character character, final SkillType skillType) {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        return characterSkill == null ? 0 : characterSkill.getGraduation().getGraduationValue();
    }
}
