package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
@RestController
public class Depsag extends AuditCageBase implements
		 Do2AuditCageProcessor {
	Employee employee = null;
	private Rank rank;
	String message;
	StringBuffer buffer = new StringBuffer();
	String status = "";
	boolean validity = true;
	 
	List<Allowance> allowances = new ArrayList<Allowance>();
	
	List<String> stopAllowanceList = Arrays.asList("DEPSAG", "DEPA");
	
	static public final String NOT_ADMISSIBLE_RECRUIT= "AU:Not Admissible to Recruit. \n";
	
	@Override
	public boolean isValid(Do2 do2) {
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
	    if (do2.getColumnA().trim().equals(Do2Manager.CEASED)) {
	    	validity = true;
	    } else if (!isRecruit(rank)) {
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
		
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		 	
			AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage); 
			double depaAmount = 0;
		    double depsagAmount = 0; 
		    
			//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
			rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		    depsagAmount = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate(), rank).get(0).getRateAmount();		 
		 
			int basicPay = 0;
 		    int classPay = 0;

 		    if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
 		    	basicPay = baseAuditCage.getOldBasicPay() == null ? 0 :baseAuditCage.getOldBasicPay();
 		    } else {
 		    	basicPay = baseAuditCage.getBasicPay() == null ? 0 : baseAuditCage.getBasicPay();
 		    }
		     		 
 		    
 		   if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
 				basicPay = baseAuditCage.getBasicPay() == null ? 0 : baseAuditCage.getBasicPay();
 				classPay = baseAuditCage.getClassPay() == null ? 0 : baseAuditCage.getClassPay();
 			} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
 				basicPay = baseAuditCage.getOldBasicPay() == null ? 0 :baseAuditCage.getOldBasicPay();
 			} else {
 				basicPay = baseAuditCage.getBasicPay() == null ? 0 :baseAuditCage.getBasicPay();
 			}
		//    classPay = baseAuditCage.getClassPay() == null ? 0 : baseAuditCage.getClassPay();
		    depaAmount = Math.round((basicPay + classPay)*0.1);
		 	
		 	if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
		 		AuditCageHelper.updateAuditCageAllowance(newAuditCage, "DEPSAG",depsagAmount);
		 		AuditCageHelper.updateAuditCageAllowance(newAuditCage,"DEPA", depaAmount);
	 
		 	} else {	 
		 	  	AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
		 	}	
		 	
			return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isRecruit(Rank rank) {
		return (rank != null && rank.getRankCode().compareTo(Do2Manager.RECRUIT) > 0);	}
	
}

