package org.aventyrs.core.title.bruxo;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.EgoPointsService;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.magic.invocation.InvocationOptions;
import org.aventyrs.core.magic.invocation.SummonEnhancement;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateService;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.TitleIdentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.COMET_REQUIRES_OPEN_SKY;
import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_LOCKED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;

/**
 * Bruxo — "servos ou devotos de seres poderosos e que não possuem o status de Deus". Two systems
 * carry almost all of it:
 *
 * <ul>
 *   <li><b>Misticismos</b> — Árvores de Magia taught by the Título and cast with PD. Each is a set
 *       of {@link MimetizedSpell}s ({@link #getGrantedMimetizedSpells}), so it reaches the cast
 *       through the ordinary mimicry path and never touches an Arcanista's Árvores. Only the
 *       picks are stored ({@link #chooseMisticismo}, {@link #choosePactoTrees}); which rungs are
 *       castable and at what price is derived on every call from the Habilidades held.</li>
 *   <li><b>Invocations</b> — what the holder's summons gain, read by {@code
 *       #resolveSummonEnhancement} when a summon is planned.</li>
 * </ul>
 *
 * <p>Table rulings (2026-10-05): a Misticismo gives <b>both</b> ramificações; "Habilidades de
 * Bruxo" excludes Especializações; either half of a tree's tag satisfies a type restriction. See
 * {@code docs/bruxo.md}.
 */
public class Bruxo implements AventyrTitle {

    private static final String BASE_EFFECT_DESCRIPTION = BruxoDespertar.CONTRATO_COM_ALEM.getDescription();

    private static final String PRIMARY_TITLE_BONUS_DESCRIPTION =
            "Se Bruxo for seu Título Primário, uma vez por Rodada você pode escolher substituir o Custo do " +
            "Misticismo que você conjurar de PD por PV. A quantidade de PV necessária para conjurações é igual 1+ " +
            "à quantidade de PD que seria utilizada. Pontos de Vida perdidos desta forma são recuperados apenas em " +
            "Descansos Verdadeiros.";

    /** Habilidades de Bruxo needed to cast each rung — "pelo menos 2 … Com 5 … com 7". */
    static final int BROTO_ABILITIES = 2;
    static final int MUDA_ABILITIES = 5;
    static final int EMERGENTE_ABILITIES = 7;

    /** Pacto de Conjuração: "Escolha 2 de seus Misticismos". */
    public static final int PACTO_TREE_COUNT = 2;

    /** O Grande Bruxo: "A quantidade de PV necessária … é igual 1+ à quantidade de PD". */
    static final int GRANDE_BRUXO_EXTRA_HIT_POINTS = 1;

    /** The per-Rodada use marker of O Grande Bruxo ({@code CombatantSheet#spendRoundScopedUse}). */
    public static final String GRANDE_BRUXO = "O Grande Bruxo";

    private final List<BruxoSpecialization> specializations;
    private final List<AventyrTitleAbility> abilities;
    private final Map<String, SpellTree> misticismos = new LinkedHashMap<>();
    private final Set<SpellTree> pactoTrees = new LinkedHashSet<>();
    private Familiar familiar;

    /** Both lists are copied into mutable ones, so the grant mutators can append to them. */
    public Bruxo(@NonNull final List<BruxoSpecialization> specializations,
                 @NonNull final List<AventyrTitleAbility> abilities) {
        this(specializations, abilities, Map.of(), Set.of());
    }

    /**
     * As {@link #Bruxo(List, List)}, restoring the picks already made — for a caller rebuilding a
     * held Título from persistence. {@code misticismos} is keyed by the teaching trait's {@code
     * name()} ({@link #teacherNamed}).
     */
    public Bruxo(@NonNull final List<BruxoSpecialization> specializations,
                 @NonNull final List<AventyrTitleAbility> abilities,
                 @NonNull final Map<String, ? extends SpellTree> misticismos,
                 @NonNull final Set<? extends SpellTree> pactoTrees) {
        this.specializations = new ArrayList<>(specializations);
        this.abilities = new ArrayList<>(abilities);
        misticismos.forEach((teacher, tree) -> chooseMisticismo(teacherNamed(teacher)
                .orElseThrow(() -> new IllegalOperationException(TITLE_ABILITY_CHOICE_LOCKED)), tree));
        if (!pactoTrees.isEmpty()) {
            choosePactoTrees(Set.copyOf(pactoTrees));
        }
    }

    /** As the four-argument restore, with the Familiar Maior a ritual already bound, or {@code null}. */
    public Bruxo(@NonNull final List<BruxoSpecialization> specializations,
                 @NonNull final List<AventyrTitleAbility> abilities,
                 @NonNull final Map<String, ? extends SpellTree> misticismos,
                 @NonNull final Set<? extends SpellTree> pactoTrees, final Familiar familiar) {
        this(specializations, abilities, misticismos, pactoTrees);
        this.familiar = familiar;
    }

    /** The Bruxo character holds, if any. */
    public static Optional<Bruxo> heldBy(@NonNull final Character character) {
        return character.getAllTitles().stream()
                .filter(Bruxo.class::isInstance)
                .map(Bruxo.class::cast)
                .findFirst();
    }

    /** The Bruxo sheet's Character holds, if any. */
    public static Optional<Bruxo> heldBy(@NonNull final CombatantSheet sheet) {
        return heldBy(sheet.getCharacter());
    }

    /** Every Bruxo trait, by name — what a consumer restores a stored Misticismo pick against. */
    public static Optional<MisticismoTeacher> teacherNamed(@NonNull final String name) {
        return Stream.of(BruxoDespertar.values(), BruxoSpecialization.values(), BruxoAbility.values(),
                        IluminadoAbility.values(), OraculoAbissalAbility.values())
                .flatMap(Arrays::stream)
                .map(MisticismoTeacher.class::cast)
                .filter(teacher -> teacher.name().equals(name))
                .findFirst();
    }

    @Override
    public String getName() {
        return "Bruxo";
    }

    @Override
    public Optional<TitleIdentity> getIdentity() {
        return Optional.of(TitleIdentity.BRUXO);
    }

    /** "Centelha Abençoada" — the header Bruxo is listed under. */
    @Override
    public TitleArchetype getArchetype() {
        return TitleArchetype.ABENCOADO;
    }

    @Override
    public String getBaseEffectDescription() {
        return BASE_EFFECT_DESCRIPTION;
    }

    @Override
    public String getPrimaryTitleBonusDescription() {
        return PRIMARY_TITLE_BONUS_DESCRIPTION;
    }

    @Override
    public List<AventyrTitleSpecialization> getSpecializations() {
        return specializations.stream().map(AventyrTitleSpecialization.class::cast).toList();
    }

    @Override
    public List<AventyrTitleAbility> getAbilities() {
        return abilities;
    }

    /** Every trait held, the Despertar first — it carries the first Misticismo pick. */
    @Override
    public List<AventyrTitleAbility> getAllAbilities() {
        return Stream.concat(Stream.of(BruxoDespertar.CONTRATO_COM_ALEM),
                AventyrTitle.super.getAllAbilities().stream()).toList();
    }

    @Override
    public void grantAbility(final AventyrTitleAbility ability) {
        abilities.add(ability);
    }

    /**
     * Refuses anything but this Título's own two Especializações — the enforced half of "apenas
     * 'Bruxos' podem adquirir esta especialização".
     */
    @Override
    public void grantSpecialization(final AventyrTitleSpecialization specialization) {
        if (!(specialization instanceof BruxoSpecialization own)) {
            throw new IllegalOperationException(TITLE_ABILITY_PREREQUISITE_NOT_MET);
        }
        specializations.add(own);
    }

    /** Whether this Título holds trait — its Despertar, an Especialização, Habilidade or Suprema. */
    public boolean holds(final AventyrTitleAbility trait) {
        return getAllAbilities().contains(trait);
    }

    // --- Counting ----------------------------------------------------------------------------------

    /**
     * "Habilidades de Bruxo" — every Habilidade and Suprema held, Especializações excluded (table
     * ruling, 2026-10-05). What the casting rungs, the Familiar's leash and the "requer N
     * Habilidades de Bruxo" Supremas read.
     */
    public int getBruxoAbilityCount() {
        return abilities.size();
    }

    /**
     * "Habilidades ou Suprema de {specialization}" — only the Habilidades/Supremas gated on that one
     * Especialização, what each Iluminado/Oráculo Abissal scaling reads. 0 while the Especialização
     * itself is not held.
     */
    public int getAbilityCount(@NonNull final AventyrTitleSpecialization specialization) {
        if (!specializations.contains(specialization)) {
            return 0;
        }
        return (int) abilities.stream()
                .filter(ability -> ability.getRequiredSpecialization().filter(specialization::equals).isPresent())
                .count();
    }

    /** Familiar Maior: "nunca se afasta mais do que 'Quantidade de Habilidades de Bruxo' UD de você". */
    public int getFamiliarLeash() {
        return getBruxoAbilityCount();
    }

    // --- Misticismos -------------------------------------------------------------------------------

    /** Every trait held that teaches a Misticismo, the Despertar first. */
    public List<MisticismoTeacher> getMisticismoTeachers() {
        return getAllAbilities().stream()
                .filter(MisticismoTeacher.class::isInstance)
                .map(MisticismoTeacher.class::cast)
                .filter(teacher -> teacher.getMisticismoFilter().isPresent())
                .toList();
    }

    /** The teachers held whose Misticismo is not picked yet — what a client prompts for. */
    public List<MisticismoTeacher> getPendingMisticismoTeachers() {
        return getMisticismoTeachers().stream()
                .filter(teacher -> !misticismos.containsKey(teacher.name()))
                .toList();
    }

    /** The Árvores teacher may still teach: admitted by its clause and not already a Misticismo. */
    public List<SpellTree> getMisticismoOptions(@NonNull final MisticismoTeacher teacher) {
        MisticismoFilter filter = teacher.getMisticismoFilter().orElse(null);
        if (filter == null) {
            return List.of();
        }
        return Arrays.stream(MagicTree.values())
                .map(SpellTree.class::cast)
                .filter(filter::admits)
                .filter(tree -> !misticismos.containsValue(tree) || tree.equals(misticismos.get(teacher.name())))
                .toList();
    }

    /**
     * Records the Árvore teacher teaches. Picking the same tree again is harmless; a different one, a
     * tree the clause does not admit, or one another trait already taught is refused — "um novo
     * Misticismo". Not gated on holding teacher, so a caller may record the pick in the same step
     * that grants it; a pick whose teacher is not held grants nothing ({@link #getMisticismos}).
     *
     * @throws IllegalOperationException {@code TITLE_ABILITY_CHOICE_LOCKED}
     */
    public void chooseMisticismo(@NonNull final MisticismoTeacher teacher, @NonNull final SpellTree tree) {
        SpellTree current = misticismos.get(teacher.name());
        if (tree.equals(current)) {
            return;
        }
        boolean admitted = teacher.getMisticismoFilter().map(filter -> filter.admits(tree)).orElse(false);
        if (current != null || !admitted || misticismos.containsValue(tree)) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_LOCKED);
        }
        misticismos.put(teacher.name(), tree);
    }

    /** Every pick recorded, by its teacher's name — what a consumer persists. */
    public Map<String, SpellTree> getMisticismoPicks() {
        return Collections.unmodifiableMap(misticismos);
    }

    /** The Misticismos known — the picks whose teaching trait is held. */
    public List<SpellTree> getMisticismos() {
        return getMisticismoTeachers().stream()
                .map(teacher -> misticismos.get(teacher.name()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Records Pacto de Conjuração's "Escolha 2 de seus Misticismos". Exactly {@link #PACTO_TREE_COUNT}
     * trees, each already a pick; once made, only the same pair is accepted again. Not gated on holding
     * Pacto de Conjuração, for the same reason as {@link #chooseMisticismo}.
     *
     * @throws IllegalOperationException {@code TITLE_ABILITY_CHOICE_LOCKED}
     */
    public void choosePactoTrees(@NonNull final Set<? extends SpellTree> trees) {
        if (trees.equals(pactoTrees)) {
            return;
        }
        if (!pactoTrees.isEmpty() || trees.size() != PACTO_TREE_COUNT || !misticismos.values().containsAll(trees)) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_LOCKED);
        }
        pactoTrees.addAll(trees);
    }

    /** The two Pacto de Conjuração Misticismos, as picked. */
    public Set<SpellTree> getPactoTrees() {
        return Collections.unmodifiableSet(pactoTrees);
    }

    /**
     * The deepest rung tree's Magias may be cast at: Semente always, Broto from 2 Habilidades de Bruxo,
     * Muda from 5, Emergente from 7 — and Florescente for a Pacto de Conjuração tree while that Suprema
     * is held. Empty for a tree that is not one of this Bruxo's Misticismos.
     */
    public Optional<BranchLevel> getMisticismoCeiling(@NonNull final SpellTree tree) {
        if (!getMisticismos().contains(tree)) {
            return Optional.empty();
        }
        if (pactoTrees.contains(tree) && abilities.contains(BruxoAbility.PACTO_DE_CONJURACAO)) {
            return Optional.of(BranchLevel.FLORESCENTE);
        }
        int count = getBruxoAbilityCount();
        if (count >= EMERGENTE_ABILITIES) {
            return Optional.of(BranchLevel.EMERGENTE);
        }
        if (count >= MUDA_ABILITIES) {
            return Optional.of(BranchLevel.MUDA);
        }
        return Optional.of(count >= BROTO_ABILITIES ? BranchLevel.BROTO : BranchLevel.SEMENTE);
    }

    /** What a Misticismo Magia at level costs in PD — 0, 1, 2, 3, and 5 for a Pacto Florescente. */
    public static int getMisticismoCost(@NonNull final BranchLevel level) {
        return switch (level) {
            case SEMENTE -> 0;
            case BROTO -> 1;
            case MUDA -> 2;
            case EMERGENTE -> 3;
            case FLORESCENTE -> 5;
        };
    }

    /**
     * Every Magia of every Misticismo down to its ceiling, <b>both ramificações</b> (table ruling,
     * 2026-10-05), mimetized at {@link #getMisticismoCost}. Derived on every call, so a newly granted
     * Habilidade opens its rung at once.
     */
    @Override
    public List<MimetizedSpell> getGrantedMimetizedSpells(final Character character) {
        return getMisticismos().stream()
                .flatMap(tree -> {
                    BranchLevel ceiling = getMisticismoCeiling(tree).orElseThrow();
                    return tree.getSpells().stream()
                            .filter(spell -> ceiling.isAtLeast(spell.getBranchLevel()))
                            .map(spell -> MimetizedSpell.builder()
                                    .spell(spell)
                                    .determinationPointCost(getMisticismoCost(spell.getBranchLevel()))
                                    .build());
                })
                .toList();
    }

    // --- Familiar Maior ----------------------------------------------------------------------------

    /** The Familiar Maior a ritual bound, if one has. */
    public Optional<Familiar> getFamiliar() {
        return Optional.ofNullable(familiar);
    }

    /**
     * Familiar Maior's ritual — "Custo de Ativação: 1 ponto permanente de Ego, a escolha do jogador. Tempo de Ativação:
     * Ritual, 1 dia". Not an in-Cena activation: the Ego is permanent, and a sheet's {@code Character} is final, so
     * this returns holder rebuilt with sacrificed one point lower ({@code EgoPointsService#sacrificePermanent}), with
     * this Bruxo — the same instance, in both — bound to familiar for life. The day the ritual takes is the
     * caller's; nothing here keeps game time.
     *
     * @throws IllegalOperationException {@code REQUIRED_TITLE_TRAIT_NOT_HELD} without Familiar Maior or when holder does
     *         not hold this Bruxo; {@code TITLE_ABILITY_CHOICE_LOCKED} once a familiar is bound; {@code
     *         NOT_ENOUGH_EGO_POINTS} from the sacrifice — all before anything changes
     */
    public Character performFamiliarRitual(@NonNull final Character holder, @NonNull final Familiar familiar,
                                           @NonNull final EgoDomain sacrificed,
                                           @NonNull final EgoPointsService egoPointsService) {
        require(holds(BruxoAbility.FAMILIAR_MAIOR) && holder.getAllTitles().contains(this));
        if (this.familiar != null) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_LOCKED);
        }
        Character rebuilt = egoPointsService.sacrificePermanent(holder, sacrificed, 1);
        this.familiar = familiar;
        return rebuilt;
    }

    /**
     * Brings the bound Familiar Maior into a Cena beside holder: its token joins as holder's invocation with no
     * Duração ("passará a acompanhar o personagem pelo resto de sua vida"), and holder commands it as a Subordinado
     * whose creature is that token. The leash ({@link #getFamiliarLeash()} UD) is the caller's to keep — this core
     * holds no positions.
     *
     * @throws IllegalOperationException {@code REQUIRED_TITLE_TRAIT_NOT_HELD} with no familiar bound, or anything
     *         {@code SubordinateService#command} refuses (the Carisma limit, a duplicate grade)
     */
    public SceneSummon summonFamiliar(@NonNull final Scene scene, @NonNull final CombatantSheet holder,
                                      @NonNull final Player gm, @NonNull final SubordinateService subordinates) {
        require(familiar != null);
        MonsterSheet token = new FamiliarTemplate(familiar).spawn(gm);
        subordinates.command(holder, new Subordinate(familiar.benefit(), false, FAMILIAR_SOURCE, token.getId(), null),
                null);
        return scene.addSummon(holder, token, null, null);
    }

    /**
     * The Familiar Maior's token has left the Cena — it fell, or it was sent away: holder stops commanding its
     * Subordinado. The familiar itself stays bound ("pelo resto de sua vida") and may be called again. Whether any was
     * commanded.
     */
    public boolean dismissFamiliar(@NonNull final CombatantSheet holder, @NonNull final SubordinateService subordinates) {
        List<Subordinate> held = org.aventyrs.core.subordinate.SubordinateBenefits.of(holder).stream()
                .filter(subordinate -> FAMILIAR_SOURCE.equals(subordinate.getSource()))
                .toList();
        held.forEach(subordinate -> subordinates.dismiss(holder, subordinate.getId()));
        return !held.isEmpty();
    }

    /** The source a Familiar Maior's Subordinado is commanded under. */
    public static final String FAMILIAR_SOURCE = "Familiar Maior";

    // --- Invocations -------------------------------------------------------------------------------

    /**
     * Everything this Bruxo adds to an invocation. The passive clauses follow the traits held, each scaling with
     * "Habilidades ou Suprema de {Especialização}" ({@link #getAbilityCount}); the opt-ins are refused unless the trait
     * offering them is held — and the comet needs the caller's word that the sky is open.
     *
     * @throws IllegalOperationException {@code REQUIRED_TITLE_TRAIT_NOT_HELD}, {@code TITLE_ABILITY_CHOICE_LOCKED} for
     *         an Atributo other than Força or Destreza, {@code COMET_REQUIRES_OPEN_SKY}
     */
    @Override
    public SummonEnhancement resolveSummonEnhancement(@NonNull final CombatantSheet caster,
                                                      @NonNull final InvocationOptions options) {
        boolean maior = options.extraTimeForLife() || options.boostedAttribute() != null;
        require(!maior || holds(BruxoAbility.INVOCACAO_MAIOR));
        require(!options.doubled() || holds(BruxoAbility.INVOCACAO_DUPLA));
        require(!options.comet() || holds(IluminadoAbility.MALDICAO_DA_NEVASCA_DO_SUDOESTE));
        if (options.boostedAttribute() != null && options.boostedAttribute() != AttributeDomain.STRENGTH
                && options.boostedAttribute() != AttributeDomain.DEXTERITY) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_LOCKED);
        }
        if (options.comet() && !options.openSky()) {
            throw new IllegalOperationException(COMET_REQUIRES_OPEN_SKY);
        }
        int iluminado = getAbilityCount(BruxoSpecialization.ILUMINADO);
        int oraculo = getAbilityCount(BruxoSpecialization.ORACULO_ABISSAL);
        return SummonEnhancement.builder()
                .lifeMultiplierBonus(options.extraTimeForLife() ? SummonEnhancement.LIFE_MULTIPLIER_BONUS : 0)
                .boostedAttribute(options.boostedAttribute())
                .invocacaoMaior(maior)
                .doubled(options.doubled())
                .attackDifficultyReduction(holds(BruxoSpecialization.ILUMINADO) ? 1 : 0)
                .defenseDifficultyReduction(holds(BruxoSpecialization.ORACULO_ABISSAL) ? 1 : 0)
                .flightBonusRounds(holds(IluminadoAbility.BENCAO_DO_VENTO_DO_LESTE) ? iluminado / 2 : null)
                .regenerationPerRound(holds(IluminadoAbility.BENCAO_DO_MAR_DO_SUL) ? 2 + iluminado / 2 : 0)
                .sizeIncrease(holds(IluminadoAbility.MALDICAO_DO_PANTANO_DO_SUDESTE) ? 2 + iluminado / 2 : 0)
                .cometDamageBonus(options.comet() ? 2 * iluminado : null)
                .fireAttacks(holds(OraculoAbissalAbility.MALDICAO_DAS_CHAMAS_DO_NORTE))
                .damageTakenReduction(holds(OraculoAbissalAbility.MALDICAO_DO_DESERTO_DO_OESTE) ? 2 + oraculo : 0)
                .cataclysm(holds(OraculoAbissalAbility.BENCAO_DOS_VULCOES_DO_NOROESTE))
                .criticalMarginIncrease(holds(OraculoAbissalAbility.BENCAO_DOS_VULCOES_DO_NOROESTE) ? 1 + oraculo / 2 : 0)
                .lightning(holds(OraculoAbissalAbility.BENCAO_DOS_RAIOS_DO_NORDESTE))
                .lightningDamageBonus(holds(OraculoAbissalAbility.BENCAO_DOS_RAIOS_DO_NORDESTE) ? oraculo : 0)
                .build();
    }

    private static void require(final boolean held) {
        if (!held) {
            throw new IllegalOperationException(REQUIRED_TITLE_TRAIT_NOT_HELD);
        }
    }

    /** Whether mimetized is one of this Bruxo's own Misticismo Magias, at its current price. */
    public boolean isMisticismo(@NonNull final MimetizedSpell mimetized) {
        return getGrantedMimetizedSpells(null).contains(mimetized);
    }

    /**
     * O Grande Bruxo: while Bruxo is the Título Primário, "uma vez por Rodada", a Misticismo may be paid
     * with "1+ à quantidade de PD" PV instead. Empty once this Rodada's use is spent ({@link
     * #consumeMimicryHitPointPayment}).
     */
    @Override
    public OptionalInt resolveMimicryHitPointCost(@NonNull final MimetizedSpell mimetized,
                                                  @NonNull final CombatantSheet caster, final boolean primary) {
        return primary && isMisticismo(mimetized) && caster.getRoundScopedUses(GRANDE_BRUXO) == 0
                ? OptionalInt.of(GRANDE_BRUXO_EXTRA_HIT_POINTS + mimetized.getDeterminationPointCost())
                : OptionalInt.empty();
    }

    /** Spends O Grande Bruxo's use for the Rodada. */
    @Override
    public void consumeMimicryHitPointPayment(@NonNull final MimetizedSpell mimetized,
                                              @NonNull final CombatantSheet caster) {
        if (isMisticismo(mimetized)) {
            caster.spendRoundScopedUse(GRANDE_BRUXO);
        }
    }
}
