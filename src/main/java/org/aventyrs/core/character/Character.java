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
import org.aventyrs.core.character.services.FreeActionsService;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.ego.EgoAdvantage;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitle;

import java.util.ArrayList;
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
     * {@link org.aventyrs.core.sheet.CharacterSheet} wrapping it (see that class's own
     * {@code id}), e.g. so {@link org.aventyrs.core.scene.Scene} can tell participants apart
     * without relying on object-reference equality.
     */
    @Builder.Default
    protected UUID id = UUID.randomUUID();

    @NonNull
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
     * Every {@link ActiveAbility} this character has acquired — e.g. {@code
     * FocusAbility#CONCENTRACAO_PROFUNDA}'s own activatable state, granted here the moment
     * that ability is acquired via {@link AttributeAbility#resolveActiveAbility()}. Distinct
     * from {@link #attributeAbilities}/{@link #skillCompetencyAbilities}: those are always-on,
     * this is something the holder must actively spend resources to trigger.
     */
    @NonNull
    @Singular
    protected List<ActiveAbility> activeAbilities;

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
     * The Título Aventyr in this character's Título Primário slot, or {@code null} if none —
     * same "nullable, no default" shape as {@link #sexo}/{@link #deity}. Unlike an earlier
     * version of this design (a single {@code List<AventyrTitle> titles} field, with each held
     * Título self-reporting whether it was "the" primary one), a character holds **exactly
     * three** Título slots — Primário/Secundário/Terciário, {@link #secondaryTitle}/
     * {@link #tertiaryTitle} below — and which slot a Título occupies is a fact about the
     * *Character*, not the Título instance: it's now structurally impossible to have two
     * "primary" Títulos, rather than an unenforced invariant. Set via {@link
     * #grantTitle(AventyrTitle, TitleSlot)}, mirroring {@code CharacterSkill#increaseGraduation}'s
     * own plain-mutator shape (a Título costs no XP and needs no {@code CharacterSheet}, so —
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

    @Builder.Default
    protected SizeCategory sizeCategory = SizeCategory.ZERO;

    /**
     * How badly hurt this character currently is — the {@link CharacterStatus} tier their
     * current Hit Points fall into (see {@code HitPointsService#getStatus}). Kept in sync with
     * damage taken by {@code DamageService#applyDamage}/{@code DamageService#refreshStatus},
     * which recompute it from the damaged {@code CharacterSheet} and store it here via
     * {@link #updateStatus(CharacterStatus)}. It's a stored field rather than something
     * derived on read because a {@code Character} has no {@code CharacterSheet} of its own to
     * derive it from (damage taken lives on the sheet, and one Character could back more than
     * one sheet) — the same reason {@code ReactionsService}/{@code InitiativeService} compute
     * their own totals in a service rather than on this data class.
     *
     * <p>Damage applied straight to a {@code CharacterSheet} without going through {@code
     * DamageService} — {@code Sangramento}/{@code Bleeding}/{@code Withering}'s own per-Rodada
     * loss, {@code RealExecution}'s curse damage, and {@link
     * org.aventyrs.core.sheet.CharacterSheet#heal} — still leaves this stale; those paths hold
     * no {@code DamageService} to refresh through, and {@code RealExecution} already computes
     * its own {@code resultStatus} for real instead of reading this field.
     */
    @Builder.Default
    CharacterStatus status = CharacterStatus.CLEAN;

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
    public void grantFeat(@NonNull final Feat feat) {
        feats.add(feat);
    }

    /**
     * Sets this character's current {@link CharacterStatus} — a plain mutator, same as {@link
     * #grantTitle}/{@link #grantFeat}: resolving *which* tier the character is actually in is
     * {@code DamageService#refreshStatus}'s job (via {@code HitPointsService#getStatus}), not
     * this method's; it trusts the caller already did.
     */
    public void updateStatus(@NonNull final CharacterStatus status) {
        this.status = status;
    }

    /**
     * Marks {@code InstinctAbility#CENTELHA_SUPERIOR}'s one-time extra Suprema grant as spent —
     * see {@link #centelhaSuperiorSelected}'s own javadoc.
     */
    public void selectCentelhaSuperior() {
        this.centelhaSuperiorSelected = true;
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
