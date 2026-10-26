package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 



@RestController
// FLIGHT CHARGE CERTIFICATE ALLOWANCE
public class Flchca extends AuditCageBase implements
		 Do2AuditCageProcessor {
	Employee employee;
	EmployeeServiceDetail empServDet = null;
	Rank rank;
	List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();

	static public final String NO_SERVICE_DETAILS = "AU:Rank not available from Service Details.\n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table .\n";
	static public final String INVALID_RANK = "AU:Admissible to only to HAV and NbSub.\n ";
	static public final String INVALID_FROMDT = "AU:From date is prior to the date from which FLCHCA is admissible.\n";
	

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}

		if (!(isValidStartDate(do2))) {
			buffer.append(INVALID_FROMDT);
			validity = false;
		}

		if (!(isServiceDetailAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(Do2Manager.SERVICE_DETAIL_MISSING);
			validity = false;
		}

		if (!(isValidRank(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(INVALID_RANK);
			validity = false;
		}

		if (!(isRateAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_RATE_AVAILABLE);
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
		do2.setReason(buffer.toString());
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
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) && do2.getColumnA().equals(Do2Manager.GRANTED)) {	
			
			List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(), do2.getFromDate(), do2.getFromDate(),rank);

			AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2
					.getDescription(), alwList.get(0).getRateAmount());
	 	} else {
			final String[] alwUnderProcess = { do2.getDescription() };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);

			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

		return Do2Manager.SUCCESS;
	}

	// ********************************************************************
	public boolean isServiceDetailAvailable(Do2 do2) {
		 //Rank rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		 Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		 if(rank == null)
			 return false;
		 return true;
	}

	// FLCHCA admissible only to HAV and NBSUB
	public boolean isValidRank(Do2 do2) {
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
	//	System.out.println(rank.getRankCode());
		if ((rank.getRankCode().equals(Do2Manager.HAV))
				|| (rank.getRankCode().equals(Do2Manager.NBSUB)))
			return true;
		else
			return false;
	}

	public boolean isValidStartDate(Do2 do2) {
		GregorianCalendar cal1 = new GregorianCalendar(2000, Calendar.FEBRUARY,
				28);
		Date startDate = DateManager.getToday();
		startDate.setTime(cal1.getTimeInMillis());
		if (do2.getFromDate().after(startDate))
			return true;
		else
			return false;
	}

	public boolean isRateAvailable(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2
				.getFromDate(), do2.getFromDate(), rank);
	//	System.out.println(" FLCHCA RATE    "+alwList.get(0).getRateAmount());
		if ((alwList.size() > 0) && (alwList.get(0).getRateAmount() > 0))
			return true;
		else
			return false;
	}
}