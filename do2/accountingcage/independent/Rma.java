package itsdc.dolphin.do2.accountingcage.independent;
import itsdc.dolphin.do2.AccountingCageBase;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

/**
 * 
 * @author ITSDC, Sec'bad.
 *
 */

@RestController
public class Rma extends AccountingCageBase implements java.io.Serializable {
	
	private double totalAmountAdmissible = 0;
	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	public List<Allowance> alwList;
	
	static public String INVALID_DISTANCE = "VL:Distance seems to be high. Check the correctness.\n";
	static public String NOT_CORRECT_FORMAT="VL: Not in Correct Format. KMS is required. \n";
	static public String INVALID_CONVEYANCE = "VL:Invalid/unauthorised mode of conveyance.\n";
	static public String INVALID_RATE = "VL:Rate not provided.\n";
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";
	static public String IS_A_GURKHA = "AU:PBOR is a Gurka as per Employee table.\n";
	static public String NOT_A_GURKHA = "AU:PBOR is not a Gurka.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		emp = Utils.getCurrentEmployee(do2.getArmyNo(), do2.getCheckDigit());
		
		if (do2.getDescription().equals(Do2Manager.RMA)) {
			if (!isValidRate(do2)) {
				validity = false;
				buffer.append(INVALID_RATE);
			}
		}
		if (do2.getDescription().equals(Do2Manager.GRMA)) {
			if (!isGurkha(do2)) {
				validity = false;
				buffer.append(NOT_A_GURKHA);
			}
		}

		if (!isValid7a(do2)) {
			validity = false;			
		}

		if (!isValid7b(do2)) {
			validity = false;
		}
		
	    if (validity==true){
	    	if (!isValidAllowanceRate(do2)) {
	    		validity = false;
	    		buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
	    	}
        }
		
		do2.setReason(buffer.toString());
				
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
		
		do2.setQe(Utils.getCurrentQuarter());
		
		do2Repo.save(do2);
		return validity;
	}

	public boolean isValidAllowanceRate(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnA(), do2.getFromDate(), do2.getFromDate());
		return (alwList != null);
	}

	@Override
	public String process(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnA(), do2.getFromDate(), do2.getFromDate());
	
		if (alwList != null) {
			 int index =0;
			 index = do2.getColumnB().indexOf('K');
			 String columnB = do2.getColumnB().substring(0,index);
			 int kms = Integer.parseInt(columnB.trim());
			 double rate = alwList.get(0).getRateAmount();
			 if (do2.getRate() > rate) {
				 do2.setRate(rate);
			 } else {
				 // As per directions of CDA, ITSDC do2 rate taken for calculation of amount(Leh obs)
				 rate = do2.getRate();
			 }
			totalAmountAdmissible += (kms * rate);
		}
		
		Utils.addArrears(do2, totalAmountAdmissible);
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidRate(Do2 do2) {
		return (do2.getRate() > 0);
	}
	
	public boolean isValid7a(Do2 do2) {
		boolean validity = false;
		int gradePay = 0;
		if (do2.getColumnA() == null){
			buffer.append(INVALID_CONVEYANCE);
			return false;
		}			
		if (do2.getColumnA().equals("TAXI")  || do2.getColumnA().equals("AUTO") || 
			do2.getColumnA().equals("CYCLE") ||	do2.getColumnA().equals("FOOT")){
			validity = true;
		} else {
			buffer.append(INVALID_CONVEYANCE);
		}
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> auditCageList = acRepo.findByEmpIdEffectiveStatusNotEqualPendingFromDateBetweenOrderByFromDateDesc(emp.getId(), do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE "
									+ " ac.employee=:emp AND ac.effective=true AND ac.cancelled=false AND  " 
									+ " ac.recordStatus!='P' AND (:do2FromDate between ac.fromDate and ac.toDate) " 
									+ " order by ac.fromDate desc, ac.toDate desc ")
									.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
									.setParameter("do2FromDate", do2.getFromDate()).getResultList();*/
		if (auditCageList.size() > 0) {
			if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
				if (auditCageList.get(0).getOldGradePay() != null)
					gradePay = auditCageList.get(0).getOldGradePay();
				if (do2.getColumnA().equals("TAXI")) {
					if (gradePay >= 4200) {
						validity=true;
					} else {
						validity=false;
						buffer.append("Taxi not entitled for Grade pay " + gradePay);
					}
				}
			} else {
				if (auditCageList.get(0).getGradePay() != null)
					gradePay = auditCageList.get(0).getGradePay();
				if (do2.getColumnA().equals("TAXI")) {
					if (gradePay >= 4200) {
						validity=true;
					} else {
						validity=false;
						buffer.append("Taxi not entitled for Grade pay " + gradePay);
					}
				}
			}
			
		}
		return validity;
	}

	public boolean isValid7b(Do2 do2) {
	    if(do2.getColumnB()==null)
	    	 return false;
	     
		int index =0;
	 
		index = do2.getColumnB().indexOf('K');
		
		if(index <1){
		    buffer.append(NOT_CORRECT_FORMAT);	
			return false;
		}else{
			 
			 index = do2.getColumnB().indexOf('K');
			 String columnB = do2.getColumnB().substring(0,index).trim();
             if(!isNumeric(columnB)){
            	 buffer.append(NOT_CORRECT_FORMAT);
            	 return false;
             }else{
				int kms = Integer.parseInt(columnB.trim());
				if(kms >500){
					buffer.append(INVALID_DISTANCE);
				 return false;
			 }
             }
		}
		return true;
	}
	public boolean isNumeric(final String s) {
		  final char[] chars = s.toCharArray();
		  for (int x = 0; x < chars.length; x++) {      
		    final char c = chars[x];
		    if ((c >= '0') && (c <= '9')) continue; // numeric
		    return false;
		  }  
		  return true;
		}
	
		public boolean isGurkha(Do2 do2) {
		return (emp.getGsoCode() == 3);
	}
}