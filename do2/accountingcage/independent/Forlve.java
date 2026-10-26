package itsdc.dolphin.do2.accountingcage.independent;

 
import itsdc.dolphin.models.Do2;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;


@RestController
public class Forlve extends AccountingCageBase implements java.io.Serializable {
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	 
	@Override
	public boolean isValid(Do2 do2) {
		if (do2.getColumnA() == null || (do2.getColumnA() != null && Integer.parseInt(do2.getColumnA()) == 0)){
			//do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("No. of days to be mentioned in Col-7A" + NOT_IN_APPENDIX_J_FORMAT);
			do2Repo.save(do2);
			return false;
		} 
		do2.setStatus(Do2Manager.VALID);
		do2.setReason(null);
		
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
	