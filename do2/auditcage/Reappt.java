package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AppointmentRate;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC, Secunderabad
 * 
 */

@RestController
public class Reappt extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;
    Do2 refDo2;
    AppointmentRate appointmentRate;
    AuditCage baseAuditCage;

	private StringBuffer buffer = new StringBuffer();
	static public String INVALID_REFDO2 = "AU:Incorrect reference Do2.\n ";
	static public String INVALID_COLUMN7A= "AU:Appointment in Col.7A and appointment held by the PBOR not matching.\n";
		
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
	//	System.out.println("REAPPT REF DO2   "+do2.getRefDo2No());
	/*	if(do2.getRefDo2No()!= null ){
			if (!isValidRefDo2(do2)) {
				validity = false;
				buffer.append(INVALID_REFDO2);
			}
			if(validity == true){
				if (!isValidAppointment(do2)) {
					validity = false;
					buffer.append(INVALID_COLUMN7A);
				}	
			}
			
			
		}
		*/
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
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
			AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
			AuditCageHelper.removeAllowances(Arrays.asList(new String[] { Do2Manager.APTPAY }), newAuditCage);
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			if (employee!=null) {
				employee.setAppointmentRate(null);
				employee.setAppointmentDate(null);
		
				List<EmployeeServiceDetail> esdList = esdRepo.findByDo2ColAAndToDateNull(employee.getId(), "A", do2.getColumnA());
						
						
			/*			Utils.getEntityManager().createQuery("SELECT esd FROM EmployeeServiceDetail esd " +
												" WHERE esd.employee=:employee AND esd.status='A' AND esd.do2.columnA=:columnA " +
												" AND esd.toDate is null ORDER BY esd.fromDate DESC ")
												.setParameter("columnA", do2.getColumnA())
												.setParameter("employee", employee).getResultList();*/
				if (esdList.size()>0) {
					esdList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
				}
			}
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null && (refDo2.getDescription().equals("APPT"))) {
			return true;
		} 
		return  false;
	}
	public boolean isValidAppointment(Do2 do2) {
		Do2 refDo2 = Utils.getReferenceDo2(do2,"APPT");
		if(refDo2 !=null && refDo2.getColumnA().equals(do2.getColumnA()))
			return true;
		
			return false;
		}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		AppointmentRate apptName = apptRateRepo.findByAppointmentName(refDo2.getColumnA());
	/*	List<AppointmentRate> aptList = Utils.getEntityManager().createQuery(
				"SELECT apt FROM AppointmentRate apt WHERE apt.appointmentName =:appt ")
				.setParameter("appt", refDo2.getColumnA())
				.getResultList();*/
		/*for (AppointmentRate aptname : aptList){
			employee.setAppointmentRate(aptname);
		}*/
		 
			employee.setAppointmentRate(apptName);
		
		List<Do2> do2List = do2Repo.findByFromDateLessThanEqualToAndColAAndDescriptionAndStatus(employee.getId(),"APPT"
																			, currentDo2.getFromDate(), refDo2.getColumnA(), "V");
				
	/*			Utils.getEntityManager().createQuery(" SELECT d FROM Do2 d WHERE d.employee=:employee AND d.description= 'APPT' AND d.fromDate <= :fromdate " +
		" AND d.columnA=:column AND d.status = 'V' ORDER BY d.fromDate DESC " )
		.setParameter("employee", employee)
		.setParameter("fromdate", currentDo2.getFromDate())
		.setParameter("column", refDo2.getColumnA())
		.getResultList();*/
		if (do2List.size() >0){	
			employee.setAppointmentDate(do2List.get(0).getFromDate());
		}
		List<EmployeeServiceDetail> esdList = esdRepo.findByStatusColumnAOrderByFromDateDesc(employee.getId(), "A", refDo2.getColumnA());
				
		/*		Utils.getEntityManager().createQuery("SELECT esd FROM EmployeeServiceDetail esd " +
				" WHERE esd.employee=:employee AND esd.status='A' AND esd.do2.columnA=:columnA " +
				" ORDER BY esd.fromDate DESC ")
				.setParameter("columnA", refDo2.getColumnA())
				.setParameter("employee", employee).getResultList();*/
		
		if (esdList.size()>0) {
			esdList.get(0).setToDate(null);
		}
		
		return Do2Manager.SUCCESS;
	}
}