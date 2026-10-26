package itsdc.dolphin.do2.auditcage;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */

@RestController 
public class Reinst extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;

	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public String INVALID_PERCENTAGE = "AU:Incorrect Percentage in Col-7A.\n ";
	static public Date revised_appxj_impl_date ;
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	private StringBuffer buffer = new StringBuffer();
	Employee employee;
	Do2 refDo2;
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();		
		if (!do2.getDo2Date().before(revised_appxj_impl_date)) {
			if (do2.getColumnA() != null && 
			   (Integer.parseInt(do2.getColumnA()) >= 0 && Integer.parseInt(do2.getColumnA()) <= 100)){
				  ; 
			} else {
				validity = false;
				do2.setStatus("MR");
				buffer.append(INVALID_PERCENTAGE + NOT_IN_APPENDIX_J_FORMAT);   
			}
		}
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (validity == false) {
			if(do2.getStatus() != null)
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
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		updateBdtInfo(do2);
		/*AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		updateBdtInfo(do2);
		refDo2 = Utils.getReferenceDo2(do2);
		if(refDo2.getColumnB() != null){
			refundEncash(refDo2);
		}
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_DISCHARGE);*/
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (!do2.getDo2Date().before(revised_appxj_impl_date)) {
			if (do2.getDescription().equals("RINSER")) {
				if (refDo2 != null && (refDo2.getDescription().equals(Do2Manager.DISCH) 
						|| refDo2.getDescription().equals(Do2Manager.DISMIS)
						|| refDo2.getDescription().equals(Do2Manager.INVALD))) {  // Invalid cases added on 24.09.2025 - ASC(S) IRLA
						return true;
				}
			} else {
				if (refDo2 != null && refDo2.getDescription().equals(Do2Manager.DISMIS)) {
					return true;
				}
			}
			/*if (refDo2 != null && 
			   (refDo2.getDescription().equals(Do2Manager.DISMIS) &&
			   (refDo2.getFromDate().equals(DateManager.getPreviousDate(do2.getFromDate())) ||
				refDo2.getFromDate().equals(do2.getFromDate())))) {
				return true;
			}*/
			
			
		} else {
			/*if (refDo2 != null && 
			   (refDo2.getDescription().equals(Do2Manager.DISCH) ||
				refDo2.getDescription().equals(Do2Manager.DISMIS) &&
			   (refDo2.getFromDate().equals(DateManager.getPreviousDate(do2.getFromDate())) ||
			    refDo2.getFromDate().equals(do2.getFromDate())))) {
				return true;
			} */
			
			if (refDo2 != null && (refDo2.getDescription().equals(Do2Manager.DISCH) ||
									refDo2.getDescription().equals(Do2Manager.DISMIS))) {
						return true;
			}
			
			
		}
		return false;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		/*AuditCageHelper.updateEmployeeServiceDetail(refDo2, Do2Manager.ESD_DISCHARGE);
		if (refDo2.getDescription().equals(Do2Manager.DISCH)){
			employee.setFsStatus((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'G'));
		} else if (refDo2.getDescription().equals(Do2Manager.DISMIS)){
			employee.setFsStatus((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'S'));
		}
		employee.setStatusEffectDate(refDo2.getFromDate());
		employee.setFsDueDate(refDo2.getFromDate());*/
		return Do2Manager.SUCCESS;
	}
	/*public String refundEncash(Do2 refDo2) {
		double basicPay = 0;
		double gradePay = 0;
		double da = 0;
		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		List<AuditCage> ac = Utils.getEntityManager().createQuery(
				"select ac from AuditCage ac where ac.employee=:employee and " +
				" ac.effective=true AND ac.cancelled=false AND ac.recordStatus!='P' and ac.absentFlag=false " +
				" AND ac.fromDate<=:frDt order by ac.fromDate desc")
		.setParameter("employee", employee).setParameter("frDt",
				refDo2.getFromDate()).getResultList();
		if (ac.size() > 0) {
			basicPay = ac.get(0).getBasicPay();
			gradePay = ac.get(0).getGradePay();
			da = ac.get(0).getDa();
		double encashAdmissible = 0;
		double encashDays =0;
		encashDays =Integer.parseInt(refDo2.getColumnB());
		encashAdmissible = (double)(Math.round((((basicPay + gradePay + da) * -(encashDays))) / 30));
		Utils.addArrears(refDo2, encashAdmissible);
		
		}
		return Do2Manager.SUCCESS;
	}*/

	public void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		StatusCode sc=scRepo.findByStatusCode("E");
		employee.setFsStatus(sc);
	//	employee.setFsStatus((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'E'));
		employee.setFsDueDate(null);
		employee.setStatusEffectDate(do2.getFromDate());
		/*List<EmployeeServiceDetail> esdList = Utils.getEntityManager()
		.createQuery("SELECT e FROM EmployeeServiceDetail e WHERE e.status='N' "
					+ " AND e.do2.employee=:employee AND e.fromDate <= :fromDate) ORDER BY e.fromDate DESC ")
					.setParameter("employee", employee)
					.setParameter("fromDate", do2.getFromDate())
					.getResultList();
		if(esdList.size() > 0){
			employee.setStatusEffectDate(DateManager.getNextDate(esdList.get(0).getToDate()));
		} else {
			employee.setStatusEffectDate(employee.getInitialMusteringDate());
		}*/
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}
