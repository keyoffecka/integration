package net.ofk.integration.cdi.jpa;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import java.util.Map;

/**
 * @author Konstantin I. key.offecka@runbox.com
 */
public class DelegatingEntityManagerTest {
  private DelegatingEntityManager em;
  private EntityManagerFactoryStore store;
  private Map<Thread, EntityManager> ems;

  @Before
  public void setUp() {
    this.ems = Maps.newHashMap();
    this.store = Mockito.mock(EntityManagerFactoryStore.class);
    this.em = Mockito.spy(new DelegatingEntityManager("testPU", this.store));
    Mockito.doReturn(this.ems).when(this.em).getEMS();
  }

  @Test
  public void testClose() {
    Thread t1 = Mockito.mock(Thread.class);
    Thread t2 = Mockito.mock(Thread.class);

    EntityManager em1 = Mockito.mock(EntityManager.class);
    EntityManager em2 = Mockito.mock(EntityManager.class);

    this.ems.put(t1, em1);
    this.ems.put(t2, em2);

    this.em.close();

    Assert.assertTrue(this.ems.isEmpty());

    Mockito.verify(this.store, Mockito.times(2)).release(Matchers.anyString(), Matchers.anyObject());
    Mockito.verify(this.store).release("testPU", t1);
    Mockito.verify(this.store).release("testPU", t2);
  }

  @Test
  public void testGetEM() {
    Thread t1 = Mockito.mock(Thread.class);
    Thread t2 = Mockito.mock(Thread.class);

    EntityManager em1 = Mockito.mock(EntityManager.class);
    EntityManager em2 = Mockito.mock(EntityManager.class);

    this.ems.put(t1, em1);

    Mockito.doReturn(t2).when(this.em).getCurrentThread();
    Mockito.doReturn(em2).when(this.store).acquire("testPU", t2);

    EntityManager result = this.em.getEM();

    Assert.assertEquals(em2, result);
    Assert.assertEquals(ImmutableMap.of(t1, em1, t2, em2), this.ems);
  }

  @Test
  public void testGetExistingEM() {
    Thread t1 = Mockito.mock(Thread.class);
    Thread t2 = Mockito.mock(Thread.class);

    EntityManager em1 = Mockito.mock(EntityManager.class);
    EntityManager em2 = Mockito.mock(EntityManager.class);

    this.ems.put(t1, em1);
    this.ems.put(t2, em2);

    Mockito.doReturn(t2).when(this.em).getCurrentThread();

    EntityManager result = this.em.getEM();

    Assert.assertEquals(em2, result);

    Mockito.verify(this.store, Mockito.never()).acquire(Matchers.anyString(), Matchers.anyObject());
  }
}
