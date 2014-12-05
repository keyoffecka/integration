package net.ofk.integration.jetty.bitronix;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Service;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;

/**
 * Jetty lifecycle bean.
 * Starts and shuts down the Bitronix transaction manager.
 *
 * @author Konstantin I. key.offecka@runbox.com
 */
public class TransactionManagerLifeCycle extends AbstractLifeCycle {
  protected void doStart() throws Exception {
    TransactionManagerServices.getTransactionManager();

    Log.getLog().info("Transaction manager has been successfully started");
  }

  protected void doStop() throws Exception {
    Service service = TransactionManagerServices.getTransactionManager();
    service.shutdown();

    Log.getLog().info("Transaction manager has been successfully shut down");
  }
}
