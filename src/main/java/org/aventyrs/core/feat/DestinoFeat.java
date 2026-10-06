package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Deity;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Talentos de Destino — what a character is, rather than what they can do: how enemies read
 * them, which Habilidades they were born to, and when their Títulos Aventyr awaken.
 *
 * <p>Two blockers dominate — one of them now half-lifted. The first is <b>granting another
 * trait</b>: five constants here hand out a Habilidade de Atributo, a Vantagem de Ego, or another
 * Talento outright. The three that grant a <b>Habilidade de Atributo</b> are real now, through
 * {@link HabilidadeDeAtributoEscolhidaFeat}. {@link #EXCEPCIONALIDADE}'s Talento Racial is real
 * too, through {@link ExcepcionalidadeFeat}, which grants the chosen Talento whole. Only {@link
 * #AUTOCONHECIMENTO} is not: a Vantagem de Ego is chosen once at character creation and never
 * awarded later.
 *
 * <p>The second was the <b>Despertar timeline</b>, and it turned out to need no timeline at all.
 * "Adquirido antes de Despertar" is a ceiling checked at acquisition ({@code
 * FeatRequirements#maximumAwakenedTitles}); an EXP mark is read off the sheet when session-end
 * acquisitions are performed ({@link #DESPERTAR_ANTECIPADO}, {@link
 * #CENTELHA_GRAN_AVENTYR_ANTECIPADA}, through {@code CharacterSheet#applySessionEndAcquisitions});
 * and a character's Centelhas are a count ({@code Character#getCentelhas()}).
 *
 * <p><b>Counting the un-awakened ones is <i>not</i> part of that gap</b>, and used to be filed
 * under it by mistake. A Character has exactly three {@link TitleSlot}s, so "cada Título ainda não
 * Desperto" is three minus the held ones — arithmetic available today, with no timeline involved.
 * That is what {@link #ATRASAR_DESPERTAR}'s figures and {@link #ABDICADOR}'s PV/PM multiplier
 * multiply by.
 */
public enum DestinoFeat implements Feat {

    /**
     * "Sua força de vontade permitiu desenvolver-se mais que a maioria, seu multiplicador de PD
     * aumenta em +1."
     *
     * <p><b>Both halves are real.</b> The multiplier, through {@link
     * Feat#resolveDeterminationMultiplierIncrease} — an unconditional, permanent uplift consumed
     * by {@code DeterminationPointsService}. The Descanso recovery, through {@link
     * Feat#resolveRestDeterminationPointsBonus}, summed by {@code
     * RestService#getRecoveredDeterminationPoints}: "a cada Descanso" is every Descanso whatever
     * its type, and a Título Aventyr is "Desperto" simply by being held — the same reading {@code
     * MetamagicoFeat#MENTE_EXPANDIDA}'s identical PM clause takes. Both hooks were added for this
     * constant.
     *
     * <p>The rules text names this Talento "Coração de Ferro", the same as the unrelated {@code
     * DuelistaFeat#CORACAO_DE_FERRO} in another tree. The constant here is suffixed {@code
     * _DO_DESTINO} to keep the two apart, because a {@code Feat}'s {@code name()} <b>is</b> its
     * persisted identity — {@code FeatCatalog} indexes by it and aventyrs-api stores a held Talento
     * as that bare string, so two constants sharing one name are indistinguishable once written and
     * load back as whichever a reader indexes first. Display text comes from {@link
     * #getDescription()}, never from the constant name, so the suffix is invisible to a player.
     */
    CORACAO_DE_FERRO_DO_DESTINO(
            "Sua força de vontade permitiu desenvolver-se mais que a maioria, seu multiplicador de "
                    + "PD aumenta em +1. Sua recuperação de PD também aumenta, a cada Descanso "
                    + "você recupera +2PD, e então +1PD para cada Título Aventyr que tenha "
                    + "Desperto.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveDeterminationMultiplierIncrease(final Character character) {
            return 1;
        }

        @Override
        public int resolveRestDeterminationPointsBonus(final RestType restType, final Character character) {
            return BASE_REST_DETERMINATION_RECOVERY + character.getAllTitles().size();
        }
    },

    /**
     * "A menos que você seja o único alvo disponível você nunca será alvo primário de ataques nas
     * duas primeiras Rodadas de um combate."
     */
    // TODO: nothing chooses an attack's target — a caller does, and this core has no targeting
    //  step to refuse one (the same direction the gap catalog's "Forced attack targeting /
    //  interception" row records, from the other side).
    // "Inimigos inteligentes" is CombatantSheet#isIntelligent() (0.0.84) — whoever picks the target
    //  asks it of the attacker; it gates nothing here until the targeting step above exists.
    // The "Força igual ou inferior à 2" maximum is enforced now —
    // FeatRequirements#maximumAttributeDomain is its own clause, separate from the minimum,
    // because a Talento naming both names two different Atributos.
    APARENCIA_INOFENSIVA(
            "A menos que você seja o único alvo disponível, ou já tenha realizado ações ofensivas "
                    + "contra seus inimigos, você nunca será alvo primário de ataques, Magias ou "
                    + "Habilidades inimigas nas duas primeiras Rodadas de um combate. Este Talento "
                    + "afeta apenas inimigos inteligentes.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.CHARISMA)
                    .requiredAttributeValue(3)
                    .maximumAttributeDomain(AttributeDomain.STRENGTH)
                    .maximumAttributeValue(2)
                    .build()),

    /** As {@link #APARENCIA_INOFENSIVA}, extended to the first four Rodadas. */
    // Same targeting blockers as APARENCIA_INOFENSIVA; its Pré-requisito, though, is now fully
    // enforced — "Carisma 5 e Força 1" is a minimum on one Atributo and an exact figure on
    // another, read as the maximum it also is (a Força of 1 is the floor every Atributo starts
    // at, so "Força 1" can only mean "no higher than 1").
    APARENCIA_VERDADEIRAMENTE_INOFENSIVA(
            "A menos que você seja o único alvo disponível, ou já tenha realizado ações ofensivas "
                    + "contra seus inimigos, você nunca será alvo primário de ataques, Magias ou "
                    + "Habilidades inimigas nas quatro primeiras Rodadas de um combate.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.CHARISMA)
                    .requiredAttributeValue(5)
                    .maximumAttributeDomain(AttributeDomain.STRENGTH)
                    .maximumAttributeValue(1)
                    .requiredFeat(APARENCIA_INOFENSIVA)
                    .build()),

    /**
     * "Escolha um Ego que você possua valor 2 ou superior. Você adquire uma Vantagem do Ego
     * escolhido."
     */
    // TODO: a Vantagem de Ego is chosen once at character creation (CharacterCreationService,
    //  gated on EGO_ADVANTAGE_MIN_BASE) and there is no path to award one afterwards. Note this
    //  Talento would also bypass that threshold of 3, granting at 2.
    AUTOCONHECIMENTO(
            "Escolha um Ego que você possua valor 2 ou superior. Você adquire uma Vantagem do Ego "
                    + "escolhido.",
            FeatRequirements.builder().build()),

    /**
     * "Escolha um Atributo que você possua valor Base 2 ou superior. Você adquire uma Habilidade
     * do Atributo escolhido."
     */
    // Real, through HabilidadeDeAtributoEscolhidaFeat — the acquired form recording which
    // Habilidade the player picked, granted past the AttributeAbilityService slot economy by
    // Feat#getGrantedAttributeAbilities. Its "Atributo com valor Base 2 ou superior" is the same
    // floor that hook's own factory enforces, so the clause needs no second check.
    PRODIGIO(
            "Escolha um Atributo que você possua valor Base 2 ou superior. Você adquire uma "
                    + "Habilidade do Atributo escolhido, você ainda precisa preencher requisitos "
                    + "de Atributos Mínimos - se houver.",
            FeatRequirements.builder().build()),

    /** "Escolha um Atributo, você recebe uma das Habilidades do Atributo escolhido." */
    // Real, the same way as PRODIGIO — HabilidadeDeAtributoEscolhidaFeat serves all three of
    // these constants, since an AttributeAbility already reports its own Atributo.
    // "Atributo 3 ou Superior" is enforced now, through FeatRequirements#requiredAnyAttributeValue.
    GENIALIDADE(
            "Escolha um Atributo, você recebe uma das Habilidades do Atributo escolhido. Você "
                    + "precisa cumprir com Requisitos da Habilidade de Atributo escolhida para "
                    + "receber seus benefícios.",
            FeatRequirements.builder()
                    .requiredAnyAttributeValue(3)
                    .build()),

    /**
     * "Você recebe +1 de Bônus Racial no Atributo escolhido e uma de suas Habilidades do Atributo."
     *
     * <p>The chosen Atributo is recorded now, so the Bônus Racial lands on it — modelled as a
     * {@code Feat#resolveAttributeBonus} grant rather than a write to {@code
     * AttributeValue#racialBonus}, the same reading every other "recebe Bônus Racial de +1"
     * Talento takes.
     */
    // Both halves real, through HabilidadeDeAtributoEscolhidaFeat: the chosen Habilidade, and
    // the "+1 de Bônus Racial no Atributo escolhido" that only this constant of the three adds.
    GENIALIDADE_DESPERTA(
            "Escolha um Atributo, você recebe +1 de Bônus Racial no Atributo escolhido e uma de "
                    + "suas Habilidades do Atributo. Você precisa cumprir com Requisitos da "
                    + "Habilidade de Atributo escolhida para receber seus benefícios.",
            FeatRequirements.builder()
                    .requiredFeat(GENIALIDADE)
                    .requiredAwakenedTitles(2)
                    .build()),

    /** "Escolha um Talento Racial que você cumpra todos os demais requisitos, além da Raça." */
    // Real, through ExcepcionalidadeFeat — the acquired form recording the chosen Talento Racial
    // and granting it whole via Feat#getGrantedFeats, so it is held for effects and prerequisites
    // alike. "Além da Raça" is Feat#isEligibleRegardlessOfRace.
    // "só pode ser adquirido por personagens que passem longos períodos de convivência com
    //  … membros da raça de referência" is a backstory condition — nothing records a character's
    //  upbringing, so it is left to the Narrador.
    EXCEPCIONALIDADE(
            "Escolha um Talento Racial que você cumpra todos os demais requisitos, além da Raça. "
                    + "Você recebe os benefícios do Talento escolhido. Este Talento só pode ser "
                    + "adquirido por personagens que passem longos períodos de convivência com, ou "
                    + "que tenha sido criado por, membros da raça de referência.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.DESTINO)
                    .requiredFeatCategoryCount(2)
                    .build()) {
        /** "Escolha um Talento Racial" — see ExcepcionalidadeFeat#optionsFor. */
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(Feat.class, ExcepcionalidadeFeat.optionsFor(holder)));
        }
    },

    /**
     * "A GD para Conjurar suas Magias Naturais é reduzida em -1 Nível. Se você tiver pelo menos 1
     * Título Aventyr Desperto, o Tempo de Conjuração da primeira Magia Natural que conjurar em
     * Rodadas Ímpares é reduzido em -1PA."
     *
     * <p><b>Both halves are real, as report-only figures on {@code SpellCastingResult}</b> — the
     * eased GD da Conjuração through {@link Feat#resolveCastingDifficultyReduction}, and the
     * reduced Tempo de Ativação through {@link Feat#resolveCastingActionPointReduction}. Nothing
     * compares a roll against the GD or spends the PA, exactly as for every other cast figure.
     */
    // "Magias Naturais" takes both of the source document's uses of Natural — a tree tagged
    // MagicType.NATURAL (Aliados da Natureza, Polimorfismo, Vida) or an Elemental: Natural one
    // (none authored) — since MagicType itself declines to pick a side.
    // "Rodadas Ímpares" counts from 1, as the table does — the first, third, fifth Rodada — which
    // is Scene#getCurrentRound() 0, 2, 4, that counter being 0-based.
    // "A primeira Magia Natural" reads the caster's per-Rodada action log, so an earlier cast
    // counts only once the caller filed its SpellCastingResult#getRecordedAction().
    // TODO: its Pré-requisito is 'Escolhido de Gaea', a Talento de Devoção, excluded from this
    //  catalog (no Adepto/Fiel/Fundamentalista tier exists). That Talento's own Pré-requisito,
    //  Devoto de Gaea, is enforced in its place — looser than the text, never stricter.
    ARCANISMO_DRUIDICO(
            "A GD para Conjurar suas Magias Naturais é reduzida em -1 Nível. Se você tiver pelo "
                    + "menos 1 Título Aventyr Desperto, o Tempo de Conjuração da primeira Magia "
                    + "Natural que conjurar em Rodadas Ímpares é reduzido em -1PA.",
            FeatRequirements.builder()
                    .requiredDeity(Deity.GAEA)
                    .build()) {
        @Override
        public int resolveCastingDifficultyReduction(final Spell spell, final Character character) {
            return isMagiaNatural(spell) ? 1 : 0;
        }

        @Override
        public int resolveCastingActionPointReduction(final Spell spell, final Character character,
                                                      final int currentRound,
                                                      final List<CombatantAction> actionsThisRound) {
            boolean oddRodada = currentRound % 2 == 0;
            boolean firstNaturalOfRodada = actionsThisRound.stream()
                    .noneMatch(action -> action.attackSource() instanceof Spell cast && isMagiaNatural(cast));
            return !character.getAllTitles().isEmpty() && isMagiaNatural(spell) && oddRodada
                    && firstNaturalOfRodada ? 1 : 0;
        }
    },

    /**
     * "Escolha uma Perícia, você recebe os benefícios de Favoritismo da Perícia quando efetuada em
     * disputa contra um oponente que o reconheça."
     */
    // TODO: "os benefícios de Favoritismo da Perícia" are defined in no rules document in this repo
    //  (docs/rules has no Favoritismo entry beyond this Talento), so there is nothing to grant; and
    //  "um oponente que o reconheça" additionally needs recognition between characters, which
    //  nothing tracks. Obtain the Favoritismo rules before building this.
    // "Fama 15" is enforced now: Feat#isEligible has a CharacterSheet-taking overload, which
    // FeatService#grantFeat calls. Read as *either* Fama reaching 15 — the rules text says plain
    // "Fama" while this core splits it Positiva/Negativa, and Favoritismo is about being
    // recognised, which notoriety serves as well as renown. A sheet-less eligibility preview
    // skips the clause rather than failing it, so the listing stays looser than the real gate.
    FAVORITISMO_MAIOR(
            "Escolha uma Perícia, você recebe os benefícios de Favoritismo da Perícia quando "
                    + "efetuada em disputa contra um oponente que o reconheça ou visando um alvo "
                    + "que o reconheça, mesmo que não tenham pessoas neutras a cena assistindo.",
            FeatRequirements.builder()
                    .requiredFame(15)
                    .build()),

    /**
     * "Você pode escolher atrasar seu Despertar de Títulos, recebendo seus benefícios apenas
     * quando quiser e se quiser."
     */
    // Real. Each of the four figures is (1 + 1 per 15 EXP total) × Títulos not yet Desperto, doubled
    // with ABDICADOR: a bonus to every Perícia roll and dano roll, RDS, and a Margem Crítica Menor
    // widening. All four read the holder's sheet for the EXP; without a CharacterSheet they are 0.
    // "Deve ser adquirido antes de Despertar seus Títulos" is FeatRequirements#maximumAwakenedTitles(0).
    ATRASAR_DESPERTAR(
            "Você pode escolher atrasar seu Despertar de Títulos, recebendo seus benefícios apenas "
                    + "quando quiser e se quiser. Você recebe Bônus de +1 em Rolagens de Perícias e "
                    + "Danos, além de Redução de Danos Sofridos e aumento de Margem Crítica Menor "
                    + "em rolagens de Perícias, estes benefícios aumentam em +1 para cada 15EXP e "
                    + "são multiplicados pelo número de Títulos ainda não Despertos. Este Talento "
                    + "deve ser adquirido antes de Despertar seus Títulos.",
            FeatRequirements.builder()
                    .maximumAwakenedTitles(0)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return delayedAwakeningFigure(character, holder);
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                        final CombatantSheet attackTarget, final Character actor,
                                                        final AttackSource attackSource, final int targetCount,
                                                        final CombatantSheet holder) {
            int figure = delayedAwakeningFigure(actor, holder);
            return figure > 0 ? Optional.of(new DamageBonus(figure, DamageType.FISICO)) : Optional.empty();
        }

        @Override
        public int resolveDamageTakenReduction(final Character character, final CombatantSheet holder) {
            return delayedAwakeningFigure(character, holder);
        }

        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                 final Character character, final AttackSource attackSource,
                                                 final CombatantSheet holder) {
            return delayedAwakeningFigure(character, holder);
        }
    },

    /**
     * "Você não Desperta Títulos Aventyr, mantendo suas Centelhas inertes indefinidamente."
     *
     * <p><b>The multiplier sentence is real</b>: "+1 [de Multiplicador de PV e PM] para cada
     * Título ainda não Desperto", through {@link Feat#resolveLifeMultiplierIncrease} and {@link
     * Feat#resolveManaMultiplierIncrease}. A Título "ainda não Desperto" is an <b>unfilled {@link
     * TitleSlot}</b> — a Character has exactly three, so the multiplicand is simply the three
     * minus however many are held. That reading is what makes this the one clause of the Despertar
     * cluster that needs no timeline: it counts slots, not events.
     *
     * <p>It is the catalog's only multiplier that moves <em>inversely</em> with Títulos — every
     * other one grows as they awaken ({@code OrquicoFeat#TERRA_NAS_VEIAS}), which is exactly the
     * trade this Talento is written to make.
     */
    // "Os Benefícios de Atrasar Despertar são dobrados" is real too: ATRASAR_DESPERTAR's figure
    // doubles while this is held (delayedAwakeningFigure).
    // "Você não Desperta Títulos Aventyr" closes every slot (permitsTitleSlot), as Filho de Gilgamesh
    //  does — so TitleAcquisitionService#grantTitle, a pre-picked TitleAwakening and
    //  TitleAwakeningService's picker all refuse. Character#grantTitle, the unchecked mutator, still
    //  doesn't ask.
    ABDICADOR(
            "Você não Desperta Títulos Aventyr, mantendo suas Centelhas inertes indefinidamente. "
                    + "Os Benefícios de Atrasar Despertar são dobrados e seu Multiplicador de PV e "
                    + "PM aumentam em +1 para cada Título ainda não Desperto.",
            FeatRequirements.builder()
                    .requiredFeat(ATRASAR_DESPERTAR)
                    .build()) {
        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return unawakenedTitles(character);
        }

        @Override
        public int resolveManaMultiplierIncrease(final Character character) {
            return unawakenedTitles(character);
        }

        @Override
        public boolean permitsTitleSlot(final TitleSlot slot, final Character character) {
            return false;
        }
    },

    /**
     * "Você recebe Bônus Racial de +2 em todos os Atributos para cada Centelha que você não possua
     * ou voluntariamente não Despertar."
     */
    // Real. The count is the Centelhas not possessed (Character.CENTELHAS minus getCentelhas()),
    // plus — for a holder of ATRASAR_DESPERTAR, whose delay is the voluntary one — the possessed
    // ones not yet Desperto. ⚠️ Without Atrasar Despertar an un-awakened Centelha is read as not yet
    // awakened rather than voluntarily withheld. The Pré-requisito's disjunction is enforced: Atrasar
    // Despertar, or at most CENTELHAS - 1 Centelhas.
    FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH(
            "Você recebe Bônus Racial de +2 em todos os Atributos para cada Centelha que você não "
                    + "possua ou voluntariamente não Despertar.",
            FeatRequirements.builder()
                    .alternative(FeatRequirements.builder().requiredFeat(ATRASAR_DESPERTAR).build())
                    .alternative(FeatRequirements.builder().maximumCentelhas(Character.CENTELHAS - 1).build())
                    .build()) {
        @Override
        public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
            return GILGAMESH_ATTRIBUTE_BONUS * forgoneCentelhas(character);
        }
    },

    /** "Você pode reduzir o Tempo de Ativação de suas Habilidades de Título em -1PA." */
    // Real, as an opt-in: name it on TitleAbilityActivationRequest#activatedFeats and the activation
    // takes 1PA off a fixed Tempo de Ativação (never below 1PA) for +2PD. "Uma vez a cada Rodada" is
    // one use per Turn (CombatantSheet#countActivationsThisTurn); a second is refused.
    // TODO: "apenas em seu Turno" is not checked — the request does not say whose Turn it is.
    ACELERAR_HABILIDADE(
            "Você pode reduzir o Tempo de Ativação de suas Habilidades de Título em -1PA, acelerar "
                    + "Habilidades aumenta o Custo de Ativação da Habilidade em +2PD. Este efeito "
                    + "pode ser ativado apenas uma vez a cada Rodada e apenas em seu Turno.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveTitleActivationSurcharge(final AventyrTitleAbility ability, final CombatantSheet activator,
                                                   final Set<Feat> activatedFeats) {
            return TITLE_OPT_IN_SURCHARGE;
        }

        @Override
        public int resolveTitleActivationActionPointReduction(final AventyrTitleAbility ability,
                                                             final CombatantSheet activator,
                                                             final Set<Feat> activatedFeats) {
            return 1;
        }

        @Override
        public boolean permitsTitleActivationOptIn(final CombatantSheet activator) {
            return activator.countActivationsThisTurn(this) == 0;
        }
    },

    /** "Durante a Ativação de uma Habilidade você pode aumentar seu Custo em +2PD, se o fizer a Duração da Habilidade é aumentada em +2 Unidades." */
    // Real, as an opt-in (TitleAbilityActivationRequest#activatedFeats): +2PD, and +2 Unidades of
    // Duração reported on InteractionResult#getTitleAbilityDurationIncrease.
    // TODO: the Duração itself is not extended here — each Habilidade's Interaction hard-codes its
    //  own, so the caller applies the reported +2.
    CENTELHA_DURADOURA(
            "Durante a Ativação de uma Habilidade você pode aumentar seu Custo em +2PD, se o fizer "
                    + "a Duração da Habilidade é aumentada em +2 Unidades.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveTitleActivationSurcharge(final AventyrTitleAbility ability, final CombatantSheet activator,
                                                   final Set<Feat> activatedFeats) {
            return TITLE_OPT_IN_SURCHARGE;
        }

        @Override
        public int resolveTitleAbilityDurationIncrease(final AventyrTitleAbility ability, final CombatantSheet activator,
                                                       final Set<Feat> activatedFeats) {
            return CENTELHA_DURADOURA_INCREASE;
        }
    },

    /**
     * "Seu personagem desperta seu Título Primário ao fim da primeira sessão de Jogo." Pré-requisito:
     * "Apenas personagens recém-criados".
     *
     * <p><b>Real.</b> The player picks the Título when taking the Talento ({@link
     * DespertarAntecipadoFeat}, offered by {@link #resolveRequiredChoices}), and {@code
     * CharacterSheet#applySessionEndAcquisitions} awakens it into the Título Primário slot when
     * the session ends. "Recém-criados" is {@link #isAcquirableOnlyAtCreation()}: only a starting
     * Talento slot can take it.
     */
    DESPERTAR_ANTECIPADO(
            "Seu personagem desperta seu Título Primário ao fim da primeira sessão de Jogo.",
            FeatRequirements.builder().build()) {
        /** "seu Título Primário" — which one; see DespertarAntecipadoFeat#optionsFor. */
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(AventyrTitle.class, DespertarAntecipadoFeat.optionsFor(holder)));
        }

        @Override
        public boolean isAcquirableOnlyAtCreation() {
            return true;
        }
    },

    /** "Você desperta seu Título Secundário ao atingir a marca de 23EXP." */
    // Real, through CentelhaGranAventyrAntecipadaFeat: the Secundário picked on acquisition is
    // awakened at the first reported session end once the Primário is filled and total EXP reaches 23.
    CENTELHA_GRAN_AVENTYR_ANTECIPADA(
            "Você desperta seu Título Secundário ao atingir a marca de 23EXP.",
            FeatRequirements.builder()
                    .requiredFeat(DESPERTAR_ANTECIPADO)
                    .build()) {
        /** "seu Título Secundário" — which one; see CentelhaGranAventyrAntecipadaFeat#optionsFor. */
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(AventyrTitle.class, CentelhaGranAventyrAntecipadaFeat.optionsFor(holder)));
        }
    };

    /** CORACAO_DE_FERRO_DO_DESTINO's flat "+2PD" per Descanso, before the per-Título term. */
    private static final int BASE_REST_DETERMINATION_RECOVERY = 2;

    /** Acelerar Habilidade's and Centelha Duradoura's "+2PD". */
    private static final int TITLE_OPT_IN_SURCHARGE = 2;

    /** Centelha Duradoura: "a Duração da Habilidade é aumentada em +2 Unidades". */
    private static final int CENTELHA_DURADOURA_INCREASE = 2;

    /** Fragmento da Encarnação de Gilgamesh: "+2 em todos os Atributos para cada Centelha". */
    private static final int GILGAMESH_ATTRIBUTE_BONUS = 2;

    /** Atrasar Despertar: "aumentam em +1 para cada 15EXP". */
    private static final BigDecimal DELAYED_AWAKENING_EXPERIENCE_STEP = BigDecimal.valueOf(15);

    /**
     * {@link #ATRASAR_DESPERTAR}'s figure: 1, +1 per 15 EXP total, times the Títulos not yet Desperto
     * — doubled for a holder of {@link #ABDICADOR}. 0 without a {@code CharacterSheet} to read the EXP
     * from.
     */
    private static int delayedAwakeningFigure(final Character character, final CombatantSheet holder) {
        if (character == null || !(holder instanceof CharacterSheet sheet)) {
            return 0;
        }
        int perTitle = 1 + sheet.getTotalExperience()
                .divide(DELAYED_AWAKENING_EXPERIENCE_STEP, 0, RoundingMode.FLOOR).intValue();
        int figure = perTitle * unawakenedTitles(character);
        boolean abdicador = character.getFeats().stream().anyMatch(feat -> feat.catalogEntry() == ABDICADOR);
        return abdicador ? 2 * figure : figure;
    }

    /**
     * {@link #FRAGMENTO_DA_ENCARNACAO_DE_GILGAMESH}'s count: Centelhas not possessed, plus — while the
     * holder delays through {@link #ATRASAR_DESPERTAR} — possessed ones not yet Desperto.
     */
    private static int forgoneCentelhas(final Character character) {
        int missing = Math.max(0, Character.CENTELHAS - character.getCentelhas());
        boolean delaying = character.getFeats().stream().anyMatch(feat -> feat.catalogEntry() == ATRASAR_DESPERTAR);
        int withheld = delaying ? Math.max(0, character.getCentelhas() - character.getAllTitles().size()) : 0;
        return missing + withheld;
    }

    /**
     * Títulos Aventyr this character has <b>not</b> awakened — the three {@link TitleSlot}s minus
     * the filled ones. What {@link #ABDICADOR}'s "para cada Título ainda não Desperto" multiplies
     * by; see that constant for why an empty slot is the right reading.
     */
    private static int unawakenedTitles(final Character character) {
        return TitleSlot.values().length - character.getAllTitles().size();
    }

    /**
     * Whether spell is a "Magia Natural" for {@link #ARCANISMO_DRUIDICO} — its tree carries {@link
     * MagicType#NATURAL} as either half of its tag, or is {@code Elemental: Natural}.
     */
    private static boolean isMagiaNatural(final Spell spell) {
        SpellTree tree = spell.getTree();
        return tree != null && (tree.hasMagicType(MagicType.NATURAL)
                || tree.getElementalType().filter(ElementalType.NATURAL::equals).isPresent());
    }

    private final String description;
    private final FeatRequirements featRequirements;

    DestinoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.DESTINO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }
}
