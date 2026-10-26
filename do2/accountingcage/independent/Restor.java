package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC , Secunderabad
 * 
 */
@RestController
public class Restor extends AccountingCageBase implements java.io.Serializable {

	 
    Do2 refDo2;
	
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public String INVALID_TJRA = "VL:No of Months not mentioned in TJRA column.\n";
	static public String INVALID_LRA = "VL:No of Days not mentioned or more than 30 days in LRA column.\n";
	static public String INVALID_LEAVE_PERIOD = "VL:Leave Period not matching with months/days.\n";

	private StringBuffer buffer = new StringBuffer();
	private double do2Days = 0.0;
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (!isValidTjra(do2)) {
			validity = false;
			buffer.append(INVALID_TJRA);
		}
		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(INVALID_LRA);
		}
		if (!isTjraLra(do2)) {
			validity = false;
			buffer.append(INVALID_LEAVE_PERIOD);
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
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_LPC_SERVICE+"");
		return Do2Manager.SUCCESS;
	}
   
	public boolean isValidTjra(Do2 do2) {
		if (do2.getTjra() >= 0) {
			return true;
		} 
		return false;
	}

	public boolean isValidLra(Do2 do2) {
		if (do2.getLra() > 0 && do2.getLra() <= 30) {
			return true;
		} 
		return false;
	}
	public boolean isTjraLra(Do2 do2) {
		do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		if (do2.getTjra() * 30 + do2.getLra() == do2Days) {
			System.out.println("Do2 days  "+do2Days);
			return true;
		} 
		return false;
	}
	public boolean isValidRefDo2(Do2 do2) {
			Do2 refDo2= Utils.getReferenceDo2(do2);
			if (refDo2!=null && (refDo2.getDescription().equals("SERFIT") || refDo2.getDescription().equals("DESERT"))) {
				return true;
		} 
		return false;
	}
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_LPC_SERVICE);
		return Do2Manager.SUCCESS;
	}
}
