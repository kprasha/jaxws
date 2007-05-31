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

package type_substitution.client;


import java.util.List;

public class CarBuyerApp {
    public static void main(String[] args) {
        CarDealerService service = new CarDealerService();
        CarDealer dealer = service.getCarDealerPort();

        /**
         * CarDealer.getSedans() returns List<Car>
         * Car is abstract class. The code below shows
         * that the client is expecting a Toyota which extends
         * Car.
         *
         * It shows a doc wrapper style operation.
        */
        List<Car> r = dealer.getSedans("toyota");
        Toyota car = (Toyota)r.get(0);
        if(car != null && car.getMake().equals("Toyota") &&
                car.getModel().equals("Camry") &&
                car.getYear().equals("1998") &&
                car.getColor().equals("white")){
            System.out.println("Received Right car!");
        }else{
            System.out.println("Failed to get Car!");
        }

        /**
         * CarDealer.tradeIn(Car) takes an abstract class Car and returns the same.
         * We will send a sub-class instead and expect to get the same.
         *
         */
        Toyota oldCar = new Toyota();
        oldCar.setMake("Toyota");
        oldCar.setModel("Avalon");
        oldCar.setYear("1999");
        oldCar.setColor("white");

        Toyota newCar = (Toyota)dealer.tradeIn(oldCar);

        if(newCar != null &&
                newCar.getMake().equals("Toyota") &&
                newCar.getModel().equals("Avalon") &&
                newCar.getYear().equals("2007") &&
                newCar.getColor().equals("black")){
            System.out.println("Traded in Right Car!");
        }else{
            System.out.println("Failed to tradeIn!");
        }
    }
}
