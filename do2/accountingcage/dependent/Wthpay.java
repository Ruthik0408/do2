package itsdc.dolphin.do2.accountingcage.dependent;
  
import java.util.List; 
import org.springframework.web.bind.annotation.RestController; 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeDeduction; 
import itsdc.dolphin.do2.AccountingCageBase; 
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * @author -- Withholding of pay during arrest / close arrest period 
 */

@RestController
public class Wthpay extends AccountingCageBase implements java.io.Serializable {
 
	Employee employee;
	public StringBuffer buffer = new StringBuffer();
	static public String PUNISHMENT_INVALID = "VL:OC or CM should be mentioned.\n";
	static public String LRA_DAYS = "VL:Days in LRA Col. is not matching.\n";
	static public String ZERO_AUDIT_CAGE = "AU:PBOR is not getting Pay and Allowances.\n";
 	static public String BLANK_COLUMNA = "VL:Column7A Cannot be Blank.\n";
	static public String BLANK_COLUMNC = "VL:Column7C Cannot be Blank.\n";
	static public String INCORRECT_COLUMNA = "VL:Column7A Value Exceeds 3 Characters.\n";
	static public String NONNUMERIAL_COLUMNA = "VL:Column7A Does Not Contain Numerical Value.\n";
	static public String INCORRECT_NUMERICAL_COLUMNA = "VL:Incorrect Numerical Value In Column7A.\n";
	static public String INCORRECT_COLUMNC = "VL:ARREST/CLOSAR Not Mentioned.\n";
    static public String PAY_NOT_DRAWN = "AU: Pay Not Found Drawn By The Individual. \n";
    static public String NON_EFFECTIVE = "VL:IRLA Is In Non-Effective Status.\n";
    static public String PRIOR_PERIOD = "VL:Period Prior To Introduction of Description.\n";
    
    @Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;	
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().trim().length()==0)) {
			validity = false;
			buffer.append(BLANK_COLUMNA);
		} else {
			if (do2.getColumnA().trim().length()>3) {
				validity = false;
				buffer.append(BLANK_COLUMNA);
			}
			if (!Utils.isNumeric(do2.getColumnA().trim())) {
				validity = false;
				buffer.append(NONNUMERIAL_COLUMNA);
			} else if (Integer.parseInt(do2.getColumnA().trim())<=0 || Integer.parseInt(do2.getColumnA().trim())>100) {
					validity = false;
					buffer.append(INCORRECT_NUMERICAL_COLUMNA);
			}
		}

		if (do2.getColumnC()==null || (do2.getColumnC()!=null && do2.getColumnC().trim().length()==0)) {
			validity = false;
			buffer.append(BLANK_COLUMNC);
		} else if (!(do2.getColumnC().trim().equals("ARREST") || do2.getColumnC().trim().equals("CLOSAR"))) {
			validity = false;
			buffer.append(BLANK_COLUMNC);
		}	
		
		if (!isPayDrawnInIrla(do2)) {
			validity = false;
			buffer.append(PAY_NOT_DRAWN);
		}
		 
		if (employee.getStatusCode()!=null && employee.getStatusCode().getId()>4) {
			validity = false;
			buffer.append(NON_EFFECTIVE);
		}
		
		try {
			if (do2.getFromDate().before(DateManager.getDate("2025-04-01"))) {  //deemed introduction date
				validity = false;
				buffer.append(PRIOR_PERIOD);
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
	 	if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		} 		
		do2Repo.save(do2);
		return validity;	
	}

	@Override
	public String process(Do2 do2) {
		if (isValid(do2)) {
			 ActUponDo2(do2);
		}
		return Do2Manager.SUCCESS;
	}

	public String ActUponDo2(Do2 do2) {
		employee =Utils.getCurrentEmployee(do2.getArmyNo());
		String qe = DateManager.getStringDateWithSeparators(do2.getFromDate());
		qe=qe.substring(3);
	//	System.out.println("....................qe "+qe +"  monthinrev "+qe.substring(3)+qe.substring(0,2));
		int monthinreverse = Integer.parseInt(qe.substring(3)+qe.substring(0,2));
		
		Double monthlyPayConsolidatedSinceFromDateMonth = 0.0;
		Double withholdAmountForPastPeriod = 0.0;
		monthlyPayConsolidatedSinceFromDateMonth = edRepo.getTotalQuarterPaySinceWithholdMonth(employee.getId(), monthinreverse);
		if (monthlyPayConsolidatedSinceFromDateMonth>0) {
			withholdAmountForPastPeriod=((Double) (monthlyPayConsolidatedSinceFromDateMonth*Integer.parseInt(do2.getColumnA())/100)*-1);
		}
		Utils.addArrears(do2, withholdAmountForPastPeriod);
		return Do2Manager.SUCCESS;
	}

	public boolean isPayDrawnInIrla(Do2 do2) {
		employee =Utils.getCurrentEmployee(do2.getArmyNo());
		String qe = DateManager.getStringDateWithSeparators(do2.getFromDate());
		qe=qe.substring(3);
//		System.out.println("qe arrived from fromdate ..... "+qe);
		List<EmployeeDeduction> ed = edRepo.findByEmployeeIdAndQe(employee.getId(), qe);
		if (ed!=null && ed.size()>0 && ed.get(0).getQuarterPay()>0) {
			return true;
		}
		return false;
	} 
}