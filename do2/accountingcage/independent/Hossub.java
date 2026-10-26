package itsdc.dolphin.do2.accountingcage.independent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:03-09-2009
 */

@RestController
public class Hossub extends AccountingCageBase implements java.io.Serializable {
/*	@In
	EntityManager entityManager;*/
	String message;
	
	static public final int DOB_FIELD_LENGTH = 8;
	static public final double MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT = 3000;
	static public final double MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_25PC_INCREASE = 3750;
	static public final double MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_50PC_INCREASE = 4500;
	static public final double MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC = 6750;
	static public final double MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE = 8437.5;
	
	private boolean validity = true;
 	private boolean ceatwnForElderDob = false, claimPeriodNot12M = false; 
	private Calendar fromDateCal = new GregorianCalendar();
	private Calendar toDateCal = new GregorianCalendar();
	private Date reversedob = null;
	private Employee employee;
	private StringBuffer buffer = new StringBuffer();
	static public final List<String>  VALID_COLUMN_A_VALUES = Arrays.asList("NUR","LKG","UKG","CL-01","CL-02","CL-03","CL-04",
			"CL-05","CL-06","CL-07","CL-08","CL-09","CL-10","CL-11","CL-12");
		
	static public final String INVALID_FORMAT = "VL:To be claimed through Cont. Bill format upto 01.09.2008 \n";
	static public final String INVALID_FROM_DATE = "VL:From date should be first of the month. \n";
	static public final String INVALID_TO_DATE = "VL:To date should be the last day of the month. \n";
	//static public final String INVALID_AMOUNT = "VL:Amount in Rate column cannot be zero / should not exceed Rs.3000/- or Rs.3750/- or Rs.4500/- depending upon period of claim. \n";
	static public final String INVALID_AMOUNT = "VL:Amount in Rate column cannot be zero / should not exceed monthly rate admisible. \n";  // wef 01.07.2017
	//static public final String OVERLAPPING = "OL:DOII Overlaps with already adjusted CEA/HOSSUB DO2. \n";
	static public final String OVERLAPPING = "OL:DOII Overlaps with already adjusted HOSSUB DO2. \n";  // modified on 13.05.2020.
	static public final String REACHED_MAXIMUM_AGE = "AU:Age exceeded 20 years.\n";
	static public final String MORE_THAN_TWO_CHILDREN = "AU:Admissible for two children only. \n";
	static public final String CONCURRENTLY_INADMISSIBLE = "AU:Both CEA & HOSSUB not admissible concurrently.\n";
	static public final String INVALID_COLUMN_A = "VL:Valid values for COLUMN_7A are \" NUR, LKG, UKG or CL-01 or CL-02 .......... CL-12 \". \n";
	static public final String INVALID_COLUMN_B = "VL:Date of birth of child (eg.01012000) to be given in Col. 7B. \n";
	static public final String INCORRECT_INPUT_PROVIDED = "VL: Incorrect Date of birth in Col.7B. \n";
	static public final String AGE_BELOW_FIVE_YEARS = "AU:Child age below 5 years for CL-01 & above.\n";
	static public final String AGE_BELOW_TWO_YEARS = "AU:Child age below 2 years for NUR/LKG/UKG classes.\n";
	static public final String DOB_AFTER_FROM_DATE = "AU:Date of Birth of child is after from-date of DOII. \n";
	static public final String CEATWN_FOR_ELDER_DOB = "AU:CEATWN/HSBTWN claimed for child with elder DOB. \n ";
	static public final String CHILDREN_LIMIT_EXCEED_NEW_FORMAT="AU:More than two children not admissible.\n";
	static public final String NOT_ON_DEPUTATION = "AU:CEA is admissible to deputationists to BHUTAN/UNMISN only.";
	static public final String INVALID_DATES_FOR_FINANCIAL_YEAR_CLAIMS = "VL:From Date And To Date Are Not Beginning And End Of Finanacial Year Respectively.\n";
	static public final String TO_BE_CLAIMED_AFTER_FIN_YEAR_END = "AU:To Be Claimed/Published After Completion Of Financial Year.\n";
	static public final String CLAIM_PERIOD_NOT_12M = "VL:Claim Period Is Less/More Than 12 Months In 7th CPC.\n";
	static public final String INVALID_DATES_FOR_FULL_YEAR_CLAIMS = "VL:From Date And To Date Mismatch For A Full Year Claim.\n";
	static public final String LOWER_CLASS_CLAIM="VL:Claim For Lower Class For Child Whose Higher Class Claim Already Adjusted Previously.\n";
	static public final String INVALID_DO2_DATE = "VL:DOII Publication Date Before ToDate. \n";
	static public final String FROM_DATE_BEFORE_ENROLLMENT_DATE="VL:DO2 FromDate Is Before Enrollment Date.\n";
	static public final String INVALID_FROM_DATE_FOR_DSC="VL:To Be Claimed In Previous PAO (For DSC) OR Claim Period Is Prior To Enrolled Year (For TA).\n";
	static public final String ENROLLMENT_DATE_NULL="VL:Enrollment Date Is Blank In Employee Table.\n";
	static public final String INVALID_COLUMN_C = "VL:UDISE Code Required wef 01.04.2026 In Column7C Is Blank Or Incorrect. \n";

	private boolean isUdiseValidationRequired(Do2 do2) {
		if (do2 == null || do2.getFromDate() == null) {
			return false;
		}
		if (do2.getFromDate().before(DateManager.getUdiseCodeIntroductionDate())) {
			return false;
		}
		return !isNepalDomiciled(do2);
	}

	private boolean isNepalDomiciled(Do2 do2) {
		Employee currentEmployee = do2 != null ? do2.getEmployee() : null;
		if (currentEmployee != null && currentEmployee.getNepalDomiciled() != null) {
			return Boolean.TRUE.equals(currentEmployee.getNepalDomiciled());
		}
		if (currentEmployee == null && do2 != null && do2.getArmyNo() != null) {
			currentEmployee = Utils.getCurrentEmployee(do2.getArmyNo());
		}
		return currentEmployee != null && Boolean.TRUE.equals(currentEmployee.getNepalDomiciled());
	}

	@Transactional
	@Override
	public boolean isValid(Do2 do2) {
		validity = true;
	 	toDateCal.setTime(do2.getToDate());
		fromDateCal.setTime(do2.getFromDate());
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (!isValidPeriod(do2)) {
			buffer.append(INVALID_FORMAT);
			validity = false;
		}

		if (fromDateCal.get(Calendar.DATE)>1) {
			buffer.append(INVALID_FROM_DATE);
			validity = false;
		}

		if (!isValidToDate()) {
			buffer.append(INVALID_TO_DATE);
			validity = false;
		}
		
		if (!isValidYear(do2)) {
    		if (claimPeriodNot12M) {
    			buffer.append(CLAIM_PERIOD_NOT_12M);     			
    		} else {
    			buffer.append(INVALID_DATES_FOR_FULL_YEAR_CLAIMS);    			   			
    		}
     		validity = false; 
    	} 
		
		if (!isValidRate(do2)) {
			buffer.append(INVALID_AMOUNT);
			validity = false;
		}
		
		if(!isValidColumnA(do2)) {
			buffer.append(INVALID_COLUMN_A);  
			validity = false;
		}
		
		if (!isValidColumnB(do2)){
 			buffer.append(INVALID_COLUMN_B); 
 			validity = false;
		}
		
		if (isUdiseValidationRequired(do2)) {
			if (!isValidColumnC(do2)) {
				buffer.append(INVALID_COLUMN_C); 
	 			validity = false;
			}  	
 		} 
		
		if (isOnDeputationOtherthanBhutanUNMISN(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NOT_ON_DEPUTATION);
			validity = false;
		}
		
		if (do2.getDo2Date().before(do2.getToDate())) {
			buffer.append(INVALID_DO2_DATE);  
			validity = false;
		}

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (employee!=null && employee.getEnrolmentDate()!=null) {
			if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
				if (employee.getRegularTaFlag()!=null && !employee.getRegularTaFlag().equals("T")) {
					if (do2.getFromDate().before(employee.getEnrolmentDate())) {
						validity=false;
						buffer.append(FROM_DATE_BEFORE_ENROLLMENT_DATE);				
					}
				}					
			}
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) || (employee.getRegularTaFlag()!=null && employee.getTaAppointmentCategory()!=null && 
					employee.getRegularTaFlag().equals("T") && employee.getTaAppointmentCategory().equals("R"))) {
				if (employee.getEnrolmentDate().after(do2.getFromDate())) {
					Calendar cal = new GregorianCalendar(), do2FromDateCal = new GregorianCalendar();
					cal.setTime(employee.getEnrolmentDate());
					int enrolYear = cal.get(Calendar.YEAR);
					int enrolMonth = cal.get(Calendar.MONTH);					 
					do2FromDateCal.setTime(do2.getFromDate());
					int fromDateYear = do2FromDateCal.get(Calendar.YEAR);
					int fromDateMonth = do2FromDateCal.get(Calendar.MONTH);
			//		System.out.println("xxxxxxxxxxxxxxxxxxx ano "+ do2.getArmyNo()+"  enrolyear " +enrolYear+" enrolmonth "+enrolMonth);
			//		System.out.println("xxxxxxxxxxxxxxxxxxx ano "+ do2.getArmyNo()+"  fromdtyear " +fromDateYear+" enrolmonth "+fromDateMonth);					
					if (fromDateYear<(enrolYear-1)) {
						validity=false;
						buffer.append(INVALID_FROM_DATE_FOR_DSC);
					} else if (fromDateYear==(enrolYear-1)) {
						if (fromDateMonth<3) {
							validity=false;
							buffer.append(INVALID_FROM_DATE_FOR_DSC);
						}						 
					}	
				}				
			}
		} else {
			validity=false;
			buffer.append(ENROLLMENT_DATE_NULL);	
		}
		
		if (validity == true){
			if (!isValidAge(do2, reversedob)) {
				validity = false;
			}
		}

		if (validity == true){
			if (isOverlapping(do2)) {
				buffer.append(OVERLAPPING);
				validity = false;
			}
		}
	 	
		if (validity == true){
	 		if (whetherMoreThanTwoChildren(do2)) {
	 			if (ceatwnForElderDob) {
					buffer.append(CEATWN_FOR_ELDER_DOB);
				} else {
		 			buffer.append(CHILDREN_LIMIT_EXCEED_NEW_FORMAT);
					validity = false;
				}
	 		}
	 	}
	 	
	 	if (validity) {
 	 		if (!isValidClassStudied(do2)) {
 	 			buffer.append(LOWER_CLASS_CLAIM);
				validity= false;
 	 		}
 	 	}	
		
	 	if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
	 	message = buffer.toString();
		do2.setReason(message);
		do2Repo.save(do2);
 		return validity;
	}

	@Override
	public String process(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (isValid(do2))  {
			double adjustableAmount = 0;
			if (do2.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
				if (do2.getToDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
					if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT) {
						adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT*DateManager.getNoOfMonths(do2.getFromDate(), 
								do2.getToDate()));
					} else {
						adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()));
					}
				} else {
					if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT) {
						adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT*DateManager.getNoOfMonths(do2.getFromDate(), 
								DateManager.getPreviousDate(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())));
						adjustableAmount += (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT*DateManager.getNoOfMonths(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate(), 
								do2.getToDate()));
					} else {
						adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), 
								DateManager.getPreviousDate(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())));
						adjustableAmount += (do2.getRate()*DateManager.getNoOfMonths(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate(), 
								do2.getToDate()));
					}
 			 	 }
			} else if (do2.getFromDate().before(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())) {
				if (do2.getToDate().before(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())) {
					if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_25PC_INCREASE) {
						adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_25PC_INCREASE*DateManager.getNoOfMonths(do2.getFromDate(), 
								do2.getToDate()));
					} else {
						adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()));	
					}					
				} else {
					if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_25PC_INCREASE) {
						adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_25PC_INCREASE*DateManager.getNoOfMonths(do2.getFromDate(), 
								DateManager.getPreviousDate(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())));
						adjustableAmount += (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_25PC_INCREASE*DateManager.getNoOfMonths(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate(), 
								do2.getToDate()));
					} else {
						adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), 
								DateManager.getPreviousDate(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())));
						adjustableAmount += (do2.getRate()*DateManager.getNoOfMonths(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate(), 
								do2.getToDate()));
					} 					
 				 }
			} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					if (do2.getToDate().before(DateManager.getSeventhCpcAllowanceDate())) {
						if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_50PC_INCREASE) {
							adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_50PC_INCREASE*DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()));
						} else {
							adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()));
						}	
					} else {
			 			if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_50PC_INCREASE) {
							adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_50PC_INCREASE*DateManager.getNoOfMonths(do2.getFromDate(), DateManager.getPreviousDate(DateManager.getSeventhCpcAllowanceDate())));
						} else {
							adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), DateManager.getPreviousDate(DateManager.getSeventhCpcAllowanceDate())));													
						}
			 			if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC) {
							adjustableAmount += (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC*DateManager.getNoOfMonths(DateManager.getSeventhCpcAllowanceDate(), do2.getToDate()));									 				
			 			} else {
							adjustableAmount += (do2.getRate()*DateManager.getNoOfMonths(DateManager.getSeventhCpcAllowanceDate(), do2.getToDate()));							
			 			}		 			
					}
							
			} else if (do2.getFromDate().before(DateManager.getSeventhCpcDaFiftyPercentDate())
						&& do2.getToDate().before(DateManager.getSeventhCpcDaFiftyPercentDate())) {
				if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC) {
					adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC*DateManager.getNoOfMonths(do2.getFromDate(), 
							do2.getToDate()));
				} else {
					adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()));
				}	
			} else {
				if (do2.getFromDate().before(DateManager.getSeventhCpcDaFiftyPercentDate())
						&& do2.getToDate().after(DateManager.getSeventhCpcDaFiftyPercentDate())) {
					if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE) {
						adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC*DateManager.getNoOfMonths(do2.getFromDate(), 
								DateManager.getPreviousDate(DateManager.getSeventhCpcDaFiftyPercentDate())));
						adjustableAmount += (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE*DateManager.getNoOfMonths(DateManager.getSeventhCpcDaFiftyPercentDate(), 
								do2.getToDate()));
					} else if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC) {
						adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC*DateManager.getNoOfMonths(do2.getFromDate(), 
								DateManager.getPreviousDate(DateManager.getSeventhCpcDaFiftyPercentDate())));
						adjustableAmount += (do2.getRate()*DateManager.getNoOfMonths(DateManager.getSeventhCpcDaFiftyPercentDate(), 
								do2.getToDate()));
					} else {
						adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()));
					}
				} else {
					if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE) {
						adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE*DateManager.getNoOfMonths(do2.getFromDate(), 
								do2.getToDate()));
					} else {
						adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()));
					}	
				}					
			}
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) || (employee.getRegularTaFlag()!=null && employee.getTaAppointmentCategory()!=null && 
					employee.getRegularTaFlag().equals("T") && employee.getTaAppointmentCategory().equals("R"))) {
		//		System.out.println("aaaaaaaaaaaaa");
				if (employee!=null && employee.getEnrolmentDate()!=null && !employee.getEnrolmentDate().after(do2.getFromDate())) {
			//		System.out.println("bbbbbbbbbbbbbb");
					;
				} else {
				 	Calendar enrollCalendar = new GregorianCalendar(), do2FromDateCal = new GregorianCalendar();
					enrollCalendar .setTime(employee.getEnrolmentDate());
					int enrolYear = enrollCalendar.get(Calendar.YEAR);
					int enrolMonth = enrollCalendar.get(Calendar.MONTH); 
					do2FromDateCal.setTime(do2.getFromDate());
					int fromDateYear = do2FromDateCal.get(Calendar.YEAR);
					int fromDateMonth = do2FromDateCal.get(Calendar.MONTH);
					Date fromDate = do2.getFromDate();
					if (enrolYear==fromDateYear) {
						if (fromDateMonth<enrolMonth) {
							try {
								fromDate = DateManager.getDate(fromDateYear, (enrolMonth+1), 1);
							} catch (Exception e) {
								Utils.handleException(e);
							}
						}
					} else if (enrolYear>fromDateYear) {				 
						try {
							fromDate = DateManager.getDate(enrolYear, (enrolMonth+1), 1);
						} catch (Exception e) {
							Utils.handleException(e);
						}				 
					}
					if (!fromDate.equals(do2.getFromDate()) && !fromDate.before(DateManager.getSeventhCpcAllowanceDate())) {  // to be extended prior to this date on demand from pao
						adjustableAmount=0;
						if (fromDate.before(DateManager.getSeventhCpcDaFiftyPercentDate())
									&& do2.getToDate().before(DateManager.getSeventhCpcDaFiftyPercentDate())) {
							if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC) {
								adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC*DateManager.getNoOfMonths(fromDate, 
										do2.getToDate()));
							} else {
								adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(fromDate, do2.getToDate()));
							}	
						} else {
							if (fromDate.before(DateManager.getSeventhCpcDaFiftyPercentDate())
									&& do2.getToDate().after(DateManager.getSeventhCpcDaFiftyPercentDate())) {
								if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE) {
									adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC*DateManager.getNoOfMonths(fromDate, 
											DateManager.getPreviousDate(DateManager.getSeventhCpcDaFiftyPercentDate())));
									adjustableAmount += (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE*DateManager.getNoOfMonths(DateManager.getSeventhCpcDaFiftyPercentDate(), 
											do2.getToDate()));
								} else if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC) {
									adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC*DateManager.getNoOfMonths(fromDate, 
											DateManager.getPreviousDate(DateManager.getSeventhCpcDaFiftyPercentDate())));
									adjustableAmount += (do2.getRate()*DateManager.getNoOfMonths(DateManager.getSeventhCpcDaFiftyPercentDate(), 
											do2.getToDate()));
								} else {			
									adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(fromDate, do2.getToDate()));
								}
							} else {
								if (do2.getRate()>MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE) {
									adjustableAmount = (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE*DateManager.getNoOfMonths(fromDate, do2.getToDate()));
								} else {
									adjustableAmount = (do2.getRate()*DateManager.getNoOfMonths(fromDate, do2.getToDate()));
								}	
							}					
						}
					}
		 			System.out.println("cccccccccccccccccc ano "+do2.getArmyNo()+"    fromdt " +fromDate+"  enorlmntdt "+enrollCalendar.getTime() +" adj amt " +adjustableAmount);
				}			
			}		
			if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {		
				do2.setColumnB(getDateOfBirthInColumnBInReverseOrder(do2));
				do2Repo.save(do2);	
			}
					
			if (!isArrearsAlreadyAdjustedForDo2(do2)) {
				Utils.addArrears(do2, adjustableAmount);
			}
		} 		 
		return Do2Manager.SUCCESS;
	}
	
	//Method to ensure that Do2 format is not used for claims before 01.09.2008. However, this is taken care of in GenericProcessor.
	private boolean isValidPeriod(Do2 do2) {
	 	
		if ((do2.getFromDate()).before(DateManager.getSixthCpcAllowanceDate())
				|| (do2.getToDate()).before(DateManager.getSixthCpcAllowanceDate())) {
			return false;
		}
		return true;
	}
   

	//Method to ensure that to_date in current DO2 is the last date of the month 
    private boolean isValidToDate() {
    	int days = DateManager.getNoOfDaysInMonth(toDateCal.get(Calendar.MONTH)+1, toDateCal.get(Calendar.YEAR));
	 	return (toDateCal.get(Calendar.DATE)==days); 
	}
 

	// Method to check overlapping of current DO2 with already adjusted DOs2 of HOSSUB or CEA
	private boolean isOverlapping(Do2 do2) {		
		List <Do2> validDo2List = do2Repo.findCeaOverlapping6Cpc(do2.getEmployee().getId(), do2.getId(), Do2Manager.VALID, do2.getFromDate(), do2.getToDate());
				
				
		/*		Utils.getEntityManager().createQuery(
				" SELECT d FROM Do2 d WHERE d.employee =:employee AND d.id !=:id AND d.status=:status AND " +
				" d.description in ('CEA', 'CEATWN', 'CEAHC', 'HOSSUB', 'HSBTWN') AND " +
			    " ((:fromDate between d.fromDate AND d.toDate ) OR (:toDate between d.fromDate AND d.toDate ) OR " +
				" (d.fromDate between :fromDate AND :toDate) OR (d.toDate between :fromDate AND :toDate))")
		.setParameter("employee", do2.getEmployee())
		.setParameter("status", Do2Manager.VALID)
		.setParameter("fromDate", do2.getFromDate())
		.setParameter("toDate", do2.getToDate())
		.setParameter("id", do2.getId()).getResultList();*/
		
		if (validDo2List.size() > 0) {
			String dateInReverse = getDateOfBirthInColumnBInReverseOrder(do2);
			String ymobInReverse = dateInReverse.substring(0,6);			
			
			for (Do2 oldDo2 : validDo2List) {
		 		if ((oldDo2.getDescription().equals(do2.getDescription()) && 
					(oldDo2.getColumnB() != null && oldDo2.getColumnB().substring(0,6).equals(ymobInReverse)))) {
			 		return true;
		 		}		 		
		 		if (do2.getDescription().equals("HOSSUB") && do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) { // date check added on 13.05.2020 to exclude concurrent check in 7cpc
		 			if ((oldDo2.getDescription().equals("CEA") || oldDo2.getDescription().equals("CEAHC")) && 
						(oldDo2.getColumnB() != null && 
						 oldDo2.getColumnB().substring(0,6).equals(ymobInReverse))) {
						buffer.append(CONCURRENTLY_INADMISSIBLE);
						return true;
					}
		 		}
		 		if (do2.getDescription().equals("HSBTWN") && do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) { // date check added on 13.05.2020 to exclude concurrent check in 7cpc
		 			if (oldDo2.getDescription().equals("CEATWN") && 
						(oldDo2.getColumnB() != null && 
						 oldDo2.getColumnB().substring(0,6).equals(ymobInReverse))) {
						buffer.append(CONCURRENTLY_INADMISSIBLE);
						return true;
					}
		 		}
								 
				if (oldDo2.getDescription().equals("CEATWN") || oldDo2.getDescription().equals("HSBTWN")) {
					if (!ymobInReverse.equals(oldDo2.getColumnB().substring(0,6))) {
						try {
							if (Integer.parseInt(ymobInReverse) > Integer.parseInt(oldDo2.getColumnB().substring(0,6))) {
								buffer.append("Drawing CEATWN/HSBTWN for elder children");
								return true;	
							}
						} catch (Exception e) {
							Utils.handleException(e);
						}
					}
				}
			}
			Boolean hossubAdjusted = false;
			for (Do2 oldDo2 : validDo2List) {
				if ((do2.getDescription().equals("HSBTWN") && (oldDo2.getDescription().equals("HOSSUB")) &&
					(oldDo2.getColumnB() != null && oldDo2.getColumnB().substring(0,6).equals(ymobInReverse)))) {
						hossubAdjusted = true;
			 	}	
			}
			if (do2.getDescription().equals("HSBTWN") && hossubAdjusted == false){
				buffer.append("Hossub not adjusted for one of twin children");
				return true;
			}
		}
		return false;

	}

	//Method to ensure that rate given in rate column does not exceed Rs.3000/- or Rs.3750/-
	private boolean isValidRate(Do2 do2) {
	 
		if (do2.getRate()!=null && do2.getRate()>0) {
			if (do2.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
				if (do2.getToDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
					if (do2.getRate() <= MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT) {
						return true;
					}  
				} else {
					if (do2.getRate() <= MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_25PC_INCREASE) {
						return true;
					}  
				}
			} else if (do2.getFromDate().before(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())) {
				if (do2.getToDate().before(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())) {
					if (do2.getRate() <= MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_25PC_INCREASE) {
						return true;
					}  
				} else {
					if (do2.getRate() <= MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_50PC_INCREASE) {
						return true;
					}  
				}
			} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				if (do2.getToDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					if (do2.getRate() <= MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_AFTER_50PC_INCREASE) {
						return true;
					} 
				} else {
					if (do2.getRate() <= MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC) {
						return true;
					}  
				}			
			} else if (do2.getFromDate().before(DateManager.getSeventhCpcDaFiftyPercentDate())
						&& do2.getToDate().before(DateManager.getSeventhCpcDaFiftyPercentDate())) {
				if (do2.getRate() <= MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC) {
					return true;
				}  
			} else {
				if (do2.getRate() <= (MAXIMUM_MONTHLY_ADMISSIBLE_AMOUNT_IN_7CPC_WITH_25PC_INCREASE+0.5)) {
					return true;
				}
			}			
		} 
		return false;
	}


	public boolean isValidColumnA(Do2 do2) {
		if (do2.getColumnA()!=null && do2.getColumnA().length()==3 
				&& (do2.getColumnA().trim().equals("NUR") || do2.getColumnA().trim().equals("LKG") || do2.getColumnA().trim().equals("UKG"))) {
			return true;
		}
		return (do2.getColumnA()!=null && do2.getColumnA().length()==5 && VALID_COLUMN_A_VALUES.contains(do2.getColumnA().trim()));
	}	
	
	private boolean isValidDate(String dateStr) {
		int year = Integer.valueOf(dateStr.substring(4, 8));
		int month = Integer.valueOf(dateStr.substring(2, 4));
		int day = Integer.valueOf(dateStr.substring(0, 2));
		try {
			DateManager.getDate(year, month, day);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isValidColumnB(Do2 do2) {
		int year  = 0; 
		int month = 0;
		int day   = 0; 
		
		String dateStr = do2.getColumnB();
		
		if (dateStr==null || (dateStr.trim().length()!=8)) {
			return false;
		}
				
		try {
			year  = Integer.valueOf(dateStr.substring(4, 8));
			month = Integer.valueOf(dateStr.substring(2, 4));
			day   = Integer.valueOf(dateStr.substring(0, 2));
		} catch (Exception e) {
			buffer.append(INCORRECT_INPUT_PROVIDED);
			return false;
		}
		
		if (year<1980) {
			return false;
		}
		
		if (month<1 || month>12) {
			return false;
		}
		
		if ((month==1 || month==3 || month==5 || month==7 || month==8 || month==10 || month==12) && (day<1 || day>31)) {
			return false;
		}
	
		if ((month==4 || month==6 || month==9 || month==11) && (day<1 || day>30)) {
			return false;
		}
	
		
		if (((year%4)==0 && month==2 && (day<1 || day>29))) {
			return false;
		} 
		
		if (((year%4)!=0 && month==2 && (day<1 || day>28))) {
			return false;
		}
		
		Date birthDate = new Date();
		
		try {
			birthDate = DateManager.getDate(year, month, day);
		} catch (Exception e) {
			 Utils.handleException(e);
		}
		reversedob = birthDate;
		return true;
	}

    public boolean isValidColumnC(Do2 do2) {

        if (do2.getColumnC() == null) return false;

        String value = do2.getColumnC().trim().toUpperCase();

        // Case 1: Normal UDISE — UDISE + 11 digits = 16 chars
        if (value.length() == 16
                && !value.contains("HCAP")
                && value.startsWith("UDISE")
                && Utils.isValidNumber(value.substring(5))) {
            return isValidUdiseDigits(value.substring(5));
        }

        // Case 2: HCAP — HCAP + UDISE + 11 digits = 21 chars
        if (value.length() == 21
                && value.startsWith("HCAP")
                && value.contains("UDISE")
                && Utils.isValidNumber(value.substring(10))) {
            return isValidUdiseDigits(value.substring(10));
        }

        return false;
    }

    private boolean isValidUdiseDigits(String udise) {
        if (!Utils.isValidNumber(udise)) return false;

        int state    = Integer.parseInt(udise.substring(0, 2));
        int district = Integer.parseInt(udise.substring(2, 4));
        int block    = Integer.parseInt(udise.substring(4, 6));
        int village  = Integer.parseInt(udise.substring(6, 9));
        int school   = Integer.parseInt(udise.substring(9, 11));

        return state >= 1 && state <= 36
                && district > 0
                && block    > 0
                && village  > 0
                && school   > 0;
    }
	
	private boolean isValidAge(Do2 do2, Date reversedob) {		
		if (!reversedob.before(do2.getFromDate())) {
			buffer.append(DOB_AFTER_FROM_DATE);
			return false;
		} 
			
		if (do2.getColumnA()!=null) {
			if (do2.getColumnA().startsWith("CL")) {                      //   6 months reduced to allow +/- 5 or 2 years.
				if (DateManager.getNoOfMonths(reversedob, do2.getFromDate())<54) {
					buffer.append(AGE_BELOW_FIVE_YEARS);
					return false;
				}
			} else {
				if (DateManager.getNoOfMonths(reversedob, do2.getFromDate())<18) {
					buffer.append(AGE_BELOW_TWO_YEARS);
					return false;
				}
			}
		}
		
		if (!do2.getFromDate().before(DateManager.getUdiseCodeIntroductionDate())) {
			if (do2.getColumnC() != null && do2.getColumnC().contains("HCAP")) {
				if (DateManager.getNoOfMonths(reversedob, do2.getFromDate()) > 264) {
					buffer.append(REACHED_MAXIMUM_AGE);
					return false;
				} else {
					if (DateManager.getNoOfMonths(reversedob, do2.getFromDate()) > 240) {
						buffer.append(REACHED_MAXIMUM_AGE);
						return false;
					}
				}
			} 
		} else if (do2.getColumnC() != null && do2.getColumnC().equals("HCAP")) {
			if (DateManager.getNoOfMonths(reversedob, do2.getFromDate()) > 264) {
				buffer.append(REACHED_MAXIMUM_AGE);
				return false;
			}
		} else {
			if (DateManager.getNoOfMonths(reversedob, do2.getFromDate()) > 240) {
				buffer.append(REACHED_MAXIMUM_AGE);
				return false;
			}
		}
		return true;
	}
	//Method to check whether claim is for more than two children  
	private boolean whetherMoreThanTwoChildren(Do2 do2) {
		boolean fpoFailed = false;
		Date fpoFailedDate = null;
		Date birthDate = null;
		Date demiseDate = null;
		Date demisedChildDob = null;
		Date currentDob = DateManager.getDateFromDdMmYyyy(do2.getColumnB().trim());
		List<String>children = new ArrayList<String>();
		List<Do2>strincList;
		strincList = do2Repo.findByDescStatusColAOrderByFromDateDesc(do2.getEmployee().getId(), "V", "STRINC", "CEASED");
				
		/*		Utils.getEntityManager().createQuery(
				" SELECT d FROM Do2 d WHERE d.employee = :employee and d.status='V' AND " +
				" d.description = 'STRINC' AND d.columnA = 'CEASED' order by d.fromDate desc")
				.setParameter("employee", do2.getEmployee()).getResultList();*/
		if (strincList != null && strincList.size() > 0){
			fpoFailedDate = strincList.get(0).getFromDate();
			if (fpoFailedDate!=null) {
				fpoFailed = true;
			}
		}
		List<String> relList=Arrays.asList(new String[] {"SON","DAUGHTER"});
		List<FamilyDetail> fdList = fdRepo.findByEmpRelationListAndStatus(do2.getEmployee().getId(), relList, "DEMISE");
				
	/*			Utils.getEntityManager().createQuery(
				"SELECT fd from FamilyDetail fd where fd.employee = :emp and " + 
				"(fd.relationship = 'SON' or fd.relationship = 'DAUGHTER') and fd.statusOfMember = 'DEMISE'")
				.setParameter("emp", do2.getEmployee()).getResultList();*/
		if (fdList != null && fdList.size() > 0){
			demiseDate = fdList.get(0).getToDate();
			demisedChildDob=fdList.get(0).getDateOfBirth();			
		}
		if (do2.getDescription().equals("HSBTWN")){
			List<String> hsbList = do2Repo.findByFromDateGreaterThanAndColumnB(do2.getEmployee().getId(),
								DateManager.getSixthCpcAllowanceDate(), getDateOfBirthInColumnBInReverseOrder(do2), "HOSSUB");
					
			/*		Utils.getEntityManager().createNativeQuery(
					" SELECT column_b as dob FROM do2 WHERE fk_employee = :employee and from_date > :frdate " +
					" AND status='V' AND description = 'HOSSUB' AND column_b = :currentdob")
					.setParameter("employee",  do2.getEmployee())
					.setParameter("currentdob", getDateOfBirthInColumnBInReverseOrder(do2))
					.setParameter("frdate",DateManager.getSixthCpcAllowanceDate()).getResultList();*/
			if (hsbList.size() == 0){
				buffer.append("No HOSSUB DO2 for Twin child found - to be claimed as HOSSUB");
				return true;
			} 				
		}
		
		List<String> validColBList = do2Repo.findCeaDistinctColBList(do2.getEmployee().getId(),DateManager.getSixthCpcAllowanceDate(),do2.getId());
				
	/*			Utils.getEntityManager().createNativeQuery(
			" SELECT distinct (column_b) as dob FROM do2 WHERE fk_employee = :employee and from_date > :frdate " +
			" AND status='V' AND description in ('CEA', 'CEATWN','CEAHC','HOSSUB','HSBTWN') AND id != :currentDo2Id " +
			" AND (column_b not like 'S%' AND column_b not like 'P%') ORDER BY dob")
				.setParameter("employee",  do2.getEmployee())
				.setParameter("frdate", DateManager.getSixthCpcAllowanceDate())
				.setParameter("currentDo2Id", do2.getId()).getResultList();*/
		 
		if (validColBList == null || validColBList.size() == 0) { // To adjust first CEA Do2 in r/o of first or second child											
			return false;
		}
		
		String prevymob = "";
		String ymob = "";	
		
		for (int i = 0; i < validColBList.size(); i++){
			ymob=validColBList.get(i).trim().substring(0,6);
			if (!prevymob.equals(ymob)){
				children.add(validColBList.get(i));
				prevymob = ymob;
			}
		}
		
	/*	if (demiseDate != null && do2.getFromDate().after(demiseDate)){
			for (int i = 0; i < children.size(); i++) {
				if (demisedChildDob != null && children.get(i).equals(demisedChildDob))
					children.remove(i);
			}			
		}*/
		
		if (demiseDate != null && do2.getFromDate().after(demiseDate)){
		 	String demisedDobForComparison = null;
			for (int i = 0; i < children.size(); i++) {
				if (demisedChildDob!=null) {
					demisedDobForComparison = DateManager.getStringDateWithoutSeparators(demisedChildDob);
					demisedDobForComparison = demisedDobForComparison.substring(4)+demisedDobForComparison.substring(2,4)+demisedDobForComparison.substring(0,2);
				}
				if (demisedDobForComparison!=null && children.get(i).equals(demisedDobForComparison)) {
					children.remove(i);
				}
				demisedDobForComparison=null;
			}			
		}
		
		if (children.size() > 3) {
			return true;
		}
		ymob = getDateOfBirthInColumnBInReverseOrder(do2).substring(0,6);
		for (int i = 0; i < children.size(); i++) {
			prevymob = children.get(i).substring(0,6);
			if (!prevymob.equals(ymob)) {
				try {
					// Date of birth within six months from dob of first child not expected  // Modified as six on par with CEA on 12.03.2021
					birthDate = DateManager.getDate(children.get(i).substring(0,4)+"-"+children.get(i).substring(4,6)+"-"+children.get(i).substring(6));
					if (birthDate != null && DateManager.getNoOfMonths(currentDob, birthDate) < 6){    
						buffer.append("Date of birth appears to be incorrect");
						return true;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (children.size() == 3) {// Third Child			
			boolean dobMatched = false;
			if (fpoFailed == true) {
				int count = 0;
				for (int i = 0; i < children.size(); i++) {
					birthDate = null;
					try {
						birthDate = DateManager.getDate(children.get(i).substring(0,4)+"-"+children.get(i).substring(4,6)+"-"+children.get(i).substring(6));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (birthDate != null && !birthDate.before(fpoFailedDate)) {
						++count;
					}
					if (birthDate != null && currentDob.equals(birthDate)){
						dobMatched = true;
					}						
				}
				if (dobMatched == false){
					return true;
				} else if (count > 1){ // admissible to only one delivery after family planning operation failed
					return true;
				} else return false;				
			} else {
				return true;	
			}
		}
		
		if (children.size()==2) {
			for (int i = 0; i < children.size(); i++) {
				prevymob = children.get(i).substring(0,6);
				if (prevymob.equals(ymob))
					return false;
			}
			if (fpoFailed == true && !currentDob.before(fpoFailedDate)){ //Third child
				return false;
			}
			return true;
		}
		if (children.size()==1) {	
			prevymob = children.get(0).substring(0,6);
			List<String> twnList = do2Repo.findCeaTwinDobList(do2.getEmployee().getId(),DateManager.getSixthCpcAllowanceDate(),do2.getId(),prevymob);
					
		/*			Utils.getEntityManager().createNativeQuery(
					" SELECT column_b as dob FROM do2 WHERE fk_employee = :employee and from_date > :frdate " +
					" AND status='V' AND (description = 'CEATWN' OR description = 'HSBTWN') AND " +
					" id != :currentDo2Id AND substr(column_b,0,7) = :prevymob")
					.setParameter("employee",  do2.getEmployee())
					.setParameter("prevymob", prevymob)
					.setParameter("frdate",DateManager.getSixthCpcAllowanceDate())
					.setParameter("currentDo2Id", do2.getId()).getResultList();*/
			if (twnList.size() > 0 && (Integer.parseInt(ymob) < Integer.parseInt(prevymob))){
				// To reject second child if twins/multiple children in first instance
				ceatwnForElderDob = true;	
				return true;
			} 			
			return false;
		}		
		return false;
	}
	 private String getDateOfBirthInColumnBInReverseOrder(Do2 do2) {
	    	String dateStr = do2.getColumnB();
			int year = Integer.valueOf(dateStr.substring(4, 8));
			int month = Integer.valueOf(dateStr.substring(2, 4));
			int day = Integer.valueOf(dateStr.substring(0, 2));
		   
			String DAY = day+"";
			String MONTH = month+"";
				
			if (month<10)
				MONTH="0"+MONTH;
			if (day<10)
				DAY="0"+DAY;
			
			return year+MONTH+DAY;
	 }
	 
	 private boolean isArrearsAlreadyAdjustedForDo2(Do2 do2) {
	    	List<Arrears> arrearsList = arrRepo.findByDo2Id(do2.getId());
	    			
	    	//		Utils.getEntityManager().createQuery("SELECT a FROM Arrears a WHERE a.do2=:do2")
	    	//								.setParameter("do2", do2).getResultList();
	    	
	    	return (arrearsList.size()>0); 
	 }

	 public boolean isOnDeputationOtherthanBhutanUNMISN(Do2 do2) {
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			List<AuditCage> auditCageList = acRepo.findByEmpIdEffectiveStatusNotEqualPendingFromDateBetweenOrderByFromDateDesc(employee.getId(),do2.getFromDate());
					
		/*			Utils.getEntityManager().createQuery(
					" SELECT ac FROM AuditCage ac WHERE ac.employee=:emp AND ac.effective=true AND" +
					" ac.cancelled=false AND ac.recordStatus!='P' AND " +
					" (:do2FromDate between ac.fromDate and ac.toDate) order by ac.fromDate desc, ac.toDate desc ")
					.setParameter("emp", employee)
					.setParameter("do2FromDate", do2.getFromDate()).getResultList();*/
			if (auditCageList.size() > 0) {
				if (auditCageList.get(0).getDeputationFlag() == true) {
					if (do2.getFromDate().after(DateManager.getCgguardDeputationDate())) { 
						 if (auditCageList.get(0).getDeputationStatus().equals("U") || 
								 auditCageList.get(0).getDeputationStatus().equals("T") ||
								 auditCageList.get(0).getDeputationStatus().equals("K") ||
								 auditCageList.get(0).getDeputationStatus().equals("O")) 
								  return false;
					} else {
					 if (auditCageList.get(0).getDeputationStatus().equals("U") || 
						 auditCageList.get(0).getDeputationStatus().equals("T") ||
						 auditCageList.get(0).getDeputationStatus().equals("K")) {
						 return false; 
					 }
				}	
			} else {
				  return false;  
			}
		}
		return false;
    }
	 
	private boolean isValidYear(Do2 do2) {
		boolean validity = true;
		Calendar fromDateCal = new GregorianCalendar(), toDateCal = new GregorianCalendar();
		fromDateCal.setTime(do2.getFromDate());
		int fromDateYear = fromDateCal.get(Calendar.YEAR);
		int fromDateMonth = fromDateCal.get(Calendar.MONTH)+1;
		int fromDateDate = fromDateCal.get(Calendar.DATE);
		toDateCal.setTime(do2.getToDate());
		int toDateYear = toDateCal.get(Calendar.YEAR);
		int toDateMonth = toDateCal.get(Calendar.MONTH)+1;
		int toDateDate = toDateCal.get(Calendar.DATE);
		Date finYearEndDate =null;
		claimPeriodNot12M = false;
		
		if (fromDateYear==2018 && toDateYear==2018 && toDateMonth==3) {  // To allow the last claim - Quarterly as per 6cpc.
			return validity;
		}
		
		if (fromDateYear>2017) {
			if (DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate())==12) {
				;
	  		} else {
	  			claimPeriodNot12M=true;
	  			validity=false;
	  		}
			
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)  || (employee.getRegularTaFlag()!=null && employee.getTaAppointmentCategory()!=null && 
					employee.getRegularTaFlag().equals("T") && employee.getTaAppointmentCategory().equals("R"))) {
		 		if (validity==false && claimPeriodNot12M==true) {
		 			if (do2.getEmployee()!=null && do2.getEmployee().getEnrolmentDate()!=null) {
		 				double noOfDays = 0;
		 				Calendar enrollCal = new GregorianCalendar();
		 				enrollCal.setTime(do2.getEmployee().getEnrolmentDate());
		 				if (toDateMonth>3 && (enrollCal.get(Calendar.YEAR)==toDateYear || (enrollCal.get(Calendar.YEAR)+1)==toDateYear)) {
		 					return validity;
		 				}
		 				if (do2.getEmployee().getEnrolmentDate().before(do2.getFromDate())) {
		 	//				noOfDays = DateManager.getNoOfDays(do2.getEmployee().getEnrolmentDate(), do2.getFromDate());
		 	//				if (noOfDays<31) {
		 						if (enrollCal.get(Calendar.YEAR)==fromDateYear && enrollCal.get(Calendar.MONTH)>2) {
				 					validity=true;  claimPeriodNot12M=false;	
				 				} else if (enrollCal.get(Calendar.YEAR)==fromDateYear && fromDateMonth>=0 && toDateMonth<4) {
				 					validity=true;  claimPeriodNot12M=false;
				 				} else if ((enrollCal.get(Calendar.YEAR)+1)==fromDateYear && toDateMonth<4) {
				 					validity=true;  claimPeriodNot12M=false;
				 				}	
		 	//				} // Commented on 05.10.2021 (DSC)
		 			//		System.out.println(".............. no of days " + noOfDays);
			 			} else {
			 				noOfDays = DateManager.getNoOfDays(do2.getFromDate(), do2.getEmployee().getEnrolmentDate());
			 				if (noOfDays<31) {
		 						if (enrollCal.get(Calendar.YEAR)==fromDateYear && enrollCal.get(Calendar.MONTH)>2) {
				 					validity=true;  claimPeriodNot12M=false;	
				 				} else if (enrollCal.get(Calendar.YEAR)==fromDateYear && fromDateMonth>=0 && toDateMonth<4) {
				 					validity=true;  claimPeriodNot12M=false;
				 				} else if ((enrollCal.get(Calendar.YEAR)+1)==fromDateYear && toDateMonth<4) {
				 					validity=true;  claimPeriodNot12M=false;
				 				}	
		 					}
			 		//		System.out.println("..............,,,,,,,,,,,,,,,,,,,,,,,,,,,, no of days " + noOfDays);
			 			}
		 			}		 				
		 		}
		 		return validity;
		 	}
				
			if (fromDateYear==toDateYear) {
				if (fromDateMonth==1 && toDateMonth==12 && fromDateDate==1 && toDateDate==31) {
					;
				} else {
					validity=false;
				}
			} else {
				if (toDateYear>fromDateYear) {
					if (fromDateMonth==(toDateMonth+1) ) {
						;
					} else {
						validity=false;
					}					
				} else {
					validity=false;
				}				
			}
		}
		return validity;		
	}
	
	 public boolean isValidClassStudied(Do2 do2) {  // To Check Whether Claim Is For Lower Class In Subsequent Years
	    	
	      	List<Do2> do2List = do2Repo.findByDescColBFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(), "HOSSUB", do2.getFromDate(), getDateOfBirthInColumnBInReverseOrder(do2));
	      			
	      			
	      /*			Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.status='V' and d.description='HOSSUB' and "
	    			+ " d.fromDate<:fromDate and d.columnB=:colB order by d.fromDate DESC ").setParameter("emp", do2.getEmployee())
	    			.setParameter("fromDate", do2.getFromDate()).setParameter("colB",  getDateOfBirthInColumnBInReverseOrder(do2)).getResultList();*/
	    	
	    	if (do2List!=null && do2List.size()>0) {
	    		String adjustedDo2ColumnA = do2List.get(0).getColumnA();
	    		if (do2.getColumnA().equals("LKG") && (adjustedDo2ColumnA.equals("UKG") || adjustedDo2ColumnA.startsWith("CL-"))) {
	    			return false;
	    		} else if (do2.getColumnA().equals("UKG") && (adjustedDo2ColumnA.startsWith("CL-"))) {
	    			return false;
	    		} else {
	    			if (do2.getColumnA().startsWith("CL-")) {
	    				int presentClass = Integer.parseInt(do2.getColumnA().substring(3));
	    				int previousClass = 0;
	    				if (adjustedDo2ColumnA.startsWith("CL-")) {
	    					previousClass = Integer.parseInt(adjustedDo2ColumnA.substring(3));
	    					if (presentClass<previousClass) {
	        					return false;
	        				}  
	    				} else {
	    					if (adjustedDo2ColumnA.equals("NUR") || adjustedDo2ColumnA.equals("LKG") || adjustedDo2ColumnA.equals("UKG")) {
	    						return true;
	    					} else {
	    						return false;
	    					}
	    				}
	    				
	    			}
	    			
	    		}   		
	    	}
	    	return true;
	    }
}
