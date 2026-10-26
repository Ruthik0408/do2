package itsdc.dolphin.do2.accountingcage.independent;
import itsdc.dolphin.models.Do2;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
/**
 * 
 * @author Y Raja Reddy
 *  Date: 8-07-2009
 */
@RestController
public class Fra extends Nra {
	public static String FRA_BEYOND_CUT_OFF_DATE="AU:FRA not admissible after 30-06-1996.\n";
		
	@Override
	public String process(Do2 do2){
		super.process(do2);
		return Do2Manager.SUCCESS; 
	}

	@Override
	public boolean isValid(Do2 do2) {
		if(!do2.getToDate().after(DateManager.getFraCutOffDate())){
			boolean validity = super.isValid(do2);
			if (validity) {
				do2.setStatus(Do2Manager.VALID);
				if(do2.getAcceptedQe()==null){
					do2.setAcceptedQe(Utils.getCurrentQuarter());	
				}else{
					if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
						do2.setReviewQe(Utils.getCurrentQuarter());
					}
				}
			}
			return validity;
		}else{
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(FRA_BEYOND_CUT_OFF_DATE);
			do2Repo.save(do2);
			return false;
		}
	
	}	
	
}