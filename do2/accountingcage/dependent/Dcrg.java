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
public class Dcrg extends AccountingCageBase implements java.io.Serializable {
	Employee employee;
	public StringBuffer buffer = new StringBuffer();	
	static public final String INVALID_QS = "VL:QS should not be more than 33 years \n";
	static public final String INVALID_TJRA = "VL:QS years cannot be blank or zero \n";
	static public final String INVALID_LRA = "VL:QS Months cannot be more than 11 \n";
	static public final String INVALID_FRDT = "VL:From Date is valid only on or after 1.9.2008\n";
	static public final String NOT_IN_FS_LIST = "AU:This Army No is Not a FS Case/Fs Case Entry Not approved. \n.";
	static public final String INVALID_PAO = "VL:This Do2 can be fed in only PAO DSC \n";
	static public final double MAXIMUM_DCRG = 1000000.0;

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
	/*	//***lines added validating the armyno available in current month fs
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
		}
		*/
		if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			validity = false;
			buffer.append(INVALID_FRDT);
		}
		//if (do2.getTjra() == null || do2.getTjra() == 0){
		if (do2.getTjra() == null){
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
			 calculateDcrg(do2);
		}
		return Do2Manager.SUCCESS;
	}

	public String calculateDcrg(Do2 do2) {
		double dcrgAdmissible = 0;
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
		}	else {
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
		// to give weightage of 5 years -- 66 half years * 1/4  (16 and half - reckonable emoluments)
	//	half_yrly_periods += 10;  --- as discussed with Shri Vadivelu, AO
		if (half_yrly_periods > 66)
			half_yrly_periods = 66;
		
		dcrgAdmissible = (Math.round((totalPay/4) * half_yrly_periods));	
		
		// IN ORDER TO RESTRICT MAXIMUM PAYABLE DCRG TO 10,00,000.
		if (dcrgAdmissible > MAXIMUM_DCRG){
			dcrgAdmissible = MAXIMUM_DCRG;
		}
		// in order to restrict dcrg to service gratuity,  if service gratuity is payable
		if (half_yrly_periods < 31){
			double srgrtyAdmissible = (double)(Math.round((totalPay/2) * half_yrly_periods));	
			if (dcrgAdmissible > srgrtyAdmissible){
				dcrgAdmissible = srgrtyAdmissible;
			}
		}
		
		Utils.addArrears(do2, dcrgAdmissible);
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
		if (do2.getTjra() < 5 && do2.getTjra() > 33){
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
		} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
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
