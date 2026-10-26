package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC,Secunderabad
 * 
 */
@RestController
public class Reserv extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	static public String INVALID_COLUMNAB = "VL:Col.5A should contain CEASED or RESERVE liability yrs/age or GRANTED in Col.5B.\n ";

	private StringBuffer buffer = new StringBuffer();
	Do2 refDo2;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (!isValidAb(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNAB);
		}
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
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
		message = buffer.toString();
		do2.setReason(message);
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}

	public boolean isValidAb(Do2 do2) {
		if (do2.getColumnA() != null && do2.getColumnA().equals("CEASED")) {
			return true;
		} else if (do2.getColumnA() != null && do2.getColumnB().equals("GRANTED")) {
			return true;
		}
		return false;
	}
}
