package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.Utils;

@RestController
public class Blddon extends AccountingCageBase implements java.io.Serializable {
	private static final String INVALID_COLUMNA = "VL:Col. 7A should contain blood group.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidA(do2)){
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(INVALID_COLUMNA);	
		}
		if (validity == true){		 
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
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
	
	public boolean isValidA(Do2 do2){
		if (do2.getColumnA() != null && do2.getColumnA().startsWith("GP-")){
			return true;
		}
		return false;
	}
}
