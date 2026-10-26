package itsdc.dolphin.do2.accountingcage.independent;
 
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:28-07-2009
 */

@RestController
public class Rtf extends AccountingCageBase implements java.io.Serializable {

	String message;
	private Calendar fromDateCal = new GregorianCalendar(), toDateCal = new GregorianCalendar();
	private boolean validity = true, childrenValidity = true;
	private double do2Amount=0;
 	private List<String> mutexNames = Arrays.asList("CEA", "HOSSUB", "CEAHC", "CEATWN", "HSBTWN");
	private StringBuffer buffer = new StringBuffer();
	static public final List<String> VALID_COL_ABC_VALUES = 	  Arrays.asList("CLASS01-1","CLASS01-2","CLASS01-3",
																				"CLASS02-1","CLASS02-2","CLASS02-3",
																				"CLASS03-1","CLASS03-2","CLASS03-3",
																				"CLASS04-1","CLASS04-2","CLASS04-3",
																				"CLASS05-1","CLASS05-2","CLASS05-3",
																				"CLASS06-1","CLASS06-2","CLASS06-3",
																				"CLASS07-1","CLASS07-2","CLASS07-3",
																				"CLASS08-1","CLASS08-2","CLASS08-3",
																				"CLASS09-1","CLASS09-2","CLASS09-3",
																				"CLASS10-1","CLASS10-2","CLASS10-3",
																				"CLASS11-1","CLASS11-2","CLASS11-3",
																				"CLASS12-1","CLASS12-2","CLASS12-3"); 
			
 	public static final String TO_CLAIM_UNDER_NEW_SCHEME = "VL:To be claimed under Children Education Assistance scheme after 31.08.2008.  \n";
	public static final String INCORRECT_FROM_DATE = "VL:From date should be first day of the month. \n";
	public static final String INCORRECT_TO_DATE = "VL:To date should be last day of the month.  \n ";
	public static final String INVALID_COLUMN_ABC = "VL:Column 7A/7B/7C should contain values like \"CLASS01-1\" OR \"CLASS09-2\" .... \n";
	public static final String MORE_THAN_THREE_CHILDREN = "AU:Claim for more than three children not admissible. \n";
	public static final String INVALID_CLAIM = "DU:Already adjusted for three children during the period, hence current claim rejected. \n";
	public static final String OVERLAP_DESCRIPTION = "OL:Current claim overlaps with already adjusted CEA/HOSSUB. \n";
    public static final String INCORRECT_AMOUNT_IN_RATE_COL = "VL:Incorrect amount mentioned in Rate column of the DO2.  \n";
	 
	
// TO CHECK THE VALIDITY OF THE DOII
    @Transactional
    @Override
	public boolean isValid(Do2 do2) {
 
	 	validity = true;
		fromDateCal.setTime(do2.getFromDate());
		toDateCal.setTime(do2.getToDate());
	 
		if (!isValidPeriod(do2)) {
			buffer.append(TO_CLAIM_UNDER_NEW_SCHEME);
			validity =  false;
		}

		if (!isValidFromDate(fromDateCal)) {
			buffer.append(INCORRECT_FROM_DATE);
			validity =  false;
		}

		if (!isValidToDate(toDateCal)) {
			buffer.append(INCORRECT_TO_DATE);
			validity =  false;
		}
		
		if (!isValidColABC(do2)) {
	 		buffer.append(INVALID_COLUMN_ABC);
	 		validity = false;
		}
		
		if (!isOverlapping(do2)) {
			buffer.append(OVERLAP_DESCRIPTION);
			validity =  false;
		} 
	 
		if (!buffer.toString().contains(INVALID_COLUMN_ABC)) {
			if ((getNoOfChildren(do2.getColumnA()) + (getNoOfChildren(do2.getColumnB())) 
					+ (getNoOfChildren(do2.getColumnC()))) > 3) {  							// in current do2
				buffer.append(MORE_THAN_THREE_CHILDREN);
				childrenValidity = false;
				validity = false;
			}

			if (!isValidClaim(do2)) { 														// total No. of children including old Do2
				if (childrenValidity)
					buffer.append(INVALID_CLAIM);
				validity = false;
			}

			if (!isValidAmountInRateColumn(do2)) {
				buffer.append(INCORRECT_AMOUNT_IN_RATE_COL);
				validity = false;
			}
		}
		
		if (validity){
			do2.setStatus(Do2Manager.VALID);
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}else {
			do2.setStatus(Do2Manager.INVALID);
		}
		message = buffer.toString();
		do2.setReason(message);
		do2Repo.save(do2);
		return validity;
	}

// PROCESS OF VALID DOII
    @Override
	public String process(Do2 do2) {
	  	Utils.addArrears(do2, (do2Amount*(DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()))));
		return Do2Manager.SUCCESS;
	}
	
// TO CHECK THE VALID DATE
	private boolean isValidPeriod(Do2 do2) {
	
		if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())
				&& !do2.getToDate().before(DateManager.getSixthCpcAllowanceDate())) 
			return false;
		 else 
			return true;
		 
	}

// TO CHECK THE VALID FIRST DATE (FROM DATE)
	private boolean isValidFromDate(Calendar fromDateCal) {

		if (fromDateCal.get(Calendar.DATE) > 1) 
			return false;
		else 
			return true;
	}

// TO CHECK THE VALID LAST DATE (TO DATE)
	private boolean isValidToDate(Calendar toDateCal) {
		 	
		if (toDateCal.get(Calendar.DATE) == DateManager.getNoOfDaysInMonth(toDateCal.get(Calendar.MONTH)+1,toDateCal.get(Calendar.YEAR))) 
			return true;
		else 
			return false;
	}
	
// TO CHECK WHETHER DATA IN COLUMN A, B, C  IS  VALID	
	private boolean isValidColABC(Do2 do2) {
		
		boolean colABCvalidity = false;
	 	
		if ((do2.getColumnA()==null || do2.getColumnA().trim().length()==0) 
				&& (do2.getColumnB()==null || do2.getColumnB().trim().length()==0)
				&& (do2.getColumnC()==null || do2.getColumnC().trim().length()==0)) {
			return false;
		}
		

		if (do2.getColumnA()!=null && do2.getColumnA().trim().length()==9 && VALID_COL_ABC_VALUES.contains(do2.getColumnA())) {
			if (do2.getColumnB()!=null && do2.getColumnB().trim().length()==9 && VALID_COL_ABC_VALUES.contains(do2.getColumnB())) {
				if (do2.getColumnC()!=null && do2.getColumnC().trim().length()==9 && VALID_COL_ABC_VALUES.contains(do2.getColumnC())) {
					colABCvalidity = true;	
				} else {
					if (do2.getColumnC()==null || do2.getColumnC().trim().length()==0) {
						colABCvalidity = true;	
					} else {
						colABCvalidity = false;
					}
				}
			} else {
				if (do2.getColumnB()==null || do2.getColumnB().trim().length()==0) {
					colABCvalidity = true;	
				} else {
					colABCvalidity = false;
				}
			}
		} else {
			if (do2.getColumnA()==null || do2.getColumnA().trim().length()==0) {
				if (do2.getColumnB()!=null && do2.getColumnB().trim().length()==9 && VALID_COL_ABC_VALUES.contains(do2.getColumnB())) {
					if (do2.getColumnC()!=null && do2.getColumnC().trim().length()==9 && VALID_COL_ABC_VALUES.contains(do2.getColumnC())) {
						colABCvalidity = true;	
					} 
					if (do2.getColumnC()==null || do2.getColumnC().trim().length()==0) {
						colABCvalidity = true;	
					} else {
						colABCvalidity = false;
					}
				} else {
					if (do2.getColumnB()==null || do2.getColumnB().trim().length()==0) {
						if (do2.getColumnC()!=null && do2.getColumnC().trim().length()==9 && VALID_COL_ABC_VALUES.contains(do2.getColumnC())) {
							colABCvalidity = true;	
						} else {
							colABCvalidity = false;	
						}
					}
				}
			}
		}
				 	
 		return colABCvalidity;
 				  	
	}
	
// TO CHECK THE OVERLAPPLING OF DOII
	private boolean isOverlapping(Do2 do2) {
		
		if (Utils.isMutexAllowance(do2.getArmyNo(), mutexNames, fromDateCal.getTime(), toDateCal.getTime(),do2))
			return false;
		 
		return true;
	}
		


// TO ENSURE THAT No. OF CHILDREN DO NOT EXCEED THREE TAKING INTO ACCOUNT ALREADY ADJUSTED CLAIMS
	private boolean isValidClaim(Do2 do2) {
		int totalChildrenCurrentDo2 = 0, totalChildrenOldDo2 = 0;
	
		totalChildrenCurrentDo2 = ((getNoOfChildren(do2.getColumnA()) + (getNoOfChildren(do2.getColumnB())) 
										+ (getNoOfChildren(do2.getColumnC()))));
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> do2List = do2Repo.findByFromDateBetweenAndToDateBetweenStatusAndDesc(emp.getId(),"RTF",Do2Manager.VALID,do2.getFromDate(),do2.getToDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE (d.armyNo=:armyNo" 
										+ " AND d.description = :rtf AND UPPER(d.status)=:status) "
										+ " AND ((:fromDate between d.fromDate AND d.toDate ) "
										+ " OR (:toDate between d.fromDate AND d.toDate))")
										.setParameter("armyNo", do2.getArmyNo())
										.setParameter("rtf", "RTF") 
										.setParameter("status",	Do2Manager.VALID)
										.setParameter("fromDate", do2.getFromDate())
										.setParameter("toDate", do2.getToDate()).getResultList();*/
		if (do2List.size() > 0) {
			for (Do2 oldDo2 : do2List)  
				totalChildrenOldDo2 += 	((getNoOfChildren(oldDo2.getColumnA()) + (getNoOfChildren(oldDo2.getColumnB())) 
												+ (getNoOfChildren(oldDo2.getColumnC()))));
		}
	
		if ((totalChildrenCurrentDo2 + totalChildrenOldDo2) > 3)
			return false;
		else 
			return true;
	}


	// TO GET THE NO OF CHILDREN
	private int getNoOfChildren(String column) {
		if (column!=null && column.length()!=0 && column.length()==9) {
			return Integer.parseInt((column.substring(8,9)));
		}
		return 0;
	}
// TO CHECK WHETHER THE AMOUNT ARRIVED FROM THE ABOVE PROCEDURE IS TALLIED 
// WITH GIVEN RATE IN THE DOII

	private boolean isValidAmountInRateColumn(Do2 do2) {
		do2Amount = 0;
		boolean rateValidity = true;
	
		do2Amount = (getColumnAmount(do2.getColumnA()) + getColumnAmount(do2.getColumnB())+ getColumnAmount(do2.getColumnC()));
	
		if (do2.getRate() <= (do2Amount + 50)) {
			if (do2.getRate() < do2Amount) {
				rateValidity = false;
			} else {
				rateValidity = true;
				do2Amount = do2.getRate();								// stage where one of the children is handicapped.
			}
		} else {
			rateValidity = false;
		}
	
		return rateValidity;
	}

	private int getColumnAmount(String column) {
	
		while (column!=null && column.length()!=0 && column.length()==9) {
			int childClass = Integer.parseInt(column.substring(5,7));
		
			if (childClass > 10) {
				return (Do2Manager.HIGHER_RTF_RATE* Integer.parseInt((column.substring(8,9))));  
			} else {
				return (Do2Manager.LOWER_RTF_RATE* Integer.parseInt((column.substring(8,9)))); 
			}
		}
		return 0;
	}
}
