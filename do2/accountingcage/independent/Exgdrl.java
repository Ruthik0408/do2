package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */
@RestController
public class Exgdrl extends AccountingCageBase implements java.io.Serializable {
  
	static public final String INVALID_REFDO2 = "AU:Incorrect Reference Do2 No.\n ";
	static public final String INVALID_LEAVE_PERIOD = "VL:Leave period not tallied.\n";
	static public final String REFDO2_BLANK = "VL:ReferenceDO2No Column Is Blank.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		StringBuffer buffer = new StringBuffer();
		if (do2.getRefDo2No()==null || (do2.getRefDo2No()!=null && do2.getRefDo2No().length()==0)) {
			validity = false;
			buffer.append(REFDO2_BLANK);
		}
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (!isValidDays(do2)) {
			validity = false;
			buffer.append(INVALID_LEAVE_PERIOD );
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);			
			if (do2.getAcceptedQe()==null) {
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())) {
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
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	public boolean isValidDays(Do2 do2) {		
		if (do2.getLra()==null || (do2.getLra()!=null && do2.getLra()<=0)) {
			return false;
		}		
		return (DateManager.getNoOfDays(do2.getFromDate(),do2.getToDate())==do2.getLra());
	}	
	public boolean isValidRefDo2(Do2 do2) {
		Do2 refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2!=null && refDo2.getDescription()!=null && (refDo2.getDescription().equals("AWL")
				|| refDo2.getDescription().equals("OSL")
				|| refDo2.getDescription().equals("DESN"))) {
			return true;
		} 
		return false; 
	}
}
