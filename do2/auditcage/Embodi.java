package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 
/**
 * 
 * author : A J NORONHA, IT & SDC (Secunderabad)
 * 
 */
@RestController
public class Embodi extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	Employee employee = null;
	AuditCage disembodiAtCage=null;
	static final String NOT_TA = "AU:PBOR not on TA strength. \n";
    	
	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		boolean validity = true;
		StringBuffer buffer = new StringBuffer();
		do2.setStatus(Do2Manager.VALID);
		
		if (!(isTaPbor(do2))) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(NOT_TA);
		}
		
		do2.setReason(buffer.toString());
		
		do2Repo.save(do2);
		
	    return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		List<Do2> disembodiDo2List = Utils.getReferenceDo2ForDisembodiment(newAuditCage.getDo2(),"DISEMB");
		if (disembodiDo2List.size()>0){
			disembodiAtCage = Utils.getAuditCage(disembodiDo2List.get(0));			
		}
		newAuditCage.setAbsentFlag(false);
		newAuditCage.setDeputationFlag(false);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (disembodiAtCage !=null && !disembodiAtCage.getFromDate().equals(do2.getFromDate())){
			List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
					
		//			Utils.getEntityManager().createQuery(
	//		"SELECT aca FROM AuditCageAllowance aca WHERE aca.auditCage=:auditCage ")
	//		.setParameter("auditCage", newAuditCage).getResultList();
			for (AuditCageAllowance aca : newAuditCageAllowanceList) {		
				if (aca.getPayCode().getPersonal() == false ) {	
					acaRepo.delete(aca);
				//	 Utils.getEntityManager().remove(aca);
				}
			}
		//	Utils.getEntityManager().flush();
		}
		List<EmployeeServiceDetail> esdList = esdRepo.findByEmployeeStatusAndDo2DescriptionAndToDateNull(employee.getId(), "DISEMB",Do2Manager.ESD_NQS+"");
		
				
	/*			Utils.getEntityManager().createQuery(
			"SELECT esd FROM EmployeeServiceDetail esd WHERE esd.do2.armyNo=:ano AND "	+ 
			" esd.do2.description=:desc AND esd.status=:stat AND esd.toDate is null ")
				.setParameter("ano", employee.getArmyNo())
				.setParameter("desc", "DISEMB")
				.setParameter("stat", Do2Manager.ESD_NQS).getResultList();*/

		if (esdList.size() > 0) {
			if (esdList.get(0).getFromDate().equals(do2.getFromDate())) {
				esdList.get(0).setToDate(do2.getFromDate());
			} else {
				esdList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
			}
			esdRepo.save(esdList.get(0));
		//	Utils.getEntityManager().persist(esdList.get(0));
		}

		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		// commented by dnr
		/*List<Do2> do2List = Utils.getReferenceDo2ForDisembodiment(refDo2,"DISEMB");
		if (do2List.size() > 0) {
			Do2 disembodiDo2 = do2List.get(0);
			AuditCage disembodiAuditCage = Utils.getAuditCage(disembodiDo2);
			if(disembodiAuditCage !=null){
				//code added not make the absent flag true if it is base and absent flag is false
				if (disembodiAuditCage.getAbsentFlag()==false
						&& disembodiAuditCage.getDo2().getDescription().equals("BASE")){
					;
				} else {
					disembodiAuditCage.setToDate(null);
					disembodiAuditCage.setAbsentFlag(true);
				}
			}
			
			Utils.getEntityManager().persist(disembodiAuditCage);
			
		}*/
			
		
		List<EmployeeServiceDetail> esdList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(), Do2Manager.ESD_NQS+"");
				
		/*		Utils.getEntityManager().createQuery(
				"select esd from EmployeeServiceDetail esd where esd.do2.employee = :employee AND esd.status=:status order by fromDate DESC")
				.setParameter("employee", employee).
				setParameter("status", Do2Manager.ESD_NQS).getResultList();*/

		if (esdList.size() > 0) {
			esdList.get(0).setToDate(null);
 		}
		return Do2Manager.SUCCESS;
	}
	public boolean isTaPbor(Do2 do2) {
		if (employee.getRegularTaFlag().equals("T")) {
			return true;
		}
		return false;
	}

	/*public boolean isDisembodied(Do2 do2) {
		List<AuditCage> atcList = Utils.getEntityManager().createQuery(
			"SELECT atcl FROM AuditCage atcl WHERE atcl.employee=:emp AND atcl.effective=true " +
			" AND atcl.cancelled=false AND atcl.recordStatus!='P' AND atcl.fromDate <:frdt AND atcl.do2.status=:stat ORDER BY atcl.fromDate DESC")
			.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
			.setParameter("frdt", do2.getFromDate())
			.setParameter("stat", Do2Manager.VALID).getResultList();
		
		if (atcList.size() > 0) {
			if (atcList.get(0).getAbsentFlag() == true) {
				return true;
			}
		}
		return false;
	}*/
	/*public boolean isDisembodiExists(Do2 do2){
	   Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
       List<Do2> do2List = Utils.getEntityManager().createQuery(
    	 " SELECT d FROM Do2 d where d.employee=:emp and d.description=:description and " +
    	 " d.fromDate<=:fromDate and d.status =:status order by d.fromDate desc")
			   .setParameter("emp", emp)
			   .setParameter("description","DISEMB")
			   .setParameter("fromDate",do2.getFromDate())
			   .setParameter("status",Do2Manager.VALID)
			   .getResultList();
       if (do2List.size() ==0){
    	   List<Do2> baseDo2List = Utils.getEntityManager().createQuery(
   			"select d from Do2 d where d.employee=:employee AND d.status=:status AND d.description=:description and d.fromDate=:fromDate ")
   			.setParameter("description", "BASE")
   			.setParameter("employee", employee)
   			.setParameter("status",	Do2Manager.VALID)
   			.setParameter("fromDate",DateManager.getTaCutOffDate()).getResultList();
    		   
    	  if (baseDo2List.size()>0){
    		  return true;
    	  } else{
    		  return false;
    	  }
       }
       return true;   
		       											  
	}*/
	

}