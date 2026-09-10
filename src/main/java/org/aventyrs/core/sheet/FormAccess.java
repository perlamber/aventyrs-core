package org.aventyrs.core.sheet;

/**
 * What a held trait has to say about its holder taking a given {@link FormType} — the answer
 * {@code org.aventyrs.core.feat.Feat#resolveFormAccess} returns, combined by {@link
 * CombatantSheet#canTakeForm}.
 *
 * <p>The same three-valued shape {@code Feat#resolveTitleAcquisitionPermission} uses, and for the
 * same reason: with only booleans, "this Talento says nothing" and "this Talento says no" would
 * be the same answer, and a trait that merely doesn't care would silently veto.
 */
public enum FormAccess {

    /** This trait has nothing to say about that shape — the default, and every Talento but two. */
    NO_OPINION,

    /** This trait refuses that shape outright — "não pode acessar a forma monstruosa". */
    FORBIDDEN,

    /**
     * This trait locks its holder into that shape — "está sempre em sua forma monstruosa e é
     * incapaz de alternar". It therefore refuses every <em>other</em> shape, and the holder's own
     * besides, which is what makes it more than a preference.
     */
    REQUIRED
}
