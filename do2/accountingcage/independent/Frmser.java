package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.models.Do2;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

 

@RestController
public class Frmser extends AccountingCageBase implements java.io.Serializable {

	static public String FROM_DATE_AFTER_TO_DATE = "AU:To Date is before From Date.\n";
	
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (do2.getToDate().before(do2.getFromDate())) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(FROM_DATE_AFTER_TO_DATE);
			validity = false;
		}
		if (validity) {
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
