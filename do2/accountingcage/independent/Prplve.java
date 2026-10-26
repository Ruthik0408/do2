package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC, Secunderabad
 * 
 */

@RestController
public class Prplve extends AccountingCageBase implements java.io.Serializable {

	static public final String INVALID_REFDO2 = "AU:Incorrect Reference Do2 No.\n ";
	static public final String INVALID_LRA = "VL:LRA days not mentioned or more than 10 days.\n";
	static public final String RATE_NOT_AVAILABLE = "AU:Rate not available in Allowance Rate table.\n ";
		
	private StringBuffer buffer = new StringBuffer();
	private double do2Days;
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(INVALID_LRA);
		}
		if (!isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_AVAILABLE);
		} 
		if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null
					&& !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
			} 
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

		List<Allowance> allowances = Utils.getApplicableAllowanceRates(Do2Manager.NRA,
					do2.getFromDate(), do2.getToDate());
		double totalNra = 0.0;
		for (Allowance allow : allowances) {
			totalNra += (allow.getRateAmount()
						* DateManager.getNoOfDays(allow.getFromDate(), allow.getToDate()));
			
		}
		Utils.addArrears(do2, Math.round(totalNra), Do2Manager.NRA, do2.getFromDate(), do2.getToDate());
		return Do2Manager.SUCCESS;
	}

	
	public boolean isValidLra(Do2 do2) {
		System.out.println("do2 dates"+do2.getFromDate()+"  "+do2.getToDate()+"  "+do2.getArmyNo()+"  "+do2.getDescription());
		do2Days=DateManager.getNoOfDays(do2.getFromDate(),do2.getToDate());
		if (do2.getLra() != null && do2.getLra() == do2Days && do2.getLra() <= 10) {
			return true;
		} 
		return false;
	}

	public boolean isValidRefDo2(Do2 do2) {
        List<Do2> do2List = Utils.getReferenceDo2List(do2);
        for(Do2 refDo2 : do2List){
        	  if(refDo2.getDescription().equals("POSIN"))
        		  return true;
        }
		
		return false;
	}
	
	public boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(Do2Manager.NRA,
				do2.getFromDate(), do2.getToDate());
		return allowances.size() >0;
	}
}