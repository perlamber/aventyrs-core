package org.aventyrs.core.feat;

/** A Talento's contribution to whether its holder may acquire one named Título. */
public enum TitleAcquisitionPermission {
    NO_OPINION,
    PROHIBIT,
    ALLOW;

    /**
     * Combines independent Talento permissions. An explicit allowance lifts a racial prohibition,
     * while an unrelated Talento has no effect.
     */
    public TitleAcquisitionPermission merge(final TitleAcquisitionPermission other) {
        if (this == ALLOW || other == ALLOW) {
            return ALLOW;
        }
        if (this == PROHIBIT || other == PROHIBIT) {
            return PROHIBIT;
        }
        return NO_OPINION;
    }
}
