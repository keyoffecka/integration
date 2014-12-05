#Integration utilities
They help you to integrate one technology into another.
Currently you can integrate:
* Bitronix transaction manager into Jetty

There is Jetty lifecycle bean which starts the transaction manager on Jetty startup.

* JTA into CDI

The provided transaction interceptor when registered in **beans.xml**
will intercept any method annotated with **javax.transaction.Transactional**
and if there is no a transaction associated with the thread
a new transaction will be started and committed (or rolled back) accordingly.
The implementation is very simple and supports only
the **javax.transaction.Transactional.TxType.REQUIRED** transaction propagation mode.

* JPA into CDI

There is a delegating entity manager implemented which lazily initializes underlying real entity manager
which allows to use them in transactional methods since the actual entity manager creations happens in the same place (thread)
where the entity manager is used. These guarantees that the created instance is associated with the transaction effective
for the current transactional method.

##Motivation
There is a very good set of integration utilities in the [Apache DeltaSpike](http://deltaspike.apache.org/documentation/#_introduction) project
but it's too heavy for simple applications.
