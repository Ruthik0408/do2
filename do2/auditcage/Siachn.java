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
 * Author : A J NORONHA & T VANAJA, IT & SDC, Secunderabad.
 */

// SIACHEN ALLOWANCE
@RestController
public class Siachn extends AuditCageBase implements
		 Do2AuditCageProcessor {
	Employee employee;
	Rank rank;
	public List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();

	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String NOT_A_VALID_RANK = "AU:Invalid Rank on date of effect.\n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table.\n";
	 
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
		
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
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

		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);

		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),
					do2.getFromDate(), do2.getFromDate());

			AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2
					.getDescription(), alwList.get(0).getRateAmount());

			AuditCageHelper.allowInclusiveAllowances(do2,
					newAuditCage, baseAuditCage);
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

	 
	public boolean isRateAvailable(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2
				.getFromDate(), do2.getFromDate());
		if ((alwList.size() > 0) && (alwList.get(0).getRateAmount() > 0))
			return true;
		else
			return false;
	}
}