package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Unit;

import itsdc.dolphin.utils.Utils;
import itsdc.dolphin.utils.DateManager;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;

/**
 * 
 * @author ITSDC, Sec'bad.
 *
 */

@RestController
public class Hra extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	public List<Allowance> alwList;
	static public String INVALID_AGE = "AU:PBOR has not completed 25 / 21 years of Age.\n";
	static public String INVALID_HRA = "AU:HRA is not admissible to Unmarried PBOR.\n";
	static public String INVALID_REFDO2 = "VL:Reference Do2 not provided for Subsequent Claim.\n";
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";
	static public String CITY_CLASS_DIFFERS = "AU:Class of City differs from previous grant of HRA.\n";
	static public String INVALID_COLUMNC = "AU:ColumnC is not valid. On deputation other town rate is eligible.\n";
	static public String ORIGINAL_CITY_MISMATCH = "AU:ColumnC-City Value Mismatch With Initial DO2.\n";

	public StringBuffer buffer = new StringBuffer();
	Rank rank;
	Employee emp;

	@Override
	public boolean isValid(Do2 do2) {
		if (do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
		boolean validity = true;

		emp = Utils.getCurrentEmployee(do2.getArmyNo(), do2.getCheckDigit());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");

		// validation of age removed for do2s having from date >= 01-07-2017 vide HQRs
		// letter
		// No. AT/I/3510/7thCPC/VI dated 10-02-2020.

		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			if (!isValidAge(do2)) {
				validity = false;
				buffer.append(INVALID_AGE);
				do2.setStatus(Do2Manager.INVALID); // Dolphin System Circular 326
			}
		}

		if (do2.getAcceptedQe() == null && do2.getEmployee().getStatusCode().getId() < 5) {
			if (!isValidAllowanceRate(do2)) {
				validity = false;
				buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
				do2.setStatus(Do2Manager.PENDING);
			}
		}

		/*
		 * if (!isCityMatchWithInitialClaim(do2, emp)) { validity = false;
		 * buffer.append(ORIGINAL_CITY_MISMATCH); do2.setStatus(Do2Manager.INVALID); }
		 */

		if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			if (do2.getColumnA().trim().equals("GRANTED")) {
				if (isPborOnDeputationCcz(do2)) {
					// System.out.println ( " SUB STRING " +do2.getColumnC().substring(0,3));
					/*
					 * if (!do2.getColumnC().substring(0,3).equals("CCZ")){
					 * do2.setStatus(Do2Manager.PENDING); buffer.append(INVALID_COLUMNC);
					 */
					validity = true;
				}
			}
		}

		if (do2.getAcceptedQe() == null && do2.getReviewQe() == null) {
			if (!isClassOfCitySame(do2)) {
				validity = false;
				// do2.setStatus(Do2Manager.PENDING);
				do2.setStatus(Do2Manager.INVALID); // Dolphin System Circular 326
				buffer.append(CITY_CLASS_DIFFERS);
			}
		}

		if (!validity) {
			if (do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING)) {
				do2.setStatus(Do2Manager.INVALID);
			}
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		createSysDo2(do2);

		return validity;
	}

	public boolean isValidAllowanceRate(Do2 do2) {
		String columnC = "";
		alwList = null;

		try {
			if (do2.getDescription().equals("HRA") && do2.getColumnA().equals("GRANTED")
					&& !do2.getDo2Date().before(DateManager.getDate("2022-06-01"))
					&& !do2.getFromDate().before(DateManager.getDate("2022-04-08"))
					&& (do2.getRate() == null || (do2.getRate() != null && do2.getRate() >= 0))) {
				return true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (do2.getColumnC() != null) {
			if (do2.getColumnC().startsWith("CCZ")) {
				columnC = "CCZ-1";
			}
			if (do2.getColumnC().startsWith("CCY")) {
				columnC = "CCY-1";
			}
			if (do2.getColumnC().startsWith("CCX")) {
				columnC = "CCX-1";
			}
		}
		if (do2.getRate()!=null && do2.getRate()>0) { 
		if (Utils.isValidAllowanceRatePercentage(do2.getDescription(), do2.getFromDate(), do2.getFromDate(), columnC,
				do2.getRate() / 100)) {
			return true;
		}

		Date preJuly2021Date = do2.getFromDate();

		try {
			preJuly2021Date = DateManager.getDate("2021-06-30");
		} catch (Exception e) {
			Utils.handleException(e);
		}

		if (do2.getFromDate().after(preJuly2021Date)) {
			if (Utils.isValidAllowanceRatePercentage(do2.getDescription(), preJuly2021Date, preJuly2021Date, columnC,
					do2.getRate() / 100)) {
				return true;
			}
		}
		}

		// alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),
		// do2.getFromDate(), do2.getFromDate(), do2.getColumnC(), do2.getRate());

		return false;

		// return (alwList != null);
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		int hraAdmissible = 0;
		int hraLfAdmissible = 0;
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			final String[] alwUnderProcess = { do2.getDescription(), Do2Manager.HRALF };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			String columnC = "";
			if (do2.getColumnC() != null) {
				if (do2.getColumnC().startsWith("CCZ")) {
					columnC = "CCZ-1";
				}
				if (do2.getColumnC().startsWith("CCY")) {
					columnC = "CCY-1";
				}
				if (do2.getColumnC().startsWith("CCX")) {
					columnC = "CCX-1";
				}
			}
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate(),
					columnC);

			if (alwList.size() > 0) {

				if (do2.getColumnC().substring(0, 3).equals("CCZ")) {
					final String[] alwUnderProcess = { Do2Manager.HRA, Do2Manager.CILQ, Do2Manager.FAA,
							Do2Manager.HRALF };
					final List<String> alwToStop = Arrays.asList(alwUnderProcess);
					AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
				} else {
					final String[] alwUnderProcess = { Do2Manager.HRA, Do2Manager.CILQ, Do2Manager.HRAFAM,
							Do2Manager.FAA, Do2Manager.HRALF, Do2Manager.FAMLF };
					final List<String> alwToStop = Arrays.asList(alwUnderProcess);
					AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
				}

				int gradePay = baseAuditCage.getGradePay() == null ? 0 : baseAuditCage.getGradePay();
				int msPay = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();
				/*
				 * hraAdmissible = (int) Math.round(((newAuditCage.getBasicPay() + gradePay+
				 * msPay) * alwList.get(0).getRatePercentage()));
				 */
				// above line commented and following lines added due to sevencpc

				int oldGradePay = baseAuditCage.getOldGradePay() == null ? 0 : baseAuditCage.getOldGradePay();
				int basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
				int oldBasicPay = newAuditCage.getOldBasicPay() == null ? 0 : newAuditCage.getOldBasicPay();

				/*
				 * if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())){
				 * hraAdmissible = (int) Math.round(((oldBasicPay + oldGradePay+ msPay) *
				 * alwList.get(0).getRatePercentage())); } else { msPay =
				 * Utils.get6cpcMsPay(newAuditCage); hraAdmissible = (int) Math.round(((basicPay
				 * + gradePay+ msPay) * alwList.get(0).getRatePercentage())); }
				 */

				if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
					hraAdmissible = (int) Math
							.round(((basicPay + gradePay + msPay) * alwList.get(0).getRatePercentage()));
				} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					msPay = Utils.get6cpcMsPay(newAuditCage);
					hraAdmissible = (int) Math
							.round(((oldBasicPay + oldGradePay + msPay) * alwList.get(0).getRatePercentage()));
				} else {
					if ((do2.getLra() != null && do2.getLra() == 5)
							&& (do2.getColumnC() != null && !do2.getColumnC().substring(0, 3).equals("CCZ"))) {
						hraAdmissible = (int) Math.round(basicPay * (alwList.get(0).getRatePercentage() - 0.05));
					} else {
						hraAdmissible = (int) Math.round(((basicPay) * alwList.get(0).getRatePercentage()));
					}
				}

				AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.HRA, hraAdmissible);

				if (Integer.parseInt(rank.getRankCode()) > 70) {
					// if (rank.getRankCode().equals("80") || rank.getRankCode().equals("90")) {
					;
				} else {
					// hraLfAdmissible=80; // appendix J SRA column - lowest rate

					if (Integer.parseInt(rank.getRankCode()) > 49) {
						if (do2.getColumnC() != null) {
							if (do2.getColumnC().startsWith("CCZ")) {
								columnC = "CCZ-4";
							}
							if (do2.getColumnC().startsWith("CCY")) {
								columnC = "CCY-4";
							}
							if (do2.getColumnC().startsWith("CCX")) {
								columnC = "CCX-4";
							}
						}
					} else {
						if (do2.getColumnC() != null) {
							if (do2.getColumnC().startsWith("CCZ")) {
								columnC = "CCZ-3";
							}
							if (do2.getColumnC().startsWith("CCY")) {
								columnC = "CCY-3";
							}
							if (do2.getColumnC().startsWith("CCX")) {
								columnC = "CCX-3";
							}
						}
					}

					alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(),
							do2.getFromDate(), columnC);
					if (alwList.size() > 0) {
						hraLfAdmissible = (int) alwList.get(0).getRateAmount();
					}

					AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.HRALF, hraLfAdmissible);
				}
			}
		}

		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		Do2 refdo2 = Utils.getReferenceDo2(do2);
		return (refdo2 != null);
	}

	public boolean isValidAge(Do2 do2) {

		if (emp.getBirthDate() == null) {
			return false;
		}

		Date age25RelaxOrderDate = null;
		try {
			age25RelaxOrderDate = DateManager.getDate("2018-01-19");
		} catch (Exception e) {
			Utils.handleException(e);
		}

		if (do2.getFromDate().before(age25RelaxOrderDate)) {
			return ((DateManager.getNoOfMonths(emp.getBirthDate(), do2.getFromDate()) / 12) >= 25);
		} else {
			// Age reduced from 25 to 21 w.e.f. 19.01.2018
			// (Authority : GOI, MOD letter No. 3(1)/2015-S(Q&C)/pt dated 19-01-2018)
			return ((DateManager.getNoOfMonths(emp.getBirthDate(), do2.getFromDate()) / 12) >= 21);
		}
	}

	public boolean isMarried(Do2 do2) {
		return (emp.getMarried() != null && emp.getMarried() == true);
	}

	private void createSysDo2(Do2 do2) {
		// Generation Of ALWREV DoII To Revise HRALF rate with effect from 01.07.2017
		Date alwrevDate = DateManager.getAlwrevDateForHraLFRevision();

		GregorianCalendar cal1 = new GregorianCalendar(2010, Calendar.JANUARY, 1);
		Date startDate = new Date();
		startDate.setTime(cal1.getTimeInMillis());

		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (!do2.getFromDate().before(startDate) && do2.getFromDate().before(alwrevDate)) {
			if (!isAlwrevAlreadyGenerated(emp, alwrevDate)) {
				AuditCageHelper.insertAllowanceRevisionDo2(emp, alwrevDate, do2);
			}
		}

	}

	private boolean isAlwrevAlreadyGenerated(Employee emp, Date alwrevDate) {

		List<Do2> do2List = do2Repo.findByDescFromDateEqualValidAndPending(emp.getId(), "V", "P", "ALWREV", alwrevDate);

		/*
		 * Utils.getEntityManager().
		 * createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee " +
		 * " AND d.description='ALWREV' AND d.fromDate=:fromDate AND (d.status='V' OR d.status='P') "
		 * ) .setParameter("employee", emp) .setParameter("fromDate", alwrevDate)
		 * .getResultList();
		 */
		return do2List.size() > 0;
	}

	public boolean isClassOfCitySame(Do2 do2) {
		if (do2.getColumnA() != null && !do2.getColumnA().equals("GRANTED")) {
			return true;
		}
		if (do2.getColumnB() != null && do2.getColumnB().equals("INITIAL")) {
			return true;
		}
		if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) { 
			return false;
		}

		List<Do2> do2List = do2Repo.findByUnitCodeDescColAFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(),
				do2.getUnitCode(), do2.getFromDate(), do2.getDescription(), "GRANTED");

		/*
		 * Utils.getEntityManager().
		 * createQuery("SELECT d FROM Do2 d where d.employee = :employee AND " +
		 * " d.unitCode = :unitcd and (d.status = 'V' or d.status= 'P') and d.fromDate < :fromDate and "
		 * + " d.description = :desc AND (d.columnA is not null and d.columnA = :gtd)" +
		 * " order by d.fromDate desc)") .setParameter("employee", do2.getEmployee())
		 * .setParameter("unitcd", do2.getUnitCode()) .setParameter("desc",
		 * do2.getDescription()) .setParameter("fromDate", do2.getFromDate())
		 * .setParameter("gtd", "GRANTED").getResultList();
		 */

		if (do2List == null || do2List.size() == 0)
			return true;

		if (do2List.get(0).getColumnC().substring(0, 3).equals(do2.getColumnC().substring(0, 3))) {
			return true;
		} else {
			List<Do2> ceasedDo2List = do2Repo.findByUnitCodeDescColAFromDateBetweenLastGrantedAndCurrentGrantedDatesOrderByFromDateDesc(do2.getEmployee().getId(), do2List.get(0).getFromDate(), do2.getFromDate(),
					"HRA", "CEASED");
			if (ceasedDo2List!=null && ceasedDo2List.size()>0) {
				return true;
			}
			
			List<Do2> posinDo2List = do2Repo.findByUnitCodeDescFromDateBetweenLastGrantedAndCurrentGrantedDatesOrderByFromDateDesc(do2.getEmployee().getId(), do2List.get(0).getFromDate(), do2.getFromDate(),
					"POSIN");
			
			if (posinDo2List!=null && posinDo2List.size()>0) {
				return true;
			}
			
			return false;
		}
	}

	public boolean isPborOnDeputationCcz(Do2 do2) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<EmployeeServiceDetail> esdList = esdRepo.findByFromDateLessThanEqual2StatusOrderByFromDateDescToDateDesc(
				employee.getId(), do2.getFromDate(), Do2Manager.DEPU_DANTAK_BHUTAN + "",
				Do2Manager.DEPU_IMTRAT_BHUTAN + "");

		/*
		 * Utils.getEntityManager().createQuery(
		 * "select esd from EmployeeServiceDetail esd where esd.employee=:employee AND esd.fromDate<=:fromDate "
		 * +
		 * "and (esd.status=:status or esd.status=:status1) order by esd.fromDate desc, esd.toDate desc"
		 * ) .setParameter("employee", employee) .setParameter("status1",
		 * Do2Manager.DEPU_DANTAK_BHUTAN)
		 * .setParameter("status",Do2Manager.DEPU_IMTRAT_BHUTAN)
		 * .setParameter("fromDate", do2.getFromDate()) .getResultList();
		 */

		if (esdList.size() > 0) {
			if (!esdList.get(0).getDo2().getDescription().equals("TFRIN")) {
				if (esdList.get(0).getFromDate().equals(do2.getFromDate())
						|| (esdList.get(0).getFromDate().before(do2.getFromDate()))) {
					if (esdList.get(0).getToDate() == null) {
						return true;
					} else {
						if (do2.getFromDate().after(esdList.get(0).getToDate())) {
							return false;
						} else {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/*
	 * public boolean isCityMatchWithInitialClaim(Do2 do2, Employee emp) { if
	 * (do2.getColumnB()!=null && do2.getColumnB().trim().equals("INITIAL")) {
	 * return true; } List<Do2>do2List=Utils.getEntityManager().
	 * createQuery("SELECT d FROM Do2 d where d.employee = :employee AND " +
	 * " (d.unitCode = :unitcd or d.unit = :unit) and d.status = 'V' and d.fromDate < :fromDate and "
	 * +
	 * " d.description = 'HRA' AND d.columnA ='GRANTED' AND d.columnB = 'INITIAL'  "
	 * + " order by d.fromDate desc)").setParameter("unit", do2.getUnit())
	 * .setParameter("employee", do2.getEmployee()) .setParameter("unitcd",
	 * do2.getUnitCode()) .setParameter("fromDate", do2.getFromDate())
	 * .setParameter("colC", do2.getColumnC().trim()) .getResultList();
	 * 
	 * if (do2List!=null && do2List.size()>0) { for (Do2 d : do2List) { if
	 * (d.getColumnC().startsWith(do2.getColumnC().substring(0,3))) { return true; }
	 * } } return false; } // TO BE TESTED BEFORE RELEASE
	 */
}
