package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
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
 * @author   K.RAMADEVI & N.RAMACHANDRAN Dt.15.09.2009.
 * 
 */

@RestController
public class Redesn extends AuditCageBase implements  Do2AuditCageProcessor, java.io.Serializable {

	 
	Employee employee;
	EmployeeServiceDetail esd;
	Do2 refDo2, desnDo2,redesndo2;
	private StringBuffer buffer = new StringBuffer();
	private String message;
	private boolean validity=true;
	static final public String INCORRECT_REF_DOII = "AU:Incorrect Reference DOII - does not refer to DESN.\n";
	static public String DATE_BEFORE_DESN = "VL:Do2 Date is before DESN Date. Check the Correctness.\n";
	static public String REDESN_EXISTS = "VL:Rejoining Desertion do2 already exists with same reference do2.\n";
	static public String AFTER_FSDUE_DATE = "AU:From Date is After Fs Due Date.\n ";
    
    @Transactional
	@Override
	public boolean isValid(Do2 do2) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		refDo2 = Utils.getReferenceDo2(do2,Do2Manager.DESN);
		
		if(refDo2!=null) {
			if (newDo2.getId()==do2.getId()) {
	  			if (!isValidRefDo2(do2)) {
					do2.setStatus(Do2Manager.PENDING);
					buffer.append(INCORRECT_REF_DOII);
					validity = false;
				} else if (do2.getFromDate().before(refDo2.getFromDate())){
					validity=false;
					buffer.append(DATE_BEFORE_DESN);
				}
			}
		} else {
			buffer.append(INCORRECT_REF_DOII);
			validity = false;
		}
		
		if (do2.getEmployee().getFsDueDate()!=null) {
			if (!do2.getFromDate().before(do2.getEmployee().getFsDueDate())) {
				do2.setReason(AFTER_FSDUE_DATE);
				do2.setStatus(Do2Manager.INVALID);
				do2Repo.save(do2);
				return false;
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
		newAuditCage.setAbsentFlag(false);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		
		updateBdtInfo(do2);
		List<String> allceList = Arrays.asList("CILQ","FAA","HRA","HRAFAM");
		Date cutOffDate = new Date();
		refDo2 = Utils.getReferenceDo2(do2,Do2Manager.DESN);
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
	//			"SELECT aca FROM AuditCageAllowance aca WHERE aca.auditCage=:auditCage ")
	//	.setParameter("auditCage", baseAuditCage).getResultList();
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
				esdList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
				esdRepo.save(esdList.get(0));
			//	Utils.getEntityManager().persist(esdList.get(0));
			}
		}
		return Do2Manager.SUCCESS;
		
		/*refDo2 = Utils.getReferenceDo2(do2,Do2Manager.DESN);
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(refDo2);
		if (esdList.size() > 0) {
				esdList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
				Utils.getEntityManager().persist(esdList.get(0));
			}
			return Do2Manager.SUCCESS;*/
	}
//method replaced on 15/04/2012
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
	    
		  
        List<Do2> do2List = Utils.getReferenceDo2ListForRedesn(refDo2);
        for (Do2 do2 : do2List){
       	   if (do2.getDescription().equals("DESN"))
       		   desnDo2 = do2;
        }
	              //getting DESN do2 from reference of CANCEL of REDESN do2
        if (desnDo2 !=null){
       	  AuditCage desnAuditCage = Utils.getAuditCage(desnDo2);
       	  if (desnAuditCage !=null){
       		  desnAuditCage.setToDate(null);
       		  desnAuditCage.setAbsentFlag(true);
       		  acRepo.save(desnAuditCage);
       		//  Utils.getEntityManager().persist(desnAuditCage);
 			 // Utils.getEntityManager().flush();
       	  }
       	  	updateEmployee('D', refDo2);
       	  	List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(desnDo2);
       		if (esdList.size() > 0) {
       			esdList.get(0).setToDate(null);
       			esdRepo.save(esdList.get(0));
       	//		Utils.getEntityManager().persist(esdList.get(0));
       		}
        }
		
		
	
		return Do2Manager.SUCCESS;
	}

	public void updateEmployee(char status, Do2 do2) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
	    StatusCode sc=scRepo.findByStatusCode("E");
	    employee.setStatusCode(sc);
	//	employee.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(),"statusCode",status));
	
	 	employee.setStatusEffectDate(do2.getFromDate());
	 	
	 }

	public boolean isValidRefDo2(Do2 do2){
		
		List <Do2> refDo2List = Utils.getReferenceDo2ListForRedesn(do2);
		for (Do2 refDo2 : refDo2List){
			if (refDo2.getDescription().equals("DESN") || refDo2.getDescription().equals("AWL")
					|| refDo2.getDescription().equals("OSL")){
				return true;
			}
		}
			
		return false;
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee('E', do2);
	}
	
}
