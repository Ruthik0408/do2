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
public class Medcat extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public String INVALID_COLA   = "AU:SHAPE to be given in Col.5A\n ";

	private StringBuffer buffer = new StringBuffer();
	Do2 refDo2;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
        if (do2.getColumnA()==null){
        	validity = false;
        	buffer.append(INVALID_COLA);
        }
        
        if (!do2.getColumnA().equals("INITIAL")) {
        	if (!isValidRefDo2(do2)) {
    			validity = false;
    			buffer.append(INVALID_REFDO2);
    		}
        }
		
		if (!validity) {
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

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null) {
			return true;
		} 
		return false;
	}
}
