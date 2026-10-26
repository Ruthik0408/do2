package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.Utils;

 
 
// DIFFICULT AREA ALLOWANCE
@RestController
public class Daa extends AuditCageBase implements 
		Do2AuditCageProcessor {
	Employee employee;
	StringBuffer buffer = new StringBuffer();
	Rank rank;
	public List<Allowance> alwList;

	static public final String NO_RATE_AVAILABLE = "AU:Rate Not Available in Allowance Rate Table.\n";
	static public final String BEFORE_START_DATE = "AU:Admissible From 10/08/2007.\n";
	static public final String NOT_YET_MUSTERED = "AU:PBOR not yet mustered as per Employee table. \n";
	static public final String DO2_BEFORE_MUSTER = "AU:From date is before the date of mustering.\n ";
	static public final String NOT_ON_DEPUTATION = "AU:DAA is admissible to deputationists to BHUTAN only.";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		do2.setStatus(Do2Manager.VALID);
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setReason(null);
			do2Repo.save(do2);
			return validity;
		}
		if (!(startDate(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(BEFORE_START_DATE);
			validity = false;
		}

		if (!(isDo2AfterMuster(do2))) {
			if (employee.getInitialMusteringDate() == null) {
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(NOT_YET_MUSTERED);
			} else {
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(DO2_BEFORE_MUSTER);
			}
			validity = false;
		}
		if (!(isServiceDetailAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(Do2Manager.SERVICE_DETAIL_MISSING);
			validity = false;
		}

		if (!(isRateAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_RATE_AVAILABLE);
			validity = false;
		}

		if (isOnDeputationOtherthanBhutan(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NOT_ON_DEPUTATION);
			validity = false;
		}
		if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null
					&& !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(buffer.toString());
			}
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
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);

		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(), do2.getFromDate(), do2.getFromDate(),
					rank);

			AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2
					.getDescription(), alwList.get(0).getRateAmount());
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);

	 	} else {
			final String[] alwUnderProcess = { "DAA" };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.stopAllowances(alwToStop, newAuditCage,
					baseAuditCage);
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	// ***************************************************************************
	public boolean startDate(Do2 do2) {
		GregorianCalendar cal1 = new GregorianCalendar(2005, Calendar.MARCH, 31);
		Date startDate = new Date();
		startDate.setTime(cal1.getTimeInMillis());
		if (do2.getFromDate().after(startDate))
			return true;
		else
			return false;
	}

	public boolean isDo2AfterMuster(Do2 do2) {
		if ((employee.getInitialMusteringDate() != null)
				&& (do2.getFromDate().after(employee.getInitialMusteringDate())))
			return true;
		else
			return false;
	}

	public boolean isServiceDetailAvailable(Do2 do2) {
		if (rank.equals(null))
			return false;
		else
			return true;
	}

	public boolean isRateAvailable(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2
				.getFromDate(), do2.getFromDate(), rank);
		if ((alwList.size() > 0) && (alwList.get(0).getRateAmount() > 0))
			return true;
		else
			return false;
	}

	 public boolean isOnDeputationOtherthanBhutan(Do2 do2) {
			 
			List<AuditCage> auditCageList =acRepo.findByEmpIdEffectiveStatusNotEqualPendingFromDateBetweenOrderByFromDateDesc(do2.getEmployee().getId(),do2.getFromDate());
				 
			if (auditCageList.size() > 0) {
				if (auditCageList.get(0).getDeputationFlag() == true) {
					 if (auditCageList.get(0).getDeputationStatus().equals("T") ||
						 auditCageList.get(0).getDeputationStatus().equals("K"))
						  return false;
					 else
						 return true;
				} else {
				  return false;  
				}
			}
			return false;
	    }
}