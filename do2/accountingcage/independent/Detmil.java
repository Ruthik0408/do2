package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Detmil extends AccountingCageBase implements java.io.Serializable {

	 
	String message;

	static public final String INVALID_REFDO2 = "AU:Incorret Reference Do2.\n ";
	static public final String INVALID_TJRA = "VL:No of Months to be mentioned in TJRA column.\n";
	static public final String INVALID_LRA = "VL:No of Days not mentioned or more than 30 days in LRA column .\n";
	static public final String INVALID_DETENTION_PERIOD = "VL:Detention Period and months/days not tallied.\n";

	private StringBuffer buffer = new StringBuffer();
	private double do2Days;
    Do2 refDo2;
	
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
			buffer.append(INVALID_DETENTION_PERIOD);
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
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidTjra(Do2 do2) {
		if (do2.getDescription().equals(Do2Manager.LINES)){
			return true;
		} else if (do2.getTjra() != null && do2.getTjra() >= 0) {
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
		do2Days = 0.0;
		do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		
		if (do2.getDescription().equals(Do2Manager.LINES) && do2.getLra() == do2Days){
			return true;
		} else if (do2.getTjra() != null && ((do2.getTjra() * 30) + do2.getLra() == do2Days)) {
			return true;
		} 
		return false;
	}
	public boolean isValidRefDo2(Do2 do2) {
		// As per the discussion with ASDC, ND visiting team on 15.04.2014
		/*refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2!=null && (refDo2.getDescription().equals(Do2Manager.AWL)
				|| refDo2.getDescription().equals(Do2Manager.OSL)
				|| refDo2.getDescription().equals("DESN"))) {
			return true;
		} 
		return false;*/
		return true;
	}
}
