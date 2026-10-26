package itsdc.dolphin.do2.accountingcage.independent;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Form16;
import itsdc.dolphin.repositories.Form16Repository;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
import java.util.Date; 

// Option for Income Tax Regime
@RestController
public class Itropt extends AccountingCageBase implements java.io.Serializable {
	
	static final String INVALID_COL7A = "VL:Col. 7A should contain OLD . \n";
	static final String DUPLICATE_DO2 = "DU:Duplicate Do2 fed for the PBOR. \n";
	static final String INVALID_DATE = "VL:AFTER DUE DATE FOR FINANCIAL YEAR. \n";
	static final String INVALID_QE_FOR_DO2 = "VL:ITROPT Cannot Be Processed In This (3rd) Month. \n";
	static final String INVALID_COL7A_FOR_ASSESSMENTYEAR = "VL:Invalid Value Against Column7A For Assessment Year - ";
	static final String UNKNOWN_DEFAULT_TAX_REGIME_ASSESSMENTYEAR = "VL:Default Tax Regime Unknown For Assessment Year - ";
	 
	StringBuffer buffer = new StringBuffer();
	Employee employee;
	Form16 form16;
	String qe, assessmentYear;
	int currentQuarterYear=0, currentQuarterMonth=0, fyStartMonth=0, fyEndMonth=0;
	Date fyStartDate, fyEndDate;
 	
	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
		setFinYearValues();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
			
		if (Utils.getCurrentQuarter().startsWith("03")) {
			buffer.append(INVALID_QE_FOR_DO2);
			validity = false;
		} 
		
		if (!(isValid7a(do2))) {			 
			buffer.append(INVALID_COL7A);
			validity = false;
		}
		
		if (Do2Manager.DEFAULT_NEW_TAX_REGIME_ASSESSMENT_YEARS.contains(assessmentYear)) {
			if (do2.getColumnA()!=null && do2.getColumnA().trim().toUpperCase().equals("NEW")) { // publication not required for default tax regime
				buffer.append(INVALID_COL7A_FOR_ASSESSMENTYEAR+assessmentYear);
				validity=false;
			}
		} else {
			buffer.append(UNKNOWN_DEFAULT_TAX_REGIME_ASSESSMENTYEAR+assessmentYear);
			validity=false;
		}
		
		if (isDuplicateDo2(do2)) {
     	 	buffer.append(DUPLICATE_DO2);     	 	 
	    	validity = false;	    	
	   	}
		
	 	if (!isValidFromDate(do2)) {
     	 	buffer.append(INVALID_DATE); 
	    	validity = false;	    	
	   	} 
		
		if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null	&& !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
			}
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

	@Override
	public String process(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		setFinYearValues();
		updateForm16(do2);
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		setFinYearValues();
		reverseUpdateForm16(refDo2);
		return Do2Manager.SUCCESS;
	}
		
	public boolean isValid7a(Do2 do2) {
		if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().trim().length()==0)) {
			return false;
		}		
	/*	if (do2.getColumnA().trim().length()==3 && (do2.getColumnA().trim().toUpperCase().equals("OLD") 
				|| do2.getColumnA().trim().toUpperCase().equals("NEW"))) {
			return true;
		}*/
		
		if (do2.getColumnA().trim().length()==3 && (do2.getColumnA().trim().toUpperCase().equals("OLD"))) {
			return true;
		}
		return false; 
	}
	
	public boolean isDuplicateDo2(Do2 do2) {
		 List<Do2> do2List=null;
		 do2List  = do2Repo.findFromDateBetweenAndDo2IdNotEqual(employee.getId(), do2.getDescription(), fyStartDate, fyEndDate,  do2.getId());		 
		 return (do2List!=null && do2List.size()>0);	 
	}
	
	public boolean isValidFromDate(Do2 do2) {	
	      form16 = null;
		  form16 = f16Repo.findByEmpIdAndAssessmentYearBetweenFromDateToDate(employee.getId(), assessmentYear, do2.getFromDate());
		  return (form16!=null && form16.getId()>0); 		  
 	}	 
	
	public void updateForm16(Do2 do2) { 
		form16 = null;
		form16 = f16Repo.findByEmpIdAndAssessmentYearBetweenFromDateToDate(employee.getId(), assessmentYear, do2.getFromDate()); 
	 	if (form16!=null) {
			if (!form16.getItOption().trim().toLowerCase().equals(do2.getColumnA().trim().toLowerCase())) {
				form16.setItOption(do2.getColumnA().trim());
				f16Repo.save(form16);
			}
		}		
	}
	
	public void reverseUpdateForm16(Do2 refdo2) { 
		form16 = null;
		form16 = f16Repo.findByEmpIdAndAssessmentYearBetweenFromDateToDate(employee.getId(), assessmentYear, refdo2.getFromDate()); 
	 	if (form16!=null) {
			if (form16.getItOption().trim().toLowerCase().equals(refdo2.getColumnA().trim().toLowerCase())) {
				if (Do2Manager.DEFAULT_NEW_TAX_REGIME_ASSESSMENT_YEARS.contains(assessmentYear)) {
					form16.setItOption("NEW");
				} else {
					form16.setItOption("OLD");
				}
				f16Repo.save(form16);
			}
		}		
	}	
	
	public void setFinYearValues() {
		qe=Utils.getCurrentQuarter();
		currentQuarterYear = Integer.parseInt(qe.substring(3));
		currentQuarterMonth = Integer.parseInt(qe.substring(0,2));		   
	    try {
	    	if (currentQuarterMonth > 3 && currentQuarterMonth < 13) {
	    		fyStartDate = DateManager.getDate(currentQuarterYear, 4, 1);
	    		fyEndDate = DateManager.getDate((currentQuarterYear+1), 3, 31);	   	
	    		assessmentYear = (currentQuarterYear+1)+"-"+(currentQuarterYear+2);
	   	    } else {
	   	    	fyStartDate = DateManager.getDate((currentQuarterYear-1), 4, 1);
	    		fyEndDate = DateManager.getDate(currentQuarterYear, 3, 31);	
	    		assessmentYear = (currentQuarterYear)+"-"+(currentQuarterYear+1);
	   	    }			
		} catch (Exception e) {
			Utils.handleException(e);
		}   
	    System.out.println("...........fystartdate " +fyStartDate);
	    System.out.println(".............fyenddate " +fyEndDate);
	    System.out.println("..........assmnt year  " +assessmentYear);
	}
}