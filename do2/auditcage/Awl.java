package itsdc.dolphin.do2.auditcage;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.EmployeeServiceDetail;
 

 
@RestController
public class Awl extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	 
	@Override
	public boolean isValid(Do2 do2) {
		do2.setStatus(Do2Manager.VALID);
		do2.setReason(null);
		do2Repo.save(do2);
		return true;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
			newAuditCage.setTransport(0);
			newAuditCage.setAbsentFlag(true);
			return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_NQS+"");
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		/*AuditCage awlAuditCage = Utils.getAuditCage(refDo2);
	//	awlAuditCage.setAbsentFlag(false);
		Utils.getEntityManager().persist(awlAuditCage);*/
		List<EmployeeServiceDetail> esdList=esdRepo.findByDo2(refDo2.getId());
		
		if(esdList!=null && esdList.size()>0) {
			for(EmployeeServiceDetail esd:esdList) {
				esdRepo.delete(esd);
			}
		}
		 
		return Do2Manager.SUCCESS;
	}
}
