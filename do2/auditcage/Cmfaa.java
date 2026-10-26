package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
 
// COMPENSATORY MODIFIED FIELD AREA ALLOWANCE
@RestController
public class Cmfaa extends AuditCageBase implements 
		Do2AuditCageProcessor {

	Employee employee;
	Rank rank;
	List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();

	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate Not Available in Allowance Rate table. \n";
	static public final String AFTER_EXPIRY_DATE = "AU:Claim After Expiry Date Of The Allowance - 22022019.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		do2.setStatus(Do2Manager.VALID);

		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
	//	System.out.println("XXXXXXXXXXXXXXXXXXXXX"+rank);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setReason(null);
			do2Repo.save(do2);
			return validity;
		}
		if (!Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) {
			if (!do2.getDo2Date().before(DateManager.getRiskAndHaucaPublicationStartDate())
					&& !do2.getFromDate().before(DateManager.getRiskAndHaucaAllowanceStartDate())) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(AFTER_EXPIRY_DATE);
				do2Repo.save(do2);
				return false;
			}
		}
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}

		if (!(isServiceDetailAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(Do2Manager.MISSING_SERVICE_DETAIL);
			validity = false;
		}

		if (!(isRateAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_RATE_AVAILABLE);
			validity = false;
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
		Pao pao;

		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);

		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			pao = Utils.getPao();
		} else {
			pao = null;
		}
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(), do2.getFromDate(), do2.getFromDate(),
					rank, pao);
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2
					.getDescription(), alwList.get(0).getRateAmount());

			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage,
					baseAuditCage);
		} else {
			final String[] alwUnderProcess = { "CMFAA" };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);

			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

		return Do2Manager.SUCCESS;
	}

	// *************************************************************************
 

	public boolean isServiceDetailAvailable(Do2 do2) {
		if (rank == null)
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
}