package itsdc.dolphin.do2.auditcage;

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
public class Recovr extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	
	 
	static public String INVALID_REFDO2 = "AU:Invalid Reference Do2. \n ";
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
		}
		do2.setStatus(Do2Manager.VALID);
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(false);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		updateBdtInfo(do2);
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null && refDo2.getDescription().equals(Do2Manager.POW)) {
			return true;
		}  
		return false;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		reverseUpdateEmployee(refDo2);
		return Do2Manager.SUCCESS;
	}

	public void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		StatusCode sc=scRepo.findByStatusCode("E");
		employee.setStatusCode(sc);
	//	employee.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'E'));
		employee.setFsDueDate(null);
		List<EmployeeServiceDetail> esdList = esdRepo.findByStatusAndFromDateLessThanEqualOrderByFromDateDesc(employee.getId(),
																					Do2Manager.ESD_NQS+"", do2.getFromDate());
				
	/*			Utils.getEntityManager()
		.createQuery("SELECT e FROM EmployeeServiceDetail e WHERE e.status=:stat "
					+ " AND e.employee=:employee AND e.fromDate <= :fromDate) ORDER BY e.fromDate DESC ")
					.setParameter("employee", employee)
					.setParameter("stat", Do2Manager.ESD_NQS)
					.setParameter("fromDate", do2.getFromDate())
					.getResultList();*/
		if(esdList.size() > 0){
			employee.setStatusEffectDate(DateManager.getNextDate(esdList.get(0).getToDate()));
		} else {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
				employee.setStatusEffectDate(employee.getEnrolmentDate());
			} else {
				employee.setStatusEffectDate(employee.getInitialMusteringDate());
			}
		}
		empRepo.save(employee);
	}
	
	public void reverseUpdateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		StatusCode sc=scRepo.findByStatusCode("E");
		employee.setStatusCode(sc);
		//employee.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'W'));
		employee.setStatusEffectDate(refDo2.getFromDate());
		employee.setFsDueDate(do2.getFromDate());
		
		empRepo.save(employee);
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}
