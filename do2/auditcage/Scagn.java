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
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.Unit;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author ITSDC, Sec'bad.
 * 
 */

@RestController
public class Scagn extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	 
	Employee emp;
	Rank rank;
	Unit unit;
	AuditCage auditcage;
	
	static public String INVALID_RANK = "AU:SCAGN is NOT admissible to Sub Maj.\n ";
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";
	static public String SERVICE_DETAIL_UNAVAILABLE = "AU:Employee Service Detail is not available.\n";
	static public final String BEYOND_DISCONTINUE_DATE = "AU:Admissible upto 31/12/1995. \n";
	 
	public List<Allowance> alwList;

	private StringBuffer buffer = new StringBuffer();
	
	@Transactional
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
		if (!isValidRank(do2)) {
			validity = false;
			buffer.append(INVALID_RANK);
		}
		
		if (!(isBeyondDiscontinueDate(do2))) {
			buffer.append(BEYOND_DISCONTINUE_DATE);
			validity = false;
		}
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
			
		}
		do2.setStatus(Do2Manager.VALID);
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
	return validity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
	
		if (isValid(do2)) {
			if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
				//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
				rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
				if (rank == null) {
					emp = Utils.getCurrentEmployee(do2.getArmyNo());
					if (emp.getSubstantiveRank().getId() != 1) {
						buffer.append(SERVICE_DETAIL_UNAVAILABLE);
						do2.setReason(buffer.toString());
						do2.setStatus(Do2Manager.PENDING);
					}
				}
			alwList = Utils.getApplicableAllowanceRates(do2
						.getDescription(), emp.getEnrolmentDate(), do2.getFromDate(), rank);
				if(alwList != null) {
					AuditCageHelper.copyCurrentAllowances(newAuditCage,
							baseAuditCage);

					if (isDrawingMutexAllowances(do2)) {
						final String[] alwUnderProcess = { "BHUTAN", "CCA1", "CCA",
								"CCB1", "CCB2", "CCOTH", "CFAA", "CHAFAA", "HCA",
								"CMFAA", "RLCA1", "RLCA2", "RLCA3", "RLCA4",
								"SCCIA" };

						final List<String> alwToStop = Arrays
								.asList(alwUnderProcess);
						AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
					}
					AuditCageHelper.updateAuditCageAllowance(newAuditCage,
							Do2Manager.SCAGN, alwList.get(0).getRateAmount());
	 
				} else {
					buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
					do2.setReason(buffer.toString());
					do2.setStatus(Do2Manager.PENDING);
					do2Repo.save(do2);
				}
			} else if (do2.getColumnA().equals(Do2Manager.CEASED)) {
				final String[] alwUnderProcess = { "SCAGN" };
				final List<String> alwToStop = Arrays.asList(alwUnderProcess);
				AuditCageHelper.copyCurrentAllowances(newAuditCage,
						baseAuditCage);
				AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
			} else {
				return Do2Manager.FAILURE;
			}
		}
		return Do2Manager.SUCCESS;

	}
		
	public boolean isValidRank(Do2 do2) {
		return (!rank.getRankCode().equals("70"));
	}
	public boolean isBeyondDiscontinueDate(Do2 do2) {
		GregorianCalendar cal1 = new GregorianCalendar(1996, Calendar.JANUARY, 01);
		Date startDate = new Date();
		startDate.setTime(cal1.getTimeInMillis());
		if (do2.getFromDate().before(startDate))
			return true;
		else
			return false;
	}
	public boolean isDrawingMutexAllowances(Do2 do2) {
		List<String> allowanceList = Arrays.asList(new String[] {"BHUTAN", "CCA1","CCA","CCB1","CCB2","CCOTH","CFAA",
				                    "CHAFAA","HCA","CMFAA","RLCA1","RLCA2","RLCA3","RLCA4","SCCIA"});
		return Utils.isMutexAllowance(do2.getArmyNo(), allowanceList, do2.getFromDate(), do2.getFromDate(),do2);
	}
		
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
}