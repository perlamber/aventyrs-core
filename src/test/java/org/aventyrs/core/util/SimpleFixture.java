package org.aventyrs.core.util;

import br.com.six2six.fixturefactory.Fixture;

public abstract class SimpleFixture {

    /**
     *  Method to get a new object from the given template, when using on a
     * @param templateName
     * @param as
     * @param <T>
     * @return
     */
    public static <T> T of(final String templateName, final Class<T> as) {
        return Fixture.from(as).gimme(templateName);
    }

}
