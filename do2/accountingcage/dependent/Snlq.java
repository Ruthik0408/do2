package itsdc.dolphin.do2.accountingcage.dependent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;

import itsdc.dolphin.utils.Utils;
import itsdc.dolphin.utils.DateManager;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;

 

/**
 * 
 * @author ITSDC, Sec'bad.
 *
 */

@RestController
public class Snlq extends AccountingCageBase implements java.io.Serializable {

	static public String INVALID_AGE = "AU:Invalid Age.\n"; 
	static public String INVALID_RANK = "AU:Rank is Invalid.\n"; 
	static public String SNLQ_NOT_ADMISSIBLE = "AU:SNLQ is not admissible as CILQ is not on CCOTH Rates.\n";
	static public String ZERO_REFDO2 = "VL:Reference Do2 not provided for Subsequent Claim.\n";
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2. \n";
	static public String DRAWING_FIELD_ALLOWANCE = "AU:PBOR is drawing Field Allowance.\n";
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";

	public StringBuffer buffer = new StringBuffer();
	private double totalSnlqAdmissible = 0;
	public List<Allowance> alwList;
	
	Employee emp;
	Rank rank;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if(!isValidRefDo2(do2)) {
			validity = false;
			if(do2.getRefDo2No() == null) {
				buffer.append(ZERO_REFDO2);
			} else {
				buffer.append(INVALID_REFDO2);
			}
		}

		if(!isValidAge(do2)) {
			validity = false;
			buffer.append(INVALID_AGE);
		}

		if(!isValidRank(do2)) {
			validity = false;
			buffer.append(INVALID_RANK);
		}
		
		
		if(!isCilqCcothCcz(do2)) {
			validity = false;
			buffer.append(SNLQ_NOT_ADMISSIBLE);
		}
		
		if (isDrawing_Field_Allowance(do2)) {
			validity = false;
			buffer.append(DRAWING_FIELD_ALLOWANCE);
		}

		if(!isValidAllowanceRate(do2)) {
			validity = false;
			buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
			do2.setStatus(Do2Manager.PENDING);
		}
		
		if(!validity) {
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

		do2.setQe(Utils.getCurrentQuarter());
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}	
	
	public boolean isValidAllowanceRate(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(Do2Manager.SNLQ, do2.getFromDate(), do2.getToDate(), rank, do2.getColumnC());
		return (alwList.size() != 0);
	}
	
	@Override
	public String process(Do2 do2) {
		if(emp == null) {
			emp = Utils.getCurrentEmployee(do2.getArmyNo(), do2.getCheckDigit());
		}
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		String city = do2.getColumnC();
		
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
		
			if (city!=null && (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()))) {
				if (city.equals("CCC") || city.equals("CCOTH")) {
					city = "CCZ";
				} 
				if (city.equals("CCA") || city.equals("CCB1") || city.equals("CCB2")) {
					city = "CCY";
				}
				if (city.equals("CCA1")) {
					city = "CCX";
				}
			}
				
			alwList = Utils.getApplicableAllowanceRates(Do2Manager.SNLQ, do2.getFromDate(), do2.getToDate(), rank, city);
			
			for (Allowance allow : alwList) {
				totalSnlqAdmissible += DateManager.getNoOfMonths(allow.getFromDate(), allow.getToDate()) * allow.getRateAmount();
			}
				
			Utils.addArrears(do2, totalSnlqAdmissible);
		}
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		if(do2.getColumnA().equals(Do2Manager.GRANTED)) {
			if(do2.getRefDo2No() != null) {
				Do2 refdo2 = Utils.getReferenceDo2(do2);
				if(refdo2 != null) {
					return (refdo2.getDescription().equals(Do2Manager.SNLQ) || refdo2.getDescription().equals(Do2Manager.CILQ));
				}
			}
		} else if(do2.getColumnA().equals("INITIAL")){
			return (do2.getRefDo2No() == null);
		}

		return false;
	}

	
	public boolean isValidAge(Do2 do2) {
		double empAge = DateManager.getNoOfMonths(emp.getBirthDate(), do2.getFromDate());
		return ((empAge / 12) >= 25);
	}
   /*  Auth: CGDA letter No.AT/I/07 dated 27.02.1992 addressed to CDA(ORs) South, Bangalore, 
	   CDA(ORs) Central Nagpur, CDA(ORs) North, Meerut Cantt.
	   Single JCOs and married JCOs living without families who are permitted to make their own
	   arrangements for accommodation are entitled to CILQ at two-thirds of the rate payable to JCOs at duty station
	   Para 3 says - Incidentally it may be mentioned that in addition to JCOs, NCOs and ORs posted to NCC units are 
	   also eligible to draw CILQ for family at other town rates in conjection with provision of single accommodation
	   at duty station or compensation in lieu thereof at 2/3rd of the married rate 
	   under GOI MOD lr. No.6731/CILQ/DGNCC/ADM(A)/890/B/D(GS-I) dated 13.07.87 */
	public boolean isValidRank(Do2 do2) {
		if (Integer.parseInt(rank.getRankCode()) >= 50 && Integer.parseInt(rank.getRankCode()) < 80) {
			return true;
		} else if (Integer.parseInt(rank.getRankCode()) >= 20 && Integer.parseInt(rank.getRankCode()) < 50){
			if (do2.getUnitCode() != null && do2.getUnitCode().startsWith("N")){
				return true;
			}
		}
		return false;
	}

	public boolean isFirstClaim(Do2 do2) {
		return (do2.getColumnB() != null);
	}
	
	public boolean isCilqCcothCcz(Do2 do2) {
		Employee employee =Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> d2 = do2Repo.findByColumnCFromDateGreaterThanEqual(employee.getId(), "CILQ", getAllowanceFromDate(do2.getFromDate()), 
																										"CCOTH", "CCZ");
				
				
		/*		Utils.getEntityManager()
							.createQuery("select doii from Do2 doii where " +  
							"doii.employee=:employee and doii.checkDigit=:cd and "+ 
							"doii.description=:desc and doii.fromDate>=:frDate and " + 
							"(doii.columnC=:colc1 or doii.columnC=:colc2)")
							.setParameter("employee",employee)
							.setParameter("cd",do2.getCheckDigit())
							.setParameter("desc","CILQ")
							.setParameter("colc1","CCOTH")
							.setParameter("colc2","CCZ")
							.setParameter("frDate",getAllowanceFromDate(do2.getFromDate())).getResultList();*/
		return (d2.size() >= 0);
	}
	
	public Date getAllowanceFromDate(Date frDate) {
		Date ldFromDate = DateManager.getToday();
		Calendar cal1 = new GregorianCalendar();
		cal1.setTime(frDate);
		GregorianCalendar cal2 = new GregorianCalendar(cal1.get(Calendar.YEAR),Calendar.JANUARY,1);
		GregorianCalendar cal3 = new GregorianCalendar(cal1.get(Calendar.YEAR),Calendar.JULY,1);
		if(cal1.get(Calendar.MONTH) <= 6) {
			ldFromDate.setTime(cal2.getTimeInMillis());
		} else {
			ldFromDate.setTime(cal3.getTimeInMillis());
		}
		return ldFromDate;
	} 

	public boolean isDrawing_Field_Allowance(Do2 do2) {
		List<String> allowanceList = Arrays.asList(new String[] {"CMFAA", "CFAA", "SIACHN", "CHAFAA", "SCCIA"});

		return Utils.isMutexAllowance(do2.getArmyNo(), allowanceList, do2.getFromDate(), do2.getToDate(),do2);
	}

}