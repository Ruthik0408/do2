package itsdc.dolphin.do2.accountingcage.independent;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:  13-08-2009
 */

@RestController
public class Plbon extends AccountingCageBase implements java.io.Serializable {
	 
	private GregorianCalendar fromDateCal = new GregorianCalendar();
	private double nonQualifyingService = 0;
	private double qualifyingService = 0;
	private boolean validity = true;
	private double totalEmoluments= 0;
	private int honyDays=0;
	private StringBuffer buffer = new StringBuffer();
	private List<Allowance> plbonFromAllowanceRate;
	private final String ENROLLED_AFTER_FIRST_OCTOBER = "AU:SIX months service not completed. Enrollment Date : ";
	private final String HCOMMN_BEFORE_FIRST_OCTOBER = "AU: Less than SIX months as JCO. Hony Lt Date : ";
	private final String MINIMUM_SERVICE_NOT_COMPLETED = "AU:Minimum service of SIX months not completed in the qualifying year.\n";
	private final String FULL_DETAILS_NOT_AVAILABLE = "AU:Full Details Not Available for Calculating Bonus.\n";
	private final String DUPLICATE_DO2 = "DU:Duplicate do2 fed for the PBOR. \n";
    private final double AVERAGE_DAYS_OF_MONTH_FOR_PLBON = 30.4;
    final static public long MILLIS_IN_DAY = 1000 * 3600 * 24;
 
    // TO CHECK THE VALIDITY OF THE DOII
	@Transactional	
	@Override
	public boolean isValid(Do2 do2){
     	validity = true;     	   	
 // Cut off date is 1st October   	
     	Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
     	//Rank rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
     	Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
    	if (isDuplicateDo2(do2, employee)) {
     	 	buffer.append(DUPLICATE_DO2);
	    	validity = false;
     	}
    	if ((rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) || 
     		(Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC))) {
     		if (isEnrolledAfterCutOffDate(employee, do2)){     		
     			validity=false;
     		}
     	}
    	if (employee.getRegularTaFlag().equals("T") && 
    		!employee.getTaAppointmentCategory().equals("R")){
    		validity=false;
    	}    		
    	if (do2.getArmyNo().substring(0,2).equals("JC") ||
     		do2.getArmyNo().substring(0,2).equals("TJ")	||
     		do2.getArmyNo().substring(0,2).equals("RV")) {
     		if (isHCommnBeforeCutOffDate(employee, do2)){
     			validity=false;
     		}
     	}
    	//added
    	//conditionally  calling this method if do2 is valid upto this point
    	if(validity==true){
    	
     		if (!whetherCompletedMinimumService(employee, do2)) {	       	
    	    	validity = false;
         	}	
    	}
     	
     	
     	
     	
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		 }
		
		do2.setReason(buffer.toString());
		
		do2Repo.save(do2);
		
		return validity;
	}
		
	// PROCESS OF VALID DOII
	@Override
	public String process(Do2 do2) {
		int noOfMonths = 0;
		int remainingDays = 0;
		noOfMonths = (int)(qualifyingService / 30);
		remainingDays=(int)(qualifyingService % 30);
		if (remainingDays >= 15){
			noOfMonths += 1;
		}
							
		if (isValid(do2)) {
	
			double arrearsAmount=((totalEmoluments*plbonFromAllowanceRate.get(0).getRatePercentage())/AVERAGE_DAYS_OF_MONTH_FOR_PLBON);
			
			arrearsAmount=Math.round(arrearsAmount * noOfMonths / 12);
			
			Utils.addArrears(do2, arrearsAmount);
		
		}
		return Do2Manager.SUCCESS;
	}	
		
	//Method to check whether minimum 6 months service completed for all viz. deputationists, absentees, etc.	
	private boolean whetherCompletedMinimumService(Employee emp, Do2 do2) {	
		//qualifyingService = 365;
		if (do2.getDescription().equals("NEBON")) {
			qualifyingService = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
			//System.out.println("NEBON qualifyingService days are "+qualifyingService);
		} else {
			qualifyingService = 365;
		}
		nonQualifyingService = getNqsForBonus(emp, do2);		
		
		if (nonQualifyingService < 0) {
		    buffer.append(FULL_DETAILS_NOT_AVAILABLE);
			return false;
		}			
		if (nonQualifyingService > 182) {
			buffer.append(MINIMUM_SERVICE_NOT_COMPLETED);
			return false;
		}
		double enrollNqsDays=0;
		if(emp.getEnrolmentDate()!=null && emp.getEnrolmentDate().after(do2.getFromDate())){
	    	
	    	Date endDate = DateManager.getPreviousDate(emp.getEnrolmentDate());
	    	   enrollNqsDays = (int)DateManager.getNoOfDays(do2.getFromDate(),endDate);
	    }
		qualifyingService -=enrollNqsDays;
		qualifyingService -=honyDays;
		qualifyingService -= nonQualifyingService;
		return true;
	} 
	
	public boolean isDuplicateDo2(Do2 do2, Employee employee) {
		
	 	List<Do2> do2List = do2Repo.findByFromDateEqualtoAndToDateEqualToMultipleDescAndMultipleStatus(employee.getId(),
	 																		do2.getId(), "PLBON","NEBON", do2.getFromDate(), do2.getToDate());
	 			
	 			
	 		/*	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee "
					+ " AND d.id!=:currentDo2Id AND (d.status='V' or d.status='P') AND (d.description='PLBON' OR d.description='NEBON') " 
					+ " AND (d.fromDate=:fromDate OR d.toDate=:toDate) ") 
					.setParameter("employee", employee) 
					.setParameter("currentDo2Id", do2.getId()) 
					.setParameter("fromDate", do2.getFromDate()).setParameter("toDate", do2.getToDate())
					.getResultList();*/
		
		 return do2List.size()>0;
	}
	
	public boolean isEnrolledAfterCutOffDate(Employee employee, Do2 do2) {	
		EmployeeServiceDetail esd = null;
		boolean enrFlag = false;
		String msg = "";
		fromDateCal.setTime(do2.getFromDate());
		GregorianCalendar bonusQualifyingCal = new GregorianCalendar(((fromDateCal.get(Calendar.YEAR))),Calendar.OCTOBER,1);
		List<EmployeeServiceDetail> employeeServiceDetails =esdRepo.findByEmployeeIdAndStatus(employee.getId(),"E");
				
			/*	Utils.getEntityManager()
				.createQuery(
					" SELECT esd FROM EmployeeServiceDetail esd WHERE esd.employee=:emp  AND esd.status='E' ")
				.setParameter("emp", employee)
				.getResultList();*/
		if (employeeServiceDetails.size() > 0) {
			esd = employeeServiceDetails.get(employeeServiceDetails.size() - 1);
			if (esd.getDo2().getFromDate().after(bonusQualifyingCal.getTime())){
				msg=ENROLLED_AFTER_FIRST_OCTOBER+esd.getDo2().getFromDate()+"\n";
				buffer.append(msg);
				enrFlag=true;
			}
		}else if (employee.getEnrolmentDate()!= null && employee.getEnrolmentDate().after(bonusQualifyingCal.getTime())){
			msg=ENROLLED_AFTER_FIRST_OCTOBER+employee.getEnrolmentDate()+"\n";
			buffer.append(msg);
			enrFlag=true;
		}
		return enrFlag;
	}
	public boolean isHCommnBeforeCutOffDate(Employee employee, Do2 do2) {
		    honyDays=0;
			boolean hCommnFlag = false;
			String msg = "";
			fromDateCal.setTime(do2.getFromDate());
			GregorianCalendar bonusQualifyingCal = new GregorianCalendar(((fromDateCal.get(Calendar.YEAR))),Calendar.OCTOBER,1);
			
			List<EmployeeServiceDetail> employeeServiceDetails = esdRepo.findByDo2DescriptionAndStatusOrderByFromDate(employee.getId(),"R","HCOMMN");
					
			/*		Utils.getEntityManager()
					.createQuery(
						" SELECT esd FROM EmployeeServiceDetail esd WHERE esd.employee=:emp AND esd.status='R' " +
						" AND esd.do2.description =:description ORDER BY esd.fromDate")
					.setParameter("emp", employee)
					.setParameter("description","HCOMMN")
					.getResultList();*/
			if (employeeServiceDetails.size() > 0) {
				for (EmployeeServiceDetail esd1 : employeeServiceDetails) {				
					if (esd1.getDo2().getColumnA().equals("HONYLT") &&
						esd1.getDo2().getFromDate().before(bonusQualifyingCal.getTime())){
						msg=HCOMMN_BEFORE_FIRST_OCTOBER+esd1.getDo2().getFromDate()+"\n";
						buffer.append(msg);
						hCommnFlag=true;
					}//else condition is added to get the number of days as hony service
					else{
						if(esd1.getDo2().getColumnA().equals("HONYLT") 
								&& esd1.getDo2().getFromDate().before(do2.getToDate())){
						//	System.out.println("honydays   9999999  "+esd1.getDo2().getFromDate()+"  "+do2.getToDate());
							honyDays = (int)DateManager.getNoOfDays(esd1.getDo2().getFromDate(),do2.getToDate());	
						}
						
					}
					
				}			
		   }
		   return hCommnFlag;
	 }
	 public int getNqsForBonus(Employee emp, Do2 do2) {
			// If there is no audit cage, the method will return -1. 
				Calendar cal = new GregorianCalendar();
				cal.setTime(do2.getToDate());
				int year = cal.get(Calendar.YEAR);
				int prev_year = (year - 1);
				int nqsDays        = 0;
				int deputationDays  = 0;
				//variable introduced for arrving the number of days before enroll if enroll is after 1.4.2010
				int enrollNqsDays =0;
				Date qual_year_frdt = null;
				Date qual_year_todt = null;
				Date first_date     = null;
				Date last_date      = null;
				Date startDate      = null;
				Date endDate        = DateManager.getToday();
				AuditCage auditCage = null;
				Date enrolmentDate =null;
				totalEmoluments     = 0;
				qual_year_frdt = do2.getFromDate();
				enrolmentDate= emp.getEnrolmentDate();
				if(enrolmentDate.after(qual_year_frdt)){
					qual_year_frdt=enrolmentDate;
				}
				qual_year_todt = do2.getToDate();
				
				
				List<AuditCage> atcgList = acRepo.findAllEffectiveAcOrderByFromDateToDateAsc(emp.getId());
						
		//				Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac where ac.employee=:employee" +
		//				" and ac.effective = true and ac.cancelled = false and ac.recordStatus != 'P' order by ac.fromDate, ac.toDate")	
		//		.setParameter("employee", emp).getResultList();
				if (atcgList == null || atcgList.size() == 0)
		              return nqsDays = -1;
				for(AuditCage ac : atcgList){
				//	System.out.println("ac values  "+ac.getFromDate());
					if (!ac.getFromDate().after(qual_year_frdt)){
						first_date = ac.getFromDate();
						last_date  = ac.getFromDate();
					}
				
					if (ac.getFromDate().after(qual_year_frdt) && 
					   !(ac.getFromDate().after(qual_year_todt))){
						last_date = ac.getFromDate();
					}
									
				}
				
			    
			//	System.out.println("date values  "+enrolmentDate+"  "+first_date+"  "+atcgList.get(0).getFromDate()+"  "+qual_year_frdt);
			    if (first_date==null)
		    		return nqsDays = -1;
			    
			    atcgList = acRepo.findByFromDateBetweenOrderByFromDateToDate(emp.getId(), first_date, last_date);
			    		
			   /* 		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac where ac.employee=:employee and ac.effective = true" +
			    	" and ac.cancelled = false and ac.recordStatus != 'P' and ac.fromDate between :firstDate and :lastDate order by ac.fromDate, ac.toDate")	
				.setParameter("employee", emp)
				.setParameter("firstDate", first_date)
				.setParameter("lastDate", last_date).getResultList();	*/
			    
			    // to read last audit cage which absent_flag is false
			    for (AuditCage ac : atcgList) {
			       if (ac.getAbsentFlag() == false)
			           auditCage = ac;
			    }
			    //this condition added for retreival of allowances
			    if(auditCage !=null){
			    	
			    	// APPT PAY, GSPAY, STRINC TO BE TAKEN FOR totalEmoluments (NOT IN AUDIT CAGE TABLE)	
			    	for (AuditCageAllowance aca : auditCage.getAuditCageAllowances()) {
						if (aca.getPayCode().getPayCode().trim().equals("GSPAY")) {
					        totalEmoluments = (totalEmoluments + aca.getAmount());
						}
						if (aca.getPayCode().getPayCode().trim().equals("APTPAY")) {
					        totalEmoluments = (totalEmoluments + aca.getAmount());
						}
						if (aca.getPayCode().getPayCode().trim().equals("STRINC")) {
					        totalEmoluments = (totalEmoluments + aca.getAmount());
						}
			    	}
			    }
			    
			    
			    
			    //plbonFromAllowanceRate = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getToDate());
				//Added for NE Bonus		
			    if (do2.getDescription().equals("NEBON")) {
			    	plbonFromAllowanceRate = Utils.getApplicableAllowanceRates("PLBON", do2.getFromDate(), do2.getToDate());
		    	} else {
		    	plbonFromAllowanceRate = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getToDate());
		    	}
				//checking auditcage null added
				if(auditCage !=null){					 	
				totalEmoluments =  totalEmoluments + auditCage.getBasicPay()
								  +auditCage.getGradePay()
								  +auditCage.getMsPay()
								  +auditCage.getXGroupPay()
								  +auditCage.getClassPay()
								  +auditCage.getDa();
				}
			 
				
				if (totalEmoluments > plbonFromAllowanceRate.get(0).getRateAmount()) {
					totalEmoluments = plbonFromAllowanceRate.get(0).getRateAmount();
				}
			
			    int i = 0;
			    startDate = null;
		        endDate = qual_year_todt;
		        nqsDays=0;
			    for(AuditCage ac : atcgList){
			    	++i;
			    	if (i == atcgList.size()){
			    	 	if  (ac.getDeputationFlag() == false && ac.getAbsentFlag() == true && startDate != null){
			    		     endDate = qual_year_todt;
			    		     nqsDays += DateManager.getNoOfDays(startDate, endDate);
			     		} else if  (ac.getDeputationFlag() == false && ac.getAbsentFlag() == false && startDate != null){
			     					endDate=DateManager.getDate(ac.getFromDate(), -1);		
			     					nqsDays += DateManager.getNoOfDays(startDate, endDate);
			                    } 
			    		} else if  (ac.getDeputationFlag() == false && ac.getAbsentFlag() == false && startDate != null){
			    					endDate=DateManager.getDate(ac.getFromDate(), -1);	
			    					nqsDays += DateManager.getNoOfDays(startDate, endDate);
			    					startDate = null;
			    					endDate = qual_year_todt;
			    		}
			    	
			    	if (ac.getDeputationFlag() == false && ac.getAbsentFlag() == true && startDate == null){
			    	   startDate = ac.getFromDate();
			    	   if (startDate.before(qual_year_frdt))
			    		   startDate=qual_year_frdt;
			    	   
			    		   
			     	}
			    }
			    i = 0;
			    startDate = null;
		        endDate = qual_year_todt;
		        deputationDays=0;
			    for(AuditCage ac : atcgList){
			    	++i;
			    	if (i == atcgList.size()){
			    	 	if  (ac.getDeputationFlag() == true && ac.getAbsentFlag() == true && startDate != null){
			    		     endDate = qual_year_todt;
			    		     deputationDays += DateManager.getNoOfDays(startDate, endDate);
			     		} else if  (ac.getDeputationFlag() == false && ac.getAbsentFlag() == false && startDate != null){
			     					endDate=DateManager.getDate(ac.getFromDate(), -1);		
			     					deputationDays += DateManager.getNoOfDays(startDate, endDate);
			                    } 
			    	} else if  (ac.getDeputationFlag() == false && ac.getAbsentFlag() == false && startDate != null){
			    				endDate=DateManager.getDate(ac.getFromDate(), -1);		
			    				deputationDays += DateManager.getNoOfDays(startDate, endDate);
			    				startDate = null;
			    				endDate = qual_year_todt;
			    	}
			    	
			    	if (ac.getDeputationFlag() == true && ac.getAbsentFlag() == true && startDate == null){
			    	   startDate = ac.getFromDate();
			    	   if (startDate.before(qual_year_frdt))
			    		   startDate=qual_year_frdt;
			     	}
			    }
			    
			 //   System.out.println("days   "+nqsDays+"  "+deputationDays+" "+enrollNqsDays+"  "+honyDays);
			    //enrollnqsday,honydays added
			    return (nqsDays + deputationDays);
		}
}