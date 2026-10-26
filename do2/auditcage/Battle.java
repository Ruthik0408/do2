package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
 

@RestController
public class Battle extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	Employee employee;	
	private StringBuffer buffer = new StringBuffer();
	static final public String FS_DUE_DATE = "AU:Fs Due Date already updated in Employee table. Check the Correctness. \n";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = do2.getEmployee();
		Date revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
	
		if (do2.getAcceptedQe() == null && employee.getFsDueDate() != null) {
			validity = false;
			buffer.append(FS_DUE_DATE);
		} 
		
		if (do2.getDescription().equals("BATTLE")) {
			if (!do2.getDo2Date().before(revised_appxj_impl_date)){			
				if (do2.getColumnA() == null && do2.getColumnB() == null){
					;
				} else {
					validity = false;
					buffer.append("Col-7A and Col-7b should be left blank");
				}
			}
			
		}
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		if (newAuditCage.getDo2().getDescription().equals("BATTLE") && 
		   (newAuditCage.getDo2().getColumnB() == null || (newAuditCage.getDo2().getColumnB() != null && !newAuditCage.getDo2().getColumnB().equals("WOUNDED")))){
			newAuditCage.setAbsentFlag(true);
		} else if (newAuditCage.getDo2().getDescription().equals("MISING")){
			newAuditCage.setAbsentFlag(true);
		}
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_DISCHARGE+"");
		updateEmployee(do2);
		return Do2Manager.SUCCESS;
	}

	

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		employee.setFsDueDate(null);
		employee.setFsStatus(null);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_DISCHARGE);
		return Do2Manager.SUCCESS;
	}

	public void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getDescription().equals("BATTLE") && 
		   (do2.getColumnB() == null || (do2.getColumnB() != null && !do2.getColumnB().equals("WOUNDED")))){
			
			employee.setFsStatus(scRepo.findByStatusCode("B"));
			employee.setFsDueDate(do2.getFromDate());
		} else if (do2.getDescription().equals("MISING")){
			employee.setFsStatus(scRepo.findByStatusCode("M"));
			employee.setFsDueDate(do2.getFromDate());
		} 
		
	}
}