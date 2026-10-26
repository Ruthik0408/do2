package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.ReadyReckoner;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Lorank extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	Employee employee = null;
	Rank rank = null;
	Rank empRank = null;
	String previousRankCode = "";
	String currentRankCode = "";
	Date previousRankDate = null;
	Date naikRankDate = null;
	Date sepoyRankDate = null;
	Date havRankDate = null;

	Date previousMacpRankDate = null;
	Date earlierRankDate = null;
	List<EmployeeServiceDetail> esdPromotList = null;
	public String[][] strRankReg = new String[][] { { "20", "SEP" }, { "30", "NK" }, { "40", "HAV" }, { "50", "NBSUB" },
			{ "60", "SUB" }, { "70", "SUBMAJ" }, { "20", "SOWAR" }, { "30", "LD" }, { "40", "DFR" }, { "50", "NBRIS" },
			{ "60", "RIS" }, { "70", "RISMAJ" } };
	public static final String INVALID_RANK = "AU:Invalid Rank In Col.7A.\n";
	public static final String INVALID_CURRENT_RANK = "AU:Cannot be Reverted from SEP/NBSUB.\n";
	public static final String PROMOT_MISSING = "AU:Promot detail of current rank missing in ESD table.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean revertValidity = true;
		StringBuffer buffer = new StringBuffer();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
		empRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PROMOT");
		if (rank == null) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(Do2Manager.MISSING_SERVICE_DETAIL);
			revertValidity = false;
		}

		if (!(isValidCurrentRank())) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_CURRENT_RANK);
			revertValidity = false;
		}

		if (!do2.getFromDate().after(DateManager.getSixthCpcDate())) {
			if (!isOldBasicPayAvailable(do2)) {
				do2.setStatus(Do2Manager.INVALID);
				buffer.append("Base audit cage before Promot date not found to get basic pay ");
				revertValidity = false;
			}
		}

		if (newDo2.getId() == do2.getId()) {
			if (!(isValidRank(do2, rank))) {
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(INVALID_RANK);
				revertValidity = false;
			}
		} /*
			 * else{ if (!(isValidRank(do2,empRank))) { do2.setStatus(Do2Manager.INVALID);
			 * buffer.append(INVALID_RANK); revertValidity = false; } }
			 */

		if (revertValidity) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		} else {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		}

		do2Repo.save(do2);

		return revertValidity;
	}

	/*
	 * @Override public AuditCage getNewAuditCage(AuditCage newAuditCage,AuditCage
	 * baseAuditCage) { employee =
	 * Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo()); rank =
	 * Utils.getPborRank(newAuditCage.getDo2().getArmyNo(),
	 * newAuditCage.getDo2().getFromDate(),"ALLCE");
	 * updateBdtInfo(newAuditCage.getDo2()); int probableBasic =
	 * getPreviousRankBasic(newAuditCage.getDo2()); if (probableBasic != 0 ) { if
	 * (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
	 * newAuditCage.setBasicPay(probableBasic); if
	 * (!newAuditCage.getFromDate().before(DateManager.getDPDate()))
	 * newAuditCage.setDp((int) Math.round(newAuditCage.getBasicPay() * 0.50)); }
	 * else { // No Change In Basic Pay only grade pay of reverted rank
	 * newAuditCage.setGradePay(getPreviousRankGradePay(newAuditCage.getDo2())); }
	 * updateOldBasicPay(newAuditCage,newAuditCage.getDo2(),previousRankDate); }
	 * return newAuditCage; }
	 */

	// above method commented and amended as stated below due to sevencpc

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(), "ALLCE");
		updateBdtInfo(newAuditCage.getDo2());
		int probableBasic = getPreviousRankBasic(newAuditCage.getDo2());
		int probable7pcbBasic = 0;
		if (probableBasic != 0) {
			if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				newAuditCage.setBasicPay(probableBasic);
				if (!newAuditCage.getFromDate().before(DateManager.getDPDate()))
					newAuditCage.setDp((int) Math.round(newAuditCage.getBasicPay() * 0.50));
			} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) { // No Change In Basic Pay
																								// only grade pay of
																								// reverted rank
				newAuditCage.setGradePay(getPreviousRankGradePay(newAuditCage.getDo2()));
				updateOldBasicPay(newAuditCage, newAuditCage.getDo2(), previousRankDate);
			} else {
				if (newAuditCage.getGradePay() == 0) {
					get7cpcPay(newAuditCage, newAuditCage.getDo2());
					newAuditCage.setOldBasicPay(newAuditCage.getOldBasicPay());
					newAuditCage.setOldGradePay(getPreviousRankGradePay(newAuditCage.getDo2()));
				}

			}

		}
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT + "");
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT + "");

		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		final String[] alwNotAdmissible = { "APTPAY" };
		List<String> alwToStop = Arrays.asList(alwNotAdmissible);
		AuditCageHelper.removeAllowances(alwToStop, newAuditCage);

		String columnB = do2.getColumnB();
		String columnC = do2.getColumnC();
		List<Allowance> allowances = null;
		if (columnB != null && columnC != null) {
			if (columnB.equals("FORGSP") && (columnC.equals("ALL") || columnC.equals("FIRST"))) {
				AuditCageHelper.removeAllowances(Arrays.asList(new String[] { Do2Manager.GSPAY }), newAuditCage);
			} else if (columnB.equals("FORGSP") && columnC.equals("SECOND")) {
				allowances = Utils.getApplicableAllowanceRates(Do2Manager.GSPAY, "FIRST", do2.getFromDate(),
						do2.getFromDate());

			} else {
				allowances = Utils.getApplicableAllowanceRates(Do2Manager.GSPAY, "SECOND", do2.getFromDate(),
						do2.getFromDate());
			}
		}
		if (allowances != null && allowances.size() > 0) {
			if (Integer.parseInt(rank.getRankCode()) < 50) {
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.GSPAY,
						allowances.get(0).getRateAmount());
			}
		}

		return null;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PROMOT);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isValidCurrentRank() {
		if (rank.getRankCode().equals("10") || rank.getRankCode().equals("50")) {
			return false;
		}
		return true;
	}

	public boolean isValidRank(Do2 do2, Rank rank) {
		for (int i = 0; i < strRankReg.length; i++) {
			if (do2.getColumnA().trim().equals(strRankReg[i][1])
					&& (rank.getRankName().trim().equals(strRankReg[i + 1][1])))
				return true;
		}
		return false;
	}

	public boolean isESDPromotAvailable(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<EmployeeServiceDetail> esdPromotList = esdRepo.findByStatusNotEqualToAndDo2ColA(employee.getId(),
				Do2Manager.DO2_CANCEL_STATUS, rank.getRankName());

		/*
		 * Utils .getEntityManager() .createQuery(
		 * "SELECT pl from EmployeeServiceDetail AS pl WHERE " +
		 * " pl.do2.employee=:employee AND pl.do2.status !=:stat AND pl.do2.columnA=:cola "
		 * ) .setParameter("employee",employee) .setParameter("cola",rank.getRankName())
		 * .setParameter("stat",Do2Manager.DO2_CANCEL_STATUS) .getResultList();
		 */
		// char xx = Do2Manager.DO2_CANCEL_STATUS;
		if (esdPromotList.size() > 0)
			return true;
		return false;
	}

	public boolean isOldBasicPayAvailable(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		// System.out.println ( " RANK " +rank.getRankName());
		esdPromotList = esdRepo.findByFromDateLessThanAndStatusOrderByFromDateDesc(employee.getId(),
				Do2Manager.ESD_PROMOT + "", do2.getFromDate());

		/*
		 * Utils.getEntityManager().createQuery(
		 * "SELECT esd FROM EmployeeServiceDetail esd WHERE esd.status=:status AND " +
		 * " esd.fromDate < :fromDate AND esd.employee=:emp ORDER BY esd.fromDate DESC")
		 * .setParameter("fromDate", do2.getFromDate()) .setParameter("emp", employee)
		 * .setParameter("status", Do2Manager.ESD_PROMOT) .getResultList();
		 */
		if (esdPromotList.size() > 0) {
			previousRankDate = DateManager.getPreviousDate(esdPromotList.get(0).getFromDate());
		}
		if (!do2.getColumnA().equals("SEP")) {
			previousRankCode = "30";
		} else {
			previousRankCode = "20";
		}

		if (do2.getColumnA().equals("SEP")) {
			currentRankCode = "20";
		}
		if (do2.getColumnA().equals("NK")) {
			currentRankCode = "30";
		}
		List<AuditCage> atcList = acRepo.findByFromDateLessThanOrderByFromDateDesc(employee.getId(), previousRankDate);

		/*
		 * Utils.getEntityManager().createQuery("SELECT atc FROM AuditCage atc WHERE " +
		 * " atc.employee=:employee AND atc.effective=true AND atc.cancelled=false AND atc.absentFlag=false "
		 * +
		 * " AND atc.recordStatus!='P' AND atc.fromDate<:frdt ORDER BY atc.fromDate DESC"
		 * ) .setParameter("employee",employee) .setParameter("frdt",previousRankDate)
		 * .getResultList();
		 */
		if (atcList == null || atcList.size() == 0) {
			return false;
		} else
			return true;

	}

	public int getPreviousRankGradePay(Do2 do2) {
		String empRank = do2.getColumnA();
		String rankCode = null;
		if (!do2.getColumnA().equals("SEP")) {
			empRank = "NK";
		}
		if (empRank.equals("NK")) {
			rankCode = "30";
		} else {
			rankCode = "20";
		}
		GroupCategory gp = Utils.getGroupCategory(do2, "PREV");
		Rank rank = rankRepo.findByRankCode(rankCode);
		List<PayScale> payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
				rank.getId(), gp.getSoldierGroup(), do2.getFromDate());

		/*
		 * Utils .getEntityManager() .createQuery(
		 * "SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk " +
		 * " AND ps.soldierGroup.soldierGroup=:grp " +
		 * " AND ((:fromDate between ps.fromDate and ps.toDate ) " +
		 * " or (:fromDate >= ps.fromDate and ps.toDate is null )) " +
		 * " AND ps.startScale IS NOT null") .setParameter("rnk",Utils.getForeignKey(new
		 * Rank(), "rankCode",rankCode)) .setParameter("grp",gp.getSoldierGroup())
		 * .setParameter("fromDate", do2.getFromDate()) .getResultList();
		 */
		return payScaleList.get(0).getGradePay();
	}

	public int getPreviousRankBasic(Do2 do2) {

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		esdPromotList = esdRepo.findByFromDateLessThanAndToDateNotEqualToOrderByFromDateDesc(employee.getId(),
				Do2Manager.ESD_PROMOT + "", do2.getFromDate(), DateManager.getPreviousDate(do2.getFromDate()));

		/*
		 * Utils.getEntityManager().
		 * createQuery("SELECT esd FROM EmployeeServiceDetail esd WHERE esd.status=:status AND "
		 * +
		 * " esd.fromDate < :fromDate AND esd.employee=:emp and (esd.toDate is not null and esd.toDate!=:toDate) ORDER BY esd.fromDate DESC"
		 * ) .setParameter("fromDate", do2.getFromDate()) .setParameter("emp", employee)
		 * .setParameter("toDate", DateManager.getPreviousDate(do2.getFromDate()))
		 * .setParameter("status", Do2Manager.ESD_PROMOT) .getResultList();
		 */

		if (esdPromotList.size() > 0) {
			previousRankDate = DateManager.getPreviousDate(esdPromotList.get(0).getFromDate());
		}

		if (do2.getColumnA().equals("NK")) {
			previousRankCode = "40";
		} else if (do2.getColumnA().equals("SEP")) {
			previousRankCode = "30";
		} else {
			previousRankCode = "20";
		}

		/*
		 * if(!do2.getColumnA().equals("SEP")){ previousRankCode="30"; } else {
		 * previousRankCode="20"; }
		 */

		List<AuditCage> atcList = acRepo.findByFromDateLessThanOrderByFromDateDesc(employee.getId(), previousRankDate);

		/*
		 * Utils.getEntityManager().createQuery("SELECT atc FROM AuditCage atc WHERE " +
		 * " atc.employee=:employee AND atc.effective=true AND atc.cancelled=false AND atc.absentFlag=false "
		 * +
		 * " AND atc.recordStatus!='P' AND atc.fromDate<:frdt ORDER BY atc.fromDate DESC"
		 * ) .setParameter("employee",employee) .setParameter("frdt",previousRankDate)
		 * .getResultList();
		 */
		if (atcList.size() > 0) {
			return arriveProbablePay(do2, previousRankDate, atcList.get(0).getBasicPay());
		}
		return 0;

	}

	public int arriveProbablePay(Do2 do2, Date previousPromotDate, int previousBasic) {
		int probableBasic = previousBasic;
		Date previousIncrDate = DateManager.getNextIncrementDate(previousPromotDate);
		Rank rank = rankRepo.findByRankCode(previousRankCode);
		List<PayScale> payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
				rank.getId(), employee.getGroupCategory().getSoldierGroup(), previousPromotDate);

		/*
		 * Utils.getEntityManager().createQuery(
		 * "SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk " +
		 * " AND ps.soldierGroup.soldierGroup=:grp " +
		 * " AND ((:fromDate between ps.fromDate and ps.toDate ) " +
		 * " or (:fromDate >= ps.fromDate and ps.toDate is null )) " +
		 * " AND ps.startScale IS NOT null") .setParameter("rnk",Utils.getForeignKey(new
		 * Rank(), "rankCode",previousRankCode))
		 * .setParameter("grp",employee.getGroupCategory().getSoldierGroup())
		 * .setParameter("fromDate", previousPromotDate) .getResultList();
		 */

		if (payScaleList.size() > 0) {
			while (previousIncrDate.before(do2.getFromDate())) {
				probableBasic += payScaleList.get(0).getIncr0();
				previousIncrDate = DateManager.getNextIncrementDate(previousIncrDate);
			}

		}

		employee.setIncrementDate(previousIncrDate);
		return probableBasic;
	}

	public void updateEmployee(Do2 do2) {
		String empRank = do2.getColumnA();
		String rankCode = null;

		if (do2.getColumnA().equals("NK")) {
			empRank = "NK";
			rankCode = "30";
			employee.setHavDate(null);
		}

		if (do2.getColumnA().equals("SEP")) {
			employee.setNaikDate(null);
			rankCode = "20";
		}

		/*
		 * if (!do2.getColumnA().equals("SEP")) { empRank="NK"; rankCode="30";
		 * employee.setHavDate(null); } else { employee.setNaikDate(null);
		 * rankCode="20"; }
		 */

		Rank rank = rankRepo.findByRankCode(rankCode);
		employee.setSubstantiveRank(rank);
		// employee.setSubstantiveRank((Rank) Utils.getForeignKey(new Rank(),
		// "rankCode", rankCode));
		employee.setSubstantiveRankDate(do2.getFromDate());
		employee.setAppointmentDate(null);
		employee.setAppointmentRate(null);
	}

	private void updateOldBasicPay(AuditCage auditCage, Do2 do2, Date previousPromotDate) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atcList = acRepo.findByFromDateLessThanOrderByFromDateDesc(employee.getId(),
				previousPromotDate);

		/*
		 * Utils.getEntityManager().createQuery("SELECT atc FROM AuditCage atc WHERE " +
		 * " atc.employee=:employee AND atc.effective=true AND atc.cancelled=false AND atc.absentFlag=false AND "
		 * + " atc.recordStatus!='P' AND atc.fromDate<:frdt ORDER BY atc.fromDate DESC")
		 * .setParameter("employee",employee) .setParameter("frdt",previousPromotDate)
		 * .getResultList();
		 */

		int probableBasic = atcList.get(0).getOldBasicPay();
		Date previousIncrDate = DateManager.getNextIncrementDate(previousPromotDate);
		GroupCategory gp = Utils.getGroupCategory(do2, "PREV");
		Rank rank = rankRepo.findByRankCode(previousRankCode);
		List<PayScale> payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
				rank.getId(), gp.getSoldierGroup(), previousPromotDate);

		/*
		 * Utils .getEntityManager() .createQuery(
		 * "SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk " +
		 * " AND ps.soldierGroup.soldierGroup=:grp " +
		 * " AND ((:fromDate between ps.fromDate and ps.toDate ) " +
		 * " or (:fromDate >= ps.fromDate and ps.toDate is null )) " +
		 * " AND ps.startScale IS NOT null") .setParameter("rnk",Utils.getForeignKey(new
		 * Rank(), "rankCode",previousRankCode))
		 * .setParameter("grp",gp.getSoldierGroup()) .setParameter("fromDate",
		 * previousPromotDate) .getResultList();
		 */

		while (previousIncrDate.before(do2.getFromDate())) {
			probableBasic += payScaleList.get(0).getIncr0();
			previousIncrDate = DateManager.getNextIncrementDate(previousIncrDate);
		}
		auditCage.setOldBasicPay(probableBasic);
	}

	@Override
	public void updateBdtInfo(Do2 do2) {
		updateEmployee(do2);
	}

	public void get7cpcPay(AuditCage nAuditCage, Do2 do2) {

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		esdPromotList = esdRepo.findByFromDateLessThanAndStatusOrderByFromDateDesc(employee.getId(),
				Do2Manager.ESD_PROMOT + "", do2.getFromDate());

		/*
		 * Utils.getEntityManager().createQuery(
		 * "SELECT esd FROM EmployeeServiceDetail esd WHERE esd.status=:status AND " +
		 * " esd.fromDate < :fromDate AND esd.employee=:emp ORDER BY esd.fromDate DESC")
		 * .setParameter("fromDate", do2.getFromDate()) .setParameter("emp", employee)
		 * .setParameter("status", Do2Manager.ESD_PROMOT) .getResultList();
		 */

		// if(esdPromotList.size()>0){
		if (esdPromotList != null && esdPromotList.size() > 0) {
			for (int i = 0; i < esdPromotList.size(); i++) {
				if (esdPromotList.get(i).getDo2().getStatus().equals("V")
						&& (esdPromotList.get(i).getDo2().getColumnA() != null
								&& esdPromotList.get(i).getDo2().getColumnA().substring(2).equals("NK"))) {
					naikRankDate = esdPromotList.get(i).getFromDate();
				}

				if (esdPromotList.get(i).getDo2().getStatus().equals("V")
						&& (esdPromotList.get(i).getDo2().getColumnA() != null
								&& esdPromotList.get(i).getDo2().getColumnA().substring(2).equals("HAV"))) {
					havRankDate = esdPromotList.get(i).getFromDate();
				}

				if (esdPromotList.get(i).getDo2().getStatus().equals("V")
						&& esdPromotList.get(i).getDo2().getDescription().equals("MUSTER")) {
					sepoyRankDate = esdPromotList.get(i).getFromDate();
				}
			}

		}

		if (do2.getColumnA().equals("SEP")) {
			previousRankDate = naikRankDate;
			previousRankCode = "30";
			currentRankCode = "20";
			if (employee.getAcphavDate() != null && employee.getAcphavDate().before(previousRankDate)) {
				previousRankDate = employee.getAcphavDate();
			} else if (employee.getAcpnaikDate() != null && employee.getAcpnaikDate().before(previousRankDate)) {
				previousRankDate = employee.getAcpnaikDate();
				previousRankCode = "29";
			}
		} else if (do2.getColumnA().equals("NK")) {
			previousRankDate = havRankDate;
			previousRankCode = "40";
			currentRankCode = "30";
			if (employee.getAcphavDate() != null && employee.getAcphavDate().before(previousRankDate)) {
				previousRankDate = employee.getAcphavDate();
				previousRankCode = "39";
			} else if (employee.getAcpnaikDate() != null && employee.getAcpnaikDate().before(previousRankDate)) {
				previousRankDate = employee.getAcpnaikDate();
				previousRankCode = "29";
			}
		} else {
			previousRankCode = "20";
			previousRankDate = sepoyRankDate;
		}

		Rank previousRank = null;

		int probable7pcBasic = 0;
		int probable6pcBasic = 0;
		int probable6pcXpay = 0;
		int probable5pcBasic = 0;
		int probablegradepay = 0;
		boolean adhocIncrement01012006Exists = false;

		if (do2.getColumnA().equals("SEP")) {
			previousRank = rankRepo.findByRankCode("30");
			// previousRank = (Rank)Utils.getForeignKey(new Rank(), "rankCode", "30");
		} else if (do2.getColumnA().equals("NK")) {
			previousRank = rankRepo.findByRankCode("40");
		} else {
			previousRank = rankRepo.findByRankCode("20");
			// previousRank = (Rank)Utils.getForeignKey(new Rank(), "rankCode", "20");
		}

		System.out.println(" previous Rank date " + previousRankDate + " Previous Rank " + previousRank.getRankCode());

		Date previousIncrDate = getNextIncrementDate(previousRankDate);
		
		System.out.println (  " Previous Incr Date xxx " +previousIncrDate);
		
		List<Do2> prevIncrDo2List = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanAndDescValidStatus(
				employee.getId(), "INCR", previousRankDate, previousIncrDate);
		
		System.out.println ( " prevIncrDo2List.size " +prevIncrDo2List.size());
		
		if (prevIncrDo2List.size()==1) {
			previousIncrDate = prevIncrDo2List.get(0).getFromDate();
		}

		String currentRankPayLevel = Do2Manager.getPayLevelPromot(currentRankCode);

		System.out.println(" Current Rank code " + currentRankPayLevel + " current rank code " + currentRankCode);
		String previousRankPayLevel = Do2Manager.getPayLevelPromot(previousRank.getRankCode());
		System.out.println(
				" previous Incr date " + previousIncrDate + " previous Rank Pay level " + previousRankPayLevel);
		Rank rank = rankRepo.findByRankCode(previousRankCode);
		List<PayScale> payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
				rank.getId(), employee.getGroupCategory().getSoldierGroup(), previousRankDate);

		/*
		 * Utils.getEntityManager().createQuery(
		 * "SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk " +
		 * " AND ps.soldierGroup.soldierGroup=:grp " +
		 * " AND ((:fromDate between ps.fromDate and ps.toDate ) " +
		 * " or (:fromDate >= ps.fromDate and ps.toDate is null )) " +
		 * " AND ps.startScale IS NOT null") .setParameter("rnk",Utils.getForeignKey(new
		 * Rank(), "rankCode",previousRankCode))
		 * .setParameter("grp",employee.getGroupCategory().getSoldierGroup())
		 * .setParameter("fromDate", previousRankDate) .getResultList();
		 */

		if (previousIncrDate.before(DateManager.getSixthCpcDate())) {
			List<Do2> do2List = null;
			try {
				do2List = do2Repo.findByDescColCFromDateEqual(employee.getId(), "V", "INCR", "ADHOC",
						DateManager.getDate("2006-01-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Utils.getEntityManager().createQuery("select d from Do2 d where
			// d.employee=:emp and description='INCR' and "
			// + " d.columnC='ADHOC' and d.fromDate='2006-01-01' and status='V'
			// ").setParameter("emp", employee).getResultList();
			if (do2List != null && do2List.size() > 0) {
				adhocIncrement01012006Exists = true;
			}
		}

		List<AuditCage> atcList = acRepo.findByFromDateLessThanAndToDateGreaterThanEqualToOrderByFromDateToDateDesc(
				employee.getId(), previousRankDate);

		/*
		 * Utils.getEntityManager().createQuery("SELECT atc FROM AuditCage atc WHERE " +
		 * " atc.employee=:employee AND atc.effective=true AND atc.cancelled=false AND atc.absentFlag=false "
		 * +
		 * " AND atc.recordStatus!='P' AND atc.fromDate=:frdt ANd atc.toDate>=:frdt ORDER BY atc.fromDate DESC,atc.toDate DESC"
		 * ) .setParameter("employee",employee) .setParameter("frdt",previousRankDate)
		 * .getResultList();
		 */
		if (atcList.size() > 0) {

			if (previousRankDate.before(DateManager.getSixthCpcDate())) {
				probable5pcBasic = atcList.get(0).getBasicPay();
			} else if (previousRankDate.before(DateManager.getSeventhCpcDate())) {
				probable6pcBasic = atcList.get(0).getBasicPay();
				probable6pcXpay = atcList.get(0).getXGroupPay();
				probablegradepay = atcList.get(0).getGradePay();
			} else {
				atcList = acRepo.findByFromDateLessthanOrderByFromDateToDateDesc(employee.getId(), previousRankDate);
				probable7pcBasic = atcList.get(0).getBasicPay();
			}
		}
		System.out
				.println(" probable 5 pc basic " + probable5pcBasic + " atc from date " + atcList.get(0).getFromDate());
		while (previousIncrDate.before(do2.getFromDate())) {

			if (previousIncrDate.before(DateManager.getSixthCpcDate())) {
				Rank rank1 = rankRepo.findByRankCode(previousRankCode);
				payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
						rank1.getId(), Utils.getGroupCategory(do2, "CUR").getSoldierGroup(), previousIncrDate);

				/*
				 * Utils.getEntityManager().createQuery(
				 * "SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk " +
				 * " AND ps.soldierGroup.soldierGroup=:grp " +
				 * " AND ((:fromDate between ps.fromDate and ps.toDate ) " +
				 * " or (:fromDate >= ps.fromDate and ps.toDate is null )) " +
				 * " AND ps.startScale IS NOT null") .setParameter("rnk",Utils.getForeignKey(new
				 * Rank(), "rankCode",previousRankCode))
				 * .setParameter("grp",Utils.getGroupCategory(do2,"CUR").getSoldierGroup())
				 * .setParameter("fromDate", previousIncrDate) .getResultList();
				 */
				probable5pcBasic += payScaleList.get(0).getIncr0();
				System.out.println(" aaaa  probable5pcbasic " + probable5pcBasic);
				previousIncrDate = DateManager.getNextIncrementDate(previousIncrDate);

				if (previousIncrDate.after(DateManager.getSixthCpcDate())) {
					if (adhocIncrement01012006Exists) {
						probable5pcBasic += payScaleList.get(0).getIncr0();
						System.out.println(" YY  Y  " + probable5pcBasic);
					}
				}

			} else if (previousIncrDate.before(DateManager.getSeventhCpcDate())) {

				String soldierGroup = "";
				// GroupCategory groupCategory = Utils.getGroupCategory(do2,"PREV");
				GroupCategory groupCategory = Utils.getGroupCategory(atcList.get(0).getDo2(), "PREV");
				if (groupCategory != null)
					soldierGroup = groupCategory.getSoldierGroup();
				Rank rank2 = rankRepo.findByRankCode(previousRankCode);
				if (previousRankDate.before(DateManager.getSixthCpcDate())) {
					System.out.println(" axxccaa preivousRankDate " + previousRankDate + " rank2 " + rank2.getId()
							+ " group " + soldierGroup + " 5pcbcsic " + probable5pcBasic);

					List<ReadyReckoner> reckonerList = rrRepo.findByRankGroupAndBasic(rank2.getId(), soldierGroup,
							probable5pcBasic);

					/*
					 * Utils.getEntityManager().createQuery(
					 * "SELECT rl FROM ReadyReckoner AS rl WHERE " + " rl.rank=:rnk AND " +
					 * " rl.soldierGroup.soldierGroup=:grp AND " // +
					 * " rl.effectiveDate >=:frdt AND " + " rl.oldBasic =:basic") .setParameter(
					 * "rnk",Utils.getForeignKey(new Rank(), "rankCode",previousRankCode))
					 * .setParameter("grp",soldierGroup) .setParameter("basic", probable5pcBasic)
					 * .getResultList();
					 */

					if (reckonerList.size() > 0) {
						probable6pcBasic = reckonerList.get(0).getNewBasic();
					}

					List<PayScale> payList = psRepo
							.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),
									soldierGroup, previousRankDate);

					if (!previousRankDate.before(DateManager.getSixthCpcDate()) && payList != null && payList.size() > 0
							&& payList.get(0).getNewRecruitPay() != null
							&& probable6pcBasic < payList.get(0).getNewRecruitPay()
							&& employee.getEnrolmentDate() != null
							&& employee.getEnrolmentDate().before(DateManager.getSixthCpcDate())
							&& (rank.getRankCode().equals("20"))) {

						probable6pcBasic = payList.get(0).getNewRecruitPay();
					}
				}

				while (previousIncrDate.before(DateManager.getSeventhCpcDate())) {

					System.out.println(" Previous Incr Date xxxxxx " + previousIncrDate);
					List<String> descList = Arrays.asList(new String[] { "ACPFIX", "PROFIX" });
					List<Do2> acpProfixDo2List = do2Repo
							.findByDescListFromDateGreaterThanEqualToAndFromDateLessThanEqualTo(employee.getId(),
									descList, previousRankDate, previousIncrDate);

					/*
					 * Utils.getEntityManager().
					 * createQuery("select d from Do2 d where d.employee=:emp and description in ('ACPFIX', 'PROFIX') and "
					 * + "  d.fromDate>=:fromDate and d.fromDate<=:frDate  and status='V' ")
					 * .setParameter("emp", employee) .setParameter("fromDate", previousRankDate)
					 * .setParameter("frDate", previousIncrDate) .getResultList();
					 */

					List<Do2> incrDo2List = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanEqualToAndDesc(
							employee.getId(), "INCR", previousRankDate, previousIncrDate);

					/*
					 * Utils.getEntityManager().
					 * createQuery("select d from Do2 d where d.employee=:emp and description in ('INCR') and "
					 * + "  d.fromDate>=:fromDate and d.fromDate<=:frDate  and status='V' ")
					 * .setParameter("emp", employee) .setParameter("fromDate", previousRankDate)
					 * .setParameter("frDate", previousIncrDate) .getResultList();
					 */

					Calendar yy = new GregorianCalendar();
					yy.setTimeInMillis(previousRankDate.getTime());
					int zz = yy.get(Calendar.YEAR);
					Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
					Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
					Date jan01 = chkDate1.getTime();
					Date jul01 = chkDate2.getTime();

					if (previousRankDate.after(jan01) && previousRankDate.before(jul01)) {
						if (acpProfixDo2List.size() > 0 && incrDo2List.size() > 0) {

							System.out.println(" Problable 6pc basic  " + probable6pcBasic + " prev incr date "
									+ previousIncrDate + " grade pay " + probablegradepay + " xpay " + probable6pcXpay);
							probable6pcBasic = (probable6pcBasic) + Utils.getIncrmentRoundedToTen(
									probable6pcBasic + (probablegradepay) + (probable6pcXpay), previousIncrDate);
							System.out.println(" Problable 6pc basic 00 " + probable6pcBasic);
						}
					} else {
						if (incrDo2List.size() > 0) {

							System.out.println(" Problable 6pc basic  " + probable6pcBasic + " prev incr date "
									+ previousIncrDate + " grade pay " + probablegradepay + " xpay " + probable6pcXpay);
							probable6pcBasic = (probable6pcBasic) + Utils.getIncrmentRoundedToTen(
									probable6pcBasic + (probablegradepay) + (probable6pcXpay), previousIncrDate);
							System.out.println(" Problable 6pc basic 00 " + probable6pcBasic);
						}
					}

					previousIncrDate = Utils.getNextIncrementDate(previousIncrDate);
				}
				// System.out.println ( " Problable 7pc basic xxx " +probable7pcBasic +"
				// previous incr date " +previousIncrDate);
				// previousIncrDate=Utils.getNextIncrementDate(previousIncrDate);
				// System.out.println ( " Problable 7pc basic xxx " +probable7pcBasic +"
				// previous incr date " +previousIncrDate);
			} else {
				if (previousRankCode.equals("40") && do2.getDescription().equals("LORANK") && do2.getColumnA().equals("NK")) {
					probablegradepay = 2400;
				} else if (previousRankCode.equals("30") || previousRankCode.equals("29")) {
					probablegradepay = 2400;
				} else {
					probablegradepay = 2000;
				}

				System.out.println(" Problable 6pc basic 11  " + probable6pcBasic+"  currentpaylevel "+currentRankPayLevel+"  probgradepay " +probablegradepay+"  prevrankcode "+previousRankCode+"  prevrankdate "+previousRankDate);

				if (previousRankDate.before(DateManager.getSeventhCpcDate())) {
					probable7pcBasic = (int) Math.round((probable6pcBasic + probablegradepay) * 2.57);
					List<PayMatrix> paymatrixList = matrixRepo
							.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(currentRankPayLevel,
									probable7pcBasic);

					if (paymatrixList.size() > 0) {
						probable7pcBasic = paymatrixList.get(0).getBasicPay();
					}

				}

				System.out.println(" Problable 6pc basic 11  " + probable6pcBasic);

				System.out.println(" Problable 7pc basic  " + probable7pcBasic);

				System.out.println(
						" Problable 7pc basic 00 " + probable7pcBasic + "  previous incr date " + previousIncrDate);
				while (previousIncrDate.before(do2.getFromDate())) {

					System.out.println(
							" PREVIOUS INCR DATE xxxx " + previousIncrDate + "  do2 from date " + do2.getFromDate());
					String incrYear = getYear(previousIncrDate);
					// String incrMonth = getMonth(preivousIncrDate);

					String do2fromDateYear = "";
					List<Do2> incrDo2List1=null;
					incrDo2List1 = do2Repo.findByDescFromDateGreaterThanEqualAndLessThanCurrentDo2FromDate(employee.getId(), "V", "INCR",	previousIncrDate, do2.getFromDate());
System.out.println(".......incrdo2list1 size "+incrDo2List1.size()+"  previncrdt " +previousIncrDate+"  do2fromdate " +do2.getFromDate());
					if (incrDo2List1.size() > 0) {
						System.out.println(" IMA HERE IN SIZE");
						do2fromDateYear = getYear(incrDo2List1.get(0).getFromDate());
						System.out.println(" do2 from year " + do2fromDateYear +"  incrdo2list fromdt " +incrDo2List1.get(0).getFromDate());
						 	System.out.println(" no of months   "
						 		+ DateManager.getNoOfMonths(incrDo2List1.get(0).getFromDate(), previousIncrDate));
			
					}
					// System.out.println ( " INCR DO@ LIST XXXX YYY
					// "+incrDo2List1.get(0).getFromDate() +" increment year " +incrYear +"
					// do2fomDateyear " +do2fromDateYear);

					// System.out.println ( " incr do2 list from date
					// "+incrDo2List1.get(0).getFromDate() +" previous incr date "
					// +previousIncrDate);
					//
				//	System.out.println(" no of months   "
					//		+ DateManager.getNoOfMonths(incrDo2List1.get(0).getFromDate(), previousIncrDate));
					// System.out.println ( " no of months xxxx "
					// +DateManager.getNoOfMonths(previousIncrDate,incrDo2List1.get(0).getFromDate()));

					if (incrDo2List1 != null && incrDo2List1.size() > 0 && incrYear.equals(do2fromDateYear)) {
						List<PayMatrix> paymatrixList = matrixRepo
								.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(currentRankPayLevel,
										probable7pcBasic);
						System.out.println(" XXXKKKKKKXX " + previousRankPayLevel + " basic 7pc " + probable7pcBasic
								+ " current pay level " + currentRankPayLevel);
						System.out.println(" AAAA paymatrix List size BBBBB " + paymatrixList.size());
						if (paymatrixList.size() > 0) {
							probable7pcBasic = paymatrixList.get(0).getBasicPay();
						}

					} else if ((incrDo2List1 != null && incrDo2List1.size() > 0
							&& DateManager.getNoOfMonths(incrDo2List1.get(0).getFromDate(), previousIncrDate) >= 6)
							|| (incrDo2List1 != null  && incrDo2List1.size() > 0 && DateManager.getNoOfMonths(previousIncrDate,
									incrDo2List1.get(0).getFromDate()) >= 6)) {

						List<PayMatrix> paymatrixList = matrixRepo
								.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(currentRankPayLevel,
										probable7pcBasic);
						System.out.println(" pay matrix list size " + paymatrixList.size() + "PAY LEVEL "
								+ currentRankPayLevel + " probable basic pay 7cpc " + probable7pcBasic);
						if (paymatrixList.size() > 0) {
							probable7pcBasic = paymatrixList.get(0).getBasicPay();
						}

					}
					previousIncrDate = DateManager.getNextIncrementDate(previousIncrDate);
				}
				previousIncrDate = DateManager.getNextIncrementDate(previousIncrDate);

				nAuditCage.setBasicPay(probable7pcBasic);
				nAuditCage.setPayLevel(currentRankPayLevel);
				employee.setIncrementDate(previousIncrDate);
				return;
			}
		}

		if (previousRankCode.equals("40")) {
			probablegradepay = 2800;
		} else if (previousRankCode.equals("30")) {
			probablegradepay = 2400;
		} else {
			probablegradepay = 2000;
		}

		probable7pcBasic = (int) Math.round((probable6pcBasic + probablegradepay) * 2.57);

		List<PayMatrix> paymatrixList = matrixRepo
				.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(previousRankPayLevel, probable7pcBasic);

		/*
		 * Utils.getEntityManager().createQuery(
		 * " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND " +
		 * " pm.basicPay>=:sevencpcpay order by basicPay asc ")
		 * .setParameter("paylevel", previousRankPayLevel) .setParameter("sevencpcpay",
		 * probable7pcBasic) .getResultList();
		 */

		List<Do2> incrDo2List2 = do2Repo.findByEmployeeDescriptionAndFromDate(employee.getId(), "INCR",
				previousIncrDate);

		/*
		 * Utils.getEntityManager().
		 * createQuery("select d from Do2 d where d.employee=:emp and description in ('INCR') and "
		 * + "  d.fromDate=:fromDate  and status='V' ") .setParameter("emp", employee)
		 * .setParameter("fromDate", previousIncrDate) .getResultList();
		 */

		if (incrDo2List2.size() > 0) {
			if (paymatrixList.size() > 0) {
				probable7pcBasic = paymatrixList.get(0).getBasicPay();
			}
		}

		previousIncrDate = DateManager.getNextIncrementDate(previousIncrDate);
		nAuditCage.setBasicPay(probable7pcBasic);
		nAuditCage.setPayLevel(currentRankPayLevel);
		employee.setIncrementDate(previousIncrDate);

	}

	static public String getYear(Date date) {
		String strdate = null;
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		String mm = null, dd = null;

		String mth = String.valueOf(gc.get(Calendar.MONTH) + 1);
		if (mth.length() < 2)
			mm = "0".concat(mth);
		else
			mm = mth;

		String day = String.valueOf(gc.get(Calendar.DATE));

		if (day.length() < 2)
			dd = "0".concat(day);
		else
			dd = day;
		String yyyy = String.valueOf(gc.get(Calendar.YEAR));

		strdate = dd + mm + yyyy;

		return yyyy;
	}

	static public Date getNextIncrementDate(Date currentIncrementDate) {

		Date dni = new Date();
		Calendar currentIncrCal = new GregorianCalendar();
		Calendar nextIncrCal = new GregorianCalendar();

		currentIncrCal.setTime(currentIncrementDate);
		int date = currentIncrCal.get(Calendar.DATE);
		int year = currentIncrCal.get(Calendar.YEAR);
		int month = currentIncrCal.get(Calendar.MONTH);

		if ((year + 1) < 2006) {
			nextIncrCal = new GregorianCalendar(year + 1, month, 1);
		} else if (((year + 1) >= 2006) && ((year + 1) <= 2016)) {
			if (month == Calendar.JANUARY && date == 1 && year == 2005) {
				nextIncrCal = new GregorianCalendar(year + 1, month, 1);
			} else if (month == Calendar.JANUARY && date == 1) {
				nextIncrCal = new GregorianCalendar(year, Calendar.JULY, 1);
			} else {
				nextIncrCal = new GregorianCalendar(year + 1, Calendar.JULY, 1);
			}
		} else if ((year + 1) > 2016) {
			if (month == Calendar.JANUARY && date == 1) {
				nextIncrCal = new GregorianCalendar(year + 1, Calendar.JANUARY, 1);
			} else if (month == Calendar.JULY && date == 1) {
				nextIncrCal = new GregorianCalendar(year + 1, Calendar.JULY, 1);
			} else if (((month == Calendar.JANUARY && date > 1) || (month > Calendar.JANUARY && date >= 1))
					&& month < Calendar.JULY) {
				nextIncrCal = new GregorianCalendar(year + 1, Calendar.JANUARY, 1);
			} else if (((month == Calendar.JULY && date > 1) || (month > Calendar.JULY && date >= 1))
					&& month <= Calendar.DECEMBER) {
				nextIncrCal = new GregorianCalendar(year + 1, Calendar.JULY, 1);
			}
		}
		dni.setTime(nextIncrCal.getTimeInMillis());
		return dni;
	}

	/*
	 * static public Date getNextIncrementDate(Date currentIncrementDate) { Date
	 * nextIncrementDate = new Date(); Calendar cal = new GregorianCalendar();
	 * cal.setTime(currentIncrementDate); cal.set(Calendar.DATE,1);
	 * 
	 * if(currentIncrementDate.before(DateManager.getSixthCpcDate())){
	 * cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
	 * nextIncrementDate.setTime(cal.getTimeInMillis()); if
	 * (nextIncrementDate.after(DateManager.getSixthCpcDate())){
	 * cal.setTime(nextIncrementDate); cal.set(Calendar.DATE,1);
	 * cal.set(Calendar.MONTH,Calendar.JULY);
	 * nextIncrementDate.setTime(cal.getTimeInMillis()); } }else
	 * if((cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DATE) ==
	 * 1) && (!currentIncrementDate.before(DateManager.getSixthCpcDate()))){
	 * cal.set(Calendar.DATE,1); cal.set(Calendar.MONTH,Calendar.JULY);
	 * nextIncrementDate.setTime(cal.getTimeInMillis()); } else{
	 * 
	 * cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
	 * cal.set(Calendar.MONTH,Calendar.JULY); cal.set(Calendar.DATE,1);
	 * nextIncrementDate.setTime(cal.getTimeInMillis()); }
	 * 
	 * return nextIncrementDate; }
	 */

}