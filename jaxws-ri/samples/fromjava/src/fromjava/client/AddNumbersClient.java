package fromjava.client;

<<<<<<< AddNumbersClient.java
import java.rmi.RemoteException;

import javax.xml.ws.BindingProvider;

=======
>>>>>>> 1.6.12.1.2.1
public class AddNumbersClient {
    public static void main (String[] args) {
        try {
            AddNumbersImpl port = new AddNumbersImplService().getAddNumbersImplPort();
            log((BindingProvider)port);

            int number1 = 10;
            int number2 = 20;

            System.out.printf ("Invoking addNumbers(%d, %d)\n", number1, number2);
            int result = port.addNumbers (number1, number2);
            System.out.printf ("The result of adding %d and %d is %d.\n\n", number1, number2, result);

            number1 = -10;
            System.out.printf ("Invoking addNumbers(%d, %d)\n", number1, number2);
            result = port.addNumbers (number1, number2);
            System.out.printf ("The result of adding %d and %d is %d.\n", number1, number2, result);
        } catch (AddNumbersException_Exception ex) {
            System.out.printf ("Caught AddNumbersException_Exception: %s\n", ex.getFaultInfo ().getDetail ());
        }
    }

    private static final void log(BindingProvider port) {
        if (Boolean.getBoolean("wsmonitor")) {
            String address = (String)port.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            address = address.replaceFirst("8080", "4040");
            port.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);
        }
    }
}
