package org.aventyrs.core.sheet;

import java.util.function.Supplier;

public class IllegalOperationException extends IllegalStateException {

    public IllegalOperationException(final String message)
    {
        super(message);
    }
}
