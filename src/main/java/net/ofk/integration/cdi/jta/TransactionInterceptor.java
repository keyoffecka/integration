package net.ofk.integration.cdi.jta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

/**
 * Intercepts any method invocation annotated with the {@link javax.transaction.Transactional} annotation.
 *
 * @author Konstantin I. key.offecka@runbox.com
 */
@Transactional
@Interceptor
public class TransactionInterceptor {
  private static final Logger LOG = LoggerFactory.getLogger(TransactionInterceptor.class);

  @Resource(name="TransactionManager")
  public TransactionManager tm;
  TransactionManager getTM() {return this.tm;}

  @AroundInvoke
  public Object intercept(final InvocationContext invocationContext) throws Exception {
    Object result = null;

    Transaction tx = this.begin();

    try {
      result = invocationContext.proceed();

      this.commit(tx);
    } catch (final Throwable th) {
      this.rollback(tx);

      throw th;
    }

    return result;
  }

  Transaction begin() throws SystemException, NotSupportedException {
    Transaction tx = this.getTM().getTransaction();
    if (tx == null) {
      this.getTM().begin();

      TransactionInterceptor.LOG.debug("Starting a new transaction.");
    } else {
      TransactionInterceptor.LOG.debug("Joining to the existing transaction.");
    }
    return tx;
  }

  void commit(final Transaction tx) throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException {
    if (tx == null) {
      this.getTM().commit();

      TransactionInterceptor.LOG.debug("Transaction has been committed successfully.");
    }
  }

  void rollback(final Transaction tx) {
    if (tx == null) {
      try {
        int tmStatus = this.getTM().getTransaction().getStatus();
        if (tmStatus == Status.STATUS_ACTIVE) {
          this.getTM().rollback();

          TransactionInterceptor.LOG.debug("Failed transaction has been rolled back successfully.");
        }
      } catch (final IllegalStateException | SystemException ex) {
        TransactionInterceptor.LOG.error("Failed to rollback the failed transaction, the cause error follows.", ex);
      }
    }
  }
}
