package org.aventyrs.core.modifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifierResolverImpl implements ModifierResolver {

    /**
     * Reflection is only ever done once per concrete class (enum constants with a body
     * compile to their own anonymous subclass, so each one is scanned independently);
     * every later lookup for that class and type is a cache hit.
     */
    private final Map<Class<?>, Map<ModifierType, List<Method>>> methodsByClass = new HashMap<>();

    @Override
    public int sumModifiers(final Object source, final ModifierType type) {
        return methodsFor(source.getClass(), type).stream()
                .mapToInt(method -> invoke(source, method))
                .sum();
    }

    @Override
    public int sumModifiers(final Collection<?> sources, final ModifierType type) {
        return sources.stream()
                .mapToInt(source -> sumModifiers(source, type))
                .sum();
    }

    private List<Method> methodsFor(final Class<?> sourceClass, final ModifierType type) {
        return methodsByClass
                .computeIfAbsent(sourceClass, this::indexModifierMethods)
                .getOrDefault(type, List.of());
    }

    private Map<ModifierType, List<Method>> indexModifierMethods(final Class<?> sourceClass) {
        Map<ModifierType, List<Method>> byType = new EnumMap<>(ModifierType.class);
        for (Method method : sourceClass.getDeclaredMethods()) {
            Modifier annotation = method.getAnnotation(Modifier.class);
            if (annotation != null) {
                method.setAccessible(true);
                byType.computeIfAbsent(annotation.value(), key -> new ArrayList<>()).add(method);
            }
        }
        return byType;
    }

    private int invoke(final Object source, final Method method) {
        try {
            return (int) method.invoke(source);
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new ModifierResolutionException(e);
        }
    }
}
