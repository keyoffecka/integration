package net.ofk.integration.cdi.jpa;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;
import java.util.Map;

/**
 * Lazily initializes underlying transaction managers for every thread
 * in which the entity manager is used.
 * This means that if the bean is injected into another bean
 * and the former is used in different threads every thread will have
 * its own underlying entity manager instance.
 * This allows to use the bean in different transactions
 * since every thread has its own associated transaction.
 *
 * @author Konstantin I. key.offecka@runbox.com
 */
public class DelegatingEntityManager implements EntityManager {
  private static final Logger LOG = LoggerFactory.getLogger(DelegatingEntityManager.class);

  private final Map<Thread, EntityManager> ems = Maps.newHashMap();

  private final String puName;
  public String getPUName() {return this.puName;}

  private final EntityManagerFactoryStore store;

  public DelegatingEntityManager(final String puName, final EntityManagerFactoryStore store) {
    this.puName = puName;
    this.store = store;
  }

  /**
   * If there is an underlying bean associated with the current thread that bean is returned,
   * otherwise a new entity manager is created and registered within the thread.
   *
   * @return underlying entity manager instance.
   */
  EntityManager getEM() {
    EntityManager em = null;

    synchronized(this.ems) {
      Thread thread = Thread.currentThread();
      em = this.ems.get(thread);
      if (em == null) {
        DelegatingEntityManager.LOG.debug("Acquiring a new entity manager of the {} persistence unit.", EntityManagerFactoryStore.getPUName(this.puName));

        em = this.store.acquire(this.puName, thread);
        this.ems.put(thread, em);
      } else {
        DelegatingEntityManager.LOG.debug("Found an entity manager of the {} persistence unit.", EntityManagerFactoryStore.getPUName(this.puName));
      }
    }

    return em;
  }

  /**
   * Closes all underlying entity managers.
   * Should be called within the bean which has produced this delegating entity manager.
   */
  @Override
  public void close() {
    synchronized (this.ems) {
      this.ems.forEach((thread, em) -> this.store.release(this.puName, thread));
      this.ems.clear();

      DelegatingEntityManager.LOG.debug("Closed the delegating entity manager of the {} persistence unit.", EntityManagerFactoryStore.getPUName(this.puName));
    }
  }

  @Override
  public void persist(final Object entity) {
    this.getEM().persist(entity);
  }

  @Override
  public <T> T merge(final T entity) {
    return this.getEM().merge(entity);
  }

  @Override
  public void remove(final Object entity) {
    this.getEM().remove(entity);
  }

  @Override
  public <T> T find(final Class<T> entityClass, final Object primaryKey) {
    return this.getEM().find(entityClass, primaryKey);
  }

  @Override
  public <T> T find(final Class<T> entityClass, final Object primaryKey, final Map<String, Object> properties) {
    return this.getEM().find(entityClass, primaryKey, properties);
  }

  @Override
  public <T> T find(final Class<T> entityClass, final Object primaryKey, final LockModeType lockMode) {
    return this.getEM().find(entityClass, primaryKey, lockMode);
  }

  @Override
  public <T> T find(final Class<T> entityClass, final Object primaryKey, final LockModeType lockMode, final Map<String, Object> properties) {
    return this.getEM().find(entityClass, primaryKey, lockMode, properties);
  }

  @Override
  public <T> T getReference(final Class<T> entityClass, final Object primaryKey) {
    return this.getEM().getReference(entityClass, primaryKey);
  }

  @Override
  public void flush() {
    this.getEM().flush();
  }

  @Override
  public void setFlushMode(final FlushModeType flushMode) {
    this.getEM().setFlushMode(flushMode);
  }

  @Override
  public FlushModeType getFlushMode() {
    return this.getEM().getFlushMode();
  }

  @Override
  public void lock(final Object entity, final LockModeType lockMode) {
    this.getEM().lock(entity, lockMode);
  }

  @Override
  public void lock(final Object entity, final LockModeType lockMode, final Map<String, Object> properties) {
    this.getEM().lock(entity, lockMode, properties);
  }

  @Override
  public void refresh(final Object entity) {
    this.getEM().refresh(entity);
  }

  @Override
  public void refresh(final Object entity, final Map<String, Object> properties) {
    this.getEM().refresh(entity, properties);
  }

  @Override
  public void refresh(final Object entity, final LockModeType lockMode) {
    this.getEM().refresh(entity, lockMode);
  }

  @Override
  public void refresh(final Object entity, final LockModeType lockMode, final Map<String, Object> properties) {
    this.getEM().refresh(entity, lockMode, properties);
  }

  @Override
  public void clear() {
    this.getEM().clear();
  }

  @Override
  public void detach(final Object entity) {
    this.getEM().detach(entity);
  }

  @Override
  public boolean contains(final Object entity) {
    return this.getEM().contains(entity);
  }

  @Override
  public LockModeType getLockMode(final Object entity) {
    return this.getEM().getLockMode(entity);
  }

  @Override
  public void setProperty(final String propertyName, final Object value) {
    this.getEM().setProperty(propertyName, value);
  }

  @Override
  public Map<String, Object> getProperties() {
    return this.getEM().getProperties();
  }

  @Override
  public Query createQuery(final String qlString) {
    return this.getEM().createQuery(qlString);
  }

  @Override
  public <T> TypedQuery<T> createQuery(final CriteriaQuery<T> criteriaQuery) {
    return this.getEM().createQuery(criteriaQuery);
  }

  @Override
  public <T> TypedQuery<T> createQuery(final String qlString, final Class<T> resultClass) {
    return this.getEM().createQuery(qlString, resultClass);
  }

  @Override
  public Query createNamedQuery(final String name) {
    return this.getEM().createNamedQuery(name);
  }

  @Override
  public <T> TypedQuery<T> createNamedQuery(final String name, final Class<T> resultClass) {
    return this.getEM().createNamedQuery(name, resultClass);
  }

  @Override
  public Query createNativeQuery(final String sqlString) {
    return this.getEM().createNativeQuery(sqlString);
  }

  @Override
  public Query createNativeQuery(final String sqlString, final Class resultClass) {
    return this.getEM().createNativeQuery(sqlString, resultClass);
  }

  @Override
  public Query createNativeQuery(final String sqlString, final String resultSetMapping) {
    return this.getEM().createNativeQuery(sqlString, resultSetMapping);
  }

  @Override
  public void joinTransaction() {
    this.getEM().joinTransaction();
  }

  @Override
  public <T> T unwrap(final Class<T> cls) {
    return this.getEM().unwrap(cls);
  }

  @Override
  public Object getDelegate() {
    return this.getEM().getDelegate();
  }

  @Override
  public boolean isOpen() {
    return this.getEM().isOpen();
  }

  @Override
  public EntityTransaction getTransaction() {
    return this.getEM().getTransaction();
  }

  @Override
  public javax.persistence.EntityManagerFactory getEntityManagerFactory() {
    return this.getEM().getEntityManagerFactory();
  }

  @Override
  public CriteriaBuilder getCriteriaBuilder() {
    return this.getEM().getCriteriaBuilder();
  }

  @Override
  public Metamodel getMetamodel() {
    return this.getEM().getMetamodel();
  }
}
