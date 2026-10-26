package itsdc.dolphin.do2.accountingcage.independent;

import java.util.List;

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
public class Ptal extends AccountingCageBase implements java.io.Serializable {
	static public final String INVALID_REFDO2 = "AU:Incorrect Reference Do2 No.\n ";
	private StringBuffer buffer = new StringBuffer();
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
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
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
        List<Do2> do2List = Utils.getReferenceDo2List(do2);
        for (Do2 refDo2 : do2List){
        	  if (refDo2.getDescription().equals("FORLVE"))
        		  return true;
        }
		
		return false;
	}
}

