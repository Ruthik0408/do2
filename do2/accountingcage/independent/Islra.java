package itsdc.dolphin.do2.accountingcage.independent;

 
import itsdc.dolphin.models.Do2;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;


@RestController
public class Islra extends AccountingCageBase implements java.io.Serializable {

	 
	@Override
	public boolean isValid(Do2 do2) {
		if (do2.getFromDate().after(DateManager.getSixthCpcDate())){
			do2.setStatus(Do2Manager.INVALID);
			if (do2.getDescription().equals("ISLRA")){
				do2.setReason("to be published as ISNRA");
			} else if (do2.getDescription().equals("ISMRA")){
				do2.setReason("to be published as ISNRA");				
			}
			do2Repo.save(do2);
			return false;
		}
		do2.setStatus(Do2Manager.VALID);
		
		if (do2.getAcceptedQe()==null){
			do2.setAcceptedQe(Utils.getCurrentQuarter());	
		} else {
			if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
				do2.setReviewQe(Utils.getCurrentQuarter());
			}
		}
		do2Repo.save(do2);
		return true;
	}
		
	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
}		
	