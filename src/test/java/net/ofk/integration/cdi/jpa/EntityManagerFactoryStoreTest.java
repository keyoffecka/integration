package net.ofk.integration.cdi.jpa;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Map;

/**
 * @author Konstantin I. key.offecka@runbox.com
 */
public class EntityManagerFactoryStoreTest {
  private EntityManagerFactoryStore store;
  private Map<String, EntityManagerFactory> emfs;
  private Map<String, Map<Thread, EntityManagerFactoryStore.Context>> map;

  @Before
  public void setUp() {
    this.map = Maps.newHashMap();
    this.emfs = Maps.newHashMap();
    this.store = Mockito.spy(new EntityManagerFactoryStore());
    Mockito.doReturn(this.emfs).when(this.store).getEMFs();
    Mockito.doReturn(this.map).when(this.store).getContextMap();
  }

  @Test
  public void testGetEMF() {
    EntityManagerFactory emf1 = Mockito.mock(EntityManagerFactory.class);
    EntityManagerFactory emf2 = Mockito.mock(EntityManagerFactory.class);

    this.emfs.put("pu", emf1);

    Mockito.doReturn(emf2).when(this.store).createEMF("testPU");

    EntityManagerFactory result = this.store.getEMF("testPU");

    Assert.assertEquals(emf2, result);
    Assert.assertEquals(ImmutableMap.of("pu", emf1, "testPU", emf2), this.emfs);
  }

  @Test
  public void testGetExistingEMF() {
    EntityManagerFactory emf1 = Mockito.mock(EntityManagerFactory.class);
    EntityManagerFactory emf2 = Mockito.mock(EntityManagerFactory.class);

    this.emfs.put("pu", emf1);
    this.emfs.put("testPU", emf2);

    EntityManagerFactory result = this.store.getEMF("testPU");

    Assert.assertEquals(emf2, result);
    Assert.assertEquals(ImmutableMap.of("pu", emf1, "testPU", emf2), this.emfs);

    Mockito.verify(this.store, Mockito.never()).createEMF(Matchers.anyString());
  }

  @Test
  public void testAcquire() {
    Thread thread = Mockito.mock(Thread.class);
    EntityManagerFactory emf = Mockito.mock(EntityManagerFactory.class);
    EntityManager em = Mockito.mock(EntityManager.class);

    Mockito.doReturn(emf).when(this.store).getEMF("testPU");
    Mockito.doReturn(em).when(emf).createEntityManager();

    EntityManager result = this.store.acquire("testPU", thread);

    Assert.assertEquals(em, result);

    Assert.assertEquals(ImmutableMap.of("testPU", ImmutableMap.of(thread, new EntityManagerFactoryStore.Context(em))), this.map);
  }

  @Test
  public void testAcquireWithNotEmptyMap() {
    Thread thread = Mockito.mock(Thread.class);
    EntityManagerFactory emf = Mockito.mock(EntityManagerFactory.class);
    EntityManager em = Mockito.mock(EntityManager.class);

    Map<Thread, EntityManagerFactoryStore.Context> contexts = Maps.newHashMap();
    this.map.put("testPU", contexts);

    Mockito.doReturn(emf).when(this.store).getEMF("testPU");
    Mockito.doReturn(em).when(emf).createEntityManager();

    EntityManager result = this.store.acquire("testPU", thread);

    Assert.assertEquals(em, result);

    Assert.assertSame(contexts, this.map.get("testPU"));
    Assert.assertEquals(ImmutableMap.of("testPU", ImmutableMap.of(thread, new EntityManagerFactoryStore.Context(em))), this.map);
  }

  @Test
  public void testAcquireExistingEM() {
    Thread thread = Mockito.mock(Thread.class);
    EntityManagerFactory emf = Mockito.mock(EntityManagerFactory.class);
    EntityManager em = Mockito.mock(EntityManager.class);
    EntityManagerFactoryStore.Context ctx = new EntityManagerFactoryStore.Context(em, 24);

    Map<Thread, EntityManagerFactoryStore.Context> contexts = ImmutableMap.of(thread, ctx);
    this.map.put("testPU", contexts);

    Mockito.doReturn(emf).when(this.store).getEMF("testPU");

    EntityManager result = this.store.acquire("testPU", thread);

    Assert.assertEquals(em, result);

    Assert.assertEquals(ImmutableMap.of("testPU", (Map<Thread, EntityManagerFactoryStore.Context>) ImmutableMap.of(thread, ctx)), this.map);
    Assert.assertSame(ctx, this.map.get("testPU").get(thread));
    Assert.assertEquals(25, ctx.getCount());

    Mockito.verify(emf, Mockito.never()).createEntityManager();
  }

  @Test
  public void testReleaseNotAcquired1() {
    Thread thread = Mockito.mock(Thread.class);

    this.store.release("testPU", thread);

    Assert.assertTrue(this.map.isEmpty());
  }

  @Test
  public void testReleaseNotAcquired2() {
    Thread thread = Mockito.mock(Thread.class);

    Map<Thread, EntityManagerFactoryStore.Context> contexts = Maps.newHashMap();
    this.map.put("testPU", contexts);

    this.store.release("testPU", thread);

    Assert.assertEquals(1, this.map.size());
    Assert.assertSame(contexts, this.map.get("testPU"));
    Assert.assertTrue(contexts.isEmpty());
  }

  @Test
  public void testReleaseAndDec() {
    Thread thread = Mockito.mock(Thread.class);
    EntityManager em = Mockito.mock(EntityManager.class);
    EntityManagerFactoryStore.Context ctx = new EntityManagerFactoryStore.Context(em, 12);

    Map<Thread, EntityManagerFactoryStore.Context> contexts = ImmutableMap.of(thread, ctx);
    this.map.put("testPU", contexts);

    this.store.release("testPU", thread);

    Assert.assertEquals(1, this.map.size());
    Assert.assertSame(contexts, this.map.get("testPU"));
    Assert.assertEquals(1, contexts.size());
    Assert.assertEquals(ctx, contexts.get(thread));
    Assert.assertEquals(new EntityManagerFactoryStore.Context(em, 11), ctx);
  }

  @Test
  public void testReleaseAndClean() {
    Thread thread1 = Mockito.mock(Thread.class);
    Thread thread2 = Mockito.mock(Thread.class);
    EntityManager em1 = Mockito.mock(EntityManager.class);
    EntityManager em2 = Mockito.mock(EntityManager.class);
    EntityManagerFactoryStore.Context ctx1 = new EntityManagerFactoryStore.Context(em1, 1);
    EntityManagerFactoryStore.Context ctx2 = new EntityManagerFactoryStore.Context(em2, 1);

    Map<Thread, EntityManagerFactoryStore.Context> contexts = Maps.newHashMap(ImmutableMap.of(thread1, ctx1, thread2, ctx2));
    this.map.put("testPU", contexts);

    this.store.release("testPU", thread1);

    Mockito.verify(em1).close();

    Assert.assertEquals(1, this.map.size());
    Assert.assertSame(contexts, this.map.get("testPU"));
    Assert.assertEquals(1, contexts.size());
    Assert.assertSame(ctx2, contexts.get(thread2));
  }

  @Test
  public void testReleaseAndCleanAll() {
    Thread thread = Mockito.mock(Thread.class);
    EntityManager em = Mockito.mock(EntityManager.class);
    EntityManagerFactoryStore.Context ctx1 = new EntityManagerFactoryStore.Context(em, 1);

    Map<Thread, EntityManagerFactoryStore.Context> contexts = Maps.newHashMap(ImmutableMap.of(thread, ctx1));
    this.map.put("testPU", contexts);

    this.store.release("testPU", thread);

    Mockito.verify(em).close();

    Assert.assertEquals(0, this.map.size());
  }
}
