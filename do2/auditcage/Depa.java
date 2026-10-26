package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Depa extends AuditCageBase implements  Do2AuditCageProcessor {
	Employee employee = null;
	String message;
	double depaAmount = 0;
	private Rank rank;
	StringBuffer buffer = new StringBuffer();
	boolean validity = true;
	private List<String> stopAllowanceList = Arrays.asList("DEPA");
	static public final String NOT_ADMISSIBLE_RECRUIT= "AU:Not admissible to Recruit. \n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public final String NOT_ADDMISSIBLE_NCCUNIT = "AU: Serving in NCC UNIT . Not Admissible. \n";
	
	List<String> stopAllowanceListNsga =  Arrays.asList("NSGA");
	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		boolean validity = true;
		Date admissibleDate = null;
		try {
			admissibleDate = DateManager.getDate("2002-10-10");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (isRecruit(rank)) {
 			do2.setStatus(Do2Manager.PENDING);
	    	buffer.append(NOT_ADMISSIBLE_RECRUIT);
	    	validity = false;
	    }
		if (!(do2.getColumnA().equals("CEASED") || do2.getColumnA().equals("GRANTED"))) {
			do2.setStatus("MR");
			buffer.append("Col-7A should be either GRANTED / CEASED" + NOT_IN_APPENDIX_J_FORMAT);
			validity = false;
		}
		
		// As intimated by SAO of PAO Subatu-- done in 10/2018.
		if(isServingInNccUnit(do2)) {
			buffer.append(NOT_ADDMISSIBLE_NCCUNIT);
			validity = false;
		}
		
		if (do2.getColumnA().equals("CEASED")) {
		        validity = true;
		} else {
			if (!do2.getFromDate().after(admissibleDate)){
				if (do2.getRate() != 10){
					validity = false;
					buffer.append("VL: Rate column should be 10");
				} 
			} else if (do2.getFromDate().after(admissibleDate) && 
					    do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
				validity = false;
				buffer.append("VL: Not admissible during 11.10.2002 and 31.08.2008");
			} else if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
				/*if (!(do2.getRate() == 50) || do2.getRate() == 100){
					validity = false;
					buffer.append("VL: Rate Should be equal to either 50 or 100");
		        }*/
				if (do2.getColumnB() == null || 
				   (do2.getColumnB() != null && !(do2.getColumnB().equals("50%") || do2.getColumnB().equals("100%")))){
					validity = false;
					do2.setStatus("MR");
					buffer.append("VL: Percentage in Col-7B Should be equal to either 50 or 100" + NOT_IN_APPENDIX_J_FORMAT);
				}
			}
		}
			 
	    if (!validity) {
	    	if(do2.getStatus() != null)
	    		do2.setStatus(Do2Manager.INVALID);
		}else{
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
			do2.setStatus(Do2Manager.VALID);
		}
		
		
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
	 	return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		return newAuditCage;
	}
	

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage); 
 		
		/*
		int basicPay = baseAuditCage.getBasicPay() == null ? 0 : baseAuditCage.getBasicPay();
		int classPay = baseAuditCage.getClassPay() == null ? 0 : baseAuditCage.getClassPay();
		int gradePay = baseAuditCage.getGradePay() == null ? 0 : baseAuditCage.getGradePay();*/
		
		// above lines commented and amended as given below due to seven cpc
		int basicPay = 0;
		int classPay = 0;
		int gradePay = 0;
		
		if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			basicPay = baseAuditCage.getBasicPay() == null ? 0 : baseAuditCage.getBasicPay();
			classPay = baseAuditCage.getClassPay() == null ? 0 : baseAuditCage.getClassPay();
			gradePay = baseAuditCage.getGradePay() == null ? 0 : baseAuditCage.getGradePay();
		} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			basicPay = baseAuditCage.getOldBasicPay() == null ? 0 :baseAuditCage.getOldBasicPay();
			gradePay = baseAuditCage.getOldGradePay() == null ? 0 :baseAuditCage.getOldGradePay();
		} else {
			basicPay = baseAuditCage.getBasicPay() == null ? 0 :baseAuditCage.getBasicPay();
		}
		
		
	 	if (do2.getColumnA().trim().equals("GRANTED")) { 
	 		try {
				if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
					List<Allowance> allowances = Utils.getApplicableAllowanceRates(do2
							.getDescription(), do2.getFromDate(), do2.getFromDate());
					if (allowances.size() > 0){
						// DEPA percentage was 10 before 11.10.2002
						depaAmount = Math.round((basicPay + classPay) * 0.1);
					}
				} else  if (newAuditCage.getFromDate().before((DateManager.getSeventhCpcAllowanceDate()))){
					/* DEPA 5% within same station and 10% in other cases
					 * For JCO/ORs - 50% of the above with service concessions
					 *               100% without service concessions
					 * DEPA should not exceed Rs.2000 and Rs.4000 for 5% and 10% respectively
					 */
					if (do2.getColumnB().equals("50%")) {
					    depaAmount = Math.round((basicPay + gradePay) * 0.05);
					    if (depaAmount > 2000){
					    	depaAmount = 2000;
					    }
					} else {
						 depaAmount = Math.round((basicPay + gradePay) * 0.1);
						 if (depaAmount > 4000){
						    depaAmount = 4000;
						 }
					}
				} else {
					if (do2.getColumnB().equals("50%")) {
					    depaAmount = Math.round((basicPay) * 0.05);
					    if (depaAmount > 4500){
					    	depaAmount = 4500;
					    }
					} else {
						 depaAmount = Math.round((basicPay) * 0.1);
						 if (depaAmount > 9000){
						    depaAmount = 9000;
						 }
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	 	}
	 	AuditCageHelper.updateAuditCageAllowance(newAuditCage,"DEPA", depaAmount);
	 	AuditCageHelper.removeAllowances(stopAllowanceListNsga, newAuditCage);
	 	if (do2.getColumnA().trim().equals("CEASED")) {
	 	   	 AuditCageHelper.removeAllowances(stopAllowanceList, newAuditCage);
	 	}
	 	
		return Do2Manager.SUCCESS;
	}
	

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isRecruit(Rank rank) {
		return (rank != null && rank.getRankCode().equals("10"));
	}

	
private boolean isServingInNccUnit(Do2 do2) {
	 	
		/*if (do2.getUnitCode().startsWith("N") && 
				(do2.getUnit().getUnitName().contains("NCC") ||  do2.getUnit().getUnitName().contains("N C C"))) {
			return true;
		} else {
			return false;
		}*/
	
		if (do2.getUnitCode()==null) {
			return true;
		}
	
		if (do2.getUnitCode().startsWith("N")) {
			if (do2.getUnit()==null || (do2.getUnit()!=null && do2.getUnit().getUnitName()==null)) {
				return true;
			} else {
				if (do2.getUnit().getUnitName().contains("NCC") || do2.getUnit().getUnitName().contains("N C C")) {
					return true;
				}
			}
		}
		return false;
	}
}

	