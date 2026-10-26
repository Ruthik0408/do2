package itsdc.dolphin.do2.xml;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import itsdc.dolphin.controllers.Do2ControlController;

@Component
public class RejectedAcknScheduler {

    @Autowired
    private Do2ControlController do2ControlController;

    private static final SimpleDateFormat LOG_SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    
    
  //  @Scheduled(cron = "0 38 12 * * ?")
    @Scheduled(cron = "0 0 16 ? * MON,THU")
     public void runBulkRejectedAcknEmail() {

        System.out.println("[RejectedAcknScheduler] Triggered at " + LOG_SDF.format(new Date()));

        try {
            String result = do2ControlController.processAndEmailRejectedAckn();
            System.out.println("[RejectedAcknScheduler] Result : " + result);
        } catch (Exception e) {
            // Scheduled methods swallow exceptions silently by default - always catch & log
            e.printStackTrace();
        }
    }
}