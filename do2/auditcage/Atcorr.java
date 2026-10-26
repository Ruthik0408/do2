package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;

 
@RestController
public class Atcorr extends AuditCageBase implements
 Do2AuditCageProcessor, java.io.Serializable{

	@Override
	public boolean isValid(Do2 do2) {
		do2.setStatus(Do2Manager.VALID);
		do2Repo.save(do2);
	return true;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
	/// the following 	5 lines addes to handle the updation of LRA rate, which was not adjusted at the time of enroll do2 for directly enrolled NBSUB (PAO GUARDS KAMPTEE) updated on 26-03-2026
		if (baseAuditCage.getLra()==0) {
			 if (newAuditCage.getFromDate().equals(newAuditCage.getEmployee().getEnrolmentDate()) && (newAuditCage.getFromDate().equals(newAuditCage.getEmployee().getNbsubDate()))) {
				 newAuditCage.setLra((int)AuditCageHelper.getLraRate(newAuditCage));
			 }
		}
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}
	
	
}
