package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

/**
 * Author : IT & SDC, Secunderabad
 */
@RestController
public class Taincr extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	private static final long serialVersionUID = 1L;
	Employee employee;
	List<PayScale> payList;
	boolean incrValidity = true;
	StringBuffer buffer = new StringBuffer();
	Do2 do2;
	static String IS_A_RECRUIT = "AU:PBOR is a Recruit.\n";
	static String FS_DUE = "AU:Increment Date Beyond FS Due Date.\n";
	static String DUPLICATE_INCR_DO2 = "DU:Duplicate Incr Do2. \n";
	static String HONYCOMMN_THIS_YEAR = "AU:Honorary Commission granted this year. \n";
	static public String TA_DEPTAL_EMP = "AU:PBOR belongs to TA deptal category - not entitled\n";
	static public String NOT_BELONGS_TO_TA = "AU:PBOR does not belong to Territorial Army \n";
	static public String APPOINTMENT_CATG_MISSING = "AU:Appointment Category for TA not available \n";
	static public String MISSING_SERVICE_DETAIL = "AU:Service Details Not available. \n";
	static String INCR_DO2_WITHIN_ONE_YEAR = "DU:Incr Do2 found adjusted within previous 12 months \n";
	static String PROMOT_MACPS_WITHIN_ONE_YEAR = "AU:One Year Not Completed Since Recent Beneficial PROMOTION/MACPS Date \n";

	@Override
	public boolean isValid(Do2 do2) {
		buffer.setLength(0);
		employee = do2.getEmployee();

		if (employee == null)
			employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (!employee.getRegularTaFlag().equals("T")) {
			do2.setReason(NOT_BELONGS_TO_TA);
			do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);
			return false;
		}

		if (employee.getTaAppointmentCategory() == null) {
			do2.setReason(APPOINTMENT_CATG_MISSING);
			do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);
			return false;
		}

		if (employee.getTaAppointmentCategory() != null && employee.getTaAppointmentCategory().equals("D")) {
			do2.setReason(TA_DEPTAL_EMP);
			do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);
			return false;
		}

		if (!employee.getTaAppointmentCategory().equals("D")) {

			if (doesDuplicateIncrDo2Exist(do2, employee)) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(DUPLICATE_INCR_DO2);
				incrValidity = false;
				return false;
			}

			if (doesIncrDo2ExistWithinOneYear(do2, employee)) {
				do2.setStatus(Do2Manager.PENDING);
				do2.setReason(INCR_DO2_WITHIN_ONE_YEAR);
				incrValidity = false;
				return false;
			}

			if (doesPromotMacpsExistsWithinOneYear(do2, employee)) {
				do2.setStatus(Do2Manager.PENDING);
				do2.setReason(PROMOT_MACPS_WITHIN_ONE_YEAR);
				incrValidity = false;
				return false;
			}

			Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
			if (rank == null) {
				incrValidity = false;
				buffer.append(MISSING_SERVICE_DETAIL);
			} else if (rank.getRankCode().equals(Do2Manager.RECRUIT)) {
				incrValidity = false;
				buffer.append(IS_A_RECRUIT);
			}

			if (isHonoraryCommissionedThisYear(do2, employee)) {
				incrValidity = false;
				buffer.append(HONYCOMMN_THIS_YEAR);
			}

			if (employee.getFsDueDate() != null && !employee.getFsDueDate().after(do2.getFromDate())) {
				incrValidity = false;
				buffer.append(FS_DUE);
			}
		}

		if (!incrValidity) {
			if (do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING)) {
				do2.setStatus(Do2Manager.INVALID);
			}
		} else {
			do2.setStatus(Do2Manager.VALID);
		}

		do2.setReason(buffer.toString());

		do2Repo.save(do2);
		return incrValidity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {

		employee = Utils.getCurrentEmployee(baseAuditCage.getDo2().getArmyNo());

		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
				|| (newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())
						&& (newAuditCage.getGradePay() == null
								|| (newAuditCage.getGradePay() != null && newAuditCage.getGradePay() == 0)))) {
			if (newAuditCage.getBasicPay() == 2440 || newAuditCage.getBasicPay() == 2850) {
				return newAuditCage;
			}
			newAuditCage.setBasicPay(get5cpcPay(newAuditCage.getDo2(), baseAuditCage));
			if (!newAuditCage.getFromDate().before(DateManager.getDPDate())) {
				newAuditCage.setDp(getDp(newAuditCage.getDo2(), baseAuditCage));
			}
			return newAuditCage;
		} else // if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
		if (!newAuditCage.getFromDate().after(DateManager.getSeventhCpcDate())) { // amended on 16.06.2025 paopara irla
			if (employee.getSubstantiveRank() != null
					&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)
					&& (newAuditCage.getBasicPay() == 11400 || newAuditCage.getBasicPay() == 8550
							|| newAuditCage.getBasicPay() == 5700)) {
				return newAuditCage;
			}
			newAuditCage.setBasicPay(get6cpcPay(newAuditCage.getDo2(), baseAuditCage, newAuditCage.getFromDate()));
			newAuditCage.setOldBasicPay(getOldBasicPay(newAuditCage.getDo2(), baseAuditCage));

			// Transport Allowance
			if (newAuditCage.getTransport() != null && newAuditCage.getTransport() != 0) {
				newAuditCage.setTransport(AuditCageHelper.getTransportRate(newAuditCage, baseAuditCage));
			}
		} else {
			if (newAuditCage.getGradePay() == 0) {
				newAuditCage.setBasicPay(get7cpcPay(newAuditCage.getDo2(), baseAuditCage, newAuditCage.getFromDate()));
				newAuditCage.setOldBasicPay(get6cpcOldPay(newAuditCage.getDo2(), baseAuditCage));
			}
		}
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		employee = baseAuditCage.getEmployee();
		Calendar nextIncrCal = new GregorianCalendar();
		nextIncrCal.setTime(do2.getFromDate());
		int nextincryear = nextIncrCal.get(Calendar.YEAR) + 1;
		if (((nextincryear) % 4) == 0) {
			employee.setIncrementDate(DateManager.getDate(do2.getFromDate(), 366));
		} else {
			employee.setIncrementDate(DateManager.getDate(do2.getFromDate(), 365));
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	// ******************************************************************************
	public int get5cpcPay(Do2 do2, AuditCage auditCage) {
		int newBasic = auditCage.getBasicPay();
		try {
			Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PAYFIX");
			GroupCategory groupCategory = Utils.getGroupCategory(do2, "PREV");
			payList = psRepo.findByRankGroupFromDateToDateStartScaleGreaterThanZero(rank.getId(),
					groupCategory.getSoldierGroup(), DateManager.getDate("1997-10-10"),
					DateManager.getDate("2005-12-31"));

			/*
			 * Utils.getEntityManager().
			 * createQuery("SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank AND ps.startScale>0 "
			 * +
			 * " AND ps.soldierGroup.soldierGroup=:grp AND ps.fromDate='1997-10-10' AND ps.toDate='2005-12-31' "
			 * ) .setParameter("rank", rank).setParameter("grp",
			 * groupCategory.getSoldierGroup()).getResultList();
			 */
		} catch (Exception e) {
			Utils.handleException(e);
		}
		if (payList != null && payList.size() > 0) {
			if ((newBasic + payList.get(0).getIncr0()) < payList.get(0).getScaleEnd0())
				newBasic += +payList.get(0).getIncr0();
			else
				newBasic = payList.get(0).getScaleEnd0();
		}
		return newBasic;
	}

	public int getDp(Do2 do2, AuditCage auditCage) {
		int newDp = 0;
		if (!auditCage.getEmployee().getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)
				&& !(do2.getFromDate().before(DateManager.getDPDate()))
				&& (do2.getFromDate().before(DateManager.getSixthCpcDate()))) {
			newDp = ((int) Math.round(auditCage.getBasicPay() * 0.50));
		}
		return newDp;
	}

	public int get6cpcPay(Do2 do2, AuditCage auditCage, Date promotDate) {
		int newBasic = auditCage.getBasicPay();

		newBasic = (auditCage.getBasicPay()
				+ Utils.getIncrmentRoundedToTen(
						auditCage.getBasicPay() + (auditCage.getGradePay() == null ? 0 : auditCage.getGradePay())
								+ (auditCage.getXGroupPay() == null ? 0 : auditCage.getXGroupPay()),
						auditCage.getFromDate()));

		// }
		return newBasic;
	}

	public boolean isFsDue(Do2 do2) {
		if (employee.getFsDueDate() != null && (employee.getFsDueDate().equals(do2.getFromDate())
				|| employee.getFsDueDate().before(do2.getFromDate())))
			return false;
		return true;
	}

	public int getOldBasicPay(Do2 do2, AuditCage auditCage) {
		int oldBasic = auditCage.getOldBasicPay();
		try {
			// Rank rank = Utils.getRankForPayFixation(do2.getArmyNo(), do2.getFromDate());
			Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PAYFIX");
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			GroupCategory groupCategory = Utils.getGroupCategory(do2, "PREV");
			if (groupCategory != null && rank != null) {
				payList = psRepo.findByRankGroupFromDateToDateStartScaleGreaterThanZero(rank.getId(),
						groupCategory.getSoldierGroup(), DateManager.getDate("1997-10-10"),
						DateManager.getDate("2005-12-31"));

				/*
				 * Utils .getEntityManager() .createQuery(
				 * "SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank " +
				 * " AND ps.startScale > 0 " + " AND ps.soldierGroup.soldierGroup=:grp " +
				 * " AND ps.fromDate=:fromDate " + "  AND ps.toDate=:toDate")
				 * .setParameter("rank", rank)
				 * .setParameter("grp",groupCategory.getSoldierGroup())
				 * .setParameter("fromDate", DateManager.getDate("1997-10-10"))
				 * .setParameter("toDate", DateManager.getDate("2005-12-31")) .getResultList();
				 */

			}

		} catch (Exception e) {
			Utils.handleException(e);

		}
		if (payList != null && payList.size() > 0)
			if (auditCage.getOldBasicPay() + payList.get(0).getIncr0() <= payList.get(0).getScaleEnd0())
				oldBasic = (auditCage.getOldBasicPay() + payList.get(0).getIncr0());
			else
				oldBasic = payList.get(0).getScaleEnd0();
		return oldBasic;
	}

	public boolean doesDuplicateIncrDo2Exist(Do2 do2, Employee emp) {

		List<Do2> duplicateDo2List = do2Repo.findByDescriptionStatusFromDateEqualToAndDo2IdNotEqualTo(emp.getId(),
				"TAINCR", "V", do2.getFromDate(), do2.getId());

		/*
		 * Utils.getEntityManager().
		 * createQuery("SELECT d FROM Do2 d WHERE d.id!=:id AND " +
		 * " (d.employee=:emp OR d.armyNo=:ano) AND d.description='TAINCR' AND d.status='V' AND d.fromDate=:fromDate "
		 * ) .setParameter("ano", do2.getArmyNo()).setParameter("emp",
		 * emp).setParameter("id", do2.getId()) .setParameter("fromDate",
		 * do2.getFromDate()).getResultList();
		 */

		return (duplicateDo2List.size() > 0);
	}

	public boolean doesIncrDo2ExistWithinOneYear(Do2 do2, Employee emp) {
		Date prevIncrDate = DateManager.getDate(do2.getFromDate(), -364);
		// System.out.println("jjjjjjjjjjjjjjjjjjjjjjjjjj do2 fromdate previncrdate
		// "+do2.getFromDate()+" "+prevIncrDate);
		
		
		
		
		List<Do2> incrDo2List = do2Repo.findFromDateBetweenAndDo2IdNotEqualForTaincr(emp.getId(), "TAINCR",
				prevIncrDate, do2.getFromDate(), do2.getId());

		/*
		 * Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
		 * " d.employee=:emp AND d.id!=:id AND d.description='TAINCR'  and d.status='V' "
		 * + " AND d.fromDate between :prevIncrDate and :currentDo2Date")
		 * .setParameter("id", do2.getId()).setParameter("emp", emp)
		 * .setParameter("prevIncrDate",prevIncrDate) .setParameter("currentDo2Date",
		 * do2.getFromDate()).getResultList();
		 */

		if (incrDo2List == null || (incrDo2List != null && incrDo2List.size() == 0))
			return false;

		System.out.println(
				"..........incrdo2list size " + incrDo2List.size() + "  fromdt " + incrDo2List.get(0).getFromDate()
						+ "  previncrdt " + prevIncrDate + "  do2fromdt " + do2.getFromDate());

		List<Do2> acpprofixDo2List = do2Repo.getValidProfixAcpfixIn7CPCforTaincr(emp.getId(),
				incrDo2List.get(0).getFromDate());
		if (acpprofixDo2List != null && acpprofixDo2List.size() > 0) {
			double noOfDays = DateManager.getNoOfDays(acpprofixDo2List.get(0).getFromDate(), do2.getFromDate());
			if (noOfDays > 180) {
				return false;
			}
		}

		return true;
	}

	/*
	 * static public Date getPreviousIncrementDate(Date currentIncrementDate) { Date
	 * dni = new Date(); Calendar currentIncrCal = new GregorianCalendar(); Calendar
	 * prevIncrCal = new GregorianCalendar();
	 * currentIncrCal.setTime(currentIncrementDate); int year =
	 * currentIncrCal.get(Calendar.YEAR); int month =
	 * currentIncrCal.get(Calendar.MONTH); prevIncrCal = new GregorianCalendar(year
	 * - 1, month, 1); dni.setTime(prevIncrCal.getTimeInMillis()); return dni; }
	 */

	public Date getPreviousIncrementDate(Date currentIncrementDate) {
		System.out.println("Calculating previous DNI for currentIncrementDate: " + currentIncrementDate);
		Calendar currentIncrCal = new GregorianCalendar();
		currentIncrCal.setTime(currentIncrementDate);
		int year = currentIncrCal.get(Calendar.YEAR);
		int month = currentIncrCal.get(Calendar.MONTH);
		int day = currentIncrCal.get(Calendar.DAY_OF_MONTH);

		// Calculate previous DNI by subtracting 6 months
		Calendar prevIncrCal = new GregorianCalendar();
		prevIncrCal.setTime(currentIncrementDate);
		prevIncrCal.add(Calendar.MONTH, -6);
		// Adjust day if necessary (e.g., for end-of-month cases like Feb 30)
		if (prevIncrCal.get(Calendar.DAY_OF_MONTH) != day) {
			prevIncrCal.set(Calendar.DAY_OF_MONTH, Math.min(day, prevIncrCal.getActualMaximum(Calendar.DAY_OF_MONTH)));
		}
		Date prevDNI = prevIncrCal.getTime();
		System.out.println("Calculated previous DNI: " + prevDNI);
		return prevDNI;
	}

	public boolean isHonoraryCommissionedThisYear(Do2 do2, Employee employee) {

		if (employee != null && employee.getSubstantiveRank() != null
				&& (employee.getSubstantiveRank().getRankName().equals("HONYLT")
						|| employee.getSubstantiveRank().getRankName().equals("HONYCAPT"))) {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(do2.getFromDate());
			Date firstJanuary = do2.getFromDate();
			try {
				firstJanuary = DateManager.getDate(cal.get(Calendar.YEAR), cal.get(Calendar.JANUARY), 1);
			} catch (Exception e) {
				Utils.handleException(e);
			}

			if ((employee.getHonyLieuDate() != null && !employee.getHonyLieuDate().before(firstJanuary)
					&& employee.getHonyLieuDate().before(do2.getFromDate()))
					|| (employee.getHonyCaptainDate() != null && !employee.getHonyCaptainDate().before(firstJanuary)
							&& employee.getHonyCaptainDate().before(do2.getFromDate()))) {
				return true;
			}
		}
		return false;
	}

	public int get7cpcPay(Do2 do2, AuditCage auditCage, Date promotDate) {

		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PAYFIX");
		int newBasic = auditCage.getBasicPay();

		String payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
		int atcgOldGradePay = auditCage.getOldGradePay();
		String payLevel1 = Do2Manager.getPayLevel(atcgOldGradePay);
		if (payLevel == null) {
			payLevel = auditCage.getPayLevel();
		}
		if (payLevel1 == null) {
			payLevel1 = auditCage.getPayLevel();
		}
		if (payLevel != null && payLevel1 != null && !payLevel.equals(payLevel1)) {
			// System.out.println ( " MISMATCH IN RANK & PAY LEVEL ");
			return newBasic;
		}
		List<PayMatrix> paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(payLevel,
				newBasic);

		/*
		 * Utils.getEntityManager().
		 * createQuery(" select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND " +
		 * " pm.basicPay>:sevencpcpay order by basicPay asc ").setParameter("paylevel",
		 * payLevel) .setParameter("sevencpcpay", newBasic).getResultList();
		 */
		if (paymatrixList.size() > 0) {
			newBasic = paymatrixList.get(0).getBasicPay();
		}

		return newBasic;
	}

	public int get6cpcOldPay(Do2 do2, AuditCage auditCage) {
		int oldBasic = auditCage.getBasicPay();
		oldBasic = (auditCage.getOldBasicPay() + (Utils.getIncrmentRoundedToTen(
				auditCage.getOldBasicPay() + (auditCage.getOldGradePay() == null ? 0 : auditCage.getOldGradePay())
						+ (auditCage.getXGroupPay() == null ? 0 : auditCage.getXGroupPay()),
				do2.getFromDate())));
		return oldBasic;
	}

	/*
	 * public boolean doesPromotMacpsExistsWithinOneYear(Do2 do2, Employee emp) {
	 * Date prevIncrDate = getPreviousIncrementDate(do2.getFromDate()); Date
	 * promotFromDate=null;
	 * 
	 * List<Do2> do2List =
	 * do2Repo.findByFromDateBetweenAndDescriptionListOrderByFromDateDesc(emp.getId(
	 * ), Do2Manager.INCR_CANCEL_DESCRIPTIONS, prevIncrDate, do2.getFromDate());
	 * 
	 * Utils.getEntityManager().
	 * createQuery("SELECT d FROM Do2 d WHERE (d.employee=:emp OR d.armyNo=:ano) AND "
	 * +
	 * " d.description in (:promotDesc) AND d.status='V' AND d.fromDate between :prevIncrDate and :currentIncrDate "
	 * + " order by d.fromDate desc").setParameter("ano",
	 * do2.getArmyNo()).setParameter("emp", emp) .setParameter("promotDesc",
	 * Do2Manager.INCR_CANCEL_DESCRIPTIONS).setParameter("prevIncrDate",
	 * prevIncrDate) .setParameter("currentIncrDate",
	 * do2.getFromDate()).getResultList(); //
	 * System.out.println("vvvvvvvvvvvvvvv  do2 list siz e  " +do2List.size()); if
	 * (do2List!=null && do2List.size()>0) {
	 * promotFromDate=getAcpDateBeforeCurrentDo2Date(do2List.get(0), emp);
	 * 
	 * // System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv promtfromdate "
	 * +promotFromDate); List<Do2> prevIncrDo2List =
	 * do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(emp.getId(), "V",
	 * "TAINCR", do2.getFromDate());
	 * 
	 * Utils.getEntityManager().
	 * createQuery("SELECT d FROM Do2 d WHERE d.id!=:id AND " +
	 * " (d.employee=:emp OR d.armyNo=:ano) AND d.description='TAINCR' AND d.status='V' "
	 * + " AND d.fromDate<:fromDate ORDER BY d.fromDate DESC ").setParameter("ano",
	 * do2.getArmyNo()) .setParameter("emp", emp).setParameter("id", do2.getId())
	 * .setParameter("fromDate", do2.getFromDate()).getResultList();
	 * 
	 * if (prevIncrDo2List!=null && prevIncrDo2List.size()>0 &&
	 * promotFromDate!=null) { if (DateManager.getNoOfDays(promotFromDate,
	 * do2.getFromDate()) < 365) { return true; } else { return false; } } } return
	 * false; }
	 * 
	 * public Date getAcpDateBeforeCurrentDo2Date(Do2 do2, Employee employee) {
	 * 
	 * if (do2.getDescription().equals("STEPUP")) return null;
	 * 
	 * Date revertDate = null; Date acpDate = null; String rankName =
	 * do2.getColumnA();
	 * 
	 * if (do2.getDescription().equals("HCOMMN")) { List<Do2>
	 * macpList=do2Repo.findByEmployeeIdAndDescriptionAndColumnAAndStatus(employee.
	 * getId(),"MACPS","HONYGRADE","V");
	 * 
	 * // Utils.getEntityManager().createQuery("select d from Do2 d where " // +
	 * " d.employee=:emp and d.description='MACPS' and d.columnA='HONYGRADE' and d.status='V'"
	 * ) // .setParameter("emp", employee).getResultList(); if (macpList.size() >
	 * 0){ if (macpList.get(0).getFromDate() != null &&
	 * macpList.get(0).getFromDate().before(do2.getFromDate()) &&
	 * do2.getColumnA().equals("HONYLT")) { acpDate = macpList.get(0).getFromDate();
	 * } } }
	 * 
	 * if (do2.getDescription().equals("MSTHAV")) {
	 * rankName=do2.getDescription().substring(3); }
	 * 
	 * if (do2.getDescription().equals("MSTJCO")) { rankName="NBSUB"; }
	 * 
	 * if (do2.getColumnA()!=null) { if (!do2.getColumnA().equals("NBSUB") &&
	 * !do2.getColumnA().equals("NBRIS")) { rankName=do2.getColumnA().substring(2);
	 * } if (rankName!=null) { if ((rankName.equals("NK") || rankName.equals("LD"))
	 * && employee.getAcpnaikDate()!=null) { acpDate=employee.getAcpnaikDate(); }
	 * else if ((rankName.equals("HAV") || rankName.equals("DFR")) &&
	 * employee.getAcphavDate()!=null) { acpDate=employee.getAcphavDate(); } else if
	 * ((rankName.equals("NBSUB") || rankName.equals("NBRIS")) &&
	 * employee.getAcpnbsubDate()!=null) { acpDate = employee.getAcpnbsubDate(); }
	 * else if ((rankName.equals("SUB") || rankName.equals("RIS")) &&
	 * employee.getAcpsubDate()!=null) { acpDate = employee.getAcpsubDate(); } else
	 * if ((rankName.equals("SUBMAJ") || rankName.equals("RISMAJ")) &&
	 * employee.getAcpsubmajDate()!=null) { acpDate = employee.getAcpsubmajDate(); }
	 * } }
	 * 
	 * revertDate = Utils.getRevertDateForPromot(do2);
	 * 
	 * if (acpDate!=null && revertDate!=null) { if (acpDate.after(revertDate) &&
	 * !do2.getFromDate().before(acpDate)) { return acpDate; } } if
	 * (revertDate==null && acpDate!=null && !acpDate.after(do2.getFromDate())) {
	 * return acpDate; }
	 * 
	 * if (do2.getDescription().startsWith("MST") && acpDate==null &&
	 * revertDate==null) { return null; } return null; }
	 */
	public boolean doesPromotMacpsExistsWithinOneYear(Do2 do2, Employee emp) {
		System.out.println("Starting doesPromotMacpsExistsWithinOneYear for Do2 ID: " + do2.getId() + ", ArmyNo: " + do2.getArmyNo() + ", FromDate: " + do2.getFromDate());

		// Calculate previous DNI for ACPFIX/PROFIX check (6 months prior)
		Date previousDNI = getPreviousIncrementDate(do2.getFromDate());
		System.out.println("Previous DNI for fixation check: " + previousDNI);

		// First, check for valid ACPFIX or PROFIX on the previous DNI
		List<Do2> fixationDo2List = do2Repo.findByEmployeeIdAndDescriptionInAndFromDateAndStatus(emp.getId(), List.of("ACPFIX", "PROFIX"), previousDNI, "V");
		System.out.println("Fixation DO-IIs found: " + (fixationDo2List != null ? fixationDo2List.size() : 0));
		if (fixationDo2List != null && fixationDo2List.size() > 0) {
			double noOfDays = DateManager.getNoOfDays(previousDNI, do2.getFromDate());
			System.out.println("Days between fixation (" + previousDNI + ") and TAINCR (" + do2.getFromDate() + "): " + noOfDays);
			if (noOfDays < 180) {
				System.out.println("Pending: Less than 6 months since fixation");
				return true; // Pending if <6 months from fixation
			} else {
				System.out.println("Valid: 6 months or more since fixation");
				return false; // Valid if >=6 months
			}
		}

		// If no fixation, check for promotion/MACPS in the last 12 months
		Date prevIncrDate12Months = DateManager.getDate(do2.getFromDate(), -364); // 12-month window
		System.out.println("No fixation found, checking for promotion/MACPS between " + prevIncrDate12Months + " and " + do2.getFromDate());
		List<Do2> do2List = do2Repo.findByFromDateBetweenAndDescriptionListOrderByFromDateDesc(emp.getId(), Do2Manager.INCR_CANCEL_DESCRIPTIONS,
				prevIncrDate12Months, do2.getFromDate());
		System.out.println("Promotion/MACPS DO-IIs found: " + (do2List != null ? do2List.size() : 0));

		if (do2List != null && do2List.size() > 0) {
			Date promotFromDate = getAcpDateBeforeCurrentDo2Date(do2List.get(0), emp);
			System.out.println("Promotion/MACPS date from getAcpDateBeforeCurrentDo2Date: " + promotFromDate);

			List<Do2> prevIncrDo2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(emp.getId(),
					"V", "TAINCR", do2.getFromDate());
			System.out.println("Previous TAINCR DO-IIs found: " + (prevIncrDo2List != null ? prevIncrDo2List.size() : 0));
			
			if (prevIncrDo2List!=null && prevIncrDo2List.size()>0) {
		 		double noOfDaysSinceLastIncr=DateManager.getNoOfDays(prevIncrDo2List.get(0).getFromDate(), do2.getFromDate());	
		 		System.out.println ( " noOfDaysSinceLastIncr " +noOfDaysSinceLastIncr  + " last incr from date " +prevIncrDo2List.get(0).getFromDate()  +" do2 from date " +do2.getFromDate());
 		 		 if ((prevIncrDo2List.get(0).getColumnC()==null || (prevIncrDo2List!=null && prevIncrDo2List.get(0).getColumnC().length()>0))
		 	 			&& noOfDaysSinceLastIncr>364) {
		 			return false;
		 		} else {
		 			List<String> descList2=Arrays.asList(new String[] {"ACPFIX","PROFIX","HAVFIX"});
		 			List<Do2> payfixDo2List = do2Repo.findByDescListFromdateGreaterThanFromDateLessThanAndDoIdNotEqualTo(emp.getId(),
		 																						descList2,previousDNI,do2.getFromDate(),do2.getId());
		 					
		 			/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND "
							+ " d.description in ('ACPFIX', 'PROFIX', 'HAVFIX') AND d.status='V' and d.fromDate>=:lastIncrDate and d.fromDate<:thisIncrDate and d.id!=:did ORDER BY " 
							+ " d.fromDate DESC ").setParameter("emp", emp).setParameter("lastIncrDate", oneYearPriorDate)
							.setParameter("thisIncrDate", do2.getFromDate()).setParameter("did", do2.getId()).getResultList();*/
		 			if (payfixDo2List == null || (payfixDo2List!=null && payfixDo2List.size()==0)) {
		 				return true;
		 			} else {
		 				return false;	
		 			}			 			
		 		}			 		 
			}

		/*	if (promotFromDate != null) {
				double noOfDays = DateManager.getNoOfDays(promotFromDate, do2.getFromDate());
				System.out.println("Days between promotion/MACPS (" + promotFromDate + ") and TAINCR (" + do2.getFromDate() + "): " + noOfDays);
				if (noOfDays < 365) {
					System.out.println("Pending: Less than 12 months since promotion/MACPS");
					return true; // Pending if <12 months
				} else {
					System.out.println("Valid: 12 months or more since promotion/MACPS");
					return false; // Valid
				}
			}*/
		}
		System.out.println("No promotion/MACPS or valid date found, returning false");
		return false;
	}

	public Date getAcpDateBeforeCurrentDo2Date(Do2 do2, Employee employee) {
		System.out.println("Starting getAcpDateBeforeCurrentDo2Date for Do2 ID: " + do2.getId() + ", Description: " + do2.getDescription() + ", FromDate: " + do2.getFromDate());

		if (do2.getDescription().equals("STEPUP")) {
			System.out.println("Do2 is STEPUP, returning null");
			return null;
		}

		Date revertDate = null;
		Date acpDate = null;
		String rankName = do2.getColumnA();
		System.out.println("Initial rankName: " + rankName);

		if (do2.getDescription().equals("HCOMMN")) {
			List<Do2> macpList = do2Repo.findByEmployeeIdAndDescriptionAndColumnAAndStatus(employee.getId(), "MACPS", "HONYGRADE", "V");
			System.out.println("MACPS HONYGRADE DO-IIs found: " + (macpList != null ? macpList.size() : 0));
			if (macpList.size() > 0) {
				if (macpList.get(0).getFromDate() != null && macpList.get(0).getFromDate().before(do2.getFromDate())
						&& do2.getColumnA().equals("HONYLT")) {
					acpDate = macpList.get(0).getFromDate();
					System.out.println("HCOMMN found with MACPS HONYGRADE, acpDate: " + acpDate);
				}
			}
		}

		if (do2.getDescription().equals("MSTHAV")) {
			rankName = do2.getDescription().substring(3);
			System.out.println("MSTHAV detected, updated rankName: " + rankName);
		}

		if (do2.getDescription().equals("MSTJCO")) {
			rankName = "NBSUB";
			System.out.println("MSTJCO detected, updated rankName: " + rankName);
		}

		if (do2.getColumnA() != null) {
			if (!do2.getColumnA().equals("NBSUB") && !do2.getColumnA().equals("NBRIS")) {
				rankName = do2.getColumnA().substring(2);
				System.out.println("ColumnA not NBSUB/NBRIS, updated rankName: " + rankName);
			}
			if (rankName != null) {
				if ((rankName.equals("NK") || rankName.equals("LD")) && employee.getAcpnaikDate() != null) {
					acpDate = employee.getAcpnaikDate();
					System.out.println("Rank NK/LD, acpDate set to AcpnaikDate: " + acpDate);
				} else if ((rankName.equals("HAV") || rankName.equals("DFR")) && employee.getAcphavDate() != null) {
					acpDate = employee.getAcphavDate();
					System.out.println("Rank HAV/DFR, acpDate set to AcphavDate: " + acpDate);
				} else if ((rankName.equals("NBSUB") || rankName.equals("NBRIS")) && employee.getAcpnbsubDate() != null) {
					acpDate = employee.getAcpnbsubDate();
					System.out.println("Rank NBSUB/NBRIS, acpDate set to AcpnbsubDate: " + acpDate);
				} else if ((rankName.equals("SUB") || rankName.equals("RIS")) && employee.getAcpsubDate() != null) {
					acpDate = employee.getAcpsubDate();
					System.out.println("Rank SUB/RIS, acpDate set to AcpsubDate: " + acpDate);
				} else if ((rankName.equals("SUBMAJ") || rankName.equals("RISMAJ")) && employee.getAcpsubmajDate() != null) {
					acpDate = employee.getAcpsubmajDate();
					System.out.println("Rank SUBMAJ/RISMAJ, acpDate set to AcpsubmajDate: " + acpDate);
				}
			}
		}

		revertDate = Utils.getRevertDateForPromot(do2);
		System.out.println("RevertDate from Utils.getRevertDateForPromot: " + revertDate);

		if (acpDate != null && revertDate != null) {
			if (acpDate.after(revertDate) && !do2.getFromDate().before(acpDate)) {
				System.out.println("Returning acpDate: " + acpDate + " (acpDate after revertDate and not before Do2 fromDate)");
				return acpDate;
			}
		}
		if (revertDate == null && acpDate != null && !acpDate.after(do2.getFromDate())) {
			System.out.println("Returning acpDate: " + acpDate + " (no revertDate and acpDate not after Do2 fromDate)");
			return acpDate;
		}

		if (do2.getDescription().startsWith("MST") && acpDate == null && revertDate == null) {
			System.out.println("MST description with no acpDate or revertDate, returning null");
			return null;
		}
		System.out.println("No valid acpDate found, returning null");
		return null;
	}
}
