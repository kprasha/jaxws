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

package type_substitution.server;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.WebServiceException;

/**
 * Sample showing type substitution.
 */
@WebService(name="CarDealer")
@XmlSeeAlso({Car.class, Toyota.class})
public class CarDealer {

    public Car[] getSedans(String carType){
        if(carType.equalsIgnoreCase("Toyota")){
            Car[] cars = new Toyota[2];
            cars[0] = new Toyota("Camry", "1998", "white");
            cars[1] = new Toyota("Corolla", "1999", "red");
            return cars;
        }
        throw new WebServiceException("Not a dealer of: "+carType);
    }

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public Car tradeIn(Car oldCar){
        if(!(oldCar instanceof Toyota))
            throw new WebServiceException("Expected Toyota, received, "+oldCar.getClass().getName());

        Toyota toyota = (Toyota)oldCar;
        if(toyota.getMake().equals("Toyota") && toyota.getModel().equals("Avalon") && toyota.getYear().equals("1999") && toyota.getColor().equals("white")){
            return new Toyota("Avalon", "2007", "black");
        }
        throw new WebServiceException("Invalid Toyota Car");
    }
}
