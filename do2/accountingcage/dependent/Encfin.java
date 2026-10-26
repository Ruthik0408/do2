package itsdc.dolphin.do2.accountingcage.dependent;

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
 

@RestController
public class Encfin extends AccountingCageBase implements java.io.Serializable {

	Employee employee;
	public StringBuffer buffer = new StringBuffer();
	private static final String INVALID_COL_A = "VL:DISCH/INVALD/DEATH/BATTLE are only valid";
	private static final String INVALID_COL_B = "VL:Invalid number of days for total accummulation of leave.\n";
	private static final String INVALID_COL_C = "AU:Encash days cannot be more than Accumulation of Annual Leave";
	private static final String INVALID_PUBLICATION = "VL:Description is not valid in old Appendix-J.\n";
	private static final String INVALID_REF_DO2 = "VL:Invalid reference do2 number.\n";
	static public final String NOT_IN_FS_LIST = "AU:This Army No is Not a FS Case/Fs Case Entry Not approved. \n.";
	static public List<String> COL_7A_LIST = Arrays.asList("DISCH","INVALD","DEATH","BATTLE","PRDEAD");
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Date revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (do2.getDo2Date().before(revised_appxj_impl_date)){
			validity = false;
			buffer.append(INVALID_PUBLICATION);	
		}
		if (!isValidA(do2)){
			// do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
			buffer.append(INVALID_COL_A + NOT_IN_APPENDIX_J_FORMAT);	
		}
		if (!isValidB(do2)){
		//	do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
			buffer.append(INVALID_COL_B + NOT_IN_APPENDIX_J_FORMAT);	
		}
		if (!isValidC(do2)) {
		//	do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
			buffer.append(INVALID_COL_C + NOT_IN_APPENDIX_J_FORMAT);
		}
		
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REF_DO2);
		}
		 
		if (do2.getEmployee().getStatusCode().getId() > 5){
			   ;
		} else if (!Utils.isEmpThisMonthFs(employee)){
			validity = false;
			buffer.append(NOT_IN_FS_LIST);
		}
		
		if (!validity) {
		//	if(do2.getStatus() != null)
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		}else{
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
		
		do2Repo.save(do2);
	 	return validity;
	}

	@Override
	public String process(Do2 do2) {
		if (isValid(do2)) {
			 commitEncfinDo2(do2);
		}
		return Do2Manager.SUCCESS;
	}

	public String commitEncfinDo2(Do2 do2) {
		double encashAdmissible = 0;
		int totalPay =0;
		AuditCage baseAuditCage = AuditCageHelper.getBaseAuditCage(do2.getFromDate(), do2);
		
		/*int basicPay = baseAuditCage.getBasicPay();
		int gradePay = baseAuditCage.getBasicPay() == null ? 0 :baseAuditCage.getGradePay();
		int da       = getFullMonthDa(do2,baseAuditCage);
		int xPay     = baseAuditCage.getXGroupPay() == null ? 0 : baseAuditCage.getXGroupPay();
		int msPay    = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();		
		if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			totalPay = basicPay+gradePay+da+xPay+msPay;
		}*/
		
		int basicPay = 0;
		int gradePay =0;
		int xPay = 0;
		int msPay = 0;
		 int da     = getFullMonthDa(do2,baseAuditCage);
		 /*if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			 basicPay = baseAuditCage.getBasicPay();
			 gradePay = baseAuditCage.getGradePay() ==null ? 0 :baseAuditCage.getGradePay();
			 xPay  = baseAuditCage.getXGroupPay() == null ? 0 : baseAuditCage.getXGroupPay();
			 msPay  = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();
		 } else {
			 basicPay = baseAuditCage.getOldBasicPay();
			 gradePay = baseAuditCage.getOldGradePay() ==null ? 0 :baseAuditCage.getOldGradePay();
			 xPay = Utils.get6cpcXGroupPay(baseAuditCage);
			 msPay = Utils.get6cpcMsPay(baseAuditCage);
		 }*/
		 
		 if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			 basicPay = baseAuditCage.getBasicPay();
			// xPay  = baseAuditCage.getXGroupPay() == null ? 0 : baseAuditCage.getXGroupPay();
			// msPay  = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();
		 } else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) 
				 && !do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			 basicPay = baseAuditCage.getOldBasicPay();
			 gradePay = baseAuditCage.getOldGradePay() ==null ? 0 :baseAuditCage.getOldGradePay();
			 xPay = Utils.get6cpcXGroupPay(baseAuditCage);
			 msPay = Utils.get6cpcMsPay(baseAuditCage);
		 } else  if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			 basicPay = baseAuditCage.getBasicPay();
			 gradePay = baseAuditCage.getGradePay() ==null ? 0 :baseAuditCage.getGradePay();
			 xPay  = baseAuditCage.getXGroupPay() == null ? 0 : baseAuditCage.getXGroupPay();
			 msPay  = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();
		 } 
		 
		 
		if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			totalPay = basicPay+gradePay+da+xPay+msPay;
		}
		
		int noOfDays= Integer.parseInt(do2.getColumnC());
		encashAdmissible = (double)(Math.round((totalPay * noOfDays) / 30));	
		
		
		Utils.addArrears(do2, encashAdmissible);
		return Do2Manager.SUCCESS;
		
	}
	public boolean isValidA(Do2 do2){
		if (do2.getColumnA() != null && COL_7A_LIST.contains(do2.getColumnA().trim())) {
			return true;
		}
		return false;
	}
  
	public boolean isValidB(Do2 do2){
		if (do2.getColumnB() != null && Integer.valueOf(do2.getColumnB()) > 0){
			return true;
		}
		return false;
	}
	
	public boolean isValidC(Do2 do2) {
		if(do2.getColumnC() == null || do2.getColumnC().length() == 0){
			return false;
		}
		if ((do2.getColumnA().equals(Do2Manager.DISCH)
				|| do2.getColumnA().equals(Do2Manager.DEATH)
				|| do2.getColumnA().equals(Do2Manager.BATTLE)
				|| do2.getColumnA().equals(Do2Manager.INVALD)
				|| do2.getColumnA().equals("PRDEAD")) && 
				(do2.getColumnC() != null && (Integer.parseInt(do2.getColumnC()) <= Constants.MAX_ENCASH_DAYS) &&
						do2.getColumnB() != null && Integer.parseInt(do2.getColumnC()) <= Integer.parseInt(do2.getColumnB()))) {
			return true;
		}
		return false;
	}
		
	
	public boolean isValidRefDo2(Do2 do2) {
		Do2 refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null && do2.getColumnA() != null) {
			if (refDo2.getDescription().equals(do2.getColumnA()) && refDo2.getFromDate().equals(do2.getFromDate())) {
				return true;
			} 
		} 
		return false;
	}
/*	
	public int getFullMonthDa(Do2 do2,AuditCage dueAuditCage) {				
		
		employee = Utils.getCurrentEmployee(dueAuditCage.getDo2().getArmyNo());

		if (employee.getRegularTaFlag() == Do2Manager.TERRITORIAL_ARMY
				&& employee.getTaAppointmentCategory() == Do2Manager.STOP_DA_TA_CATEGORY) {
			return 0;
		}
		if (employee.getSubstantiveRank() != null
				&& employee.getSubstantiveRank().getRankCode().equals(
						Do2Manager.RECRUIT)) {
			return 0;
		}
		if (employee.getDeputationFlag() != null
				&& employee.getDeputationFlag() == true
				&& employee.getRecordStatus().equals(Do2Manager.DEPU_IMTRAT_BHUTAN)) {
			return 0;
		}

		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
				dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

		double da = 0.0;

		int variableComponents = 0;
		int xGroupPay = dueAuditCage.getXGroupPay() == null ? 0 : dueAuditCage
				.getXGroupPay();
		int gradePay = dueAuditCage.getGradePay() == null ? 0 : dueAuditCage
				.getGradePay();
		int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage
				.getMsPay();
		
		double daRate = 0.0;
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
			if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = gradePay + msPay + xGroupPay;
			}
			da += (dueAuditCage.getBasicPay() + variableComponents) * daRate;
		}
		return ((int) Math.round(da));
	}*/
	
	
public int getFullMonthDa(Do2 do2,AuditCage dueAuditCage) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (employee.getRegularTaFlag().equals(Do2Manager.TERRITORIAL_ARMY)
				&& employee.getTaAppointmentCategory().equals(Do2Manager.STOP_DA_TA_CATEGORY)) {
			return 0;
		}
		//*************************** to be reviewed
		if ((employee.getSubstantiveRank() != null
				&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))) {
			return 0;
		}
		
		
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
				dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

		double da = 0.0;
		int variableComponents = 0;
		int basicPay = dueAuditCage.getBasicPay() == null ? 0 : dueAuditCage.getBasicPay();
		int xGroupPay = dueAuditCage.getXGroupPay() == null ? 0 : dueAuditCage.getXGroupPay();
		int gradePay = dueAuditCage.getGradePay() == null ? 0 : dueAuditCage.getGradePay();
		int dp = dueAuditCage.getDp() == null ? 0 : dueAuditCage.getDp();
		int classPay = dueAuditCage.getClassPay() == null ? 0 : dueAuditCage.getClassPay();
		int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage.getMsPay();
		int oldGradePay = dueAuditCage.getOldGradePay() == null ? 0 : dueAuditCage.getOldGradePay();
		double daRate = 0.0;
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
			if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0) {
				daRate = allowance.getRateAmount();
			} else if (!dueAuditCage.getFromDate().before (DateManager.getSeventhCpcDate()) && dueAuditCage.getGradePay()!=0) {
				basicPay = dueAuditCage.getOldBasicPay();
				msPay = Utils.get6cpcMsPay(dueAuditCage);
				if (dueAuditCage.getXGroupPay()!=0)
					xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);				
				variableComponents = oldGradePay + msPay + xGroupPay;
			}
			if (dueAuditCage.getFromDate().before(DateManager.getDPDate())) {
				variableComponents = classPay;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					|| (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0)) {
				variableComponents = dp;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = gradePay + xGroupPay;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
				variableComponents = gradePay + msPay + xGroupPay;
			}  else if (dueAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				basicPay = dueAuditCage.getOldBasicPay();
			 	msPay = Utils.get6cpcMsPay(dueAuditCage);
				if (dueAuditCage.getXGroupPay()!=0)
					xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);
				daRate = 1.25;	
				variableComponents = oldGradePay + msPay + xGroupPay;
			} else {
			//	Below line is commented on 06-10-2021 as the mspay x group pay is not considered for encashment so DA on mspay and x group 
			//	pay also not taken in to account as the elements of mspay, xpay itself not considred for encashemnt 
			
				//	variableComponents =  msPay + xGroupPay;
				
			}
	/*	System.out.println("nnnnnnnnnnnnnnnnnnnnnnnnn   variable components + date  " + variableComponents 
				+"  "+dueAuditCage.getFromDate()+"  "+basicPay +"  " +daRate);*/
			 da += (basicPay + variableComponents) * daRate;
	 	}
		return ((int) Math.round(da));
	}
}
