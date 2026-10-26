package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC, Secunderabad
 * 
 */
@RestController
public class Rejlve extends AccountingCageBase implements java.io.Serializable {
	
	static public final String INVALID_COLUMNA = "VL:Invalid Col.7A.\n";
	StringBuffer buffer = new StringBuffer();
	String message;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidA(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNA);	
		}
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
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
		if ((do2.getColumnA() != null) && (do2.getColumnA().equals("AL") || do2.getColumnA().equals("PAL") || do2.getColumnA().equals("ADVAL") 
				|| do2.getColumnA().equals("BAL")|| do2.getColumnA().equals("RL") || do2.getColumnA().equals("CL") 
				|| do2.getColumnA().equals("SL")|| do2.getColumnA().equals("ACCMAL") || do2.getColumnA().equals("CAL") 
				|| do2.getColumnA().equals("EXTNLV") || do2.getColumnA().equals("PATLVE"))) {				
			return true;
		} 
		return false;
	}
}
