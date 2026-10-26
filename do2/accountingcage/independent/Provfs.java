package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase; 
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.models.Do2; 
import itsdc.dolphin.do2.Do2Manager; 
import itsdc.dolphin.utils.Utils;

@RestController
public class Provfs extends AccountingCageBase implements java.io.Serializable { 
	
	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity=false;
		
		if (do2.getEmployee()!=null && do2.getEmployee().getStatusCode().getId()!=1) {				
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("PROVFS Published For An NON-EFFECTIVE/Recruit IRLA.  Please check!!! ");
			validity=false;
		} else {
			do2.setStatus(Do2Manager.VALID);	
			do2.setReason(null);
			validity=true;
		}		
		
		if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null
					&& !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
			} 
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())) {
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}		 
		do2Repo.save(do2);
		return validity;		
	}
		
	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
}		
	