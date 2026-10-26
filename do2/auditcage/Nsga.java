package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 

/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:26-08-2009
 */



@RestController
public class Nsga extends AuditCageBase implements  Do2AuditCageProcessor {
	Employee employee = null;
	String message;
	private Rank rank;
	StringBuffer buffer = new StringBuffer();
	boolean validity = true;
	Date sevencpcDate = DateManager.getSeventhCpcDate();
	
	static public final String NOT_ADMISSIBLE_RECRUIT= "AU:Not admissible to Recruit.\n";
	
	List<String> stopAllowanceList;
	
	List<String> stopAllowanceListDepa =  Arrays.asList("DEPA");
	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
	    if (do2.getColumnA().equals("CEASED")) {
	        validity = true;
	    } else 
	    	if (!isRecruit(rank)) {
	    			do2.setStatus(Do2Manager.PENDING);
		    		buffer.append(NOT_ADMISSIBLE_RECRUIT);
		    		validity = false;
		   	}
	 
		if (validity) {
			do2.setStatus(Do2Manager.VALID);
		}
		
		message = buffer.toString();	
		do2.setReason(message);
		do2Repo.save(do2);
		 
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
	 
		return newAuditCage;
	}
	

	/*@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
			if(do2.getColumnA().equals("GRANTED")) {
	 		
	 		double nsgaAmount = 0;
	 		double daRate =0;
	 		double dearnessAmount=0;
			int basicPay = baseAuditCage.getBasicPay() == null ? 0 : baseAuditCage.getBasicPay();
			int dp = baseAuditCage.getDp() == null ? 0 : baseAuditCage.getDp();
			int gradePay = baseAuditCage.getGradePay() == null ? 0 : baseAuditCage.getGradePay();
			int msPay = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();
			int xGroupPay = baseAuditCage.getXGroupPay() == null ? 0 : baseAuditCage.getXGroupPay();
			int classPay = baseAuditCage.getClassPay() == null ? 0 : baseAuditCage.getClassPay();
			int dearnessAllowance = AuditCageHelper.getFullMonthDaWithoutTransport(do2, baseAuditCage);
			int oldBasicPay = baseAuditCage.getOldBasicPay() == null ? 0 : baseAuditCage.getOldBasicPay();
			int oldClassPay = baseAuditCage.getOldClassPay() == null ? 0 : baseAuditCage.getOldClassPay();
			int oldDp = (int) Math.round(oldBasicPay * 0.5);
			List<Allowance> allowances  = Utils.getApplicableAllowanceRates(newAuditCage.getDo2().getDescription(),newAuditCage.getFromDate(), newAuditCage.getFromDate());
			List<Allowance> daRates = Utils.getApplicableAllowanceRates("DA",newAuditCage.getFromDate(),newAuditCage.getFromDate());
			
			if(daRates.size() >0){
				daRate = daRates.get(0).getRateAmount();
				dearnessAmount = (oldBasicPay+oldDp) * daRate;
			}
			if(allowances.size() >0){
			 if(newAuditCage.getDo2().getFromDate().before((DateManager.getSixthCpcDate()))){
				
				nsgaAmount = Math.round((basicPay + dp + classPay + dearnessAllowance)*allowances.get(0).getRatePercentage());
	 		 } else if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
	 			
		 			nsgaAmount = (int) Math.round((oldBasicPay + oldDp + oldClassPay + dearnessAmount) * allowances.get(0).getRatePercentage());
	 		 } else {
	 			
	 			nsgaAmount = Math.round((basicPay + gradePay + msPay + xGroupPay + classPay + dearnessAllowance)*allowances.get(0).getRatePercentage());
	 			
	 		 }
		 	
	 		 AuditCageHelper.updateAuditCageAllowance(newAuditCage,newAuditCage.getDo2().getDescription(), nsgaAmount);
			}
	 	} else {	 
	 		stopAllowanceList = Arrays.asList(newAuditCage.getDo2().getDescription());
	 		AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
	 	}	     	 	

		
	 	 		  
		return Do2Manager.SUCCESS;
	}
	*/
	
	// above method commented and modified as below due to sevencpc
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
			if(do2.getColumnA().equals("GRANTED")) {
	 		
	 		double nsgaAmount = 0;
	 		double daRate =0;
	 		double dearnessAmount=0;
			int basicPay = baseAuditCage.getBasicPay() == null ? 0 : baseAuditCage.getBasicPay();
			int dp = baseAuditCage.getDp() == null ? 0 : baseAuditCage.getDp();
			int gradePay = baseAuditCage.getGradePay() == null ? 0 : baseAuditCage.getGradePay();
			int msPay = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();
			int xGroupPay = baseAuditCage.getXGroupPay() == null ? 0 : baseAuditCage.getXGroupPay();
			int classPay = baseAuditCage.getClassPay() == null ? 0 : baseAuditCage.getClassPay();
			int dearnessAllowance = AuditCageHelper.getFullMonthDaWithoutTransport(do2, baseAuditCage);
			int oldBasicPay = baseAuditCage.getOldBasicPay() == null ? 0 : baseAuditCage.getOldBasicPay();
			int oldClassPay = baseAuditCage.getOldClassPay() == null ? 0 : baseAuditCage.getOldClassPay();
			int oldDp = (int) Math.round(oldBasicPay * 0.5);
			int oldGradePay = baseAuditCage.getOldGradePay() == null ? 0 :baseAuditCage.getOldGradePay();
			
			List<Allowance> allowances  = Utils.getApplicableAllowanceRates(newAuditCage.getDo2().getDescription(),newAuditCage.getFromDate(), newAuditCage.getFromDate());
			List<Allowance> daRates = Utils.getApplicableAllowanceRates("DA",newAuditCage.getFromDate(),newAuditCage.getFromDate());
			
			if(daRates.size() >0){
				daRate = daRates.get(0).getRateAmount();
				dearnessAmount = (oldBasicPay+oldDp) * daRate;
			}
			
			 optcpcDo2For7cpcExists(do2);
						
			if(allowances.size() >0){
			 if(newAuditCage.getDo2().getFromDate().before((DateManager.getSixthCpcDate()))){
				
				nsgaAmount = Math.round((basicPay + dp + classPay + dearnessAllowance)*allowances.get(0).getRatePercentage());
	 		 } else if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
	 			
		 			nsgaAmount = (int) Math.round((oldBasicPay + oldDp + oldClassPay + dearnessAmount) * allowances.get(0).getRatePercentage());
	 		 } else  if (newAuditCage.getFromDate().before(sevencpcDate)){
	 			
	 			nsgaAmount = Math.round((basicPay + gradePay + msPay + xGroupPay + classPay + dearnessAllowance)*allowances.get(0).getRatePercentage());
	 			
	 		 } else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
	 			int xpay=0;
	 			int mspay=0;
	 			mspay = Utils.get6cpcMsPay(newAuditCage);
	 			xpay = Utils.get6cpcXGroupPay(newAuditCage);
	 			int da = (int) Math.round((oldBasicPay + oldGradePay + msPay + xGroupPay)*daRate);
	 			nsgaAmount = Math.round((oldBasicPay + oldGradePay + msPay + xpay + classPay + da)*allowances.get(0).getRatePercentage());
	 	//		nsgaAmount = Math.round((oldBasicPay + oldGradePay + msPay + xpay + classPay + dearnessAllowance)*allowances.get(0).getRatePercentage());
	 			
	 		 } else {
	 			// nsgaAmount = Math.round((basicPay + dearnessAllowance)*allowances.get(0).getRatePercentage());
	 			nsgaAmount = Math.round((basicPay)*allowances.get(0).getRatePercentage()); // DA element removed on 05.04.2018
	 		 }
		 	
	 		 AuditCageHelper.updateAuditCageAllowance(newAuditCage,Do2Manager.NSGA, nsgaAmount);
	 		 AuditCageHelper.removeAllowances(stopAllowanceListDepa, newAuditCage);
			}
	 	} else {	 
	 		stopAllowanceList = Arrays.asList(newAuditCage.getDo2().getDescription());
	 		AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
	 	}	     	 	

		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isRecruit(Rank rank) {
		return (rank != null && rank.getRankCode().compareTo(Do2Manager.RECRUIT) > 0);
	}
	
	public boolean optcpcDo2For7cpcExists(Do2 do2){
		List<Do2> do2List = do2Repo.findByDescColB(do2.getEmployee().getId(),"V", "OPTCPC","SEVENPC");
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status='V' and d.description='OPTCPC' "
					+ " AND d.columnB='SEVENPC' ").setParameter("emp",do2.getEmployee()).getResultList();*/
		
		if (do2List.size()>0){
		   	sevencpcDate = do2List.get(0).getFromDate();
		   	return true;
		}
		return false;
	}
	
	
}

	