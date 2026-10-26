package itsdc.dolphin.do2.auditcage;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.SoldierGroup;
import itsdc.dolphin.models.Unit;

 
import itsdc.dolphin.utils.InvalidDateRangeException;
import itsdc.dolphin.utils.Utils;
import itsdc.dolphin.utils.DateManager;

import itsdc.dolphin.do2.AccountingCageHelper;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;

 

/**
 * 
 * @author ITSDC, Sec'bad.
 * 
 */

@RestController
public class Tyoj extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	static public final String INVALID_COLUMN7A = "VL:Departure/Arrival time not mentioned in Col.7A.\n ";
	static public final String INVALID_COL7A = "VL:Col.7A should be left blank. \n ";
	static public final String INVALID_COLUMN7B = "VL:Arrival/Departure time not mentioned in Col.7B.\n ";
	static public final String INVALID_COL7B = "VL:Days & Hrs not mentioned in Col.7B.\n ";
	static public final String INVALID_TIME = "VL:Invalid Departure/Arrival time in Column.7A/7B.\n ";
	static public final String RATE_NOT_AVAILABLE = "AU:Rate not matching with allowance rate w.r.to City/Basic.\n ";
	static public final String INVALID_TYRJ = "AU:No corresponding TYOJ/DAILY Do2.\n ";
	static public final String NO_DA = "AU:No Daily allowance for less than 6 hours.\n";
	static public final String INCORRECT_DATA="AU:Incorrect data - Field Allowance may be ceased for more than 180 days.  Pl. check \n";
 
	private double totalDaily, dailyAmount, dailyDays, do2Days,
			allowRateamount, tyojtoAmount, tyojfromAmount, tyojfromHrs,
			tyojtoHrs, tyojtotalHrs = 0.0;
	private double dayMinutes = 24 * 60;
	private boolean newFormat = true;
	private boolean incorrectData = false;
	private Integer basicPay;
	private StringBuffer buffer = new StringBuffer();
	

	@Override
	public boolean isValid(Do2 do2) {
		
		String columnA = do2.getColumnA();
		String columnB = do2.getColumnB();
		
		if (do2.getLra()==null) {
			do2.setLra(0);
		}
		
		if(columnA !=null && (columnA.endsWith("H") || columnA.endsWith("HR")|| columnA.endsWith("HRS"))){
			do2.setColumnA(columnA.substring(0,columnA.lastIndexOf("H")));
		}
		if(columnB !=null && (columnB.endsWith("H") || columnB.endsWith("HR")|| columnB.endsWith("HRS"))){
			do2.setColumnB(columnB.substring(0,columnB.lastIndexOf("H")));
		}
		boolean validity = true;
		if (!isValid7b(do2)) {
			validity = false;
			if (newFormat)
			  buffer.append(INVALID_COLUMN7B);
			else
			  buffer.append(INVALID_COL7B);
		}
		if (!isValid7a(do2)) {
			validity = false;
			if (newFormat)
				  buffer.append(INVALID_COLUMN7A);
				else
				  buffer.append(INVALID_COL7A);
		}
		if (newFormat) {
		  if (!isValidTime(do2)) {
			validity = false;
			buffer.append(INVALID_TIME);
		  }
		}
			
		if (!isRateAvailable(do2)) {
			validity = false;
			buffer.append(RATE_NOT_AVAILABLE);
		} 
	
		/*if(do2.getDescription().equals("TYRJ")){
			if(!isDailyAvailable(do2)){
			   validity =false;
			   do2.setStatus(Do2Manager.PENDING);
			   buffer.append(INVALID_TYRJ);
			}
		}*/
		
		if (validity && newFormat && do2.getDescription().equals("TYRJ"))
			checkForFieldAllowances(do2);
		
		if (incorrectData && do2.getDescription().equals("TYRJ") && newDo2.getId()==do2.getId()) {
			validity =false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(INCORRECT_DATA);
		}
		
		if (validity == false) {
			/*if ((do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
			} */	
			do2.setStatus(Do2Manager.INVALID);
		} else{
			if (newFormat && isLessThanSixHrs(do2)){
				buffer.append(NO_DA);
			}
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
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getId()==newDo2.getId()) {
			if ((do2.getColumnB().substring(0, 3)).equals("DA-"))
			newFormat = false;
		
	    if (newFormat && !isLessThanSixHrs(do2)) {
			List<Allowance> allowances = Utils.getApplicableAllowanceRates(do2.getDescription(),
					do2.getFromDate(), do2.getToDate(), getBasicPay(do2));
			for (Allowance allowance : allowances) {
				allowRateamount = allowance.getRateAmount();
				getTyojDays(do2);
				getTyojFromAmount(do2);
				getTyojToAmount(do2);
				dailyAmount = (dailyDays) * allowRateamount;
				totalDaily += Math.round(dailyAmount + tyojtoAmount + tyojfromAmount);
			}
	    }
	    if (!newFormat) {
	        if ((do2.getColumnB().substring(0, 3)).equals("DA-")){
				int i = do2.getColumnB().indexOf("/");
				dailyDays = 0.0;
				if (i > 0){
				  int days = Integer.parseInt(do2.getColumnB().substring(3, i));
				  int hrs = Integer.parseInt(do2.getColumnB().substring((i+1), (i+3)));
				  if (hrs > 6 && hrs < 13)
				     dailyDays = days + 0.7;
				  else if (hrs > 12)
					  dailyDays = days + 1;
				}
				
	    	    List<Allowance> allowances = Utils.getApplicableAllowanceRates(do2.getDescription(),
						do2.getFromDate(), do2.getFromDate(), getBasicPay(do2));
	    	   for (Allowance allowance : allowances) {
					allowRateamount = allowance.getRateAmount();
					dailyAmount = (dailyDays) * allowRateamount;
					totalDaily += Math.round(dailyAmount);
				}
	        }
	    }
			
			Utils.addArrears(do2, totalDaily, Do2Manager.DAILY,do2.getFromDate(),do2.getToDate());
		} 
		    return Do2Manager.SUCCESS;
	}

	public boolean isValid7a(Do2 do2) {
	 
		if  (!newFormat) {
			if (do2.getColumnA()==null || do2.getColumnA().length()==0)
			  return true;
			else
			  return false;
		}
		if (do2.getColumnA() != null) {
			do2.setColumnA(do2.getColumnA().trim());
			return true;
		} else {
			return false;
		}
	}

	public boolean isValid7b(Do2 do2) {
		if ((do2.getColumnB() == null) || (do2.getColumnB().trim().length()<4))
			return false;
		do2.setColumnB(do2.getColumnB().trim());
		if ((do2.getColumnB().substring(0, 3)).equals("DA-")){
			newFormat = false;
			int i = do2.getColumnB().indexOf("/");
			if (i > 0){
			  int days = Integer.parseInt(do2.getColumnB().substring(3, i));
			  int hrs = Integer.parseInt(do2.getColumnB().substring((i+1), (i+3)));
			  if ((hrs >  23) || (hrs == 0 && days == 0)){
			    	return false;
			  }
			} else {
				    return false;
			}
		}
		return true;		
	}

	public Integer getBasicPay(Do2 do2) {
		basicPay=0;
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> acList = acRepo.findByFromDateLessThanEqualOrderByFromDateDesc(emp.getId(), do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery(
						"SELECT ac from AuditCage ac where ac.employee=:e and ac.fromDate<=:fromDate AND " +
						" ac.effective=true and ac.cancelled=false and ac.absentFlag=false and ac.recordStatus!='P' ORDER BY ac.fromDate DESC")
				.setParameter("fromDate", do2.getFromDate())
				.setParameter("e", Utils.getCurrentEmployee(do2.getArmyNo())).getResultList();*/
		if (acList.size() > 0) {
			if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()))
				basicPay = acList.get(0).getOldBasicPay()+ acList.get(0).getClassPay();
			else if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
				basicPay = acList.get(0).getBasicPay();
			} else {
				basicPay = acList.get(0).getOldBasicPay();
			}
		}
		return basicPay;
		
	}

	public double getTyojFromAmount(Do2 do2) {
		try {
			if (do2.getFromDate().equals(do2.getToDate())) {
				tyojfromHrs = (DateManager.getMinuteCount(do2.getColumnA())) / 60.0;
			} else {
				tyojfromHrs = (dayMinutes - DateManager.getMinuteCount(do2
						.getColumnA())) / 60.0;
			}
			tyojtoHrs = DateManager.getMinuteCount(do2.getColumnB()) / 60.0;
		} catch (InvalidDateRangeException e) {
			Utils.handleException(e);
		}
		if (do2.getFromDate().equals(do2.getToDate())) {
			tyojtotalHrs = tyojtoHrs - tyojfromHrs;

			if (tyojtotalHrs <= 6) {
				tyojfromAmount = 0;
			} else if (tyojtotalHrs > 6 && tyojtotalHrs <= 12) {
				tyojfromAmount = allowRateamount * 70 / 100;
			} else if (tyojtotalHrs > 12 && tyojtotalHrs < 24) {
				tyojfromAmount = allowRateamount;
			}
		} else {
			if (tyojfromHrs <= 6) {
				tyojfromAmount = 0;
			} else if (tyojfromHrs > 6 && tyojfromHrs <= 12) {
				tyojfromAmount = allowRateamount * 70 / 100;
			} else if (tyojfromHrs > 12 && tyojfromHrs < 24) {
				tyojfromAmount = allowRateamount;
			}
		}
		return tyojfromAmount;
	}


	public double getTyojToAmount(Do2 do2) {
		if (do2.getFromDate().equals(do2.getToDate())) {
			tyojtoAmount = 0;
		} else if (tyojtoHrs <= 6) {
			tyojtoAmount = 0;
		} else if (tyojtoHrs > 6 && tyojtoHrs <= 12) {
			tyojtoAmount = allowRateamount * 70 / 100;
		} else if (tyojtoHrs > 12 && tyojtoHrs < 24) {
			tyojtoAmount = allowRateamount;
		}
		return tyojtoAmount;
	}

	public double getTyojDays(Do2 do2) {
		if (do2.getFromDate().equals(do2.getToDate())) {
			dailyDays = DateManager.getNoOfDays(do2.getFromDate(), do2
					.getToDate()) - 1;
			return dailyDays;
		} else {
			dailyDays = DateManager.getNoOfDays(do2.getFromDate(), do2
					.getToDate()) - 2;
			return dailyDays;
		}
	}
	
	public boolean isValidTime(Do2 do2){
		if(do2.getColumnA() == null || do2.getColumnB() == null ){
			return false;
		}
		 if(!DateManager.isValidTime(do2.getColumnA()) || !DateManager.isValidTime(do2.getColumnB()))
				return false;
		 try {
				double arrivalTime = (DateManager.getMinuteCount(do2.getColumnB())) / 60.0;
				double departureTime = (DateManager.getMinuteCount(do2.getColumnA())) / 60.0;
				if (do2.getFromDate().equals(do2.getToDate()) && (arrivalTime > departureTime )) {
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
	
	public boolean isLessThanSixHrs(Do2 do2){
		try {
			double departureTime = (DateManager.getMinuteCount(do2.getColumnA())) / 60.0;
			double arrivalTime= (DateManager.getMinuteCount(do2.getColumnB())) / 60.0;
			
			if (do2.getFromDate().equals(do2.getToDate()) && ( (arrivalTime - departureTime) <= 6) ) {
				return true;
			}
			
		} catch (InvalidDateRangeException e) {
			Utils.handleException(e);
		}
		return false;
	}
	
	public boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(do2.getDescription(),
				do2.getFromDate(), do2.getFromDate(), getBasicPay(do2));
		if (allowances.size() > 0 && allowances.get(0).getRateAmount() <=135.0) {
			return true;
		}
		return false;
	}
	public boolean isDailyAvailable(Do2 do2){
		Employee employee =Utils.getCurrentEmployee(do2.getArmyNo());
		List <Do2> do2List  = null;
		do2List = do2Repo.findByFromDateAndPreviousDate(employee.getId(),Do2Manager.VALID,Do2Manager.TYOJ,do2.getFromDate(),DateManager.getPreviousDate(do2.getFromDate()));
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT d FROM Do2 d WHERE d.employee=:employee "
						+ " AND d.status=:status AND d.description =:description"
						+ " AND (d.toDate =:fromDate or d.toDate =:previousDate) ")
		.setParameter("employee", employee).setParameter("status",
				Do2Manager.VALID).setParameter("fromDate", do2.getFromDate())
		.setParameter("description", Do2Manager.TYOJ)
		.setParameter("previousDate", DateManager.getPreviousDate(do2.getFromDate())).getResultList();*/
		if(do2List.size()==0){
			 do2List = do2Repo.findByFromDateAndPreviousDate(employee.getId(),Do2Manager.VALID,Do2Manager.DAILY,do2.getFromDate(),DateManager.getPreviousDate(do2.getFromDate()));
					 
					 
			/*		 Utils.getEntityManager().createQuery(
					"SELECT d FROM Do2 d WHERE d.employee=:employee "
							+ " AND d.status=:status AND d.description =:description"
							+ " AND (d.toDate =:fromDate or d.toDate =:previousDate) ")
			.setParameter("employee", employee).setParameter("status",
					Do2Manager.VALID).setParameter("fromDate", do2.getFromDate())
			.setParameter("description", Do2Manager.DAILY)
			.setParameter("previousDate", DateManager.getPreviousDate(do2.getFromDate())).getResultList();*/
		}
		return do2List.size() >0;
		}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		List<Arrears> arrearsList = arrRepo.findByDo2Id(refDo2.getId());
				
		//		Utils.getEntityManager().createQuery("SELECT a FROM Arrears a " 
			//	+ " WHERE a.do2=:do2 ").setParameter("do2", refDo2).getResultList();
		
		int totalArrears=0;
		
		for (Arrears arr : arrearsList) {
			if (arr.getPayCode().getId() == 171)
			    totalArrears+=arr.getAmount();
		}
		Utils.addArrearsForCancelDo2(currentDo2, refDo2, totalArrears*-1, "DAILY", refDo2.getFromDate(),refDo2.getToDate());  
		return Do2Manager.SUCCESS;
	}

	public void checkForFieldAllowances(Do2 do2) {
		
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		double temporaryDutyDays=0;
		Date tyojDailyCheckDate = DateManager.getDate(do2.getFromDate(), -2);
		Date ceasedDo2FromDate = null;
		Do2 tyojDo2 = null; 
		Date toDate = null;
		String refDo2No = Utils.getReferenceDo2No(do2);
		
		List<Do2> dailyDo2List  = getDo2ListForDescription(employee, "DAILY", tyojDailyCheckDate, do2.getFromDate());
 	
		if (dailyDo2List!=null && dailyDo2List.size()>0) {
 
			tyojDailyCheckDate = DateManager.getDate(dailyDo2List.get(0).getFromDate(),-2);
			List<Do2> tyojDo2List = getDo2ListForDescription(employee, "TYOJ", tyojDailyCheckDate, do2.getFromDate());
			if (tyojDo2List!=null && tyojDo2List.size()>0) {
				tyojDo2 = tyojDo2List.get(0);
			} else {
				List<Do2> alDo2List = getDo2ListForDescription(employee, "%AL", tyojDailyCheckDate, do2.getFromDate());
				if (alDo2List!=null && alDo2List.size()>0) {
					tyojDo2 = alDo2List.get(0);
			 	} else {
			 		tyojDo2 = dailyDo2List.get(0);   // cases where no TYOJ/AL/PAL/BAL exists
			 	}
			}
		} else {            // cases where only TYOJ/TYRJ exists
			List<Do2> tyojDo2List = getDo2ListForDescription(employee, "TYOJ", tyojDailyCheckDate, do2.getFromDate());
			if (tyojDo2List!=null && tyojDo2List.size()>0) {
				tyojDo2 = tyojDo2List.get(0);
		 	} else {
		 		tyojDo2 = do2;                          // cases where no DAILY/TYOJ exists --- to check TYRJ exceeding 14/21 days.
		 	}
		}
	 
		if (tyojDo2!=null) {
			toDate = do2.getToDate();
			try {
				if (do2.getColumnB()!=null && (DateManager.getMinuteCount(do2.getColumnB())/60.0)<6) {
					toDate=DateManager.getPreviousDate(do2.getToDate());
				}
			} catch (InvalidDateRangeException e) {
				Utils.handleException(e);			
			}
			ceasedDo2FromDate = getCeasedDo2FromDate(tyojDo2);
			temporaryDutyDays = DateManager.getNoOfDays(ceasedDo2FromDate, toDate);
		}
		
		if (temporaryDutyDays<=14)   
			return;
		
		if (temporaryDutyDays>180) {
			incorrectData=true;
			return;
		}
						
		List<AuditCage> auditCageList = acRepo.findByFromDateLessThanEqualOrderByFromDateDesc(employee.getId(), do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac " +
												" WHERE ac.employee=:emp AND ac.cancelled=false AND ac.absentFlag=false AND " +
												" ac.deputationFlag=false AND ac.effective=true AND ac.fromDate<=:fromDate " +
												" ORDER BY fromDate DESC")
												.setParameter("emp", employee)
												.setParameter("fromDate", do2.getFromDate()).getResultList();*/
		AuditCage baseAuditCage = null;
		
		if (auditCageList.size()>0) {
			baseAuditCage = auditCageList.get(0);
		}
		
		if (baseAuditCage.getTransport() != null && baseAuditCage.getTransport() > 0){
			if (DateManager.getNoOfMonths(ceasedDo2FromDate, toDate) > 1.0 ){
				Date tptlCeasedDate = getTransportAllceDate(ceasedDo2FromDate, toDate);								
				if (tptlCeasedDate != null){
					Do2 tptldo2 = getTransportAllceDo2(employee, ceasedDo2FromDate);
					if (tptldo2 != null) {
						String description = tptldo2.getDescription();
						double rate        = tptldo2.getRate();
						Do2	 tptlEarlierGrantDo2 = tptldo2;
						String columnC = tptldo2.getColumnC();
						generatetptlCeaseDo2(do2, tptlCeasedDate, description);
					    generateTptlGrantDo2(do2, description, rate, tptlEarlierGrantDo2, columnC);
					}					
				}
			}
		}
		
		List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(baseAuditCage.getId());
				
			//	Utils.getEntityManager().createQuery("SELECT aca FROM AuditCageAllowance aca " +
			//									" WHERE aca.auditCage=:baseAuditCage")
				//								.setParameter("baseAuditCage", baseAuditCage).getResultList();
		for (AuditCageAllowance aca : acaList) {
			
			if (tyojDo2!=null && temporaryDutyDays>14 && temporaryDutyDays<=21) {
				if (Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyFourteenDays().contains(aca.getPayCode().getPayCode())) {
					List<Do2> do2List  = do2Repo.findByStatusDescriptionColAAndFromDateBetween(employee.getId(), 
							Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyFourteenDays(),"CEASED", ceasedDo2FromDate, tyojDo2.getToDate());
							
				/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee "
						+ " AND (d.status='V' OR d.status='P') AND d.description in (:descriptions) AND d.columnA='CEASED' AND " 
						+ " d.fromDate BETWEEN :fromDate AND :toDate").setParameter("employee", employee)
						.setParameter("fromDate", ceasedDo2FromDate).setParameter("toDate", tyojDo2.getToDate())
						.setParameter("descriptions", Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyFourteenDays())
						.getResultList();*/
				
					if (do2List.size()==0) {
						generateAllowanceCeaseDo2(tyojDo2, aca.getPayCode().getPayCode(), ceasedDo2FromDate, employee, refDo2No);
					}
				}
			} else if (tyojDo2!=null) {
				if (Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyTwentyoneDays().contains(aca.getPayCode().getPayCode())) {
					List<Do2> do2List  = do2Repo.findByStatusDescriptionColAAndFromDateBetween(employee.getId(), 
							Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyTwentyoneDays(),"CEASED", ceasedDo2FromDate, tyojDo2.getToDate());
							
				/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee "
						+ " AND (d.status='V' OR d.status='P') AND d.description in (:descriptions) AND d.columnA='CEASED' AND " 
						+ " d.fromDate BETWEEN :fromDate AND :toDate ").setParameter("employee", employee).setParameter("fromDate", ceasedDo2FromDate)
						.setParameter("descriptions", Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyTwentyoneDays())
						.setParameter("toDate", tyojDo2.getToDate())
						.getResultList();*/
				
					if (do2List.size()==0) {
						generateAllowanceCeaseDo2(tyojDo2, aca.getPayCode().getPayCode(), ceasedDo2FromDate, employee, refDo2No);
					}
				}
			}
		}
	}
	
	public void generateAllowanceCeaseDo2(Do2 do2, String description, Date ceaseDo2Date, Employee employee, String refDo2No) {
		Do2 allowanceCeaseDo2 = new Do2();
	 	allowanceCeaseDo2.setEmployee(employee);
		allowanceCeaseDo2.setArmyNo(employee.getArmyNo());
		allowanceCeaseDo2.setCheckDigit(employee.getCheckDigit());
		allowanceCeaseDo2.setUnitCode(do2.getUnitCode());
		allowanceCeaseDo2.setUnit(do2.getUnit());
		allowanceCeaseDo2.setDo2Group("IA");
		allowanceCeaseDo2.setDo2Type(do2.getDo2Type());
		allowanceCeaseDo2.setDo2No(do2.getDo2No());
		allowanceCeaseDo2.setDo2ItemNo(do2.getDo2ItemNo());
		allowanceCeaseDo2.setDo2Date(do2.getDo2Date());
		allowanceCeaseDo2.setDescription(description);
		allowanceCeaseDo2.setFromDate(ceaseDo2Date);
		allowanceCeaseDo2.setColumnA("CEASED");
		allowanceCeaseDo2.setColumnC("TD");
		allowanceCeaseDo2.setStatus(Do2Manager.PENDING);
		allowanceCeaseDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		allowanceCeaseDo2.setAuditCage(true);
		allowanceCeaseDo2.setQe(Utils.getCurrentQuarter());
		allowanceCeaseDo2.setAcceptedQe(Utils.getCurrentQuarter());
		allowanceCeaseDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		allowanceCeaseDo2.setRefDo2No(refDo2No);
		do2Repo.save(allowanceCeaseDo2);
	//	Utils.getEntityManager().persist(allowanceCeaseDo2);
	//	Utils.getEntityManager().flush();
	}
	
	public List<Do2> getDo2ListForDescription(Employee employee, String description, Date fromDate, Date toDate) {
		List<Do2> do2List = null;
		
		do2List = do2Repo.findByToDateBetweenAndDescriptionLikeOrderByToDateDesc(employee.getId(), description, fromDate, toDate);
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee "
				+ " AND d.status='V' AND d.description like :description AND d.toDate BETWEEN :fromDate1 AND :fromDate2 " 
				+ " ORDER BY d.toDate DESC").setParameter("employee", employee)
				.setParameter("fromDate1", fromDate).setParameter("fromDate2", toDate).setParameter("description", description)
				.getResultList();*/

		return do2List;
	}
	
	public Date getCeasedDo2FromDate(Do2 do2) {
		Date ceasedDo2FromDate = do2.getFromDate();
		
		try {
			if (do2.getColumnA()!=null && (DateManager.getMinuteCount(do2.getColumnA())/60.0)<18) {
				ceasedDo2FromDate=do2.getFromDate();
			} else {
				ceasedDo2FromDate=DateManager.getNextDate(do2.getFromDate());
			}
		} catch (InvalidDateRangeException e) {
			Utils.handleException(e);			
		}	
		
		return ceasedDo2FromDate; 
	}
	private Date getTransportAllceDate(Date fromDate, Date toDate) {		
		Date tptlDate = DateManager.getToday();
		Date ceaseDate = DateManager.getToday();
		GregorianCalendar fromCal = new GregorianCalendar();
		GregorianCalendar toCal = new GregorianCalendar();
		if (!fromDate.after(DateManager.getTptlDate())) {
			double do2Days = DateManager.getNoOfDays(fromDate, toDate);
			if (do2Days > 30) {
				fromCal.setTime(fromDate);
				ceaseDate.setTime(fromCal.getTimeInMillis());
				tptlDate = ceaseDate;
			}
		} else {
			double monthCount = DateManager.getNoOfMonths(fromDate, toDate);
			if (monthCount < 1.0)
				return null;

			fromCal.setTime(fromDate);
			toCal.setTime(toDate);
			int toMonthMaxDays = DateManager.getNoOfDaysInMonth(toCal
					.get(Calendar.MONTH) + 1, toCal.get(Calendar.YEAR));

			// Doesn't span full month
			if (monthCount < 2.0){ 
			
				if((fromCal.get(Calendar.MONTH)+1 == toCal.get(Calendar.MONTH) && fromCal.get(Calendar.DATE) !=1 && toCal.get(Calendar.DATE) !=toMonthMaxDays)
						|| (fromCal.get(Calendar.MONTH)==Calendar.DECEMBER && toCal.get(Calendar.MONTH)==Calendar.JANUARY )
						&& fromCal.get(Calendar.DATE) !=1 && toCal.get(Calendar.DATE) != toMonthMaxDays)
					
				return null;
			}

			if (fromCal.get(Calendar.DATE) != 1
					&& toCal.get(Calendar.DATE) == toMonthMaxDays) {
				fromCal.set(Calendar.MONTH, fromCal.get(Calendar.MONTH) + 1);
			} else if (fromCal.get(Calendar.DATE) != 1
					&& toCal.get(Calendar.DATE) != toMonthMaxDays) {
				fromCal.set(Calendar.MONTH, fromCal.get(Calendar.MONTH) + 1);
			} 
			fromCal.set(Calendar.DATE, 1);
			ceaseDate.setTime(fromCal.getTimeInMillis());
			tptlDate = ceaseDate;
		}
		return tptlDate;
	}

	private void generatetptlCeaseDo2(Do2 do2, Date ceaseDate, String description){
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());	
		if (!isDo2Generated(description,Do2Manager.CEASED,ceaseDate,do2)){
			Do2 tptlCeaseDo2 = new Do2();			
			tptlCeaseDo2.setEmployee(employee);
			tptlCeaseDo2.setArmyNo(employee.getArmyNo());
			tptlCeaseDo2.setCheckDigit(employee.getCheckDigit());
			tptlCeaseDo2.setUnitCode(do2.getUnitCode());
			Unit uc=unitRepo.findByUnitCode(do2.getUnitCode());
			tptlCeaseDo2.setUnit(uc);
		//	tptlCeaseDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode",	do2.getUnitCode()));
			tptlCeaseDo2.setDo2Group("IA");
			tptlCeaseDo2.setDo2Type(do2.getDo2Type());
			tptlCeaseDo2.setDo2No(do2.getDo2No());
			tptlCeaseDo2.setDo2ItemNo(do2.getDo2ItemNo());
			tptlCeaseDo2.setDo2Date(do2.getDo2Date());
			tptlCeaseDo2.setDescription(description);
			tptlCeaseDo2.setFromDate(ceaseDate);
			tptlCeaseDo2.setColumnA(Do2Manager.CEASED);
			tptlCeaseDo2.setStatus(Do2Manager.PENDING);
			tptlCeaseDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			tptlCeaseDo2.setAuditCage(true);
			tptlCeaseDo2.setQe(Utils.getCurrentQuarter());
			tptlCeaseDo2.setAcceptedQe(Utils.getCurrentQuarter());
			tptlCeaseDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			do2Repo.save(tptlCeaseDo2);
		//	Utils.getEntityManager().persist(tptlCeaseDo2);
		//	Utils.getEntityManager().flush();	
			/*Do2Home d2Home = (Do2Home) Component.getInstance("do2Home");
			d2Home.setInstance(tptlCeaseDo2);
			d2Home.persist();*/
			
		}				
	}
		
	private void generateTptlGrantDo2(Do2 do2, String description, double rate, Do2 tptlEarlierGrantDo2, String columnC ){
		Date tptlRegrantDate = getTptlRegrantDate(do2.getToDate());
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());		 
		if (!isDo2Generated(description,Do2Manager.GRANTED,tptlRegrantDate,do2)){
			Do2 tptlGrantDo2 = new Do2();
			tptlGrantDo2.setEmployee(employee);
			tptlGrantDo2.setArmyNo(employee.getArmyNo());
			tptlGrantDo2.setCheckDigit(employee.getCheckDigit());
			tptlGrantDo2.setUnitCode(do2.getUnitCode());
			Unit uc=unitRepo.findByUnitCode(do2.getUnitCode());
			tptlGrantDo2.setUnit(uc);
		//	tptlGrantDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode",	do2.getUnitCode()));
			tptlGrantDo2.setDo2Group("IA");
			tptlGrantDo2.setDo2Type(tptlEarlierGrantDo2.getDo2Type());
			tptlGrantDo2.setDo2No(tptlEarlierGrantDo2.getDo2No());
			tptlGrantDo2.setDo2ItemNo(tptlEarlierGrantDo2.getDo2ItemNo());
			tptlGrantDo2.setDo2Date(tptlEarlierGrantDo2.getDo2Date());
			tptlGrantDo2.setDescription(description);
			tptlGrantDo2.setFromDate(tptlRegrantDate);
			tptlGrantDo2.setColumnA(Do2Manager.GRANTED);
			tptlGrantDo2.setColumnC(columnC);
			tptlGrantDo2.setRate(rate);
			tptlGrantDo2.setStatus(Do2Manager.PENDING);
			tptlGrantDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			tptlGrantDo2.setAuditCage(true);
			tptlGrantDo2.setQe(Utils.getCurrentQuarter());
			tptlGrantDo2.setAcceptedQe(Utils.getCurrentQuarter());
			tptlGrantDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			do2Repo.save(tptlGrantDo2);
		//	Utils.getEntityManager().persist(tptlGrantDo2);
		//	Utils.getEntityManager().flush();		
		/*	Do2Home d2Home = (Do2Home) Component.getInstance("do2Home");
			d2Home.setInstance(tptlGrantDo2);
			d2Home.persist();*/
		}		 
	}
	private Date getTptlRegrantDate(Date toDate){
		   Date tptlRegrantDate=null;
		   GregorianCalendar fromCal = new GregorianCalendar();
		   fromCal.setTime(toDate);
		   int noOfDays = DateManager.getNoOfDaysInMonth(fromCal.get(Calendar.MONTH)+1,fromCal.get(Calendar.YEAR));
		   int datedays = fromCal.get(Calendar.DAY_OF_MONTH);
		  
		   if ((noOfDays - datedays) == 0) {
			   fromCal.set(Calendar.MONTH,fromCal.get(Calendar.MONTH)+1);
			   fromCal.set(Calendar.DATE,1);
			    tptlRegrantDate = fromCal.getTime();			  
		   } else {
			   fromCal.set(Calendar.DATE,1);
			   tptlRegrantDate = fromCal.getTime();
		   }
		   return tptlRegrantDate;
	}
	public boolean isDo2Generated(String description,String columnA,Date fromDate,Do2 do2){
		Employee employee=Utils.getCurrentEmployee(do2.getArmyNo());
		   List<Do2> do2List = do2Repo.findByDescStatusColAFromDateEqual(employee.getId(), Do2Manager.VALID, description,
				   																columnA, fromDate);
				   
			/*	   Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:employee and d.description=:description and d.fromDate=:fromDate and d.columnA=:columnA and d.status=:status")
		             .setParameter("employee", Utils.getCurrentEmployee(do2.getArmyNo()))
		             .setParameter("description",description)
		             .setParameter("fromDate",fromDate)
		             .setParameter("columnA", columnA)
		             .setParameter("status", Do2Manager.VALID)
		             .getResultList();*/
		                                 
		   return do2List.size() > 0;
	}
	  
	public Do2 getTransportAllceDo2(Employee employee, Date fromDate){
		  List<Do2> tptlList = do2Repo.findBy2DescColAFromDateLessThanEqual(employee.getId(), Do2Manager.VALID, Do2Manager.TPTL, 
				  													Do2Manager.TPTH, Do2Manager.GRANTED, fromDate);
				  
		/*		  Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:employee and " +
		  		" d.fromDate<=:fromDate and d.status=:status and d.columnA=:columnA and " +
		  		" (d.description=:description or d.description=:description1) order by d.fromDate desc ")
                   .setParameter("employee",employee)
                   .setParameter("fromDate", fromDate)
                   .setParameter("status",Do2Manager.VALID)
                   .setParameter("columnA",Do2Manager.GRANTED)
                   .setParameter("description", Do2Manager.TPTL)
                   .setParameter("description1",Do2Manager.TPTH)
                   .getResultList();*/
		  if (tptlList.size() == 0)
			  return null;
		  return tptlList.get(0);
	}


}