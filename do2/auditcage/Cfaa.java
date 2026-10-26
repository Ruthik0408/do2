package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
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

 

 
// COMPENSATORY FIELD AREA ALLOWANCE
@RestController
public class Cfaa extends AuditCageBase implements 
		Do2AuditCageProcessor {
	Employee employee;
	Rank rank;
	StringBuffer buffer = new StringBuffer();
	List<Allowance> alwList = null;
	
	static public final String NOT_A_VALID_RANK = "AU:Invalid Rank On Date Of Effect. \n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate Not Available in Allowance Rate Table. \n";
	static public final String SERVICE_DETAIL_MISSING = "AU:Service details are missing. \n";
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String AFTER_EXPIRY_DATE = "AU:Claim After Expiry Date Of The Allowance - 22022019.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if(do2.getColumnA() != null 
				&& do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
		return true;
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
			buffer.append(SERVICE_DETAIL_MISSING);
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

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
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
			if (alwList.size() > 0) {

				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2
						.getDescription(), alwList.get(0).getRateAmount());
				AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
			} else {
				return Do2Manager.SUCCESS;
			}
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