package itsdc.dolphin.do2.auditcage;

import java.util.List;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Acquit extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	 
	String message;
	Do2 refDo2;
	Date fromDate, toDate;
	static public Date revised_appxj_impl_date ;
	static public String INVALID_REFDO2 = "AU:Incorrect Ref Do2 - does not refer to ARREST.\n ";
	static public String INVALID_FROM_DATE = "AU:From Date should be date of Arrest not the date of Judgement.\n ";
	
	
	private StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (do2.getDo2Date().before(revised_appxj_impl_date)){
			if (!isValidFromDate(do2)) {
				validity = false;
				buffer.append(INVALID_FROM_DATE);
			}	
		}
		
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(false);
		newAuditCage.setLra((int)AuditCageHelper.getLraRate(newAuditCage));
		newAuditCage.setPmha((int)AuditCageHelper.getPmhaRate(newAuditCage.getDo2(), newAuditCage));
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null) {
			AuditCage arrestAuditCage = Utils.getAuditCage(refDo2);
			List<EmployeeServiceDetail> esdList=esdRepo.findByDo2(refDo2.getId());
			if(esdList!=null && esdList.size()>0) {
				for(EmployeeServiceDetail esd:esdList) {
					esdRepo.delete(esd);
				}
			}
			//above will replace the below... requires review
			/*Utils.getEntityManager().createNativeQuery(
				"DELETE FROM employee_service_detail where fk_do2 =:refDo2 ")
					.setParameter("refDo2", refDo2).executeUpdate();*/

		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

	// TODO Auto-generated method stub

		return Do2Manager.SUCCESS;

	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		return (refDo2 != null && refDo2.getDescription().equals(Do2Manager.ARREST));
	}


	public boolean isValidFromDate(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		return (refDo2 != null && refDo2.getDescription().equals(Do2Manager.ARREST) && do2.getFromDate().equals(refDo2.getFromDate()));
		
	}



}