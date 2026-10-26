package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 

/**
 * Author :A J Noronha & T Vanaja, IT & SDC, Secunderabad.
 */
@RestController
// FLYING PAY
public class Flypay extends AuditCageBase implements
		 Do2AuditCageProcessor {
	Employee employee;
	Rank rank;
	List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();

	static public final String NO_SERVICE_DETAILS = "AU:Rank not available from Service Details. \n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table. \n";
	static public final String NOT_YET_MUSTERED = "AU:PBOR is not yet mustered.\n";
	static public final String INVALID_RANK = "AU:Not admissible for HonyLt/HonyCapt.\n";
	static public final String INVALID_CATEGORY = "AU:Category is not MMG GUNNER.\n";
	static public final String DO2_BEFORE_MUSTER = "AU:From date is before the date of mustering.\n";
	static public final String NOT_JCO = "AU:PBOR is not a JCO.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");

		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		if (!(isValidCategory(do2))) {
			buffer.append(INVALID_CATEGORY);
			validity = false;
		}
		
		if (!(isValidRank(do2))) {
			buffer.append(INVALID_RANK);
			validity = false;
		}
		
		if (!(isDo2AfterMuster(do2))) {
			if (employee.getInitialMusteringDate() == null) {
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(NOT_YET_MUSTERED);
			} else {
				buffer.append(DO2_BEFORE_MUSTER);
			}
			validity = false;
		}

		if (!(isServiceDetailAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(Do2Manager.SERVICE_DETAIL_MISSING);
			validity = false;
		}

		if (!(isRateAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_RATE_AVAILABLE);
			validity = false;
		}
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		}else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
		/*if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null
					&& !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(buffer.toString());
			}
		} else {
			do2.setStatus(Do2Manager.VALID);
		}*/
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
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");

		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);

		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(), do2.getFromDate(), do2.getFromDate(),
					rank);
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
		if (rank == null)
			return false;
		else
			return true;
	}

	public boolean isDo2AfterMuster(Do2 do2) {
		if ((employee.getInitialMusteringDate() != null)
				&& (do2.getFromDate().after(employee.getInitialMusteringDate())))
			return true;
		else
			return false;
	}

	// FLYPAY not admissible only to HonyLt & HonyCapt
	public boolean isValidRank(Do2 do2) {
	//	System.out.println(rank.getRankCode());
		if ((rank.getRankCode().equals("80"))
				|| (rank.getRankCode().equals("90")))
			return false;
		else
			return true;
	}

	public boolean isValidCategory(Do2 do2) {
		if (employee.getGroupCategory().getSoldierCategory().equals("MMG"))
			return true;
		else
			return false;
	}

	public boolean isRateAvailable(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2
				.getFromDate(), do2.getFromDate(), rank);
		if ((alwList.size() > 0) && (alwList.get(0).getRateAmount() > 0))
			return true;
		else
			return false;
	}
}