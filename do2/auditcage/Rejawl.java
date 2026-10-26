package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;


@RestController
public class Rejawl extends AuditCageBase implements
		Do2AuditCageProcessor, java.io.Serializable {

	static public String INVALID_REFDO2 = "AU: Incorrect Reference Do2.\n";
	static public String DATE_BEFORE_AWL = "VL:Do2 Date is before AWL Date. Check the Correctness.\n";
	
	private StringBuffer buffer = new StringBuffer();
	Do2 refDo2,rejawldo2;

	@Override
	public boolean isValid(Do2 do2) {
	
		boolean validity = true;
		
        if (newDo2.getId() == do2.getId()) {
        	if (!isValidRefDo2(do2)) {
    			validity = false;
    			buffer.append(INVALID_REFDO2);
    		} else {
				if  (do2.getFromDate().before(refDo2.getFromDate())){
					validity=false;
					buffer.append(DATE_BEFORE_AWL);
				}
			}
        }
		
		if (!validity) {
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
		boolean desnflag = scandesertion(newAuditCage.getDo2());
		if (desnflag == false){
			newAuditCage.setAbsentFlag(false);
		}
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		List<String> allceList = Arrays.asList("CILQ","FAA","HRA","HRAFAM");
		Date cutOffDate = new Date();
		refDo2 = Utils.getReferenceDo2(do2,Do2Manager.AWL);
		Calendar currentCal = new GregorianCalendar();
		Calendar cutOffCal = new GregorianCalendar();
		currentCal.setTime(do2.getFromDate());
		int date = currentCal.get(Calendar.DATE);
		int year = currentCal.get(Calendar.YEAR);
		int month = currentCal.get(Calendar.MONTH);
		if (month+1 >= 7) {
			 cutOffCal = new GregorianCalendar(year, Calendar.JULY, 1);
			 cutOffDate.setTime(cutOffCal.getTimeInMillis());
		} else if (month+1 >= 1){
			cutOffCal = new GregorianCalendar(year, Calendar.JANUARY, 1);
			cutOffDate.setTime(cutOffCal.getTimeInMillis());
		}		
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		List<AuditCageAllowance> currentAuditCageAllowanceList = acaRepo.findAllByAuditCageId(baseAuditCage.getId());
				
			//	Utils.getEntityManager().createQuery(
		//		"SELECT aca FROM AuditCageAllowance aca WHERE aca.auditCage=:auditCage ")
		//.setParameter("auditCage", baseAuditCage).getResultList();
		if (currentAuditCageAllowanceList.contains(allceList)){
			if (refDo2.getFromDate().before(cutOffDate)){
				AuditCageHelper.removeAllowances(allceList, newAuditCage);
			}
		}
		List<EmployeeServiceDetail> esdList=null;
		if (refDo2!=null) {
			esdList = Utils.getEmployeeServiceDetail(refDo2);
		}
	
		if (esdList!=null && esdList.size() > 0) {
			if (esdList.get(0).getFromDate().equals(do2.getFromDate())) {
				esdRepo.delete(esdList.get(0));
			//	Utils.getEntityManager().remove(esdList.get(0));
			} else {
				esdList.get(0).setToDate(
						DateManager.getPreviousDate(do2.getFromDate()));
				esdRepo.save(esdList.get(0));
			//	Utils.getEntityManager().persist(esdList.get(0));
			}
		}
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		
		refDo2 = Utils.getReferenceDo2(do2,Do2Manager.AWL);
		if (refDo2 == null || !refDo2.getDescription().equals(Do2Manager.AWL))
			return false;
		return true;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		List<Do2> awlList = Utils.getStopAllRefDo2(refDo2, Do2Manager.AWL);
		if(awlList.size() == 0){ 
			Do2 awlRefDo2 = Utils.getReferenceDo2(refDo2);
		
			if(awlRefDo2 != null){
				AuditCage awlAuditCage = Utils.getAuditCage(awlRefDo2);
				
				awlAuditCage.setToDate(null);
				awlAuditCage.setAbsentFlag(true);
				acRepo.save(awlAuditCage);
			//	Utils.getEntityManager().persist(awlAuditCage);
			//	Utils.getEntityManager().flush();
				List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(refDo2);
				if (esdList.size() > 0) {
					esdList.get(0).setToDate(null);
					esdRepo.save(esdList.get(0));
				//	Utils.getEntityManager().persist(esdList.get(0));
				}
			}
		}
				return Do2Manager.SUCCESS;
	}
	
	public boolean scandesertion(Do2 do2) {
		boolean desnflag = false;
		List<Do2> desnList = do2Repo.findByFromDateLessThanEqualToAndEitherDescriptionAndStatus(Utils.getCurrentEmployee(do2.getArmyNo()).getId(),
																				Do2Manager.VALID, "DESN", "REDESN", do2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.status =:status and " +
				" (d.description='DESN' or d.description='REDESN') and d.employee=:employee and d.fromDate<=:fromDate " +
				" order by d.fromDate")
				.setParameter("fromDate", do2.getFromDate())
				.setParameter("employee",Utils.getCurrentEmployee(do2.getArmyNo()))
				.setParameter("status",Do2Manager.VALID)
				.getResultList();*/
		if (desnList == null || desnList.size() == 0){
			 return false;
		}
		for (Do2 d : desnList){
			if (d.getDescription().equals(Do2Manager.DESN)){
				desnflag = true;
			}
			if (d.getDescription().equals(Do2Manager.REDESN)){
				desnflag = false;
			}
		}
		if (desnflag == true){
			return true;
		}
		return false;
	}

}