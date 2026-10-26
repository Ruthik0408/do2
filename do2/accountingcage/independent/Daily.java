package itsdc.dolphin.do2.accountingcage.independent;

 import java.util.List;
import java.util.Date;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.InvalidDateRangeException;
import itsdc.dolphin.utils.Utils;
 
@RestController
public class Daily extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	public Date sra_frdt = new Date();
	public Date sra_todt = new Date();
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public final String INVALID_COLUMN7A = "VL:Arrival time not mentioned in Col.7A.\n ";
	static public final String INVALID_COL7A = "VL:Days & Hrs not mentioned in Col.7A.\n ";
	static public final String INVALID_COLUMN7B = "VL:Departure time not mentioned in Col.7B.\n ";
	static public final String INVALID_COL7B = "VL:Col 7B should be left blank\n ";
	static public final String INVALID_TIME = "VL:Invalid Departure/Arrival time in Column.7A/7B.\n ";
	static public final String INVALID_TYOJ = "AU:Corresponding TYOJ not available.\n";
	static public final String INVALID_LRA = "VL:LRA days are not matching with the period and time\n";
	static public final String INVALID_PERIOD = "VL:Daily period is more than 180 days.\n";
	static public final String RATE_NOT_AVAILABLE = "AU:Rate not matching with the allowance rate w.r.to City/Basic.\n ";
	static public final String TJRA_REQ = "AU:No of Drill Days required in TJRA.\n ";
	static public final String TJRA_DAYS = "AU:No of Drill Days should be either 8, 15 or 22 days.\n ";
	static public final String NOT_IN_FS_LIST = "AU:This Army No is Not a FS Case/Fs Case Entry Not approved. \n.";
	public double totalDaily, dailyAmount, allowRateamount, arrivalDayAmount,
			departureDayAmount, arrivalHrs, departureHrs, arrdepHrs, do2Days,
			totLraDays, dailyDays = 0.0;
	private int do2LraDays = 0;
	private boolean newFormat = true;
	private double dayMinutes = 24 * 60;
	private Integer basicPay;

	private StringBuffer buffer = new StringBuffer();

	@Override
	public String process(Do2 do2) {
		if (isValid(do2)) {
			// ***lines added for calculating DA for FS
			if (Utils.isEmpThisMonthFs(do2.getEmployee()) && do2.getDescription().equals("FDAILY")) {
				getLraDays(do2);
				calCulateDaForFs(do2, totLraDays);
				return Do2Manager.SUCCESS;
			}
			double sraAmount = 0;
			getLraDays(do2);
			double daRate = pickCorrectRate(do2);
			dailyAmount = totLraDays * daRate;
			totalDaily = Math.round(dailyAmount);
			Utils.addArrears(do2, totalDaily);		

			if (do2.getLra() > 0 && !sra_todt.before(sra_frdt)) {
				List<Allowance> allowances = Utils.getApplicableAllowanceRates(
						"SRA", sra_frdt, sra_todt);
				if (allowances.size() > 0) {
					for (Allowance allowance : allowances) {
						sraAmount += (allowance.getRateAmount() * DateManager
								.getNoOfDays(allowance.getFromDate(),
										allowance.getToDate()));

					}
					Utils.addArrears(do2, Math.round(sraAmount), "SRA",	sra_frdt, sra_todt);
				}
			}
		}
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public boolean isValid(Do2 do2) {

		boolean validity = true;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		// ***lines added for checking the description for amryno exists in FS
		if (do2.getDescription().equals("FDAILY")) {
			if (!Utils.isEmpThisMonthFs(employee)) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(NOT_IN_FS_LIST);
				do2Repo.save(do2);
				return false;
			}
		}

		// ***lines added for checking drill days for fs case
		if (do2.getDescription().equals("FDAILY")
				&& Utils.isEmpThisMonthFs(employee)) {
			if (do2.getTjra() == 0 || do2.getTjra() == null) {
				validity = false;
				buffer.append(TJRA_REQ);
			}
		}
		if (do2.getDescription().equals("FDAILY")
				&& Utils.isEmpThisMonthFs(employee)) {
			if(Utils.getPao().getPaoCode().equals("94")){
				if(do2.getTjra() < 5 && do2.getTjra() > 22){
					validity = false;
					buffer.append(TJRA_DAYS);
				}
			}else{
				if (do2.getTjra() == 8 || do2.getTjra() == 15
						|| do2.getTjra() == 22) {
					;
				} else {
					validity = false;
					buffer.append(TJRA_DAYS);
				}
			}
		}

		if (do2.getLra() != null)
			do2LraDays = do2.getLra();
		String columnA = do2.getColumnA();
		String columnB = do2.getColumnB();
		if (do2.getLra() == null) {
			do2.setLra(0);
		}

		if (columnA != null) {
			int i = do2.getColumnA().indexOf("H");
			if (i > 0) {
				do2.setColumnA(columnA.substring(0, columnA.lastIndexOf("H")));
			} else {
				i = do2.getColumnA().indexOf("h");
				if (i > 0)
					do2.setColumnA(columnA.substring(0,
							columnA.lastIndexOf("h")));
			}
		}
		if (columnB != null) {
			int i = do2.getColumnB().indexOf("H");
			if (i > 0) {
				do2.setColumnB(columnB.substring(0, columnB.lastIndexOf("H")));
			} else {
				i = do2.getColumnB().indexOf("h");
				if (i > 0)
					do2.setColumnB(columnB.substring(0,
							columnB.lastIndexOf("h")));
			}
		}

		if (!isValid7a(do2)) {
			validity = false;
		//	do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			if (newFormat)
				buffer.append(INVALID_COLUMN7A + NOT_IN_APPENDIX_J_FORMAT);
			else
				buffer.append(INVALID_COL7A + NOT_IN_APPENDIX_J_FORMAT);
		}
		if (!isValid7b(do2)) {
			validity = false;
		//	do2.setStatus("MR");
			do2.setStatus(Do2Manager.INVALID);
			if (newFormat)
				buffer.append(INVALID_COLUMN7B + NOT_IN_APPENDIX_J_FORMAT);
			else
				buffer.append(INVALID_COL7B + NOT_IN_APPENDIX_J_FORMAT);
		}
		if (!isValidDaily(do2)) {
			validity = false;
			buffer.append(INVALID_PERIOD);
		}
		/*
		 * if (!isTyojAvailable(do2)) { validity = false;
		 * do2.setStatus(Do2Manager.PENDING); buffer.append(INVALID_TYOJ); }
		 */
		if (!isRateAvailable(do2)) {
			validity = false;
			buffer.append(RATE_NOT_AVAILABLE);
		}
		if (!isValidTime(do2)) {
			validity = false;
			buffer.append(INVALID_TIME);
		} else if (do2LraDays != 0) {
			getLraDays(do2);
			if ((do2LraDays > 0) && ((int) totLraDays != do2LraDays)) {
				validity = false;
				buffer.append(INVALID_LRA);
			}
		}

		if (validity == false) {
			//     if(do2.getStatus() != null)
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setStatus(null);
			if (do2.getAcceptedQe() == null) {
				do2.setAcceptedQe(Utils.getCurrentQuarter());
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())) {
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
		do2Repo.save(do2);
		return validity;
	}

	// ***lines added for calculating DA for the FS Case
	public void calCulateDaForFs(Do2 do2, double totLraDays) {
		if (do2.getTjra() != null) {
			if (totLraDays > do2.getTjra()) {
				totLraDays = do2.getTjra();
			}
		}
		double daRate = pickCorrectRate(do2);
		System.out.println("da rate   " + daRate + " " + totLraDays);

		dailyAmount = totLraDays * daRate;
		System.out.println("daily amou nt  " + dailyAmount);
		totalDaily = Math.round(dailyAmount);
		System.out.println("daily amou nt  1" + totalDaily);
		Utils.addArrears(do2, totalDaily);

	}

	public boolean isValid7a(Do2 do2) {
		if (do2.getColumnA() == null)
			return false;
		int i = do2.getColumnA().indexOf("/");
		if (i > 0) {
			int days = Integer.parseInt(do2.getColumnA().substring(0, i));
			int hrs = Integer.parseInt(do2.getColumnA().substring((i + 1),
					(i + 3)));
			if ((hrs > 23) || (hrs == 0 && days == 0)) {
				return false;
			}
			newFormat = false;
		}

		return true;
	}

	public boolean isValid7b(Do2 do2) {
		if (newFormat) {
			if (do2.getColumnB() != null)
				return true;
			else
				return false;
		} else {
			if (do2.getColumnB() == null)
				return true;
			else
				return false;
		}
	}

	public boolean isValidDaily(Do2 do2) {
		do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		if (do2Days >= 180)
			return false;
		if (newFormat) {
			if (do2.getFromDate().equals(do2.getToDate()))
				dailyDays = do2Days - 1;
			else
				dailyDays = do2Days - 2;
		} else {
			int i = do2.getColumnA().indexOf("/");
			int hrs = Integer.parseInt(do2.getColumnA().substring((i + 1),
					(i + 3)));
			dailyDays = Integer.parseInt(do2.getColumnA().substring(0, i));
			if (hrs > 6 && hrs < 24)
				dailyDays += 1;
		}
		return true;
	}

	public boolean isValidTime(Do2 do2) {
		if (!newFormat)
			return true;
		if (!DateManager.isValidTime(do2.getColumnA())
				|| !DateManager.isValidTime(do2.getColumnB()))
			return false;
		try {
			double arrivalTime = (DateManager.getMinuteCount(do2.getColumnA())) / 60.0;
			double departureTime = (DateManager
					.getMinuteCount(do2.getColumnB())) / 60.0;
			if (do2.getFromDate().equals(do2.getToDate())
					&& (arrivalTime < departureTime && do2.getColumnA() != null && do2
							.getColumnB() != null)) {
				return true;
			}
			if (!do2.getFromDate().equals(do2.getToDate())) {
				return true;
			}
		} catch (InvalidDateRangeException e) {
			Utils.handleException(e);
		}

		return false;
	}

	public boolean isRateAvailable(Do2 do2) {
		double daRate = 0.0;
		String description = do2.getDescription();
		if (description.equals("FDAILY")) {
			description = "DAILY";
		}
		if (do2.getLra() != null) {
			if (do2.getLra() == 0)
				daRate = do2.getRate() * 4;
			else
				daRate = do2.getRate() * 2;
		}
		String city = do2.getColumnC();
		if (city != null) {
			if ((!city.equals("CCA") && !city.equals("CCA1") && !city
					.equals("CCB1"))) {
				if (city.startsWith("CC"))
					city = "CCOTH";
			}
		}
		if (!Utils.isValidAllowanceRate(description, do2.getFromDate(),
				do2.getFromDate(), city, daRate)) {
			return false;
		}
		return true;
	}

	public double pickCorrectRate(Do2 do2) {
		double daRate = 0.0;
		String description = do2.getDescription();
		if (do2.getDescription().equals("FDAILY")) {
			description = "DAILY";
		}
		String city = do2.getColumnC();
		if ((!city.equals("CCA") && !city.equals("CCA1") && !city
				.equals("CCB1"))) {
			if (city.startsWith("CC"))
				city = "CCOTH";
		}
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(
				description, do2.getFromDate(), do2.getToDate(), city,
				getBasicPay(do2));

		if (allowances.size() > 0 && allowances.get(0).getRateAmount() > 0.0) {
			if (do2.getLra() != null) {
				if (do2.getLra() == 0) {
					daRate = (allowances.get(0).getRateAmount() / 4);
				} else if (do2.getLra() > 0) {
					daRate = (allowances.get(0).getRateAmount() / 2);
				}
			}
		}
		return daRate;
	}

	public boolean isTyojAvailable(Do2 do2) {
		List<Do2> do2List =do2Repo.findByDescToDateEqualFromDateOrToDateEqualPreviousDate(do2.getEmployee().getId(),Do2Manager.VALID,Do2Manager.TYOJ,do2.getFromDate(),DateManager.getPreviousDate(do2.getFromDate()));
				 

		if (do2List.size() > 0) {
			return true;
		}
		return false;
	}

	public Integer getBasicPay(Do2 do2) {
		List<AuditCage> acList =acRepo.findByFromDateLessThanEqualOrderByFromDateDesc(do2.getEmployee().getId(),do2.getFromDate()); 
		 

		if (acList.size() > 0) {
			if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				basicPay = acList.get(0).getOldBasicPay()+ acList.get(0).getClassPay();
			} else  if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
				basicPay = acList.get(0).getBasicPay();
			} else if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
				basicPay = acList.get(0).getOldBasicPay();
			}
		}
		return basicPay;
	}

	public double getArrivalTime(Do2 do2) {

		try {
			String columnA = do2.getColumnA();
			String columnB = do2.getColumnB();
			if (columnA != null && columnA.equals("2400")) {
				columnA = "2359";
			}

			if (columnB != null && columnB.equals("2400")) {
				columnB = "2359";
			}
			if (do2.getFromDate().equals(do2.getToDate())) {
				arrivalHrs = (DateManager.getMinuteCount(columnA)) / 60.0;
			} else {
				arrivalHrs = (dayMinutes - DateManager.getMinuteCount(columnA)) / 60.0;
			}
			departureHrs = DateManager.getMinuteCount(columnB) / 60.0;

		} catch (InvalidDateRangeException e) {
			Utils.handleException(e);
		}
		sra_frdt = do2.getFromDate();
		sra_todt = do2.getToDate();

		if (do2.getFromDate().equals(do2.getToDate())) {
			arrdepHrs = departureHrs - arrivalHrs;

			if (arrdepHrs <= 6) {
				totLraDays = 0;
				sra_todt = DateManager.getDate(do2.getToDate(), -1);
			} else if (arrdepHrs > 6 && arrdepHrs <= 12) {
				totLraDays = 0.70;
				sra_todt = DateManager.getDate(do2.getToDate(), -1);
			} else if (arrdepHrs > 12 && arrdepHrs < 24) {
				totLraDays = 1;
			}
		} else {
			if (arrivalHrs <= 6) {
				totLraDays = 0;
				sra_frdt = DateManager.getDate(do2.getFromDate(), 1);
			} else if (arrivalHrs > 6 && arrivalHrs <= 12) {
				totLraDays = 0.70;
				sra_frdt = DateManager.getDate(do2.getFromDate(), 1);
			} else if (arrivalHrs > 12 && arrivalHrs < 24) {
				totLraDays = 1;
			}
		}
		return totLraDays;
	}

	public double getDepartureTime(Do2 do2) {

		if (!do2.getFromDate().equals(do2.getToDate())) {
			sra_todt = do2.getToDate();
			if (departureHrs <= 6) {
				sra_todt = DateManager.getDate(do2.getToDate(), -1);
			} else if (departureHrs > 6 && departureHrs <= 12) {
				totLraDays = totLraDays + 0.70;
				sra_todt = DateManager.getDate(do2.getToDate(), -1);
			} else if (departureHrs > 12 && departureHrs < 24) {
				++totLraDays;
			}
		}
		return totLraDays;
	}

	public double getLraDays(Do2 do2) {

		if ((do2.getColumnA().indexOf("/")) <= 0) {
			getArrivalTime(do2);
			getDepartureTime(do2);
			do2Days = DateManager.getNoOfDays(do2.getFromDate(),
					do2.getToDate());
			if (do2.getFromDate().equals(do2.getToDate()))
				dailyDays = do2Days - 1;
			else
				dailyDays = do2Days - 2;
			totLraDays = totLraDays + dailyDays;
		} else {
			int i = do2.getColumnA().indexOf("/");
			int hrs = Integer.parseInt(do2.getColumnA().substring((i + 1),
					(i + 3)));
			dailyDays = Integer.parseInt(do2.getColumnA().substring(0, i));
			totLraDays = dailyDays;
			if (hrs > 6 && hrs < 12)
				totLraDays = dailyDays + 0.7;
			else if (hrs > 11 && hrs < 24)
				totLraDays = totLraDays + dailyDays;
		}

		return totLraDays;
	}
}
