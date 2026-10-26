package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AppointmentRate;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class Promot extends PromotBase implements Do2AuditCageProcessor, java.io.Serializable {

	/*
	 * public String[] strRankReg = new String[] { "SEP", "NK", "HAV", "NBSUB",
	 * "SUB", "SUBMAJ" }; public String[] strRankNgr = new String[] { "SOWAR", "LD",
	 * "DFR", "NBRIS","RIS", "RISMAJ" };
	 */
	public String[] strRankReg = new String[] { "20", "30", "40", "50", "60", "70" };
	boolean actionRequired;
	Employee employee = null;
	Employee previousEmployeeStatus = null;
	Rank rank = null;
	Rank empRank = null;
	public List<String> alwToStop = null;
	public List<PayScale> currentPayList = null;
	public List<PayScale> previousPayList = null;
	public List<EmployeeServiceDetail> esdPromotList = null;

	public List<Do2> do2List;
	public List<Do2> acpDo2List;
	public Date chkDate1 = null;
	public Date chkDate2 = null;
	Do2 acpDo2;
	char empGroup = ' ';
	String acpRankDescription = "";
	Date acpPromotDate;

	public static final String INVALID_PROMOT_RANK = "AU:Invalid Lower Rank For Promot To Higher Rank.\n";
	public static final String DUPLICATE_IN_ESD = "DU:PROMOT DOII already adjusted and available in ESD table.\n";
	public static final String INVALID_PAY_STAGE = "AU:Pay stage wrong for fixation.\n";
	public static final String DUPLICATE_DO2 = "DU:DO2 already available with same rank. \n";
	public static final String PBOR_ON_LEAVE = "AU:PBOR is on leave on the date of promotion. \n";
	public static final String SIXCPC_TSNK_NOT_AVAILABLE = "AU:Orders for TSNK fixation in 6CPC awaited. \n";
	public static final String SAME_PAID_ACTING_RANK = " Enrolled as SUB / SUB MAJ ";
	public static final String MISMATCH_PAY = " Mismatch in Current Rank i.e. current Pay level and corresponding current pay ";
	static public final String INCR_NOT_DUE_ON_OPTION_DATE = "AU:Increment Not Due In Pre-Promoted/Pre-Upgraded Grade On Opted Date \n";
	static public final String INVALID_TODATE = "VL:To-Date/Option Not Acceptable In This DO2 Format Prior To Option Approval Date, 26.02.2019 \n";

	StringBuffer buffer = new StringBuffer();
	boolean promotValidity = true;
	public static Date revised_appxj_impl_date;

	@Override
	public boolean isValid(Do2 do2) {
		// checks substantive promotion is available before
		// paid acting and rejects the paid acting part II order added on
		// 16042012
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (!do2.getDo2Date().before(revised_appxj_impl_date)
				&& (do2.getColumnA() != null && do2.getColumnA().equals("PANK") || do2.getColumnA().equals("PALD")
						|| do2.getColumnA().equals("PASUBMAJ") || do2.getColumnA().equals("PARISMAJ"))) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("Paid acting promotions are admissible only for Hav/Dfr/Sub/Ris");
			do2Repo.save(do2);
			return false;
		}
		if (do2.getAcceptedQe() == null) {
			if (Do2Manager.getPaidActingDescription().contains(do2.getColumnA())) {
				List<Do2> substantiveList = Utils.getSubstantiveDo2(do2);
				if (substantiveList.size() > 0) {
					if (substantiveList.get(0).getFromDate().before(do2.getFromDate())) {
						Do2 subDo2 = substantiveList.get(0);
						do2.setReason("SUBSTANTIVE PROMOT AVAILABLE BEFORE PAID ACTING From Date  "
								+ subDo2.getFromDate() + "  Do2 No " + subDo2.getDo2No() + " Item No "
								+ subDo2.getDo2ItemNo() + "  Do2 Date " + subDo2.getDo2Date());
						do2.setStatus(Do2Manager.INVALID);
						do2Repo.save(do2);
						return false;
					}

				}
			}
		}

		if (do2.getAcceptedQe() == null) {
			if (!do2.getFromDate().before(DateManager.getSixthCpcDate())) {
				if (do2.getDescription().equals("PROMOT") && do2.getColumnA().equals("NBSUB")
						&& do2.getColumnB() != null
						&& do2.getColumnB().substring(do2.getColumnB().length() - 1).equals("Z")) {
					do2.setStatus(Do2Manager.INVALID);
					do2.setReason("VL: Invalid Group in column 7B");
					do2Repo.save(do2);
					return false;
				}
			}
			if (do2.getColumnA().equals("NBSUB")) {
				if (!AuditCageHelper.isValidZcategory(do2)) {
					do2.setStatus(Do2Manager.INVALID);
					do2.setReason("VL: Group mismatch with column7B and column 7C");
					do2Repo.save(do2);
					return false;
				}
			}
		}
		if (do2.getStatus() != null && do2.getStatus().equals(Do2Manager.DO2_CANCEL_STATUS))
			return true;

		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (do2.getAcceptedQe() == null) {
			empRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
		} else {
			empRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PROMOT");
		}

		if (empRank == null) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(Do2Manager.MISSING_SERVICE_DETAIL);
			do2Repo.save(do2);
			return false;
		}
		if (!isValidColumnA(do2)) {
			buffer.append(SAME_PAID_ACTING_RANK);
			promotValidity = false;
		}

		if (isNotDuplicate(do2)) {
			if (!do2.getColumnA().equals("SPHAV") && !do2.getColumnA().equals("GRHAV")
					&& !do2.getColumnA().equals("SPNBSUB") && !do2.getColumnA().equals("SPRIS")) {
				if (!isValidRank(do2)) {
					do2.setStatus(Do2Manager.PENDING);
					buffer.append(INVALID_PROMOT_RANK);
					promotValidity = false;
				}
			} else if (do2.getColumnA().equals("SPHAV") || do2.getColumnA().equals("GRHAV")) {
				if (do2.getFromDate().equals(employee.getEnrolmentDate())) {
					if (do2.getColumnC() == null || (do2.getColumnC() != null && !do2.getColumnC().equals("DIRECT"))) {
						do2.setStatus(Do2Manager.PENDING);
						buffer.append("Promotion can not be published with enrollment date");
						promotValidity = false;
					}

				}
			}
		}

		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			if (!(isValidPayStage(do2))) {
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(INVALID_PAY_STAGE);
				promotValidity = false;
			}
		}

		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			if (!isValidPayLevel(do2)) {
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(MISMATCH_PAY);
				promotValidity = false;
			}
		}

		// validation of leave on MACPS removed - when PBOR is on leave)
		if (!do2.getDescription().equals("MACPS")) {
			if (isPborOnRegularLeave(do2, employee)) {
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(PBOR_ON_LEAVE);
				promotValidity = false;
			}
		}

		if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getToDate() != null)
			if (!doesIncrExistsOnOptionDate(do2, employee)) {
				buffer.append(INCR_NOT_DUE_ON_OPTION_DATE);
				promotValidity = false;
				do2.setStatus(Do2Manager.INVALID);
			}

		/*
		 * if (do2.getFromDate().before(DateManager.getOptionOnPromotionApprovedDate())
		 * && do2.getToDate()!=null) { List<Do2> optfixDo2List =
		 * do2Repo.findByDescFromDateEqual(do2.getEmployee().getId(), "V", "OPTFIX",
		 * do2.getFromDate()); // Utils.getEntityManager().
		 * createQuery("SELECT d from Do2 d where d.description='OPTFIX' and d.fromDate=:fd "
		 * // + " and d.employee=:emp and d.status='V' ").setParameter("fd",
		 * do2.getFromDate()).setParameter("emp", do2.getEmployee()).getResultList(); if
		 * (optfixDo2List==null || (optfixDo2List!=null && optfixDo2List.size()==0)) {
		 * buffer.append(INVALID_TODATE); promotValidity=false;
		 * do2.setStatus(Do2Manager.INVALID); } }
		 */

		if (!promotValidity) {
			if (do2.getStatus() == null)
				do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			// profix not requried for TA personnel as the pay will be fixed on
			// the same date
			if (promotValidity && do2.getId() == newDo2.getId()) {
				// if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())
						|| (do2.getFromDate().after(DateManager.getOptionOnPromotionApprovedDate())
								&& do2.getToDate() != null)) {
					if (do2.getAcceptedQe() == null && employee.getRegularTaFlag() != null
							&& employee.getRegularTaFlag().equals("R")) {
						if (!Utils.isAcpBeforeCurrentDo2(do2)) {
							if (do2.getColumnA().equals("NBSUB")) {
								if (!isMinimumPay(do2) && !do2.getFromDate().before(DateManager.getSixthCpcDate())
										&& do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
									createSysDo2(employee, do2);
								} else if (do2.getFromDate().after(DateManager.getOptionOnPromotionApprovedDate())) {
									createSysDo2(employee, do2);
								}
							} else {
								createSysDo2(employee, do2);
							}
						}
					}
				}
			}

			/*
			 * if (do2.getDescription().equals("PROMOT") &&
			 * do2.getColumnA().startsWith("NB")){ if
			 * (!AuditCageHelper.isTdmergDo2Exist(do2)){ String presentTrade = null; String
			 * sldrClass = null; String group =""; String
			 * previousTrade=do2.getColumnC().trim(); if (do2.getColumnB()!=null &&
			 * do2.getColumnB().length()>3){ group = do2.getColumnB().charAt(3)+""; }
			 * List<GroupCategory> grpCatList =
			 * gcRepo.findBySoldierGroupAndSoldierCategory(group, previousTrade);
			 * 
			 * // Utils.getEntityManager().createQuery( //
			 * "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat "
			 * ) // .setParameter("sgrp", group ).setParameter("scat",
			 * previousTrade).getResultList(); if ((grpCatList.size() > 0) &&
			 * (previousTrade.equals(grpCatList.get(0).getSoldierCategory()))) { if
			 * (grpCatList.get(0).getMergedAbbr() != null){ presentTrade =
			 * grpCatList.get(0).getMergedAbbr(); AuditCageHelper.createMergedTradeDo2(do2,
			 * sldrClass, previousTrade, presentTrade); } } } }
			 */

			if (do2.getDescription().equals("PROMOT") && do2.getColumnA().startsWith("NB")) {
				if (!AuditCageHelper.isTdmergDo2Exist(do2)) {
					String presentTrade = null;
					String sldrClass = null;
					String group = "";
					String previousTrade = do2.getColumnC().trim();
					if (do2.getColumnB() != null && do2.getColumnB().length() > 3) {
						group = do2.getColumnB().charAt(3) + "";
					}
					List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(group, previousTrade);

					// Utils.getEntityManager().createQuery(
					// "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND
					// gc.soldierCategory=:scat ")
					// .setParameter("sgrp", group ).setParameter("scat",
					// previousTrade).getResultList();
					if ((grpCatList.size() > 0) && (previousTrade.equals(grpCatList.get(0).getSoldierCategory()))) {
						if (grpCatList.get(0).getMergedAbbr() != null) {
							presentTrade = grpCatList.get(0).getMergedAbbr();
							List<GroupCategory> grpCatList1 = gcRepo.findBySoldierGroupAndSoldierCategory(group,
									presentTrade);
							Date presentTradeFromDate = grpCatList1.get(0).getFromDate();
							if (!do2.getFromDate().after(presentTradeFromDate)) {
								AuditCageHelper.createMergedTradeDo2(do2, sldrClass, previousTrade, presentTrade,
										presentTradeFromDate);
							} else {
								AuditCageHelper.createMergedTradeDo2(do2, sldrClass, previousTrade, presentTrade);
							}

						}
					}
				}
			}
			// Utils.getEntityManager().merge(do2);

		}

		do2.setReason(buffer.toString());
		// Utils.getEntityManager().flush();

		return promotValidity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		previousEmployeeStatus = employee;
		actionRequired = true;

		updateBdtInfo(newAuditCage.getDo2());
		// If PROMOT to JCO Rank removing Class Pay
		if (Do2Manager.getJcoRankDescription().contains(newAuditCage.getDo2().getColumnA())) {
			newAuditCage.setClassPay(0);
			newAuditCage.setOldClassPay(0);
		}

		// Transport Allowance
		if (newAuditCage.getTransport() != null && newAuditCage.getTransport() != 0) {
			newAuditCage.setTransport(AuditCageHelper.getTransportRate(newAuditCage, baseAuditCage));
		}

		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (newAuditCage.getDo2().getColumnA().equals("SBSUB")) {
				if (employee.getSubDate().equals(employee.getEnrolmentDate())) {
					return newAuditCage;
				}
			}

			if (newAuditCage.getDo2().getColumnA().equals("SBSUBMAJ")) {
				if (employee.getSubmajDate().equals(employee.getEnrolmentDate())) {
					return newAuditCage;
				}
			}

			String columnA = "SB" + newAuditCage.getDo2().getColumnA().substring(2);

			/*
			 * if (newAuditCage.getDo2().getColumnA().equals(columnA)) {
			 * 
			 * List<Do2> do2List = getLatenrDo2(newAuditCage.getDo2()); if (do2List.size() >
			 * 0) { Do2 paDo2 = do2List.get(0); if
			 * (!newAuditCage.getFromDate().before(paDo2.getFromDate())) { return
			 * newAuditCage; } } }
			 */

			// Above method changed to address revert cases also
			if (newAuditCage.getDo2().getColumnA().equals(columnA)) {

				List<Do2> do2List = getLatenrDo2(newAuditCage.getDo2());
				if (do2List.size() > 0) {
					Do2 paDo2 = do2List.get(0);
					Date paDo2Date = paDo2.getFromDate();
					// System.out.println (" paDo2 Date " +paDo2.getFromDate() +"
					// "+newAuditCage.getFromDate());
					List<Do2> revertDo2List = getRevertDo2(newAuditCage.getDo2(), paDo2Date);
					if (revertDo2List.size() > 0) {
						Do2 revertDo2 = revertDo2List.get(0);
						if (!newAuditCage.getFromDate().after(revertDo2.getFromDate())) {
							return newAuditCage;
						}
					} else if (!newAuditCage.getFromDate().before(paDo2.getFromDate())) {
						return newAuditCage;
					}
				}
			}

		}
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_ACR)
				|| Utils.getPao().getPaoCode().equals(Do2Manager.PAO_MIRC)) {

			String columnA = "SB" + newAuditCage.getDo2().getColumnA().substring(2);

			/*
			 * if (newAuditCage.getDo2().getColumnA().equals(columnA)) {
			 * 
			 * List<Do2> do2List = Utils.getPaidActingDo2(newAuditCage .getDo2()); if
			 * (do2List.size() > 0) { Do2 paDo2 = do2List.get(0); if
			 * (!newAuditCage.getFromDate().before(paDo2.getFromDate())) { return
			 * newAuditCage; } }
			 * 
			 * }
			 */

			// Above method changed to address revert cases also
			if (newAuditCage.getDo2().getColumnA().equals(columnA)) {

				List<Do2> do2List = getLatenrDo2(newAuditCage.getDo2());
				if (do2List.size() > 0) {
					Do2 paDo2 = do2List.get(0);
					Date paDo2Date = paDo2.getFromDate();
					// System.out.println (" paDo2 Date " +paDo2.getFromDate() +"
					// "+newAuditCage.getFromDate());
					List<Do2> revertDo2List = getRevertDo2(newAuditCage.getDo2(), paDo2Date);
					if (revertDo2List.size() > 0) {
						Do2 revertDo2 = revertDo2List.get(0);
						if (!newAuditCage.getFromDate().after(revertDo2.getFromDate())) {
							return newAuditCage;
						}
					} else if (!newAuditCage.getFromDate().before(paDo2.getFromDate())) {
						return newAuditCage;
					}
				}
			}

			// *********lines added
			if (Utils.isAcpBeforeCurrentDo2(newAuditCage.getDo2())) {
				return newAuditCage;
			}
			Date sixcpcFromDate = sixcpcDo2Exists(newAuditCage.getDo2());

			/*
			 * if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) ||
			 * (sixcpcFromDate != null &&
			 * !newAuditCage.getFromDate().after(sixcpcFromDate))) {
			 * get5cpcPay(newAuditCage, baseAuditCage, previousEmployeeStatus); } else {
			 * get6cpcPay(newAuditCage, baseAuditCage); }
			 */

			// above lines commented and modified as stated below due to sevencpc

			if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					|| (sixcpcFromDate != null && !newAuditCage.getFromDate().after(sixcpcFromDate))) {
				get5cpcPay(newAuditCage, baseAuditCage, previousEmployeeStatus);
			} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
				get6cpcPay(newAuditCage, baseAuditCage);
				getOldBasicPay(newAuditCage, baseAuditCage, previousEmployeeStatus);
			} else {
				if (newAuditCage.getGradePay() == 0) {
					get7cpcPay(newAuditCage, baseAuditCage);
					get6cpcOldPay(newAuditCage, baseAuditCage);
				} else if (newAuditCage.getPayLevel() != null && newAuditCage.getPayLevel().equals("0")
						&& !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
					get6cpcPay(newAuditCage, baseAuditCage);
					get6cpcOldPay(newAuditCage, baseAuditCage);
				}
			}
			updateEsd(newAuditCage.getDo2());

			/*
			 * if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			 * getOldBasicPay(newAuditCage, baseAuditCage, previousEmployeeStatus); }
			 */

		} else {
			String columnA = "SB" + newAuditCage.getDo2().getColumnA().substring(2);
			if (newAuditCage.getDo2().getColumnA().equals(columnA)) {
				List<Do2> do2List = Utils.getPaidActingDo2(newAuditCage.getDo2());
				if (do2List.size() > 0) {
					Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate(),
							"PROMOT");
					int currentRank = 0;
					if (newAuditCage.getDo2().getColumnA().equals("NBSUB")) {
						currentRank = 50;
					} else if (newAuditCage.getDo2().getColumnA().substring(2).equals("NK")) {
						currentRank = 30;
					} else if (newAuditCage.getDo2().getColumnA().substring(2).equals("HAV")) {
						currentRank = 40;
					} else if (newAuditCage.getDo2().getColumnA().substring(2).equals("SUB")) {
						currentRank = 60;
					} else if (newAuditCage.getDo2().getColumnA().substring(2).equals("SUBMAJ")) {
						currentRank = 70;
					}
					int existingRank = Integer.parseInt(rank.getRankCode());
					if (existingRank >= currentRank)
						return newAuditCage;
				} else if (columnA.equals("SBHAV")) {
					do2List = do2Repo.findAllByEmployeeIdAndDescriptionAndStatusOrderByFromDateDesc(
							Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo()).getId(), "MSTHAV",
							Do2Manager.VALID);

					/*
					 * Utils.getEntityManager().createQuery(
					 * " SELECT d FROM Do2 d WHERE d.employee=:emp AND d.description=:promot AND " +
					 * " d.status=:status order by d.fromDate desc")
					 * .setParameter("emp",Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo(
					 * ))) .setParameter("promot", "MSTHAV") .setParameter("status",
					 * Do2Manager.VALID).getResultList();
					 */
					if (do2List.size() > 0) {
						Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate(),
								"PROMOT");
						int existingRank = Integer.parseInt(rank.getRankCode());
						int currentRank = 0;
						if (newAuditCage.getDo2().getColumnA().equals("NBSUB")) {
							currentRank = 50;
						} else if (newAuditCage.getDo2().getColumnA().substring(2).equals("NK")) {
							currentRank = 30;
						} else if (newAuditCage.getDo2().getColumnA().substring(2).equals("HAV")) {
							currentRank = 40;
						} else if (newAuditCage.getDo2().getColumnA().substring(2).equals("SUB")) {
							currentRank = 60;
						} else if (newAuditCage.getDo2().getColumnA().substring(2).equals("SUBMAJ")) {
							currentRank = 70;
						}
						if (existingRank >= currentRank)
							return newAuditCage;
					}
				}
			}
			if (Utils.isAcpBeforeCurrentDo2(newAuditCage.getDo2())) {
				return newAuditCage;
			}
			// or condition added to fix the pay on promotion
			// in 5cpc if sixcpc exists-30082012
			Date sixcpcFromDate = sixcpcDo2Exists(newAuditCage.getDo2());

			/*
			 * if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) ||
			 * (sixcpcFromDate != null &&
			 * !newAuditCage.getFromDate().after(sixcpcFromDate))) {
			 * get5cpcPay(newAuditCage, baseAuditCage, previousEmployeeStatus); } else { //
			 * depending on the regular_ta_flag method will be called to fix the pay
			 * Employee emp = Utils.getCurrentEmployee(newAuditCage.getDo2() .getArmyNo());
			 * if (emp.getRegularTaFlag() != null && emp.getRegularTaFlag() == 'T') {
			 * get6cpcPayForTa(newAuditCage, baseAuditCage); } else {
			 * get6cpcPay(newAuditCage, baseAuditCage); }
			 * 
			 * }
			 */

			// above lines commented and modified as stated below due to sevencpc

			if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					|| (sixcpcFromDate != null && !newAuditCage.getFromDate().after(sixcpcFromDate))) {
				get5cpcPay(newAuditCage, baseAuditCage, previousEmployeeStatus);

			} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
				// depending on the regular_ta_flag method will be called to fix the pay
				Employee emp = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());

				if (emp.getRegularTaFlag() != null && emp.getRegularTaFlag().equals("T")) {
					get6cpcPayForTa(newAuditCage, baseAuditCage);
				} else {
					get6cpcPay(newAuditCage, baseAuditCage);
					getOldBasicPay(newAuditCage, baseAuditCage, previousEmployeeStatus);
				}

			} else {
				if (newAuditCage.getGradePay() == 0) {
					get7cpcPay(newAuditCage, baseAuditCage);
					// depending on the regular_ta_flag method will be called to fix the pay
					Employee emp = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());

					if (emp.getRegularTaFlag() != null && emp.getRegularTaFlag().equals("T")) {
						get6cpcOldPayForTa(newAuditCage, baseAuditCage);
					} else {
						get6cpcOldPay(newAuditCage, baseAuditCage);
					}

				} else if (newAuditCage.getPayLevel() != null && newAuditCage.getPayLevel().equals("0")
						&& !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
					get6cpcPay(newAuditCage, baseAuditCage);
					get6cpcOldPay(newAuditCage, baseAuditCage);
				}
			}

			updateEsd(newAuditCage.getDo2());
			/*
			 * if (!newAuditCage.getFromDate().before( DateManager.getSixthCpcDate())) {
			 * getOldBasicPay(newAuditCage, baseAuditCage, previousEmployeeStatus); }
			 */

		}

		return newAuditCage;
	}

	// IF LOGIC FOR SBSTANTIVE AND PAACTING IS CORRECT THIS METHOD TO BE
	// UNCOMMENTED AND ABOVE METHOD TO BE REMOVED
	/*
	 * @Override public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage
	 * baseAuditCage) { employee =
	 * Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
	 * previousEmployeeStatus = employee; actionRequired = true;
	 * 
	 * updateBdtInfo(newAuditCage.getDo2());
	 * 
	 * String columnA = "SB" + newAuditCage.getDo2().getColumnA().substring(2);
	 * System .out.println("promot at cage  "+columnA+"  "+newAuditCage.getDo2().
	 * getColumnA ()); if (newAuditCage.getDo2().getColumnA().equals(columnA)) {
	 * 
	 * List<Do2> do2List = Utils.getPaidActingDo2(newAuditCage.getDo2());
	 * if(do2List.size() >0){ Do2 paDo2 = do2List.get(0);
	 * if(!newAuditCage.getFromDate().before(paDo2.getFromDate())){ return
	 * newAuditCage; } }
	 * System.out.println("inside if condition sb "+do2List.size()); if
	 * (do2List.size() > 0) return newAuditCage; }
	 * 
	 * //*******lines added String pacolumnA = "PA" +
	 * newAuditCage.getDo2().getColumnA().substring(2);
	 * System.out.println("promot at cage  "
	 * +pacolumnA+"  "+newAuditCage.getDo2().getColumnA()); if
	 * (newAuditCage.getDo2().getColumnA().equals(pacolumnA)) { List<Do2> do2List =
	 * Utils.getSubstantiveDo2(newAuditCage.getDo2()); if(do2List.size() >0){ Do2
	 * substantiveDo2 = do2List.get(0); if(!newAuditCage
	 * .getDo2().getFromDate().before(substantiveDo2.getFromDate())){ return
	 * newAuditCage; } }
	 * System.out.println("inside if condition pa "+do2List.size());
	 * 
	 * } //*********lines added
	 * if(Utils.isAcpBeforeCurrentDo2(newAuditCage.getDo2())){ return newAuditCage;
	 * }
	 * 
	 * if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
	 * get5cpcPay(newAuditCage, baseAuditCage, previousEmployeeStatus); } else {
	 * get6cpcPay(newAuditCage, baseAuditCage); }
	 * 
	 * if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
	 * getOldBasicPay(newAuditCage, baseAuditCage, previousEmployeeStatus); }
	 * 
	 * return newAuditCage; }
	 */
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		String columnA = "PA" + do2.getColumnA().substring(2);
		if (do2.getColumnA().equals(columnA)) {
			List<Do2> do2List = Utils.getSubstantiveDo2(do2);
			if (do2List.size() > 0) {
				AuditCageHelper.reverseUpdateESDForPromotCancel(do2List.get(0), Do2Manager.ESD_PROMOT);
				AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT + "");
			} else {
				AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT + "");
			}
		} else {
			List<Do2> do2List = Utils.getPaidActingDo2(do2);
			if (do2List.size() == 0)
				AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT + "");
		}

		if (Do2Manager.getJcoRankDescription().contains(newAuditCage.getDo2().getColumnA())) {
			final String[] alwNotAdmissible = { "APTPAY", "GSPAY", "FAA" };
			alwToStop = Arrays.asList(alwNotAdmissible);
		} else {
			if ((do2.getColumnA().equals("PAHAV")) || (do2.getColumnA().equals("SBHAV"))) {
				final String[] alwNotAdmissible = { "APTPAY", "LSGCM" };
				alwToStop = Arrays.asList(alwNotAdmissible);
			} else {
				final String[] alwNotAdmissible = { "APTPAY" };
				alwToStop = Arrays.asList(alwNotAdmissible);
			}

		}
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		// System.out.println(" DO2 for Cancellation: "+currentDo2.getDescription()+"
		// "+refDo2.getDescription());
		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());

		List<EmployeeServiceDetail> esdList = esdRepo.findByDo2(refDo2.getId());

		/*
		 * Utils.getEntityManager() .createQuery(
		 * "SELECT esd FROM EmployeeServiceDetail esd " +
		 * " WHERE esd.do2=:do2 ").setParameter("do2", refDo2).getResultList();
		 */

		if (esdList.size() > 0)
			AuditCageHelper.reverseUpdateESDForPromotCancel(refDo2, Do2Manager.ESD_PROMOT);

		reverseUpdateEmployee(refDo2);

		return Do2Manager.SUCCESS;
	}

	public boolean isPborOnRegularLeave(Do2 do2, Employee employee) {
		Date adjustedQeDate = null;
		Date dolphinStartDate = Utils.getDolphinStartDate();
		if (do2.getAcceptedQe() != null) {
			try {
				adjustedQeDate = DateManager
						.getDate(do2.getAcceptedQe().substring(3) + "-" + do2.getAcceptedQe().substring(0, 2) + "-01");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (adjustedQeDate != null && adjustedQeDate.before(dolphinStartDate)) {
			return false;
		}
		if (do2.getColumnA().equals("TSNK")) {
			return false;
		}
		List<String> descList = Arrays.asList(new String[] { "CL", "EXTNCL" });
		List<Do2> do2List = do2Repo.findForMultipleDescriptionsWithFromDateBetween(employee.getId(), Do2Manager.VALID,
				Do2Manager.PENDING, descList, do2.getFromDate());

		/*
		 * Utils.getEntityManager().createQuery(
		 * " SELECT d2 FROM Do2 d2 WHERE d2.employee.id=:empId AND " +
		 * " :fromDate BETWEEN d2.fromDate AND d2.toDate AND " +
		 * " (d2.status=:status OR d2.status=:status1) AND " +
		 * " d2.description!=:desc1 AND d2.description!=:desc2 ") .setParameter("empId",
		 * employee.getId()) .setParameter("fromDate", do2.getFromDate())
		 * .setParameter("status", Do2Manager.VALID)
		 * .setParameter("status1",Do2Manager.PENDING) .setParameter("desc1", "CL")
		 * .setParameter("desc2", "EXTNCL").getResultList();
		 */
		/*
		 * for (Do2 leaveDo2 : do2List) { if
		 * (Do2Manager.getAllLeaveTypes().contains(leaveDo2.getDescription())) return
		 * true; }
		 */

		for (Do2 leaveDo2 : do2List) {
			if (Do2Manager.getAllLeaveTypes().contains(leaveDo2.getDescription())) {
				Date leaveDo2FromDate = leaveDo2.getFromDate();
				List<Do2> recallDo2List = do2Repo.findRecallDo2(employee, leaveDo2FromDate);
				if (recallDo2List.size() != 0) {
					if (recallDo2List.get(0).getToDate().equals(leaveDo2.getToDate()))
						;
					return false;
				}
				return true;
			}

		}
		return false;
	}

	public boolean isMinimumPay(Do2 do2) {

		List<AuditCage> auditCageList = acRepo.findByFromDateLessThanEqualOrderByFromDateDesc(
				Utils.getCurrentEmployee(do2.getArmyNo()).getId(), do2.getFromDate());

		/*
		 * Utils .getEntityManager() .createQuery(
		 * "SELECT ac FROM AuditCage ac WHERE ac.employee=:employee " +
		 * " AND ac.cancelled=false AND ac.recordStatus!='P' AND ac.absentFlag=false " +
		 * " AND ac.fromDate<=:fromDate AND ac.effective=true ORDER BY fromDate DESC ")
		 * .setParameter("employee", Utils.getCurrentEmployee(do2.getArmyNo()))
		 * .setParameter("fromDate", do2.getFromDate()).getResultList();
		 */

		if (auditCageList.size() > 0) {
			if (auditCageList.get(0).getXGroupPay() != null && auditCageList.get(0).getXGroupPay() != 0
					&& auditCageList.get(0).getBasicPay() < 8580) {
				return true;
			}

			if (auditCageList.get(0).getXGroupPay() != null && auditCageList.get(0).getXGroupPay() == 0
					&& auditCageList.get(0).getBasicPay() < 8600) {
				return true;
			}

		}
		return false;

	}

	public boolean isDo2InESD(Do2 do2) {
		List<EmployeeServiceDetail> esdList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateAsc(
				Utils.getCurrentEmployee(do2.getArmyNo()).getId(), Do2Manager.ESD_PROMOT + "");

		/*
		 * Utils.getEntityManager() .createQuery(
		 * "SELECT esdl FROM EmployeeServiceDetail AS esdl WHERE " +
		 * "esdl.employee=:emp AND esdl.status=:stat") .setParameter("emp",
		 * Utils.getCurrentEmployee(do2.getArmyNo())) .setParameter("stat",
		 * Do2Manager.ESD_PROMOT).getResultList();
		 */
		if (esdList.size() > 0) {
			for (EmployeeServiceDetail esdp : esdList) {
				if (esdp.getDo2().getColumnA().trim().equals(do2.getColumnA().trim()))
					return false;
			}
		}
		return true;
	}

	/*
	 * public boolean isValidRank(Do2 do2) {
	 * 
	 * if (isValidSameRank(do2)) { return true; } String rankName =
	 * empRank.getRankName(); String columnA = do2.getColumnA();
	 * 
	 * if (!columnA.equals("NBSUB") && !columnA.equals("NBRIS")) { columnA =
	 * do2.getColumnA().substring(2); } if (columnA.equals("NK") &&
	 * rankName.equals("TSNK")) { return true; } if (do2.getColumnC() != null &&
	 * do2.getColumnC().equals("OUTOFTURN")){ int promotrnkcd = 0; if
	 * (newAuditCage.getDo2().getColumnA().equals("NBSUB")){ promotrnkcd=50; } else
	 * if (newAuditCage.getDo2().getColumnA().substring(2).equals("NK")){
	 * promotrnkcd=30; } else if
	 * (newAuditCage.getDo2().getColumnA().substring(2).equals("HAV")){
	 * promotrnkcd=40; } else if
	 * (newAuditCage.getDo2().getColumnA().substring(2).equals("SUB")){
	 * promotrnkcd=60; } else if
	 * (newAuditCage.getDo2().getColumnA().substring(2).equals("SUBMAJ")){
	 * promotrnkcd=70; } int presentrnkcd = Integer.parseInt(empRank.getRankCode());
	 * if (promotrnkcd > presentrnkcd){ return true; } return false; } for (int i =
	 * 0; i < strRankReg.length; i++) { if (columnA.equals(strRankReg[i]) &&
	 * (rankName.equals(strRankReg[i - 1]))) { return true; } } return false; }
	 */

	public boolean isValidRank(Do2 do2) {

		if (isValidSameRank(do2)) {
			return true;
		}
		int presentrnkcd = Integer.parseInt(empRank.getRankCode());
		if (presentrnkcd == 25)// TSNK
			presentrnkcd = 20;
		int promotrnkcd = 0;
		if (do2.getColumnA().equals("NBSUB")) {
			promotrnkcd = 50;
		} else if (do2.getColumnA().substring(2).equals("NK")) {
			promotrnkcd = 30;
		} else if (do2.getColumnA().substring(2).equals("HAV")) {
			promotrnkcd = 40;
		} else if (do2.getColumnA().substring(2).equals("SUB")) {
			promotrnkcd = 60;
		} else if (do2.getColumnA().substring(2).equals("SUBMAJ")) {
			promotrnkcd = 70;
		}
		if (do2.getColumnC() != null && do2.getColumnC().equals("OUTOFTURN")) {
			if (promotrnkcd > presentrnkcd) {
				return true;
			}
			return false;
		}
		for (int i = 0; i < strRankReg.length; i++) {
			if (promotrnkcd == Integer.parseInt(strRankReg[i])
					&& (presentrnkcd == Integer.parseInt(strRankReg[i - 1]))) {
				return true;
			}
		}
		return false;
	}

	public boolean isValidSameRank(Do2 do2) {
		String rankCode = null;

		String rankName = do2.getColumnA();

		if (do2.getColumnA() != null && (do2.getColumnA().startsWith("PA") || do2.getColumnA().startsWith("SB"))) {
			rankName = do2.getColumnA().substring(2);
		}
		if (rankName.equals("NK")) {
			rankCode = "30";
		} else if (rankName.equals("HAV")) {
			rankCode = "40";
		} else if (rankName.equals("SUB")) {
			rankCode = "60";
		} else if (rankName.equals("SUBMAJ")) {
			rankCode = "70";
		}

		if (empRank.getRankCode().equals(rankCode)) {
			if (do2.getColumnA().substring(0, 2).equals("PA") || do2.getColumnA().substring(0, 2).equals("SB")) {
				return true;
			}

		}

		return false;

	}

	public boolean isNotDuplicate(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		int currentDo2RankCode = 0;
		List<Do2> do2List = do2Repo.findByDescColumnAFromDateEqualIdNotEqual(employee.getId(), Do2Manager.VALID,
				do2.getDescription(), do2.getColumnA(), do2.getId(), do2.getFromDate());

		if (do2List.size() > 0) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(DUPLICATE_DO2);
			promotValidity = false;
			return false;
		}

		do2List = do2Repo.findByDescColumnAIdNotEqual(employee.getId(), Do2Manager.VALID, do2.getDescription(),
				do2.getColumnA(), do2.getId());

		if (do2List.size() > 0) {
			int existingRankCode = Integer.parseInt(empRank.getRankCode());
			String rankCode = null;
			if (do2.getColumnA() != null && !do2.getColumnA().equals("NBSUB")) {
				if (do2.getColumnA().substring(2).equals("NK")) {
					rankCode = "30";
				} else if (do2.getColumnA().substring(2).equals("HAV")) {
					rankCode = "40";
				} else if (do2.getColumnA().substring(2).equals("SUB")) {
					rankCode = "60";
				} else if (do2.getColumnA().substring(2).equals("SUBMAJ")) {
					rankCode = "70";
				}
			}

			if (do2.getColumnA() != null && (do2.getColumnA().equals("NBSUB") || do2.getColumnA().equals("NBRIS")
					|| do2.getColumnA().equals("SPNBSUB") || do2.getColumnA().equals("SPNBRIS"))) {
				rankCode = "50";
			}

			currentDo2RankCode = Integer.parseInt(rankCode);
			if (existingRankCode == 20) {
				if (currentDo2RankCode == 29 || currentDo2RankCode == 30) {
					return true;
				}
			} else if (existingRankCode == 30) {
				if (currentDo2RankCode == 39 || currentDo2RankCode == 40) {
					return true;
				}
			} else if (existingRankCode == 40) {
				if (currentDo2RankCode == 49 || currentDo2RankCode == 50) {
					return true;
				}
			} else if (existingRankCode == 50) {
				if (currentDo2RankCode == 59 || currentDo2RankCode == 60) {
					return true;
				}
			} else if (existingRankCode == 60) {
				if (currentDo2RankCode == 69 || currentDo2RankCode == 70) {
					return true;
				}
			}
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(DUPLICATE_DO2);
			promotValidity = false;
		}

		return (do2List.size() == 0);
	}

	public boolean isValidPayStage(Do2 do2) {

		if (do2.getDescription().equals("PROMOT") && do2.getColumnA() != null && do2.getColumnA().equals("SPHAV")
				&& do2.getColumnC() != null && do2.getColumnC().equals("DIRECT")) {
			if (do2.getEmployee() != null && do2.getEmployee().getEnrolmentDate() != null
					&& do2.getEmployee().getEnrolmentDate().equals(do2.getFromDate())) {
				return true;
			}
		}

		empRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PAYFIX");

		if (Do2Manager.getSubstantiveRankDescription().contains(do2.getColumnA())) {
			List<Do2> do2List = Utils.getPaidActingDo2(do2);
			if (do2List.size() > 0)
				return true;
		}
		if (Do2Manager.getPaidActingDescription().contains(do2.getColumnA())) {
			List<Do2> do2List = Utils.getSubstantiveDo2(do2);
			if (do2List.size() > 0)
				return true;
		}
		GroupCategory gp = Utils.getGroupCategory(do2, "PREV");
		previousPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
				empRank.getId(), gp.getSoldierGroup(), do2.getFromDate());

		/*
		 * Utils.getEntityManager().createQuery(
		 * " SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk AND ps.soldierGroup.soldierGroup=:grp "
		 * + " AND ((:fromDate between ps.fromDate and ps.toDate ) or " +
		 * " (:fromDate >= ps.fromDate and ps.toDate is null )) " +
		 * " AND ps.startScale IS NOT null") .setParameter("rnk", empRank)
		 * .setParameter("grp", gp.getSoldierGroup()) .setParameter("fromDate",
		 * do2.getFromDate()).getResultList();
		 */

		List<AuditCage> atcList = acRepo.findByFromDateLessThan(employee.getId(), do2.getFromDate());

		/*
		 * Utils.getEntityManager().createQuery(
		 * " SELECT atc FROM AuditCage atc WHERE atc.employee=:emp AND atc.cancelled=false AND "
		 * +
		 * " atc.absentFlag=false AND atc.effective=true AND atc.fromDate < :frdt AND "
		 * + " atc.recordStatus!='P' ORDER BY atc.fromDate DESC") .setParameter("emp",
		 * employee) .setParameter("frdt", do2.getFromDate()).getResultList();
		 */
		if (atcList == null || atcList.size() == 0) {
			buffer.append("Base audit cage not found ");
			return false;
		} else {
			for (int i = previousPayList.get(0).getStartScale(); i <= previousPayList.get(0)
					.getScaleEnd0(); i += previousPayList.get(0).getIncr0()) {
				if (atcList.get(0).getBasicPay() == i)
					return true;
			}
		}

		List<Do2> remustDo2List = do2Repo.findByEmployeeIdAndFromDateAndStatusAndDescription(employee.getId(),
				do2.getFromDate(), "V", "REMUST");

		if (remustDo2List != null && remustDo2List.size() > 0) {
			gp = Utils.getGroupCategory(do2, "CURR");
			previousPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(empRank.getId(),
					gp.getSoldierGroup(), do2.getFromDate());

			atcList = null;
			atcList = acRepo
					.findByEmployeeIdAndRecordStatusAndAbsentFlagAndCancelledAndFromDateAndEffectiveOrderByFromDateDesc(
							employee.getId(), "V", false, false, do2.getFromDate(), true);

			if (atcList != null && atcList.size() > 0) {
				for (int i = previousPayList.get(0).getStartScale(); i <= previousPayList.get(0)
						.getScaleEnd0(); i += previousPayList.get(0).getIncr0()) {
					if (atcList.get(0).getBasicPay() == i)
						// System.out.println(".... sending true................");
						return true;
				}
			}
		}
		return false;
	}

	public boolean isValidColumnA(Do2 do2) {
		if (employee.getEnrolmentDate().equals(employee.getSubDate())
				&& ((do2.getColumnA() != null && do2.getColumnA().equals("PASUB")))) {
			return false;
		}
		if (employee.getEnrolmentDate().equals(employee.getSubmajDate())
				&& ((do2.getColumnA() != null && do2.getColumnA().substring(2).equals("PASUBMAJ")))) {
			return false;
		}
		return true;
	}

	/*
	 * public void updateEmployee(Do2 do2) { employee =
	 * Utils.getCurrentEmployee(do2.getArmyNo()); do2List = null;
	 * 
	 * // employee.setIncrementDate(fixDni(do2));
	 * 
	 * if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)){ if
	 * (employee.getEnrolmentDate().equals(employee.getSubDate()) &&
	 * ((do2.getColumnA() != null && do2.getColumnA().equals("SBSUB")))) { return; }
	 * if (employee.getEnrolmentDate().equals(employee.getSubmajDate()) &&
	 * ((do2.getColumnA() != null && do2.getColumnA().equals("SBSUBMAJ")))) {
	 * return; }
	 * 
	 * String columnA = "SB"+ do2.getColumnA().substring(2);
	 * 
	 * if (do2.getColumnA() != null && do2.getColumnA().equals(columnA)) {
	 * 
	 * List<Do2> do2List = getLatenrDo2(do2); if (do2List.size() > 0) { Do2 paDo2 =
	 * do2List.get(0); if (do2List.size() > 0) { return; } } } } if
	 * (do2.getColumnA()!=null && do2.getColumnA().startsWith("NB") &&
	 * do2.getColumnB()!=null && do2.getColumnB().trim().length()==4 &&
	 * do2.getColumnC()!=null) { String trade=do2.getColumnC().trim();
	 * List<GroupCategory> grpCatList =
	 * gcRepo.findByGroupCatAndFromDateLessThanEqual(do2.getColumnB().charAt(3)+"",
	 * do2.getColumnC().trim(),do2.getFromDate());
	 * 
	 * Utils.getEntityManager().createQuery(
	 * "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat AND gc.fromDate<=:frdt ORDER BY gc.fromDate DESC "
	 * ) .setParameter("sgrp", do2.getColumnB().charAt(3)) .setParameter("scat",
	 * do2.getColumnC().trim()) .setParameter("frdt", do2.getFromDate())
	 * .getResultList();
	 * 
	 * if (grpCatList.size() > 0 ) { if (grpCatList.get(0).getRedesignatedAbbr() !=
	 * null){ trade = grpCatList.get(0).getRedesignatedAbbr(); } else if
	 * (grpCatList.get(0).getMergedAbbr() != null) { trade =
	 * grpCatList.get(0).getMergedAbbr(); } else { trade =
	 * grpCatList.get(0).getSoldierCategory(); } }
	 * 
	 * 
	 * if (do2.getAcceptedQe() == null) {
	 * employee.setPreviousGroupCategory(employee.getGroupCategory()); } else if
	 * (grpCatList.get(0).getId() != employee.getGroupCategory().getId()){
	 * employee.setPreviousGroupCategory(employee.getGroupCategory()); }
	 * 
	 * 
	 * 
	 * if (do2.getAcceptedQe() == null) { if
	 * (employee.getPreviousGroupCategory()==null) {
	 * employee.setPreviousGroupCategory(employee.getGroupCategory()); } else { if
	 * (grpCatList.size()>0 && grpCatList.get(0).getId() !=
	 * employee.getPreviousGroupCategory().getId()){
	 * if(!grpCatList.get(0).getSoldierGroup().equals(employee.getGroupCategory().
	 * getSoldierGroup())) {
	 * employee.setPreviousGroupCategory(employee.getGroupCategory()); } } } }
	 * 
	 * 
	 * 
	 * else {
	 * System.out.println("..... promot army no "+employee.getArmyNo()+"  trade "
	 * +trade );
	 * System.out.println("..... emp gp catg "+employee.getGroupCategory().
	 * getCategoryName()+"  prev catg "+employee.getPreviousGroupCategory().
	 * getCategoryName()); if (employee.getPreviousGroupCategory()==null) {
	 * employee.setPreviousGroupCategory(employee.getGroupCategory()); } else { if
	 * (grpCatList.get(0).getId() != employee.getPreviousGroupCategory().getId()){
	 * if(!grpCatList.get(0).getSoldierGroup().equals(employee.getGroupCategory().
	 * getSoldierGroup())) {
	 * employee.setPreviousGroupCategory(employee.getGroupCategory()); } } } }
	 * Commented on 27.08.2021
	 * 
	 * // employee.setGroupCategory((GroupCategory) Utils.getForeignKey(new
	 * GroupCategory(), "soldierCategory", trade ));
	 * 
	 * grpCatList = null; grpCatList =
	 * gcRepo.findByCatAndFromDateLessThanEqual(trade.trim(),do2.getFromDate());
	 * 
	 * Utils.getEntityManager().createQuery(
	 * "SELECT gc FROM GroupCategory gc WHERE gc.soldierCategory=:scat AND gc.fromDate<=:frdt ORDER BY gc.fromDate DESC "
	 * ) .setParameter("scat", trade.trim()) .setParameter("frdt",
	 * do2.getFromDate()) .getResultList();
	 * 
	 * if (grpCatList.size()>0) { employee.setGroupCategory(grpCatList.get(0)); }
	 * 
	 * if (employee.getGroupCategory()!=null &&
	 * employee.getPreviousGroupCategory()!=null &&
	 * !employee.getGroupCategory().equals(employee.getPreviousGroupCategory())) {
	 * if (employee.getInitialMusteringDate()!=null &&
	 * employee.getRemusteringDate()!=null &&
	 * employee.getInitialMusteringDate().before(employee.getRemusteringDate()) &&
	 * employee.getRemusteringDate().before(do2.getFromDate())) { ; } else {
	 * employee.setRemusteringDate(do2.getFromDate()); } } }
	 * 
	 * //String rankName = do2.getColumnA(); String rankCode = null;
	 * 
	 * if (do2.getDescription().equals("MSTHAV")) { rankCode = "40"; }
	 * 
	 * if (do2.getDescription().equals("MUSTER") ||
	 * do2.getDescription().equals("ATTSAM") ||
	 * do2.getDescription().equals("ATTSBM")) { rankCode = "20"; } if
	 * (do2.getColumnA() != null && (!do2.getColumnA().equals("NBSUB") &&
	 * !do2.getColumnA().equals("NBRIS")) && !do2.getDescription().equals("MUSTER")
	 * && !do2.getDescription().equals("ATTSAM") &&
	 * !do2.getDescription().equals("ATTSBM")) { if
	 * (do2.getColumnA().substring(2).equals("NK")){ rankCode = "30"; } else if
	 * (do2.getColumnA().substring(2).equals("HAV")){ rankCode = "40"; } else if
	 * (do2.getColumnA().substring(2).equals("SUB")){ rankCode = "60"; } else if
	 * (do2.getColumnA().substring(2).equals("SUBMAJ")){ rankCode = "70"; } }
	 * 
	 * if (do2.getColumnA() != null && (do2.getColumnA().equals("NBSUB")
	 * ||do2.getColumnA().equals("NBRIS")|| do2.getColumnA().equals("SPNBSUB") ||
	 * do2.getColumnA().equals("SPNBRIS"))) { rankCode="50"; }
	 * 
	 * if (do2.getColumnA() != null && (do2.getColumnA().equals("SPRIS"))) {
	 * rankCode="60"; }
	 * 
	 * rank = rankRepo.findByRankCode(rankCode);
	 * 
	 * 
	 * if (rank!=null) { updateSpecificRankDateInEmployee(employee, do2);
	 * 
	 * if (do2.getDescription().equals("PROMOT")&& (do2.getColumnA() != null &&
	 * (!do2.getColumnA().equals("NBSUB") || !do2.getColumnA().equals("NBRIS") ||
	 * !do2.getColumnA().equals("SPNBRIS") || !do2.getColumnA().equals("SPNBSUB") ||
	 * !do2.getColumnA().equals("SPRIS")))) {
	 * 
	 * String columnA = "PA" + do2.getColumnA().substring(2);
	 * 
	 * if (do2.getColumnA().equals(columnA)) { if (employee.getActingRank() == null)
	 * { employee.setActingRank(rank);
	 * employee.setActingRankDate(do2.getFromDate()); } else if
	 * (employee.getActingRank() != null && employee.getActingRank().getId() <
	 * rank.getId()) { employee.setActingRank(rank);
	 * employee.setActingRankDate(do2.getFromDate()); } } else if
	 * (do2.getColumnA().equals("SB" + columnA.substring(2))) { if
	 * (employee.getActingRank() == null) { employee.setActingRank(rank);
	 * employee.setActingRankDate(do2.getFromDate());
	 * employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); } else if
	 * (employee.getActingRank() != null && employee.getActingRank().getId() <
	 * rank.getId()) { employee.setActingRank(rank);
	 * employee.setActingRankDate(do2.getFromDate());
	 * employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); } else if
	 * (employee.getSubstantiveRank() != null &&
	 * employee.getSubstantiveRank().getId() < rank.getId()) {
	 * employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); } } else { if
	 * (do2.getColumnA().equals("SP" + columnA.substring(2))) { if
	 * (employee.getActingRank() != null &&
	 * rank.getId()==employee.getActingRank().getId() &&
	 * employee.getActingRankDate() != null && do2.getFromDate().equals(
	 * employee.getActingRankDate())) { employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); } else {
	 * employee.setActingRank(rank); employee.setActingRankDate(do2.getFromDate());
	 * employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); } }else if
	 * (do2.getColumnA().equals("GR" + columnA.substring(2))) { if
	 * (employee.getActingRank() != null &&
	 * rank.getId()==employee.getActingRank().getId() &&
	 * employee.getActingRankDate() != null && do2.getFromDate().equals(
	 * employee.getActingRankDate())) { employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); } else {
	 * employee.setActingRank(rank); employee.setActingRankDate(do2.getFromDate());
	 * employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); } } else if
	 * (do2.getColumnA().equals("MSTHAV")) { if (employee.getActingRank() != null &&
	 * rank.getId()==employee.getActingRank().getId() &&
	 * employee.getActingRankDate() != null && do2.getFromDate().equals(
	 * employee.getActingRankDate())) { employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); } else {
	 * employee.setActingRank(rank); employee.setActingRankDate(do2.getFromDate());
	 * employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); } } } } else if
	 * (do2.getDescription().equals("PROMOT")&& (do2.getColumnA() != null &&
	 * (do2.getColumnA().equals("NBSUB") || do2.getColumnA().equals("NBRIS") ||
	 * do2.getColumnA().equals("SPNBRIS") ||do2.getColumnA().equals("SPNBSUB") ||
	 * do2.getColumnA().equals("SPRIS")))){
	 * 
	 * employee.setActingRank(rank); employee.setActingRankDate(do2.getFromDate());
	 * employee.setSubstantiveRank(rank);
	 * employee.setSubstantiveRankDate(do2.getFromDate()); }
	 * employee.setAppointmentDate(null); employee.setAppointmentRate(null);
	 * employee.setTermsColourService(rank.getColorService());
	 * employee.setTermsMaxAgeRetention(rank .getServiceRetention()); }
	 * 
	 * if (rankCode.equals("20") && rank!=null) { rank=null; if
	 * (employee.getAcphavDate() != null &&
	 * employee.getAcphavDate().after(do2.getFromDate())) {
	 * rank=rankRepo.findByRankCode("39");
	 * 
	 * // rankList = Utils.getEntityManager().createQuery( //
	 * "SELECT rnk FROM Rank AS rnk WHERE rnk.rankCode=:rankCode") //
	 * .setParameter("rankCode", "39").getResultList();
	 * 
	 * if (rank!=null) { employee.setActingRank(rank);
	 * employee.setActingRankDate(employee.getAcphavDate()); } } else if
	 * (employee.getAcphavDate() == null && employee.getAcpnaikDate() != null &&
	 * employee.getAcpnaikDate().after(do2.getFromDate())) { rankList =
	 * Utils.getEntityManager().createQuery(
	 * "SELECT rnk FROM Rank AS rnk WHERE rnk.rankCode=:rankCode")
	 * .setParameter("rankCode", "29").getResultList();
	 * rank=rankRepo.findByRankCode("29"); if (rank!=null) {
	 * employee.setActingRank(rank);
	 * employee.setActingRankDate(employee.getAcphavDate()); } } else if
	 * (employee.getAcphavDate() == null && employee.getAcpnaikDate() != null &&
	 * employee.getAcpnaikDate().after(do2.getFromDate())) {
	 * 
	 * rankList = Utils .getEntityManager() .createQuery(
	 * "SELECT rnk FROM Rank AS rnk WHERE rnk.rankName=:rankName")
	 * .setParameter("rankName", "ACPNK").getResultList();
	 * rank=rankRepo.findByRankName("ACPNK"); if (rank!=null) {
	 * employee.setActingRank(rank);
	 * employee.setActingRankDate(employee.getAcpnaikDate()); } } } //Commented on
	 * 30082012 - not to update the values //and to retain the value at the time of
	 * promotion
	 * 
	 * if ((employee.getSubstantiveRank() != null && Do2Manager.JCO_RANKS
	 * .contains(employee.getSubstantiveRank().getRankCode())) ||
	 * (employee.getActingRank() != null && Do2Manager.JCO_RANKS
	 * .contains(employee.getActingRank().getRankCode()))) {
	 * employee.setSoldierClass(null); employee.setCurrentClassPayDate(null);
	 * employee.setCurrentGspayCode(null); employee.setCurrentGsPayDate(null); } }
	 */
	
	public void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		do2List = null;
		 
		// employee.setIncrementDate(fixDni(do2));

		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)){
			if (employee.getEnrolmentDate().equals(employee.getSubDate()) &&
		       ((do2.getColumnA() != null && do2.getColumnA().equals("SBSUB")))) {
				return;
			}
			if (employee.getEnrolmentDate().equals(employee.getSubmajDate()) &&
			   ((do2.getColumnA() != null && do2.getColumnA().equals("SBSUBMAJ")))) {
				return;
			}
			
			String columnA = "SB"+ do2.getColumnA().substring(2);
			
			if (do2.getColumnA() != null && do2.getColumnA().equals(columnA)) {
	
				List<Do2> do2List = getLatenrDo2(do2);
				if (do2List.size() > 0) {
					Do2 paDo2 = do2List.get(0);
					if (do2List.size() > 0) {
						return;
					}
				}
			}
		}
		if (do2.getColumnA()!=null && do2.getColumnA().startsWith("NB") && 
				do2.getColumnB()!=null && do2.getColumnB().trim().length()==4 && do2.getColumnC()!=null) {
            String trade=do2.getColumnC().trim();           
            List<GroupCategory> grpCatList = gcRepo.findByGroupCatAndFromDateLessThanEqual(do2.getColumnB().charAt(3)+"",
            		do2.getColumnC().trim(),do2.getFromDate());
            		
      /*      		Utils.getEntityManager().createQuery( 
            		"SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat AND gc.fromDate<=:frdt ORDER BY gc.fromDate DESC ")
			.setParameter("sgrp", do2.getColumnB().charAt(3))
			.setParameter("scat", do2.getColumnC().trim())
			.setParameter("frdt", do2.getFromDate())
			.getResultList();*/	
            
            if (grpCatList.size() > 0 ) {
    			if (grpCatList.get(0).getRedesignatedAbbr() != null){
    				trade = grpCatList.get(0).getRedesignatedAbbr();
    			} else if (grpCatList.get(0).getMergedAbbr() != null) {
    				trade = grpCatList.get(0).getMergedAbbr();
    			} else {
    				trade = grpCatList.get(0).getSoldierCategory();
    			}
    		}	
			
            /*
			if (do2.getAcceptedQe() == null) {
				employee.setPreviousGroupCategory(employee.getGroupCategory());
            } else if (grpCatList.get(0).getId() != employee.getGroupCategory().getId()){
            	employee.setPreviousGroupCategory(employee.getGroupCategory());
            } */
			


			if (do2.getAcceptedQe() == null) {
				if (employee.getPreviousGroupCategory()==null) {
					employee.setPreviousGroupCategory(employee.getGroupCategory());
				} else {
					if (grpCatList.size()>0 && grpCatList.get(0).getId() != employee.getPreviousGroupCategory().getId()){
		            	if(!grpCatList.get(0).getSoldierGroup().equals(employee.getGroupCategory().getSoldierGroup())) {
		            		employee.setPreviousGroupCategory(employee.getGroupCategory());
		            	}
		             }
				}
			}
			
		
			
			/*else  {
	    	   System.out.println("..... promot army no "+employee.getArmyNo()+"  trade "+trade );
	    	   System.out.println("..... emp gp catg "+employee.getGroupCategory().getCategoryName()+"  prev catg "+employee.getPreviousGroupCategory().getCategoryName());
	    	   if (employee.getPreviousGroupCategory()==null) {
					employee.setPreviousGroupCategory(employee.getGroupCategory());
				} else {
					if (grpCatList.get(0).getId() != employee.getPreviousGroupCategory().getId()){
						if(!grpCatList.get(0).getSoldierGroup().equals(employee.getGroupCategory().getSoldierGroup())) {
		            		employee.setPreviousGroupCategory(employee.getGroupCategory());
		            	}
		             }
				}
	       } Commented on 27.08.2021*/
			
		//	employee.setGroupCategory((GroupCategory) Utils.getForeignKey(new GroupCategory(), "soldierCategory", trade ));
			
			grpCatList = null;
			grpCatList = gcRepo.findByCatAndFromDateLessThanEqual(trade.trim(),do2.getFromDate());
					
	/*				Utils.getEntityManager().createQuery( 
            		"SELECT gc FROM GroupCategory gc WHERE gc.soldierCategory=:scat AND gc.fromDate<=:frdt ORDER BY gc.fromDate DESC ")
		 	.setParameter("scat", trade.trim())
			.setParameter("frdt", do2.getFromDate())
			.getResultList();	*/
			
			if (grpCatList.size()>0) {
				employee.setGroupCategory(grpCatList.get(0));						
			}	
			
			if (employee.getGroupCategory()!=null && employee.getPreviousGroupCategory()!=null 
					&& !employee.getGroupCategory().equals(employee.getPreviousGroupCategory())) {
				if (employee.getInitialMusteringDate()!=null && employee.getRemusteringDate()!=null 
						&& employee.getInitialMusteringDate().before(employee.getRemusteringDate())
						&& employee.getRemusteringDate().before(do2.getFromDate())) {
					;
				} else {
					employee.setRemusteringDate(do2.getFromDate());	
				}		
			}		
		}

			//String rankName = do2.getColumnA();
		String rankCode = null;

		if (do2.getDescription().equals("MSTHAV")) {
			rankCode = "40";
		}

		if (do2.getDescription().equals("MUSTER") || 
			do2.getDescription().equals("ATTSAM") || 
			do2.getDescription().equals("ATTSBM")) {
			rankCode = "20";
		}
		if (do2.getColumnA() != null && 
		   (!do2.getColumnA().equals("NBSUB")      && !do2.getColumnA().equals("NBRIS"))	&& 
		    !do2.getDescription().equals("MUSTER") && !do2.getDescription().equals("ATTSAM") && 
		    !do2.getDescription().equals("ATTSBM")) {
			if (do2.getColumnA().substring(2).equals("NK")){
				rankCode = "30";
			} else if (do2.getColumnA().substring(2).equals("HAV")){
				rankCode = "40";
			} else if (do2.getColumnA().substring(2).equals("SUB")){
				rankCode = "60";
			} else if (do2.getColumnA().substring(2).equals("SUBMAJ")){
				rankCode = "70";
			}
		}
		
		if (do2.getColumnA() != null && (do2.getColumnA().equals("NBSUB") ||do2.getColumnA().equals("NBRIS")||
				do2.getColumnA().equals("SPNBSUB") || do2.getColumnA().equals("SPNBRIS"))) {
			rankCode="50"; 
		}
		
		if (do2.getColumnA() != null && (do2.getColumnA().equals("SPRIS"))) {
			rankCode="60"; 
		}

		rank = rankRepo.findByRankCode(rankCode);
				 

		if (rank!=null) {
			updateSpecificRankDateInEmployee(employee, do2);

            if ("PROMOT".equals(do2.getDescription())
                    && "NBSUB".equals(do2.getColumnA())
                    && employee.getSubstantiveRank() != null
                    && "HAV".equals(employee.getSubstantiveRank().getRankName())) {

                employee.setSubstantiveRank(rank);          
                employee.setSubstantiveRankDate(do2.getFromDate());

                employee.setActingRank(rank);               
                employee.setActingRankDate(do2.getFromDate());
            }

            if ("PROMOT".equals(do2.getDescription())
                    && do2.getColumnA() != null
                    && !do2.getColumnA().equals("NBSUB")
                    && !do2.getColumnA().equals("NBRIS")
                    && !do2.getColumnA().equals("SPNBRIS")
                    && !do2.getColumnA().equals("SPNBSUB")
                    && !do2.getColumnA().equals("SPRIS")) {

				String columnA = "PA" + do2.getColumnA().substring(2);

				if (do2.getColumnA().equals(columnA)) {
					if (employee.getActingRank() == null) {						  
						employee.setActingRank(rank);
						employee.setActingRankDate(do2.getFromDate());
					} else 	if (employee.getActingRank() != null &&
					   employee.getActingRank().getId() < rank.getId()) {
						employee.setActingRank(rank);
						employee.setActingRankDate(do2.getFromDate());
					}
				} else if (do2.getColumnA().equals("SB" + columnA.substring(2))) {					
					if (employee.getActingRank() == null) {						  
						employee.setActingRank(rank);
						employee.setActingRankDate(do2.getFromDate());
						employee.setSubstantiveRank(rank);
						employee.setSubstantiveRankDate(do2.getFromDate());
					} else if (employee.getActingRank() != null && 
							employee.getActingRank().getId() < rank.getId()) {
						employee.setActingRank(rank);
						employee.setActingRankDate(do2.getFromDate());
						employee.setSubstantiveRank(rank);
						employee.setSubstantiveRankDate(do2.getFromDate());
					} else if (employee.getSubstantiveRank() != null && 
							employee.getSubstantiveRank().getId() < rank.getId()) {
						employee.setSubstantiveRank(rank);
						employee.setSubstantiveRankDate(do2.getFromDate());
					}
				} else {
					if (do2.getColumnA().equals("SP" + columnA.substring(2))) {
						if (employee.getActingRank() != null
								&& rank.getId()==employee.getActingRank().getId()
								&& employee.getActingRankDate() != null
								&& do2.getFromDate().equals(
										employee.getActingRankDate())) {
							employee.setSubstantiveRank(rank);
							employee.setSubstantiveRankDate(do2.getFromDate());
						} else {
							employee.setActingRank(rank);
							employee.setActingRankDate(do2.getFromDate());
							employee.setSubstantiveRank(rank);
							employee.setSubstantiveRankDate(do2.getFromDate());
						}
					}else if (do2.getColumnA().equals("GR" + columnA.substring(2))) {
						if (employee.getActingRank() != null
								&& rank.getId()==employee.getActingRank().getId()
								&& employee.getActingRankDate() != null
								&& do2.getFromDate().equals(
										employee.getActingRankDate())) {
							employee.setSubstantiveRank(rank);
							employee.setSubstantiveRankDate(do2.getFromDate());
						} else {
							employee.setActingRank(rank);
							employee.setActingRankDate(do2.getFromDate());
							employee.setSubstantiveRank(rank);
							employee.setSubstantiveRankDate(do2.getFromDate());
						}
					} else if (do2.getColumnA().equals("MSTHAV")) {
						if (employee.getActingRank() != null
								&& rank.getId()==employee.getActingRank().getId()
								&& employee.getActingRankDate() != null
								&& do2.getFromDate().equals(
										employee.getActingRankDate())) {
							employee.setSubstantiveRank(rank);
							employee.setSubstantiveRankDate(do2.getFromDate());
						} else {
							employee.setActingRank(rank);
							employee.setActingRankDate(do2.getFromDate());
							employee.setSubstantiveRank(rank);
							employee.setSubstantiveRankDate(do2.getFromDate());
						}
					}
				}
			} else if ("PROMOT".equals(do2.getDescription())
                    && do2.getColumnA() != null
                    && (do2.getColumnA().equals("NBRIS")
                    || do2.getColumnA().equals("SPNBRIS")
                    || do2.getColumnA().equals("SPNBSUB")
                    || do2.getColumnA().equals("SPRIS"))) {

                employee.setActingRank(rank);
				employee.setActingRankDate(do2.getFromDate());
				employee.setSubstantiveRank(rank);
				employee.setSubstantiveRankDate(do2.getFromDate());
			}
			employee.setAppointmentDate(null);
			employee.setAppointmentRate(null);
			employee.setTermsColourService(rank.getColorService());
			employee.setTermsMaxAgeRetention(rank
					.getServiceRetention());
		}

		if (rankCode.equals("20") && rank!=null) {
			rank=null;
			if (employee.getAcphavDate() != null && employee.getAcphavDate().after(do2.getFromDate())) {
				rank=rankRepo.findByRankCode("39");
				
			//	rankList = Utils.getEntityManager().createQuery(
			//	   "SELECT rnk FROM Rank AS rnk WHERE rnk.rankCode=:rankCode")
			//			.setParameter("rankCode", "39").getResultList();

				if (rank!=null) {
					employee.setActingRank(rank);
					employee.setActingRankDate(employee.getAcphavDate());
				}
			} else if (employee.getAcphavDate() == null
					&& employee.getAcpnaikDate() != null
					&& employee.getAcpnaikDate().after(do2.getFromDate())) {
			/*	rankList = Utils.getEntityManager().createQuery(
					"SELECT rnk FROM Rank AS rnk WHERE rnk.rankCode=:rankCode")
						.setParameter("rankCode", "29").getResultList();*/
				rank=rankRepo.findByRankCode("29");	
				if (rank!=null) {
					employee.setActingRank(rank);
					employee.setActingRankDate(employee.getAcphavDate());
				}
			} else if (employee.getAcphavDate() == null
					&& employee.getAcpnaikDate() != null
					&& employee.getAcpnaikDate().after(do2.getFromDate())) {

			/*	rankList = Utils
						.getEntityManager()
						.createQuery(
								"SELECT rnk FROM Rank AS rnk WHERE rnk.rankName=:rankName")
						.setParameter("rankName", "ACPNK").getResultList();*/
				rank=rankRepo.findByRankName("ACPNK");
				if (rank!=null) {
					employee.setActingRank(rank);
					employee.setActingRankDate(employee.getAcpnaikDate());
				}
			}
		}
		//Commented on 30082012 - not to update the values
		//and to retain the value at the time of promotion

		/*if ((employee.getSubstantiveRank() != null && Do2Manager.JCO_RANKS
				.contains(employee.getSubstantiveRank().getRankCode()))
				|| (employee.getActingRank() != null && Do2Manager.JCO_RANKS
						.contains(employee.getActingRank().getRankCode()))) {
			employee.setSoldierClass(null);
			employee.setCurrentClassPayDate(null);
			employee.setCurrentGspayCode(null);
			employee.setCurrentGsPayDate(null);
		}*/
	}
	public void updateSpecificRankDateInEmployee(Employee employee, Do2 do2) {

		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PROMOT");

		int existingRank = Integer.parseInt(rank.getRankCode());
		int currentRank = 0;

		if (do2.getColumnA().equals("NBSUB")) {
			currentRank = 50;
		} else if (do2.getColumnA().substring(2).equals("NK")) {
			currentRank = 30;
		} else if (do2.getColumnA().substring(2).equals("HAV")) {
			currentRank = 40;
		} else if (do2.getColumnA().substring(2).equals("SUB")) {
			currentRank = 60;
		} else if (do2.getColumnA().substring(2).equals("SUBMAJ")) {
			currentRank = 70;
		}
		if (do2.getColumnA() != null) {
			if (existingRank < currentRank) {
				if (((do2.getColumnA().equals("TSNK")) || (do2.getColumnA().equals("TSLD")))) {
					employee.setTsnkDate(do2.getFromDate());
					return;
				} else if (((do2.getColumnA().substring(2).equals("NK"))
						|| (do2.getColumnA().substring(2).equals("LD"))))
					employee.setNaikDate(do2.getFromDate());
				else if (((do2.getColumnA().equals("GRHAV")) || (do2.getColumnA().equals("GRDFR")))) {
					employee.setHavDate(do2.getFromDate());
					employee.setGraduateHavildarDate(do2.getFromDate());
					employee.setGraduateHavildarFlag(true);
				} else if (((do2.getColumnA().equals("SPHAV")) || (do2.getColumnA().equals("SPDFR")))) {
					employee.setHavDate(do2.getFromDate());
					employee.setSportHavildarDate(do2.getFromDate());
					employee.setSportHavildarFlag(true);
				} else if (((do2.getColumnA().substring(2).equals("HAV"))
						|| (do2.getColumnA().substring(2).equals("DFR")))) {
					employee.setHavDate(do2.getFromDate());

				} else if (do2.getColumnA().equals("NBSUB") || (do2.getColumnA().equals("NBRIS")
						|| (do2.getColumnA().equals("SPNBRIS") || (do2.getColumnA().equals("SPNBSUB"))))) {
					employee.setNbsubDate(do2.getFromDate());

				} else if (((do2.getColumnA().substring(2).equals("SUB"))
						|| (do2.getColumnA().substring(2).equals("RIS")))) {
					employee.setSubDate(do2.getFromDate());

				} else if (((do2.getColumnA().substring(2).equals("SUBMAJ"))
						|| (do2.getColumnA().substring(2).equals("RISMAJ")))) {
					employee.setSubmajDate(do2.getFromDate());
				}

			}
		}
	}

	public void createSysDo2(Employee employee, Do2 do2) {
		// if the new do2 is substantivepromotion ,if paid acting is available
		// profix
		// will not be generated added on 16042012
		if (Do2Manager.getSubstantiveRankDescription().contains(do2.getColumnA())) {
			List<Do2> do2List = Utils.getPaidActingDo2(do2);
			if (do2List.size() > 0) {
				return;
			}
		}

		List<Do2> ranksDo2List = do2Repo.ranksDo2List(do2.getFromDate(), do2.getEmployee().getId());
		if (ranksDo2List.size() > 0) {
			Date rankFromDate = ranksDo2List.get(0).getFromDate();
			List<Do2> promotDo2List = do2Repo.getPromotDo2List(do2.getColumnA(), do2.getEmployee().getId(),
					rankFromDate);
			if (promotDo2List.size() > 0) {
				return;
			}
		}

		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (employee.getEnrolmentDate().equals(employee.getSubDate())
					&& ((do2.getColumnA() != null && do2.getColumnA().equals("SBSUB")))) {
				return;
			}
			if (employee.getEnrolmentDate().equals(employee.getSubmajDate())
					&& ((do2.getColumnA() != null && do2.getColumnA().equals("SBSUBMAJ")))) {
				return;
			}
			String columnA = "SB" + do2.getColumnA().substring(2);

			if (do2.getColumnA() != null && do2.getColumnA().equals(columnA)) {

				List<Do2> do2List = getLatenrDo2(do2);
				if (do2List.size() > 0) {
					Do2 paDo2 = do2List.get(0);
					if (do2List.size() > 0) {
						return;
					}
				}
			}
		}
		if (!(do2.getFromDate().before(DateManager.getSixthCpcDate()))) {
			Date xDate = do2.getFromDate();
			Calendar yy = new GregorianCalendar();
			yy.setTimeInMillis(xDate.getTime());
			int zz = yy.get(Calendar.YEAR);

			Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
			Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);

			Date jan01 = chkDate1.getTime();
			Date jul01 = chkDate2.getTime();
			if ((xDate.after(jan01)) && (xDate.before(jul01)) && !Utils.isAcpBeforeCurrentDo2(do2)) {
				createProfixDo2(employee, do2);
			}
		}
	}

	public void reverseUpdateSpecificRankDateInEmployee(Employee employee, String columnA) {
		if (columnA != null) {

			if (((columnA.equals("TSNK")) || (columnA.equals("TSLD")))) {
				employee.setTsnkDate(null);
				return;
			} else if (((columnA.substring(2).equals("NK")) || (columnA.substring(2).equals("LD"))))
				employee.setNaikDate(null);

			else if (((columnA.substring(2).equals("HAV")) || (columnA.substring(2).equals("DFR"))))
				employee.setHavDate(null);

			else if (columnA.equals("NBSUB")
					|| (columnA.equals("NBRIS") || (columnA.equals("SPNBSUB") || (columnA.equals("SPNBRIS")))))
				employee.setNbsubDate(null);

			else if (((columnA.substring(2).equals("SUB")) || (columnA.substring(2).equals("RIS"))))
				employee.setSubDate(null);

			else if (((columnA.substring(2).equals("SUBMAJ")) || (columnA.substring(2).equals("RISMAJ"))))
				employee.setSubmajDate(null);

			else if (((columnA.equals("GRHAV")) || (columnA.equals("GRDFR")))) {
				employee.setHavDate(null);
				employee.setGraduateHavildarDate(null);
				employee.setGraduateHavildarFlag(false);
			}

			else if (((columnA.equals("SPHAV")) || (columnA.equals("SPDFR")))) {
				employee.setHavDate(null);
				employee.setSportHavildarDate(null);
				employee.setSportHavildarFlag(false);
			}

		}

	}

	public String reverseUpdateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		do2List = null;
		String columnA = do2.getColumnA();
		boolean employeeUpdated = false;
		reverseUpdateSpecificRankDateInEmployee(employee, columnA);

		esdPromotList = esdRepo.findByFromDateLessThanAndDescriptionNotInOtherDescriptions(employee.getId(),
				Do2Manager.ESD_PROMOT + "", do2.getFromDate(), Do2Manager.REMUSTER, Do2Manager.ACP, Do2Manager.MACPS);

		/*
		 * Utils .getEntityManager() .createQuery(
		 * "SELECT epl FROM EmployeeServiceDetail epl WHERE " +
		 * " epl.employee.id=:empId AND epl.status=:stat AND epl.fromDate<:fromDate AND "
		 * + " epl.do2.description !=:desc1 AND epl.do2.description !=:desc2 and " +
		 * " epl.do2.description !=:desc3 ORDER BY epl.fromDate DESC")
		 * .setParameter("empId", employee.getId()).setParameter("stat",
		 * Do2Manager.ESD_PROMOT).setParameter("desc1",
		 * Do2Manager.REMUSTER).setParameter("desc2",
		 * Do2Manager.ACP).setParameter("desc3", Do2Manager.MACPS)
		 * .setParameter("fromDate", do2.getFromDate()).getResultList();
		 */
		if (esdPromotList.size() > 0) {
			updateEmployee(esdPromotList.get(0).getDo2());
		}

		// reverse appointment pay code
		List<EmployeeServiceDetail> esdAptList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(
				Utils.getCurrentEmployee(do2.getArmyNo()).getId(), Do2Manager.ESD_APPT + "");

		/*
		 * Utils .getEntityManager() .createQuery(
		 * "SELECT ac FROM EmployeeServiceDetail ac WHERE ac.employee=:emp AND ac.status=:stat ORDER BY ac.fromDate DESC"
		 * ) .setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
		 * .setParameter("stat", Do2Manager.ESD_APPT).getResultList();
		 */
		if (esdPromotList.size() > 0 && esdAptList.size() > 0) {

			if (esdAptList.get(0).getFromDate().after(esdPromotList.get(0).getFromDate())) {
				employee.setAppointmentRate(getAppointmentRate(esdAptList.get(0).getDo2()));
				employee.setAppointmentDate(esdAptList.get(0).getDo2().getFromDate());
			} else {
				employee.setAppointmentRate(null);
				employee.setAppointmentDate(null);
			}
		} else {
			employee.setAppointmentRate(null);
			employee.setAppointmentDate(null);
		}
		if (do2.getDescription().equals("PROMOT") && (do2.getColumnA().equals("NBSUB")
				|| do2.getColumnA().equals("NBRIS") || (columnA.equals("SPNBSUB") || (columnA.equals("SPNBRIS"))))) {

			// reverse class pay code
			List<EmployeeServiceDetail> esdClassList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(
					Utils.getCurrentEmployee(do2.getArmyNo()).getId(), Do2Manager.ESD_CLASS_PAY + "");

			/*
			 * Utils .getEntityManager() .createQuery(
			 * "SELECT cr FROM EmployeeServiceDetail cr WHERE " +
			 * " cr.employee=:emp AND cr.status=:stat order by cr.fromDate desc")
			 * .setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
			 * .setParameter("stat", Do2Manager.ESD_CLASS_PAY) .getResultList();
			 */

			if (esdClassList.size() > 0) {
				if (!(esdClassList.get(0).getDo2().getColumnB().equals(null)))
					employee.setSoldierClass(esdClassList.get(0).getDo2().getColumnB().charAt(3) + "");
				employee.setCurrentClassPayDate(esdClassList.get(0).getFromDate());
				employee.setMusterJcoFlag(false);
				employee.setMusterJcoDate(null);
			}
			// reverse Gspay code
			List<EmployeeServiceDetail> esdGspayList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(
					Utils.getCurrentEmployee(do2.getArmyNo()).getId(), Do2Manager.ESD_GSPAY + "");

			/*
			 * Utils .getEntityManager() .createQuery(
			 * "SELECT gsp FROM EmployeeServiceDetail AS gsp " +
			 * " WHERE gsp.employee=:emp AND gsp.status=:stat order by gsp.fromDate desc")
			 * .setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
			 * .setParameter("stat", Do2Manager.ESD_GSPAY).getResultList();
			 */
			if (esdGspayList.size() > 0) {
				employee.setCurrentGsPayDate(esdGspayList.get(0).getDo2().getFromDate());
				if (esdGspayList.get(0).getDo2().getColumnA().substring(2).equals("FIRST")) {
					employee.setCurrentGspayCode("1");
				} else if (esdGspayList.get(0).getDo2().getColumnA().substring(2).equals("SECOND")) {
					employee.setCurrentGspayCode("2");
				} else if (esdGspayList.get(0).getDo2().getColumnA().substring(2).equals("THIRD")) {
					employee.setCurrentGspayCode("3");
				}
			}

		}
		return Do2Manager.SUCCESS;
	}

	private AppointmentRate getAppointmentRate(Do2 do2) { // Appointment Do2 is
		// received
		Pao pao = null;
		AppointmentRate appointmentRate = null;
		List<AppointmentRate> appointmentRateList = null;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_NASIK)
				&& do2.getColumnA().equals(Do2Manager.PAO_NASIK_APPOINMENT)) {
			pao = Utils.getPao();
		} else {
			pao = null;
		}
		/*
		 * if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_NASIK) &&
		 * do2.getColumnA().equals(Do2Manager.PAO_NASIK_APPOINMENT)) { pao =
		 * Utils.getPao(); } else { pao = null; }
		 */

		if (pao == null) {
			appointmentRateList = apptRateRepo.findByApptNameFromDateBetweenOrFromDateGreater(do2.getColumnA(),
					do2.getFromDate());

			/*
			 * Utils .getEntityManager() .createQuery(
			 * "SELECT appt FROM AppointmentRate appt WHERE appt.appointmentName = :apptName and pao is null and "
			 * +
			 * " ((:fromDate between appt.fromDate and appt.toDate) or (:fromDate > appt.fromDate and appt.toDate is null))"
			 * ) .setParameter("apptName", do2.getColumnA()).setParameter( "fromDate",
			 * do2.getFromDate()).getResultList();
			 */
		} else {
			appointmentRateList = apptRateRepo.findByApptNamePaoIdFromDateBetweenOrFromDateGreater(do2.getColumnA(),
					pao.getId(), do2.getFromDate());

			/*
			 * Utils .getEntityManager() .createQuery(
			 * "SELECT appt FROM AppointmentRate appt WHERE appt.appointmentName = :apptName and pao=:pao and "
			 * +
			 * " ((:fromDate between appt.fromDate and appt.toDate) or (:fromDate > appt.fromDate and appt.toDate is null))"
			 * ) .setParameter("apptName", do2.getColumnA()).setParameter( "fromDate",
			 * do2.getFromDate()).setParameter("pao", pao).getResultList();
			 */
		}

		if (appointmentRateList.size() > 0) {
			appointmentRate = appointmentRateList.get(0);
		}

		return appointmentRate;
	}

	@Override
	public void updateBdtInfo(Do2 do2) {
		updateEmployee(do2);
	}

	// new method to update esd before updating old basicpay (call this method
	// from new auditcage before oldbasic pay update)
	public void updateEsd(Do2 do2) {
		String columnA = "PA" + do2.getColumnA().substring(2);
		if (do2.getColumnA().equals(columnA)) {
			List<Do2> do2List = Utils.getSubstantiveDo2(do2);
			if (do2List.size() > 0) {
				AuditCageHelper.reverseUpdateESDForPromotCancel(do2List.get(0), Do2Manager.ESD_PROMOT);
				AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT + "");
			} else {
				AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT + "");
			}
		} else {
			List<Do2> do2List = Utils.getPaidActingDo2(do2);
			if (do2List.size() == 0) {

				AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT + "");
			} else {// CODE ADDED TO TAKE CARE OFF LORANK
				/*
				 * Rank rank = Utils.getRankForPromot(do2.getArmyNo(), do2 .getFromDate());
				 */
				Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PROMOT");
				int existingRank = Integer.parseInt(rank.getRankCode());
				int currentRank = 0;
				if (do2.getColumnA().equals("NBSUB")) {
					currentRank = 50;
				} else if (do2.getColumnA().substring(2).equals("NK")) {
					currentRank = 30;
				} else if (do2.getColumnA().substring(2).equals("HAV")) {
					currentRank = 40;
				} else if (do2.getColumnA().substring(2).equals("SUB")) {
					currentRank = 60;
				} else if (do2.getColumnA().substring(2).equals("SUBMAJ")) {
					currentRank = 70;
				}
				if (existingRank < currentRank) {
					AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT + "");
				}
			}

		}
	}

	public Date sixcpcDo2Exists(Do2 do2) {
		List<Do2> do2List = do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(do2.getEmployee().getId(), "SIXCPC",
				Do2Manager.VALID);

		/*
		 * Utils.getEntityManager().
		 * createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status=:status and d.description=:description"
		 * ) .setParameter("emp",do2.getEmployee())
		 * .setParameter("description","SIXCPC")
		 * .setParameter("status",Do2Manager.VALID) .getResultList();
		 */

		if (do2List.size() > 0) {
			return do2List.get(0).getFromDate();
		}
		return null;
	}

	public List<Do2> getLatenrDo2(Do2 do2) {
		String description = "PA" + do2.getColumnA().substring(2);

		/*
		 * List<Do2> do2List = Utils .getEntityManager() .createQuery(
		 * "SELECT d FROM Do2 d WHERE d.employee=:emp AND d.description=:latenr AND " +
		 * " (d.columnA =:columna) AND d.status=:status order by d.fromDate desc")
		 * .setParameter("emp",Utils.getCurrentEmployee(do2.getArmyNo())).setParameter(
		 * "latenr", "LATENR").setParameter("columna", description)
		 * .setParameter("status", Do2Manager.VALID).getResultList();
		 */

		List<Do2> do2List = do2Repo.findByEitherDescriptionColumnAStatusOrderFromDateDesc(
				Utils.getCurrentEmployee(do2.getArmyNo()).getId(), "LATENR", "PROMOT", description, Do2Manager.VALID);

		/*
		 * Utils .getEntityManager() .createQuery(
		 * "SELECT d FROM Do2 d WHERE d.employee=:emp AND (d.description=:latenr OR d.description=:promot) AND "
		 * + " (d.columnA =:columna) AND d.status=:status order by d.fromDate desc")
		 * .setParameter("emp",Utils.getCurrentEmployee(do2.getArmyNo()))
		 * .setParameter("latenr", "LATENR") .setParameter("promot", "PROMOT")
		 * .setParameter("columna", description) .setParameter("status",
		 * Do2Manager.VALID).getResultList();
		 */

		return do2List;

	}

	public List<Do2> getRevertDo2(Do2 do2, Date paDo2Date) {
		String description = "PA" + do2.getColumnA().substring(2);

		List<String> descList = Arrays.asList(new String[] { "RANKS", "REVERT" });
		List<Do2> revertDo2List = do2Repo.findByDescListAndFromDateGreater(
				Utils.getCurrentEmployee(do2.getArmyNo()).getId(), Do2Manager.VALID, descList, paDo2Date);

		/*
		 * Utils .getEntityManager() .createQuery(
		 * "SELECT d FROM Do2 d WHERE d.employee=:emp AND d.description in ('RANKS', 'REVERT') AND "
		 * + "  d.status=:status and d.fromDate>:paDo2Date order by d.fromDate desc")
		 * .setParameter("emp",Utils.getCurrentEmployee(do2.getArmyNo()))
		 * .setParameter("paDo2Date", paDo2Date) .setParameter("status",
		 * Do2Manager.VALID).getResultList();
		 */

		return revertDo2List;
	}

	public boolean isValidPayLevel(Do2 do2) {

		String atcgPayLevel = "";
		String currentPayLevel = "";
		int currentBasicPay = 0;

		empRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PAYFIX");
		currentPayLevel = Do2Manager.getPayLevelPromot(empRank.getRankCode());

		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atcList = null;

		if (do2.getFromDate().equals(emp.getEnrolmentDate()) && do2.getColumnA().equals("SPHAV")
				&& do2.getColumnC().equals("DIRECT")) {
			atcList = acRepo.findByFromDateLessThanEqualOrderByFromDateDesc(employee.getId(), do2.getFromDate());

			/*
			 * Utils.getEntityManager().createQuery(
			 * " SELECT atc FROM AuditCage atc WHERE atc.employee=:emp AND atc.cancelled=false AND "
			 * +
			 * " atc.absentFlag=false AND atc.effective=true AND atc.fromDate <= :frdt AND "
			 * + " atc.recordStatus!='P' ORDER BY atc.fromDate DESC, atc.toDate DESC ")
			 * .setParameter("emp", employee) .setParameter("frdt",
			 * do2.getFromDate()).getResultList();
			 */
		} else {
			atcList = acRepo.findByFromDateLessThanAndAbsentFlagAndDeputationFlag(employee.getId(), do2.getFromDate());

			/*
			 * Utils.getEntityManager().createQuery(
			 * " SELECT atc FROM AuditCage atc WHERE atc.employee=:emp AND atc.cancelled=false AND "
			 * +
			 * " (atc.absentFlag=false OR (atc.absentFlag=true AND atc.deputationFlag=true)) "
			 * + " AND atc.effective=true AND atc.fromDate < :frdt AND " +
			 * " atc.recordStatus!='P' ORDER BY atc.fromDate DESC, atc.toDate DESC ")
			 * .setParameter("emp", employee) .setParameter("frdt",
			 * do2.getFromDate()).getResultList();
			 */
		}

		if (atcList == null || atcList.size() == 0) {
			buffer.append("Base audit cage not found ");
			return false;
		} else {
			atcgPayLevel = atcList.get(0).getPayLevel();
			currentBasicPay = atcList.get(0).getBasicPay();
			// System.out.println ( " ATCG PAY LEVEL " +atcgPayLevel +" current Basic Pay "
			// +currentBasicPay +" current pay level "+currentPayLevel);
			if (!atcgPayLevel.equals("0")) {
				if (atcgPayLevel.equals(currentPayLevel)) {
					List<PayMatrix> paymatrixList = matrixRepo
							.findByPayLevelAndBasicPayOrderByBasicPayAsc(currentPayLevel, currentBasicPay);

					/*
					 * Utils.getEntityManager().createQuery(
					 * " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND " +
					 * "  pm.basicPay=:sevencpcpay order by basicPay asc ")
					 * .setParameter("paylevel", currentPayLevel) .setParameter("sevencpcpay",
					 * currentBasicPay) .getResultList();
					 */

					if (paymatrixList.size() == 0) {
						return false;
					} else {
						return true;
					}
				} else if (Integer.parseInt(atcgPayLevel) > Integer.parseInt(currentPayLevel)) {
					List<PayMatrix> paymatrixList = matrixRepo.findByPayLevelAndBasicPayOrderByBasicPayAsc(atcgPayLevel,
							currentBasicPay);

					/*
					 * Utils.getEntityManager().createQuery(
					 * " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND " +
					 * "  pm.basicPay=:sevencpcpay order by basicPay asc ")
					 * .setParameter("paylevel", atcgPayLevel) .setParameter("sevencpcpay",
					 * currentBasicPay) .getResultList();
					 */
					if (paymatrixList.size() == 0) {
						return false;
					} else {
						return true;
					}
				} else if (atcgPayLevel.equals("9") && currentPayLevel.equals("10")) {
					List<PayMatrix> paymatrixList = matrixRepo.findByPayLevelAndBasicPayOrderByBasicPayAsc(atcgPayLevel,
							currentBasicPay);

					/*
					 * Utils.getEntityManager().createQuery(
					 * " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND " +
					 * "  pm.basicPay=:sevencpcpay order by basicPay asc ")
					 * .setParameter("paylevel", atcgPayLevel) .setParameter("sevencpcpay",
					 * currentBasicPay) .getResultList();
					 */
					if (paymatrixList.size() == 0) {
						return false;
					} else {
						return true;
					}
				} else {
					return false;
				}
			} else {
				return true;
			}
		}
	}

	public boolean doesIncrExistsOnOptionDate(Do2 do2, Employee employee) {
		String incrdesc = "INCR";
		if (employee.getRegularTaFlag().equals("T")) {
			incrdesc = "TAINCR";
		}
	//	List<Do2> do2List = do2Repo.findByDescFromDateEqualStatusEqual(employee.getId(), "V", incrdesc,
	//			do2.getToDate());
		

	    List<String> descriptions;
	    if (employee.getRegularTaFlag().equals("T")) {
	        descriptions = Arrays.asList("TAINCR", "STEPUP");
	    } else {
	        descriptions = Arrays.asList("INCR", "STEPUP");
	    }

	    List<Do2> do2List = do2Repo.findByDescFromDateLessThanEqualDesc(employee.getId(), "V", descriptions, do2.getFromDate());

		/*
		 * Utils.getEntityManager().
		 * createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' " +
		 * " and d.employee=:emp and d.status='V' and d.fromDate=:toDate ")
		 * .setParameter("emp", employee).setParameter("toDate", do2.getToDate())
		 * .getResultList();
		 */

		System.out.println("...........do2list size " + do2List.size());

		if (do2List==null || (do2List!=null && do2List.size()==0)) {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR) && do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				return true;
			}
		}
		
		if (do2List != null && do2List.size() > 0) {
			return true;
		} else {
			do2List = null;
			do2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(employee.getId(), "V", incrdesc,
					do2.getToDate());

			/*
			 * Utils.getEntityManager().
			 * createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' " +
			 * " and d.employee=:emp and d.status='V' and d.fromDate<:toDate order by d.fromDate desc "
			 * ) .setParameter("emp", employee).setParameter("toDate", do2.getToDate())
			 * .getResultList();
			 */

			System.out.println("................ lessthan incr date .......do2list size  " + do2List.size());

			if (do2List != null && do2List.size() > 0) {
				List<Do2> acpfixprofixList = do2Repo.getIncrDateAcpfixProfixDo2List(employee.getId(),
						do2List.get(0).getFromDate());
				if (acpfixprofixList != null && acpfixprofixList.size() > 0) {
					Calendar incrDate = new GregorianCalendar();
					incrDate.setTime(do2List.get(0).getFromDate());
					int year = incrDate.get(Calendar.YEAR);
					int month = incrDate.get(Calendar.MONTH);
					if (month == Calendar.JULY) {
						year++;
						month = Calendar.JANUARY;
					} else {
						month = Calendar.JULY;
					}
					Date dni = null;
					try {
						dni = DateManager.getDate(year, month + 1, 1);
					} catch (Exception e) {
						Utils.handleException(e);
					}
					/*
					 * System.out.
					 * println("..........dni ..... do2 todate from promot...........java ........................ "
					 * +dni+"  "+do2.getToDate());
					 * 
					 * if (dni!=null && do2.getToDate()!=null && dni.equals(do2.getToDate())) {
					 * System.out.println("..............returning true .............."); } else {
					 * System.out.println("..............returning false.............."); }
					 */
					return (dni != null && do2.getToDate() != null && dni.equals(do2.getToDate()));
				}

				Calendar nextIncrDate = new GregorianCalendar();
				nextIncrDate.setTime(do2List.get(0).getFromDate());
				int year = nextIncrDate.get(Calendar.YEAR);
				year++;
				try {
					nextIncrDate.setTime(DateManager.getDate(year, nextIncrDate.get(Calendar.MONTH) + 1,
							nextIncrDate.get(Calendar.DATE)));
				} catch (Exception e) {
					Utils.handleException(e);
				}
				if (do2.getToDate().equals(nextIncrDate.getTime())) {
					return true;
				} else {

					List<String> descList = Arrays
							.asList(new String[] { "PROMOT", "MACPS", "MSTHAV", "HCOMMN", "MSTJCO" });
					List<Do2> macpsPromotDo2List = do2Repo.findByDescListAndFromDateBetween(do2.getEmployee().getId(),
							descList, do2List.get(0).getFromDate(), nextIncrDate.getTime());

					/*
					 * Utils.getEntityManager().
					 * createQuery("select d from Do2 d where d.status='V' and d.employee=:emp " +
					 * " and d.description in ('PROMOT', 'MACPS', 'MSTHAV', 'HCOMMN', 'MSTJCO') and d.fromDate between :adjustedincrdt "
					 * + " and :subsequentincrdt order by d.fromDate ").setParameter("emp",
					 * do2.getEmployee()).setParameter("adjustedincrdt",
					 * do2List.get(0).getFromDate()) .setParameter("subsequentincrdt",
					 * nextIncrDate.getTime()).getResultList();
					 */

					if (macpsPromotDo2List != null && macpsPromotDo2List.size() > 0) {
						for (Do2 d : macpsPromotDo2List) {
							if (d.getDescription().equals("MACPS")) {
								return (Utils.getSevenPcNextIncrementDate(d.getFromDate()).equals(do2.getToDate()));
							} else {
								if (Utils.isAcpBeforeCurrentDo2(d)) {
									continue;
								} else {
									return (Utils.getSevenPcNextIncrementDate(d.getFromDate()).equals(do2.getToDate()));
								}
							}
						}
					}
					return false;
				}
			} else {
				// System.out.println("3333333333333");
				if (employee != null && employee.getIncrementDate() != null) {
					// System.out.println("33333333333334444444444444444444");
					return (employee.getIncrementDate().equals(do2.getToDate()));
				} else {
					// System.out.println("33333333333335555555555555555555");
					return false;
				}
			}
		}
	}
}
