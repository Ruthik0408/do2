package itsdc.dolphin.do2.accountingcage.dependent;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.SoldierGroup;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:11-08-2009
 */

@RestController
public class Htinc extends AccountingCageBase implements java.io.Serializable {

	
	 
	String message;
	private Calendar fromDateCal = new GregorianCalendar();
	private Calendar toDateCal = new GregorianCalendar();
	private boolean validity = true;
	private boolean firstValidity = true;
	private StringBuffer buffer = new StringBuffer();
	private double rate = 0;
	private double do2RateAmount = 0;
	private Employee employee;
	private SoldierGroup solGroup;
	static public final String INCORRECT_FROM_DATE ="VL: Date should be the first of the month.\n";
	static public final String INCORRECT_TO_DATE = "VL: To be published quarterly in arrears.\n";
	static public final String INVALID_RATE_COLUMN = "VL:Amount in rate column is incorrect. \n";
	static public final String ALREADY_DRAWN = "AU:HINDI increment already adjusted for a year. \n";
	static public final String FIRST_NOT_ADJUSTED = "AU:Increment DO2 with 'FIRST' in Column-B not yet adjusted.\n";
	static public final String OVERLAPPING_CLAIM = "OL:Claim is overlapping with already adjusted one.\n";

	@Transactional	

	// TO CHECK THE VALIDITY OF THE DOII
	@Override
	public boolean isValid(Do2 do2) {
		
	    boolean do2Validity = true;
	    fromDateCal.setTime(do2.getFromDate());
	    toDateCal.setTime(do2.getToDate());
	    
		if (!isValidFromDate(do2)) {
			buffer.append(INCORRECT_FROM_DATE);
			do2Validity = false;
		}
		 
		if (!isValidToDate(do2)) {
			buffer.append(INCORRECT_TO_DATE);
			do2Validity = false;
		}
		 
		if (!isOverlapping(do2)) {
			buffer.append(OVERLAPPING_CLAIM);
			do2Validity = false;
		}
	 
		
		if (!whetherValidColumnB(do2)) {
			buffer.append(ALREADY_DRAWN);
			do2Validity = false;
		}
		 
		if ((do2.getFromDate().before(DateManager.getSixthCpcDate()))) {
		 		if (!isValidAmountInRateColumn(do2)) {
					buffer.append(INVALID_RATE_COLUMN);
					do2Validity = false;
				}
			do2RateAmount = do2.getRate();
		}
			
		if(!firstValidity) {
			buffer.append(FIRST_NOT_ADJUSTED);
			do2.setStatus(Do2Manager.PENDING);
		 	validity = false;
		}
		
		if (!do2Validity) {
			if (do2.getStatus()!=null && !do2.getStatus().equals(Do2Manager.PENDING))
				do2.setStatus(Do2Manager.INVALID);
			validity = false;
		}
		
		message = buffer.toString();	
		do2.setReason(message);
		
		if (do2Validity) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
		
		
		do2Repo.save(do2);
	
		return do2Validity;
	}
	
	// PROCESS OF VALID DOII
	@Override
	public String process(Do2 do2) {
		
		if (do2.getStatus()!=null && do2.getStatus().equals(Do2Manager.VALID))
		  	Utils.addArrears(do2, (do2RateAmount * DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate())));
	
		return Do2Manager.SUCCESS;
	}	
	
	// Method to check whether from date in current DO2 is a valid one i.e. first of the month
	public boolean isValidFromDate(Do2 do2) {
		return (fromDateCal.get(Calendar.DATE)==1); 
	}

	// Method to ensure that to-date in current DO2 is the last date of the third month from the from date
	public boolean isValidToDate(Do2 do2) {
		int days = DateManager.getNoOfDaysInMonth(toDateCal.get(Calendar.MONTH)+1, toDateCal.get(Calendar.YEAR));
	 
		if (toDateCal.get(Calendar.DATE)==days) {
			if (fromDateCal.get(Calendar.MONTH) < 10) {
				if ((fromDateCal.get(Calendar.MONTH)+2)==(toDateCal.get(Calendar.MONTH))) {
					return true;
				} 
			}
			if (fromDateCal.get(Calendar.MONTH)==10 && toDateCal.get(Calendar.MONTH)==0) {
				return true;
			}
			if (fromDateCal.get(Calendar.MONTH)==11 && toDateCal.get(Calendar.MONTH)==1) {
				return true;
			}
		}
		
		return false;
		
	}

	// Method to check overlapping of current DO2 with already adjusted DOs2 and other mutually exclusive items
	private boolean isOverlapping(Do2 do2) {
		boolean overlapValidity = false;
		validity = true;
		List<String> mutexNames = Arrays.asList(new String[] { "HPINC", "HSINC", "HTINC" });
		try {
		overlapValidity = Utils.isMutexAllowance(do2.getArmyNo(), mutexNames, do2.getFromDate(), do2.getToDate(),do2);
		}
		catch (Exception e) {
			Utils.handleException(e);
		}
		 
		if (overlapValidity) {
			validity = false;
		} else {
			validity = true;
		}
		return validity;
	}

	// Method to get increment amount @3% w.e.f. 1.1.06.
	private double getIncrAmount(Do2 do2) {

		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> acList =acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(emp.getId(), do2.getFromDate());
				
	/*			Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac " +
								" WHERE ac.employee=:emp AND ac.effective=true AND ac.cancelled=false AND ac.absentFlag=false AND " +
								" ac.recordStatus!='P' AND ac.fromDate<=:do2FromDate order by ac.fromDate DESC")
								.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
								.setParameter("do2FromDate",do2.getFromDate())
								.getResultList();*/
	
			if (acList.size() > 0) {	
				rate = ((acList.get(0).getBasicPay() + acList.get(0).getGradePay() + acList.get(0).getXGroupPay()) * 0.03);
						if ((rate % 10) < 1) {
							rate = (((int) (rate / 10)) * 10);
						} else {
							rate = ((((int) (rate / 10)) * 10) + 10);	
						}
					}
			return rate;
			}

	// Method to check whether amount given in rate column of the DOII is a
	// valid one (PRE-6CPC)
	private boolean isValidAmountInRateColumn(Do2 do2) {
		Rank rank = null;
		String soldierGroup = "";
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		validity = true;
		GroupCategory groupCategory = Utils.getGroupCategory(do2, "CUR");
		
		if (groupCategory==null) {
			return false;
		} else {
			soldierGroup = groupCategory.getSoldierGroup();
		}
					
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");

		List<PayScale> psList = psRepo.findByRankAndSoldierGroup(rank.getId(), solGroup.getSoldierGroup());
				
		/*		Utils
				.getEntityManager()
				.createQuery("SELECT ps from PayScale ps where (ps.rank = :rank AND ps.soldierGroup = :soldierGroup)")
				.setParameter("rank", rank).setParameter("soldierGroup", solGroup).getResultList();*/
	 		
		if (((double) psList.get(0).getIncr0()) == do2.getRate()) {
			return validity = true;
		} else {
			return validity = false;
		}
	}
	
	private boolean whetherValidColumnB(Do2 do2) {
		validity = true;
		if ((do2.getColumnB()!=null) && (do2.getColumnB().equals("FIRST"))) {
			
			do2RateAmount = getIncrAmount(do2);
			System.out.println("    HINDI INCR  in CoulB:   "+do2RateAmount);
		 	do2.setRate(do2RateAmount);
			return validity = true;
		}
		else {
			if ((do2.getColumnB().equals("SECOND")) || (do2.getColumnB().equals("THIRD")) 
					|| (do2.getColumnB().equals("FOURTH")))    {
			 return validity = checkEarlierReferenceForColumnB(do2);
			}
			else {
			return validity = false;
			}
		}
	}
	
	private boolean checkEarlierReferenceForColumnB(Do2 do2) {
		employee =Utils.getCurrentEmployee(do2.getArmyNo());
		validity = true;
		List<Do2> oldDo2List = do2Repo.findByDescColB(employee.getId(), Do2Manager.VALID, do2.getDescription(), "FIRST");
				
/*			Utils.getEntityManager()
		.createQuery("SELECT do2 from Do2 do2 where ((do2.employee =:employee) and (do2.description = :description) " +
				" and (do2.columnB = :columnB) and (do2.status = :status))")
		.setParameter("employee", employee)
		.setParameter("description",do2.getDescription())
		.setParameter("columnB", "FIRST")
		.setParameter("status", Do2Manager.VALID)
		.getResultList();*/
		
		if (oldDo2List.size() > 0) {
		for (Do2 oldDo2 : oldDo2List) {
			fromDateCal.setTime(oldDo2.getFromDate()); 		// variable 'fromDateCal' being used for old Do2
			fromDateCal.set(Calendar.YEAR, fromDateCal.get(Calendar.YEAR)+1);
				if ((oldDo2.getFromDate().before(do2.getFromDate())) && 
						(do2.getToDate().before(fromDateCal.getTime()))) {
					do2RateAmount = oldDo2.getRate();
				return validity = true;	
				}
				else {
					return validity = false;
				}
			}
		fromDateCal.setTime(do2.getFromDate());			// restoring the original value in 'fromDateCal'
		}
		else {
		firstValidity = false;
		}
		return validity;
	}
}


