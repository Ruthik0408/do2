package itsdc.dolphin.do2.auditcage;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;

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
public class Rtyoj extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	static public final String INVALID_PUBLICATION = "AU:Published before Revised Appendix J Impl date.\n";
	static public final String INVALID_TYOJ = "AU:Cannot be combined with TYOJ/DAILY.\n";
	static public final String INVALID_RTYOJ = "AU:Corresponding RTYOJ/DAILY not available.\n";
	static public final String INVALID_TJRA = "VL:Departure time not mentioned in TJRA or incorrect. \n ";
	static public final String INVALID_LRA = "VL:Arrival time not mentioned in LRA or incorrect. \n ";
	static public final String INVALID_TIME = "VL:Invalid Departure/Arrival time in TJRA/LRA.\n ";
	static public final String INVALID_PERIOD = "VL:Daily period is more than 180 days.\n";
	static public final String INVALID_COL7A = "VL:Accommodation amount to be shown in Col-7A amount should be 0 or > 0. \n";
	static public final String INVALID_COL7B = "VL:Food amount to be shown in Col-7B amount should be 0 or > 0 \n ";
	static public final String INVALID_COL7C = "VL:Transport amount to be shown in Col-7C amount should be 0 or > 0 \n";
	static public final String NO_DA = "AU:No Daily allowance for less than 6 hours.\n";
	static public final String INCORRECT_DATA="AU:Incorrect data - Field Allowance may be ceased for more than 180 days.  Pl. check \n";
	public double totalDaily = 0.0, foodAmount = 0.0, transportAmount = 0.0;
	public Integer accommodationAmount = 0;
	public double totAccomAmt=0.0, totFoodAmt = 0.0, totTransAmt= 0.0;
	
    private boolean todatesame = false;
    private boolean incorrectData = false;
	private StringBuffer buffer = new StringBuffer();
	@Override
	public boolean isValid(Do2 do2) {

		boolean validity = true;
		if (!Utils.getPao().getPaoCode().equals("94")) {
			if (do2.getDo2Date().before(DateManager.getRevisedAppendixJImplementationDate())){
				validity = false;
				buffer.append(INVALID_PUBLICATION);
			}	
		}
		
		if (!isValidTime(do2)) {
			validity = false;
			buffer.append(INVALID_TIME);
		} 
//		if ((do2.getColumnA() != null &&  Integer.parseInt(do2.getColumnA())!=0)) {
		if (!isValid7a(do2)) {
			validity = false;
			buffer.append(INVALID_COL7A);
		}
		if (!isValid7b(do2)) {
			validity = false;
			buffer.append(INVALID_COL7B);
		}
//		if ((do2.getColumnC() != null && Integer.parseInt(do2.getColumnC())!=0)) {
		if (!isValid7c(do2)) {
			validity = false;
			buffer.append(INVALID_COL7C);
		}
		if (!isValidRtyojRtyrj(do2)) {
			validity = false;
			buffer.append(INVALID_PERIOD);
		}
		
		if (do2.getDescription().equals("RTYRJ") && !isRdailyAvailable(do2)) { 
			validity = false;
		    do2.setStatus(Do2Manager.PENDING); 
		    buffer.append(INVALID_RTYOJ); 
		}
	/*	
		if (do2.getDescription().equals("RTYRJ") && isDailyAvailable(do2)) { 
			validity = false;
		    do2.setStatus(Do2Manager.INVALID); 
		    buffer.append(INVALID_TYOJ); 
		}		
		*/
		if (validity && do2.getDescription().equals("RTYRJ"))
			checkForFieldAllowances(do2);
		
		if (incorrectData && do2.getDescription().equals("RTYRJ") && do2.getAcceptedQe() == null) {
			validity =false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INCORRECT_DATA);
		}
		
		if (validity == false) {
			if ((do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
			} 			
		} else{
			if (isLessThanSixHrs(do2)){
				buffer.append(NO_DA);
			}
			do2.setStatus(Do2Manager.VALID);
		}
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe() == null) {
				do2.setAcceptedQe(Utils.getCurrentQuarter());
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())) {
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}

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
		double do2Days = 0.0;
		double dailyAmount = 0.0;
		totalDaily = 0.0;
		if (isValid(do2) && !isLessThanSixHrs(do2)) {
			if (todatesame){
				if (do2.getToDate().after(do2.getFromDate())){
					do2Days = DateManager.getNoOfDays(DateManager.getNextDate(do2.getFromDate()), do2.getToDate());
				}
			} else {
				do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
			}
			
			List<Allowance> allowances = Utils.getApplicableAllowanceRates("RDAILY",
					getGradePay(do2), do2.getFromDate(), do2.getToDate()) ;
			
			
			if (allowances.size() > 0) {
				if(allowances.get(0).getPayEndRange() > 0 ) {
					if (Integer.parseInt(do2.getColumnA()) < allowances.get(0).getPayEndRange()) {
						accommodationAmount = Integer.parseInt(do2.getColumnA());
					} else {
						accommodationAmount = allowances.get(0).getPayEndRange();
					}
				}
				
			/*	if(allowances.get(0).getPayEndRange() > 0 ) {
					if (Integer.parseInt(do2.getColumnA()) < allowances.get(0).getPayEndRange()) {
						accommodationAmount = Integer.parseInt(do2.getColumnA());
					} else {
						accommodationAmount = allowances.get(0).getPayEndRange();
					}
				}*/
				
				if(allowances.get(0).getRatePercentage() > 0 ) {
					
					double departureTime = (((do2.getTjra()/100) * 60.0) + (do2.getTjra()%100)) / 60.0;
					double arrivalTime = (((do2.getLra()/100) * 60.0) + (do2.getLra()%100)) / 60.0;
					
					
					if (do2.getFromDate().after(DateManager.getSeventhCpcAllowanceDate())) {
						 if (do2.getFromDate().equals(do2.getToDate()) && ( (arrivalTime - departureTime) <= 6) ) {
								foodAmount = allowances.get(0).getRatePercentage()*0.3;
							}else if (do2.getFromDate().equals(do2.getToDate()) && 
									( (arrivalTime - departureTime) > 6) && ( (arrivalTime - departureTime) <= 12) ) {
								foodAmount = allowances.get(0).getRatePercentage()*0.7;
							}else {
								foodAmount = allowances.get(0).getRatePercentage();
						}
					}else 
						if (Integer.parseInt(do2.getColumnB()) < allowances.get(0).getRatePercentage()) {
						foodAmount = Integer.parseInt(do2.getColumnB());
						} else {
						foodAmount = allowances.get(0).getRatePercentage();
					}
				}
				
				
				if(allowances.get(0).getRateAmount() > 0 ) {
					if (Integer.parseInt(do2.getColumnC()) < allowances.get(0).getRateAmount()) {
						transportAmount = Integer.parseInt(do2.getColumnC());
					} else {
						transportAmount = allowances.get(0).getRateAmount();
					}
				}
				
				totAccomAmt = (int) Math.round(do2Days * accommodationAmount);
				totFoodAmt  = (int) Math.round(do2Days * foodAmount);
				totTransAmt =  (int) Math.round(do2Days * transportAmount);
				
				
				
			}
		}
			
			
			Utils.addArrears(do2, Math.min(Integer.parseInt(do2.getColumnA()), totAccomAmt) 
            		+ Math.min(Integer.parseInt(do2.getColumnB()), totFoodAmt) 
            		+ Math.min(Integer.parseInt(do2.getColumnC()), totTransAmt), 
            		Do2Manager.DAILY, do2.getFromDate(), do2.getToDate());
			/*
			totalDaily = totAccomAmt+totFoodAmt+totTransAmt;
			Utils.addArrears(do2, totalDaily, Do2Manager.DAILY, do2.getFromDate(), do2.getToDate());
			for (Allowance allowance : allowances) {
				dailyAmount = (do2Days * allowance.getRatePercentage());
				totalDaily += Math.round(dailyAmount);
			}
	    }
	   	Utils.addArrears(do2, totalDaily, Do2Manager.DAILY, do2.getFromDate(), do2.getToDate());*/
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRtyojRtyrj(Do2 do2) {
		double do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		if (do2Days >= 180)
			return false;
		return true;
	}
	
	public boolean isValid7b(Do2 do2) {
		if (do2.getColumnB() == null){
			return false;
		}
		if (do2.getColumnB() != null){
			try {
				Integer.parseInt(do2.getColumnB().trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isValid7a(Do2 do2) {
		if (do2.getColumnA() == null){
			return false;
		}
		if (do2.getColumnA() != null){
			try {
				Integer.parseInt(do2.getColumnA().trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isValid7c(Do2 do2) {
		if (do2.getColumnC() == null){
			return false;
		}
		if (do2.getColumnC() != null){
			try {
				Integer.parseInt(do2.getColumnC().trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	public int getGradePay(Do2 do2) {
		int gradePay = 0;
		List<AuditCage> acList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(Utils.getCurrentEmployee(do2.getArmyNo()).getId(),do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery(
				" SELECT ac from AuditCage ac WHERE ac.employee=:emp AND ac.effective=true AND ac.cancelled=false AND " +
				" ac.absentFlag=false AND ac.recordStatus!='P' AND ac.fromDate<=:fromDate ORDER BY ac.fromDate DESC")
				.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
				.setParameter("fromDate", do2.getFromDate()).getResultList();*/

		if (acList.size() > 0) {
			if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
				gradePay = acList.get(0).getOldGradePay();
			} else if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) && !do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
				gradePay = acList.get(0).getGradePay();
			}
		}
		return gradePay;
	}

	public boolean isValidTime(Do2 do2) {
		int hours = 0;
		int minutes = 0;
		if (do2.getTjra() != null) {
			hours = do2.getTjra()/100;
			minutes = do2.getTjra()%100;;
			if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59){
				buffer.append(INVALID_TJRA);
				return false;
			}
		}
		if (do2.getLra() != null) {
			hours = do2.getLra()/100;
			minutes = do2.getLra()%100;;
			if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59){
				buffer.append(INVALID_LRA);
				return false;
			}
		}
		
		
		double departureTime = (((do2.getTjra()/100) * 60.0) + (do2.getTjra()%100)) / 60.0;
		double arrivalTime = (((do2.getLra()/100) * 60.0) + (do2.getLra()%100)) / 60.0;
		if (do2.getFromDate().equals(do2.getToDate()) && (departureTime < arrivalTime)) {
			return true;
		}
		
		if (!do2.getFromDate().equals(do2.getToDate())) {
			return true;
		}

		return false;
	}
	
	
	public boolean isLessThanSixHrs(Do2 do2){
		
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			
		
		double departureTime = (((do2.getTjra()/100) * 60.0) + (do2.getTjra()%100)) / 60.0;
		double arrivalTime = (((do2.getLra()/100) * 60.0) + (do2.getLra()%100)) / 60.0;
		if (do2.getFromDate().equals(do2.getToDate()) && ( (arrivalTime - departureTime) <= 6) ) {
			return true;
		}		
		return false;
		}
		return false;
	}
		
	public boolean isRdailyAvailable(Do2 do2){
		boolean do2available = false;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List <Do2> do2List  = null;
		do2List = do2Repo.findByFromDateAndPreviousDate(employee.getId(), Do2Manager.VALID, "RTYOJ",
															do2.getFromDate(), DateManager.getPreviousDate(do2.getFromDate()));
				
			/*	Utils.getEntityManager().createQuery(
				" SELECT d FROM Do2 d WHERE d.employee=:employee AND d.status=:status AND d.description =:description" +
				" AND (d.toDate =:fromDate or d.toDate =:previousDate) ")
				.setParameter("employee", employee)
				.setParameter("status",	Do2Manager.VALID)
				.setParameter("fromDate", do2.getFromDate())
				.setParameter("description", "RTYOJ")
				.setParameter("previousDate", DateManager.getPreviousDate(do2.getFromDate())).getResultList();*/
		if (do2List.size() > 0) {
			if (do2List.get(0).getToDate().equals(do2.getFromDate())){
				todatesame=true;
			}
			do2available=true;
		}
		if (do2List.size()==0){
			 do2List = do2Repo.findByFromDateAndPreviousDate(employee.getId(), Do2Manager.VALID, "RDAILY",
						do2.getFromDate(), DateManager.getPreviousDate(do2.getFromDate()));
					 
			/*		 Utils.getEntityManager().createQuery(
					" SELECT d FROM Do2 d WHERE d.employee=:employee AND d.status=:status AND d.description =:description " +
					" AND (d.toDate =:fromDate or d.toDate =:previousDate) ")
					.setParameter("employee", employee)
					.setParameter("status",	Do2Manager.VALID)
					.setParameter("fromDate", do2.getFromDate())
			.setParameter("description", "RDAILY")
			.setParameter("previousDate", DateManager.getPreviousDate(do2.getFromDate())).getResultList();*/
		}
		if (do2List.size() > 0) {
			if (do2List.get(0).getToDate().equals(do2.getFromDate())){
				todatesame=true;
			}
			do2available=true;
		} else {
			do2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(employee.getId(),Do2Manager.VALID
																				,"RTYOJ",do2.getFromDate());
					
		/*			Utils.getEntityManager().createQuery(
					" SELECT d FROM Do2 d WHERE d.employee=:employee AND d.status=:status AND d.description =:description" +
					" AND (d.fromDate<:fromDate) order by d.fromDate DESC ")
					.setParameter("employee", employee)
					.setParameter("status",	Do2Manager.VALID)
					.setParameter("fromDate", do2.getFromDate())
					.setParameter("description", "RTYOJ").getResultList();*/
			if (do2List.size() > 0) {
		//		if (DateManager.getNoOfDays(do2.getFromDate(), do2List.get(0).getFromDate())<=5) {      //Commented on 23.09.2020 (Ref:  DSC email 09/2020)
					do2available=true;
		//		}
		
			}
		}
		return do2available;
	}
	
	public boolean isDailyAvailable(Do2 do2){
		boolean do2available = false;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List <Do2> do2List  = null;
		do2List = do2Repo.findByFromDateAndPreviousDate(employee.getId(), Do2Manager.VALID, "TYOJ",
														do2.getFromDate(), DateManager.getPreviousDate(do2.getFromDate()));
				
		/*		Utils.getEntityManager().createQuery(
				" SELECT d FROM Do2 d WHERE d.employee=:employee AND d.status=:status AND d.description =:description" +
				" AND (d.toDate =:fromDate or d.toDate =:previousDate) ")
				.setParameter("employee", employee)
				.setParameter("status",	Do2Manager.VALID)
				.setParameter("fromDate", do2.getFromDate())
				.setParameter("description", "TYOJ")
				.setParameter("previousDate", DateManager.getPreviousDate(do2.getFromDate())).getResultList();*/
		if (do2List.size() > 0) {
			
			do2available=true;
		}
		if (do2List.size()==0){
			 do2List = do2Repo.findByFromDateAndPreviousDate(employee.getId(), Do2Manager.VALID, "DAILY",
															do2.getFromDate(), DateManager.getPreviousDate(do2.getFromDate()));
					 
					 
			/*		 Utils.getEntityManager().createQuery(
					" SELECT d FROM Do2 d WHERE d.employee=:employee AND d.status=:status AND d.description =:description " +
					" AND (d.toDate =:fromDate or d.toDate =:previousDate) ")
					.setParameter("employee", employee)
					.setParameter("status",	Do2Manager.VALID)
					.setParameter("fromDate", do2.getFromDate())
			.setParameter("description", "DAILY")
			.setParameter("previousDate", DateManager.getPreviousDate(do2.getFromDate())).getResultList();*/
		}
		if (do2List.size() > 0) {
			
			do2available=true;
		} else {
			do2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(employee.getId(),Do2Manager.VALID,"TYOJ"
																				,do2.getFromDate());
					
					
			/*		Utils.getEntityManager().createQuery(
					" SELECT d FROM Do2 d WHERE d.employee=:employee AND d.status=:status AND d.description =:description" +
					" AND (d.fromDate<:fromDate) order by d.fromDate DESC ")
					.setParameter("employee", employee)
					.setParameter("status",	Do2Manager.VALID)
					.setParameter("fromDate", do2.getFromDate())
					.setParameter("description", "TYOJ").getResultList();*/
			if (do2List.size() > 0) {
		//		if (DateManager.getNoOfDays(do2.getFromDate(), do2List.get(0).getFromDate())<=5) {   //Commented on 23.09.2020 (Ref:  DSC email 09/2020)
					do2available=true;
		//		}
		
			}
		}
		return do2available;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		List<Arrears> arrearsList = arrRepo.findByDo2Id(refDo2.getId());
				
			//	Utils.getEntityManager().createQuery("SELECT a FROM Arrears a " 
		//		+ " WHERE a.do2=:do2 ").setParameter("do2", refDo2).getResultList();
		
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
		
		List<Do2> dailyDo2List  = getDo2ListForDescription(employee, "RDAILY", tyojDailyCheckDate, do2.getFromDate());
 	
		if (dailyDo2List!=null && dailyDo2List.size()>0) {
 
			tyojDailyCheckDate = DateManager.getDate(dailyDo2List.get(0).getFromDate(),-2);
			List<Do2> tyojDo2List = getDo2ListForDescription(employee, "RTYOJ", tyojDailyCheckDate, do2.getFromDate());
			if (tyojDo2List!=null && tyojDo2List.size()>0) {
				tyojDo2 = tyojDo2List.get(0);
			} else {
				List<Do2> alDo2List = getDo2ListForDescription(employee, "%AL", tyojDailyCheckDate, do2.getFromDate());
				if (alDo2List!=null && alDo2List.size()>0) {
					tyojDo2 = alDo2List.get(0);
			 	} else {
			 		tyojDo2 = dailyDo2List.get(0);   // cases where no RTYOJ/AL/PAL/BAL exists
			 	}
			}
		} else {            // cases where only RTYOJ/RTYRJ exists
			List<Do2> tyojDo2List = getDo2ListForDescription(employee, "RTYOJ", tyojDailyCheckDate, do2.getFromDate());
			if (tyojDo2List!=null && tyojDo2List.size()>0) {
				tyojDo2 = tyojDo2List.get(0);
		 	} else {
		 		tyojDo2 = do2;                          // cases where no RDAILY/RTYOJ exists --- to check RTYRJ exceeding 14/21 days.
		 	}
		}
	 
		if (tyojDo2!=null) {
			toDate = do2.getToDate();
			if (do2.getLra() !=null && ((((do2.getLra()/100) * 60.0) + (do2.getLra()%100)) / 60.0)<6) {
					toDate=DateManager.getPreviousDate(do2.getToDate());
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
						
		List<AuditCage> auditCageList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(employee.getId(), do2.getFromDate());
				
				
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
					String description = tptldo2.getDescription();
					double rate        = tptldo2.getRate();
					Do2	 tptlEarlierGrantDo2 = tptldo2;
					String columnC = tptldo2.getColumnC();
					generatetptlCeaseDo2(do2, tptlCeasedDate, description);
				    generateTptlGrantDo2(do2, description, rate, tptlEarlierGrantDo2, columnC);
				}
			}
		}
		
		List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(baseAuditCage.getId());
				
		/*		Utils.getEntityManager().createQuery("SELECT aca FROM AuditCageAllowance aca " +
												" WHERE aca.auditCage=:baseAuditCage")
												.setParameter("baseAuditCage", baseAuditCage).getResultList();*/
		for (AuditCageAllowance aca : acaList) {
			
			if (tyojDo2!=null && temporaryDutyDays>14 && temporaryDutyDays<=21) {
				if (Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyFourteenDays().contains(aca.getPayCode().getPayCode())) {
					List<Do2> do2List  = do2Repo.findByFromDateBetweenAndListDescriptionsAndColumnA(employee.getId(),
							Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyFourteenDays(),
							"CEASED",ceasedDo2FromDate,tyojDo2.getToDate());
							
				/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee "
						+ " AND (d.status='V' OR d.status='P') AND d.description in (:descriptions) AND d.columnA='CEASED' AND " 
						+ " d.fromDate BETWEEN :fromDate AND :toDate")
						.setParameter("employee", employee)
						.setParameter("fromDate", ceasedDo2FromDate)
						.setParameter("toDate", tyojDo2.getToDate())
						.setParameter("descriptions", Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyFourteenDays())
						.getResultList();*/
				
					if (do2List.size()==0) {
						generateAllowanceCeaseDo2(tyojDo2, aca.getPayCode().getPayCode(), ceasedDo2FromDate, employee, refDo2No);
					}
				}
			} else if (tyojDo2!=null) {
				if (Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyTwentyoneDays().contains(aca.getPayCode().getPayCode())) {
					List<Do2> do2List  = do2Repo.findByStatusDescriptionColAAndFromDateBetween(employee.getId(),
							Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyTwentyoneDays(), "CEASED",
							ceasedDo2FromDate, tyojDo2.getToDate());
							
				/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee "
						+ " AND (d.status='V' OR d.status='P') AND d.description in (:descriptions) AND d.columnA='CEASED' AND " 
						+ " d.fromDate BETWEEN :fromDate AND :toDate ")
						.setParameter("employee", employee)
						.setParameter("fromDate", ceasedDo2FromDate)
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
	
	public  void generateAllowanceCeaseDo2(Do2 do2, String description, Date ceaseDo2Date, Employee employee, String refDo2No) {
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
				
			/*	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee "
				+ " AND d.status='V' AND d.description like :description AND d.toDate BETWEEN :fromDate1 AND :fromDate2 " 
				+ " ORDER BY d.toDate DESC")
				.setParameter("employee", employee)
				.setParameter("fromDate1", fromDate)
				.setParameter("fromDate2", toDate)
				.setParameter("description", description)
				.getResultList();*/

		return do2List;
	}
	
	public Date getCeasedDo2FromDate(Do2 do2) {
		Date ceasedDo2FromDate = do2.getFromDate();
		if (do2.getTjra()!=null && ((((do2.getTjra()/100) * 60.0) + (do2.getTjra()%100)) / 60.0)<18) {
			ceasedDo2FromDate=do2.getFromDate();
		} else {
			ceasedDo2FromDate=DateManager.getNextDate(do2.getFromDate());
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
	//		Utils.getEntityManager().persist(tptlCeaseDo2);
		//	Utils.getEntityManager().flush();				
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
/*		   List<Do2> do2List = do2Repo.findByDescStatusColAFromDateEqual(Utils.getCurrentEmployee(do2.getArmyNo()).getId(),
				   															Do2Manager.VALID, description, columnA, fromDate);
	*/			   
		/*		   Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:employee and d.description=:description and d.fromDate=:fromDate and d.columnA=:columnA and d.status=:status")
		             .setParameter("employee", Utils.getCurrentEmployee(do2.getArmyNo()))
		             .setParameter("description",description)
		             .setParameter("fromDate",fromDate)
		             .setParameter("columnA", columnA)
		             .setParameter("status", Do2Manager.VALID)
		             .getResultList();*/
		List<Do2> do2List = do2Repo.findByDescColAFromDateEqualStatusPendingOrValid(do2.getEmployee().getId(), description, columnA, fromDate);                           
		return do2List.size() > 0;
	}
	  
	public Do2 getTransportAllceDo2(Employee employee, Date fromDate){
		  List<Do2> tptlList = do2Repo.findByFromDateLessThanEqualToAndColAAndMultipleDescriptionsOrderByFromDateDesc(employee.getId(),
				  														fromDate, Do2Manager.VALID, Do2Manager.GRANTED, Do2Manager.TPTL, Do2Manager.TPTH);
				  
				/*  Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:employee and " +
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