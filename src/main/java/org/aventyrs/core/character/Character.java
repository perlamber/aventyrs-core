package org.aventyrs.core.character;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.ability.AcquiredChoice;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.FreeActionsService;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.item.RegaliaGrade;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitle;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;

@Builder(toBuilder = true) @Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Character {
    /**
     * A unique, stable identifier for this Character — independent of any specific
     * {@link org.aventyrs.core.sheet.CombatantSheet} wrapping it (see that class's own
     * {@code id}), e.g. so {@link org.aventyrs.core.scene.Scene} can tell participants apart
     * without relying on object-reference equality.
     */
    @Builder.Default
    protected UUID id = UUID.randomUUID();

    /**
     * The {@link Player} behind this character, or {@code null} for one nobody plays — a monster
     * (see {@code org.aventyrs.core.monster.MonsterTemplate}), or an NPC. Deliberately not
     * {@code @NonNull}: nothing in this core reads it, and the {@code player} that actually
     * matters is {@link org.aventyrs.core.sheet.CharacterSheet}'s own, which stays required
     * there because a <i>player character's sheet</i> genuinely has one.
     */
    protected Player player;

    @NonNull
    protected String name;

    @NonNull
    protected Race race;

    /**
     * The character's sex — e.g. {@code PersuasaoCompetencyAbility#SEDUTOR}'s "personagens do
     * sexo oposto". No default and not {@code @NonNull}: unlike {@link #race}/{@link #name},
     * nothing in this core currently requires every {@code Character} to name one, so it
     * stays {@code null} unless set.
     */
    protected Sexo sexo;

    /**
     * The deity (if any) this character is devoted to. No default and not {@code @NonNull},
     * same as {@link #sexo} — nothing in this core currently requires every {@code Character}
     * to name one.
     */
    protected Deity deity;

    /**
     * Tendência — a 1-10 scale (per this ruleset's character sheet). Defaults to 1 (the
     * floor of that range, not a meaningful "neutral" value — chosen only so an unset
     * {@code Character} doesn't silently read as an out-of-range 0). Nothing in this core
     * currently validates a value actually stays within 1-10, same restraint already applied
     * to {@code AttributeValue#base}/{@code CharacterSkill}'s Graduação elsewhere.
     */
    @Builder.Default
    protected int tendencia = 6;

    @NonNull
    protected CharacterAttributes attributes;

    @NonNull
    protected CharacterEgos egos;

    /**
     * The Vantagem de Ego chosen at creation for each {@link EgoDomain} whose eligibility
     * threshold was reached (see {@link
     * org.aventyrs.core.character.services.CharacterCreationService#isEgoAdvantageAvailable}),
     * keyed by domain for O(1) lookup — mirrors {@link #skills}' shape rather than growing one
     * more nullable field per domain (an earlier version had separate {@code
     * autocontroleAdvantage}/{@code initiativeAdvantage} fields; a domain with no eligible or
     * chosen Vantagem is simply absent from this map, not a {@code null} value inside it). Use
     * {@link #getEgoAdvantage(EgoDomain)} rather than indexing this map directly.
     */
    @NonNull
    @Singular
    protected Map<EgoDomain, EgoAdvantage> egoAdvantages;

    /** Trained Perícias, keyed by {@link SkillType} for O(1) lookup instead of filtering a list. */
    @NonNull
    @Singular
    protected Map<SkillType, CharacterSkill> skills;

    @NonNull
    @Singular
    protected List<AttributeAbility> attributeAbilities;

    /**
     * Every {@link ActiveAbility} this character has acquired from an {@link AttributeAbility}
     * — e.g. {@code FocusAbility#CONCENTRACAO_PROFUNDA}'s own activatable state, copied here the
     * moment that ability is acquired via {@link AttributeAbility#resolveActiveAbility()}.
     * Distinct from {@link #attributeAbilities}/{@link #skillCompetencyAbilities}: those are
     * always-on, this is something the holder must actively spend resources to trigger.
     *
     * <p>A Talento-granted Poder Vampírico is <b>not</b> copied here — it is surfaced live by
     * {@link #getActiveAbilities()} aggregating {@link #feats}. Read that method, never this
     * field, to get "every {@code ActiveAbility} this character can trigger".
     */
    @NonNull
    @Singular
    @Getter(AccessLevel.NONE)
    protected List<ActiveAbility> activeAbilities;

    /**
     * Every {@link ActiveAbility} this character can trigger — the {@link #activeAbilities}
     * copied over from an {@code AttributeAbility} at acquisition, plus every held Talento's own
     * {@link Feat#resolveActiveAbility()} (a Poder Vampírico). {@code
     * ActiveAbilityService#activate} identifies a held ability by reference, which is why a
     * {@code Feat} overriding {@code resolveActiveAbility} must return a stable singleton.
     */
    public List<ActiveAbility> getActiveAbilities() {
        return Stream.concat(
                        activeAbilities.stream(),
                        feats.stream().flatMap(feat -> feat.resolveActiveAbility().stream()))
                .toList();
    }

    /** Habilidades de Competência acquired from trained Perícias (e.g. ArtesCompetencyAbility). */
    @NonNull
    @Singular
    protected List<SkillCompetencyAbility> skillCompetencyAbilities;

    /**
     * Values chosen when acquiring an ability whose rules require picking one — e.g. which
     * Perícia {@code GnoseAbility.PERITO_TEORICO} applies to. The ability instance itself still lives
     * in {@link #attributeAbilities}/{@link #skillCompetencyAbilities} as normal — this is
     * purely the extra "what did they pick" data, looked up via
     * {@link org.aventyrs.core.character.services.AbilityChoiceService#getChoiceFor}.
     */
    @NonNull
    @Singular
    protected List<AcquiredChoice<?>> abilityChoices;

    /**
     * Feats acquired via {@link org.aventyrs.core.character.services.FeatService#grantFeat} —
     * unlike {@link #skillCompetencyAbilities}/{@link #attributeAbilities}'s {@code @Singular}
     * shape (fixed at creation, through the builder only), this is a real mutable list: a Feat
     * is acquired well after a character is created, spending XP, the same
     * Character-progression shape as {@link #grantTitle}/{@link #selectCentelhaSuperior} — see
     * {@link #grantFeat(Feat)}. Defaults to a fresh, empty, mutable list when built through the
     * normal Lombok builder (a new instance per {@code build()} call, so no aliasing across
     * separate Characters) — but {@code CharacterFixture} bypasses that builder entirely and
     * defaults this to an immutable {@code List.of()} instead, same as every other trait list
     * there; a test that needs to grant a Feat onto a fixture-built Character must first swap
     * in a fresh mutable list via {@code .toBuilder().feats(new ArrayList<>()).build()}.
     */
    @NonNull
    @Builder.Default
    protected List<Feat> feats = new ArrayList<>();

    /**
     * Magias acquired via {@link org.aventyrs.core.character.services.SpellService#grantSpell} —
     * the same real-mutable-list shape as {@link #feats}, and for the same reason: a Magia is
     * learned well after a character is created, by climbing an Árvore de Magia, not chosen at
     * creation through the builder. Granted via {@link #grantSpell(Spell)}.
     *
     * <p>Carries the identical {@code CharacterFixture} caveat {@link #feats} documents: the
     * fixture bypasses the Lombok builder, so this defaults to an immutable {@code List.of()}
     * there. A test granting a Magia onto a fixture-built Character must first swap in a fresh
     * mutable list via {@code .toBuilder().spells(new ArrayList<>()).build()}.
     */
    @NonNull
    @Builder.Default
    protected List<Spell> spells = new ArrayList<>();

    /**
     * The Itens this character currently has equipped — the same real-mutable-list shape as
     * {@link #feats}, and for the same reason: equipment is picked up, worn and dropped long
     * after a character exists, so it can't be a {@code @Singular} builder-only list. Granted
     * via {@link #equip(Item)}/{@link #unequip(Item)}.
     *
     * <p>A catalog entry ({@code org.aventyrs.core.item.ItemTemplate}) may be equipped directly, in which case every
     * copy of an Armadura Completa reads identically and the enum constant itself is what's held
     * — equipping the same constant twice therefore genuinely means "wearing two of them", and
     * both contribute. A forged {@code org.aventyrs.core.item.AbstractItem} carries per-copy state instead: its own
     * damage ({@link Item#applyDamage}), Obra-Prima and Aprimoramento. Who produced it is still
     * unmodeled — see {@link Item}'s own javadoc. A copy reduced to 0 PV stays in this list as
     * garbage and simply grants nothing ({@link Item#isDestroyed()}).
     *
     * <p>Read by {@code org.aventyrs.core.character.services.DefenseService} (an item's DF/DM
     * columns plus its Favor's {@code DEFESAS}/{@code PHYSICAL_DEFENSE}/{@code MAGIC_DEFENSE}
     * bonuses) and by {@code DamageService#getTotalDamageReduction} (a Favor's {@code
     * DAMAGE_REDUCTION}), which is what finally makes {@code
     * org.aventyrs.core.item.ArmorItem}'s Favores real rather than data with no consumer.
     *
     * <p>Same fixture caveat as {@link #feats}: {@code CharacterFixture} bypasses the Lombok
     * builder and defaults this to an immutable {@code List.of()}, so a test that equips
     * something must first swap in a fresh mutable list via {@code .toBuilder().equipment(new
     * ArrayList<>()).build()}.
     */
    @NonNull
    @Builder.Default
    protected List<Item> equipment = new ArrayList<>();

    /**
     * The {@link Weapon}s this character currently has <b>in hand</b> — drawn, as opposed to
     * merely carried. Always a subset of {@link #equipment}: you cannot draw what you are not
     * carrying, which {@link #drawWeapon(Weapon)} enforces.
     *
     * <p><b>"Utilizando uma arma" means drawn, not equipped.</b> A sword sheathed on your belt is
     * in {@code equipment} and is not being used — which is what lets {@code
     * ArtesMarciaisFeat#DEFESA_DE_MAOS_LIMPAS} pay out to a martial artist who owns a blade but
     * is fighting bare-handed, and what {@code AssassinoFeat#SAQUE_RAPIDO}'s "sacar uma arma"
     * changes.
     *
     * <p><b>No hands.</b> Drawing is tracked as a plain set of weapons, not as hand slots: no
     * entry in {@code docs/rules/equipamentos.txt} states how many hands a weapon needs, so a
     * two-handed weapon would have nothing to declare itself with. Every Talento reading this
     * today asks only "is anything drawn" / "is this drawn", never *which* hand. Hand slots are
     * the natural next step once the catalogue carries handedness.
     *
     * <p>Same fixture caveat as {@link #equipment}.
     */
    @NonNull
    @Builder.Default
    protected List<Weapon> drawnWeapons = new ArrayList<>();

    /**
     * The Título Aventyr in this character's Título Primário slot, or {@code null} if none —
     * same "nullable, no default" shape as {@link #sexo}/{@link #deity}. Unlike an earlier
     * version of this design (a single {@code List<AventyrTitle> titles} field, with each held
     * Título self-reporting whether it was "the" primary one), a character holds **exactly
     * three** Título slots — Primário/Secundário/Terciário, {@link #secondaryTitle}/
     * {@link #tertiaryTitle} below — and which slot a Título occupies is a fact about the
     * *Character*, not the Título instance: it's now structurally impossible to have two
     * "primary" Títulos, rather than an unenforced invariant. Set via {@link
     * #grantTitle(AventyrTitle, TitleSlot)}, mirroring {@code CharacterSkill#increaseGraduation}'s
     * own plain-mutator shape (a Título costs no XP and needs no {@code CombatantSheet}, so —
     * unlike {@code CharacterAttributeService#upgradeBase} — there's no reason to route it
     * through a dedicated service that returns a new value either). See CLAUDE.md's "Adding a
     * new Título" section for the full rationale.
     */
    protected AventyrTitle primaryTitle;

    /** The Título Aventyr in this character's Título Secundário slot, or {@code null} if none — see {@link #primaryTitle}. */
    protected AventyrTitle secondaryTitle;

    /** The Título Aventyr in this character's Título Terciário slot, or {@code null} if none — see {@link #primaryTitle}. */
    protected AventyrTitle tertiaryTitle;

    /**
     * Whether this character has already spent {@code InstinctAbility#CENTELHA_SUPERIOR}'s own
     * one-time "uma Suprema adicional" grant — a plain flag, not a reference to which Título
     * received it (that's already recoverable, if ever needed, by scanning {@link
     * #getAllTitles()}'s own held abilities). {@code false} by default, same shape as {@link
     * #reactions}/{@link #freeActions}'s own {@code @Builder.Default} fields. A real stored
     * flag, not something derived purely by counting each held Título's own Supremas — once a
     * Título that started with none receives its first Suprema, a plain count can no longer
     * tell "that was the normal base allotment" apart from "that was CENTELHA_SUPERIOR's own
     * extra," so {@code
     * org.aventyrs.core.character.services.TitleAbilityService#getAvailableSupremaSlots}/
     * {@code #grantTitleAbility} both need this explicit marker instead. Set via {@link
     * #selectCentelhaSuperior()}.
     */
    @Builder.Default
    protected boolean centelhaSuperiorSelected = false;

    /**
     * How many Regalias of each {@link RegaliaGrade} this character has successfully forged —
     * the "ter sido bem-sucedido na criação de 3 ou mais Regalias" craft-history count the
     * {@code org.aventyrs.core.feat.ArtificeFeat} ladder gates on. A real stored counter because
     * this core keeps no game-session or action history to derive it from (the same reason {@link
     * #centelhaSuperiorSelected} is a stored flag). Incremented by {@link
     * #recordRegaliaCrafted(RegaliaGrade)}, which {@code
     * org.aventyrs.core.character.services.EquipmentCraftingService#forgeRegalia} calls on a
     * successful forge. Defaults to a fresh mutable map through the Lombok builder; {@code
     * CharacterFixture} bypasses that and leaves it {@link Map#of()} — same caveat as {@link
     * #feats} — so {@link #recordRegaliaCrafted} tolerates an immutable starting map.
     */
    @NonNull
    @Builder.Default
    protected Map<RegaliaGrade, Integer> regaliasCraftedByGrade = new EnumMap<>(RegaliaGrade.class);

    @NonNull
    protected ActionProfile actionProfile;

    /**
     * The character's own fixed Pontos de Ação (PA) counter — what they have when no
     * external influence (abilities/feats' {@link org.aventyrs.core.modifier.ModifierType#ACTION_POINTS}
     * bonus, or the {@link ActionProfile}'s Turn adjustment) applies. Permanent resources
     * that grant or remove PA change this value directly, as opposed to the Turn-scoped
     * adjustments {@link ActionPointsService} layers on top of it.
     */
    @Builder.Default
    protected int actionPoints = ActionPointsService.DEFAULT_ACTION_POINTS;

    /**
     * A temporary PA bonus (or malus), gained and spent much like temporary Ego points —
     * on top of the fixed {@link #actionPoints} counter and any
     * {@link org.aventyrs.core.modifier.ModifierType#ACTION_POINTS} ability/feat bonus.
     * Changing it produces a new {@code Character} via {@link #toBuilder()}, same as every
     * other field here; this project doesn't track session-based recovery for it.
     */
    @Builder.Default
    protected int temporaryActionPointsBonus = 0;

    /**
     * The character's own fixed Multiplicador de Vida, feeding {@code
     * HitPointsService#getMaxHitPoints} (which multiplies it by Vigor's total). Defaults to
     * {@code HitPointsService.DEFAULT_LIFE_MULTIPLIER}, and is editable per character exactly
     * like {@link #manaMultiplier} — a GM house rule, a campaign adjustment, or a monster whose
     * bulk shouldn't be paid for in Vigor. Without this, the only way to make something tanky
     * was to inflate Vigor, which also inflates every Vigor-governed Perícia roll and its
     * Determinação pool; a 200-PV ooze with ordinary reflexes had no representation.
     */
    @Builder.Default
    protected int lifeMultiplier = HitPointsService.DEFAULT_LIFE_MULTIPLIER;

    /**
     * The character's own fixed Multiplicador de Determinação — the {@link #lifeMultiplier}
     * counterpart for {@code DeterminationPointsService#getMaxDeterminationPoints}, added for
     * the same reason and defaulting to that service's own constant.
     */
    @Builder.Default
    protected int determinationMultiplier = DeterminationPointsService.DEFAULT_DETERMINATION_MULTIPLIER;

    @Builder.Default
    protected SizeCategory sizeCategory = SizeCategory.ZERO;

    /**
     * The character's own fixed Reação counter — what they have when no external influence
     * (abilities'/competencies'/excellencies' {@link org.aventyrs.core.modifier.ModifierType#REACTIONS}
     * bonus) applies. {@value ReactionsService#DEFAULT_REACTIONS} by default, lowered to 0 or
     * raised to 2 by some Talentos/Habilidades. See {@link ReactionsService#getTotalReactions}
     * for the fully-modified total.
     */
    @Builder.Default
    protected int reactions = ReactionsService.DEFAULT_REACTIONS;

    /**
     * The character's own fixed Ação Livre counter — what they have when no external
     * influence (abilities'/competencies'/excellencies'
     * {@link org.aventyrs.core.modifier.ModifierType#FREE_ACTIONS} bonus) applies.
     * {@value FreeActionsService#DEFAULT_FREE_ACTIONS} by default. Unlike {@link #reactions},
     * an Ação Livre may be spent on the character's own Turn. See
     * {@link FreeActionsService#getTotalFreeActions} for the fully-modified total.
     */
    @Builder.Default
    protected int freeActions = FreeActionsService.DEFAULT_FREE_ACTIONS;

    /**
     * The character's own fixed Multiplicador de Mana — what it is when no external influence
     * (abilities'/competencies' {@link org.aventyrs.core.modifier.ModifierType#MANA_MULTIPLIER}
     * bonus, e.g. Conexão com o Mana) applies. {@value MagicPointsService#DEFAULT_MANA_MULTIPLIER}
     * by default, but editable per character (a GM house rule, a future racial/campaign
     * adjustment) via {@link #toBuilder()}, same as every other field here. See
     * {@link MagicPointsService#getManaMultiplier} for the fully-modified total.
     */
    @Builder.Default
    protected int manaMultiplier = MagicPointsService.DEFAULT_MANA_MULTIPLIER;

    /**
     * The Vantagem de Ego chosen for domain, or {@code null} if none was chosen (or the
     * character was never eligible) for that domain — mirrors {@link CharacterEgos#getEgo}'s
     * shape for the {@link #egoAdvantages} map.
     */
    public EgoAdvantage getEgoAdvantage(final EgoDomain domain) {
        return egoAdvantages.get(domain);
    }

    /**
     * Grants title into slot — see {@link #primaryTitle}'s own javadoc for why this is a real
     * mutator. Overwrites whatever (if anything) already occupied that slot.
     */
    public void grantTitle(@NonNull final AventyrTitle title, @NonNull final TitleSlot slot) {
        switch (slot) {
            case PRIMARY -> primaryTitle = title;
            case SECONDARY -> secondaryTitle = title;
            case TERTIARY -> tertiaryTitle = title;
        }
    }

    /**
     * Appends feat to this character's held Feats — a plain mutator, same as {@link
     * #grantTitle}: prerequisite validation and XP spending are {@code
     * org.aventyrs.core.character.services.FeatService#grantFeat}'s job, not this method's; it
     * trusts the caller already did both, the same restraint {@link
     * org.aventyrs.core.title.AventyrTitle#grantAbility} applies. Throws {@code
     * UnsupportedOperationException} if {@link #feats} is currently an immutable list (e.g. a
     * fixture-built Character that never swapped in a mutable one — see {@link #feats}'s own
     * javadoc).
     */
    /**
     * Whether this character counts weapon as an <b>Arma Natural</b> — the single view every
     * Arma-Natural clause consults, rather than each testing {@code ItemCategory.NATURAL_WEAPON}
     * for itself.
     *
     * <p>True when the weapon simply is one, <em>or</em> when a held Talento reclassifies it (see
     * {@link Feat#reclassifiesAsNaturalWeapon}) — which is what makes {@code
     * ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA}'s "consideradas Armas Naturais
     * para você" reach the other Talentos its rules text promises it reaches. Being
     * per-character is the whole point: the same dagger is an Arma Natural for its holder and an
     * ordinary blade for anyone else, so this cannot live on {@link org.aventyrs.core.item.Weapon}.
     *
     * <p>{@code null} is false rather than an error — an Ataque Desarmado is not a weapon, and
     * every caller here reaches this from a hook whose weapon parameter is optional.
     */
    public boolean treatsAsNaturalWeapon(final Weapon weapon) {
        if (weapon == null) {
            return false;
        }
        return weapon.getCategory() == ItemCategory.NATURAL_WEAPON
                || feats.stream().anyMatch(feat -> feat.reclassifiesAsNaturalWeapon(weapon, this));
    }

    /**
     * The Armas Naturais ({@link NaturalWeapon}) this character possesses — the parts of its
     * body it can strike with, and what a caller names as the {@link Weapon} when rolling such
     * an attack (there is no possession gate: {@code DamageBaseService} takes the weapon as a
     * parameter, so this is the list a UI offers, not a check the roll enforces).
     *
     * <p>Two sources, deduplicated: held Talentos via {@link Feat#getGrantedNaturalWeapons}
     * ({@code ArmamentoDraconicoFeat}, {@code DraconicoFeat#SOPRO_DE_DRAGAO}, {@code BestialFeat}'s
     * Bovídea/Canina/Felina Heranças) and the {@link Race} via {@link
     * Race#getGrantedNaturalWeapons()} ({@code Vampiro}'s Sangue, Poder e Dependência). {@code
     * NascidoDoDragao}/{@code Feral}/{@code Monstruoso}/{@code HomemFera} name Armas Naturais in
     * their rules text too but are blocked on a form state or a per-sub-race authoring gap.
     */
    public List<NaturalWeapon> getNaturalWeapons() {
        return Stream.concat(
                        feats.stream().flatMap(feat -> feat.getGrantedNaturalWeapons(this).stream()),
                        race == null ? Stream.empty() : race.getGrantedNaturalWeapons().stream())
                .distinct()
                .toList();
    }

    public void grantFeat(@NonNull final Feat feat) {
        feats.add(feat);
    }

    /**
     * Adds spell to this character's known {@link #spells} — a plain mutator, the same shape as
     * {@link #grantFeat}. Every acquisition rule (the level cap, the in-tree climb, the branch
     * lock) is enforced by {@code SpellService#grantSpell}, not here — the usual
     * builders-and-mutators-aren't-gatekeepers restraint this codebase applies everywhere else.
     */
    public void grantSpell(@NonNull final Spell spell) {
        spells.add(spell);
    }

    /**
     * Adds item to this character's equipped {@link #equipment} — a plain mutator, same as
     * {@link #grantFeat}: it validates nothing. Whether the character can actually carry or
     * wield it (Carga doesn't exist in this core), whether they paid its Preço in Pontos de
     * Equipamento (no PE economy exists either), and whether they meet the {@code ItemFavor}'s
     * own Requisitos (that's resolved per-read, live, by {@link Item#grantsFavorTo}) are all
     * outside this method. Throws {@code UnsupportedOperationException} if {@link #equipment} is
     * currently an immutable list — see that field's own javadoc.
     */
    public void equip(@NonNull final Item item) {
        equipment.add(item);
    }

    /**
     * Takes weapon in hand — the "sacar uma arma" of {@code AssassinoFeat#SAQUE_RAPIDO}. Refuses
     * a weapon this character is not carrying, and is idempotent: drawing what is already drawn
     * changes nothing and reports {@code false}.
     *
     * @return whether the weapon was actually drawn by this call
     */
    public boolean drawWeapon(@NonNull final Weapon weapon) {
        if (!equipment.contains(weapon) || drawnWeapons.contains(weapon)) {
            return false;
        }
        return drawnWeapons.add(weapon);
    }

    /**
     * Puts weapon away — "guardar sua arma" ({@code AssassinoFeat#TROCA_DE_ARMA_VELOZ}). It stays
     * in {@link #equipment}; only the hand is freed.
     *
     * @return whether the weapon was actually drawn before this call
     */
    public boolean sheatheWeapon(@NonNull final Weapon weapon) {
        return drawnWeapons.remove(weapon);
    }

    /** Whether weapon is currently in hand. */
    public boolean isDrawn(final Weapon weapon) {
        return weapon != null && drawnWeapons.contains(weapon);
    }

    /** Whether this character has any weapon in hand — "está utilizando uma arma". */
    public boolean isWieldingAWeapon() {
        return !drawnWeapons.isEmpty();
    }

    /**
     * Removes one occurrence of item from {@link #equipment}, returning whether anything was
     * actually removed. The mirror of {@link #equip(Item)}, and equally unvalidating.
     */
    public boolean unequip(@NonNull final Item item) {
        // A weapon that leaves the character's possession cannot still be in their hand. Guarded
        // by contains() so an un-drawn weapon never touches the list — CharacterFixture defaults
        // drawnWeapons to an immutable List.of(), which a blind remove() would blow up on.
        if (item instanceof Weapon weapon && drawnWeapons.contains(weapon)) {
            drawnWeapons.remove(weapon);
        }
        return equipment.remove(item);
    }

    /**
     * Marks {@code InstinctAbility#CENTELHA_SUPERIOR}'s one-time extra Suprema grant as spent —
     * see {@link #centelhaSuperiorSelected}'s own javadoc.
     */
    public void selectCentelhaSuperior() {
        this.centelhaSuperiorSelected = true;
    }

    /**
     * Whether this character has a Regalia of at least minimumGrade among their {@link
     * #equipment} — the <b>use</b>-condition every {@code ArtificeFeat} names ("Após estudar
     * incansavelmente a Regalia em sua posse …"), read by {@code Feat#itsAllowedToCraftRegalia}.
     * Grades compare by rank ({@code MENOR < SUPERIOR < DIVINA}), so {@link RegaliaGrade#MENOR}
     * asks only "any Regalia at all".
     *
     * <p><b>Not an acquisition prerequisite.</b> Studying a Regalia is what the Talento's rules
     * text describes doing <i>with</i> it; nothing there gates learning the Talento on still
     * owning one. That is why this is consulted at forge time and not by {@code Feat#isEligible}
     * — a crafter who sells the Regalia they trained on keeps the Talento and simply cannot use
     * it until they hold one again.
     *
     * <p>Scans the equipped list rather than a full inventory for the usual reason: a {@code
     * Character} has no inventory — that lives on {@code AbstractCombatantSheet} — so a Regalia
     * carried in a pack is invisible here, the same simplification every other equipment scan in
     * this core makes.
     */
    public boolean possessesRegalia(@NonNull final RegaliaGrade minimumGrade) {
        return equipment.stream()
                .map(Item::getRegaliaGrade)
                .anyMatch(grade -> grade != null && grade.compareTo(minimumGrade) >= 0);
    }

    /** How many Regalias of grade this character has successfully forged so far. */
    public int getRegaliasCrafted(@NonNull final RegaliaGrade grade) {
        return regaliasCraftedByGrade == null ? 0 : regaliasCraftedByGrade.getOrDefault(grade, 0);
    }

    /**
     * Records one more successfully-forged Regalia of grade — the mutator {@code
     * EquipmentCraftingService#forgeRegalia} calls after a successful craft. Swaps in a mutable
     * map first if the character was built through {@code CharacterFixture} (which leaves the
     * field {@link Map#of()}), the same defensive copy {@link #grantFeat} does not need only
     * because its list is seeded mutable by the real builder.
     */
    public void recordRegaliaCrafted(@NonNull final RegaliaGrade grade) {
        if (!(regaliasCraftedByGrade instanceof EnumMap)) {
            Map<RegaliaGrade, Integer> mutable = new EnumMap<>(RegaliaGrade.class);
            if (regaliasCraftedByGrade != null) {
                mutable.putAll(regaliasCraftedByGrade);
            }
            regaliasCraftedByGrade = mutable;
        }
        regaliasCraftedByGrade.merge(grade, 1, Integer::sum);
    }

    /**
     * Every Título Aventyr slot this character has actually filled, Primário first — e.g. for a
     * caller that needs to scan every held Título's own abilities regardless of which slot each
     * occupies (see {@code DamageServiceImpl}'s own Título-ability scan). Empty slots are
     * simply omitted, never a {@code null} entry.
     */
    public List<AventyrTitle> getAllTitles() {
        return Stream.of(primaryTitle, secondaryTitle, tertiaryTitle)
                .filter(Objects::nonNull)
                .toList();
    }

    public enum Sexo {
        MASCULINO,
        FEMININO
    }
}
