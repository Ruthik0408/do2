package itsdc.dolphin.do2.auditcage;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

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
 * @author IT&SDC Secunderabad
 * 
 */

@RestController
public class Reduce extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;
	Do2 refDo2;

	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public String INVALID_PERIOD = "AU:No. of Days in Col.7B not matching with days in MILRI DOII.\n ";
	static public String INVALID_REDUCE = "AU:No. of Days in Col.7C not less than No. of days in Col7B.\n ";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	private StringBuffer buffer = new StringBuffer();
	private List scList;
	private double do2Days, noofDays = 0.0;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (do2.getTranscriptionType().equals(Do2Manager.SYSTEM_GENERATED)) {
			do2.setStatus(Do2Manager.VALID);
			return true;
		}
		if (!isValidColumn7B(do2)) {
			do2.setStatus("MR");
			validity = false;
			buffer.append(INVALID_PERIOD + NOT_IN_APPENDIX_J_FORMAT);
		}
		if (!isValidColumn7C(do2)) {
			do2.setStatus("MR");
			validity = false;
			buffer.append(INVALID_REDUCE +NOT_IN_APPENDIX_J_FORMAT);
		}
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		if (validity == false) {
			 if(do2.getStatus() != null)
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
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		Do2 milriDo2 = null;
		Do2 reduceDo2 = null;
		Date reduceFromDate = null;
		if (!do2.getTranscriptionType().equals(Do2Manager.SYSTEM_GENERATED) && 
			(do2.getColumnB() != null && do2.getColumnB().equals("REP"))) {

			reduceDo2 = do2;
			reduceFromDate = DateManager.getFromDateForReduce(reduceDo2);
			List<Do2> refDo2List = Utils.getReferenceDo2List(do2);

			for (Do2 refDo2 : refDo2List) {				
				if (refDo2.getDescription().equals(Do2Manager.MILRI)) {
					milriDo2 = refDo2;
				}
			}
		}

		List<EmployeeServiceDetail> esdList = esdRepo.findByDo2(milriDo2.getId());
				
		//		Utils.getEntityManager().createQuery(
		//				"SELECT esd FROM EmployeeServiceDetail esd WHERE esd.do2=:refdo2 ")
		//		.setParameter("refdo2", milriDo2).getResultList();
		for (EmployeeServiceDetail esd : esdList) {
			esd.setToDate(DateManager.getPreviousDate(reduceFromDate));
			esdRepo.save(esd);
		//	Utils.getEntityManager().persist(esd);
		}
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		for (Do2 refDo2 : refDo2List) {
			if (do2.getColumnA() != null && refDo2.getDescription().equals(do2.getColumnA()) && 
				refDo2.getDo2Group().equals("IB") && do2.getFromDate().equals(refDo2.getFromDate())) {
				return true;
			}
		}
		return false;
	}

	public boolean isValidColumn7B(Do2 do2) {
		refDo2 = Utils.getReferenceDo2(do2);
		if (do2.getColumnB() != null && !do2.getColumnB().matches(".*\\d.*")){
			if (do2.getColumnA() != null && do2.getColumnB() != null && 
				do2.getColumnA().equals("SEVREP") && do2.getColumnB().equals("REP")){
				return true;
			}
		}
		if (refDo2 != null	&& (do2.getColumnB() != null && !do2.getColumnB().matches(".*\\d.*")) &&
			Integer.valueOf(do2.getColumnB()) == (refDo2.getTjra() * 30 + refDo2.getLra())) {
			return true;
		}
		return false;
	}

	public boolean isValidColumn7C(Do2 do2) {
		if (do2.getColumnB() != null && !do2.getColumnB().matches(".*\\d.*")){
			if (do2.getColumnB().equals("REP")){
				return true;
			}
		}
		if (do2.getColumnB() != null && do2.getColumnB().matches(".*\\d.*") && 
		    do2.getColumnC() != null && do2.getColumnC().matches(".*\\d.*") && 
		    Integer.valueOf(do2.getColumnB()) > Integer.valueOf(do2.getColumnC())) {
			return true;
		}
		return false;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		Do2 milriDo2 = null;
		Do2 rmilriDo2 = null;
		List<Do2> do2List = Utils.getReferenceDo2List(currentDo2);
		for (Do2 reduceDo2 : do2List) {
			if (reduceDo2.getDescription().equals("MILRI")) {
				milriDo2 = reduceDo2;
			}
			if (reduceDo2.getDescription().equals("RMILRI")) {
				rmilriDo2 = reduceDo2;
			}
		}
		List<EmployeeServiceDetail> esdList = esdRepo.findByDo2(milriDo2.getId());
				
		/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT esd FROM EmployeeServiceDetail esd WHERE esd.do2=:refdo2 ")
				.setParameter("refdo2", milriDo2).getResultList();*/
		for (EmployeeServiceDetail esd : esdList) {
			esd.setToDate(DateManager.getPreviousDate(rmilriDo2.getFromDate()));

		}
		return Do2Manager.SUCCESS;
	}
}