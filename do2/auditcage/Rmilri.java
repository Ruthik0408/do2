package itsdc.dolphin.do2.auditcage;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */

@RestController
public class Rmilri extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
	 
	Employee employee;
	Do2 refDo2;

	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public String AFTER_FSDUE_DATE = "AU:From Date is After Fs Due Date.\n ";
	
	@Override
	public boolean isValid(Do2 do2) {
	    /*if (newDo2.getDescription().equals("MILRI")){
	    	AuditCage previousAbsentAuditCage = Utils.getPreviousStopAllAuditCage(newDo2);
	    	if (previousAbsentAuditCage !=null && previousAbsentAuditCage.getToDate()==null){
	    		do2.setStatus(Do2Manager.PENDING);
	    		if (do2.getId()==0) {
	    		 	Utils.getEntityManager().persist(do2);
	    		} else {
	    		 	Utils.getEntityManager().merge(do2);
	    		}
	    		return false;
	    	}
	    } else {
	    	List<AuditCage> atCageList = Utils.getAbsentAuditCageBefore(do2);
	    	for(AuditCage ac : atCageList){
	    		if (ac.getDo2().getDescription().equals("MILRI")){
	    			continue;
	    		}
	    		if (ac.getAbsentFlag()==true && ac.getToDate()==null){
	    			do2.setStatus(Do2Manager.PENDING);
	    			if (do2.getId()==0) {
		    		 	Utils.getEntityManager().persist(do2);
		    		} else {
		    		 	Utils.getEntityManager().merge(do2);
		    		}
		    		return false;
	    		}
	    	}
	    }*/
		
		if (do2.getEmployee().getFsDueDate()!=null) {
			if (!do2.getFromDate().before(do2.getEmployee().getFsDueDate())) {
				do2.setReason(AFTER_FSDUE_DATE);
				do2.setStatus(Do2Manager.INVALID);
				do2Repo.save(do2);
				return false;
			}
		}
		do2.setStatus(Do2Manager.VALID);
		do2Repo.save(do2);
		return true;
	}
 
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
	    newAuditCage.setAbsentFlag(false);
		return newAuditCage;
	} 
	
	/*	Commented and introduced the following method on 23.07.2021 for AMC Dismissal case
   
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo()); 
		
		if(employee.getStatusCode().getId() < 5) {
			
		
		List<Do2> do2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:employee and (d.description in (:description) "
				+ " OR d.description in (:descriptionFs)) and d.fromDate <= :frdt and "
				 + " d.status='V' order by d.fromDate desc")
				.setParameter("description", Do2Manager.DISCHARGE_TYPES)
				.setParameter("descriptionFs", Do2Manager.FS_DISCH_DESCRIPTIONS)
				.setParameter("frdt", newAuditCage.getFromDate())
				.setParameter("employee", employee)
				.getResultList();
		if (do2List.size() > 0) {
			    newAuditCage.setAbsentFlag(true);
		}else {
			 newAuditCage.setAbsentFlag(false);
			}
		}
		return newAuditCage;
	}
*/	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		
		List<Do2> do2List = Utils.getReferenceDo2List(do2);
		
		if (do2List!=null && do2List.size()>0) {  // added on 14.07.2023
			for (Do2 milriDo2 : do2List){
				if (milriDo2.getDescription().equals("MILRI")){
					refDo2 = milriDo2;
				}
			}
			if (refDo2!=null) {
				List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(refDo2);
				for(EmployeeServiceDetail esd : esdList){
					esd.setToDate(DateManager.getPreviousDate(do2.getFromDate()));
					esdRepo.save(esd);
				//	Utils.getEntityManager().persist(esd);
				}
			}				
		}	
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
	
        refDo2 = Utils.getReferenceDo2(do2, "MILRI");
	    return refDo2 != null;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
}