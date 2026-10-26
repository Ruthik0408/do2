package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;
 

/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:27-08-2009
 */

// PAEXMT : exempted from attending para refreshment course - due to sickness or otherwise - col 7(a) should be space or SICK
// make a note in the corresponding para Year (1st July to 30th June) for regulating para  pay and para reserve pay.
//PAJUMP: jumps during tactical exercises. Jumped during tactical exercise for continuance of para pay.
// make a note in the corresponding para year
//PARA : 1) PBOR should be posted to para unit.
//		2) if Granted. a) the PBOR should have attended Para refresher course during the year including minimum of two jumps.
// PAJUMP OR PAEXMT. IF PAEXMT the PBOR should complete Refresher Course (PARARC) before 30th september of the following training year.
// if the training year has already been completed with reference to from date stop the allowance from 1st July of the next training year.
//PARAR: Admissible to PBORS posted to non-para units as parachute reservists. same as PARA
//PARARC : processing as PAEXMT. Attending of Para Refresher Course for continuance of para / para reserve pay.
@RestController
public class Para extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
	private boolean validity = true;
    
	StringBuffer buffer = new StringBuffer();
	Employee employee;
	Rank rank;
	List<Allowance> allowances;
	
	static public final String NOT_ADMISSIBLE_RECRUIT = "AU:Not admissible to Recruit. \n";
	static public final String NOT_ON_DEPUTATION = "AU:PBOR is not on deputation to UNMISSION";
	//static public final String IS_NOT_PARA_UNIT = " PBOR not serving in a Para Unit. \n";

	
	@Override
	public boolean isValid(Do2 do2) {
	   	
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
	    		buffer.append(NOT_ADMISSIBLE_RECRUIT);
	    		validity = false;
	    	} 

	    	if (!isRateAvailable(do2)) {
	    		do2.setStatus(Do2Manager.PENDING);
	    		buffer.append(Do2Manager.RATE_NOT_AVAILABLE);
	    		validity = false;
	    	}

	    	/*if (!isPborOnUNMissionDeputation(do2)) {
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(NOT_ON_DEPUTATION);
				validity = false;
			}*/
	    
	    if (validity) {
	    	do2.setStatus(Do2Manager.VALID);
		}
	    do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	public boolean isRecruit(Do2 do2) {
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		return rank != null && !rank.getRankCode().equals(Do2Manager.RECRUIT); 
	}	

	public boolean isRateAvailable(Do2 do2) {
		allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate());
		return  (allowances.size() > 0);
	}	

	 public boolean isPborOnUNMissionDeputation(Do2 do2) {
			Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
			List<EmployeeServiceDetail> esdList =  esdRepo.findByFromDateLessThanEqualAndStatus(employee.getId(),
					do2.getFromDate(),Do2Manager.DEPU_UNMISSION+"");
					
					
		/*			Utils.getEntityManager().createQuery(
			"select esd from EmployeeServiceDetail esd where esd.employee=:employee AND esd.fromDate<=:fromDate " +
			"and esd.status=:status order by esd.fromDate desc,esd.toDate desc")
			.setParameter("employee", employee)
			.setParameter("status", Do2Manager.DEPU_UNMISSION)
			.setParameter("fromDate", do2.getFromDate())
			.getResultList();*/
			
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
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		
	   if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
		   List<String> stopAllowanceList = Arrays.asList(new String[] {"COMNDO"});
		   AuditCageHelper.removeAllowances(stopAllowanceList,newAuditCage);
		   allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate());
		   if(allowances.size() >0)
			AuditCageHelper.updateAuditCageAllowance(newAuditCage,do2.getDescription(),allowances.get(0).getRateAmount());
	 
		} else {		
			List<String> stopAllowanceList = Arrays.asList(new String[] {do2.getDescription()});
			AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
		}

		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
}
