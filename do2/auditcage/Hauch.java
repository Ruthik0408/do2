package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 

/**
 * Author : A J Noronha & T Vanaja IT & SDC, Secunderabad
 */

// HIGH ALTITUDE UN-CONGENIAL CLIMATE ALLOWANCE - HIGHER RATE
@RestController
public class Hauch extends AuditCageBase implements 
		Do2AuditCageProcessor {
	Employee employee;
	Rank rank;
	List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();

	static public final String NO_SERVICE_DETAILS = "AU:Rank not available from Service Details.\n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table.\n";
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String NOT_ON_DEPUTATION = "AU:HAUCL is admissible to deputationists to BHUTAN only.";
	static public final String AFTER_EXPIRY_DATE = "AU:Claim After Expiry Date Of The Allowance - 22022019.\n";
	
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
			buffer.append(NO_SERVICE_DETAILS);
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

		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);

		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(), do2.getFromDate(), do2.getFromDate(),
					rank);
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2
					.getDescription(), alwList.get(0).getRateAmount());

			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage,
					baseAuditCage);
			

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
	 
	public boolean isRateAvailable(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2
				.getFromDate(), do2.getFromDate(), rank);
		if ((alwList.size() > 0) && (alwList.get(0).getRateAmount() > 0))
			return true;
		else
			return false;
	}
	
	public boolean isOnDeputationOtherthanBhutan(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> auditCageList = acRepo.findByEmpIdEffectiveStatusNotEqualPendingFromDateBetweenOrderByFromDateDesc(employee.getId(),
																										do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery(
				" SELECT ac FROM AuditCage ac WHERE ac.employee=:emp AND ac.effective=true AND" +
				" ac.cancelled=false AND ac.recordStatus!='P' AND " +
				" (:do2FromDate between ac.fromDate and ac.toDate) order by ac.fromDate desc, ac.toDate desc ")
				.setParameter("emp", employee)
				.setParameter("do2FromDate", do2.getFromDate()).getResultList();*/
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