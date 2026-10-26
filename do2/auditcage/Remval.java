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
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Remval extends AuditCageBase implements
 Do2AuditCageProcessor, java.io.Serializable{
    Employee employee;
    static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n";
    static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	private StringBuffer buffer = new StringBuffer();
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getColumnA() != null && (do2.getColumnA().equals("F") || do2.getColumnA().equals("T") 
				|| do2.getColumnA().equals("H") || do2.getColumnA().equals("N"))){    // N added on 12.12.2025 - Not Eligible for Pension / Gratuity
			;
		} else {
		//	do2.setStatus("MR");	
			buffer.append("Col.7A should contain F/T/H/N" + NOT_IN_APPENDIX_J_FORMAT);   
			validity = false;
		}
		if (validity == false){
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}		
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		employee.setFsDueDate(null);
		employee.setFsStatus(null);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_DISCHARGE);
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(true); 
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		StatusCode sc=scRepo.findByStatusCode("S");
		employee.setFsStatus(sc);
	//	employee.setFsStatus((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'S'));
		employee.setFsDueDate(do2.getFromDate());
		empRepo.save(employee);
	 //   Utils.getEntityManager().persist(employee);
		AuditCageHelper.updateEmployeeServiceDetail(do2,Do2Manager.ESD_DISCHARGE+"");
		return Do2Manager.SUCCESS;
	}
	
	
}
