package itsdc.dolphin.do2.auditcage;

import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.StatusCode;
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
public class Rejmis extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	
	 
	String message;
	static public String INVALID_REFDO2 = "Incorrect Reference Do2.\n ";
	private StringBuffer buffer = new StringBuffer();
	Employee employee;
    Do2 refDo2;
    	
    @Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();			
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

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(false);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		if (refDo2 == null) {
			refDo2 = Utils.getReferenceDo2(do2);
		}
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(refDo2);
		if (esdList.size() > 0) {
			if (esdList.get(0).getFromDate().equals(do2.getFromDate())){
				esdRepo.delete(esdList.get(0));
			//	Utils.getEntityManager().remove(esdList.get(0));
			} else {
				esdList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
				esdRepo.save(esdList.get(0));
			//	Utils.getEntityManager().persist(esdList.get(0));
			}			
		}
		updateBdtInfo(do2);
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null && refDo2.getDescription().equals(Do2Manager.MISING)) {
			return true;
		} 
		return false;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		List<Do2> misList = Utils.getStopAllRefDo2(refDo2, Do2Manager.MISING);
		if (misList.size() == 0){ 
			Do2 misRefDo2 = Utils.getReferenceDo2(refDo2);		
			List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(refDo2);
			if (esdList.size() > 0) {
				esdList.get(0).setToDate(null);
				esdRepo.save(esdList.get(0));
			//	Utils.getEntityManager().persist(esdList.get(0));
			}
		}
		reverseUpdateEmployee(refDo2);
		return Do2Manager.SUCCESS;
	}

	public void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		StatusCode sc=scRepo.findByStatusCode("E");
	//	employee.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'E'));
		employee.setStatusCode(sc);
		employee.setStatusEffectDate(do2.getFromDate());
		employee.setFsDueDate(null);
		empRepo.save(employee);
	}
	
	public void reverseUpdateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		StatusCode sc=scRepo.findByStatusCode("M");
	//	employee.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'M'));
		employee.setStatusCode(sc);
		employee.setStatusEffectDate(refDo2.getFromDate());
		employee.setFsDueDate(do2.getFromDate());
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}
