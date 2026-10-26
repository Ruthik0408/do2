package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author T.Subba Reddy & V.Naga Prasad
 * 
 */

@RestController
public class Hosptl extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	private StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (do2.getRefDo2No() != null && do2.getRefDo2No().length() > 0 && !isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}

		message = buffer.toString();
		do2.setReason(message);

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
		
		// to be reviewed
		/*if(newDo2.getId()== do2.getId()){
			List<Arrears> arrearsList = Utils.getEntityManager().createQuery(
					"SELECT a FROM Arrears a WHERE a.employee=:employee AND a.payCode.payCode in ('ABF', 'DAILY', 'GRMA', 'ISNRA', 'LDAILY','NRA','SRA','TYOJ','TYRJ') "
							+ " AND ((:fromDate between a.fromDate AND a.toDate ) "
							+ " OR (:fromDate between a.fromDate AND a.toDate ) "
							+ " OR (a.fromDate between :fromDate AND :toDate) "
							+ " OR (a.toDate between :fromDate AND :toDate))")
							.setParameter("employee", Utils.getCurrentEmployee(do2.getArmyNo()))
							.setParameter("fromDate", fromDate)
							.setParameter("toDate",toDate).getResultList();
			for (Arrears arr : arrearsList) {
				Arrears reverse = new Arrears();
				reverse.setEmployee(employee);
				reverse.setDo2ReverseArrears(do2);
				reverse.setDo2(arr.getDo2());
				reverse.setPayCode(arr.getPayCode());
				reverse.setFromDate(arr.getFromDate());
				reverse.setToDate(arr.getToDate());
				reverse.setAmount(arr.getAmount() * -1);
				reverse.setQe(Utils.getCurrentQuarter());
				reverse.setArrestPayment(false);
				reverse.setRegularPay(false);
				Utils.getEntityManager().persist(reverse);
			}
		}		*/

		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		if (do2.getColumnA() == null) {
				List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
				for (Do2 admitDo2 : refDo2List) {
					if (admitDo2.getDescription().equals(Do2Manager.HOSPITAL) && admitDo2.getColumnA().equals("TFR")) {
						return true;
					}
					if (Do2Manager.getLeaveTypes().contains(admitDo2.getDescription())){
						return true;
					}	
					if ((admitDo2.getDescription().equals("TYOJ")) || (admitDo2.getDescription().equals("DAILY")) ||
						(admitDo2.getDescription().equals("TYRJ"))) {
						return true;
					}
				}
		} else if (do2.getColumnA().equals(Do2Manager.ADMITTED)) {
				List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
				for (Do2 admitDo2 : refDo2List) {
					if (admitDo2.getDescription().equals(Do2Manager.HOSPITAL) && admitDo2.getColumnA().equals("TFR")) {
						return true;
					}
					if (Do2Manager.getLeaveTypes().contains(admitDo2.getDescription())){
						return true;
					}	
					if ((admitDo2.getDescription().equals("TYOJ")) || (admitDo2.getDescription().equals("DAILY")) ||
						(admitDo2.getDescription().equals("TYRJ"))) {
						return true;
					}
				}
		} else	if (do2.getColumnA().equals(Do2Manager.DISCH)) {
				List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
				for (Do2 admitDo2 : refDo2List) {
					if (admitDo2.getDescription().equals(Do2Manager.HOSPITAL) && admitDo2.getColumnA().equals(Do2Manager.ADMITTED)) {
						return true;
					}
				}
		} else if (do2.getColumnA().equals("TFR")) {
			List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
			for (Do2 admitDo2 : refDo2List) {
				if (admitDo2.getDescription().equals(Do2Manager.HOSPITAL) && admitDo2.getColumnA().equals(Do2Manager.ADMITTED)) {
					return true;
				}
			}
		} 
		return false;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
}
