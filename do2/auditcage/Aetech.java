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
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.Utils;
 


@RestController
public class Aetech extends AuditCageBase implements Do2AuditCageProcessor {
	Employee employee = null;
	String message;
	StringBuffer buffer = new StringBuffer();
	String status = "";
	boolean validity = true;
	Rank rank;
	List<Allowance> allowances = new ArrayList<Allowance>();
	
	static public final String NOT_YET_MUSTERED= "AU:Not yet mustered as per Employee table. \n";
	static public final String INVALID_CATEGORY = "AU:Not valid Category for Grant of Allowance. \n";
	static public final String NO_RATE_AVAILABLE = "VL:Invalid Rate. \n";
	static public final List<String> VALID_CATEGORY= Arrays.asList("AVFAF", "AVFARM", "AVFINP", "AVFELE", 
		 "AVFAEN", "AVFAVN", "AVNTECAE","AVNTECAMT", "AVNTECAF", "AVNTECAVS","AVNTECELE","AVNTECIP");
	
	@Override
	public boolean isValid(Do2 do2) {
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
	    if (do2.getColumnA().equals(Do2Manager.GRANTED)) {

			if (!isRecruit(do2)) {
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(NOT_YET_MUSTERED);
				validity = false;
			} else {

				if (!isRateAvailable(do2)) {
					do2.setStatus(Do2Manager.PENDING);
					buffer.append(NO_RATE_AVAILABLE);
					validity = false;
				}

				if (!isValidCategory(employee, do2)) {
					do2.setStatus(Do2Manager.INVALID);
					buffer.append(INVALID_CATEGORY);
					validity = false;
				}
			}
		}

		message = buffer.toString();
		do2.setReason(message);
		
		 if (validity) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
		 
		do2Repo.save(do2);
		return validity;
		
	}

	private boolean isValidCategory(Employee employee, Do2 do2) {
	   GroupCategory groupCategory = Utils.getGroupCategory(do2, "CUR"); 
	   if (VALID_CATEGORY.contains(groupCategory.getSoldierCategory())) {
		   return true;
	   } else if (groupCategory.getSoldierCategory().equals("HMT") || Do2Manager.MSTHAV_TRADE_LIST.contains(groupCategory.getSoldierCategory())){
	       if (employee.getRemusteringDate() != null) {
    		   if (VALID_CATEGORY.contains(employee.getPreviousGroupCategory().getSoldierCategory())){
    			   return true;
    		   }
	       }
	   } 
	   return false;
	}
	public boolean isRecruit(Do2 do2) {
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		return ((rank != null) && (rank.getRankCode().compareTo(Do2Manager.RECRUIT) > 0)); 
	}	

	public boolean isRateAvailable(Do2 do2) {
		allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate());
		return  (allowances.size() > 0);
	}	
		
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage,baseAuditCage); 
		 if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
				allowances = Utils.getApplicableAllowanceRates(do2.getDescription()
					.trim(), do2.getFromDate(),do2.getFromDate());
			
			AuditCageHelper.updateAuditCageAllowance(newAuditCage,do2.getDescription(),allowances.get(0).getRateAmount());
	
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








