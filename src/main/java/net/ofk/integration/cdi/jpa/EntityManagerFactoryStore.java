package net.ofk.integration.cdi.jpa;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;

/**
 * Stores instances of entity managers.
 * Every created entity manager is associated with its entity manager factory
 * which is actually used to create an instance of the entity manager
 * and with the thread in which the new instance was acquired.
 * This associations allow to re-use an instance if it's called within the same thread
 * and uses the same persistence unit.
 *
 * @author Konstantin I. key.offecka@runbox.com
 */
@Named
@ApplicationScoped
public class EntityManagerFactoryStore {
  private static final Logger LOG = LoggerFactory.getLogger(EntityManagerFactoryStore.class);

  private static final String DEFAULT_PU_NAME = "default";

  private final Map<String, EntityManagerFactory> emfs = Maps.newHashMap();
  Map<String, EntityManagerFactory> getEMFs() {return this.emfs;}

  private final Map<String, Map<Thread, EntityManagerFactoryStore.Context>> contextMap = Maps.newHashMap();
  Map<String, Map<Thread, EntityManagerFactoryStore.Context>> getContextMap() {return this.contextMap;}

  /**
   * Returns a printable name of the persistence unit.
   * Printable name is the trimmed name of the persistence unit.
   * If the persistence unit name is null the default name is returned.
   *
   * @param puName - original name of the persistence unit.
   * @return printable name of the persistence unit.
   */
  public static String getPUName(final String puName) {
    String trimmedPUName = Strings.nullToEmpty(puName).trim();
    String result = trimmedPUName.isEmpty() ? EntityManagerFactoryStore.DEFAULT_PU_NAME : trimmedPUName;
    return result;
  }

  /**
   * Returns a persistence manager factory of the persistence unit.
   * If the factory has already bean created by the store the cached instance is returned.
   *
   * @param puName - name of the persistence unit which entity manager factory to create.
   * @return entity manager factory.
   */
  EntityManagerFactory getEMF(final String puName) {
    EntityManagerFactory emf = null;

    synchronized (this.getEMFs()) {
      emf = this.getEMFs().get(puName);
      if (emf == null) {
        emf = this.createEMF(puName);
        this.getEMFs().put(puName, emf);

        EntityManagerFactoryStore.LOG.debug("Created a new entity manager factory of the {} persistence unit.", EntityManagerFactoryStore.getPUName(puName));
      }
    }

    return emf;
  }

  /**
   * Creates an entity manager factory.
   * Needed fo testing.
   *
   * @param puName - name of the persistence context.
   * @return new instance of the persistence factory.
   */
  EntityManagerFactory createEMF(final String puName) {
    return Persistence.createEntityManagerFactory(puName);
  }

  /**
   * Returns an entity manager of the persistence unit associated with the given thread.
   * If there is no such instance a new one is created and registered.
   *
   * @param puName - name of the persistence unit of the entity manager.
   * @param thread - thread where the entity manger will be used.
   * @return entity manager instance.
   */
  public EntityManager acquire(final String puName, final Thread thread) {
    EntityManager em = null;

    synchronized (this.getContextMap()) {
      Map<Thread, EntityManagerFactoryStore.Context> contexts = this.getContextMap().get(puName);
      if (contexts == null) {
        EntityManagerFactoryStore.LOG.debug("Entity managers of the {} persistence unit do not exist.", EntityManagerFactoryStore.getPUName(puName));

        contexts = Maps.newHashMap();
        this.getContextMap().put(puName, contexts);
      }
      EntityManagerFactoryStore.Context context = contexts.get(thread);
      if (context == null) {
        EntityManagerFactory emf = this.getEMF(puName);
        em = emf.createEntityManager();

        context = new EntityManagerFactoryStore.Context(em);
        contexts.put(thread, context);

        EntityManagerFactoryStore.LOG.debug("Registered the new entity manager of the {} persistence unit.", EntityManagerFactoryStore.getPUName(puName));
      } else {
        context.inc();

        em = context.getEM();

        EntityManagerFactoryStore.LOG.debug("Found the registered entity manager of the {} persistence unit.", EntityManagerFactoryStore.getPUName(puName));
      }
    }

    return em;
  }

  /**
   * Unregisters the entity manager of the persistence unit associated with the given thread.
   * The unregistered entity manager will be closed and should be used after later.
   *
   * @param puName - name of the persistence unit of the entity manager.
   * @param thread - thread associated with the entity manager.
   */
  public void release(final String puName, final Thread thread) {
    synchronized (this.getContextMap()) {
      Map<Thread, EntityManagerFactoryStore.Context> contexts = this.getContextMap().get(puName);
      if (contexts == null) {
        EntityManagerFactoryStore.LOG.warn("Entity managers of the {} persistence unit were not created.", EntityManagerFactoryStore.getPUName(puName));
      } else {
        EntityManagerFactoryStore.Context context = contexts.get(thread);
        if (context == null) {
          EntityManagerFactoryStore.LOG.warn("Entity manager of the {} persistence unit was not created.", EntityManagerFactoryStore.getPUName(puName));
        } else {
          context.dec();

          if (context.getCount() == 0) {
            context.getEM().close();
            contexts.remove(thread);

            if (contexts.isEmpty()) {
              this.getContextMap().remove(puName);
            }
            EntityManagerFactoryStore.LOG.debug("Released the entity manager of the {} persistence unit.", EntityManagerFactoryStore.getPUName(puName));
          } else {
            EntityManagerFactoryStore.LOG.debug("Didn't release the entity manager, there are other references to the entity manager of the {} persistence unit.", EntityManagerFactoryStore.getPUName(puName));
          }
        }
      }
    }
  }

  /**
   * Holds an entity manager and the number of how many times
   * the manager was acquired.
   */
  static class Context {
    private final EntityManager em;
    public EntityManager getEM() {return this.em;}

    private long count;
    public long getCount() {return this.count;}

    Context(final EntityManager em) {
      this(em, 1);
    }

    Context(final EntityManager em, final long count) {
      Preconditions.checkArgument(em != null);

      this.em = em;
      this.count = count;
    }

    public void inc() {
      this.count+= 1;
    }

    public void dec() {
      this.count-= 1;
    }

    @Override
    public boolean equals(final Object object) {
      boolean result = false;

      if (object != null && object.getClass() == this.getClass()) {
        EntityManagerFactoryStore.Context ctx = (EntityManagerFactoryStore.Context) object;

        result = this.getCount() == ctx.getCount() && this.getEM().equals(ctx.getEM());
      }

      return result;
    }

    @Override
    public int hashCode() {
      int result = Long.valueOf(this.getCount()).hashCode() ^ this.getEM().hashCode();
      return result;
    }
  }
}
