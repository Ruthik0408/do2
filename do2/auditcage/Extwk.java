package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
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
//EXTRA WORK ALLOWANCE   -- 2% of Basic Pay (BP) -- BP as per para 3(m) of Army Pay Rules 2017
public class Extwk extends AuditCageBase implements Do2AuditCageProcessor {
	Employee employee;
	List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();

	static public final String INVALID_FROMDT = "AU:From date is prior to the date from which EXTWK is admissible.\n";
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String COOLING_OFF_PERIOD_NOT_COMPLETED = "AU:One Year From Last Drawn Date Not Completed. \n";
	static public final String OVERLAP = "OL:Allowance Already Being Drawn On The Grant Date.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}

		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
		if (rank != null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}

		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_FROMDT);
			validity = false;
		}

		if (!isOneYearCompletedSinceLastCessation(do2)) {
			buffer.append(COOLING_OFF_PERIOD_NOT_COMPLETED);
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
		}

		// if (do2.getId() == newDo2.getId())
		if (do2.getId() == 0) {
			if (isOverlap(do2)) {
				buffer.append(OVERLAP);
				validity = false;
				do2.setStatus(Do2Manager.INVALID);
			}
		}

		if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING))) {
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
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		double extraWorkAllowanceAmount = 0;
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			int basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
			extraWorkAllowanceAmount = Math.round(basicPay * 0.02);
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.EXTWK, extraWorkAllowanceAmount);
			final String[] alwUnderProcess = { "APTPAY", "FLCHCA" };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
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

	/*
	 * public boolean isOneYearCompletedSinceLastCessation(Do2 do2) { List<Do2>
	 * do2List =
	 * do2Repo.findByFromDateLessThanEqualToAndColAAndDescriptionAndStatus(do2.
	 * getEmployee().getId(), "EXTWK", do2.getFromDate(), "CEASED", "V");
	 * 
	 * Utils.getEntityManager().
	 * createQuery("select d from Do2 d where d.employee=:emp and d.status='V' and "
	 * +
	 * " d.description='EXTWK' and d.columnA='CEASED' and d.fromDate<=:fromdate order by d.fromDate DESC"
	 * ) .setParameter("emp", do2.getEmployee()).setParameter("fromdate",
	 * do2.getFromDate()).getResultList();
	 * 
	 * if (do2List==null || (do2List!=null && do2List.size()==0)) { return true; }
	 * 
	 * if ((DateManager.getNoOfDays(do2List.get(0).getFromDate(),
	 * do2.getFromDate()))<365) { // = removed on 23.06.2022 return false; } return
	 * true; }
	 */
	private boolean hasPosin(Do2 do2) {
		List<Do2> posins = do2Repo.findByEmployeeIdAndDescriptionAndFromDate(
				do2.getEmployee().getId(), "POSIN", do2.getFromDate());
		return !posins.isEmpty();
	}
	public boolean isOneYearCompletedSinceLastCessation(Do2 do2) {
		List<Do2> do2List = do2Repo.findByFromDateLessThanEqualToAndColAAndDescriptionAndStatus(do2.getEmployee().getId(),
				"EXTWK", do2.getFromDate(), "CEASED", "V");

			/*		Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.status='V' and "
					+ " d.description='EXTWK' and d.columnA='CEASED' and d.fromDate<=:fromdate order by d.fromDate DESC")
					.setParameter("emp", do2.getEmployee()).setParameter("fromdate", do2.getFromDate()).getResultList();*/

		if (do2List==null || (do2List!=null && do2List.size()==0)) {
			return true;
		}

		Date lastCeaseDate = do2List.get(0).getFromDate();
		double days = DateManager.getNoOfDays(lastCeaseDate, do2.getFromDate());
		if (days < 365) { // = removed on 23.06.2022
			if (days == 1 && hasPosin(do2)) {
				return true;
			}
			return false;
		}
		return true;
	}

	public boolean isOverlap(Do2 do2) {
		List<AuditCage> acList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDescToDateDesc(
				do2.getEmployee().getId(), do2.getFromDate());

		// Utils.getEntityManager().createQuery("select a from AuditCage a where
		// a.employee=:emp and a.recordStatus='V' and a.cancelled=false and
		// a.effective=true and "
		// + " a.fromDate<=:fromdate order by a.fromDate DESC, a.toDate
		// DESC").setParameter("emp", do2.getEmployee()).setParameter("fromdate",
		// do2.getFromDate()).getResultList();

		if (acList != null && acList.size() > 0) {
			List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(acList.get(0).getId());

			// Utils.getEntityManager().createQuery("select aca from AuditCageAllowance aca
			// where aca.auditCage=:ac ")
			// .setParameter("ac", acList.get(0)).getResultList();
			for (AuditCageAllowance aca : acaList) {
				if (aca.getPayCode().getPayCode().trim().equals("EXTWK")) {
					return true;
				}
			}
		}
		return false;
	}

}