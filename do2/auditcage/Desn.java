package itsdc.dolphin.do2.auditcage;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
@RestController
public class Desn extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	 
	Employee employee;
	private StringBuffer buffer = new StringBuffer();
	private String message;
	private boolean validity = true;
	static final public String FS_STATUS_CODE = "AU: Status code not effective in Employee Table. Check the Correctness.\n";
	static final public String INCORRECT_REF_DOII = "AU:Reference DOII of corresponding OSL or AWL occurrence not found.\n";
	static final public String INVALID_TODATE = "AU: To date is before From Date\n";
 
	@Override
	public boolean isValid(Do2 do2) {
		validity=true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo(), do2.getCheckDigit());		
		if (do2.getAcceptedQe() == null &&  employee.getStatusCode().getId() > 3  && do2.getFromDate().after(employee.getStatusEffectDate())) {
			validity = false;			
			buffer.append(FS_STATUS_CODE);
		}

		if (do2.getToDate()!=null) {
			if (!isValidToDate(do2)) {
				validity=false;
				buffer.append(INVALID_TODATE);
			}
		}


		/*if (do2.getRefDo2No() != null) {
			if (!isValidRefDo2(do2)) {
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(INCORRECT_REF_DOII);
				validity = false;
			} else {
				
				validity = true;
			}

		}*/
		if (validity == false){
			do2.setReason(buffer.toString());
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
		
		do2Repo.save(do2);
		return validity;
		
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
		AuditCageHelper.removeAllowances(newAuditCage);
		updateBdtInfo(do2);

		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_NQS+"");

		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

	
		updateEmployee('E', currentDo2);

	    List<Do2> do2List = Utils.getReferenceDo2List(refDo2);
	    for(Do2 do2 : do2List){
	    	if(do2.getDescription().equals(Do2Manager.OSL) || do2.getDescription().equals(Do2Manager.AWL)){
	    		 List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(do2, Do2Manager.ESD_NQS+"");
	    		 if(esdList.size() >0){
	    			 esdList.get(0).setToDate(null);
	    			 esdRepo.save(esdList.get(0));
	    		 }
	    			 
	    	}
	    }
	    
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,Do2Manager.ESD_NQS);
	

		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {

		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		for (Do2 refDo2 : refDo2List) {
			if (refDo2.getDescription().equals("OSL")
					|| refDo2.getDescription().equals("AWL")) {
				return true;
			}
		}

		return false;
	}

	public void updateEmployee(char statusChar, Do2 do2) {

	/*	StatusCode sc = (StatusCode) Utils.getEntityManager().createQuery(
				"select sc from StatusCode sc where sc.statusCode=:scode")
				.setParameter("scode", statusChar).getSingleResult();*/
		employee = Utils.getCurrentEmployee(do2.getArmyNo(), do2
				.getCheckDigit());
        employee.setStatusCode(scRepo.findByStatusCode(statusChar+""));
	//	employee.setStatusCode(sc);

		if (do2.getDescription().equals(Do2Manager.CANCEL)) {
			employee.setStatusEffectDate(employee.getEnrolmentDate());
		} else {
			employee.setStatusEffectDate(do2.getFromDate());
		}
		
		empRepo.save(employee);
	}
	public void updateBdtInfo(Do2 do2){
		updateEmployee('D', do2); // assumed that ESD is updated w.r.t. OSL Do2.
	}
	
	public boolean isValidToDate(Do2 do2) {
		if (DateManager.isFromDateAfterToDate(do2.getFromDate(), do2.getToDate())) {
			System.out.println ( " Do2 from date to date bbbbbbb " +do2.getFromDate()   +" " +do2.getToDate()  +" " +do2.getDescription());
			return false;
		}
		return true;
	}
}
