package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Avlpay extends AccountingCageBase implements java.io.Serializable {
	
	static final String INVALID_FROM_DATE = "VL:Discontinued from 1.5.1991 \n";
	static final String ADJ_VR = "VL:Adjust through adjustment voucher.\n";
	StringBuffer buffer = new StringBuffer();
	 
	@Override
	
	public boolean isValid(Do2 do2) {
		boolean validity = false;
		try {
			if (!do2.getFromDate().before(DateManager.getDate(1991,5,1))){
				validity=false;
				buffer.append(INVALID_FROM_DATE);
			} else {
				validity=false;
				buffer.append(ADJ_VR);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
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
		do2Repo.save(do2);
		return validity;
	}
		
	@Override
	public String process(Do2 do2) {
		return Do2Manager.SUCCESS;
	}
}		
	