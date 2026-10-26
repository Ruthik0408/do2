package itsdc.dolphin.do2.auditcage;

import java.util.Date;
import java.util.Calendar;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.Unit;
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
public class Milri extends AuditCageBase implements
		Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;
	Do2 refDo2;

	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2. \n ";
	static public String INVAID_TO_DATE = "VL:To Date is incorrect. \n";
	static public String INVALID_FROM_DATE = "AU:MILRI From Date is not after reference Do2 From Date.\n";
	private StringBuffer buffer = new StringBuffer();
	private double dayCount = 0.0;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
        employee = Utils.getCurrentEmployee(do2.getArmyNo());
        refDo2 = Utils.getReferenceDo2(do2);
		if (do2.getToDate() != null) {
			if (!isValidTodate(do2)) {
				validity = false;
				buffer.append(INVAID_TO_DATE);
			}
		} else {
			validity = false;
			buffer.append(INVAID_TO_DATE);
		}

		if (!(validity)) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			
			do2.setStatus(Do2Manager.VALID);
			if(employee.getCurrentGspayCode() !=null && !employee.getCurrentGspayCode().equals("0") 
					&& do2.getPunishment().equals("CM")){
				         if(isdo2GenerationRequired(do2)){
				        	 AuditCageHelper.generateForGspayDo2(do2,getGsPayGrade(do2));	 
				         }
						
					}
		} 
		do2.setReason(buffer.toString());

		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(true);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
			AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (employee.getAppointmentRate() != null) {
			AuditCageHelper.removeAllowances(Arrays.asList(new String[] { Do2Manager.APTPAY }), newAuditCage);
		}
	/*	if (employee.getCurrentGspayCode() != null && newDo2.getId() == do2.getId()) {
			updateEmployee(do2);
		}*/
		if (do2.getPunishment()!= null && do2.getPunishment().equals("CM")) {
			AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_NQS+"");
		}
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null) {
			if (refDo2.getDescription().equals(Do2Manager.AWL)
					|| refDo2.getDescription().equals(Do2Manager.OSL)
					|| refDo2.getDescription().equals(Do2Manager.REP)
					|| refDo2.getDescription().equals(Do2Manager.SEVREP)) {
				return true;
			}
		}

		return false;
	}

	public boolean isValidCol4(Do2 do2) {
		if (do2.getPunishment().equals("OC") && do2.getLra() > 0
				&& do2.getLra() <= 29 && do2.getTjra() == 0) {
			return true;
		} else if (do2.getPunishment().equals("CM")) {
			if (do2.getLra() <= 29 && do2.getTjra() <= 3
					|| (do2.getTjra() == 3 && do2.getLra() == 0)) {
				return true;
			}
		}
		return false;
	}

	public boolean isValidTodate(Do2 do2) {
		//Date toDate = DateManager.getRmilriToDate(do2);
		 int tjraDays =do2.getTjra();
		 int lraDays = do2.getLra();
		 
		if(do2.getTjra()==null){
			tjraDays =0;
		}
		if(do2.getLra()==null) {
			lraDays =0;
		}
		if(tjraDays==0 && lraDays==0) {
			return false;
		}
		int dayCount = tjraDays * 30 + lraDays;
		return DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate()) == dayCount;
	}

	public boolean isValidFromDate(Do2 do2) {
		if (refDo2 != null) {
			return do2.getFromDate().after(refDo2.getFromDate());
		}
		return false;

	}

	public String getGsPayGrade(Do2 do2) {
		    String columnA =" ";
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			if (employee.getCurrentGspayCode().equals("3")) {
				  columnA = "THIRD";
			} else if (employee.getCurrentGspayCode().equals("2")) {
				 columnA = "SECOND";
			} else if (employee.getCurrentGspayCode().equals("1")) {
				 columnA = "FIRST";
			}
		return columnA;
		
	}

	public void reverseUpdateEmployee(Do2 refDo2) {
		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		if (employee.getCurrentGspayCode().equals("2")) {
			employee.setCurrentGspayCode("3");
			employee.setCurrentGsPayDate(refDo2.getFromDate());
		} else if (employee.getCurrentGspayCode().equals("1")) {
			employee.setCurrentGspayCode("2");
			employee.setCurrentGsPayDate(refDo2.getFromDate());
		} else if (employee.getCurrentGspayCode().equals("0")) {
			employee.setCurrentGspayCode("1");
			employee.setCurrentGsPayDate(refDo2.getFromDate());
		}
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
	
			esdRepo.deleteFromEsdForDo2(refDo2);
		
	/*	Utils.getEntityManager().createNativeQuery(
				"DELETE FROM employee_service_detail where fk_do2 =:refDo2 ")
				.setParameter("refDo2", refDo2).executeUpdate();*/
		 
		return Do2Manager.SUCCESS;
	}
	public boolean isdo2GenerationRequired(Do2 do2){
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> do2List = do2Repo.findByEmpIdRefDo2NoDescription(employee.getId(), "FORGSP", Utils.getReferenceDo2No(do2));
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee AND d.description=:description and d.refDo2No=:refDo2No")
													.setParameter("employee",employee)
													.setParameter("description","FORGSP")
													.setParameter("refDo2No",Utils.getReferenceDo2No(do2))
													.getResultList();*/
		
		   return do2List.size()== 0;
	}

}