package org.srg.smartclient.jpa;

import org.junit.jupiter.api.Test;

import javax.persistence.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class JpaDSDispatcherTest {

    @Test
    public void test() {
        final EntityManagerFactory emf = JpaTestSupport.createEntityManagerFactory("testPU", SimpleEntity.class);

        EntityManager em = emf.createEntityManager();
        try {
            em.find(SimpleEntity.class, 42);
            assert emf != null;


            final SimpleEntity te1 = em.find(SimpleEntity.class, 42);
            assertNull(te1);

            final SimpleEntity ethalon = new SimpleEntity(42, "Answer to the Ultimate Question of Life, The Universe, and Everything");
            em.persist(ethalon);

            final SimpleEntity te2 = em.find(SimpleEntity.class, 42);
            assertEquals(ethalon.name(), te2.name());

        } finally {
            if(em != null) {
                em.close();
            }
        }

    }
}
