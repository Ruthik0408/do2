package itsdc.dolphin.do2.auditcage;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.LpcDeputation;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

 
@RestController
public class Commn extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	Employee employee;
	boolean validity = true;
	private StringBuffer buffer = new StringBuffer();
	static final public String FS_DUE_DATE = "AU:Fs Due date already updated in Employee Table. Check the Correctness. \n";
	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
	//	if (newDo2.getId() == do2.getId() && employee.getFsDueDate() != null) {
		if (do2.getAcceptedQe() == null && employee.getFsDueDate() != null) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(FS_DUE_DATE);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;	

	}
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(true);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_DISCHARGE+"");
		/*if (do2.getDescription().equals(Do2Manager.COMMN)){
	        updateLpcDeputation(do2);		
		}*/
		updateBdtInfo(do2);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		employee.setFsDueDate(null);
		employee.setFsStatus(null);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_DISCHARGE);
		if (refDo2.getFromDate().after(DateManager.getCgguardDeputationDate())) {
			if (Do2Manager.getValidStopPayDeputationFromAug2016().contains(refDo2.getColumnA())){
				lpcDepRepo.deleteFromLpcDepForDo2(refDo2.getId());
				 	
			}
		} else {
			if (Do2Manager.getValidStopPayDeputation().contains(refDo2.getColumnA())){
				lpcDepRepo.deleteFromLpcDepForDo2(refDo2.getId());		
			}
		}
		
		return Do2Manager.SUCCESS;
	}

	public void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getDescription().equals(Do2Manager.COMMN)){
			
			employee.setFsStatus(scRepo.findByStatusCode("X"));
			employee.setFsDueDate(do2.getFromDate());
		} else if (do2.getDescription().equals(Do2Manager.DISMIS)){
			employee.setFsStatus(scRepo.findByStatusCode("S"));
			employee.setFsDueDate(do2.getFromDate());
		} else if (do2.getDescription().equals(Do2Manager.POW)){
			employee.setFsStatus(scRepo.findByStatusCode("W"));
		}
		empRepo.save(employee);
	}
 
	public void updateLpcDeputation(Do2 do2) {
		LpcDeputation lpcDeputation = new LpcDeputation();
		lpcDeputation.setEmployee(do2.getEmployee());
		lpcDeputation.setDo2(do2);
		lpcDeputation.setUsr(do2.getUsr());
		if (do2.getUsr() != null){
			lpcDeputation.setUsr(do2.getUsr());
			Utils.setActionDate(do2.getUsr(), lpcDeputation);
		}
		lpcDeputation.setRecordStatus("V");
		lpcDeputation.setCreatedAt(DateManager.getCurrentTimestamp());
		lpcDeputation.setApproved(false);
		lpcDeputation.setLpcType("O");
		lpcDeputation.setArmyNo(do2.getArmyNo());
		lpcDeputation.setCheckDigit(do2.getCheckDigit());
		lpcDepRepo.save(lpcDeputation);
	}
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}