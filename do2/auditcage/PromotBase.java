package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.ClassPayRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;

public class PromotBase extends AuditCageBase {
	static Employee employee;
	List<PayScale> currentPayList = null;
	List<PayScale> previousPayList = null;
	public static final String[][] str7aRankCode = { { "TSNK", "25" }, { "TSLD", "25" }, { "NK", "30" }, { "LD", "30" },
			{ "TSHAV", "35" }, { "TSDFR", "35" }, { "HAV", "40" }, { "DFR", "40" }, { "GRHAV", "40" },
			{ "GRDFR", "40" }, { "SPHAV", "40" }, { "SPDFR", "40" }, { "NBSUB", "50" }, { "SPNBSUB", "50" },
			{ "SPNBRIS", "50" }, { "NBRIS", "50" }, { "SUB", "60" }, { "RIS", "60" }, { "SUBMAJ", "70" },
			{ "RISMAJ", "70" } };
	public static final String[][] strDesnRankCode = { { "MSTHAV", "40" }, { "MSTJCO", "50" } };

	public Date fixDni(Do2 do2) {
		Date xx = do2.getFromDate();
		Date dni = DateManager.getToday();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xx.getTime());
		int year = yy.get(Calendar.YEAR);
		int month = yy.get(Calendar.MONTH);

		int date = 01;
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			Calendar chkDate0 = new GregorianCalendar(year, month, date);
			dni = chkDate0.getTime();
			return DateManager.getNextIncrementDate(dni);
		} else {
			Calendar chkDate1 = new GregorianCalendar(year, Calendar.JANUARY, 01);
			Calendar chkDate2 = new GregorianCalendar(year, Calendar.JULY, 01);

			if ((do2.getFromDate().equals(chkDate1))) {
				dni = chkDate2.getTime();
				return dni;
			} else {
				dni = chkDate2.getTime();
				return DateManager.getNextIncrementDate(dni);
			}
		}
	}

	public boolean checkNQS(Employee employee, Do2 do2) {
		Date checkStartDate = DateManager.getToday();
		Date checkEndDate = employee.getIncrementDate();
		Calendar cal = new GregorianCalendar();
		cal.setTime(checkEndDate);
		cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
		checkStartDate.setTime(cal.getTimeInMillis());
		int nqsDays = 0;
		Date firstIncrDate = new Date(2006 - 07 - 01);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<EmployeeServiceDetail> nqsInESD = esdRepo.findNqs(employee.getId(), Do2Manager.ESD_NQS + "",
				checkStartDate, checkEndDate);

		if (nqsInESD.size() == 0) {

			return true;
		}

		if (nqsInESD.size() > 0) {
			Date sDate = null;
			Date eDate = null;
			for (EmployeeServiceDetail nqs : nqsInESD) {
				sDate = nqs.getFromDate();
				eDate = nqs.getToDate();
				if (employee.getIncrementDate().equals(firstIncrDate))
					sDate = new Date(2006 - 01 - 01);
				eDate = new Date(2006 - 06 - 30);
				nqsDays += (int) DateManager.getNoOfDays(sDate, eDate);
				if (nqsDays > 0) {

					return false;
				}
				if (sDate.before(checkStartDate))
					sDate = checkStartDate;
				else if (eDate.after(checkEndDate))
					eDate = checkEndDate;
				nqsDays += (int) DateManager.getNoOfDays(sDate, eDate);
			}
			if (nqsDays > 180) {

				return false;
			}
		}

		return true;
	}

	Date incrDate = new Date();

	public Date getIncrDate() {
		return incrDate;
	}

	public void setIncrDate(Date incrDate) {
		this.incrDate = incrDate;
	}

	public String createProfixDo2(Employee employee, Do2 do2) {

		String descriptionInserted = "PROFIX";

		if ((do2.getDescription().equals("ACP")) || (do2.getDescription().equals("MACPS"))) {
			descriptionInserted = "ACPFIX";
		}

		setIncrDate(DateManager.getCilqDate(do2.getFromDate()));

		List<Do2> chkFixDo2 = do2Repo.findByDescFromDateEqualStatusNotEqual(employee.getId(), Do2Manager.CANCEL,
				descriptionInserted, getIncrDate());

		if (chkFixDo2.size() > 0)
			return Do2Manager.FAILURE;

		Do2 profixDo2 = new Do2();

		if (do2.getBatchNo() != null)
			profixDo2.setBatchNo(do2.getBatchNo());

		profixDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		profixDo2.setDo2Group(do2.getDo2Group());
		profixDo2.setDo2Type(do2.getDo2Type());
		profixDo2.setDo2No(do2.getDo2No());
		profixDo2.setDo2ItemNo(do2.getDo2ItemNo());
		profixDo2.setDo2Date(do2.getDo2Date());
		profixDo2.setDescription(descriptionInserted);
		if (do2.getUnit() != null)
			profixDo2.setUnit(do2.getUnit());
		profixDo2.setUnitCode(do2.getUnitCode());
		profixDo2.setEmployee(employee);
		profixDo2.setArmyNo(do2.getArmyNo());
		profixDo2.setCheckDigit(do2.getCheckDigit());
		profixDo2.setFromDate(incrDate);
		if (do2.getDescription().equals("ACP") && do2.getFromDate().before(DateManager.getSixthCpcDate())
				&& do2.getToDate() != null) {
			profixDo2.setFromDate(do2.getToDate());
		}

		if (do2.getDescription().equals("MSTHAV"))
			profixDo2.setColumnA(do2.getDescription());
		else {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) { // in view of Estt No.22 changes are made
																				// on 28.08.2017
				if (do2.getColumnA().equals("NKGRADE")) {
					profixDo2.setColumnA("SLGRADE");
				} else if (do2.getColumnA().equals("HAVGRADE")) {
					profixDo2.setColumnA("ALGRADE");
				} else if (do2.getColumnA().equals("NSUBGRADE")) {
					profixDo2.setColumnA("DLGRADE");
				} else if (do2.getColumnA().equals("SUBGRADE")) {
					profixDo2.setColumnA("CLGRADE");
				} else if (do2.getColumnA().equals("SMAJGRADE")) {
					profixDo2.setColumnA("POLGRADE");
				}
			} else {
				profixDo2.setColumnA(do2.getColumnA());
			}
		}

		profixDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		profixDo2.setStatus(Do2Manager.PENDING);
		profixDo2.setAuditCage(true);
		profixDo2.setQe(Utils.getCurrentQuarter());
		// profixDo2.setAcceptedQe(Utils.getCurrentQuarter());
		profixDo2.setInputQe(do2.getInputQe());
		profixDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(profixDo2);

		return Do2Manager.SUCCESS;
	}

	public void get6cpcPay(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Rank qRank = null;
		Date promotPreviousDate = null;
		AuditCage promotPreviousAuditCage = null;
		AuditCage profixAuditCage = null;
		promotPreviousDate = getPreviousFromDateForPromot(nAuditCage.getDo2());

		if (promotPreviousDate != null) {
			promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCage(promotPreviousDate, nAuditCage.getDo2());
		} else {
			promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCage(nAuditCage.getDo2().getFromDate(),
					nAuditCage.getDo2());
		}

		if (nAuditCage.getDo2().getDescription().equals("MSTHAV"))
			qRank = rankRepo.findByRankCode("40");
		else if (nAuditCage.getDo2().getColumnA().equals("NKSCALE"))
			qRank = rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVSCALE"))
			qRank = rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NKGRADE"))
			qRank = rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVGRADE"))
			qRank = rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NSUBGRADE"))
			qRank = rankRepo.findByRankCode("49");
		else if (nAuditCage.getDo2().getColumnA().equals("SUBGRADE"))
			qRank = rankRepo.findByRankCode("59");
		else if (nAuditCage.getDo2().getColumnA().equals("SMAJGRADE"))
			qRank = rankRepo.findByRankCode("69");
		else if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE"))
			qRank = rankRepo.findByRankCode("79");
		else if (nAuditCage.getDo2().getColumnA().equals("TSNK"))
			qRank = rankRepo.findByRankCode("20");
		else {
			if (nAuditCage.getDo2().getColumnA().equals("TSNK")) {
				qRank = rankRepo.findByRankCode("20");
			} else if (nAuditCage.getDo2().getColumnA().equals("PANK")
					|| nAuditCage.getDo2().getColumnA().equals("SBNK")
					|| nAuditCage.getDo2().getColumnA().equals("PALD")
					|| nAuditCage.getDo2().getColumnA().equals("SBLD")) {
				qRank = rankRepo.findByRankCode("30");
			} else if (nAuditCage.getDo2().getColumnA().equals("PAHAV")
					|| nAuditCage.getDo2().getColumnA().equals("SBHAV")
					|| nAuditCage.getDo2().getColumnA().equals("SPHAV")
					|| nAuditCage.getDo2().getColumnA().equals("GRHAV")
					|| nAuditCage.getDo2().getColumnA().equals("PADFR")
					|| nAuditCage.getDo2().getColumnA().equals("SBDFR")) {
				qRank = rankRepo.findByRankCode("40");
			} else if (nAuditCage.getDo2().getColumnA().equals("NBSUB")
					|| nAuditCage.getDo2().getColumnA().equals("SPNBSUB")) {
				qRank = rankRepo.findByRankCode("50");
			} else if (nAuditCage.getDo2().getColumnA().equals("PASUB")
					|| nAuditCage.getDo2().getColumnA().equals("SBSUB")
					|| nAuditCage.getDo2().getColumnA().equals("SPRIS")
					|| nAuditCage.getDo2().getColumnA().equals("SPSUB")) {
				qRank = rankRepo.findByRankCode("60");
			} else if (nAuditCage.getDo2().getColumnA().equals("PASUBMAJ")
					|| nAuditCage.getDo2().getColumnA().equals("SBSUBMAJ")) {
				qRank = rankRepo.findByRankCode("70");
			}
		}

		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(nAuditCage.getDo2(), "CUR");

		if (groupCategory != null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}

		if (nAuditCage.getDo2().getColumnA().equals("NBSUB") || nAuditCage.getDo2().getColumnA().equals("SPNBSUB")) {
			soldierGroup = nAuditCage.getDo2().getColumnB().charAt(3) + "";
		}

		if (soldierGroup.equals("Z") && !nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}

		currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(qRank.getId(), soldierGroup,
				nAuditCage.getFromDate());

		Date xDate = nAuditCage.getDo2().getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();

		// basic pay
		if ((xDate.after(jan01) && xDate.before(jul01)) && (employee.getFsDueDate() == null
				|| (employee.getFsDueDate() != null && !employee.getFsDueDate().before(jul01)))) {
			if ((nAuditCage.getDo2().getDescription().equals("PROMOT")
					|| (nAuditCage.getDo2().getDescription().equals("MSTJCO")))
					&& nAuditCage.getDo2().getColumnA().equals("NBSUB")) {

				if ((nAuditCage.getBasicPay() < 8600 && nAuditCage.getXGroupPay() == 0)
						|| (nAuditCage.getBasicPay() < 8580 && nAuditCage.getXGroupPay() == 1400)) {
					if (currentPayList != null && currentPayList.size() > 0)
						nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
				}
				// ------------------- 06112025 CHANGE START -------------------
				// MSTJCO + NBSUB: Enforce minimum basic pay of 9300 even if 8600/8580 condition
				// not met
				if (nAuditCage.getDo2().getDescription().equals("MSTJCO")
						&& nAuditCage.getDo2().getColumnA().equals("NBSUB")) {

					System.out.println("*** 06112025: MSTJCO-NBSUB MIN BASIC ENFORCEMENT ***");
					System.out
							.println("Current Basic: " + nAuditCage.getBasicPay() + " | Target Min: 9300 | StartScale: "
									+ (currentPayList != null && currentPayList.size() > 0
											? currentPayList.get(0).getStartScale()
											: "N/A"));

					if (currentPayList != null && currentPayList.size() > 0) {
						int minJcoBasic = currentPayList.get(0).getStartScale(); // 9300
						if (nAuditCage.getBasicPay() < minJcoBasic) {
							System.out.println("*** 06112025: FORCING BASIC PAY TO 9300 ***");
							nAuditCage.setBasicPay(minJcoBasic);
						} else {
							System.out.println("*** 06112025: BASIC ALREADY >= 9300 - NO CHANGE ***");
						}
					}
				}
				// ------------------- 06112025 CHANGE END -------------------
			}
			// Added for OPTCPC
			if (!nAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()) && nAuditCage.getPayLevel() != null
					&& nAuditCage.getPayLevel().equals("0")) {
				int newBasic = bAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
						bAuditCage.getBasicPay() + bAuditCage.getGradePay()
								+ (bAuditCage.getXGroupPay() == null ? 0 : bAuditCage.getXGroupPay()),
						bAuditCage.getFromDate()));
				if (currentPayList != null && currentPayList.size() > 0) {
					nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
					if (newBasic > currentPayList.get(0).getStartScale())
						nAuditCage.setBasicPay(newBasic);
				}

			}

		} else {
			// Added to take care of MACPS condition - if the PBOR is ACPNK and getting
			// direct SPHAV/GRHAV from sepoy, then there is no 3% pay fixation
			// only increase in grade pay (ie 2400 to 2800)
			if ((nAuditCage.getDo2().getDescription().equals("MSTHAV")
					|| (nAuditCage.getDo2().getDescription().equals("PROMOT")
							&& nAuditCage.getDo2().getColumnA().equals("SPHAV"))
					|| (nAuditCage.getDo2().getDescription().equals("PROMOT")
							&& nAuditCage.getDo2().getColumnA().equals("GRHAV")))
					&& employee.getNaikDate() == null
					&& (employee.getAcpnaikDate() != null
							&& !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate()))
							&& employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))) {
				;
			} /*
				 * else if ((nAuditCage.getDo2().getDescription().equals("MSTJCO")) &&
				 * employee.getNaikDate() == null && (employee.getAcpnaikDate()!= null &&
				 * !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate()))
				 * && employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))){ if
				 * (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
				 * nAuditCage.setBasicPay(currentPayList.get(0).getStartScale()); } ;
				 */
			else if (nAuditCage.getDo2().getDescription().equals("MSTJCO")) {
				/*
				 * if (employee.getNaikDate() == null && employee.getAcpnaikDate()!=null) { //
				 * condition commented on 06.02.2025 AMC email dt if
				 * (!employee.getAcpnaikDate().before(DateManager.getMacpsStartDate())){ if
				 * (employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate())) { if
				 * (DateManager.getNoOfMonths(employee.getAcpnaikDate(),nAuditCage.getDo2().
				 * getFromDate()) < 96.0) { if(currentPayList!=null && currentPayList.size() > 0
				 * ) { if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
				 * nAuditCage.setBasicPay(currentPayList.get(0).getStartScale()); } }
				 * 
				 * }
				 * 
				 * } } } else {
				 */
				/*
				 * nAuditCage.setBasicPay(promotPreviousAuditCage.getBasicPay() +
				 * (Utils.getIncrmentRoundedToTen(promotPreviousAuditCage.getBasicPay() +
				 * promotPreviousAuditCage.getGradePay() +
				 * (promotPreviousAuditCage.getXGroupPay() == null ? 0:
				 * promotPreviousAuditCage.getXGroupPay()),
				 * promotPreviousAuditCage.getFromDate())));
				 */
				nAuditCage
						.setBasicPay(
								bAuditCage.getBasicPay()
										+ (Utils.getIncrmentRoundedToTen(
												bAuditCage.getBasicPay() + bAuditCage.getGradePay()
														+ (bAuditCage.getXGroupPay() == null ? 0
																: bAuditCage.getXGroupPay()),
												bAuditCage.getFromDate())));
				if (currentPayList != null && currentPayList.size() > 0) {
					if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
						nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
					}
				}

				// }
			} else {
				if (nAuditCage.getDo2().getDescription().equals("MSTHAV") && nAuditCage.getGradePay() == 2800) {
					;
				} else if (Utils.isPromotionAfterReversionAfter010106(nAuditCage.getDo2())) {
					;
				} else {
					promotPreviousAuditCage = AuditCageHelper
							.getPreviousDateAuditCage(nAuditCage.getDo2().getFromDate(), nAuditCage.getDo2());

					// following commented and bAuditCage replaced with promotPreviousAuditCage as
					// REMUST & PROMOT do2s fed on same day and while
					// adjusting promot, x group pay also taken in to account as the same has not to
					// be taken due to non completion of sixmonths sevice from
					// the drawal of x pay.

					if (promotPreviousAuditCage != null && promotPreviousAuditCage.getXGroupPay() == 0
							&& bAuditCage.getXGroupPay() != 0) {

						profixAuditCage = AuditCageHelper.getProfixAuditCage(nAuditCage.getDo2().getFromDate(),
								nAuditCage.getDo2());
						if (profixAuditCage != null
								&& !profixAuditCage.getDo2().getColumnA().equals(nAuditCage.getDo2().getColumnA())
								&& profixAuditCage.getFromDate().equals(jul01)) {  //jul01 condition added on 19.01.2026 - Nashik Samanvyay ticket
							System.out.println(" IAM HERE IS LLLLL " + nAuditCage.getFromDate()
									+ " promot previous Auditcage id " + promotPreviousAuditCage.getId());
							nAuditCage.setBasicPay(profixAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
									profixAuditCage.getBasicPay() + promotPreviousAuditCage.getGradePay()
											+ (promotPreviousAuditCage.getXGroupPay() == null ? 0
													: promotPreviousAuditCage.getXGroupPay()),
									profixAuditCage.getFromDate())));
							System.out.println(" llll new basic pay " + nAuditCage.getBasicPay());
						} else {

							nAuditCage
									.setBasicPay(promotPreviousAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
											promotPreviousAuditCage.getBasicPay()
													+ promotPreviousAuditCage.getGradePay()
													+ (promotPreviousAuditCage.getXGroupPay() == null ? 0
															: promotPreviousAuditCage.getXGroupPay()),
											promotPreviousAuditCage.getFromDate())));
						}
					} else if (promotPreviousAuditCage != null && promotPreviousAuditCage.getXGroupPay() != 0
							&& bAuditCage.getXGroupPay() != 0) {
						Date remustDate = Utils.getFromDateForRemust(nAuditCage.getDo2());
						if (remustDate != null && !remustDate.before(DateManager.getSixthCpcDate())) {
							if (DateManager.getNoOfMonths(remustDate, nAuditCage.getDo2().getFromDate()) > 6) {
								AuditCage remustAuditCage = AuditCageHelper.getRemustAuditCage(remustDate,
										nAuditCage.getDo2());
								/*
								 * if (remustAuditCage!=null && remustAuditCage.getXGroupPay()!=0) {
								 * nAuditCage.setBasicPay(promotPreviousAuditCage.getBasicPay() +
								 * (Utils.getIncrmentRoundedToTen(promotPreviousAuditCage.getBasicPay() +
								 * promotPreviousAuditCage.getGradePay() +
								 * (promotPreviousAuditCage.getXGroupPay() == null ? 0:
								 * promotPreviousAuditCage.getXGroupPay()),
								 * promotPreviousAuditCage.getFromDate()))); } else {
								 * nAuditCage.setBasicPay(promotPreviousAuditCage.getBasicPay() +
								 * (Utils.getIncrmentRoundedToTen(promotPreviousAuditCage.getBasicPay() +
								 * promotPreviousAuditCage.getGradePay(),
								 * promotPreviousAuditCage.getFromDate()))); }
								 */
								if (remustAuditCage != null && remustAuditCage.getXGroupPay() != 0) {
									nAuditCage
											.setBasicPay(bAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
													bAuditCage.getBasicPay() + bAuditCage.getGradePay()
															+ (bAuditCage.getXGroupPay() == null ? 0
																	: bAuditCage.getXGroupPay()),
													bAuditCage.getFromDate())));
								} else if(remustDate !=null && remustAuditCage == null &&
                                        remustDate.before(bAuditCage.getFromDate()) && bAuditCage.getXGroupPay() != 0)
                                {
                                    nAuditCage
                                            .setBasicPay(bAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
                                                    bAuditCage.getBasicPay() + bAuditCage.getGradePay()
                                                            + (bAuditCage.getXGroupPay() == null ? 0
                                                            : bAuditCage.getXGroupPay()),
                                                    bAuditCage.getFromDate())));
                                }
                                else {
									nAuditCage.setBasicPay(bAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
											bAuditCage.getBasicPay() + bAuditCage.getGradePay(),
											bAuditCage.getFromDate())));
								}
							} else {
								nAuditCage.setBasicPay(
										promotPreviousAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
												promotPreviousAuditCage.getBasicPay()
														+ promotPreviousAuditCage.getGradePay(),
												promotPreviousAuditCage.getFromDate())));
							}
						} else {
							nAuditCage
									.setBasicPay(
											bAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
													bAuditCage.getBasicPay() + bAuditCage.getGradePay()
															+ (bAuditCage.getXGroupPay() == null ? 0
																	: bAuditCage.getXGroupPay()),
													bAuditCage.getFromDate())));
						}

						/*
						 * { nAuditCage.setBasicPay(promotPreviousAuditCage.getBasicPay() +
						 * (Utils.getIncrmentRoundedToTen(promotPreviousAuditCage.getBasicPay() +
						 * promotPreviousAuditCage.getGradePay() +
						 * (promotPreviousAuditCage.getXGroupPay() == null ? 0:
						 * promotPreviousAuditCage.getXGroupPay()),
						 * promotPreviousAuditCage.getFromDate()))); }
						 */
					} else {
						nAuditCage
								.setBasicPay(
										bAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
												bAuditCage.getBasicPay() + bAuditCage.getGradePay()
														+ (bAuditCage.getXGroupPay() == null ? 0
																: bAuditCage.getXGroupPay()),
												bAuditCage.getFromDate())));
					}

					if (nAuditCage.getDo2().getColumnA().equals("SPHAV")) { // this para added to adjust minimum basic
																			// pay 8560 who promoted after 01-01-2006 on
																			// 01-11-2023
						if (employee.getEnrolmentDate() != null && employee.getHavDate() != null
								&& employee.getHavDate().after(employee.getEnrolmentDate())
								&& employee.getHavDate().equals(employee.getSportHavildarDate())
								&& employee.getSportHavildarFlag() == true && employee.getNaikDate() == null
								&& employee.getAcpnaikDate() == null && employee.getAcphavDate() == null
								&& employee.getHavDate().equals(nAuditCage.getDo2().getFromDate())) {
							if (nAuditCage.getBasicPay() <= 8560) {
								nAuditCage.setBasicPay(8560);
							}
						}
					}

					if (nAuditCage.getDo2().getColumnA().equals("SPHAV") && (nAuditCage.getDo2().getColumnC() != null
							&& nAuditCage.getDo2().getColumnC().equals("DIRECT"))) {
						if (employee.getEnrolmentDate().equals(nAuditCage.getDo2().getFromDate())) {
							/*
							 * System.out.println ( " CURRENTPAY LIST 11 "
							 * +currentPayList.get(0).getStartScale() +"    " +nAuditCage.getBasicPay() +" "
							 * +currentPayList.get(0).getNewRecruitPay());
							 */ if (currentPayList != null && currentPayList.size() > 0)
								nAuditCage.setBasicPay(currentPayList.get(0).getNewRecruitPay());

						}
					} else {
						if (currentPayList != null && currentPayList.size() > 0) {
							if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
								nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
							}
						}

					}
				}
			}
		}

		// grade pay
		if (currentPayList != null && currentPayList.size() > 0)
			nAuditCage.setGradePay(currentPayList.get(0).getGradePay());
		// ms pay
		if (!(nAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcAllowanceDate()))) {
			if (currentPayList != null && currentPayList.size() > 0)
				nAuditCage.setMsPay(currentPayList.get(0).getMsp());
		}
		// X pay
		if (soldierGroup.equals("X")) {
			if (currentPayList != null && currentPayList.size() > 0)
				nAuditCage.setXGroupPay(currentPayList.get(0).getXGroupPay());
		}
		// class Pay

		// If PROMOT to JCO Rank removing Class Pay
		if (Do2Manager.getJcoRankDescription().contains(nAuditCage.getDo2().getColumnA())) {
			nAuditCage.setClassPay(0);
		}

		// Transport Allowance
		if (nAuditCage.getTransport() != null && nAuditCage.getTransport() != 0) {
			nAuditCage.setTransport(AuditCageHelper.getTransportRate(nAuditCage, bAuditCage));
		}
	}

	public void get5cpcPay(AuditCage nAuditCage, AuditCage bAuditCage, Employee prevEmpStatus) {
		int notionalPay = 0;
		String columnA = nAuditCage.getDo2().getColumnA();

		if ((nAuditCage.getDo2().getDescription().equals(Do2Manager.ACP))
				|| (nAuditCage.getDo2().getDescription().equals("ACPFIX"))) {
			if (columnA.equals("NKSCALE")) {
				columnA = "NK";
			}
			if (columnA.equals("HAVSCALE")) {
				columnA = "HAV";
			}
		}

		/*
		 * if (nAuditCage.getDo2().getDescription().equals("PROMOT")){ if
		 * (!nAuditCage.getDo2().getColumnA().equals("NBSUB") &&
		 * (!nAuditCage.getDo2().getColumnA().equals("NBRIS"))) { columnA =
		 * nAuditCage.getDo2().getColumnA().substring(2); } }
		 */

		employee = prevEmpStatus;

		Rank oldRank = Utils.getPborRank(employee.getArmyNo(), nAuditCage.getFromDate(), "PAYFIX");

		if (nAuditCage.getDo2().getDescription().equals("ACPFIX")) {
			List<Do2> acpDo2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(
					nAuditCage.getEmployee().getId(), Do2Manager.VALID, "ACP", nAuditCage.getFromDate());

			if (acpDo2List.size() > 0) {
				oldRank = Utils.getPborRank(employee.getArmyNo(), acpDo2List.get(0).getFromDate(), "PAYFIX");
			}
		}

		if (oldRank == null && nAuditCage.getDo2().getDescription().equals("MSTJCO")
				&& nAuditCage.getDo2().getColumnA().equals("NBSUB")) {
			oldRank = rankRepo.findByRankCode("50");
		}
		Date fifthCpcDate = null;
		try {
			fifthCpcDate = DateManager.getDate("2005-12-31");
		} catch (Exception e) {
			Utils.handleException(e);
		}

		GroupCategory gp = Utils.getGroupCategory(nAuditCage.getDo2(), "CUR");

		String catg = null;
		catg = gp.getSoldierCategory();
		Date fromDt = null;
		try {
			fromDt = DateManager.getDate("1997-10-10");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<GroupCategory> grpCatList = gcRepo.findByFromDateAndRedesgAbbrNotNull(fromDt);

		if (grpCatList.size() > 0) {
			for (GroupCategory gc1 : grpCatList) {
				if (gc1.getRedesignatedAbbr().equals(catg)) {
					catg = gc1.getSoldierCategory();
					gp = gcRepo.findBySoldierCategory(catg).get(0);

				}
			}
		}
		previousPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
				oldRank.getId(), gp.getSoldierGroup(), fifthCpcDate);

		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());

		Rank promotRank = null;
		if (nAuditCage.getDo2().getColumnA().equals("NKSCALE")) {
			promotRank = rankRepo.findByRankCode("30");
		} else if (nAuditCage.getDo2().getColumnA().equals("HAVSCALE")) {
			promotRank = rankRepo.findByRankCode("40");
		} else if (nAuditCage.getDo2().getColumnA().equals("PANK") || nAuditCage.getDo2().getColumnA().equals("SBNK")) {
			promotRank = rankRepo.findByRankCode("30");
		} else if (nAuditCage.getDo2().getColumnA().equals("PAHAV") || nAuditCage.getDo2().getColumnA().equals("SBHAV")
				|| nAuditCage.getDo2().getColumnA().equals("SPHAV")
				|| nAuditCage.getDo2().getColumnA().equals("GRHAV")) {
			promotRank = rankRepo.findByRankCode("40");
		} else if (nAuditCage.getDo2().getColumnA().equals("NBSUB")
				|| nAuditCage.getDo2().getColumnA().equals("SPNBSUB")) {
			promotRank = rankRepo.findByRankCode("50");
		} else if (nAuditCage.getDo2().getColumnA().equals("PASUB")
				|| nAuditCage.getDo2().getColumnA().equals("SBSUB")) {
			promotRank = rankRepo.findByRankCode("60");
		} else if (nAuditCage.getDo2().getColumnA().equals("PASUBMAJ")
				|| nAuditCage.getDo2().getColumnA().substring(2).equals("SBSUBMAJ")) {
			promotRank = rankRepo.findByRankCode("70");
		}

		currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
				promotRank.getId(), gp.getSoldierGroup(), fifthCpcDate);

		/*
		 * Date promotPreviousDate=null; AuditCage promotPreviousAuditCage5cpc = null;
		 * promotPreviousDate = getPreviousFromDateForPromot5cpc(nAuditCage.getDo2());
		 * 
		 * if (promotPreviousDate!=null) { promotPreviousAuditCage5cpc =
		 * AuditCageHelper.getPreviousDateAuditCage5cpc(promotPreviousDate,
		 * nAuditCage.getDo2()); } else { promotPreviousAuditCage5cpc =
		 * AuditCageHelper.getPreviousDateAuditCage5cpc(nAuditCage.getDo2().getFromDate(
		 * ), nAuditCage.getDo2()); }
		 */

		int oldBasicPay = bAuditCage.getBasicPay();

		// Above lines commented on 13.02.2019 w r t cmp fs case

		AuditCage promotPreviousAuditCage5cpc = AuditCageHelper
				.getPreviousDateAuditCage5cpc(nAuditCage.getDo2().getFromDate(), nAuditCage.getDo2());
		// int oldBasicPay = promotPreviousAuditCage5cpc.getBasicPay(); -- to be
		// reviewedto keep this alive or above oldbasicpay t
		if (previousPayList.size() > 0) {
			notionalPay = oldBasicPay + previousPayList.get(0).getIncr0();
		}

		int newBasicPay = 0;
//		System.out.println("XXXXXXXXXXXXXXXXX  promotpreviousauditcage  "+promotPreviousAuditCage5cpc.getId()+"  "+oldBasicPay +"  "+notionalPay);

		// basic Pay
		if (nAuditCage.getDo2().getDescription().equals("ACP")) {
			if (nAuditCage.getDo2().getToDate() == null) {
				for (int i = currentPayList.get(0).getStartScale(); i <= currentPayList.get(0)
						.getScaleEnd0(); i += currentPayList.get(0).getIncr0()) {
					if (i >= (oldBasicPay + 100)) {
						newBasicPay = i;
						break;
					}
				}
				if (newBasicPay < currentPayList.get(0).getStartScale())
					nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
				else
					nAuditCage.setBasicPay(newBasicPay);
			}
		} else if (nAuditCage.getDo2().getDescription().equals("ACPFIX")) {
			for (int i = currentPayList.get(0).getStartScale(); i <= currentPayList.get(0)
					.getScaleEnd0(); i += currentPayList.get(0).getIncr0()) {
				if (i >= (oldBasicPay + 100)) {
					newBasicPay = i;
					break;
				}
			}
			if (newBasicPay < currentPayList.get(0).getStartScale()) {
				nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
			} else {
				nAuditCage.setBasicPay(newBasicPay);
			}
		} else {
//System.out.println("xxxxxxxxxxxxxxxx  notional pay " +notionalPay + "  "+previousPayList.get(0).getId()+"  "+currentPayList.get(0).getId());
			for (int i = currentPayList.get(0).getStartScale(); i <= currentPayList.get(0)
					.getScaleEnd0(); i += currentPayList.get(0).getIncr0()) {
				if (i > notionalPay) {
					newBasicPay = i;
					if (nAuditCage.getDo2().getColumnA().equals("TSNK")) {
						newBasicPay -= 1;
					}
					break;
				}
			}
			if (newBasicPay < currentPayList.get(0).getStartScale())
				nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
			else
				nAuditCage.setBasicPay(newBasicPay);
		}

		// dp
		if (!(nAuditCage.getDo2().getFromDate().before(DateManager.getDPDate()))) {
			if (nAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcDate()))
				nAuditCage.setDp((int) Math.round(nAuditCage.getBasicPay() * 0.50));
		}

		if (Do2Manager.getJcoRankDescription().contains(nAuditCage.getDo2().getColumnA())) {
			nAuditCage.setClassPay(0);
		}
		if (bAuditCage.getTransport() != null && bAuditCage.getTransport() != 0) {
			nAuditCage.setTransport(AuditCageHelper.getTransportRate(nAuditCage, bAuditCage));
		}

	}

	// new method added for ta -to fix the pay on the same date of promotion
	public void get6cpcPayForTa(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Rank qRank = null;
		/*
		 * String rankName= nAuditCage.getDo2().getColumnA();
		 * if(!nAuditCage.getDo2().getColumnA().equals("NBSUB") &&
		 * !nAuditCage.getDo2().getColumnA().equals("NBRIS")){ rankName =
		 * nAuditCage.getDo2().getColumnA().substring(2); } qRank = (Rank)
		 * Utils.getForeignKey(new Rank(), "rankName", rankName);
		 */
		if (nAuditCage.getDo2().getColumnA().equals("PANK") || nAuditCage.getDo2().getColumnA().equals("SBNK")
				|| nAuditCage.getDo2().getColumnA().equals("TSNK")) {
			qRank = rankRepo.findByRankCode("30");
		} else if (nAuditCage.getDo2().getColumnA().equals("PAHAV") || nAuditCage.getDo2().getColumnA().equals("SBHAV")
				|| nAuditCage.getDo2().getColumnA().equals("SPHAV")) {
			qRank = rankRepo.findByRankCode("40");
		} else if (nAuditCage.getDo2().getColumnA().equals("NBSUB")
				|| nAuditCage.getDo2().getColumnA().equals("SPNBSUB")
				|| nAuditCage.getDo2().getColumnA().equals("NBRIS")
				|| nAuditCage.getDo2().getColumnA().equals("SPNBRIS")) {
			qRank = rankRepo.findByRankCode("50");
		} else if (nAuditCage.getDo2().getColumnA().equals("PASUB")
				|| nAuditCage.getDo2().getColumnA().equals("SBSUB")) {
			qRank = rankRepo.findByRankCode("60");
		} else if (nAuditCage.getDo2().getColumnA().equals("PASUBMAJ")
				|| nAuditCage.getDo2().getColumnA().equals("SBSUBMAJ")) {
			qRank = rankRepo.findByRankCode("70");
		}
		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(nAuditCage.getDo2(), "CUR");

		if (groupCategory != null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}

		if (soldierGroup.equals("Z") && !nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}

		currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(qRank.getId(),
				soldierGroup, nAuditCage.getFromDate());

		nAuditCage.setBasicPay(bAuditCage.getBasicPay() + (Utils.getIncrmentRoundedToTen(
				bAuditCage.getBasicPay() + bAuditCage.getGradePay()
						+ (bAuditCage.getXGroupPay() == null ? 0 : bAuditCage.getXGroupPay()),
				bAuditCage.getFromDate())));

		if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
			nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
		}
		// grade pay
		nAuditCage.setGradePay(currentPayList.get(0).getGradePay());
		// ms pay
		if (!(nAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcAllowanceDate()))) {
			nAuditCage.setMsPay(currentPayList.get(0).getMsp());
		}
		// X pay
		if (soldierGroup.equals("X")) {
			nAuditCage.setXGroupPay(currentPayList.get(0).getXGroupPay());
		}
		// class Pay

		// If PROMOT to JCO Rank removing Class Pay
		if (Do2Manager.getJcoRankDescription().contains(nAuditCage.getDo2().getColumnA())) {
			nAuditCage.setClassPay(0);
		}

		// Transport Allowance
		if (nAuditCage.getTransport() != null && nAuditCage.getTransport() != 0) {
			nAuditCage.setTransport(AuditCageHelper.getTransportRate(nAuditCage, bAuditCage));
		}
	}

	public void getOldBasicPay(AuditCage nAuditCage, AuditCage bAuditCage, Employee empPreviousStatus) {
		employee = empPreviousStatus;
		Rank rank = Utils.getPborRank(employee.getArmyNo(), nAuditCage.getDo2().getFromDate(), "PAYFIX");
		if (rank == null && nAuditCage.getDo2().getDescription().equals("MSTJCO")
				&& nAuditCage.getDo2().getColumnA().equals("NBSUB")) {
			rank = rankRepo.findByRankCode("50");
		}

		try {
			previousPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
					rank.getId(), empPreviousStatus.getGroupCategory().getSoldierGroup(),
					DateManager.getDate("1997-10-10"));

			employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
			GroupCategory gc = Utils.getGroupCategory(nAuditCage.getDo2(), "CUR");
			String soldierGroup = "";
			Rank currentRank = null;
			String rankCode = " ";
			if (nAuditCage.getDo2().getDescription().equals("MSTJCO")
					|| nAuditCage.getDo2().getColumnA().equals("NBSUB")
					|| nAuditCage.getDo2().getColumnA().equals("SPNBSUB")
					|| nAuditCage.getDo2().getColumnA().equals("NSUBGRADE")) {
				rankCode = "50";
			} else if (nAuditCage.getDo2().getColumnA().equals("NKSCALE")
					|| nAuditCage.getDo2().getColumnA().equals("NKGRADE")) {
				rankCode = "30";
			} else if (nAuditCage.getDo2().getColumnA().equals("HAVSCALE")
					|| nAuditCage.getDo2().getColumnA().equals("HAVGRADE")) {
				rankCode = "40";
			} else if (nAuditCage.getDo2().getColumnA().equals("SUBGRADE")) {
				rankCode = "60";
			} else if (nAuditCage.getDo2().getColumnA().equals("SMAJGRADE")) {
				rankCode = "70";
			} else {
				if (nAuditCage.getDo2().getColumnA().substring(2).equals("NK")) {
					rankCode = "30";
				} else if (nAuditCage.getDo2().getColumnA().substring(2).equals("HAV")) {
					rankCode = "40";
				} else if (nAuditCage.getDo2().getColumnA().substring(2).equals("SUB")) {
					rankCode = "60";
				} else if (nAuditCage.getDo2().getColumnA().substring(2).equals("SUBMAJ")) {
					rankCode = "70";
				}
			}

			currentRank = rankRepo.findByRankCode(rankCode);

			if (gc != null) {
				soldierGroup = gc.getSoldierGroup();
			}

			if (nAuditCage.getDo2().getDescription().equals("MSTJCO")
					&& nAuditCage.getDo2().getColumnB().length() == 4) {
				soldierGroup = nAuditCage.getDo2().getColumnB().charAt(3) + "";
			}

			int oldBasicPay = bAuditCage.getOldBasicPay();
			int notionalPay = 0;
			int newBasicPay = 0;

			currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
					currentRank.getId(), soldierGroup, DateManager.getDate("1997-10-10"));

			if (currentPayList == null || (currentPayList != null && currentPayList.size() == 0)) {
				;
			} else {
				if (previousPayList.size() > 0)
					notionalPay = oldBasicPay + previousPayList.get(0).getIncr0();

				// basic Pay
				for (int i = currentPayList.get(0).getStartScale(); i <= currentPayList.get(0)
						.getScaleEnd0(); i += currentPayList.get(0).getIncr0()) {
					if (i > notionalPay) {
						newBasicPay = i;
						break;
					}
				}
				if (newBasicPay < currentPayList.get(0).getStartScale())
					nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());
				else
					nAuditCage.setOldBasicPay(newBasicPay);
			}
			/*
			 * System.out.
			 * println("ttttttttttttttttttttttttt from promot base  current pay scale  " +
			 * currentPayList.get(0).getStartScale()+"  "+currentPayList.get(0).getGradePay(
			 * ) + "  "+nAuditCage.getBasicPay()+"  "+nAuditCage.getOldBasicPay()+"  "
			 * +nAuditCage.getGradePay()+"  "+nAuditCage.getOldGradePay());
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Date getPreviousFromDateForPromot(Do2 do2) {

		Date promotPreviousDate = null;
		List<Do2> do2List = do2Repo.findByDescFromDateLessThanEqual(do2.getEmployee().getId(), "INCR",
				do2.getFromDate());

		if (do2List.size() > 0) {
			for (Do2 promotDo2 : do2List) {
				promotPreviousDate = promotDo2.getFromDate();
			}
		}

		return promotPreviousDate;
	}

	// the below method added to tackle PANK, SBNK given on same date 5cpc
	public Date getPreviousFromDateForPromot5cpc(Do2 do2) {

		Date promotPreviousDate = null;
		List<Do2> do2List = do2Repo.findByEmployeeIdAndFromDateLessThan(do2.getEmployee().getId(), do2.getFromDate());
		// requires review -- description missing?

		if (do2List.size() > 0) {
			for (Do2 promotDo2 : do2List) {
				promotPreviousDate = promotDo2.getFromDate();
			}
		}

		return promotPreviousDate;
	}

	public void get7cpcPay(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Rank qRank = null;

		if (nAuditCage.getDo2().getDescription().equals("MSTHAV"))
			qRank = rankRepo.findByRankCode("40");
		else if (nAuditCage.getDo2().getColumnA().equals("NKSCALE"))
			qRank = rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVSCALE"))
			qRank = rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NKGRADE"))
			qRank = rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVGRADE"))
			qRank = rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NSUBGRADE"))
			qRank = rankRepo.findByRankCode("49");
		else if (nAuditCage.getDo2().getColumnA().equals("SUBGRADE"))
			qRank = rankRepo.findByRankCode("59");
		else if (nAuditCage.getDo2().getColumnA().equals("SMAJGRADE"))
			qRank = rankRepo.findByRankCode("69");
		else if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE"))
			qRank = rankRepo.findByRankCode("79");
		else if (nAuditCage.getDo2().getColumnA().equals("TSNK"))
			qRank = rankRepo.findByRankCode("20");
		else {
			/*
			 * String rankName= nAuditCage.getDo2().getColumnA();
			 * if(!nAuditCage.getDo2().getColumnA().equals("NBSUB") &&
			 * !nAuditCage.getDo2().getColumnA().equals("NBRIS")){ rankName =
			 * nAuditCage.getDo2().getColumnA().substring(2); } qRank = (Rank)
			 * Utils.getForeignKey(new Rank(), "rankName", rankName);
			 */
			String rankCode = " ";

			if (nAuditCage.getDo2().getColumnA().substring(2).equals("NK")) {
				rankCode = "30";
			} else if (nAuditCage.getDo2().getColumnA().substring(2).equals("HAV")) {
				rankCode = "40";
			} else if (nAuditCage.getDo2().getColumnA().substring(2).equals("SUB")) {
				rankCode = "60";
			} else if (nAuditCage.getDo2().getColumnA().substring(2).equals("SUBMAJ")) {
				rankCode = "70";
			} else if (nAuditCage.getDo2().getColumnA().equals("TSNK")) {
				rankCode = "20";
			}

			if (nAuditCage.getDo2().getDescription().equals("MSTJCO")
					|| nAuditCage.getDo2().getColumnA().equals("NBSUB")
					|| nAuditCage.getDo2().getColumnA().equals("SPNBSUB")) {
				rankCode = "50";
			}
			if (nAuditCage.getDo2().getColumnA().equals("SPRIS") || nAuditCage.getDo2().getColumnA().equals("SPSUB")) {
				rankCode = "60";
			}
			qRank = rankRepo.findByRankCode(rankCode);
		}

		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(nAuditCage.getDo2(), "CUR");

		if (groupCategory != null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}

		if (nAuditCage.getDo2().getColumnA().equals("NBSUB") || nAuditCage.getDo2().getColumnA().equals("SPNBSUB")) {
			soldierGroup = nAuditCage.getDo2().getColumnB().charAt(3) + "";
		}

		if (soldierGroup.equals("Z") && !nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}

		Integer newBasic = nAuditCage.getBasicPay();
		Integer basicpay = nAuditCage.getBasicPay();

//		Rank currentRank = Utils.getPborRank(nAuditCage.getDo2().getArmyNo(), nAuditCage.getDo2().getFromDate(),"PROMOT");   	
		Rank previousRankPromot = Utils.getPborRank(nAuditCage.getDo2().getArmyNo(), nAuditCage.getDo2().getFromDate(),
				"PAYFIX");

		// String currentPayLevel =
		// Do2Manager.getPayLevelPromot(currentRank.getRankCode());
		String currentPayLevel = Do2Manager.getPayLevelPromot(previousRankPromot.getRankCode());
		String promotPayLevel = Do2Manager.getPayLevelPromot(qRank.getRankCode());

		/*
		 * System.out.println ( " ARMY NO " +nAuditCage.getDo2().getArmyNo());
		 * System.out.println( "current rank " +currentRank.getRankCode() );
		 * System.out.println( " Q rank " +qRank.getRankCode()); System.out.println
		 * (" current pay level " +currentPayLevel); System.out.println(
		 * " promot pay level" +promotPayLevel);
		 */

		if (currentPayLevel != null && promotPayLevel != null && currentPayLevel.equals(promotPayLevel)) {
			if (soldierGroup.equals("X")) {
				nAuditCage.setXGroupPay(3600);
			}
			if (Do2Manager.getJcoRankDescription().contains(nAuditCage.getDo2().getColumnA())) {
				nAuditCage.setClassPay(0);
			}
			return;
		}
		// Added code for OPTFIX Line 974 to 998

		List<Do2> optfixDo2List = do2Repo.findByDescFromDateEqual(nAuditCage.getEmployee().getId(), Do2Manager.VALID,
				"OPTFIX", nAuditCage.getFromDate());

		if ((optfixDo2List != null && optfixDo2List.size() > 0) || (nAuditCage.getDo2().getToDate() != null)) {

			// List<PayMatrix>paymatrixList1
			// =matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel,
			// newBasic); // commented on 16.05.2025 - Guards RTI
			List<PayMatrix> paymatrixList1 = matrixRepo
					.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(promotPayLevel, newBasic);

			if (paymatrixList1.size() > 0) {
				newBasic = paymatrixList1.get(0).getBasicPay();
			}

			nAuditCage.setBasicPay(newBasic);
			nAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
			nAuditCage.setPayLevel(promotPayLevel);

			return;
		}
		List<PayMatrix> paymatrixList = matrixRepo.findByPayLevelAndBasicPayOrderByBasicPayAsc(currentPayLevel,
				basicpay);

		// System.out.println ( " current pay Level " +currentPayLevel + " BASIC PAY "
		// +basicpay +" nAudit cage " +nAuditCage.getBasicPay() + " do2 id "
		// +nAuditCage.getId());

		if (paymatrixList.size() > 0) {
			newBasic = paymatrixList.get(0).getBasicPay();

			// System.out.println ( " current pay Level11 " +currentPayLevel + " BASIC PAY "
			// +basicpay +" nAudit cage " +nAuditCage.getBasicPay() + " new basic "
			// +newBasic);
			if (basicpay.equals(newBasic)) {
				List<PayMatrix> paymatrixList1 = matrixRepo
						.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(currentPayLevel, basicpay);

				if (paymatrixList1.size() > 0) {
					newBasic = paymatrixList1.get(0).getBasicPay();
				}
			} else {
				return;
			}

		}
		/*
		 * System.out.println ( " current pay Level22  " +currentPayLevel +
		 * " BASIC PAY " +basicpay +" nAudit cage " +nAuditCage.getBasicPay() +
		 * " new basic " +newBasic + " PROMOT PAY LEVEL " +promotPayLevel);
		 */
		List<PayMatrix> paymatrixList2 = matrixRepo
				.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel, newBasic);

		if (paymatrixList2.size() > 0) {
			newBasic = paymatrixList2.get(0).getBasicPay();
		}

		// System.out.println (" basic pay " +newBasic +" rank "
		// +currentRank.getRankCode() +"EMPLOYEE " +employee.getArmyNo() + " do2from
		// date " +nAuditCage.getDo2().getFromDate());

		nAuditCage.setBasicPay(newBasic);
		nAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
		nAuditCage.setPayLevel(promotPayLevel);
		if (nAuditCage.getDo2().getToDate() == null) {
			if (!Utils.isAcpBeforeCurrentDo2(nAuditCage.getDo2())) { // added on 21.11.2022 - MRC Wellington email
				if (employee == null) {
					employee = nAuditCage.getEmployee();
				}
				employee.setIncrementDate(Utils.getSevenPcNextIncrementDate(nAuditCage.getDo2().getFromDate()));
				empRepo.save(employee);
			}
		}
		// minimum xgroup pay set
		if (soldierGroup.equals("X") && nAuditCage.getXGroupPay() == 0) {
			nAuditCage.setXGroupPay(3600);
		}
		// class Pay

		// If PROMOT to JCO Rank removing Class Pay
		if (Do2Manager.getJcoRankDescription().contains(nAuditCage.getDo2().getColumnA())) {
			nAuditCage.setClassPay(0);
		}

		// Transport Allowance
		if (nAuditCage.getTransport() != null && nAuditCage.getTransport() != 0) {
			nAuditCage.setTransport(AuditCageHelper.getTransportRate(nAuditCage, bAuditCage));
		}

		/*
		 * if (!(Utils.isDateAfterCurrentQuarterEnding(employee.getIncrementDate()))) {
		 * boolean done = false; for (; !done ;) { createSysIncrDo2(nAuditCage.getDo2(),
		 * employee); if
		 * (Utils.isDateAfterCurrentQuarterEnding(employee.getIncrementDate())) { done =
		 * true; } } }
		 */

		// basic pay
		// The following lines commented hoping that the situtation does not arise in
		// 7cpc since pay fixation based on pay level
		// previously only 3% increase on promotion.

		/*
		 * Date xDate = nAuditCage.getDo2().getFromDate(); Calendar yy = new
		 * GregorianCalendar(); yy.setTimeInMillis(xDate.getTime()); int zz =
		 * yy.get(Calendar.YEAR); Calendar chkDate1 = new GregorianCalendar(zz,
		 * Calendar.JANUARY, 01); Calendar chkDate2 = new GregorianCalendar(zz,
		 * Calendar.JULY, 01); Date jan01 = chkDate1.getTime(); Date jul01 =
		 * chkDate2.getTime();
		 * 
		 * 
		 * if ((xDate.after(jan01) && xDate.before(jul01)) && (employee.getFsDueDate()
		 * == null || (employee.getFsDueDate() != null &&
		 * !employee.getFsDueDate().before(jul01)))) { if
		 * ((nAuditCage.getDo2().getDescription().equals("PROMOT") ||
		 * (nAuditCage.getDo2().getDescription().equals("MSTJCO"))) &&
		 * nAuditCage.getDo2().getColumnA().equals("NBSUB")) {
		 * 
		 * if ((nAuditCage.getBasicPay() < newBasic && nAuditCage.getXGroupPay()==0) ||
		 * (nAuditCage.getBasicPay() < newBasic && nAuditCage.getXGroupPay()==1400)) {
		 * nAuditCage.setBasicPay(newBasic); } } } else { // Added to take care of MACPS
		 * condition - if the PBOR is ACPNK and getting direct SPHAV/GRHAV from sepoy,
		 * then there is no 3% pay fixation // only increase in grade pay (ie 2400 to
		 * 2800) if ((nAuditCage.getDo2().getDescription().equals("MSTHAV") ||
		 * (nAuditCage.getDo2().getDescription().equals("PROMOT") &&
		 * nAuditCage.getDo2().getColumnA().equals("SPHAV")) ||
		 * (nAuditCage.getDo2().getDescription().equals("PROMOT") &&
		 * nAuditCage.getDo2().getColumnA().equals("GRHAV"))) && employee.getNaikDate()
		 * == null && (employee.getAcpnaikDate()!= null &&
		 * !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate()))
		 * && employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))){ ; }
		 * else if ((nAuditCage.getDo2().getDescription().equals("MSTJCO")) &&
		 * employee.getNaikDate() == null && (employee.getAcpnaikDate()!= null &&
		 * !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate()))
		 * && employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))){ if
		 * (nAuditCage.getBasicPay() < newBasic) {
		 * nAuditCage.setBasicPay(currentPayList.get(0).getStartScale()); } ; } else {
		 * if (nAuditCage.getDo2().getDescription().equals("MSTHAV") &&
		 * nAuditCage.getGradePay() == 2800){ ; } else if
		 * (Utils.isPromotionAfterReversionAfter010106(nAuditCage.getDo2())){ ; } else {
		 * nAuditCage.setBasicPay(bAuditCage.getBasicPay() +
		 * (Utils.getIncrmentRoundedToTen(bAuditCage.getBasicPay() +
		 * bAuditCage.getGradePay() + (bAuditCage.getXGroupPay() == null ? 0:
		 * bAuditCage.getXGroupPay()), bAuditCage.getFromDate()))); if
		 * (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
		 * nAuditCage.setBasicPay(currentPayList.get(0).getStartScale()); } } } }
		 * 
		 * // grade pay nAuditCage.setGradePay(currentPayList.get(0).getGradePay()); //
		 * ms pay if (!(nAuditCage.getDo2().getFromDate().before(DateManager
		 * .getSixthCpcAllowanceDate()))) {
		 * nAuditCage.setMsPay(currentPayList.get(0).getMsp()); } // X pay if
		 * (soldierGroup=='X') {
		 * nAuditCage.setXGroupPay(currentPayList.get(0).getXGroupPay()); }
		 */

	}

//new method added for ta -to fix the pay on the same date of promotion
	public void get6cpcOldPayForTa(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Rank qRank = null;
		/*
		 * String rankName= nAuditCage.getDo2().getColumnA();
		 * if(!nAuditCage.getDo2().getColumnA().equals("NBSUB") &&
		 * !nAuditCage.getDo2().getColumnA().equals("NBRIS")){ rankName =
		 * nAuditCage.getDo2().getColumnA().substring(2); } qRank = (Rank)
		 * Utils.getForeignKey(new Rank(), "rankName", rankName);
		 */
		if (nAuditCage.getDo2().getColumnA().equals("PANK") || nAuditCage.getDo2().getColumnA().equals("SBNK")) {
			qRank = rankRepo.findByRankCode("30");
		} else if (nAuditCage.getDo2().getColumnA().equals("PAHAV") || nAuditCage.getDo2().getColumnA().equals("SBHAV")
				|| nAuditCage.getDo2().getColumnA().equals("SPHAV")) {
			qRank = rankRepo.findByRankCode("40");
		} else if (nAuditCage.getDo2().getColumnA().equals("NBSUB")
				|| nAuditCage.getDo2().getColumnA().equals("SPNBSUB")) {
			qRank = rankRepo.findByRankCode("50");
		} else if (nAuditCage.getDo2().getColumnA().equals("PASUB") || nAuditCage.getDo2().getColumnA().equals("SBSUB")
				|| nAuditCage.getDo2().getColumnA().equals("SBRIS")
				|| nAuditCage.getDo2().getColumnA().equals("SPSUB")) {
			qRank = rankRepo.findByRankCode("60");
		} else if (nAuditCage.getDo2().getColumnA().equals("PASUBMAJ")
				|| nAuditCage.getDo2().getColumnA().equals("SBSUBMAJ")) {
			qRank = rankRepo.findByRankCode("70");
		}
		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(nAuditCage.getDo2(), "CUR");

		if (groupCategory != null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}

		if (soldierGroup.equals("Z") && !nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}

		int xGroupPay = Utils.get6cpcXGroupPay(bAuditCage);

		currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(qRank.getId(), soldierGroup,
				nAuditCage.getFromDate());

		/*
		 * System.out.println ( " Q RANK " +qRank +" group " +soldierGroup
		 * +" from date " +nAuditCage.getFromDate()); System.out.println (
		 * " current pay list " +currentPayList.size());
		 */ nAuditCage.setOldBasicPay(bAuditCage.getOldBasicPay() + (Utils.getIncrmentRoundedToTen(
				bAuditCage.getOldBasicPay() + bAuditCage.getOldGradePay() + (xGroupPay), bAuditCage.getFromDate())));

		if (nAuditCage.getOldBasicPay() < currentPayList.get(0).getStartScale()) {
			nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());
		}
		// grade pay
		nAuditCage.setOldGradePay(currentPayList.get(0).getGradePay());
		/*
		 * // ms pay if (!(nAuditCage.getDo2().getFromDate().before(DateManager
		 * .getSixthCpcAllowanceDate()))) {
		 * nAuditCage.setMsPay(currentPayList.get(0).getMsp()); } // X pay if
		 * (soldierGroup=='X') {
		 * nAuditCage.setXGroupPay(currentPayList.get(0).getXGroupPay()); } // class Pay
		 * 
		 * // If PROMOT to JCO Rank removing Class Pay if
		 * (Do2Manager.getJcoRankDescription().contains(nAuditCage.getDo2().getColumnA()
		 * )){ nAuditCage.setClassPay(0); }
		 * 
		 * // Transport Allowance if (nAuditCage.getTransport() != null &&
		 * nAuditCage.getTransport() !=0) {
		 * nAuditCage.setTransport(AuditCageHelper.getTransportRate(nAuditCage,
		 * bAuditCage)); }
		 */
	}

	public void get6cpcOldPay(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());

		Rank qRank = null;
		Date promotPreviousDate = null;
		AuditCage promotPreviousAuditCage = null;
		promotPreviousDate = getPreviousFromDateForPromot(nAuditCage.getDo2());

		if (promotPreviousDate != null) {
			promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCageFor6PcOldPay(promotPreviousDate,
					nAuditCage.getDo2());
		} else {
			promotPreviousAuditCage = AuditCageHelper
					.getPreviousDateAuditCageFor6PcOldPay(nAuditCage.getDo2().getFromDate(), nAuditCage.getDo2());
		}

		if (nAuditCage.getDo2().getDescription().equals("MSTHAV"))
			qRank = rankRepo.findByRankCode("40");
		else if (nAuditCage.getDo2().getColumnA().equals("NKSCALE"))
			qRank = rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVSCALE"))
			qRank = rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NKGRADE"))
			qRank = rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVGRADE"))
			qRank = rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NSUBGRADE"))
			qRank = rankRepo.findByRankCode("49");
		else if (nAuditCage.getDo2().getColumnA().equals("SUBGRADE"))
			qRank = rankRepo.findByRankCode("59");
		else if (nAuditCage.getDo2().getColumnA().equals("SMAJGRADE"))
			qRank = rankRepo.findByRankCode("69");
		else if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE"))
			qRank = rankRepo.findByRankCode("79");
		else if (nAuditCage.getDo2().getColumnA().equals("TSNK"))
			qRank = rankRepo.findByRankCode("20");
		else {
			if (nAuditCage.getDo2().getColumnA().equals("TSNK")) {
				qRank = rankRepo.findByRankCode("20");
			} else if (nAuditCage.getDo2().getColumnA().equals("PANK")
					|| nAuditCage.getDo2().getColumnA().equals("SBNK")) {
				qRank = rankRepo.findByRankCode("30");
			} else if (nAuditCage.getDo2().getColumnA().equals("PAHAV")
					|| nAuditCage.getDo2().getColumnA().equals("SBHAV")
					|| nAuditCage.getDo2().getColumnA().equals("SPHAV")
					|| nAuditCage.getDo2().getColumnA().equals("GRHAV")) {
				qRank = rankRepo.findByRankCode("40");
			} else if (nAuditCage.getDo2().getColumnA().equals("NBSUB")
					|| nAuditCage.getDo2().getColumnA().equals("SPNBSUB")) {
				qRank = rankRepo.findByRankCode("50");
			} else if (nAuditCage.getDo2().getColumnA().equals("PASUB")
					|| nAuditCage.getDo2().getColumnA().equals("SBSUB")
					|| nAuditCage.getDo2().getColumnA().equals("SPRIS")
					|| nAuditCage.getDo2().getColumnA().equals("SPSUB")) {
				qRank = rankRepo.findByRankCode("60");
			} else if (nAuditCage.getDo2().getColumnA().equals("PASUBMAJ")
					|| nAuditCage.getDo2().getColumnA().equals("SBSUBMAJ")) {
				qRank = rankRepo.findByRankCode("70");
			}
		}

		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(nAuditCage.getDo2(), "CUR");

		if (groupCategory != null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}

		if (nAuditCage.getDo2().getColumnA().equals("NBSUB") || nAuditCage.getDo2().getColumnA().equals("SPNBSUB")) {
			soldierGroup = nAuditCage.getDo2().getColumnB().charAt(3) + "";
		}

		if (soldierGroup.equals("Z") && !nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}

		currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(qRank.getId(), soldierGroup,
				nAuditCage.getFromDate());

		Date xDate = nAuditCage.getDo2().getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();

		int xGroupPay = Utils.get6cpcXGroupPay(nAuditCage);
		int promotPreviousAuditCageXGroupPay = xGroupPay;

		if (employee == null) {
			employee = nAuditCage.getEmployee();
		}
		if (nAuditCage.getDo2().getFromDate().equals(employee.getEnrolmentDate())
				&& nAuditCage.getDo2().getColumnA().equals("SPHAV")
				&& nAuditCage.getDo2().getColumnC().equals("DIRECT")) {
			promotPreviousAuditCageXGroupPay = Utils.get6cpcXGroupPay(nAuditCage);
		} else {
			promotPreviousAuditCageXGroupPay = Utils.get6cpcXGroupPay(promotPreviousAuditCage);
		}

		// basic pay
		if ((xDate.after(jan01) && xDate.before(jul01)) && (employee.getFsDueDate() == null
				|| (employee.getFsDueDate() != null && !employee.getFsDueDate().before(jul01)))) {
			if ((nAuditCage.getDo2().getDescription().equals("PROMOT")
					|| (nAuditCage.getDo2().getDescription().equals("MSTJCO")))
					&& nAuditCage.getDo2().getColumnA().equals("NBSUB")) {

				if ((nAuditCage.getOldBasicPay() < 8600 && xGroupPay == 0)
						|| (nAuditCage.getOldBasicPay() < 8580 && xGroupPay == 1400)) {
					nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());
				}
			}
		} else {
			// Added to take care of MACPS condition - if the PBOR is ACPNK and getting
			// direct SPHAV/GRHAV from sepoy, then there is no 3% pay fixation
			// only increase in grade pay (ie 2400 to 2800)
			if ((nAuditCage.getDo2().getDescription().equals("MSTHAV")
					|| (nAuditCage.getDo2().getDescription().equals("PROMOT")
							&& nAuditCage.getDo2().getColumnA().equals("SPHAV"))
					|| (nAuditCage.getDo2().getDescription().equals("PROMOT")
							&& nAuditCage.getDo2().getColumnA().equals("GRHAV")))
					&& employee.getNaikDate() == null
					&& (employee.getAcpnaikDate() != null
							&& !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate()))
							&& employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))) {
				;
			} /*
				 * else if ((nAuditCage.getDo2().getDescription().equals("MSTJCO")) &&
				 * employee.getNaikDate() == null && (employee.getAcpnaikDate()!= null &&
				 * !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate()))
				 * && employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))){ if
				 * (nAuditCage.getOldBasicPay() < currentPayList.get(0).getStartScale()) {
				 * nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale()); } ;
				 */
			else if (nAuditCage.getDo2().getDescription().equals("MSTJCO")) {
				/*
				 * if (employee.getNaikDate() == null && employee.getAcpnaikDate()!=null) { if
				 * (!employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate())){
				 * if (employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate())) { if
				 * (DateManager.getNoOfMonths(employee.getAcpnaikDate(),nAuditCage.getDo2().
				 * getFromDate()) < 96.0) { if (nAuditCage.getOldBasicPay() <
				 * currentPayList.get(0).getStartScale()) {
				 * nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale()); } }
				 * 
				 * } } } else {
				 */
				/*
				 * nAuditCage.setOldBasicPay(promotPreviousAuditCage.getOldBasicPay() +
				 * (Utils.getIncrmentRoundedToTen(promotPreviousAuditCage.getOldBasicPay() +
				 * promotPreviousAuditCage.getGradePay() +
				 * (promotPreviousAuditCage.getXGroupPay() == null ? 0:
				 * promotPreviousAuditCage.getXGroupPay()),
				 * promotPreviousAuditCage.getFromDate())));
				 */
				nAuditCage.setOldBasicPay(bAuditCage.getOldBasicPay() + (Utils.getIncrmentRoundedToTen(
						bAuditCage.getOldBasicPay() + bAuditCage.getOldGradePay() + (xGroupPay),
						bAuditCage.getFromDate())));
				/*
				 * if (nAuditCage.getOldBasicPay() < currentPayList.get(0).getStartScale()) {
				 * nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale()); }
				 */
				// }
			} else {
				if (nAuditCage.getDo2().getDescription().equals("MSTHAV") && nAuditCage.getOldGradePay() == 2800) {
					;
				} else if (Utils.isPromotionAfterReversionAfter010106(nAuditCage.getDo2())) {
					;
				} else {
					promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCageFor6PcOldPay(
							nAuditCage.getDo2().getFromDate(), nAuditCage.getDo2());

					// following commented and bAuditCage replaced with promotPreviousAuditCage as
					// REMUST & PROMOT do2s fed on same day and while
					// adjusting promot, x group pay also taken in to account as the same has not to
					// be taken due to non completion of sixmonths sevice from
					// the drawal of x pay.
					// System.out.println ( " PROMOT PREVIOUS AUDIT CAGE "
					// +promotPreviousAuditCage.getFromDate()
					// +" "+promotPreviousAuditCage.getXGroupPay());
					if (promotPreviousAuditCage != null && promotPreviousAuditCageXGroupPay == 0 && xGroupPay != 0) {
						// System.out.println ( " AAAAAAAAAAAAA PROMOT ");
						nAuditCage.setOldBasicPay(promotPreviousAuditCage.getOldBasicPay()
								+ (Utils.getIncrmentRoundedToTen(promotPreviousAuditCage.getOldBasicPay()
										+ promotPreviousAuditCage.getOldGradePay() + (promotPreviousAuditCageXGroupPay),
										promotPreviousAuditCage.getFromDate())));
					} else if (promotPreviousAuditCage != null && promotPreviousAuditCageXGroupPay != 0
							&& xGroupPay != 0) {
						Date remustDate = Utils.getFromDateForRemust(nAuditCage.getDo2());
						if (remustDate != null && !remustDate.before(DateManager.getSixthCpcDate())) {
							if (DateManager.getNoOfMonths(remustDate, nAuditCage.getDo2().getFromDate()) > 6) {
								AuditCage remustAuditCage = AuditCageHelper.getRemustAuditCage(remustDate,
										nAuditCage.getDo2());
								if (remustAuditCage != null && remustAuditCage.getXGroupPay() != 0) {
									nAuditCage.setOldBasicPay(
											promotPreviousAuditCage.getOldBasicPay() + (Utils.getIncrmentRoundedToTen(
													promotPreviousAuditCage.getOldBasicPay()
															+ promotPreviousAuditCage.getOldGradePay()
															+ (promotPreviousAuditCageXGroupPay),
													promotPreviousAuditCage.getFromDate())));
								} else {
									nAuditCage.setOldBasicPay(
											promotPreviousAuditCage.getOldBasicPay() + (Utils.getIncrmentRoundedToTen(
													promotPreviousAuditCage.getOldBasicPay()
															+ promotPreviousAuditCage.getOldGradePay(),
													promotPreviousAuditCage.getFromDate())));
								}
							} else {
								nAuditCage.setOldBasicPay(
										promotPreviousAuditCage.getOldBasicPay() + (Utils.getIncrmentRoundedToTen(
												promotPreviousAuditCage.getOldBasicPay()
														+ promotPreviousAuditCage.getOldGradePay(),
												promotPreviousAuditCage.getFromDate())));
							}
						}
					} else {
						nAuditCage.setOldBasicPay(bAuditCage.getOldBasicPay() + (Utils.getIncrmentRoundedToTen(
								bAuditCage.getOldBasicPay() + bAuditCage.getOldGradePay() + (xGroupPay),
								bAuditCage.getFromDate())));
					}

					if (currentPayList != null && currentPayList.size() > 0) {
						if (nAuditCage.getOldBasicPay() < currentPayList.get(0).getStartScale()) {
							nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());
						}
					}

				}
			}
		}

		// grade pay
		if (currentPayList != null && currentPayList.size() > 0)
			nAuditCage.setOldGradePay(currentPayList.get(0).getGradePay());

	}

	/*
	 * public void createSysIncrDo2(Do2 do2, Employee employee) { Date dni =
	 * DateManager.getToday(); Calendar empIncrDate = new GregorianCalendar();
	 * empIncrDate.setTimeInMillis(employee.getIncrementDate().getTime()); int year
	 * = empIncrDate.get(Calendar.YEAR); int month = empIncrDate.get(Calendar.MONTH)
	 * + 1; if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) && year ==
	 * 2006 && month == 1) { ; } else if
	 * (do2.getFromDate().before(DateManager.getSeventhCpcDate()) && year >= 2006) {
	 * month = 7; }
	 * 
	 * int date = 01; if (empIncrDate.get(Calendar.DATE) != 01 || month == 7) { try
	 * { dni = DateManager.getDate(year, month, date); } catch (Exception e) {
	 * Utils.handleException(e); } } else { dni = employee.getIncrementDate(); }
	 * List<Do2> duplicateDo2List =
	 * Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
	 * " d.employee=:emp AND d.description='INCR' AND d.status in ('V', 'P') AND d.fromDate = :fromDate"
	 * ) .setParameter("emp", employee) .setParameter("fromDate",
	 * dni).getResultList(); // System.out.println("from date " + dni + " employee "
	 * + employee.getArmyNo() + " size " + duplicateDo2List.size()); if
	 * (duplicateDo2List == null || duplicateDo2List.size() == 0){ Do2 incrDo2 = new
	 * Do2(); incrDo2.setEmployee(employee); incrDo2.setArmyNo(do2.getArmyNo());
	 * incrDo2.setCheckDigit(do2.getCheckDigit());
	 * incrDo2.setUnitCode(do2.getUnitCode()); incrDo2.setUnit((Unit)
	 * Utils.getForeignKey(new Unit(), "unitCode", do2.getUnitCode()));
	 * incrDo2.setDo2Group("IA"); incrDo2.setDo2Type('0'); incrDo2.setDo2No("6666");
	 * incrDo2.setDo2ItemNo("666"); incrDo2.setDo2Date(dni);
	 * incrDo2.setDescription("INCR"); incrDo2.setFromDate(dni);
	 * incrDo2.setColumnA(null); incrDo2.setColumnB(null);
	 * incrDo2.setColumnC("MUSTER"); incrDo2.setTjra(do2.getTjra());
	 * incrDo2.setLra(do2.getLra()); incrDo2.setStatus(Do2Manager.PENDING);
	 * incrDo2.setReason(null); incrDo2.setQe(Utils.getCurrentQuarter());
	 * incrDo2.setInputQe(do2.getInputQe());
	 * incrDo2.setCreatedAt(DateManager.getCurrentDate());
	 * incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
	 * incrDo2.setAuditCage(true); Utils.getEntityManager().persist(incrDo2);
	 * Utils.getEntityManager().flush(); } if (year == 2006 && month == 1) { try {
	 * employee.setIncrementDate(DateManager.getDate(year, 7, date)); } catch
	 * (Exception e) { Utils.handleException(e); } } else if (year == 2006 ) { try {
	 * employee.setIncrementDate(DateManager.getDate(year + 1, month, date)); }
	 * catch (Exception e) { Utils.handleException(e); } } else {
	 * employee.setIncrementDate(DateManager.getNextIncrementDate(employee.
	 * getIncrementDate())); }
	 * 
	 * Utils.getEntityManager().persist(employee); Utils.getEntityManager().flush();
	 * }
	 */
}
