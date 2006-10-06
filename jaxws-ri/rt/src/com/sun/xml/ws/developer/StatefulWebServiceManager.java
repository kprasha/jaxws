package com.sun.xml.ws.developer;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.addressing.MemberSubmissionEndpointReference;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.server.AsyncProvider;
import com.sun.xml.ws.api.server.AsyncProviderCallback;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

/**
 * Stateful web service support in the JAX-WS RI.
 *
 * <h2>Usage</h2>
 * <p>
 * Application service implementation classes (or providers) who'd like
 * to use the stateful web service support must declare {@link Resource}
 * injection to a static field or a method as follows:
 *
 * <pre>
 * &#64;{@link WebService}
 * class BankAccount {
 *     protected final int id;
 *     private int balance;
 *
 *     Account(int id) { this.id = id; }
 *     &#64;{@link WebMethod}
 *     public synchronized void deposit(int amount) { balance+=amount; }
 *
 *     // either via a static field
 *     <font color=red>
 *     &#64;{@link Resource} static {@link StatefulWebServiceManager} manager;
 *     </font>
 *     // ... or  via a static method (the method name could be anything)
 *     <font color=red>
 *     &#64;{@link Resource} static void setManager({@link StatefulWebServiceManager} manager) {
 *        ...
 *     }
 *     </font>
 * }
 * </pre>
 *
 * <p>
 * After your service is deployed but before you receive a first request, this injection
 * occurs.
 *
 * <p>
 * A stateful web service class does not need to have a default constructor.
 * In fact, most of the time you want to define a constructor that takes
 * some arguments, so that each instance carries certain state (as illustrated
 * in the above example.)
 *
 * <p>
 * Each instance of a stateful web service class is identified by an unique
 * {@link EndpointReference}. Your application creates an instance of
 * a class, then you'll have the JAX-WS RI assign this unique EPR for the
 * instance as follows:
 *
 * <pre>
 * &#64;{@link WebService}
 * class Bank { // this is ordinary stateless service
 *     &#64;{@link WebMethod}
 *     public synchronized W3CEndpointReference login(int accountId, int pin) {
 *         if(!checkPin(pin))
 *             throw new AuthenticationFailedException("invalid pin");
 *         BankAccount acc = new BankAccount(accountId);
 *         return BankAccount.manager.{@link #export export}(acc);
 *     }
 * }
 * </pre>
 *
 * <p>
 * Typically you then pass this EPR to remote systems. When they send
 * messages to this EPR, the JAX-WS RI makes sure that the particular exported
 * instance associated with that EPR will receive a service invocation.
 *
 * <h2>Things To Consider</h2>
 * <p>
 * When you no longer need to tie an instance to the EPR,
 * use {@link #unexport(Object)} so that the object can be GC-ed
 * (or else you'll leak memory.) You may choose to do so explicitly,
 * or you can rely on the time out by using {@link #setTimeout(int)}.
 *
 * <p>
 * {@link StatefulWebServiceManager} is thread-safe. It can be safely
 * invoked from multiple threads concurrently.
 *
 * @author Kohsuke Kawaguchi
 */
public interface StatefulWebServiceManager<T> {
    /**
     * Exports an object.
     *
     * <p>
     * This method works like {@link #export(Object)} except that
     * you can obtain the EPR in your choice of addressing version.
     *
     * @param epr
     *      Either {@link W3CEndpointReference} or {@link MemberSubmissionEndpointReference}.
     *      If other types are specified, this method throws an {@link WebServiceException}.
     * @return
     *      {@link WSEndpointReference} that identifies this exported
     *      object.
     */
    @NotNull <EPR extends EndpointReference> EPR export(Class<EPR> epr, T o);

    /**
     * Exports an object.
     *
     * <p>
     * JAX-WS RI assigns an unique EPR to the exported object,
     * and from now on, messages that are sent to this EPR will
     * be routed to the given object.
     *
     * <p>
     * The object will be locked in memory, so be sure to
     * {@link #unexport(Object) unexport} it when it's no longer needed.
     *
     * <p>
     * Notice that the obtained EPR contains the address of the service,
     * which depends on the currently processed request. So invoking
     * this method multiple times with the same object may return
     * different EPRs, if such multiple invocations are done while
     * servicing different requests. (Of course all such EPRs point
     * to the same object, so messages sent to those EPRs will be
     * served by the same instance.)
     *
     * @return
     *      {@link W3CEndpointReference} that identifies this exported
     *      object. Always non-null.
     */
    @NotNull W3CEndpointReference export(T o);

    /**
     * Exports an object (for {@link AsyncProvider asynchronous web services}.)
     *
     * <p>
     * This method works like {@link #export(Class,Object)} but it
     * takes an extra {@link WebServiceContext} that represents the request currently
     * being processed by the caller (the JAX-WS RI remembers this when the service
     * processing is synchronous, and that's why this parameter is only needed for
     * asynchronous web services.)
     *
     * <h3>Why {@link WebServiceContext} is needed?</h3>
     * <p>
     * The obtained EPR contains address, such as host name. The server does not
     * know what its own host name is (or there are more than one of them),
     * so this value is determined by what the current client thinks the server name is.
     * This is why we need to take {@link WebServiceContext}. Pass in the
     * object given to {@link AsyncProvider#invoke(Object, AsyncProviderCallback,WebServiceContext)}.
     */
    @NotNull <EPR extends EndpointReference> EPR export(Class<EPR> epr, @NotNull WebServiceContext context, T o);

    /**
     * Unexports the given instance.
     *
     * <p>
     * JAX-WS will release a strong reference to unexported objects,
     * and they will never receive further requests (requests targeted
     * for those unexported objects will be served by the fallback object.)
     *
     * @param o
     *      if null, this method will be no-op.
     */
    void unexport(@Nullable T o);

    /**
     * Sets the "fallback" instance.
     *
     * <p>
     * When the incoming request does not have the necessary header to
     * distinguish instances of <tt>T</tt>, or when the header is present
     * but its value does not correspond with any of the active exported
     * instances known to the JAX-WS, then the JAX-WS RI will try to
     * route the request to the fallback instance.
     *
     * <p>
     * This provides the application an opportunity to perform application
     * specific error recovery.
     *
     * <p>
     * If no fallback instance is provided, then the JAX-WS RI will
     * send back the fault. By default, no fallback instance is set.
     *
     * <p>
     * This method can be invoked any time, but most often you'd like to
     * use one instance at the get-go. The following code example
     * illustrates how to do this:
     *
     * <pre>
     * &#64;{@link WebService}
     * class BankAccount {
     *     ... continuting from the example in class javadoc ...
     *
     *     &#64;{@link Resource} static void setManager({@link StatefulWebServiceManager} manager) {
     *        manager.setFallbackInstance(new BankAccount(0) {
     *            &#64;{@link Override}
     *            void deposit(int amount) {
     *                putToAuditRecord(id);
     *                if(thisLooksBad())   callPolice();
     *                throw new {@link WebServiceException}("No such bank account exists");
     *            }
     *        });
     *     }
     * }
     * </pre>
     *
     * @param o
     *      Can be null.
     */
    void setFallbackInstance(T o);

    // TODO
    ///**
    // * Configures timeout for exported instances.
    // *
    // * <p>
    // * When configured, the JAX-WS RI will internally use a timer
    // * so that exported objects that have not received any request
    // * for the given amount of minutes will be automatically unexported.
    // *
    // * <p>
    // */
    //void setTimeout(int minutes, Callback<T> callback);
    //
    //
    //interface Callback<T> {
    //    boolean onTimeout(T timedOutObject, @NotNull StatefulWebServiceManager manager);
    //}
}
