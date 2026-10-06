package org.aventyrs.core.title.bruxo;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MimetizedSpell;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.sheet.CombatantSheet;
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
