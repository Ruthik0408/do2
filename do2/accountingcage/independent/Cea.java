package itsdc.dolphin.do2.accountingcage.independent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:03-08-2009
 */

@RestController
public class Cea extends AccountingCageBase implements java.io.Serializable {
	 
	String message;
	
	private Calendar fromDateCal = new GregorianCalendar();
	private Calendar toDateCal = new GregorianCalendar();
	private boolean validity = true;
	private boolean restrictValidity = false;
	private boolean concurrentValidity = true;
	private boolean concurrentCeatwnCeahc = false;
	private boolean toClaimAsCeatwn = false;
	private boolean ceatwnForElderDob = false;
	private boolean year2008_09 = false, year2017_18 = false, claimPeriodNot12M = false;
 	private double totalRate = 0;
	private String mutexDescription, mutexCol7A, mutexCol7B;
 	private int mutexNoOfChildren = 0;
	private StringBuffer buffer = new StringBuffer();
	private Employee employee;
	private Do2 overlappingDo2 = null;
	private Date reversedob = null;
	private Date academicYearBeg = new Date();
	private Date academicYearEnd = new Date();
	private String col7b = null;
	private String curqe = null;
	private Date thirtyFirstMarch2018;
	private int tjraMonth = 0;
	private int tjraYear = 0;
	private int lraMonth = 0;
	private int lraYear = 0; 

	static public final int DOB_FIELD_LENGTH = 8;
	static public final int MAXIMUM_AGE_LIMIT = 20;
	
	static public final List<String> NEW_CEA_VALID_COL_A_VALUES = Arrays.asList("NUR","LKG","UKG","CL-01","CL-02","CL-03","CL-04",
																	"CL-05","CL-06","CL-07","CL-08","CL-09","CL-10","CL-11","CL-12");
	static public final List<String> NEW_CEA_VALID_COL_C_VALUES = Arrays.asList("Q1", "Q2", "Q3", "Q4");
	static public final String INVALID_COLUMN_A_OLD_CEA="VL:Column 7A should start with 'PRY-' or 'SEC-', followed by number of children in single digit. \n";
	static public final String INVALID_COLUMN_B_OLD_CEA="VL:Column 7B should start with 'PRY-' or 'SEC-', followed by number of children in single digit. \n";
	static public final String INVALID_AMOUNT_OLD_CEA="VL:Amount in Rate column incorrect. Check w.r.to number of children in col. 7A/7B.\n";
	static public final String INVALID_AMOUNT_NEW_CEA="VL:Amount is zero or amount in rate column exceeded maximum admissible amount or child Age exceeded 20 years \n";
	static public final String NOT_VALID_FORMAT="VL:To be claimed in revised format w.e.f. 01.09.2008. \n";
	static public final String INVALID_FROM_DATE="VL:Date in from_date column should be first of the month.\n";
	static public final String INVALID_TO_DATE="VL:Date in to_date column should be end of the month.\n";
	static public final String REACHED_MAXIMUM_AGE = "VL: Child age exceeded 20 years.\n";
	static public final String CHILDREN_LIMIT_EXCEEDED="AU:More than three children not admissible.\n";
	static public final String CHILDREN_LIMIT_EXCEED_NEW_FORMAT="AU:More than two children not admissible.\n";
	static public final String OVERLAP="OL:Claim overlaps with already adjusted CEA/RTF/HOSSUB.\n";
	static public final String RESTRICTED_AMOUNT = "AU:Amount restricted to maximum admissible limit per child for academic year/first two quarters.\n";
	static public final String INCORRECT_REFERENCE_DOII = "AU:Incorrect reference do2.\n";
	static public final String RESTRICTED_AMOUNT_20082009 = "AU:Amount restricted as per the entitlement for academic year 2008-2009.\n";
	static public final String INVALID_COLUMN_A = "VL:Valid Column7A values are \" NUR, LKG, UKG OR   CL-01   OR   CL-02 .......... CL-12 \" only.\n";
	static public final String INVALID_COLUMN_B = "VL:Invalid DOB of child given in Column7B.\n";
	static public final String INVALID_COLUMN_C = "VL:Valid Column7C values are Q1, Q2, Q3 or Q4 only.\n";
	static public final String INVALID_INITIAL_COLUMN_C = "VL:If INITIAL, reference Do2 Number not to be given.\n";
	static public final String INCORRECT_INPUT_PROVIDED = "VL:Correct Date of birth of child (eg.01012000) should be given in Col.7B.\n";
	static public final String INVALID_TJRA_LRA_VALUES = "VL:Beginning of the academic year in TJRA column eg. 0409 and ending in LRA column eg. 0310 to be given.\n";
	static public final String CEA_PERIOD_NOT_MATCH_WITH_COLUMNC = "VL:Period between From date and To date and column 7C do not match.\n";
	static public final String CONCURRENTLY_INADMISSIBLE = "AU:Both CEA & HOSSUB not admissible concurrently.\n";
	static public final String AGE_BELOW_FIVE_YEARS = "AU:Child age below 5 years for CL-01 & above.\n";
	static public final String AGE_BELOW_TWO_YEARS = "AU:Child age below 2 years for NUR/LKG/UKG classes.\n";
	static public final String DOB_AFTER_FROM_DATE = "AU:Date of Birth of child is after from-date of DOII. \n";
	static public final String CONCURRENT_CEATWN_CEAHC = "AU:CEA/CEATWN/CEAHC not admissible concurrently. \n";
	static public final String TO_CLAIM_AS_CEATWN = "AU:Current claim may be claimed as CEATWN since DOB matches with earlier claim during the period. \n";
	static public final String CEATWN_FOR_ELDER_DOB = "AU:CEATWN claimed for child with elder DOB -- Pl check admissibility. \n ";
	static public final String SAME_ACADEMIC_YEAR_DIFFERENT_CLASS = "AU:Claim for different classes of study in the same academic year. Pl check \n";
	static public final String DUPLICATE_CLAIM = "DU:Already claim exists for same period for the child. \n";
	static public final String OVERLAPPING_ACADEMIC_YEAR = "AU:Overlapping academic year for the child. \n";
	static public final String ONLY_TWO_CLASSES_BELOW_CLASS_ONE_ADMISSIBLE = "AU: Only two classes i.e. either NUR/LKG or LKG/UKG or NUR/UKG is admissible for a child below Class-I. \n";
	static public final String PERIOD_OVERLAPPING_WITH_ALREADY_ADJUSTED_CLAIM = "OL: Period overlaps with already adjusted claim. \n"; 
	static public final String ALREADY_ADJUSTED_FOR_TWO_CHILDREN_FOR_THE_PERIOD = "AU: Already adjusted for two children for the period \n";
	static public final String NOT_ON_DEPUTATION = "AU:CEA is admissible to deputationists to BHUTAN/UNMISN only.";
	static public final String RESTRICTED_AMOUNT_20172018 = "AU:Amount restricted as per the entitlement for financial year 2017-2018.\n";
	static public final String CANCEL_REPUBLISH_WITH_CORRECT_PERIOD = "VL:FromDate And ToDate Overlaps 7cpc Allowance Date. Cancel And Republish With Split Date.\n";
	static public final String INVALID_VALUES_FOR_FINANCIAL_YEAR_2017_2018 = "VL:Invalid Values Provided In TJRA/LRA OR FromDate/ToDate For 7cpc CEA Claims In FY20172018.\n";
	static public final String TO_BE_CLAIMED_AFTER_FIN_YEAR_END = "AU:To Be Claimed/Published After Completion Of Financial Year.\n";
	static public final String INVALID_DATES_FOR_FINANCIAL_YEAR_CLAIMS = "VL:From Date And To Date Are Not Beginning And End Of Finanacial Year Respectively.\n";
	static public final String TJRA_LRA_VALUES_REQUIRED = "VL:Incorrect Values Given In Tjra/Lra Columns In Claims Or TJRA/LRA Is Blank.\n";
	static public final String CLAIM_PERIOD_NOT_12M = "VL:Claim Period Is Less/More Than 12 Months In 7th CPC.\n";
	static public final String INVALID_DO2_DATE = "VL:DOII Publication Date Before ToDate. \n";
	static public final String INVALID_DATES_FOR_FULL_YEAR_CLAIMS = "VL:From Date And To Date Mismatch For A Full Year Claim.\n";
	static public final String OVERLAP_7CPC="OL:Claim overlaps with already adjusted CEA.\n";
	static public final String LOWER_CLASS_CLAIM="VL:Claim For Lower Class For Child Whose Higher Class Claim Already Adjusted Previously.\n";
	static public final String FROM_DATE_BEFORE_ENROLLMENT_DATE="VL:DO2 FromDate Is Before Enrollment Date.\n";
	static public final String INVALID_FROM_DATE_FOR_DSC="VL:To Be Claimed In Previous PAO (For DSC) OR Claim Period Is Prior To Enrolled Year (For TA).\n";
	static public final String ENROLLMENT_DATE_NULL="VL:Enrollment Date Is Blank In Employee Table.\n";
	static public final String ONLY_THREE_CLASSES_BELOW_CLASS_ONE_ADMISSIBLE = "AU: Only three classes are admissible for a child below Class-I. \n";
		
	static java.util.HashMap<String, String> CLASS_MAP;

	static public HashMap<String, String> getClassMap() {
		if (CLASS_MAP != null)
			return CLASS_MAP;

		CLASS_MAP = new HashMap<String, String>();
		
		CLASS_MAP.put("CL-12","CL-11");
		CLASS_MAP.put("CL-11","CL-10");
		CLASS_MAP.put("CL-10","CL-09");
		CLASS_MAP.put("CL-09","CL-08");
		CLASS_MAP.put("CL-08","CL-07");
		CLASS_MAP.put("CL-07","CL-06");
		CLASS_MAP.put("CL-06","CL-05");
		CLASS_MAP.put("CL-05","CL-04");
		CLASS_MAP.put("CL-04","CL-03");
		CLASS_MAP.put("CL-03","CL-02");
		CLASS_MAP.put("CL-02","CL-01");
		return CLASS_MAP;
	}
	 
	@Override
	public boolean isValid(Do2 do2) {
	 	validity = false;
		fromDateCal.setTime(do2.getFromDate());
		toDateCal.setTime(do2.getToDate());
		if (do2.getColumnA() != null)	
			do2.setColumnA(do2.getColumnA().trim());
		if (do2.getColumnB() != null)	
			do2.setColumnB(do2.getColumnB().trim());
		col7b = do2.getColumnB();
		if (do2.getArmyNo() != null)
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (employee != null){
			do2.setEmployee(employee);
		}
		curqe = Utils.getCurrentQuarterEnding();
		try {
			thirtyFirstMarch2018 = DateManager.getDate("2018-03-31");
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
		if (isCeaCommonValid(do2)) {			
			if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) && do2.getToDate().before(DateManager.getSixthCpcAllowanceDate())) {
				if (isOldCeaValid(do2)) {
					totalRate = (do2.getRate()*DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()));	
					restrictValidity = false;
					validity = true;
					do2.setStatus(Do2Manager.VALID);
					if (do2.getAcceptedQe()==null) {
						do2.setAcceptedQe(curqe);	
					} else {
						if (!do2.getAcceptedQe().equals(curqe)){
							do2.setReviewQe(curqe);
						}
					}
				} else {
					validity = false;
				}
			} else if (do2.getTjra()!=null && do2.getTjra()%100!=17 && 
				do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) && do2.getToDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					if (is6cpcCeaValid(do2)) {
						validity = true;
				 		do2.setStatus(Do2Manager.VALID);
				 		if (do2.getAcceptedQe()==null) {
				 			do2.setAcceptedQe(curqe);
				 		} else {
				 			if(!do2.getAcceptedQe().equals(curqe)){
				 				do2.setReviewQe(curqe);
				 			}
				 		}
					} else {
						validity = false;
					}
			} else if (!do2.getFromDate().after(thirtyFirstMarch2018) && !do2.getToDate().after(thirtyFirstMarch2018)) {
					year2017_18=true;
					if (isCeaForYear20172018Valid(do2)) {
						validity = true;
				 		do2.setStatus(Do2Manager.VALID);
				 		if (do2.getAcceptedQe()==null) {
				 			do2.setAcceptedQe(curqe);
				 		} else {
				 			if(!do2.getAcceptedQe().equals(curqe)){
				 				do2.setReviewQe(curqe);
				 			}
				 		}
					} else {
						validity = false;
					}				  		
			} /*
				 * else if (!isValidTjraLraValues(do2)) { // Added on 15.04.2019  // Commented on 24.05.2019
				 * buffer.append(INVALID_TJRA_LRA_VALUES); validity = false; }
				 */ else if (is7cpcCeaValid(do2)) {			 
					validity = true;
					do2.setStatus(Do2Manager.VALID);
					if (do2.getAcceptedQe()==null) {
						do2.setAcceptedQe(curqe);
					} else {
						if(!do2.getAcceptedQe().equals(curqe)) {
							do2.setReviewQe(curqe);
						}
					}					
			} else {			 
				validity = false;
			}
		}		
		if (!validity) {
			do2.setColumnB(col7b);
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		}
		do2Repo.save(do2);
				
		return validity;
	}
	
	//Calling common validation methods for both old and new rates of CEA 
	private boolean isCeaCommonValid(Do2 do2) {
		boolean commonValidity = true;
		if (!isValidFormat(do2)) {
			buffer.append(NOT_VALID_FORMAT);  
			commonValidity = false;
		}
		
 		if (fromDateCal.get(Calendar.DATE)>1) {
			buffer.append(INVALID_FROM_DATE); 
			commonValidity = false;
		}
		
		if (!isValidToDate()) {
			buffer.append(INVALID_TO_DATE);  
			commonValidity = false;
		}
		
		if (do2.getDo2Date().before(do2.getToDate())) {
			buffer.append(INVALID_DO2_DATE);  
			commonValidity = false;
		}		
		
		if (isOnDeputationOtherthanBhutanUNMISN(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NOT_ON_DEPUTATION);
			commonValidity = false;
		}
		
		if (do2.getFromDate()!=null && !do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) && !do2.getFromDate().after(thirtyFirstMarch2018)
				&& do2.getToDate()!=null && !do2.getToDate().before(DateManager.getSixthCpcAllowanceDate()) && !do2.getToDate().after(thirtyFirstMarch2018) 
				&& (do2.getTjra()==null || (do2.getTjra()!=null && do2.getTjra()==0) || do2.getLra()==null || (do2.getLra()!=null && do2.getLra()==0))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(TJRA_LRA_VALUES_REQUIRED);
			commonValidity = false;			
		}
		
		if (employee!=null && employee.getEnrolmentDate()!=null) {
			if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
				if (employee.getRegularTaFlag()!=null && !employee.getRegularTaFlag().equals("T")) {
					if (do2.getFromDate().before(employee.getEnrolmentDate())) {
						commonValidity=false;
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
						commonValidity=false;
						buffer.append(INVALID_FROM_DATE_FOR_DSC);
					} else if (fromDateYear==(enrolYear-1)) {
						if (fromDateMonth<3) {
							commonValidity=false;
							buffer.append(INVALID_FROM_DATE_FOR_DSC);
						}						 
					}	
				}				
			}
		} else {
			commonValidity=false;
			buffer.append(ENROLLMENT_DATE_NULL);	
		}
		return commonValidity;
	}

	//Calling specific methods for validation of old CEA 
	private boolean isOldCeaValid(Do2 do2) {
		boolean oldValidity = true;
		
		if (!whetherValidInitialClaim(do2)) {
			buffer.append(INVALID_INITIAL_COLUMN_C);  
			oldValidity = false;
		}  
		
		if (!isValidColumnAForOldCea(do2)) {
			buffer.append(INVALID_COLUMN_A_OLD_CEA);
			oldValidity = false;
		}
		
		if (!isValidColumnBForOldCea(do2)) {
			buffer.append(INVALID_COLUMN_B_OLD_CEA);
			oldValidity = false;
		}
		
		if (oldValidity == true) {
			if (getNoOfChildren(do2) > 3) {
				buffer.append(CHILDREN_LIMIT_EXCEEDED);  
				oldValidity = false;
			}
			if (!isOverlapping(do2)) {
				buffer.append(OVERLAP); 
				if (overlappingDo2!=null)
					buffer.append(" Already adjusted Do2 No. "+overlappingDo2.getDo2Type()+"/"+overlappingDo2.getDo2No()
      				+"/"+overlappingDo2.getDo2ItemNo()+" Do2 date  :  "+overlappingDo2.getDo2Date()+" "+overlappingDo2.getDescription()
      				+ " " +overlappingDo2.getFromDate()+" to "+overlappingDo2.getToDate());
				oldValidity = false;
			}
		
			if (!isValidAmountInRateColumn(do2)) {
				buffer.append(INVALID_AMOUNT_OLD_CEA);  
				oldValidity = false;
			}
		}
		return oldValidity;
	}

	//Calling specific methods for validation of new CEA
	private boolean is6cpcCeaValid(Do2 do2) {
		boolean newValidity = true;	
		
		if (!isValidTjraLraValues(do2)) {
			buffer.append(INVALID_TJRA_LRA_VALUES);
			newValidity = false;
			return newValidity;
		}
		
	 	if (!isValidColumnAIn6cpcCea(do2)) {
			buffer.append(INVALID_COLUMN_A);  
			newValidity = false;
			return newValidity;
		}		
		if (!isValidColumnCIn6cpcCea(do2)) {			
			newValidity = false;
			return newValidity;
		}
		if (!isValidRateIn6cpcCea(do2)) {
			buffer.append(INVALID_AMOUNT_NEW_CEA);
		 	newValidity = false;
		}	
		if (!isValidColumnBIn6cpcCea(do2)){
 			buffer.append(INVALID_COLUMN_B);  
 			newValidity = false;
 			return newValidity;
		}
		if (!isValidAge(do2, reversedob)) {
	 		newValidity = false;
	 		return newValidity;
	 	}
		
		if (!isAcademicYearUniqueForTheChild(do2)) {
			newValidity = false;
			return newValidity;
		}
		
		if (isOverlapping6cpcCea(do2)) {			
			if (overlappingDo2!=null) {
				buffer.append(OVERLAP);
      			buffer.append(" Already adjusted Do2 No. "+overlappingDo2.getDo2Type()+"/"+overlappingDo2.getDo2No()
      				+"/"+overlappingDo2.getDo2ItemNo()+" Do2 date  :  "+overlappingDo2.getDo2Date()+" "+overlappingDo2.getDescription()
      				+ " " +overlappingDo2.getFromDate()+" to "+overlappingDo2.getToDate());
			}
			newValidity = false;
			if (!concurrentValidity) {
				buffer.append(CONCURRENTLY_INADMISSIBLE);
			}
			if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT") || do2.getDescription().equals("CEATWN")) {
				if (concurrentCeatwnCeahc) {
					buffer.append(CONCURRENT_CEATWN_CEAHC);
				}
			}
			
			if (do2.getDescription().equals("CEA") || do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) {
				if (toClaimAsCeatwn) {
					buffer.append(TO_CLAIM_AS_CEATWN);
				}
			}
			return newValidity;
		}	
		
		if (whetherMoreThanTwoChildren(do2)) {
			
			if (ceatwnForElderDob) {
				buffer.append(CEATWN_FOR_ELDER_DOB);
			} else {
				buffer.append(CHILDREN_LIMIT_EXCEED_NEW_FORMAT);
			}
			newValidity = false;
			return newValidity;
		}	
	 
 	 	if (!do2.getDescription().startsWith("CEAH") && !do2.getColumnA().startsWith("CL")) {
 	 		if (!isTheConditionOnlyTwoClassesBelowClassOneSatisfied(do2)) {
 	 			buffer.append(ONLY_TWO_CLASSES_BELOW_CLASS_ONE_ADMISSIBLE);
				newValidity = false;
 	 		}
 		} 			
	  	return newValidity ;
	}

	@Override
	public String process(Do2 do2) {
			
	//	System.out.println(".......................... cea process   ano " +do2.getArmyNo() +"   do2 id  " +do2.getId());
		
			if (do2.getStatus()!=null && do2.getStatus().equals(Do2Manager.VALID))  {			
				
				if (!isArrearsAlreadyAdjustedForDo2(do2)) {
					if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
						Utils.addArrears(do2, get5CPCAmount(do2));
					} else if (!year2017_18 && do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())
							&& do2.getToDate().before(DateManager.getSeventhCpcAllowanceDate())) {
						Utils.addArrears(do2, get6CPCAmount(do2));
					} else if (year2017_18 && !do2.getFromDate().after(thirtyFirstMarch2018) && !do2.getToDate().after(thirtyFirstMarch2018)) {
		 				Utils.addArrears(do2, getAmountFor20172018(do2));
		//			} else if (!year2017_18 && do2.getFromDate().after(thirtyFirstMarch2018) && do2.getToDate().after(thirtyFirstMarch2018)) {
					} else if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) && do2.getToDate().after(DateManager.getSeventhCpcAllowanceDate())) {
						Utils.addArrears(do2, get7CPCAmount(do2));		
					}					
				}
				
				if (restrictValidity) {
					if (year2008_09) {
						buffer.append(RESTRICTED_AMOUNT_20082009);  
					} else {
						buffer.append(RESTRICTED_AMOUNT);  
					}
					message = buffer.toString();
					do2.setReason(message);
				}
				
				if (do2.getDescription().trim().equals("CEATWN")) {
					buffer.append("PLEASE VERIFY FAMILY DETAILS FOR ADJUSTED CEATWN DOs2");
					message = buffer.toString();
					do2.setReason(message);
				}
				
				if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()))
					do2.setColumnB(getDateOfBirthInColumnBInReverseOrder(do2));
			}
						
			do2Repo.save(do2);			
		
 		return Do2Manager.SUCCESS;
	}
	
	//Method to ensure that proper Appendix-J formats are used for claiming CEA
	private boolean isValidFormat(Do2 do2) {
	 	if (!(do2.getFromDate()).before(DateManager.getSixthCpcAllowanceDate())
				&& (!(do2.getToDate()).before(DateManager.getSixthCpcAllowanceDate()))) {
	 		if ((do2.getColumnA()==null) 
	 				|| (do2.getColumnA()!=null && (do2.getColumnA().startsWith("PRY") || do2.getColumnA().startsWith("SEC")))) {
				if ((do2.getColumnB()==null) 
						|| (do2.getColumnB()!=null && (do2.getColumnB().startsWith("SEC") || do2.getColumnB().startsWith("PRY")))) {
	 				return false;
				}
			}
		} else {
			if (do2.getColumnA()!=null) {
				if (do2.getColumnA().startsWith("CL-")) {
	 				return false;
				}
	 			if (!(do2.getColumnA().startsWith("PRY") || do2.getColumnA().startsWith("SEC"))) {
	 				return false;
				}
	 		}
			if (do2.getColumnB()!=null) {
				if (do2.getColumnB().startsWith("CL-")) {
	 				return false;
				}
	 			if (!(do2.getColumnB().startsWith("PRY") || do2.getColumnB().startsWith("SEC"))) {
	 				return false;
				}
	 		}
		}
	 	return true;
	}

 	//Method to ensure that todate in current DO2 is the last date of the quarter ending month 
	private boolean isValidToDate() {
		int days = DateManager.getNoOfDaysInMonth(toDateCal.get(Calendar.MONTH)+1, toDateCal.get(Calendar.YEAR));
 		return (toDateCal.get(Calendar.DATE)==days); 
	}
	
	private boolean isValidColumnAForOldCea(Do2 do2) {
		if (do2.getColumnA()==null && do2.getColumnB()==null) {
			return false;
		}
		
		if (do2.getColumnA()!=null
				&& !(do2.getColumnA().startsWith("PRY-") || do2.getColumnA().startsWith("SEC-"))) {
			return false;    
		}
		
		try {
			if (do2.getColumnA()!=null && do2.getColumnA().length()==5 
					&& Integer.parseInt(do2.getColumnA().substring(4,5))>0 && Integer.parseInt(do2.getColumnA().substring(4,5))<3) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isValidColumnBForOldCea(Do2 do2) {
		if (do2.getColumnA()==null && do2.getColumnB()==null) {
			return false;
		}
		
		if (do2.getColumnA()!=null && do2.getColumnB()==null) {
			return true;
		}
	 	
		if (do2.getColumnB()!=null 
				&& !(do2.getColumnB().startsWith("PRY-") || do2.getColumnB().startsWith("SEC-"))) {
			return false;
		}
		
		try {
			if (do2.getColumnB()!=null && do2.getColumnB().length()==5 &&  
					Integer.parseInt(do2.getColumnB().substring(4,5))>0 && Integer.parseInt(do2.getColumnB().substring(4,5))<3) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	// Method to check overlapping of current DO2 with already adjusted DOs2 in respect of old rates of CEA
	private boolean isOverlapping(Do2 do2) {
		boolean validityOverlap = true;
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			List<String> descList=Arrays.asList(new String[] {"CEA","RTF","HOSSUB"});
			
			List<Do2> do2List = do2Repo.findByDescListFromDateBetweenOrToDateBetween(do2.getEmployee().getId(),Do2Manager.VALID, descList,do2.getFromDate(),do2.getToDate());
				 

			if (do2List.size() > 0) {
				for (Do2 do2Overlap : do2List) {
					mutexDescription = do2Overlap.getDescription();
					if (mutexDescription.equals("CEA") || mutexDescription.equals("RTF"))
						mutexNoOfChildren += getNoOfChildren(do2Overlap);
		
					mutexCol7A = do2Overlap.getColumnA();
					mutexCol7B = do2Overlap.getColumnB();

					try {
						if (((mutexDescription.equals("CEA")) && (mutexNoOfChildren	+ getNoOfChildren(do2) > 3))
								|| (mutexDescription.equals("RTF"))
								|| (mutexDescription.equals("HOSSUB"))) {
							validityOverlap = false;
							overlappingDo2 = do2Overlap;
	 					} else {
							if (mutexDescription.equals("CEA")) {
								if (((mutexCol7A!=null) && (mutexCol7A.equals(do2.getColumnA()))) 
										|| ((mutexCol7B!=null) && (mutexCol7B.equals(do2.getColumnB())))) {
									if ((mutexNoOfChildren + getNoOfChildren(do2) > 3)) {
										validityOverlap = false;
										overlappingDo2 = do2Overlap;
	 								}  
								}
							}
						}
					} catch (Exception e) {
						Utils.handleException(e);
					}
				}
			}
		}		
 		return validityOverlap;
 	 }
	Date adoptedDob = null;
	// Method to check overlapping of current DO2 with already adjusted DOs2 of HOSSUB or CEA
	private boolean isOverlapping6cpcCea(Do2 do2) {
		
 		List<Do2> validDo2List = do2Repo.findCeaOverlapping6Cpc(do2.getEmployee().getId(),do2.getId(),Do2Manager.VALID,do2.getFromDate(),do2.getToDate());
 		 
		if (validDo2List.size() > 0) {
			//modified on 04102012, added mob & dob in checking along with yob
			String dateInReverse = getDateOfBirthInColumnBInReverseOrder(do2);
			String ymobInReverse = dateInReverse.substring(0,6);					
			for (Do2 oldDo2 : validDo2List) {
				if(adoptedDob==null) {
			 		if ((!do2.getDescription().equals("CEATWN") && oldDo2.getDescription().equals(do2.getDescription()) && 
						(oldDo2.getColumnB() != null && oldDo2.getColumnB().substring(0,6).equals(ymobInReverse)))) {
			 			if (!oldDo2.getColumnB().equals(dateInReverse)) {
			 				// Since yyyymm of dob checked while getting value for adoptedDob column in another method, only date is being checked below in this condition - MEG RTI email 11012023.
			 				List<String> relationList = Arrays.asList("SON", "DAUGHTER");			 		 
			 				List<FamilyDetail> fdList = fdRepo.findByEmpRelationListAndAdoptedTrue(do2.getEmployee().getId(),relationList);
							if (fdList!=null && fdList.size() > 0) {
								return false;
								// commented on 29.04.2025 PRC Ramgarh - IHQ complaint
							/*	adoptedDob = fdList.get(0).getDateOfBirth();
								Calendar cal = new GregorianCalendar();
								cal.setTime(adoptedDob);
								if (Integer.parseInt(dateInReverse.substring(6,8))!=cal.get(Calendar.DATE)) {
									overlappingDo2 = oldDo2;
							 		return true;
								} else {
									return false;
								}*/
							} else {
								overlappingDo2 = oldDo2;
								return true;
							}
						}  else {
							overlappingDo2 = oldDo2;
					 		return true;	
						}	
			 		}
		 		} else if (adoptedDob!=null) {
					if ((!do2.getDescription().equals("CEATWN") && oldDo2.getDescription().equals(do2.getDescription()) && 
							(oldDo2.getColumnB() != null && oldDo2.getColumnB().substring(0,6).equals(ymobInReverse)))) {				 		
				 			overlappingDo2 = oldDo2;
					 		return true;
				 		}		 		
				}
				if ((!do2.getDescription().equals("CEATWN") && oldDo2.getDescription().equals(do2.getDescription()) && 
						(oldDo2.getColumnB() != null && oldDo2.getColumnB().substring(0,6).equals(ymobInReverse)))) {
			 			overlappingDo2 = oldDo2;
				 		return true;
			 		}
		 		
				if ((oldDo2.getDescription().equals("HOSSUB")) && 
					(oldDo2.getColumnB() != null && 
					 oldDo2.getColumnB().substring(0,6).equals(ymobInReverse))) {
					if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) { // added on 12.05.2020 to make it concurrently admissible in 7cpc
						concurrentValidity = false;						
						overlappingDo2 = oldDo2;
						return true;
					}						
				}
								 
				
			
				if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					if ((do2.getDescription().equals("CEATWN") || do2.getDescription().equals("HSBTWN")) && 
							 (oldDo2.getDescription().equals("CEAHC") || oldDo2.getDescription().equals("CEAHCT") || oldDo2.getDescription().equals("HOSSUB") ||
							  oldDo2.getDescription().equals("CEA") || oldDo2.getDescription().equals("CEATWN"))) {
						if (!ymobInReverse.equals(oldDo2.getColumnB().substring(0,6))) {
							try {
								if (Integer.parseInt(ymobInReverse) < Integer.parseInt(oldDo2.getColumnB().substring(0,6))) {
									buffer.append("Drawing CEA/HOSSUB for more than one child. \n");
									return true;	
								}
							} catch (Exception e) {
								Utils.handleException(e);
							}
						}
					}
					
					if ((do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) && oldDo2.getDescription().equals("CEATWN")) {
						if (!ymobInReverse.equals(oldDo2.getColumnB().substring(0,6))) {
							try {
								if (Integer.parseInt(ymobInReverse) < Integer.parseInt(oldDo2.getColumnB().substring(0,6))) {
									buffer.append("Drawing CEA for more than one child");
									return true;	
								}
							} catch (Exception e) {
								Utils.handleException(e);
							}
						}
					}
					
				} else { // Added on 12.05.2020 for HOSSUB in above condition in 7cpc as below
					if (do2.getDescription().equals("CEATWN") && 
							 (oldDo2.getDescription().equals("CEAHC") || oldDo2.getDescription().equals("CEAHCT") ||
							  oldDo2.getDescription().equals("CEA") || oldDo2.getDescription().equals("CEATWN"))) {
						if (!ymobInReverse.equals(oldDo2.getColumnB().substring(0,6))) {
							try {
								if (Integer.parseInt(ymobInReverse) < Integer.parseInt(oldDo2.getColumnB().substring(0,6))) {
									buffer.append("Drawing CEA More Than Admissible Children In 7cpc. \n");
									return true;	
								}
							} catch (Exception e) {
								Utils.handleException(e);
							}
						}
					}
					
					if ((do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) && oldDo2.getDescription().equals("CEATWN")) {
						if (!ymobInReverse.equals(oldDo2.getColumnB().substring(0,6))) {
							try {
								if (Integer.parseInt(ymobInReverse) < Integer.parseInt(oldDo2.getColumnB().substring(0,6))) {
									String dobInReverse    = getDateOfBirthInColumnBInReverseOrder(do2);
									List<String> validColBList=do2Repo.findCeaDistinctColBList(do2.getEmployee().getId(), DateManager.getSixthCpcAllowanceDate(), do2.getId()); 
									
									if (validColBList.size()==2) {
										for (int i = 0; i < validColBList.size(); i++){
											if (dobInReverse.equals(validColBList.get(i).trim())) {
												return false ;
											} else {
												
												buffer.append("Drawing CEA for more than one child");
												return true;	
											}
											
										}
									}
									
									
								}
							} catch (Exception e) {
								Utils.handleException(e);
							}
						}
					}
				}
			/*
				if ((do2.getDescription().equals("CEA") && (oldDo2.getDescription().equals("CEAHC") || oldDo2.getDescription().equals("CEAHCT")))
						|| ((do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) && oldDo2.getDescription().equals("CEA"))) { 
					if (ymobInReverse.equals(oldDo2.getColumnB().substring(0,6))) {
						toClaimAsCeatwn = true;
						return true;
					}
				} */
				
				
				if ((do2.getDescription().equals("CEA") && (oldDo2.getDescription().equals("CEAHC") || oldDo2.getDescription().equals("CEAHCT")))
						|| ((do2.getDescription().equals("CEAHC")) && oldDo2.getDescription().equals("CEA"))) { 
					if (dateInReverse.equals(oldDo2.getColumnB().substring(0,6))) {
						toClaimAsCeatwn = true;
						return true;
					}
				}
			}
			Boolean ceaAdjusted = false;
		 	
			for (Do2 oldDo2 : validDo2List) {
				if (do2.getDescription().equals("CEATWN") && (oldDo2.getDescription().trim().equals("CEA") || oldDo2.getDescription().trim().equals("CEAHC")
						|| do2.getDescription().trim().equals("CEAHCT"))) {
					if (oldDo2.getColumnB()!=null && oldDo2.getColumnB().substring(0,6).trim().equals(ymobInReverse)) {
						ceaAdjusted = true;
				 	} else if (oldDo2.getColumnB()!=null && Integer.parseInt(ymobInReverse) > Integer.parseInt(oldDo2.getColumnB().substring(0,6))) {
				 		ceaAdjusted = true;	
				 	}
				}	
			}
		 	
	 		if (do2.getDescription().equals("CEATWN") && ceaAdjusted == false){
				buffer.append("CEAHC/CEA not adjusted for one of twin children");
				return true;
			}
		}
		return false;
	}

	//Method of get total number of children for whom CEA is claimed/admitted in respect of old rates
	private int getNoOfChildren(Do2 do2) {
		int totalChildren = 0;
		
			if (do2.getColumnA()!=null) {
			 	totalChildren += Integer.parseInt((do2.getColumnA().substring(4, 5)));
			} else {
				totalChildren += 0;
			}

			if (do2.getColumnB()!=null) {
				totalChildren += Integer.parseInt((do2.getColumnB().substring(4, 5)));
			} else {
				totalChildren += 0;
			}
		
		return totalChildren;
	}

	//Method to validate that either INITIAL or reference DO2 only is given in current DO2
	private boolean whetherValidInitialClaim(Do2 do2) {
	
		if ((do2.getColumnC()==null || (do2.getColumnC()!=null && do2.getColumnC().length()==0)) && do2.getRefDo2No()!=null) {
			return true;
		}
	

		if (do2.getColumnC()!=null && do2.getColumnC().trim().equalsIgnoreCase("INITIAL") && do2.getRefDo2No()==null) {
			 return true;
		}
		 
		return false;
	}

	// Method to check whether valid reference DO2 is given, but does not ensure that it actually pertains to appropriate earlier claim	
	private boolean whetherValidRefDo2(Do2 do2) {
		
		Do2 oldDo2 = Utils.getReferenceDo2(do2);
		if (oldDo2 != null) {
			if ((oldDo2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) && 
				(oldDo2.getToDate().before(DateManager.getSixthCpcAllowanceDate())) && 
				(oldDo2.getDescription().equals(do2.getDescription())) && (do2.getColumnC()!=null)) {
				return true;
			} else if (oldDo2.getColumnB()!=null && do2.getColumnB()!=null 
							&& oldDo2.getColumnB().trim().equals(do2.getColumnB())) {
				return true;
			}
		} 
		return false;
	}

	//Method to check valid rate is given in respect of old CEA claim
	private boolean isValidAmountInRateColumn(Do2 do2) {
		return (do2.getRate()>0 && do2.getRate()==(getNoOfChildren(do2)*100)); 
	}

    // Method to get valid amount in respect of new CEA claim and to ensure that total amount does
	// not exceed maximum amount per child per annum
	
	private double get6CPCAmount(Do2 do2) {
		totalRate = 0;
		double maximumAmount = 0;
		double do2Amount = 0;
		double diffInClaimedAdjustedAmount = 0;
		boolean ceaForSecondHalfAdjusted=false;		
		boolean secondCeatwnInProcessForAdj=false;		
		int fromYear = do2.getTjra()%100;
		int toYear = do2.getLra()%100;		
		maximumAmount=getAmountFor6cpcCea(do2);
		if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) {
			maximumAmount = maximumAmount * 2;
		}
		List<Do2> do2List = null;
	 	do2List = do2Repo.findCeaByDobAndRate(do2.getEmployee().getId(),do2.getId(),do2.getDescription(), getDateOfBirthInColumnBInReverseOrder(do2).substring(0,6), fromYear,toYear);
	 	 
 	  	for (Do2 oldDo2 : do2List) {
 	  		if (oldDo2.getColumnC().trim().equals("Q3") || oldDo2.getColumnC().trim().equals("Q4")) {
 	  			ceaForSecondHalfAdjusted=true;
 	  		}
			List<Arrears> arrearList = arrRepo.findByDo2Id(oldDo2.getId());
				 
			if (arrearList!=null && arrearList.size()>0) {
				for (Arrears arr : arrearList)
					totalRate+=arr.getAmount();
			}
			do2Amount+=oldDo2.getRate();
			// Below condition added to adjust triplets claims- one CEA and two CEATWN
			if (do2.getDescription().trim().equals("CEATWN") && oldDo2.getDescription().trim().equals("CEATWN")) {
	 			secondCeatwnInProcessForAdj=true;				
			}
	  	}
 	  	if (secondCeatwnInProcessForAdj) {
 	  		maximumAmount = maximumAmount*2;	
 	  	} 	  	
 		diffInClaimedAdjustedAmount = do2Amount - totalRate;
		if (year2008_09) {			
			if ((totalRate+diffInClaimedAdjustedAmount+do2.getRate())>maximumAmount) {
				totalRate = maximumAmount - totalRate;
		  	} else {
		  		totalRate = do2.getRate() + diffInClaimedAdjustedAmount;
		 	}
		}
			
		if (!year2008_09) {
			double amount = maximumAmount;
			if (!ceaForSecondHalfAdjusted && (do2.getColumnC().trim().equals("Q1") || do2.getColumnC().trim().equals("Q2"))){
				amount = amount / 2;
			}
			if ((totalRate + diffInClaimedAdjustedAmount + do2.getRate()) > amount) {
				restrictValidity = true;
				totalRate = amount - totalRate;
			} else {
				totalRate = do2.getRate() + diffInClaimedAdjustedAmount;
			}
		}
 	 	return totalRate;
	}		
	
	private double get5CPCAmount(Do2 do2) {
		totalRate = 0;
		int children=0;
		  
		if (do2.getColumnA()!=null)
			children = Integer.parseInt(do2.getColumnA().substring(4,5));
		 
		if (do2.getColumnB()!=null)
			children += Integer.parseInt(do2.getColumnA().substring(4,5));
		
		totalRate = DateManager.getNoOfMonths(do2.getFromDate(),do2.getToDate())*children*100;
		
		return totalRate;
	}
		
	//Method to check whether claim is for more than two children in respect of new CEA rates.
	private boolean whetherMoreThanTwoChildren(Do2 do2) {
		boolean fpoFailed = false;
		Date fpoFailedDate = null;
		Date birthDate = null;
		Date demiseDate = null;
		Date demisedChildDob = null;
		Date changeDobDate = null;
		
		List<Do2>strincList;
		Date currentDob = DateManager.getDateFromDdMmYyyy(do2.getColumnB().trim());
		List<String>children = new ArrayList<String>();
		strincList = do2Repo.findByDescStatusColAOrderByFromDateDesc(do2.getEmployee().getId(),Do2Manager.VALID,"STRINC", "CEASED");
		 
		if (strincList != null && strincList.size() > 0){
			fpoFailedDate = strincList.get(0).getFromDate();
			if (fpoFailedDate!=null) {
				fpoFailed = true;
			}
		}
		List<String> relList=Arrays.asList(new String[] {"SON","DAUGHTER"});
		
		List<String> validColBList =do2Repo.findCeaDistinctColBList(do2.getEmployee().getId(), DateManager.getSixthCpcAllowanceDate(), do2.getId()); 
		
		 
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
		
		
		System.out.println ( " children "  +children.size() );
		
		List<FamilyDetail> fdList = fdRepo.findByEmpRelationListAndStatus(do2.getEmployee().getId(), relList,"DEMISE"); 
		 
		if (fdList != null && fdList.size() > 0){
			for (int i = 0; i < fdList.size(); i++){
				demiseDate = fdList.get(i).getToDate();
				demisedChildDob=fdList.get(i).getDateOfBirth();	
				System.out.println ( " demise date " +demiseDate   +" demise child dob " +demisedChildDob);
				System.out.println ( " do2 from date " +do2.getFromDate());
                // NEW
                if (demiseDate != null && !do2.getToDate().before(demiseDate)){
                    String demisedColB = getDateOfBirthInReverseOrder(DateManager.getStringDateWithoutSeparators(demisedChildDob));
                    List<Do2> demisedChildSamePeriodClaims = do2Repo.findValidCeaByColBAndPeriod(
                            do2.getEmployee().getId(), Do2Manager.VALID, demisedColB, do2.getFromDate(), do2.getToDate());

                    if (demisedChildSamePeriodClaims != null && demisedChildSamePeriodClaims.size() > 0) {
                        // Demised child already has a VALID CEA claim for this exact FY -
                        // reject the new (3rd child) claim instead of allowing both.
                        Do2 conflictingDo2 = demisedChildSamePeriodClaims.get(0);
                        overlappingDo2 = conflictingDo2;
                        buffer.append("AU:CEA already claimed for demised child for the same period - Pl check.\n");
                        return true; // treated as "more than two children" -> claim rejected
                    }

                    for (int j = 0; j < children.size(); j++) {
                        birthDate = null;
                        try {
                            birthDate = DateManager.getDate(children.get(j).substring(0,4)+"-"+children.get(j).substring(4,6)+"-"+children.get(j).substring(6));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (demisedChildDob != null && birthDate.equals(demisedChildDob)) {
                            children.remove(j);
                            j--;
                        }
                    }
                }
			}
			
		}
		
		System.out.println ( " childrenxxxx "  +children.size() );
		
		//commented on 02012026-NORANDP

		/*
		 * List<FamilyDetail> fdListchdb =
		 * fdRepo.findByEmpRelationListAndStatus(do2.getEmployee().getId(),
		 * relList,"CHFMDB"); Date initialDobDate=null; if (fdListchdb != null &&
		 * fdListchdb.size() > 0){ for (int i = 0; i < fdListchdb.size(); i++){
		 * changeDobDate=fdListchdb.get(i).getDateOfBirth(); String columnC =
		 * fdListchdb.get(i).getDo2().getColumnC(); // System.out.println ( " ColumnC "
		 * +columnC + "year" + columnC.substring(6) + "month  " +columnC.substring(3,5)
		 * + "date "+columnC.substring(0,2) );
		 * 
		 * try { initialDobDate =
		 * DateManager.getDate(columnC.substring(6)+"-"+columnC.substring(3,5)+"-"+
		 * columnC.substring(0,2)); } catch (Exception e) { // TODO Auto-generated catch
		 * block e.printStackTrace(); } // System.out.println ( " Initail dob " +columnC
		 * +"   xx "+initialDobDate); for (int j = 0; j < children.size(); j++) {
		 * birthDate = null; try { birthDate =
		 * DateManager.getDate(children.get(j).substring(0,4)+"-"+children.get(j).
		 * substring(4,6)+"-"+children.get(j).substring(6)); } catch (Exception e) { //
		 * TODO Auto-generated catch block e.printStackTrace(); } // System.out.println
		 * ( " birth date " +birthDate ); if (changeDobDate != null &&
		 * birthDate.equals(initialDobDate)) children.remove(j);
		 * 
		 * } }
		 * 
		 * 
		 * }
		 */
        List<FamilyDetail> fdListchdb =
                fdRepo.findByEmpRelationListAndStatus(
                        do2.getEmployee().getId(), relList, "CHFMDB");

        System.out.println(">>> FD LIST SIZE (CHFMDB) : "
                + (fdListchdb == null ? "NULL" : fdListchdb.size()));

        Date initialDobDate = null;

        for (int i = 0; i < fdListchdb.size(); i++) {

            changeDobDate = fdListchdb.get(i).getDateOfBirth();
            String columnC = fdListchdb.get(i).getDo2().getColumnC();

            System.out.println("\n--- Processing FamilyDetail Index : " + i);
            System.out.println("DO2 ID   : " + fdListchdb.get(i).getDo2().getId());
            System.out.println("ColumnC  : " + columnC);

            initialDobDate = null;

            if (columnC != null) {
                columnC = columnC.trim();

                // strictly DD/MM/YYYY only
                if (columnC.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    try {
                        initialDobDate = DateManager.getDate(
                                columnC.substring(6, 10) + "-" +
                                        columnC.substring(3, 5) + "-" +
                                        columnC.substring(0, 2)
                        );
                    } catch (Exception e) {
                        System.out.println("!! Error parsing ColumnC date : " + columnC);
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("!! ColumnC not in DD/MM/YYYY, skipped : " + columnC);
                }
            } else {
                System.out.println("!! ColumnC is NULL");
            }

            if (initialDobDate == null) {
                continue;
            }

            for (int j = 0; j < children.size(); j++) {

                birthDate = null;

                try {
                    birthDate = DateManager.getDate(
                            children.get(j).substring(0, 4) + "-" +
                                    children.get(j).substring(4, 6) + "-" +
                                    children.get(j).substring(6)
                    );
                } catch (Exception e) {
                    System.out.println("!! Error parsing child DOB : " + children.get(j));
                    e.printStackTrace();
                }

                if (changeDobDate != null && birthDate != null && birthDate.equals(initialDobDate)) {
                    System.out.println(">>> MATCH FOUND – Removing child DOB : " + children.get(j));
                    children.remove(j);
                    j--; // important when removing
                }
            }
        }		
		
		

		/*
		
		List<FamilyDetail> fdList = fdRepo.findByEmpRelationListAndStatus(do2.getEmployee().getId(), relList,"DEMISE"); 
		 
		if (fdList != null && fdList.size() > 0){
			demiseDate = fdList.get(0).getToDate();
			demisedChildDob=fdList.get(0).getDateOfBirth();			
		}
*/
		if (do2.getDescription().equals("CEATWN")) {
			String dobOfAnotherChildOfTwins = getDateOfBirthInColumnBInReverseOrder(do2);
			List<String> descList=Arrays.asList(new String[] {"CEA","CEAHC"});
			System.out.println(".......................dobofanotherchildoftwins  " +dobOfAnotherChildOfTwins);
			List<String> ceaList =do2Repo.findCeaDobListByDescList(do2.getEmployee().getId(),DateManager.getSixthCpcAllowanceDate(), getDateOfBirthInColumnBInReverseOrder(do2),descList) ;
			System.out.println("..........cea list size ..............."+ceaList.size());			
			if (ceaList==null || (ceaList!=null && ceaList.size()==0)) {
				ceaList=null;
				List<String> colBList=new ArrayList<String>();
				String dobOfAnotherChildOfTwinsString = dobOfAnotherChildOfTwins.substring(0,4)+"-"+
													dobOfAnotherChildOfTwins.substring(4,6)+"-"+
													dobOfAnotherChildOfTwins.substring(6,8);
				Date dobTwin = DateManager.getDateFromYyyyMmDd(dobOfAnotherChildOfTwinsString);
				Date oneDayPriorDobTwin = DateManager.getPreviousDate(dobTwin);
				Date oneDayAfterDobTwin = DateManager.getNextDate(dobTwin);
				System.out.println("### dobtwin "+dobTwin);
				System.out.println("$$$ onedayprior "+oneDayPriorDobTwin+" after "+oneDayAfterDobTwin);
				colBList.add(getDateOfBirthInReverseOrder(DateManager.getStringDateWithoutSeparators(oneDayPriorDobTwin)));
				colBList.add(getDateOfBirthInReverseOrder(DateManager.getStringDateWithoutSeparators(oneDayAfterDobTwin)));
				System.out.println("....colblist size " +colBList.size());
				ceaList = do2Repo.findCeaDobListByDescListColBList(do2.getEmployee().getId(), DateManager.getSixthCpcAllowanceDate(), descList, colBList);
				 System.out.println(".......2cealist size "+ceaList.size()+"  dolblist "+colBList.get(0)+"   doblist get 1  " +colBList.get(1));
				if (ceaList==null || (ceaList!=null && ceaList.size()==0)) {
					buffer.append("No CEA/CEAHC DO2 for Twin child found - to be claimed as CEA");
					return true;
				}
			} 				
		}
		/*
		List<String> validColBList =do2Repo.findCeaDistinctColBList(do2.getEmployee().getId(), DateManager.getSixthCpcAllowanceDate(), do2.getId()); 
				
		 
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
		
		if (demiseDate != null && do2.getFromDate().after(demiseDate)){
			for (int i = 0; i < children.size(); i++) {
				birthDate = null;
				try {
					birthDate = DateManager.getDate(children.get(i).substring(0,4)+"-"+children.get(i).substring(4,6)+"-"+children.get(i).substring(6));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (demisedChildDob != null && birthDate.equals(demisedChildDob))
					children.remove(i);
			}			
		}*/
		
		if (children.size() > 3) {
			return true;
		}
		ymob = getDateOfBirthInColumnBInReverseOrder(do2).substring(0,6);
		Date adoptedDob = null;
		for (int i = 0; i < children.size(); i++) {
			prevymob = children.get(i).substring(0,6);
			if (!prevymob.equals(ymob)){
				try {
					// Date of birth within six months from dob of first child not expected
					birthDate = DateManager.getDate(children.get(i).substring(0,4)+"-"+children.get(i).substring(4,6)+"-"+children.get(i).substring(6));
					if (birthDate != null &&  DateManager.getNoOfMonths(currentDob,birthDate) < 6){
						List<FamilyDetail> fdList1 = fdRepo.findByEmpRelationListAndAdoptedTrue(do2.getEmployee().getId(), relList);
						
						if (fdList1 != null && fdList1.size() > 0){
							adoptedDob = fdList1.get(0).getDateOfBirth();
							if (adoptedDob != null && (!adoptedDob.equals(birthDate) && !adoptedDob.equals(currentDob))) {
								buffer.append("Date of birth appears to be incorrect");
								return true;
							}
						} else {
							buffer.append("Date of birth appears to be incorrect");
							return true;
						}
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
					if (birthDate != null && fpoFailedDate != null &&  !birthDate.before(fpoFailedDate)) {
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
			
			List<String> ceatwnList =do2Repo.findCeaTwinDobList(do2.getEmployee().getId(), DateManager.getSixthCpcAllowanceDate(), do2.getId(), prevymob);
			 
			if (ceatwnList.size() > 0 && (Integer.parseInt(prevymob) < Integer.parseInt(ymob))){
				// To reject second child if twins/multiple children in first instance
				ceatwnForElderDob = true;	
				return true;
			} 			
			return false;
		}		
		return false;
	}
	
	//Method to check whether valid rates are given in new CEA format
	private boolean isValidRateIn6cpcCea(Do2 do2) {
		double amount = 0.0;
		 
		if (do2.getTjra() == null || do2.getLra() == null || do2.getRate() == null ||
			do2.getDescription() == null || do2.getToDate()==null) {
			return false;
		}
	 	
		amount=getAmountFor6cpcCea(do2);
		
		if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT"))
			amount *= 2;
	 	
		if (do2.getRate()>0 && do2.getRate() <= amount){
			return true;
		} else {
			return false;
		}
	
	}
	
	private boolean isValidColumnAIn6cpcCea(Do2 do2) {
		if ((do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) &&	do2.getColumnA()==null){
			return true;
		}
		
		if  (do2.getColumnA()!=null && do2.getColumnA().length()==3 && 
			(do2.getColumnA().trim().equals("NUR") || do2.getColumnA().trim().equals("LKG") || do2.getColumnA().trim().equals("UKG"))) {
			return true;
		}
		
		return (do2.getColumnA()!=null && do2.getColumnA().length()==5 && NEW_CEA_VALID_COL_A_VALUES.contains(do2.getColumnA().trim()));			 
	}	
	
   	private boolean isValidColumnCIn6cpcCea(Do2 do2) {
		if (do2.getTjra() == null || do2.getLra()== null || 
			do2.getColumnC()==null  || (do2.getColumnC()!=null && do2.getColumnC().length()<2)) {
			buffer.append(INVALID_COLUMN_C);  
			return false;
		}
		if (do2.getColumnC() != null && do2.getColumnC().equals("INITIAL")){
			buffer.append(INVALID_COLUMN_C);  
			return false;
		}
		if (!NEW_CEA_VALID_COL_C_VALUES.contains(do2.getColumnC().trim())){
			buffer.append(INVALID_COLUMN_C);  
			return false;
		}
		int validFromDateMonth = do2.getTjra()/100;
		int validFromDateYear  = do2.getTjra()%100;
		int validToDateMonth   = do2.getLra()/100;
		int validToDateYear    = do2.getLra()%100;
	
		Calendar fromDateCal = new GregorianCalendar();
		Calendar toDateCal = new GregorianCalendar();
		
		fromDateCal.setTime(do2.getFromDate());
		toDateCal.setTime(do2.getToDate());
		
		int columnC = (Integer.parseInt(do2.getColumnC().substring(1))-1)*3; 
		
		validFromDateMonth = validFromDateMonth+columnC;
		                   
		if (validFromDateMonth>12) {
			validFromDateMonth-=12;
			validFromDateYear+=1;
		} else if ((validFromDateMonth-columnC)!=1) {                 // Academic Year beginning month is not January
			validToDateYear-=1;
		}
		
		validToDateMonth=validFromDateMonth+2;
		
		if (validToDateMonth>12) {
			validToDateMonth-=12;
			validToDateYear+=1;
		}
		
		int days = DateManager.getNoOfDaysInMonth(validToDateMonth, validToDateYear+2000);
	 
		try {
			academicYearBeg = DateManager.getDate(validFromDateYear+2000, validFromDateMonth, 1);
			academicYearEnd = DateManager.getDate(validToDateYear+2000, validToDateMonth, days);
		} catch (Exception e) {
			Utils.handleException(e);
		}
	  
		if (do2.getFromDate().equals(academicYearBeg) && do2.getToDate().equals(academicYearEnd)) {
			return true;
		} 

		if (!do2.getFromDate().before(academicYearBeg) && !do2.getFromDate().after(academicYearEnd)
				&& !do2.getToDate().before(academicYearBeg) && !do2.getToDate().after(academicYearEnd)) {
	 		return true;
		} 
		buffer.append(CEA_PERIOD_NOT_MATCH_WITH_COLUMNC);  
		return false;	
	}
	  
	private boolean isValidTjraLraValues(Do2 do2) {
			
		if (do2.getTjra()==null || do2.getLra()==null || 
		   (do2.getTjra()!=null && do2.getTjra()==0) || (do2.getLra()!=null && do2.getLra()==0))
			return false;
		
		if (do2.getTjra().toString().length()<3 || do2.getTjra().toString().length()>4  ||
			do2.getLra().toString().length()<3 || do2.getLra().toString().length()>4 ) {
			return false;
		}
				
		int tjraMonth = do2.getTjra()/100;
		int tjraYear = do2.getTjra()%100;
		int lraMonth = do2.getLra()/100;
		int lraYear = do2.getLra()%100;
		if (tjraMonth>12 || tjraYear>DateManager.getCurrentYear()) {
			return false;
		}
		int validLraMonth = (tjraMonth-1);
		int validLraYear = tjraYear+1;
		
		if (tjraMonth==1) {
			validLraMonth = 12;
			validLraYear  = tjraYear;
		}
		
		if (lraMonth!=validLraMonth || lraYear!=validLraYear) {
			return false;
		}	
		
		if (tjraYear==8)// i.e.year 2008
			year2008_09=true;
		 	
		return true;	
	}
	
	//new method added to validate the age on 16102012
	private boolean isValidAge(Do2 do2, Date reversedob) {		
		if (!reversedob.before(do2.getFromDate())) { 
			buffer.append(DOB_AFTER_FROM_DATE);
			return false;
		} 
		if ((do2.getDescription().equals("CEAHC")|| do2.getDescription().equals("CEAHCT")) && do2.getColumnA()==null) {
			if (DateManager.getNoOfMonths(reversedob, do2.getFromDate())<18) {
				buffer.append(AGE_BELOW_TWO_YEARS);
				return false;
			}
		}
			
		if (do2.getColumnA()!=null) {
			if (do2.getColumnA().startsWith("CL")) {                      //   6 months reduced to allow +/- 5 or 2 years.
				if (DateManager.getNoOfMonths(reversedob, do2.getFromDate())<36) { // 54 months reduced to 48 months on 10.11.2022
					buffer.append(AGE_BELOW_FIVE_YEARS);						   // 48 months reduced to 36 months on 10.11.2022 	
					return false;
				}
			} else {
				if (DateManager.getNoOfMonths(reversedob, do2.getFromDate())<18) {
					buffer.append(AGE_BELOW_TWO_YEARS);
					return false;
				}
			}
		}
		
		int year  = 0; 
		int month = 0; 
		int ceaEligibleMonth=0;
		int ceaEligibleYear=0;
		int ceaEligibleDay=0;
		
		year  = Integer.valueOf(do2.getColumnB().substring(4, 8));
		month = Integer.valueOf(do2.getColumnB().substring(2, 4));
	 	
		Date ceaEligibleDate = new Date();
	 	
		int lraMonth = do2.getLra() == null ? 0 : do2.getLra() == 0 ? 0 : do2.getLra()/100;
 		if (lraMonth==0) {
			lraMonth=3;
		}
		ceaEligibleMonth = lraMonth;		
		
		if (!(month > lraMonth)) {
			if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) {
				ceaEligibleYear = year+22;
			} else {
				ceaEligibleYear = year+20;
			}
			ceaEligibleDay = DateManager.getNoOfDaysInMonth(ceaEligibleMonth,ceaEligibleYear);
		} else {
		 	if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) {
				ceaEligibleYear = year+22+1;
			} else {
				ceaEligibleYear = year+20+1;
			}
			ceaEligibleDay = DateManager.getNoOfDaysInMonth(ceaEligibleMonth,ceaEligibleYear);
		}
		
		try {
			ceaEligibleDate = DateManager.getDate(ceaEligibleYear, ceaEligibleMonth, ceaEligibleDay);
		} catch (Exception e) {
			 Utils.handleException(e);
		}
	
		if (do2.getFromDate().after(ceaEligibleDate)) {
	 		buffer.append(REACHED_MAXIMUM_AGE);
			return false;
		} else return true;
		
	}
	
	private boolean isValidColumnBIn6cpcCea(Do2 do2) {
		int year  = 0; 
		int month = 0;
		int day   = 0; 
		String strMonth = "";
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
		if (month > 0 && month < 10){
			strMonth = "0"+ month;
		} else {
			strMonth = month+"";
		}
		Date birthDate = new Date();
		
		try {
			birthDate = DateManager.getDate(year + "-" + strMonth + "-" + day);
		} catch (Exception e) {
			 Utils.handleException(e);
		}
		reversedob = birthDate;
		return true;
	}
	
	private double getAmountFor6cpcCea(Do2 do2) {
		List<Allowance> allowances=null;
		Date fromDate = null;
		Date toDate = null;
		Date twentiethDob = null;
		String fromMonth = "";
		String toMonth = "";
		Double totalAdmissibleAmount = 0.0;
		int validFromDateMonth = do2.getTjra()/100;
		int validFromDateYear = do2.getTjra()%100;
		int validToDateMonth = do2.getLra()/100;
		int validToDateYear = do2.getLra()%100;			
		int validFromDateYearWithCentury=validFromDateYear+2000;
		int validToDateYearWithCentury=validToDateYear+2000;
		int days = DateManager.getNoOfDaysInMonth(validToDateMonth, validToDateYearWithCentury);
		int year = 0;
		
		if (year2008_09){
			validFromDateMonth = 9;
		}
		if (validFromDateMonth > 0 && validFromDateMonth < 10){
			fromMonth = "0"+ validFromDateMonth;
		} else {
			fromMonth = validFromDateMonth+"";
		}
		if (validToDateMonth > 0 && validToDateMonth < 10){
			toMonth = "0"+ validToDateMonth;
		} else {
			toMonth = validToDateMonth+"";
		}
		try {
			fromDate = DateManager.getDate(validFromDateYearWithCentury + "-" + fromMonth + "-01");
			toDate = DateManager.getDate(validToDateYearWithCentury + "-" +  toMonth + "-" + days);
		} catch (Exception e) {
			Utils.handleException(e);
		}
		try {
			
			if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) {
				 year = Integer.parseInt(do2.getColumnB().trim().substring(4,8))+22;
			} else {
				 year = Integer.parseInt(do2.getColumnB().trim().substring(4,8))+20;
			}
	 		
			if (do2.getColumnB().trim().substring(0,2).equals("01")){
				twentiethDob = DateManager.getPreviousDate(DateManager.getDate(year+ "-" + do2.getColumnB().trim().substring(2,4) + "-01")); 
			} else {				
 				days = DateManager.getNoOfDaysInMonth(Integer.parseInt(do2.getColumnB().trim().substring(2,4)), year);
				twentiethDob = DateManager.getDate(year + "-" + do2.getColumnB().trim().substring(2,4) + "-" + days);
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}
		if (toDate.after(twentiethDob)){
			toDate=twentiethDob;
		}
		
		allowances = Utils.getApplicableAllowanceRates("CEA", fromDate, toDate);
	//System.out.println(".................allowances size 6cpc " +allowances.size());
		if (allowances !=null && allowances.size() >0){
			for (Allowance allow : allowances) {
				totalAdmissibleAmount +=(allow.getRateAmount()
						* DateManager.getNoOfMonths(allow.getFromDate(), allow.getToDate())); 	
		//		System.out.println("++++++++++++++++++++++++++++++  allow rate "+allow.getRateAmount()+"   fromdt "+allow.getFromDate()+"  todt "+allow.getToDate());
			}	
		}
	//	System.out.println("------------ totaladmissibleamt "+totalAdmissibleAmount);
	 	return totalAdmissibleAmount;
	}
      
    private String getDateOfBirthInColumnBInReverseOrder(Do2 do2) {
    	String dateStr = do2.getColumnB();
    	if (dateStr==null || (dateStr!=null && dateStr.length()<8)) {
    		return " ";
    	}
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

    private boolean isAcademicYearUniqueForTheChild(Do2 do2) {   	
    	int currentDo2TjraYear = do2.getTjra()%100;
		int currentDo2LraYear  = do2.getLra()%100;
		int currentDo2TjraMonth= do2.getTjra()/100;
		int currentDo2LraMonth = do2.getLra()/100;
		int reverseTjra        = (currentDo2TjraYear * 100) + currentDo2TjraMonth;
		int reverseLra         = (currentDo2LraYear * 100) + currentDo2LraMonth;
		String dobInReverse    = getDateOfBirthInColumnBInReverseOrder(do2);
		String newDo2Ymob      = dobInReverse.substring(0,6);
		
		List<Do2> do2List = do2Repo.findCeaUniqueAy(do2.getEmployee().getId(), DateManager.getSixthCpcAllowanceDate(), do2.getFromDate(), do2.getId(), do2.getDescription(), dobInReverse.substring(0,6), reverseTjra, reverseLra);
		
		if (do2List.size()>0) {
		 	
			for (Do2 oldDo2 : do2List) {
				int oldDo2TjraYear = oldDo2.getTjra()%100;
				int oldDo2LraYear  = oldDo2.getLra()%100;
	 			int oldDo2TjraMonth= oldDo2.getTjra()/100;
				int oldDo2LraMonth = oldDo2.getLra()/100;
				
				// modified on 04102012 added mob, dob along with yob
			    String oldDo2Ymob = oldDo2.getColumnB().substring(0,6);		
			    if (currentDo2TjraYear==oldDo2TjraYear && currentDo2LraYear==oldDo2LraYear &&
					currentDo2TjraMonth==oldDo2TjraMonth && currentDo2LraMonth==oldDo2LraMonth) {
					if (do2.getColumnA()!=null && oldDo2.getColumnA()!=null && !do2.getColumnA().equals(oldDo2.getColumnA())) {
						if (newDo2Ymob==oldDo2Ymob) {
							buffer.append(SAME_ACADEMIC_YEAR_DIFFERENT_CLASS);
							return false;
						}
					}							
					if (do2.getColumnC()!=null && oldDo2.getColumnC()!=null && do2.getColumnC().equals(oldDo2.getColumnC()) 
							&& do2.getFromDate().equals(oldDo2.getFromDate()) && do2.getToDate().equals(oldDo2.getToDate())) {
						if (newDo2Ymob==oldDo2Ymob) {
							buffer.append(DUPLICATE_CLAIM);
							return false;
						}
					}			
			    } else if ((oldDo2.getColumnA().startsWith("CL-") && 
			    		   (oldDo2.getColumnA().equals(do2.getColumnA()) || oldDo2.getColumnA().equals(getClassMap().get(do2.getColumnA())))) ||
			    		   (oldDo2.getColumnA().endsWith("KG") && (do2.getColumnA().endsWith("KG") || do2.getColumnA().equals("CL-01"))) ||
			    		   (oldDo2.getColumnA().equals("NUR") && do2.getColumnA().endsWith("KG"))) {
			    	;
				} else {
					buffer.append(OVERLAPPING_ACADEMIC_YEAR);
					return false;
				}
			}
		}
      	return true;
   }
    
    private boolean isTheConditionOnlyTwoClassesBelowClassOneSatisfied(Do2 do2) {
    	List<String> columnAList = do2Repo.findCea2ClassBelowList(do2.getEmployee().getId(),DateManager.getSixthCpcAllowanceDate(),do2.getId(), do2.getDescription(),getDateOfBirthInColumnBInReverseOrder(do2).substring(0,6), do2.getColumnA());
    	if (!do2.getFromDate().before(DateManager.getNepImplementationDateForThreeClassesBelowClassOne())) {
    		return (columnAList.size()<3);
    	}    	
	 	return (columnAList.size()<2);
    }
    
  //modified do2list 04102012 added month of birth, date of birth in the parameters.
    private boolean anyClaimAlreadyAdjustedForSamePeriod(Do2 do2) {
    	List<Do2> do2List = do2Repo.findCeaSamePeriodList(do2.getEmployee().getId(), DateManager.getSixthCpcAllowanceDate(), do2.getId(), do2.getDescription(), getDateOfBirthInColumnBInReverseOrder(do2).substring(0,6), do2.getTjra(), do2.getLra(), do2.getColumnC());
    	 
     
    	if (do2List.size()==0) {
     		return false;
    	}
    	                         
    	for (Do2 oldDo2 : do2List) {
    		if (do2.getFromDate().after(oldDo2.getToDate())) {
     			return false;
    		}
    		
    		if (do2.getToDate().before(oldDo2.getFromDate())) {
     			return false;
    		}
       	}
	
    	return true;
    }
    
    private boolean isArrearsAlreadyAdjustedForDo2(Do2 do2) {
    	List<Arrears> arrearsList =arrRepo.findByDo2Id(do2.getId());

    	double ceaPaidAmount = 0;
		for (Arrears arr : arrearsList) {
			ceaPaidAmount += arr.getAmount();
		}

		return (arrearsList.size()>0 && ceaPaidAmount>0); 
  //  	System.out.println(".................. arrears adusted?  arrearslist " +arrearsList.size());
    //	return (arrearsList.size()>0); 
    	
    }
    
    private boolean isDuplicate(Do2 do2) {		
		List<Do2> do2List =do2Repo.findCeaDuplicateList(do2.getEmployee().getId(), DateManager.getSixthCpcAllowanceDate(), do2.getId(), do2.getDescription(), getDateOfBirthInColumnBInReverseOrder(do2).substring(0,6), do2.getFromDate(),do2.getToDate());
		
		
		if (do2.getDescription().equals("CEATWN")) {
			return do2List.size()>0;
		} else {
			return do2List.size()>1;
		}
     }
    
    public boolean isOnDeputationOtherthanBhutanUNMISN(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		List<AuditCage>auditCageList = null;
		if (do2.getFromDate().after(DateManager.getSeventhCpcAllowanceDate())) {
			auditCageList =acRepo.findByToDateBetweenOrderByFromDateDescToDateDesc(do2.getEmployee().getId(),do2.getToDate());
					
				 
		} else {
			auditCageList =acRepo.findByFromDateBetweenOrderByFromDateDescToDateDesc(do2.getEmployee().getId(),do2.getFromDate()); 
			 
		}
		
		if (auditCageList.size() > 0) {
			if (auditCageList.get(0).getDeputationFlag() == true) {
				if (do2.getFromDate().after(DateManager.getCgguardDeputationDate())) { 
					 if (auditCageList.get(0).getDeputationStatus().equals("U")  
						|| auditCageList.get(0).getDeputationStatus().equals("T") 
						|| auditCageList.get(0).getDeputationStatus().equals("K") 
						|| auditCageList.get(0).getDeputationStatus().equals("O")) {
						 return false;
					 } else {
						 return true;
					 }							 
				} else if (auditCageList.get(0).getDeputationStatus().equals("U") 
							|| auditCageList.get(0).getDeputationStatus().equals("T") 
							|| auditCageList.get(0).getDeputationStatus().equals("K")) {
						 return false;
				} else {
					return true;
				}				 
			} else {
			  return false;  
			}
		}
		return false;
    }
    
    public boolean isCeaForYear20172018Valid(Do2 do2) {
    	boolean validityFor20172018 = true;
    	
     	if (!isValidValuesForFinancialYear20172018(do2)) {
     		buffer.append(INVALID_VALUES_FOR_FINANCIAL_YEAR_2017_2018); 
     		validityFor20172018 = false;
     		return validityFor20172018;
     	}     	
		if (!isValidColumnAIn6cpcCea(do2)) {
			buffer.append(INVALID_COLUMN_A);  
			validityFor20172018 = false;
			return validityFor20172018;
		}		 
		if (!isValidRateIn20172018Cea(do2)) {
			buffer.append(INVALID_AMOUNT_NEW_CEA);
			validityFor20172018 = false;
			return validityFor20172018;
		}	
		if (!isValidColumnBIn6cpcCea(do2)){
 			buffer.append(INVALID_COLUMN_B);  
 			validityFor20172018 = false;
 			return validityFor20172018;
		}
		if (!isValidAge(do2, reversedob)) {
			validityFor20172018 = false;
	 		return validityFor20172018;
	 	}		
		if (!isAcademicYearUniqueForTheChildIn20172018(do2)) {
			validityFor20172018 = false;
			return validityFor20172018;
		}
		if (!isFinancialYearUniqueForTheChild(do2)) {
			validityFor20172018 = false;
			return validityFor20172018;
		}		
		if (isOverlapping6cpcCea(do2)) {			
			if (overlappingDo2!=null) {
				buffer.append(OVERLAP);
      			buffer.append(" Already adjusted Do2 No. "+overlappingDo2.getDo2Type()+"/"+overlappingDo2.getDo2No()
      				+"/"+overlappingDo2.getDo2ItemNo()+" Do2 date  :  "+overlappingDo2.getDo2Date()+" "+overlappingDo2.getDescription()
      				+ " " +overlappingDo2.getFromDate()+" to "+overlappingDo2.getToDate());
			}
			validityFor20172018 = false;
			if (!concurrentValidity) {
				buffer.append(CONCURRENTLY_INADMISSIBLE);
			}
			if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT") || do2.getDescription().equals("CEATWN")) {
				if (concurrentCeatwnCeahc) {
					buffer.append(CONCURRENT_CEATWN_CEAHC);
				}
			}
			if (do2.getDescription().equals("CEA") || do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) {
				if (toClaimAsCeatwn) {
					buffer.append(TO_CLAIM_AS_CEATWN);
				}
			}
		}   
		
		if (whetherMoreThanTwoChildren(do2)) {			
			if (ceatwnForElderDob) {
				buffer.append(CEATWN_FOR_ELDER_DOB);
			} else {
				buffer.append(CHILDREN_LIMIT_EXCEED_NEW_FORMAT);
			}
			validityFor20172018 = false;
			return validityFor20172018;
		}	
	 
 	 	if (!do2.getDescription().startsWith("CEAH") && !do2.getColumnA().startsWith("CL")) {
 	 		if (!isTheConditionOnlyTwoClassesBelowClassOneSatisfied(do2)) {
 	 			buffer.append(ONLY_TWO_CLASSES_BELOW_CLASS_ONE_ADMISSIBLE);
				validityFor20172018 = false;
 	 		}
 		} 			
    	return validityFor20172018;
    }

    public boolean is7cpcCeaValid(Do2 do2) {
    	boolean new7cpcValidity = true;	   	 
	 	   	   	
    	if (!isValidYear(do2)) {
    		if (claimPeriodNot12M) {
    			buffer.append(CLAIM_PERIOD_NOT_12M);
    		} else {
    		//	buffer.append(INVALID_DATES_FOR_FINANCIAL_YEAR_CLAIMS); 
    			buffer.append(INVALID_DATES_FOR_FULL_YEAR_CLAIMS);    			
    		}    			
     		new7cpcValidity = false;
			return new7cpcValidity;
    	}    	
		if (!isValidColumnAIn6cpcCea(do2)) {
			buffer.append(INVALID_COLUMN_A);  
			new7cpcValidity = false;
			return new7cpcValidity;
		}		 
		if (!isValidRateIn7cpcCea(do2)) {
			buffer.append(INVALID_AMOUNT_NEW_CEA);
			new7cpcValidity = false;
			return new7cpcValidity;
		}	
		if (!isValidColumnBIn6cpcCea(do2)){
 			buffer.append(INVALID_COLUMN_B);  
 			new7cpcValidity = false;
 			return new7cpcValidity;
		}
		if (!isValidAge(do2, reversedob)) {
			new7cpcValidity = false;
	 		return new7cpcValidity;
	 	}		
		if (!isFinancialYearUniqueForTheChild(do2)) {
			new7cpcValidity = false;
			return new7cpcValidity;
		}		
		if (isOverlapping6cpcCea(do2)) {			
			if (overlappingDo2!=null) {
				buffer.append(OVERLAP_7CPC);
      			buffer.append(" Already adjusted Do2 No. "+overlappingDo2.getDo2Type()+"/"+overlappingDo2.getDo2No()
      				+"/"+overlappingDo2.getDo2ItemNo()+" Do2 date  :  "+overlappingDo2.getDo2Date()+" "+overlappingDo2.getDescription()
      				+ " " +overlappingDo2.getFromDate()+" to "+overlappingDo2.getToDate());
			}
			new7cpcValidity = false;
			if (!concurrentValidity) {
				buffer.append(CONCURRENTLY_INADMISSIBLE);
			}
			if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT") || do2.getDescription().equals("CEATWN")) {
				if (concurrentCeatwnCeahc) {
					buffer.append(CONCURRENT_CEATWN_CEAHC);
				}
			}
			if (do2.getDescription().equals("CEA") || do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) {
				if (toClaimAsCeatwn) {
					buffer.append(TO_CLAIM_AS_CEATWN);
				}
			}
		}		
		if (whetherMoreThanTwoChildren(do2)) {			
			if (ceatwnForElderDob) {
				buffer.append(CEATWN_FOR_ELDER_DOB);
			} else {
				buffer.append(CHILDREN_LIMIT_EXCEED_NEW_FORMAT);
			}
			new7cpcValidity = false;
			return new7cpcValidity;
		}	
	 
 	 	if (!do2.getDescription().startsWith("CEAH") && !do2.getColumnA().startsWith("CL")) {
 	 		if (!isTheConditionOnlyTwoClassesBelowClassOneSatisfied(do2)) {
 	 			if (!do2.getFromDate().before(DateManager.getNepImplementationDateForThreeClassesBelowClassOne()) ) {
 	 				buffer.append(ONLY_THREE_CLASSES_BELOW_CLASS_ONE_ADMISSIBLE); 	 				
 	 			} else {
 	 				buffer.append(ONLY_TWO_CLASSES_BELOW_CLASS_ONE_ADMISSIBLE); 	 							
 	 			}
 	 				new7cpcValidity= false;
 	 		}
 		}
 	 	
 	 	if (new7cpcValidity && !do2.getDescription().startsWith("CEAH") && !do2.getDescription().startsWith("CEAT")) {
 	 		if (!isValidClassStudied(do2)) {
 	 			buffer.append(LOWER_CLASS_CLAIM);
				new7cpcValidity= false;
 	 		}
 	 	}
 	 	
		return new7cpcValidity;
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
//		 	System.out.println("xxxxxxxxxxxx  claimper12m " +claimPeriodNot12M+" validity "+validity+"  paocode "+Utils.getPao().getPaoCode()+"  do2frdt "+do2.getFromDate()+"  enroldt "+do2.getEmployee().getEnrolmentDate());
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
	
	private boolean isFinancialYearUniqueForTheChild(Do2 do2) { 
 		String dobInReverse    = getDateOfBirthInColumnBInReverseOrder(do2);
		String newDo2Ymob      = dobInReverse.substring(0,6);
		System.out.println("dob "+dobInReverse+"--"+newDo2Ymob);
		System.out.println("do2id-"+do2.getEmployee().getId()+"--"+do2.getFromDate()+"--"+do2.getToDate()+"--"+do2.getDescription());
		List<Do2> do2List =null;
		do2List=do2Repo.findCeaUniqueDobList(do2.getEmployee().getId(),do2.getFromDate(),do2.getToDate(),do2.getId() , do2.getDescription(), dobInReverse.substring(0,6));
		

		if (do2List.size()>0) {		 	
			for (Do2 oldDo2 : do2List) {
			    String oldDo2Ymob = oldDo2.getColumnB().substring(0,6);		
				if (do2.getColumnA()!=null && oldDo2.getColumnA()!=null && !do2.getColumnA().equals(oldDo2.getColumnA())) {
					if (newDo2Ymob==oldDo2Ymob) {
						buffer.append(SAME_ACADEMIC_YEAR_DIFFERENT_CLASS);
						return false;
					}
				}							
				if (newDo2Ymob==oldDo2Ymob && do2.getColumnA().trim().equals(oldDo2.getColumnA().trim())) {
					buffer.append(DUPLICATE_CLAIM);
					return false;
				}
			}
		}
      	return true;
	}
	
	private boolean isValidValuesForFinancialYear20172018(Do2 do2) {
		boolean validity = true;
		Calendar fromDateCal = new GregorianCalendar(), toDateCal = new GregorianCalendar();
		fromDateCal.setTime(do2.getFromDate());
		int fromDateYear = fromDateCal.get(Calendar.YEAR);
	 	toDateCal.setTime(do2.getToDate());
		int toDateYear = toDateCal.get(Calendar.YEAR);
 		int tjraMonth = do2.getTjra() == null ? 0 : do2.getTjra() == 0 ? 0 : do2.getTjra()/100;
		int tjraYear = do2.getTjra() == null ? 0 : do2.getTjra() == 0 ? 0 : do2.getTjra()%100;
		int lraMonth = do2.getLra() == null ? 0 : do2.getLra() == 0 ? 0 : do2.getLra()/100;
		int lraYear = do2.getLra() == null ? 0 : do2.getLra() == 0 ? 0 : do2.getLra()%100;				 	
		
		if (fromDateYear<2017 || fromDateYear>2018 || (tjraYear!=0 && tjraYear!=17)) {
			validity = false; 
		}
		
		if (toDateYear<2017 || toDateYear>2018 || (lraYear!=0 && (lraYear<17 || lraYear>18))) {
			validity = false;
		}
		
		if (lraYear!=0 && lraYear==17 && tjraMonth!=0 && tjraMonth>1) {
			validity = false;			
		}
		
		if (lraYear!=0 && tjraYear!=0 && (lraYear<tjraYear || (lraYear==tjraYear && tjraMonth>1))) {
			validity = false;
		}		 
		
		if (tjraMonth!=0 && lraMonth!=0 && lraMonth>=tjraMonth && lraYear!=tjraYear) {
			validity = false;
		}
		
	 	return validity;		
	}
	
	private double get7CPCAmount(Do2 do2) {
		totalRate = 0;
		double maximumAmount = 0;
	 	Calendar fromCal = new GregorianCalendar(), toCal = new GregorianCalendar();
		fromCal.setTime(do2.getFromDate());
		toCal.setTime(do2.getToDate());	
	 	
		maximumAmount=calculateMaximumAmountFor7cpcCea(do2); 
		
		if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")){
			maximumAmount = maximumAmount * 2;
		}
	 
		return maximumAmount; // Since fixed amount is to be reimbursed in 7th CPC 
	}
	
	private double calculateMaximumAmountFor7cpcCea(Do2 do2) {
		List<Allowance> allowances=null;
		Date fromDate = null;
		Date toDate = null;
		Date twentiethDob = null;
		String fromMonth = "";
		String toMonth = "";
		Double totalAdmissibleAmount = 0.0;
		Calendar cal = new GregorianCalendar();
		cal.setTime(do2.getFromDate());
		int validFromDateMonth = cal.get(Calendar.MONTH)+1;
		int validFromDateYear = cal.get(Calendar.YEAR);
		cal.setTime(do2.getToDate());
		int validToDateMonth = cal.get(Calendar.MONTH)+1;
		int validToDateYear = cal.get(Calendar.YEAR);			
		int validFromDateYearWithCentury=validFromDateYear;
		int validToDateYearWithCentury=validToDateYear;
		int days = DateManager.getNoOfDaysInMonth(validToDateMonth, validToDateYearWithCentury);
		int year = 0;
 		 	 
		if (validFromDateMonth > 0 && validFromDateMonth < 10){
			fromMonth = "0"+ validFromDateMonth;
		} else {
			fromMonth = validFromDateMonth+"";
		}
		if (validToDateMonth > 0 && validToDateMonth < 10){
			toMonth = "0"+ validToDateMonth;
		} else {
			toMonth = validToDateMonth+"";
		}
		try {
			fromDate = DateManager.getDate(validFromDateYearWithCentury + "-" + fromMonth + "-01");
			toDate = DateManager.getDate(validToDateYearWithCentury + "-" +  toMonth + "-" + days);
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
		try {			
			if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) {
				 year = Integer.parseInt(do2.getColumnB().trim().substring(4,8))+22;
			} else {
				 year = Integer.parseInt(do2.getColumnB().trim().substring(4,8))+20;
			}
	/*		if (do2.getColumnB().trim().substring(0,2).equals("01")){
				twentiethDob = DateManager.getPreviousDate(DateManager.getDate(year+ "-" + do2.getColumnB().trim().substring(2,4) + "-01")); 
			} else {				
 				days = DateManager.getNoOfDaysInMonth(Integer.parseInt(do2.getColumnB().trim().substring(2,4)), year);
				twentiethDob = DateManager.getDate(year + "-" + do2.getColumnB().trim().substring(2,4) + "-" + days);
			}
	*/
			days = DateManager.getNoOfDaysInMonth(Integer.parseInt(do2.getColumnB().trim().substring(2,4)), year);
			twentiethDob = DateManager.getDate(year + "-" + do2.getColumnB().trim().substring(2,4) + "-" + days);	
		} catch (Exception e) {
			buffer.append(INVALID_COLUMN_B);
			totalAdmissibleAmount=0.0;
			return 0;
		}		
		
		if (fromDate.before(twentiethDob) && toDate.after(twentiethDob) && DateManager.getNoOfMonths(fromDate, toDate)!=12) {
			toDate=twentiethDob;
		}
		
	/*	if (toDate.after(twentiethDob)){
			toDate=twentiethDob;
		}*/
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
	 //			System.out.println("cccccccccccccccccc ano "+do2.getArmyNo()+"    fromdt " +fromDate+"  enorlmntdt "+enrollCalendar.getTime());
			}			
		}	 	 
	// 	System.out.println("............cea fromdt "+fromDate+"  todt "+toDate);
		
		allowances = Utils.getApplicableAllowanceRates("CEA", fromDate, toDate);
	
		if (allowances !=null && allowances.size() >0){
			for (Allowance allow : allowances) {
				totalAdmissibleAmount +=(allow.getRateAmount() * DateManager.getNoOfMonths(allow.getFromDate(), allow.getToDate())); 					
			}	
		}
	// 	System.out.println("............amt "+totalAdmissibleAmount);
 		return totalAdmissibleAmount;
	}
	
	private double getAmountFor20172018(Do2 do2) {
	 	totalRate = 0;
		double maximumAmount = 0;
		double do2Amount = 0;
		double diffInClaimedAdjustedAmount = 0; 
		Calendar fromCal = new GregorianCalendar(), toCal = new GregorianCalendar();
		fromCal.setTime(do2.getFromDate());
		toCal.setTime(do2.getToDate());	
		boolean secondCeatwnInProcessForAdj=false;
	 	
		maximumAmount=calculateMaximumAmountFor20172018(do2);
	//	System.out.println(".......... maximum amount " + maximumAmount);
		if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")){
			maximumAmount = maximumAmount * 2;
		}
	  
	 	List<Do2> do2List =do2Repo.findCeaMaxAmount2017List(do2.getEmployee().getId(),do2.getId(),do2.getDescription(), getDateOfBirthInColumnBInReverseOrder(do2).substring(0,6)); 
	 		
	 	
	 	for (Do2 oldDo2 : do2List) { 	  	
	 		List<Arrears> arrearList =arrRepo.findByDo2Id(oldDo2.getId());
	 				 			
			if (arrearList!=null && arrearList.size()>0){
				for (Arrears arr : arrearList)
					totalRate+=arr.getAmount();
			}
			do2Amount+=oldDo2.getRate();
			// Below condition added to adjust triplets claims- one CEA and two CEATWN
			if (do2.getDescription().trim().equals("CEATWN") && oldDo2.getDescription().trim().equals("CEATWN")) {
 				secondCeatwnInProcessForAdj=true;
			}
	  	}
	 	if (secondCeatwnInProcessForAdj) {
	 		maximumAmount=maximumAmount*2;		
	 	}	 
 	   	diffInClaimedAdjustedAmount = (do2Amount-totalRate);
 
		double amount = maximumAmount;		 		
		if ((totalRate + diffInClaimedAdjustedAmount + do2.getRate()) > amount) {
			restrictValidity = true;
			totalRate = (amount-totalRate);
 	 	} else {
			totalRate = (do2.getRate()+diffInClaimedAdjustedAmount);
	  	}
//		System.out.println("---------------- dffinclaimadjamt " +diffInClaimedAdjustedAmount+" totrate "+totalRate +"  do2amount "+do2Amount +" maxamt "+maximumAmount+ " amt "+amount);
	  	return totalRate;
	}
	private double calculateMaximumAmountFor20172018(Do2 do2) {
		List<Allowance> allowances=null;
		Date fromDate = null;
		Date toDate = null;
		Date twentiethDob = null;
		String fromMonth = "";
		String toMonth = "";
		Double totalAdmissibleAmount = 0.0;		
	 
  		if (tjraMonth==0 && tjraYear==0 && lraMonth==0 && lraYear==0) { 	 	 	  	
	 	  	List<Do2> do2List = do2Repo.findCeaMaxAmount2017List(do2.getEmployee().getId(),do2.getId() ,do2.getDescription(),getDateOfBirthInColumnBInReverseOrder(do2).substring(0,6));
	 	  		  	
	 	  	if (do2List!=null && do2List.size()>0) {
	 	  		tjraMonth = do2List.get(0).getTjra()/100;
	 			tjraYear = do2List.get(0).getTjra()%100;
	 			lraMonth = do2List.get(0).getLra()/100;
	 			lraYear = do2List.get(0).getLra()%100;		 		
	 	  	}
	  	}		 
  		
   		int validToDateMonth = lraMonth;;
 		int validFromDateYearWithCentury=tjraYear+2000;
		int validToDateYearWithCentury=lraYear+2000;
		int year = 0;	
		
		if (tjraMonth>0 && tjraYear>0 && lraMonth>0 && lraYear>0) {
			if (tjraMonth < 10) {
				fromMonth = "0" + tjraMonth;
			} else {
				fromMonth = tjraMonth+"";
			}
			if (lraMonth < 10) {
				toMonth = "0" + lraMonth;
			} else {
				toMonth = lraMonth+"";
			}
			validFromDateYearWithCentury=tjraYear+2000;
			validToDateYearWithCentury=lraYear+2000; 
		} else {
			fromMonth = "04";
			toMonth = "03"; 
			validToDateMonth=3;
			validFromDateYearWithCentury=2017;
			validToDateYearWithCentury=2018;		
		}
		
		int days = DateManager.getNoOfDaysInMonth(validToDateMonth, validToDateYearWithCentury);
 		try {
			fromDate = DateManager.getDate(validFromDateYearWithCentury + "-" + fromMonth + "-01");
			toDate = DateManager.getDate(validToDateYearWithCentury + "-" +  toMonth + "-" + days);
		} catch (Exception e) {
			Utils.handleException(e);
		}
	 	try {			
			if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) {
				 year = Integer.parseInt(do2.getColumnB().trim().substring(4,8))+22;
			} else {
				 year = Integer.parseInt(do2.getColumnB().trim().substring(4,8))+20;
			}
			
			if (do2.getColumnB().trim().substring(0,2).equals("01")){
				twentiethDob = DateManager.getPreviousDate(DateManager.getDate(year+ "-" + do2.getColumnB().trim().substring(2,4) + "-01")); 
			} else {				
 				days = DateManager.getNoOfDaysInMonth(Integer.parseInt(do2.getColumnB().trim().substring(2,4)), year);
				twentiethDob = DateManager.getDate(year + "-" + do2.getColumnB().trim().substring(2,4) + "-" + days);
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}
		if (toDate.after(twentiethDob)){
			toDate=twentiethDob;
		}
	 	allowances = Utils.getApplicableAllowanceRates("CEA", fromDate, toDate);
	
		if (allowances !=null && allowances.size() >0){
			for (Allowance allow : allowances) {
				totalAdmissibleAmount +=(allow.getRateAmount() * DateManager.getNoOfMonths(allow.getFromDate(), allow.getToDate())); 					
			}	
		}
 	 	return totalAdmissibleAmount;
	}
	
	public boolean isValidRateIn20172018Cea(Do2 do2) {
	 		
	 	double amount = 27000;
		 
	 	if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT"))
			amount *= 2;	
		if (do2.getRate()>0 && do2.getRate() <= amount){
			return true;
		} else {
			return false;
		} 
	}
	
	private boolean isValidTjraLraValuesFor20172018Cea(Do2 do2) {
		
		tjraMonth = do2.getTjra() == null ? 0 : do2.getTjra() == 0 ? 0 : do2.getTjra()/100;
		tjraYear = do2.getTjra() == null ? 0 : do2.getTjra() == 0 ? 0 : do2.getTjra()%100;
		lraMonth = do2.getLra() == null ? 0 : do2.getLra() == 0 ? 0 : do2.getLra()/100;
		lraYear = do2.getLra() == null ? 0 : do2.getLra() == 0 ? 0 : do2.getLra()%100;		
		
		if (tjraMonth>0 && tjraMonth<13 && tjraYear>0 && tjraYear==2017 && lraMonth>0 && lraMonth<13 && lraYear>0 && (lraYear==2017 || lraYear==2018)) {
			return true;
		} else {
			return false;
		}
	 }

    private boolean isAcademicYearUniqueForTheChildIn20172018(Do2 do2) {   	
    		
    	int currentDo2TjraYear = do2.getTjra() == null ? 0 : do2.getTjra() == 0 ? 0 : do2.getTjra()%100;
		int currentDo2LraYear  = do2.getLra() == null ? 0 : do2.getLra() == 0 ? 0 : do2.getLra()%100;	
		int currentDo2TjraMonth= do2.getTjra() == null ? 0 : do2.getTjra() == 0 ? 0 : do2.getTjra()/100;
		int currentDo2LraMonth = do2.getLra() == null ? 0 : do2.getLra() == 0 ? 0 : do2.getLra()/100;
		
		if (currentDo2TjraYear==0 || currentDo2LraYear==0 || currentDo2TjraMonth==0 || currentDo2LraMonth==0) {
			do2.setRemarks("AcademicYearUniquenessNotVerifiedDueToNonAvailabilityOfTjraLraValues");
			buffer.append(INVALID_VALUES_FOR_FINANCIAL_YEAR_2017_2018);
			return false;
		}
				
		int reverseTjra        = (currentDo2TjraYear * 100) + currentDo2TjraMonth;
		int reverseLra         = (currentDo2LraYear * 100) + currentDo2LraMonth;
		String dobInReverse    = getDateOfBirthInColumnBInReverseOrder(do2);
		String newDo2Ymob      = dobInReverse.substring(0,6);
		
		List<Do2> do2List =do2Repo.findCeaUniqueAy(do2.getEmployee().getId(),DateManager.getSixthCpcAllowanceDate(),do2.getFromDate(),do2.getId(),do2.getDescription(), dobInReverse.substring(0,6),  reverseTjra, reverseLra); 
			
		if (do2List.size()>0) {
		 	
			for (Do2 oldDo2 : do2List) {
				int oldDo2TjraYear = oldDo2.getTjra() == null ? 0 : oldDo2.getTjra() == 0 ? 0 : oldDo2.getTjra()%100; 
				int oldDo2LraYear  = oldDo2.getLra() == null ? 0 : oldDo2.getLra() == 0 ? 0 :oldDo2.getLra()%100;
	 			int oldDo2TjraMonth= oldDo2.getTjra() == null ? 0 : oldDo2.getTjra() == 0 ? 0 :oldDo2.getTjra()/100;
				int oldDo2LraMonth = oldDo2.getLra() == null ? 0 : oldDo2.getLra() == 0 ? 0 :oldDo2.getLra()/100;
				
				// modified on 04102012 added mob, dob along with yob
			    String oldDo2Ymob = oldDo2.getColumnB().substring(0,6);		
			    if (currentDo2TjraYear==oldDo2TjraYear && currentDo2LraYear==oldDo2LraYear &&
					currentDo2TjraMonth==oldDo2TjraMonth && currentDo2LraMonth==oldDo2LraMonth) {
					if (do2.getColumnA()!=null && oldDo2.getColumnA()!=null && !do2.getColumnA().equals(oldDo2.getColumnA())) {
						if (newDo2Ymob==oldDo2Ymob) {
							buffer.append(SAME_ACADEMIC_YEAR_DIFFERENT_CLASS);
							return false;
						}
					}							
					if (do2.getColumnC()!=null && oldDo2.getColumnC()!=null && do2.getColumnC().equals(oldDo2.getColumnC()) 
							&& do2.getFromDate().equals(oldDo2.getFromDate()) && do2.getToDate().equals(oldDo2.getToDate())) {
						if (newDo2Ymob==oldDo2Ymob) {
							buffer.append(DUPLICATE_CLAIM);
							return false;
						}
					}			
			    } else if ((oldDo2.getColumnA().startsWith("CL-") && 
			    		   (oldDo2.getColumnA().equals(do2.getColumnA()) || oldDo2.getColumnA().equals(getClassMap().get(do2.getColumnA())))) ||
			    		   (oldDo2.getColumnA().endsWith("KG") && (do2.getColumnA().endsWith("KG") || do2.getColumnA().equals("CL-01"))) ||
			    		   (oldDo2.getColumnA().equals("NUR") && do2.getColumnA().endsWith("KG"))) {
			    	;
				} else {
					buffer.append(OVERLAPPING_ACADEMIC_YEAR);
					return false;
				}
			}
		}
      	return true;
   }
	
    public boolean isValidRateIn7cpcCea(Do2 do2) {
	 	  
		double amount=calculateMaximumAmountFor7cpcCea(do2);
		
		if (do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT"))
			amount *= 2;	
		if (do2.getRate()>0 && do2.getRate() <= (amount+0.5)) {
			return true;
		} else {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) || (employee.getRegularTaFlag()!=null && employee.getTaAppointmentCategory()!=null && 
					employee.getRegularTaFlag().equals("T") && employee.getTaAppointmentCategory().equals("R"))) {  // braces modified on 13.05.2025
				if (do2.getFromDate().before(do2.getEmployee().getEnrolmentDate())) {
					if (amount>0 && do2.getRate()>0 && amount<do2.getRate()) {
						return true;
					}
 				}
			} 
			if (do2.getFromDate().before(DateManager.getSeventhCpcDaFiftyPercentDate())) {
				if (do2.getToDate().before(DateManager.getSeventhCpcDaFiftyPercentDate())) {
					if (amount>0 && do2.getRate()>0 && amount<=27000 && do2.getRate()<=27000) {
						return true;
					}
				} else {
					if (amount>0 && do2.getRate()>0 && amount<=28688 && do2.getRate()<=28688) {
						return true;
					}
				} 							
			} else {
			//	if (amount>0 && do2.getRate()>0 && amount<=28688 && do2.getRate()<=28688) {
				if (amount>0 && do2.getRate()>0 && amount<=33756 && do2.getRate()<=33756) {   // modified on 24.04.2025
					return true;
				}
			}									
			return false;
		} 
	}	
    
    public boolean isValidClassStudied(Do2 do2) {  // To Check Whether Claim Is For Lower Class In Subsequent Years
    	
    	if (do2.getDescription().startsWith("CEAH") || do2.getDescription().startsWith("CEAT") || do2.getColumnA().equals("NUR")) {
    		return true;
    	}
    	
    	List<Do2> do2List =do2Repo.findByDescColBFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(),"CEA", do2.getFromDate(),getDateOfBirthInColumnBInReverseOrder(do2));
    	
    	
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
    
    public String getDateOfBirthInReverseOrder(String dateStr) {
    	if (dateStr==null || (dateStr!=null && dateStr.length()<8)) {
    		return " ";
    	}
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
} 



