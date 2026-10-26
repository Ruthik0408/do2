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
import itsdc.dolphin.utils.Utils;

 

@RestController
public class Avpay extends AuditCageBase implements
		 Do2AuditCageProcessor {
	Employee employee = null;
	Rank rank = null;
	String message;
	StringBuffer buffer = new StringBuffer();
	String status = "";
	boolean validity = true;
	double avpayRate = 0;
	List<Allowance> allowances = new ArrayList<Allowance>();
	List<String> allowancesList = Arrays.asList("AVPAY");
	
	 
	
	static public final String NOT_YET_MUSTERED= "AU:PBOR is not yet Mustered. \n";
	static public final String INVALID_CATEGORY = "AU:Not valid Category for grant of Allowance. \n";
	static public final String NO_RATE_AVAILABLE = "VL:Rate not available. \n";
	static public final List<String> VALID_CATEGORY= Arrays.asList("AVFAF", "AVFARM", "AVFINP", "AVFELE", 
		 "AVFAEN", "AVFAVN", "AVNTECAE", "AVNTECAF", "AVNTECAVS","AVNTECELE","AVNTECIP");
	
	@Override
	public boolean isValid(Do2 do2) {
		validity = true;
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
		return true;
		}
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
	  	if (!isRecruit(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NOT_YET_MUSTERED);
			validity = false;
		} 

		if (!isRateAvailable(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_RATE_AVAILABLE);
			validity = false;
		}

		if (!isValidCategory(do2)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_CATEGORY);
			validity = false;
		}
	 	
		if (validity)
			do2.setStatus(Do2Manager.VALID);
			 	
		do2.setReason(buffer.toString());
	 	
		do2Repo.save(do2);
		
		return validity;
		
	}

	private boolean isValidCategory(Do2 do2) {
		   if (employee == null)
				employee = Utils.getCurrentEmployee(do2.getArmyNo());
		   
		   if (VALID_CATEGORY.contains(employee.getGroupCategory().getSoldierCategory()) || 
				((employee.getPreviousGroupCategory() !=null ) && 
						VALID_CATEGORY.contains(employee.getPreviousGroupCategory()
						.getSoldierCategory())) || Utils.getPao().getPaoCode().equals("88")) {
			
					return true;
			}
		   return false;
	}
	
	public boolean isRecruit(Do2 do2) {
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		return ((rank != null) && (rank.getRankCode().compareTo(Do2Manager.RECRUIT) > 0)); 
	}	

	public boolean isRateAvailable(Do2 do2) {
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
	 	allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate(),rank);
	 	return  (allowances.size() > 0);
	}	
	

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}
	
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {		
		
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");	
	    		
		AuditCageHelper.copyCurrentAllowances(newAuditCage,baseAuditCage); 
		if (do2.getColumnA().equals(Do2Manager.GRANTED)){
	  		AuditCageHelper.updateAuditCageAllowance(newAuditCage,do2.getDescription(),
			Utils.getApplicableAllowanceRates(do2.getDescription()
			.toUpperCase().trim(), do2.getFromDate(),do2.getFromDate(), rank)
			.get(0).getRateAmount());
		}  
		
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			List<String> stopAllowanceList = Arrays.asList(do2.getDescription());
		   	 AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
	    }
			return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

}




