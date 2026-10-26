package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.BhutanSlab;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
@RestController
public class Regsl extends AuditCageBase implements
 Do2AuditCageProcessor, java.io.Serializable{
	static public String SL_NOT_FOUND="AU:Corresponding SL Not Found ";
	@Override
	public boolean isValid(Do2 do2) {
		
		if(!isSlAvailable(do2)){
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(SL_NOT_FOUND);
			do2Repo.save(do2);
			return false;
		}
		
		
		do2.setStatus(Do2Manager.VALID);
		do2Repo.save(do2);
	return true;
	}
	
	public boolean isSlAvailable(Do2 do2){
		List<Do2> do2List = do2Repo.findByFromDateBetweenAndToDateBetweenAndStatusAndDescription(do2.getEmployee().getId(),
																					Do2Manager.VALID, "SL", do2.getFromDate(), do2.getToDate());
				
		/*		Utils.getEntityManager().createQuery("select d from Do2 d " +
				" where d.employee=:emp and d.status=:status and d.description='SL' "+
			" and (:fromDate between from_date and to_date) "+
			" and (:toDate between from_date and to_date)" )
			.setParameter("emp", do2.getEmployee())
			.setParameter("fromDate",do2.getFromDate())
			.setParameter("toDate", do2.getToDate())
			.setParameter("status", Do2Manager.VALID)
			.getResultList();*/
		return do2List.size() >0;
			
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		 
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}
	
	
}
