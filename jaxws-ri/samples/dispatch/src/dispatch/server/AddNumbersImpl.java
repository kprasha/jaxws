package dispatch.server;

@javax.jws.WebService (endpointInterface="dispatch.server.AddNumbersPortType")
public class AddNumbersImpl implements AddNumbersPortType {
    public int addNumbers (int number1, int number2) {
        return number1 + number2;
    }
}
