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

package fromjava_wsaddressing.server;

import javax.jws.WebService;
import javax.xml.ws.*;
import javax.xml.ws.soap.Addressing;

@Addressing
@WebService
public class AddNumbersImpl {

    @Action(
            input = "http://example.com/input",
            output = "http://example.com/output")
    public int addNumbers(int number1, int number2) throws AddNumbersException {
        return impl(number1, number2);
    }

    public int addNumbers2(int number1, int number2) throws AddNumbersException {
        return impl(number1, number2);
    }

    @Action(
            input = "http://example.com/input3",
            output = "http://example.com/output3",
            fault = {
            @FaultAction(className = AddNumbersException.class, value = "http://example.com/fault3")
                    }
    )
    public int addNumbers3(int number1, int number2) throws AddNumbersException {
        return impl(number1, number2);
    }

    int impl(int number1, int number2) throws AddNumbersException {
        if (number1 < 0 || number2 < 0) {
            throw new AddNumbersException("Negative numbers can't be added!",
                                          "Numbers: " + number1 + ", " + number2);
        }
        return number1 + number2;
    }
}

