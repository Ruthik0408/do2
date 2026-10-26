package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.Utils;

 

@RestController
public class AuditCageAbsentProcessor extends SpecialProcessor {
	
   public String process(Do2 do2){
	   
	   try {
			do2Processor = (Do2AuditCageProcessor) Class.forName(
					"ind.gov.cgda.sagacity.dolphin.session.do2.auditcage."
							+ Utils.getCamelCase(do2.getDescription()))
					.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			Utils.handleException(e); 
		}
	    if(dueDrawnAuditCage[1].getAbsentFlag()==false){
	    	drawnList = getAllowances(dueDrawnAuditCage[1]);	
	    }else{
	    	drawnList = new ArrayList<AuditCageAllowance>();
	    }
		
		dueDrawnAuditCage[0]= do2Processor.getNewAuditCage(currentAuditCage, base);
		
		do2Processor.setNewAuditCage(dueDrawnAuditCage[0]);
		do2Processor.process(do2, base);
		if(dueDrawnAuditCage[0].getAbsentFlag() == false){
			dueList = getAllowances(dueDrawnAuditCage[0]);	
		}else{
			dueList = new ArrayList<AuditCageAllowance>();
		}
		
	   return Do2Manager.SUCCESS;
   }
}
