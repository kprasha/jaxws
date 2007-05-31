/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package fromjava_wsaddressing.client;

public class AddNumbersClient {
    int number1 = 10;
    int number2 = 10;
    int negativeNumber = -10;

    public static void main(String[] args) {
        AddNumbersClient client = new AddNumbersClient();
        client.addNumbers();
        client.addNumbersFault();
        client.addNumbers2();
        client.addNumbers3Fault();
    }

    public void addNumbers() {
        System.out.printf("addNumbers: basic input and output name mapping\n");
        try {
            AddNumbersImpl stub = createStub();
            int result = stub.addNumbers(number1, number2);
            assert result == 20;
        } catch (Exception ex) {
            ex.printStackTrace();
            assert false;
        }
        System.out.printf("\n\n");
    }

    public void addNumbersFault() {
        System.out.printf("addNumbersFault: fault caused by out of bounds parameter value\n");
        AddNumbersImpl stub;

        try {
            stub = createStub();
            stub.addNumbers(negativeNumber, number2);
            assert false;
        } catch (AddNumbersException_Exception ex) {
            //This is expected exception
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        System.out.printf("\n\n");
    }

    public void addNumbers2() {
        System.out.printf("addNumbers2: default input and output name mapping\n");
        try {
            AddNumbersImpl stub = createStub();
            int result = stub.addNumbers2(number1, number2);
            assert result == 20;
        } catch (Exception ex) {
            ex.printStackTrace();
            assert false;
        }
        System.out.printf("\n\n");
    }

    public void addNumbers3Fault() {
        System.out.printf("addNumbers3Fault: custom fault mapping\n");
        try {
            createStub().addNumbers3(negativeNumber, number2);
            assert false;
        } catch (AddNumbersException_Exception e) {
            //This is expected exception
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        System.out.printf("\n\n");
    }

    private AddNumbersImpl createStub() throws Exception {
        return new AddNumbersImplService().getAddNumbersImplPort();
    }
}
