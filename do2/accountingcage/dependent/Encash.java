package itsdc.dolphin.do2.accountingcage.dependent;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
 * @author A. R Reddy
 * 
 */
@RestController
public class Encash extends AccountingCageBase implements java.io.Serializable {
	Employee employee;
	public StringBuffer buffer = new StringBuffer();
	//private static final String INVALID_COL_A = "VL:DISCH/INVALD/DEATH/BATTLE are only valid";
	//private static final String INVALID_COL_C = "AU:Encash days cannot be more than maximum 300 days";
	
	private static final String INVALID_COL_C = "AU:ENCASH-LTC- LTC availed for the year difference to the Encashment-LTC claimed \n";
	
	static final String INVALID_COL_A = "VL:Invalid Value Given In Column_A - Only 'LTC' Is Permitted.\n";
	static public String INVALID_SAME_YEAR = "AU:LTC Encashment already availed for the same year";
	static public String TJRA_DAYS = "VL:TJRA days is not matching.\n";
	static public String ENCASH_LIMIT_OVER = "AU:PBOR Already Encashed 60 Days for LTC.\n";
	static public String INVALID_LRA = "AU:LTC Encashed Days given in LRA Col. does not match with Employee table";
	static public String INVALID_EMP_DATA = "AU:LTC Encashed Days is blank in employee table. It should be zero or should have some value";
	static public final String NOT_IN_FS_LIST = "AU:This Army No is Not a FS Case/Fs Case Entry Not approved. \n.";
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n";
    static public final int ENCASH_MAX_DAYS = 300;
    static public final int MAXIMUM_LTC_DAYS = 60;
    static public final int MAXIMUM_LTC_DAYS_AT_A_TIME = 10;
//	static public List<String> COL_7A_LIST = Arrays.asList("DISCH","INVALD","DEATH","BATTLE","LTC");
    static public List<String> LTC_PERMISSIBLE_LEAVE = Arrays.asList("AL","PAL","BAL","ADVAL","CL");
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Date revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		Date cbillbegDate=null;
		Date cbillendDate = null;
		try {
			cbillbegDate=DateManager.getDate("2013-04-01");
			cbillendDate=DateManager.getDate("2014-07-31");
		} catch (Exception e) {
			Utils.handleException(e);
		}

		if ((!do2.getFromDate().before(cbillbegDate) && !do2.getFromDate().after(cbillendDate)) && do2.getColumnA().equals("LTC")){
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("To be claimed as advance through IAFA 194 between 1.4.2013 and 31.7.2014");	
			do2Repo.save(do2);
			return false;
		}
		
		if (!do2.getDo2Date().before(revised_appxj_impl_date) && (Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode()) && do2.getFromDate().after(cbillendDate) &&
		   (do2.getColumnA().equals("DISCH") || do2.getColumnA().equals("DISMIS") || 
			do2.getColumnA().equals("INVALD") || do2.getColumnA().equals("DEATH") ||
			do2.getColumnA().equals("BATTLE")))) {
			validity = false;
			buffer.append("To be published as ENCFIN");	
		}
		if (!do2.getDo2Date().before(revised_appxj_impl_date) && (Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode()))) {
			if (do2.getColumnA()==null || (do2.getColumnA()!=null && !do2.getColumnA().trim().equals("LTC"))) {
			//	do2.setStatus("MR");
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
				buffer.append(INVALID_COL_A);	
			}	
		}
		
	    if (!isValidRefDo2(do2)) {
				validity = false;
				buffer.append(INVALID_REFDO2);
		   }
		   	
	/*	if (!isValidA(do2)){
			do2.setStatus("MR");
			validity = false;
			buffer.append(INVALID_COL_A + NOT_IN_APPENDIX_J_FORMAT);	
		}
		
		*/
		
		if (!isValidC(do2)) {
			validity = false;
			buffer.append(INVALID_COL_C);
		}
		//***lines added validating the armyno available in current month fs
		if (!do2.getColumnA().equals("LTC")){
		   if (do2.getEmployee().getStatusCode().getId() > 5){
			   ;
		   } else if (!Utils.isEmpThisMonthFs(employee)){
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(NOT_IN_FS_LIST);
				do2.setReason(buffer.toString());
				do2Repo.save(do2);
				return false;
	 	   }
		}
		if (!isValidTjra(do2)) {
			validity = false;
			buffer.append(TJRA_DAYS);
		}
        if (do2.getColumnA().equals("LTC")){
        	if (employee.getLtcEncashmentDays() == null){
	       	   do2.setStatus(Do2Manager.INVALID);
	       	   buffer.append(INVALID_EMP_DATA);
	      	   do2.setReason(buffer.toString());
	      	   do2Repo.save(do2);
	      	   return false;      		 
        	}    	
		    if (!isExceededTotalLtcDays(do2)){
			   validity = false;
			   buffer.append(ENCASH_LIMIT_OVER);
		   	}
		    if (!isValidLra(do2)){
		        validity = false;
		    	buffer.append(INVALID_LRA);
		    }
		    if (!do2.getDo2Date().before(revised_appxj_impl_date) && 
		    		(Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode()) 
		    				&& do2.getFromDate().after(cbillendDate))){
		    	if (isSameYear(do2)){
			    	validity = false;
			    	buffer.append(INVALID_SAME_YEAR);
			    }
		    }		    
       }
	   
   
	   if (validity==false) {
		//   if(do2.getStatus() != null);
				do2.setStatus(Do2Manager.INVALID);	
				do2.setReason(buffer.toString());		
	   } else {
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else { 
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
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
			 commitEncashDo2(do2);
		}
		return Do2Manager.SUCCESS;
	}

	public String commitEncashDo2(Do2 do2) {
		double encashAdmissible = 0;
		int totalPay =0;
		AuditCage baseAuditCage = AuditCageHelper.getBaseAuditCage(do2.getFromDate(), do2);
		int basicPay = 0;
		int gradePay =0;
		int xPay = 0;
		int msPay = 0;
		 int da     = getFullMonthDa(do2, baseAuditCage);
		/* if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
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
		 
		
		int classPay = baseAuditCage.getClassPay() == null ?0 : baseAuditCage.getClassPay();
		int dp       = baseAuditCage.getDp() == null ?0 : baseAuditCage.getDp();
		
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			totalPay = basicPay+dp+da+classPay;
		} else if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			totalPay = basicPay+gradePay+da+xPay;
		} else if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			totalPay = basicPay+gradePay+da+xPay+msPay;
		} else {
			totalPay = basicPay+gradePay+da+xPay+msPay;
		}
		
		
		/*int basicPay = baseAuditCage.getBasicPay();
		int gradePay = baseAuditCage.getBasicPay() ==null ? 0 :baseAuditCage.getGradePay();
		int da     = getFullMonthDa(do2,baseAuditCage);
		int xPay  = baseAuditCage.getXGroupPay() == null ? 0 : baseAuditCage.getXGroupPay();
		int msPay  = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();
		int classPay = baseAuditCage.getClassPay() == null ?0 : baseAuditCage.getClassPay();
		int dp       = baseAuditCage.getDp() == null ?0 : baseAuditCage.getDp();
		
		if(!do2.getFromDate().after(DateManager.getSixthCpcDate())){
			totalPay = basicPay+dp+da+classPay;
		}
		if(!do2.getFromDate().before(DateManager.getSixthCpcDate()) && !do2.getFromDate().after(DateManager.getSixthCpcAllowanceDate())){
			totalPay = basicPay+gradePay+da+xPay;
		}
		if(!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			totalPay = basicPay+gradePay+da+xPay+msPay;
		}
		
		if(!do2.getFromDate().after(DateManager.getSixthCpcDate())){
			totalPay = basicPay+dp+da+classPay;
		}
		if(!do2.getFromDate().before(DateManager.getSixthCpcDate()) && !do2.getFromDate().after(DateManager.getSixthCpcAllowanceDate())){
			totalPay = basicPay+gradePay+da+xPay;
		}
		if(!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			totalPay = basicPay+gradePay+da+xPay+msPay;
		}*/
		
		encashAdmissible = (double)(Math.round((totalPay * do2.getTjra()) / 30));	
		
		
		Utils.addArrears(do2, encashAdmissible);
		if(do2.getColumnA()!=null && do2.getColumnA().equals("LTC"))
			employee.setLtcEncashmentDays(do2.getLra());
		empRepo.save(employee);
		return Do2Manager.SUCCESS;
		
	}
   
    public boolean isExceededTotalLtcDays(Do2 do2){    	   
    	   if(employee.getLtcEncashmentDays() !=null && (employee.getLtcEncashmentDays()+do2.getTjra()) <= MAXIMUM_LTC_DAYS)
    		   return true;
    	   return false;
    }
   
    public boolean isValidLra(Do2 do2){
    	if (employee.getLtcEncashmentDays() !=null && (employee.getLtcEncashmentDays()+do2.getTjra()) ==do2.getLra())
    		return true;
    	return false;
    }
	
    public boolean isSameYear(Do2 do2){
    	if (do2.getColumnC() == null)
    		return false;
        List<Do2>do2List=do2Repo.findByColumnAColumnCDo2IdNotEqual(do2.getEmployee().getId(), "ENCASH", "LTC", do2.getColumnC(),
        																								do2.getId());
        		
      /*  		Utils.getEntityManager().createQuery("select d from Do2 d where d.employee = :emp and " +
        		" d.description = 'ENCASH' and d.columnA = 'LTC' and d.columnC=:colc and d.id != :id and d.status = 'V'")
         .setParameter("colc",do2.getColumnC())
         .setParameter("emp", do2.getEmployee())
         .setParameter("id" , do2.getId()).getResultList();*/
        if (do2List.size() > 0)
        	return true;
    	return false;
    }
    
	public boolean isValidTjra(Do2 do2) {
		if (do2.getColumnA()==null || do2.getColumnA().length()==0){
			buffer.append("No Value given in TJRA Column");
			return false;
		}
		if (do2.getColumnA().equals("LTC")){
			if(do2.getTjra()<=0){
				buffer.append("Value in TJRA Column should be more than zero");
				return false;
			}
		}
		
		if (do2.getColumnA().equals("LTC") && (do2.getTjra() <= MAXIMUM_LTC_DAYS_AT_A_TIME)) {
			return true;
		} else {
			if ((do2.getColumnA().equals(Do2Manager.DISCH)
					|| do2.getColumnA().equals(Do2Manager.DEATH)
					|| do2.getColumnA().equals(Do2Manager.DISMIS)
					|| do2.getColumnA().equals(Do2Manager.INVALD)) && (do2.getTjra() <= ENCASH_MAX_DAYS)) {
				return true;
			}
		}
		return false;
	}
	/*public int getFullMonthDa(Do2 do2,AuditCage dueAuditCage) {				
		
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
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
				dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

		double da = 0.0;

		int variableComponents = 0;
		int xGroupPay = dueAuditCage.getXGroupPay() == null ? 0 : dueAuditCage
				.getXGroupPay();
		int gradePay = dueAuditCage.getGradePay() == null ? 0 : dueAuditCage
				.getGradePay();
		int dp = dueAuditCage.getDp() == null ? 0 : dueAuditCage.getDp();
		int classPay = dueAuditCage.getClassPay() == null ? 0 : dueAuditCage
				.getClassPay();
		int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage
				.getMsPay();
		
		double daRate = 0.0;
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
			if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					&& dp != 0) {
				daRate = allowance.getRateAmount();
			}
			if (dueAuditCage.getFromDate().before(DateManager.getDPDate())) {
				variableComponents = classPay;
			} else if (dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcDate())
					|| (!dueAuditCage.getFromDate().before(
							DateManager.getSixthCpcDate()) && dp != 0)) {
				variableComponents = dp;
			} else if (dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = gradePay + xGroupPay;
			} else {
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
			} else if (dueAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				basicPay = dueAuditCage.getOldBasicPay();
			 	msPay = Utils.get6cpcMsPay(dueAuditCage);
				if (dueAuditCage.getXGroupPay()!=0)
					xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);
				daRate = 1.25;	
				variableComponents = oldGradePay + msPay + xGroupPay;
			} else {
				
				// 	Below line is commented on 06-10-2021 as the mspay x group pay is not considered for encashment so DA on mspay and x group 
				//	pay also not taken in to account as the elements of mspay, xpay itself not considred for encashemnt 
	
				//variableComponents =  msPay + xGroupPay;

			}
/*		System.out.println("nnnnnnnnnnnnnnnnnnnnnnnnn   variable components + date  " + variableComponents 
				+"  "+dueAuditCage.getFromDate()+"  "+basicPay +"  " +daRate);
*/			 da += (basicPay + variableComponents) * daRate;
	 	}
		return ((int) Math.round(da));
	}
		
	
/*	public boolean isValidA(Do2 do2){
		if (do2.getColumnA() != null && COL_7A_LIST.contains(do2.getColumnA())){
			return true;
		}
		return false;
	}*/
	/*
	
	public boolean isValidC(Do2 do2) {
		if(do2.getColumnC() == null || do2.getColumnC().length() == 0){
			return false;
		}
		if ((do2.getColumnA().equals(Do2Manager.DISCH)
				|| do2.getColumnA().equals(Do2Manager.DEATH)
				|| do2.getColumnA().equals(Do2Manager.BATTLE)
				|| do2.getColumnA().equals(Do2Manager.INVALD)) && 
				(do2.getColumnC() != null && (Integer.parseInt(do2.getColumnC()) <= Constants.MAX_ENCASH_DAYS))) {
			return true;
		}
		return false;
	}
	*/
	
/*public boolean isValidC(Do2 do2) {
		
		if (do2.getColumnA().equals("LTC")) {
			int colc = 0;
			int do2fromdateyear = 0;
			
		/*	Date xDate = do2.getFromDate();
			Calendar yy = new GregorianCalendar();
			yy.setTimeInMillis(xDate.getTime());
			do2fromdateyear = yy.get(Calendar.YEAR);*/
			
			
		/*	 do2fromdateyear = do2.getFromDate().getYear();
			 System.out.println("----year---:" + do2fromdateyear);
			if (do2.getColumnC()!=null) {
				colc = Integer.parseInt(do2.getColumnC());
			    if (do2fromdateyear==colc)	{
			    	return true;
			    }
			}
		}*/
		/*
		if(do2.getColumnC() == null || do2.getColumnC().length() == 0){
			return false;
		} */
	/*	if ((do2.getColumnA().equals(Do2Manager.DISCH)
				|| do2.getColumnA().equals(Do2Manager.DEATH)
				|| do2.getColumnA().equals(Do2Manager.BATTLE)
				|| do2.getColumnA().equals(Do2Manager.INVALD)) && 
				(do2.getColumnC() != null && (Integer.parseInt(do2.getColumnC()) <= Constants.MAX_ENCASH_DAYS))) {
			return true;
		}
		return false;
	}*/
	public boolean isValidC(Do2 do2) {
		if(do2.getColumnC() == null || (do2.getColumnC()!=null && do2.getColumnC().trim().length() == 0)){
			return false;
		}
		if ((do2.getColumnA().equals(Do2Manager.DISCH)
				|| do2.getColumnA().equals(Do2Manager.DEATH)
				|| do2.getColumnA().equals(Do2Manager.BATTLE)
				|| do2.getColumnA().equals(Do2Manager.INVALD)) && 
				(do2.getColumnC() != null && (Integer.parseInt(do2.getColumnC()) <= Constants.MAX_ENCASH_DAYS))) {
			return true;
		}
		
		
		if(do2.getColumnA().equals("LTC")) {
			int do2fromdateyear = 0;
			int colc = 0;
			Date xDate = do2.getFromDate();
			Calendar yy = new GregorianCalendar();
			yy.setTimeInMillis(xDate.getTime());
			do2fromdateyear = yy.get(Calendar.YEAR);
			
			if (do2.getColumnC()!=null) {
				colc = Integer.parseInt(do2.getColumnC());
			    if (do2fromdateyear==colc)	{
			    	return true;
			    }
			}
		}
		return false;
	}
	/*
	public boolean isValidRefDo2(Do2 do2) {
		
		if (!do2.getColumnA().equals("LTC")) {
			Do2 refDo2 = Utils.getReferenceDo2(do2);
			if (refDo2 != null && do2.getColumnA() != null) {
				if (refDo2.getDescription().equals(do2.getColumnA()) && refDo2.getFromDate().equals(do2.getFromDate())) {
					return true;
				} 
			} 
		}
		
			
		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		if (refDo2List!=null && refDo2List.size()>0) {
			for (Do2 d : refDo2List) {
				if (do2.getColumnA().equals("LTC")) {
					if (d.getDescription()!=null && LTC_PERMISSIBLE_LEAVE.contains(d.getDescription().trim())) {
						return true;
					}
				}
				public boolean isValidRefDo2(Do2 do2) {
					
					if (!do2.getColumnA().equals("LTC")) {
						Do2 refDo2 = Utils.getReferenceDo2(do2);
						if (refDo2 != null && do2.getColumnA() != null) {
							if (refDo2.getDescription().equals(do2.getColumnA()) && refDo2.getFromDate().equals(do2.getFromDate())) {
								return true;
							} 
						} 
					}
					
					System.out.println ( " DO2  ddddd " +do2.getFromDate()  );
						
					List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
					System.out.println ( " DO2 mmmmmmmmmmm " +refDo2List.size()  );
					if (refDo2List!=null && refDo2List.size()>0) {
						for (Do2 d : refDo2List) {
							if (do2.getColumnA().equals("LTC")) {
								if (d.getDescription()!=null && LTC_PERMISSIBLE_LEAVE.contains(d.getDescription().trim())) {
									if (!do2.getFromDate().before(d.getFromDate()) && !do2.getFromDate().after(d.getToDate()))  {
										System.out.println ( " LLLLLL ");
										return true;	// added based on the observation made by DSC Kannur
									} 						
								}
							}
							
						}
					}		 
					return false;
				}
			}
		}		 
		return false;
	} */
	
	
	public boolean isValidRefDo2(Do2 do2) {
		
		if (!do2.getColumnA().equals("LTC")) {
			Do2 refDo2 = Utils.getReferenceDo2(do2);
			if (refDo2 != null && do2.getColumnA() != null) {
				if (refDo2.getDescription().equals(do2.getColumnA()) && refDo2.getFromDate().equals(do2.getFromDate())) {
					return true;
				} 
			} 
		}
		
		System.out.println ( " DO2  ddddd " +do2.getFromDate()  );
			
		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		System.out.println ( " DO2 mmmmmmmmmmm " +refDo2List.size()  );
		if (refDo2List!=null && refDo2List.size()>0) {
			for (Do2 d : refDo2List) {
				if (do2.getColumnA().equals("LTC")) {
					if (d.getDescription()!=null && LTC_PERMISSIBLE_LEAVE.contains(d.getDescription().trim())) {
						
						Date refDo2FrDatesuffixDate = DateManager.getNextDate(DateManager.getNextDate(d.getFromDate()));
						Date refDo2FrDateprefixDate = DateManager.getPreviousDate(DateManager.getPreviousDate(d.getFromDate()));
						
						Date refDo2ToDatesuffixDate = DateManager.getNextDate(DateManager.getNextDate(d.getToDate()));
						Date refDo2ToDateprefixDate = DateManager.getPreviousDate(DateManager.getPreviousDate(d.getToDate()));
					
				//		System.out.println ( " ref do2 fr date suffix date " +refDo2FrDatesuffixDate);
				//		System.out.println ( " ref do2 fr date pefix date " +refDo2FrDateprefixDate);
							
						if (!do2.getFromDate().before(refDo2FrDateprefixDate) && !do2.getFromDate().after(refDo2FrDatesuffixDate) &&
								 !do2.getFromDate().after(d.getToDate())) {
							System.out.println ( " LLLLLL 11111");
							return true;	// added based on the observation made by DSC Kannur
							} 		
						
						if (!do2.getFromDate().before(refDo2ToDateprefixDate) && !do2.getFromDate().after(refDo2ToDatesuffixDate) &&
								 !do2.getFromDate().after(d.getToDate())) {
							System.out.println ( " LLLLLL 22222");
							return true;	// added based on the observation made by DSC Kannur
							} 	
						
					 
						if (!do2.getFromDate().before(d.getFromDate()) && !do2.getFromDate().after(d.getToDate()))  {
							System.out.println ( " LLLLLL 33333");
							return true;	// added based on the observation made by DSC Kannur
						} 						
					}
				}
				
			}
		}		 
		return false;
	}
	
	
}
