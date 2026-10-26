package itsdc.dolphin.do2.accountingcage.dependent;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Unit;
 
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

//import org.apache.commons.digester.SetPropertiesRule;
 

/**
 * 
 * @author IT & SDC
 * 
 */
@RestController
public class Srgrty extends AccountingCageBase implements java.io.Serializable {
	Employee employee;
	public StringBuffer buffer = new StringBuffer();	
	static public String INVALID_QS = "VL:QS should not be below 5 and more than 15 years \n";
	static public String INVALID_TJRA = "VL:QS years cannot be blank or zero \n";
	static public String INVALID_LRA = "VL:QS Months cannot be more than 11 \n";
	static public String INVALID_FRDT = "VL:From Date is valid only on or after 1.9.2008 \n";
	static public final String NOT_IN_FS_LIST = "AU:This Army No is Not a FS Case/Fs Case Entry Not approved. \n.";
	static public String INVALID_PAO = "VL:This Do2 can be fed in only PAO DSC \n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());		
		if (!Utils.getPao().getPaoCode().equals("94")){
			validity = false;
			//buffer.append(INVALID_PAO);
			do2.setReason(INVALID_PAO);
			do2.setStatus(Do2Manager.INVALID);
			return false;
		}
		//***lines added validating the armyno available in current month fs
		if (!Utils.isEmpThisMonthFs(employee)){
			 do2.setStatus(Do2Manager.INVALID);
			 do2.setReason(NOT_IN_FS_LIST);
			 do2Repo.save(do2);
			 return false;
		}
		/*	
		if (!Utils.isEmpThisMonthFs(employee)) {
				if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_JKLI)
						&& Utils.isEmployeeAvailableInLpcRegister(employee)) {  // modified on 15.07.2025    -- put on hold for now
					;
				} else {
					do2.setStatus(Do2Manager.INVALID);
					do2.setReason(NOT_IN_FS_LIST);
					do2Repo.save(do2);
					return false;	
				//}			
			} */
		if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			validity = false;
			buffer.append(INVALID_FRDT);
		}
		if (do2.getTjra() == null || do2.getTjra() == 0){
			validity = false;
			buffer.append(INVALID_TJRA);
		}
		if (do2.getLra() != null && do2.getLra() > 11){
			validity = false;
			buffer.append(INVALID_LRA);
		}
		if (!isValidQs(do2)) {
			validity = false;
			buffer.append(INVALID_QS);
		}
        
	    if (!validity) {
			 do2.setStatus(Do2Manager.INVALID);			
	    } else {
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else { 
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
			do2.setStatus(Do2Manager.VALID);
		 }
		
		do2Repo.save(do2);
		
		 do2.setReason(buffer.toString());
	 	 return validity;
	
	}

	@Override
	public String process(Do2 do2) {
		if (isValid(do2)) {
			 calculateServiceGratuity(do2);
		}
		return Do2Manager.SUCCESS;
	}

	public String calculateServiceGratuity(Do2 do2) {
		double srgrtyAdmissible = 0;
		double totalPay = 0;
		int half_yrly_periods = 0;
		AuditCage baseAuditCage = AuditCageHelper.getBaseAuditCage(do2.getFromDate(), do2);
		int basicPay = 0;
		int gradePay = 0;
		int da = 0;
		int msPay = 0;
		if (baseAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
			basicPay = baseAuditCage.getBasicPay();
			gradePay = baseAuditCage.getBasicPay() ==null ? 0 :baseAuditCage.getGradePay();
			da     = getFullMonthDa(do2,baseAuditCage);
			msPay  = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();		
		} else if (baseAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) { 
			basicPay = baseAuditCage.getOldBasicPay();
			gradePay = baseAuditCage.getOldGradePay() ==null ? 0 :baseAuditCage.getOldGradePay();
			da     = getFullMonthDa(do2,baseAuditCage);
			msPay  = 2000;		
		}else {
			basicPay = baseAuditCage.getBasicPay();
			da     = getFullMonthDa(do2,baseAuditCage);
			msPay  = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();		
		}
	
		
		if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			totalPay = basicPay+gradePay+da+msPay;
		}
		if (do2.getTjra() != null){
			half_yrly_periods = do2.getTjra() * 2;
		}
		if (do2.getLra() != null){
			if (do2.getLra() >= 9){
				half_yrly_periods += 2;
			} else if (do2.getLra() >= 3){
				half_yrly_periods += 1;
			}
		}
		if (half_yrly_periods > 30)
			half_yrly_periods = 30;
		
		srgrtyAdmissible = Math.round((totalPay/2) * half_yrly_periods);	
		
		Utils.addArrears(do2, srgrtyAdmissible);
		return Do2Manager.SUCCESS;
		
	}
  
	public boolean isValidQs(Do2 do2) {
		if (do2.getTjra()==null || do2.getTjra()==0){
			buffer.append("No Value given in TJRA Column");
			return false;
		}
		if (do2.getLra() == null ){
			do2.setLra(0);
		}
		if (do2.getTjra() < 5 && do2.getTjra() > 15){
			return false;
		}		
		return true;
	}
	public int getFullMonthDa(Do2 do2,AuditCage dueAuditCage) {				
		double da = 0.0;
		double daRate = 0.0;
		employee = Utils.getCurrentEmployee(dueAuditCage.getDo2().getArmyNo());

		if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
					dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

			da = 0.0;
			int gradePay = dueAuditCage.getGradePay() == null ? 0 : dueAuditCage.getGradePay();
			int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage.getMsPay();
			
			daRate = 0.0;
			for (Allowance allowance : allowanceList) {
				daRate = allowance.getRatePercentage();
				da = (dueAuditCage.getBasicPay() + gradePay + msPay) * daRate;
			}
		}else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			da = 0.0;
			int gradePay = dueAuditCage.getOldGradePay() == null ? 0 : dueAuditCage.getOldGradePay();
			int msPay = 2000;
			daRate = 1.25;
			da = (dueAuditCage.getOldBasicPay() + gradePay + msPay) * daRate;
		} else {
			List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
					dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

			da = 0.0;
			int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage.getMsPay();
			
			daRate = 0.0;
			for (Allowance allowance : allowanceList) {
				daRate = allowance.getRatePercentage();
				da = (dueAuditCage.getBasicPay()  + msPay) * daRate;
			}
		}
		

		return ((int) Math.round(da));
	}
	
}
