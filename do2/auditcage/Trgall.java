package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.BhutanSlab;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Trgall extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable{
	 
	String message; 
	StringBuffer buffer = new StringBuffer();
	boolean validity = true; 
	List<String> mutexNames = Arrays.asList(new String[] {"TRGALL", "DEPA" });
    List<Allowance> alwList = null;

	static public final String IS_OVERLAPPING = "AU:Descripition is overLapping with DEPA DO2. \n";
	static public final String OVERLAPPING_WITH_DEPA = "AU:PBOR is in receipt of Deputation Allowance as per auditcage. \n";
	static public final String SERVICE_DETAIL_MISSING = "AU:PBOR's service details not available in ESD. \n";
	static public final String NOT_ADMISSIBLE_NCCUNIT = "AU: Serving in NCC UNIT . Not Admissible. \n";
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String INVALID_RATE = "VL:Invalid Rate Percentage. \n";
	static public final String INVALID_FROMDATE = "VL:FromDate Before Allowance Effective Date. \n";
		
	@Override
	public boolean isValid(Do2 do2) {
		
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Rank rank  = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");		
		validity = true;
		
		if (do2.getColumnA().equals("CEASED")) {
		    validity = true;
		} else {
			if (rank==null) {
				if (employee.getSubstantiveRank()!=null 
						&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
					validity = false;
					buffer.append(PBOR_IS_RECRUIT);	
					do2.setStatus(Do2Manager.PENDING);
				} else {
					validity = false;
					buffer.append(SERVICE_DETAIL_MISSING);
					do2.setStatus(Do2Manager.PENDING);
				}
			}
		 
		// As intimated by SAO of PAO Subatu-- done in 10/2018.
			if(isServingInNccUnit(do2)) {
				buffer.append(NOT_ADMISSIBLE_NCCUNIT);
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
			}
			
			if (!isOverlapping(do2)) {
				buffer.append(IS_OVERLAPPING);
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
			}
			
			if (!isOverlappingWithAuditCage(do2)) {
				buffer.append(OVERLAPPING_WITH_DEPA);
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
			}
			
			if (do2.getColumnB().equals("NATIONAL") && do2.getRate()!=24) {
				buffer.append(INVALID_RATE);
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
			}
			
			if (do2.getColumnB().equals("OTHERS") && do2.getRate()!=12) {
				buffer.append(INVALID_RATE);
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
			}
			
			if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				buffer.append(INVALID_FROMDATE);
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
			}
		 }
			
		do2.setReason(buffer.toString());
					
		if (validity) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
				
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
		
		if(do2.getColumnA().equals("GRANTED")) {
			int basicPay = (int) Math.round(newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay());
			List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(),do2.getFromDate(),do2.getFromDate());
			double rate = Math.round((basicPay) * alwList.get(0).getRatePercentage());
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), rate);
		} else {
			final String[] alwUnderProcess = { do2.getDescription() };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
 
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	
	private boolean isServingInNccUnit(Do2 do2) {
	 	
	//	if (do2.getUnitCode().startsWith("NCC")) {
		if (do2.getUnit()!=null && do2.getUnit().getUnitName().contains("NCC") || do2.getUnitCode().startsWith("N")) {     // modified on 23.06.2025
			return true;
		} else if (do2.getUnitCode()!=null && do2.getUnitCode().startsWith("N")) { // added on 22.09.2025
			return true;
		} else {
			return false;
		}
	}

//Method to ensure that Depa adjusted with AR audit cage do not overlap with instal
	private boolean isOverlappingWithAuditCage(Do2 do2) {
		
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> auditCageList = acRepo.findByFromDateBetween(emp.getId(),do2.getFromDate());
				
				
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
	
	private boolean isOverlapping(Do2 do2) {	 	
		return ! Utils.isMutexAllowance(do2.getArmyNo(), mutexNames, do2.getFromDate(), do2.getToDate(),do2);	
	}
}