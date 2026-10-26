package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Islnda extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;
	static public String RATE_PERCENTAGE_NOT_AVAILABLE = "AU:Rate Percentage not available in the Allowance Rate table.\n ";
	
	private StringBuffer buffer = new StringBuffer();
	private double islandRate = 0.0;
		
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if(do2.getColumnA() != null 
				&& do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		if (!isPercentageRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(RATE_PERCENTAGE_NOT_AVAILABLE);
	 	}else{
			do2.setStatus(Do2Manager.VALID);
	  	}
		do2Repo.save(do2);
	
		return validity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
	
		AuditCageHelper.copyCurrentAllowances(newAuditCage,	baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
				AuditCageHelper.removeAllowances(Arrays.asList(new String[]{ Do2Manager.ISLNDA }), newAuditCage);
		} else if ((do2.getColumnA().equals(Do2Manager.GRANTED))) {
			islandRate = AuditCageHelper.getIslndaRate(do2, newAuditCage);
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.ISLNDA, islandRate);
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
			
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isPercentageRateAvailable(Do2 do2) {
		List<Allowance> allowances = new ArrayList<Allowance>();
		allowances = Utils.getApplicableAllowanceRates(do2
				.getDescription(), do2.getColumnB(), do2.getFromDate(), do2.getFromDate());
		if (allowances.size() > 0 && allowances.get(0).getRatePercentage() > 0.0){
			return true;
		}
		return false;
	} 
}