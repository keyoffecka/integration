package net.ofk.integration.cdi.jta;

import org.junit.Test;

import javax.interceptor.InvocationContext;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Konstantin I. key.offecka@runbox.com
 */
public class TransactionInterceptorTest {
  @Test
  public void testSucceededIntercept() throws Exception {
    Object object = new Object();

    InvocationContext ctx = mock(InvocationContext.class);
    Transaction tx = mock(Transaction.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());

    doReturn(object).when(ctx).proceed();
    doReturn(tx).when(i).begin();
    doNothing().when(i).commit(tx);

    Object result = i.intercept(ctx);
    assertSame(result, object);

    verify(i).begin();
    verify(i).commit(tx);
    verify(i, never()).rollback(anyObject());
  }

  @Test
  public void testFailedIntercept() throws Exception {
    Exception exception = new Exception();

    InvocationContext ctx = mock(InvocationContext.class);
    Transaction tx = mock(Transaction.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());

    doThrow(exception).when(ctx).proceed();
    doReturn(tx).when(i).begin();
    doNothing().when(i).rollback(tx);

    try {
      i.intercept(ctx);
      fail();
    } catch (final Throwable ex) {
      assertSame(exception, ex);
    }

    verify(i).begin();
    verify(i).rollback(tx);
    verify(i, never()).commit(anyObject());
  }

  @Test
  public void testFailedInterceptInBegin() throws Exception {
    RuntimeException exception = new RuntimeException();

    InvocationContext ctx = mock(InvocationContext.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());

    doThrow(exception).when(i).begin();

    try {
      i.intercept(ctx);
      fail();
    } catch (final Throwable ex) {
      assertSame(exception, ex);
    }

    verify(i).begin();
    verify(ctx, never()).proceed();
    verify(i, never()).rollback(anyObject());
    verify(i, never()).commit(anyObject());
  }

  @Test
  public void testFailedInterceptInCommit() throws Exception {
    RuntimeException exception = new RuntimeException();

    InvocationContext ctx = mock(InvocationContext.class);
    Transaction tx = mock(Transaction.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());

    doThrow(exception).when(i).commit(tx);
    doReturn(tx).when(i).begin();
    doNothing().when(i).rollback(tx);

    try {
      i.intercept(ctx);
      fail();
    } catch (final Throwable ex) {
      assertSame(exception, ex);
    }

    verify(i).begin();
    verify(i).rollback(tx);
    verify(i).commit(tx);
  }

  @Test
  public void testBegin() throws Exception {
    TransactionManager tm = mock(TransactionManager.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());
    doReturn(tm).when(i).getTM();
    doReturn(null).when(tm).getTransaction();

    i.begin();

    verify(tm).begin();
  }

  @Test
  public void testByPassBegin() throws Exception {
    Transaction tx = mock(Transaction.class);
    TransactionManager tm = mock(TransactionManager.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());
    doReturn(tm).when(i).getTM();
    doReturn(tx).when(tm).getTransaction();

    i.begin();

    verify(tm, never()).begin();
  }

  @Test
  public void testByPassCommit() throws Exception {
    Transaction tx = mock(Transaction.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());

    i.commit(tx);

    verify(tx, never()).commit();
  }

  @Test
  public void testCommit() throws Exception {
    TransactionManager tm = mock(TransactionManager.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());

    doReturn(tm).when(i).getTM();

    i.commit(null);

    verify(tm).commit();
  }

  @Test
  public void testByPassRollback() throws Exception {
    TransactionManager tm = mock(TransactionManager.class);
    Transaction tx = mock(Transaction.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());

    doReturn(tm).when(i).getTM();
    doReturn(tx).when(tm).getTransaction();
    doReturn(Status.STATUS_ACTIVE).when(tx).getStatus();

    i.rollback(tx);

    verify(tm, never()).rollback();
  }

  @Test
  public void testByBassNotActiveRollback() throws Exception {
    TransactionManager tm = mock(TransactionManager.class);
    Transaction tx = mock(Transaction.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());

    doReturn(tm).when(i).getTM();
    doReturn(tx).when(tm).getTransaction();
    doReturn(Status.STATUS_COMMITTED).when(tx).getStatus();

    i.rollback(null);

    verify(tm, never()).rollback();
  }

  @Test
  public void testRollback() throws Exception {
    TransactionManager tm = mock(TransactionManager.class);
    Transaction tx = mock(Transaction.class);
    TransactionInterceptor i = spy(new TransactionInterceptor());

    doReturn(tm).when(i).getTM();
    doReturn(tx).when(tm).getTransaction();
    doReturn(Status.STATUS_ACTIVE).when(tx).getStatus();

    i.rollback(null);

    verify(tm).rollback();
  }
}
