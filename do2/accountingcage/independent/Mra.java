package itsdc.dolphin.do2.accountingcage.independent;
import itsdc.dolphin.models.Do2;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;
 

/**
 * 
 * @author Y Raja Reddy
 *  Date: 8-07-2009
 */

@RestController

public class Mra extends Nra {
	public static String  NOT_HOSPITAL ="VL:Column 7C does not contain Hospital.\n";
	@Override
	public String process(Do2 do2){
		super.process(do2);
		return Do2Manager.SUCCESS;
		
	}	
	@Override
	public boolean isValid(Do2 do2) {
		if( do2.getColumnC().equals("HOSPTL")){
			if (super.isValid(do2)) {
				do2.setStatus(Do2Manager.VALID);
				if(do2.getAcceptedQe()==null){
					do2.setAcceptedQe(Utils.getCurrentQuarter());	
				}else{
					if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
						do2.setReviewQe(Utils.getCurrentQuarter());
					}
				}
				do2Repo.save(do2);
				return true;
			} else {
				do2.setStatus(Do2Manager.INVALID);
				do2Repo.save(do2);
			    return false;
			}
		}else{
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("NOT_HOSPITAL");
 			do2Repo.save(do2);
		return false;
		}
	}	
}
 