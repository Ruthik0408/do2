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
public class Proall extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable{
	 
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";  
 	static public final String SERVICE_DETAIL_MISSING = "AU:Employee service details are missing. \n";
	static public final String PRIOR_TO_7CPC = "AU:Orders Awaited For Adjustment Prior to 01.07.2017. \n";
	static public final String OVERLAP = "OL:Allowance Already Being Drawn By The Individual. \n";
	static public final String POSIN_NOT_FOUND = "VL:POSIN Reference DO2 Not Found. \n";
 
	List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();
	
	@Override
	public boolean isValid(Do2 do2) {
		
		if (do2.getColumnA().equals("CEASED")) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;			
		}
		
		Rank rank  = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}
		
		boolean validity = true;
		if (rank==null) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(SERVICE_DETAIL_MISSING);
			validity = false;
		}

		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
	 	AuditCage ac = Utils.getPreviousDateAuditCage(employee, do2);
		
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(PRIOR_TO_7CPC);
			validity = false;
		}
	 	
		/*
	  	if (isOverlap(do2, ac)) {
	 		do2.setStatus(Do2Manager.INVALID);
			buffer.append(OVERLAP);
			validity = false;
	 	} */
	 	
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
    
		if (do2.getColumnA().equals("GRANTED")) {
			double rate = 0; 
			int gradePay = 0;
			gradePay = Do2Manager.getSevenCpcGradepay(newAuditCage.getPayLevel());			
			List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), gradePay, do2.getFromDate(),do2.getFromDate());
			if (alwList!=null && alwList.size()>0) {
				rate = alwList.get(0).getRateAmount();
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), rate);
			}		
		
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
	
	public boolean isOverlap(Do2 do2, AuditCage ac) { 	 
	 
		List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(ac.getId());
				
			//	Utils.getEntityManager().createQuery("select aca from AuditCageAllowance aca where aca.auditCage=:ac")
				//								.setParameter("ac", ac).getResultList();
	 	for (AuditCageAllowance aca : acaList) {
			if (aca.getPayCode().getPayCode().trim().equals(do2.getDescription().trim())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		Do2 d = Utils.getReferenceDo2(do2);
		if (d!=null && d.getDescription().equals("POSIN")) {
			return true;
		}
		return false;
	}		
}