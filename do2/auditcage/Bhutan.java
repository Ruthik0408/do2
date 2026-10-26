package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.BhutanSlab;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;


@RestController
public class Bhutan extends AuditCageBase implements Do2AuditCageProcessor {
	Employee employee = null;
	EmployeeServiceDetail esd;
	private List<EmployeeServiceDetail> esdList;
	String message;
	StringBuffer buffer = new StringBuffer();
	String status = "";
	private boolean validity = true;
	Rank rank;
	GroupCategory groupCategory;
	
	int basicPay = 0;
	int bhutanSlabRate, allowBhutanRate, economyCut, freeFacility,netBhutanRate = 0;
	AuditCage baseAuditCage;
	
	private List<BhutanSlab> bhutanSlabList;
	List<Allowance> allowances = new ArrayList<Allowance>();
	List<String> stopAllowanceList = Arrays.asList("BHUTAN");
	
	static public final String NOT_YET_MUSTERED = "AU:PBOR is not yet mustered as per Employee table. \n";
	static public final String NO_BHUTAN_SLAB_RATE_AVAILABLE = "AU:Deputation audit cage not available / BHUTAN slab rate not available for basic pay. \n"; 
	static public final String NOT_ON_DEPUTATION = "AU:PBOR is not on deputation to IMTRAT/DANTAK, BHUTAN";
	static public final String RATE_NOT_AVAILABLE = "AU:Rate Not Available in Reference Table";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (employee == null)
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
		if (do2.getColumnA().trim().equals("GRANTED")) {

			if (isRecruit(do2)) {
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(NOT_YET_MUSTERED);
				validity = false;
			} else {
				if (!isPborOnBhutanDeputation(do2)) {
					do2.setStatus(Do2Manager.PENDING);
					buffer.append(NOT_ON_DEPUTATION);
					validity = false;
				}
			}
		}	
		if (!isRateAvailable(do2)){
			do2.setStatus(Do2Manager.PENDING);
			validity=false;
			buffer.append(NO_BHUTAN_SLAB_RATE_AVAILABLE);
		}
        if (!validity) {
			message = buffer.toString();
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
	
	   do2Repo.save(do2);
	    return validity;
		
	}

    public boolean isRecruit(Do2 do2) {
		if ((employee.getInitialMusteringDate() == null) && (employee.getAttestationDate() == null)) {
			return true;
		} 
		return false;
	}

	//method replaced on 23042012
    public boolean isPborOnBhutanDeputation(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		esdList =  esdRepo.findByFromDateLessThanEqual2StatusOrderByFromDateDescToDateDesc(do2.getEmployee().getId(),do2.getFromDate(),Do2Manager.DEPU_DANTAK_BHUTAN+"",Do2Manager.DEPU_IMTRAT_BHUTAN+"");
		 
		
		if (esdList.size() > 0 &&  ((esdList.get(0).getFromDate().equals(do2.getFromDate()) ||
		   (esdList.get(0).getFromDate().before(do2.getFromDate()))))) {
		
			if(esdList.get(0).getToDate()==null){		
				return true;
			} else {
				if (do2.getFromDate().after(esdList.get(0).getToDate())){					
			    	return false; 
			     } else {			    	
			    	 return true;
			     }
			}
		     
		
		}  
			return false;
	 	}
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {	
		
	     	return newAuditCage;
	}
	
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		int newBhutanRate = 0;
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage); 		
 		if (newAuditCage.getDo2().getColumnA().trim().equals("GRANTED")) {
 			newBhutanRate = AuditCageHelper.getBhutanRate(do2);
	    	AuditCageHelper.updateAuditCageAllowance(newAuditCage,"BHUTAN", newBhutanRate);
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
	    }  
	    if (newAuditCage.getDo2().getColumnA().equals("CEASED")) {
		    AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
		}
	 	return Do2Manager.SUCCESS;
	}	

	public boolean isRateAvailable(Do2 do2){
		boolean validity =true;	
		AuditCage depuAuditCage = AuditCageHelper.getTfrdepAuditCage(do2);
		if (depuAuditCage == null){
	    	validity = false;
	    	return validity;
	    } 
	/*	Auth : CGDA letter No.AT/I/O/74/IV dated 30.09.2014 intimating  removal of slab deduction
		       from BCA with effect from 1.9.2008 (GOI MOD lr No.4(3)/2011/D(Pay/Services) dt.5.9.2014)*/
		if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			return validity;
		}
		Rank rank = Utils.getPborRank(do2.getArmyNo(), depuAuditCage.getDo2().getFromDate(),"PAYFIX");
		GroupCategory groupCategory = Utils.getGroupCategory(depuAuditCage.getDo2(), "CUR");
		String group = "";		
		if (groupCategory !=null){
			group=groupCategory.getSoldierGroup();
		}
	    if (depuAuditCage.getDo2().getFromDate().after(DateManager.getSixthCpcDate())){
			basicPay = depuAuditCage.getOldBasicPay();
		} else {
		  	basicPay = depuAuditCage.getBasicPay();
		}
	     List<BhutanSlab> bhutanSlabList = bhutanSlabRepo.findByRankIdSgAndBp(rank.getId(), group, basicPay);
	     
			
		if (bhutanSlabList.size()==0)
			   validity =false;
		return validity;		
		
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}	
}


