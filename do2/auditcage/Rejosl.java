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
public class Rejosl extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {

	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n";
	static public String DATE_BEFORE_OSL = "VL:Do2 Date is before OSL Date. Check the Correctness.\n";
	private StringBuffer buffer = new StringBuffer();
	Do2 refDo2,rejosldo2;
    
	@Override
	public boolean isValid(Do2 do2) {

		boolean validity = true;
		System.out.println("new do2 "+newDo2.getId()+"--"+do2.getId());
		
		if(do2.getRefDo2No()!=null) {
			String [] str=Utils.getRefDo2Parts(do2.getRefDo2No());
			System.out.println("ref ------------------"+str[0]+"--"+str[1]+"--"+str[2]+"--"+str[3]);
			List<Do2> rDo2List=do2Repo.findByEmpIdStatusDo2TypeDo2NoItemNoYearAndDesc(do2.getEmployee().getId(), "V",str[0],str[1], str[2], Integer.parseInt(str[3]),"OSL");
			if(rDo2List!=null && rDo2List.size()>0)
				refDo2=rDo2List.get(0);
			
			if(refDo2!=null) {
				//valid;
			}else {
				validity=false;
				buffer.append(INVALID_REFDO2);
			}
			
		}else {
			validity=false;
			buffer.append("Reference Do2 not available.");
		}
		
		/*if (newDo2.getId()==do2.getId()) {
			if (!isValidRefDo2(do2)) {
				validity = false;
				buffer.append(INVALID_REFDO2);
			} else {
				if (do2.getFromDate().before(refDo2.getFromDate())){
					validity=false;
					buffer.append(DATE_BEFORE_OSL);
				}
			}	
		}*/		

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
		System.out.println(">>>Do2 in Rejosl is---:" + do2.getDescription()+"==="+do2.getFromDate());
		refDo2 = Utils.getReferenceDo2(do2,Do2Manager.OSL);
		System.out.println(">>>Do2 in Ref--Rejosl is---:" + refDo2.getDescription()+"=="+refDo2.getFromDate());
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
		//		Utils.getEntityManager().createQuery(
		//		"SELECT aca FROM AuditCageAllowance aca WHERE aca.auditCage=:auditCage ")
		//.setParameter("auditCage", baseAuditCage).getResultList();
		if (currentAuditCageAllowanceList.contains(allceList)){
			if (refDo2.getFromDate().before(cutOffDate)){
				AuditCageHelper.removeAllowances(allceList, newAuditCage);
			}
		}
		
		List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(refDo2);

		if (esdList.size() > 0) {
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
		refDo2 = Utils.getReferenceDo2(do2, Do2Manager.OSL);
		System.out.println("rejosl ref "+refDo2);
		if (refDo2 == null || !refDo2.getDescription().equals(Do2Manager.OSL))
			return false;
		return true;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
	
		Do2 oslRefDo2=null;
		List<Do2> oslList = Utils.getStopAllRefDo2(refDo2, Do2Manager.OSL);
		if (oslList.size() == 0) {
			//Do2 oslRefDo2 = Utils.getReferenceDo2(refDo2);
			List<Do2> do2List = do2Repo.findByDescFromDateLessThanEqualOrderByFromdateDesc(Utils.getCurrentEmployee(refDo2.getArmyNo()).getId(),
																					Do2Manager.VALID, Do2Manager.OSL,refDo2.getFromDate());
					
					
			/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.status =:status " +
					" and d.description=:description and d.employee=:employee and d.fromDate<=:fromDate order by d.fromDate desc")
							.setParameter("description",Do2Manager.OSL)
							.setParameter("fromDate", refDo2.getFromDate())
							.setParameter("employee",Utils.getCurrentEmployee(refDo2.getArmyNo()))
							.setParameter("status",Do2Manager.VALID)
							.getResultList();*/
			if (do2List.size() > 0){
				 oslRefDo2 = do2List.get(0);
			}
	                
			if (oslRefDo2 !=null){
				AuditCage oslAuditCage = Utils.getAuditCage(oslRefDo2);
				oslAuditCage.setToDate(null);
				oslAuditCage.setAbsentFlag(true);
				acRepo.save(oslAuditCage);
			//	Utils.getEntityManager().persist(oslAuditCage);	
			//	Utils.getEntityManager().flush();
			}
			
			List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(refDo2);
			if (esdList.size() > 0) {
				esdList.get(0).setToDate(null);
				esdRepo.save(esdList.get(0));
			//	Utils.getEntityManager().persist(esdList.get(0));
			}

		}
		return Do2Manager.SUCCESS;
	}
	public boolean scandesertion(Do2 do2) {
		boolean desnflag = false;
		List<Do2> desnList = do2Repo.findByFromDateLessThanEqualToAndEitherDescriptionAndStatus(Utils.getCurrentEmployee(do2.getArmyNo()).getId(),
																				Do2Manager.VALID,"DESN","REDESN",do2.getFromDate());
				
				
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
			if (d.getDescription().equals(Do2Manager.DESN) && !do2.getFromDate().before(d.getFromDate())){
				desnflag = true;
			}
			if (d.getDescription().equals(Do2Manager.REDESN) && do2.getFromDate().after(d.getFromDate())){
				desnflag = false;
			}
		}
		if (desnflag == true){
			return true;
		}
		return false;
	}

}