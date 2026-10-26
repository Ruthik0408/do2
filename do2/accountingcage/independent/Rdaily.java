package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import java.util.List;
import java.util.Date;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.InvalidDateRangeException;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC, Secunderabad
 * 
 */

@RestController
public class Rdaily extends AccountingCageBase implements java.io.Serializable {

	 
	String message;
	static public final String INVALID_PUBLICATION = "AU:Published before Revised Appendix J Impl date.\n";
	static public final String INVALID_TYOJ = "AU:Cannot be combined with TYOJ.\n";
	static public final String INVALID_RTYOJ = "AU:Corresponding RTYOJ not available.\n";
	static public final String INVALID_TJRA = "VL:Arrival time not mentioned in TJRA or incorrect. \n ";
	static public final String INVALID_LRA = "VL:Departure time not mentioned in LRA or incorrect. \n ";
	static public final String INVALID_TIME = "VL:Invalid Departure/Arrival time in LRA/TJRA.\n ";
	static public final String INVALID_PERIOD = "VL:Daily period is more than 180 days.\n";
	static public final String INVALID_COL7A = "VL:Accommodation amount to be shown in Col-7A \n ";
	static public final String INVALID_COL7B = "VL:Food amount to be shown in Col-7B \n ";
	static public final String INVALID_COL7C = "VL:Transport within city amount to be shown in Col-7C \n ";
	public double totalDaily = 0.0, foodAmount = 0.0, transportAmount = 0.0, accommodationAmount = 0.0;
//	public Integer accommodationAmount = 0;
	public double totAccomAmt=0.0, totFoodAmt = 0.0, totTransAmt= 0.0;
	Date sevencpcDate = DateManager.getSeventhCpcDate();
	
    private boolean todatesame = false;
	private StringBuffer buffer = new StringBuffer();
	@Override
	public boolean isValid(Do2 do2) {
		System.out.println ( " in RDAILY " +do2.getDescription()  + " ano " +do2.getArmyNo()   +" xxx " +do2.getFromDate());
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
		if (!isValid7a(do2)) {
			validity = false;
			buffer.append(INVALID_COL7A);
		}
		if (!isValid7b(do2)) {
			validity = false;
			buffer.append(INVALID_COL7B);
		}
		if (!isValid7c(do2)) {
			validity = false;
			buffer.append(INVALID_COL7C);
		}
		if (!isValidDaily(do2)) {
			validity = false;
			buffer.append(INVALID_PERIOD);
		}
		
		/*if (!isRtyojAvailable(do2)) { 
			validity = false;
		    do2.setStatus(Do2Manager.PENDING); 
		    buffer.append(INVALID_RTYOJ); 
		}
		if (isTyojAvailable(do2)) { 
			validity = false;
		    do2.setStatus(Do2Manager.INVALID); 
		    buffer.append(INVALID_TYOJ); 
		}*/

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
	public String process(Do2 do2) {
		System.out.println ( " iam here " +do2.getFromDate()    +" xxxxx " +do2.getArmyNo());
		double do2Days = 0.0;
//		double totalDaily = 0.0, foodAmount = 0.0, accommodationAmount = 0.0, transportAmount = 0.0;
		if (isValid(do2)) {
			System.out.println ( " iam here xxxx " +do2.getFromDate()    +" xxxxx " +do2.getArmyNo());
			if (todatesame){
				if (do2.getToDate().after(do2.getFromDate())){
					do2Days = DateManager.getNoOfDays(DateManager.getNextDate(do2.getFromDate()), do2.getToDate());
				}
			} else {System.out.println ( " iam here yyyy " +do2.getFromDate()    +" xxxxx " +do2.getArmyNo());
				
				do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
			}

				//getGradePay(do2);
			System.out.println ( " iam here zzzz " +do2.getFromDate()    +" xxxxx " +do2.getArmyNo());
			List<Allowance> allowances = Utils.getApplicableAllowanceRates(
					do2.getDescription(), getGradePay(do2), do2.getFromDate(), do2.getToDate());
			
			System.out.println ( " allowance size " +allowances.size());
			
			
			if (allowances.size() > 0) {
				if(allowances.get(0).getPayEndRange() > 0 ) {
					if (Integer.parseInt(do2.getColumnA()) < allowances.get(0).getPayEndRange()) {
						accommodationAmount = Integer.parseInt(do2.getColumnA());
					} else {
						accommodationAmount = allowances.get(0).getPayEndRange();
						if (!do2.getFromDate().before(DateManager.getSeventhCpcDaFiftyPercentDate())) {
							accommodationAmount+=0.5;       // since allowance_rate table data type for pay_end_range column is integer, 0.5 is added separately
						}
					}
				}
				
							
				if(allowances.get(0).getRatePercentage() > 0 ) {
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
				System.out.println ( " do2 days " +do2Days   +" accomamt " +accommodationAmount   +" totfood amt " +foodAmount   +" tot transport amount " +transportAmount);

				
				
				totAccomAmt = (int) Math.round(do2Days * accommodationAmount);
				totFoodAmt  = (int) Math.round(do2Days * foodAmount);
				totTransAmt =  (int) Math.round(do2Days * transportAmount);
				
				totalDaily = totAccomAmt+totFoodAmt+totTransAmt;
				
				System.out.println ( " do2 days " +do2Days   +" TOTaccomamt " +totAccomAmt   +" totfood amt " +totFoodAmt   +" tot transport amount " +totTransAmt);
			//	Utils.addArrears(do2, totalDaily, Do2Manager.DAILY, do2.getFromDate(), do2.getToDate());
				
				
			/*	if (allowances.get(0).getPayEndRange() > 0) {
					accommodationAmount = (int) Math.round(do2Days * allowances.get(0).getPayEndRange());
				}
				
				
				
				if (allowances.get(0).getRatePercentage() > 0.0) {
					foodAmount = Math.round(do2Days * allowances.get(0).getRatePercentage());
			
				}
				
				if (allowances.get(0).getRateAmount() > 0.0) {
					transportAmount = Math.round(do2Days * allowances.get(0).getRateAmount());
			
				}*/
	
				
			}
		  // code commented on 26.11.2015
			
          /*if (Integer.parseInt(do2.getColumnA()) < accommodationAmount){
            	totalDaily += Integer.parseInt(do2.getColumnA());
            } else {
            	totalDaily += accommodationAmount;
            }
            if (Integer.parseInt(do2.getColumnB()) < foodAmount){
            	totalDaily += Integer.parseInt(do2.getColumnB());
            } else {
            	totalDaily += foodAmount;
            }
            if (Integer.parseInt(do2.getColumnC()) < transportAmount){
            	totalDaily += Integer.parseInt(do2.getColumnC());
            } else {
            	totalDaily += transportAmount;
            }
     		Utils.addArrears(do2, totalDaily, Do2Manager.DAILY, do2.getFromDate(), do2.getToDate());*/
           
			// code changed on 26.11.2015
            Utils.addArrears(do2, Math.min(Integer.parseInt(do2.getColumnA()), totAccomAmt) 
            		+ Math.min(Integer.parseInt(do2.getColumnB()), totFoodAmt) 
            		+ Math.min(Integer.parseInt(do2.getColumnC()), totTransAmt), 
            		Do2Manager.DAILY, do2.getFromDate(), do2.getToDate());
		}
		
		return Do2Manager.SUCCESS;
	}

	
	public boolean isValid7a(Do2 do2) {
		if (do2.getColumnA() != null){
			try {
				Integer.parseInt(do2.getColumnA().trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	public boolean isValid7b(Do2 do2) {
		if (do2.getColumnB() != null){
			try {
				Integer.parseInt(do2.getColumnB().trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isValid7c(Do2 do2) {
		if (do2.getColumnC() != null){
			try {
				Integer.parseInt(do2.getColumnC().trim());
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	public boolean isValidDaily(Do2 do2) {
		double do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		if (do2Days >= 180)
			return false;
		return true;
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
		
		double arrivalTime = (((do2.getTjra()/100) * 60.0) + (do2.getTjra()%100)) / 60.0;
		double departureTime = (((do2.getLra()/100) * 60.0) + (do2.getLra()%100)) / 60.0;
		if (do2.getFromDate().equals(do2.getToDate()) && (arrivalTime < departureTime )) {
			return true;
		}
		if (!do2.getFromDate().equals(do2.getToDate())) {
			return true;
		}

		return false;
	}

	public boolean isRtyojAvailable(Do2 do2) {
		List<Do2> do2List = do2Repo.findByToDateEqualToFromDateAndToDateEqualToPreviousDateAndDescriptionAndStatus(do2.getEmployee().getId(),
																			Do2Manager.VALID, "RTYOJ", do2.getFromDate(), DateManager.getPreviousDate(do2.getFromDate()));
				
	/*			Utils.getEntityManager().createQuery(
						" SELECT d FROM Do2 d WHERE d.employee=:emp AND d.status=:status AND d.description =:description " +
						" AND (d.toDate =:fromDate or d.toDate =:previousDate) ")
				.setParameter("emp", do2.getEmployee())
				.setParameter("status", Do2Manager.VALID)
				.setParameter("fromDate", do2.getFromDate())
				.setParameter("description", "RTYOJ")
				.setParameter("previousDate", DateManager.getPreviousDate(do2.getFromDate()))
				.getResultList();*/

		if (do2List.size() > 0) {
			if (do2List.get(0).getToDate().equals(do2.getFromDate())){
				todatesame=true;
			}
			return true;
		}
		return false;
	}
	public boolean isTyojAvailable(Do2 do2) {
		List<Do2> do2List = do2Repo.findByToDateEqualToFromDateAndToDateEqualToPreviousDateAndDescriptionAndStatus(do2.getEmployee().getId(),
																		Do2Manager.VALID, "TYOJ", do2.getFromDate(), DateManager.getPreviousDate(do2.getFromDate()));
				
		/*		Utils.getEntityManager().createQuery(
				" SELECT d FROM Do2 d WHERE d.armyNo=:armyNo AND d.status=:status AND d.description =:description " +
				" AND (d.toDate =:fromDate or d.toDate =:previousDate) ")
		.setParameter("armyNo", do2.getArmyNo())
		.setParameter("status", Do2Manager.VALID)
		.setParameter("fromDate", do2.getFromDate())
		.setParameter("description", "TYOJ")
		.setParameter("previousDate", DateManager.getPreviousDate(do2.getFromDate()))
		.getResultList();*/

		if (do2List.size() > 0) {
			return true;
		}
		return false;
	}

	public int getGradePay(Do2 do2) {
		int gradePay = 0;
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> acList = acRepo.findByFromDateLessThanEqualOrderByFromDateDesc(emp.getId(), do2.getFromDate());
				
		System.out.println ( " ac list size " +acList.size());
		/*		Utils.getEntityManager().createQuery(
				" SELECT ac from AuditCage ac WHERE ac.employee=:emp AND ac.effective=true AND ac.cancelled=false AND " +
				" ac.absentFlag=false AND ac.recordStatus!='P' AND ac.fromDate<=:fromDate ORDER BY ac.fromDate DESC")
				.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
				.setParameter("fromDate", do2.getFromDate()).getResultList();*/
		optcpcDo2For7cpcExists(do2);
		
		if (acList.size() > 0) {
			if (!do2.getFromDate().before(sevencpcDate)) {
				gradePay = acList.get(0).getOldGradePay();
			} else if (do2.getFromDate().before(sevencpcDate) && !do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
				System.out.println ( " do2 from date " +do2.getFromDate());
				gradePay = acList.get(0).getGradePay();
				System.out.println ( " grade pay" +do2.getFromDate());
			}
		}
		return gradePay;
	}

	public boolean optcpcDo2For7cpcExists(Do2 do2){
		List<Do2> do2List =do2Repo.findByDescColB(do2.getEmployee().getId(),"V","OPTCPC","SEVENPC");
				
			//	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status='V' and d.description='OPTCPC' "
		//			+ " AND d.columnB='SEVENPC' ").setParameter("emp",do2.getEmployee()).getResultList();
		
		if (do2List.size()>0){
		   	sevencpcDate = do2List.get(0).getFromDate();
		   	return true;
		}
		return false;
	}
	
}
