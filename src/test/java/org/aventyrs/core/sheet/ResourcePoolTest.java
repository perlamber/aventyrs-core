package org.aventyrs.core.sheet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourcePoolTest {

    @Test
    void startsEmpty() {
        assertEquals(0, new ResourcePool().getSpent());
    }

    @Test
    void spendAccumulates() {
        ResourcePool pool = new ResourcePool();
        pool.spend(3);
        assertEquals(5, pool.spend(2));
    }

    @Test
    void recoverReducesSpent() {
        ResourcePool pool = new ResourcePool();
        pool.spend(5);
        assertEquals(2, pool.recover(3));
    }

    @Test
    void recoverNeverGoesBelowZero() {
        ResourcePool pool = new ResourcePool();
        pool.spend(2);
        assertEquals(0, pool.recover(10));
    }
}
