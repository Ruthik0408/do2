package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC,Secunderabad
 * 
 */

@RestController
public class Resgsp extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	static public final String JCO = "AU:PBOR is a JCO. \n";
	static public final String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public final String INVALID_FORGSP = "AU:Invalid FORGSP - more Red Ink entires in last 3 years/1 year/less than 3 months.\n";
	static public final String RATE_NOT_AVAILABLE = "AU:Rate not available in the Allowance Rate table. \n";

	private StringBuffer buffer = new StringBuffer();
	Employee employee;
	List<EmployeeServiceDetail> esd;
	Do2 refDo2;
	double noofMonths, refDoMonths;
	Date threeyrsoldDate, oneyroldDate, threemnthsoldDate;
		
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		do2.setStatus(Do2Manager.VALID);
		
		if (isJco(do2)) {
			validity = false;
			buffer.append(JCO);
		}
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (!isValidForgsp(do2)) {
			validity = false;
			buffer.append(INVALID_FORGSP);
		}
		if (!isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_AVAILABLE);
		 } else if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
	  	}
		message = buffer.toString();
		do2.setReason(message);
	
		do2Repo.save(do2);
	
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.removeAllowances(Arrays.asList(new String[] { Do2Manager.GSPAY }), newAuditCage);
		updateBdtInfo(do2);
		
		if (do2.getColumnA().equals("ALL")) {
			List<Allowance> allowances = new ArrayList<Allowance>();
			allowances = Utils.getApplicableAllowanceRates(Do2Manager.GSPAY, "THIRD",do2.getFromDate(), do2.getFromDate());
			for (Allowance allow : allowances){
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.GSPAY, allow.getRateAmount());
			}
		} else {
			List<Allowance> allowances = new ArrayList<Allowance>();
			allowances = Utils.getApplicableAllowanceRates(Do2Manager.GSPAY, do2.getColumnA(), do2.getFromDate(), do2.getFromDate());
			for (Allowance allow : allowances){
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.GSPAY, allow.getRateAmount());
			}
		}
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		reverseUpdateEmployee(currentDo2, refDo2);
		return Do2Manager.SUCCESS;
	}

	public boolean isValidForgsp(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		refDoMonths = DateManager.getNoOfMonths(refDo2.getFromDate(), do2.getFromDate());
		GregorianCalendar cal2 = new GregorianCalendar();
		cal2.setTime(do2.getFromDate());
		int day	= cal2.get(Calendar.DATE);
		int month = cal2.get(Calendar.MONTH)+1;
		int year = cal2.get(Calendar.YEAR)-3;
		int year1 = cal2.get(Calendar.YEAR)-1;
		try {
			threeyrsoldDate = DateManager.getDate(year, month, day);
			oneyroldDate = DateManager.getDate(year1, month, day);
		} catch (Exception e) {
			Utils.handleException(e);
		}
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<EmployeeServiceDetail> esdList = esdRepo.findByFromdateBetweenOrderByFromdateDesc(employee.getId(),"P",
																						threeyrsoldDate, do2.getFromDate());
				
	/*			Utils.getEntityManager()
			.createQuery("SELECT e FROM EmployeeServiceDetail e WHERE e.status='P' "
						+ " AND e.do2.employee=:employee AND (e.fromDate between :threeyroldDate AND :fromDate) ORDER BY e.fromDate DESC ")
		.setParameter("employee", employee)
		.setParameter("fromDate", do2.getFromDate())
		.setParameter("threeyroldDate", threeyrsoldDate)
		.getResultList();*/
		
		if (esdList.size() >= 1 && esdList.size() <= 2 && refDoMonths > 3){
			List<EmployeeServiceDetail> esdList1 = esdRepo.findByFromdateBetweenOrderByFromdateDesc(employee.getId(),"P",
																						oneyroldDate, do2.getFromDate());
					
					
	/*				Utils.getEntityManager()
			.createQuery("SELECT e FROM EmployeeServiceDetail e WHERE e.status='P' "
						+ " AND e.do2.employee=:employee AND (e.fromDate between :oneyroldDate AND :fromDate) ORDER BY e.fromDate DESC ")
						.setParameter("employee", employee)
						.setParameter("fromDate", do2.getFromDate())
						.setParameter("oneyroldDate", oneyroldDate)
						.getResultList();*/
			
			if (esdList1.size() == 1 && refDoMonths > 3){
				return true;
			}
		}
		return  false;
	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if  (refDo2 != null && refDo2.getDescription().equals("FORGSP")) {
			return true;
		} 
		return false;
	}

	public boolean isJco(Do2 do2){
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (Integer.valueOf(employee.getSubstantiveRank().getRankCode()) >= 50) {
			return true;
		}
		return false;
	}
	
	public boolean isRateAvailable(Do2 do2){
		if (do2.getColumnA().equals("ALL")) {
			List<Allowance> allowances = new ArrayList<Allowance>();
			allowances = Utils.getApplicableAllowanceRates(Do2Manager.GSPAY, "THIRD",do2.getFromDate(), do2.getFromDate());
			
			if (allowances.size() > 0 && allowances.get(0).getRateAmount() > 0.0){
				return true;
			}
		} else {
			List<Allowance> allowances = new ArrayList<Allowance>();
			allowances = Utils.getApplicableAllowanceRates(Do2Manager.GSPAY, do2.getColumnA(), do2.getFromDate(), do2.getFromDate());
			
			if (allowances.size() > 0 && allowances.get(0).getRateAmount() > 0.0){
					return true;
			}
		} 
		return false;
	}
	
	public void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getColumnA().equals("THIRD") || (do2.getColumnA().equals("ALL") && do2.getColumnB() != null 
				&& do2.getColumnB().equals("SICK"))){
			employee.setCurrentGspayCode("3");
		} 
		if (do2.getColumnA().equals("SECOND")){
			employee.setCurrentGspayCode("2");
		}
		if (do2.getColumnA().equals("FIRST")){
			employee.setCurrentGspayCode("1");
		}
		employee.setCurrentGsPayDate(do2.getFromDate());
	}	

	public void reverseUpdateEmployee(Do2 do2, Do2 refDo2) {
		refDo2 = Utils.getReferenceDo2(do2);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (refDo2.getColumnA().equals("THIRD") || refDo2.getColumnA().equals("ALL")){
			employee.setCurrentGspayCode("2");
		} 
		if (refDo2.getColumnA().equals("SECOND")){
			employee.setCurrentGspayCode("1");
		}
		if (refDo2.getColumnA().equals("FIRST")){
			employee.setCurrentGspayCode("0");
		}
		employee.setCurrentGsPayDate(refDo2.getFromDate());
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}	
