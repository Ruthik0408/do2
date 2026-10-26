package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
public class Airwca extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
 
 
	private Employee employee = null;
	String message;
	StringBuffer buffer = new StringBuffer();
	String status = "";
	boolean validity = true;
	Rank rank;
	List<Allowance> allowances = new ArrayList<Allowance>();
	static public final String NOT_YET_MUSTERED= "AU:Not yet Mustered as per Employee Table. \n";
	static public final String INVALID_CATEGORY = "AU:Not valid Category for Grant of Allowance. \n";
	static public final String NO_RATE_AVAILABLE = "VL:Invalid Rate. \n";
	static public final String NOT_ENOUGH_SERVICE= "AU:Not enough service to get the AIRWCA Allowance/No. of years service not mentioned.\n";
	static public final List<String> VALID_CATEGORY= Arrays.asList("AVFAF", "AVFARM", "AVFINP", "AVFELE", 
		 "AVFAEN", "AVFAVN", "AVNTECAE", "AVNTECAF", "AVNTECAVS","AVNTECELE","AVNTECIP");
	static public Date revised_appxj_impl_date ;
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
 
	@Override
	public boolean isValid(Do2 do2) {
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
	/*		do2.setStatus("MR");
			buffer.append("GRANTED/CEASED to be mentioned in Col-7A" + NOT_IN_APPENDIX_J_FORMAT);*/  // modified on 14.07.2025.
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
	
		employee = do2.getEmployee();
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (do2.getColumnA()==null || 
		   (do2.getColumnA()!=null && !(do2.getColumnA().equals(Do2Manager.GRANTED) || do2.getColumnA().equals(Do2Manager.CEASED)))) {
			do2.setStatus("MR");
			buffer.append("GRANTED/CEASED to be mentioned in Col-7A" + NOT_IN_APPENDIX_J_FORMAT);
			validity = false;
		}
		if (do2.getColumnA()!=null && do2.getColumnA().trim().equals(Do2Manager.GRANTED)){
			if (!do2.getDo2Date().before(revised_appxj_impl_date)){
				if (do2.getColumnB() == null){
					do2.setStatus(Do2Manager.INVALID);
					buffer.append(NOT_ENOUGH_SERVICE);
					validity = false;
				} else if (Integer.parseInt(do2.getColumnB()) < 2) {
					do2.setStatus(Do2Manager.INVALID);
					buffer.append(NOT_ENOUGH_SERVICE);
					validity = false;
				}
			} else if (do2.getColumnB() != null){
				do2.setStatus(Do2Manager.INVALID);
				buffer.append("Col-7B to be left blank");
				validity = false;				
			}
		}
	    if (do2.getColumnA()!=null && do2.getColumnA().trim().equals(Do2Manager.GRANTED)) {

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
	    
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
	
		AuditCageHelper.copyCurrentAllowances(newAuditCage,baseAuditCage); 
	
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
				AuditCageHelper.updateAuditCageAllowance(newAuditCage,do2.getDescription(), do2.getRate());
	 	} else {
				List<String> stopAllowanceList = Arrays.asList(do2.getDescription());
			    AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
	    }
	
		return Do2Manager.SUCCESS;
	}
	

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	 
	private boolean isValidCategory(Employee employee, Do2 do2) {
		  	 
		   if (VALID_CATEGORY.contains(employee.getGroupCategory()
				.getSoldierCategory())) {
			if ((employee.getRemusteringDate() == null)
					&& (employee.getInitialMusteringDate() != null)) {
				if (!do2.getFromDate().before(
						employee.getInitialMusteringDate())) {
					return true;
				}
			}
			if (employee.getRemusteringDate() != null) {
				if (!do2.getFromDate()
						.before(employee.getRemusteringDate())){
				if (VALID_CATEGORY.contains(employee.getGroupCategory()
						.getSoldierCategory())) {
				 
						return true;
					}

				}else{
					if (VALID_CATEGORY.contains(employee.getPreviousGroupCategory()
							.getSoldierCategory())){
						return true;
					}
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
		double lowerRate = 0;
		double higherRate = 0;
		allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate());
		if (do2.getRate() != null){
			double do2amount = do2.getRate();
			if (allowances.size() > 0) {
				for (int i=0; i<allowances.size(); i++) {
					if (lowerRate == 0){
						lowerRate = allowances.get(i).getRateAmount();
					} else if (lowerRate < allowances.get(i).getRateAmount()) {
						higherRate = allowances.get(i).getRateAmount();
					} else {
						higherRate = lowerRate;
						lowerRate = allowances.get(i).getRateAmount();
					}					
				}
			}
			if (do2.getDo2Date().before(revised_appxj_impl_date)){
				if (do2amount == lowerRate || do2amount == higherRate)
					return true;
			} else {
				if (do2amount == lowerRate && (do2.getColumnB() != null &&
				   (Integer.parseInt(do2.getColumnB()) > 1 && Integer.parseInt(do2.getColumnB()) <= 10))) {
					return true;
				}
				if (do2amount == higherRate && (do2.getColumnB() != null &&
				   (Integer.parseInt(do2.getColumnB()) > 10))) {
					return true;
				}
			}
		}
		return  false;
	}	
	
}

/* 
***************************************************************************************************
*/


	
		
	
