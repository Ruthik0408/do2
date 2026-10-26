package itsdc.dolphin.do2.accountingcage.independent;

import java.util.*;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Instal extends AccountingCageBase implements java.io.Serializable {

	 
	String message;

	private boolean validity = true;
	StringBuffer buffer = new StringBuffer();
	List<String> mutexNames = Arrays.asList(new String[] {"INSTAL", "DEPA" });
		
	static public final String IS_OVERLAPPING = "AU:Descripition is overLapping with DEPA DO2. \n";
	static public final String OVERLAPPING_WITH_DEPA = "AU:PBOR is in receipt of Deputation Allowance as per auditcage. \n";
	static public final String INVALID_REF_DO2 = "AU:Incorrect Reference Do2 - does not pertain to POSIN casualty. \n";
	static public final String INVALID_POSIN_UNIT = "AU:PBOR's posted unit does not figure in the list of authorised training establishments. \n";
	static public final String PBOR_STILL_RECRUIT = "AU:PBOR is still a recruit. \n";
	static public final String SERVICE_DETAIL_MISSING = "AU:PBOR's service details not available in ESD. \n";
	static public final String NOT_ADDMISSIBLE_NCCUNIT = "AU: Serving in NCC UNIT . Not Admissible. \n";
	
	@Transactional
	@Override
	public boolean isValid(Do2 do2) {
		validity = true;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		/*
		if (!isValidRefDo2(do2)) {
			buffer.append(INVALID_REF_DO2);
			validity = false;
		}
		
		if (!validTrainingEstablishment) {
			buffer.append(INVALID_POSIN_UNIT);
			validity = false;
		}
		*/
		
		//Rank rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
		if (rank==null) {
			if (employee.getSubstantiveRank()!=null 
					&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
				validity = false;
				buffer.append(PBOR_STILL_RECRUIT);	
			} else {
				validity = false;
				buffer.append(SERVICE_DETAIL_MISSING);
			}
		} 
		
		// As intimated by SAO of PAO Subatu-- done in 10/2018.
		if(isServingInNccUnit(do2)) {
			buffer.append(NOT_ADDMISSIBLE_NCCUNIT);
			validity = false;
		}
		if (!isOverlapping(do2)) {
			buffer.append(IS_OVERLAPPING);
			validity = false;
		}
		
		if (!isOverlappingWithAuditCage(do2)) {
			buffer.append(OVERLAPPING_WITH_DEPA);
			validity = false;
		} 
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
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
			
		if (validity) {
			List<Allowance> alwList;
			
			/*alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getToDate(), 
					Utils.getRank(do2.getArmyNo(), do2.getFromDate()));*/
			
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getToDate(), 
					Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE"));
			
			double instalRate=0;
			
			for (Allowance allow : alwList) {
				instalRate += DateManager.getNoOfMonths(allow.getFromDate(), allow.getToDate()) * allow.getRateAmount();
			}
				 
			Utils.addArrears(do2, instalRate);
			}
		return Do2Manager.SUCCESS;
	}	
	
	private boolean isValidRefDo2(Do2 do2) {
		if (Utils.getReferenceDo2(do2).getDescription().equalsIgnoreCase(Do2Manager.POSIN)) {
			//if (Utils.getReferenceDo2(do2).getUnitCode().equalsIgnoreCase(anotherString)) {       to be filled after creation of table
				//validTrainingEstablishment = true;
			//}
			return true;
		}
		return false;
	}
	
	private boolean isOverlapping(Do2 do2) {
		 	
		return ! Utils.isMutexAllowance(do2.getArmyNo(), mutexNames, do2.getFromDate(), do2.getToDate(),do2);
	}
	
//Method to ensure that Depa adjusted with AR audit cage do not overlap with instal
	private boolean isOverlappingWithAuditCage(Do2 do2) {
		
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> auditCageList = acRepo.findByFromDateBetween(emp.getId(), do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE "
								+ " ac.employee=:emp AND ac.effective=true AND ac.cancelled=false AND ac.absentFlag=false AND " 
								+ " ac.recordStatus!='P' AND (:do2FromDate between ac.fromDate and ac.toDate)")
								.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
								.setParameter("do2FromDate", do2.getFromDate()).getResultList();*/
		if (auditCageList.size() > 0) {
			for (AuditCage ac : auditCageList) {
				for (AuditCageAllowance aca : ac.getAuditCageAllowances()) {
					if (aca.getPayCode().getPayCode().equals("DEPA")) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	
	private boolean isServingInNccUnit(Do2 do2) {
	 	
		if (do2.getUnitCode().startsWith("NCC")) {
			return true;
		} else {
			return false;
		}
	}
}