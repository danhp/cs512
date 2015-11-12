package client;

/**
 * Created by hmsimmonds on 15-11-12.
 * CLASS MADE FOR AUTOMATICALLY TESTING / RUNNING CLIENT CODE
 */
public class AutoClient extends WSClient {

    public AutoClient(String serviceName, String serviceHost, int servicePort)
            throws Exception {
        super(serviceName, serviceHost, servicePort);
    }

    public void run() {

        //SETUP FOR PERFORMANCE TESTING ----------------------------------//

        int transactionNum = 1;
        int flightNum = 100;
        int flightSeats = 500;
        int flightPrice = 1000;



        try {

//            //loop for 100 transactions to test for performance benchmark
//            for (int i = 0; i < 100; i++) {
//
//                if (i % 3 == 0) {
//                    //start new transaction every 3 operations
//                    transactionNum = proxy.start();
//                }
//
//                if (i % 3 == 0) { //if divisible by 3, then add flight
//                    proxy.addFlight(transactionNum, flightNum, flightSeats, flightPrice);
//                }
//
//            }
                Thread.sleep(1);
        } catch (InterruptedException ex) {
                System.out.println("Interrupted exception when sleeping within AutoClient");
        } catch (Exception ex) {
                System.out.println("Generic Exception caugh in AutoClient");
        }
        //----------------------------------------------------------------//
    }


}
