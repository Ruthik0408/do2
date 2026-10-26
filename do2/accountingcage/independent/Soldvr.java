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
public class Soldvr extends AccountingCageBase implements java.io.Serializable {
	public StringBuffer buffer = new StringBuffer();
    Do2 refDo2;
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (do2.getColumnA().equals("CEASED")){
			if (do2.getRefDo2No() != null){
				refDo2 = Utils.getReferenceDo2(do2);
				if (refDo2 != null && refDo2.getDescription().equals("SOLDVR")) {
					validity = true;
				} else {
					validity=false;
				}				
			} else {
				validity = false;
				buffer.append("VL: Reference Part II order to be given");
			}
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
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
}
