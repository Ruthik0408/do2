package itsdc.dolphin.do2.accountingcage.independent;

 
import itsdc.dolphin.models.Do2;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/*
 * @author N RAMACHANDRAN && K RAMADEVI   DATE: 03.08.2009
 */
@RestController
public class Pededn extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	static final public String INVALID_AMOUNT = "VL:Amount in Col.7A cannot be zero or negative. \n";
	private StringBuffer buffer = new StringBuffer();
	 	
	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
		
		if (Double.parseDouble(do2.getColumnA()) <= 0) {
			buffer.append(INVALID_AMOUNT);
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
		}
		
		if (validity) {
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

		Utils.addArrears(do2, (Double.parseDouble(do2.getColumnA())*-1));
		return Do2Manager.SUCCESS;
	}
}		