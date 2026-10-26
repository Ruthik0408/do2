package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.Utils;
  
@RestController
public class Actga extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	static public final String RATE_NOT_AVAILABLE = "AU:Rate not available in the Allowance Rate table. \n";
	static public final String ON_LEAVE = "AU:Not admissible as the JCO is on leave/temporary duty or POSIN/POSOUT falling in between. \n";
	static public final String NOT_JCO = "AU:Not a JCO. \n";
	 
	Employee employee;
	Rank rank;
	private StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		if (!isJco(do2)) {
			validity = false;
			buffer.append(NOT_JCO);
		}
		if (isOnLeave(do2)) {
			validity = false;
			buffer.append(ON_LEAVE);
		}
		if (!isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(RATE_NOT_AVAILABLE);
		}  
		
		if(!validity) {
			if (do2.getStatus()!=null && !do2.getStatus().equals(Do2Manager.PENDING)) {
				do2.setStatus(Do2Manager.INVALID);
			}	
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
	 
		do2.setReason(buffer.toString());

		do2Repo.save(do2);
		
		return validity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage,
				baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			List<String> alwToStop = Arrays.asList(new String[] { Do2Manager.ACTGA });
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		} else if ((do2.getColumnA().equals(Do2Manager.GRANTED))) {
			List<Allowance> allowances = new ArrayList<Allowance>();
			allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(),
					do2.getFromDate());
		
			for (Allowance allow : allowances) {
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.ACTGA, allow.getRateAmount());
			}
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
	
	public boolean isJco(Do2 do2){
		employee = do2.getEmployee();
		rank = Utils.getPborRank(employee.getArmyNo(), do2.getFromDate(), "ALLCE");
		if ((Integer.valueOf(rank.getRankCode()) >= 50) && (Integer.valueOf(rank.getRankCode()) <= 70)) {
			return true;
		}
		return false;
	}
	
	public boolean isOnLeave(Do2 do2) {
		List<String> nonAllowanceList = Arrays.asList(new String[] { "OSL",
				"AWL", "HOSPTL", "POSIN", "POSOUT", "ADVAL", "BAL", "AL", "EXTNLV",
				"TYOJ", "TYRJ", "ACCMAL", "CL", "DAILY", "LDAILY", "EXTNAL",
				"EXTNCL", "EXTNSL", "PAL", "RL", "SL", "RTYRJ", "RTYOJ", "RDAILY" });
		
		List<Do2> do2List = Utils.getValidDo2s(do2.getArmyNo(), do2.getFromDate(), do2.getToDate(), do2);
		
		for (Do2 adjustedDo2 : do2List) {
			if (nonAllowanceList.contains(adjustedDo2.getDescription())) {
				return true;
			}
		}
		return false;
	}

	public boolean isRateAvailable(Do2 do2){
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(),
				do2.getFromDate());
		if (allowances.size() > 0 && allowances.get(0).getRateAmount() > 0.0) {
			return true;
		}
		return false;
	}
}