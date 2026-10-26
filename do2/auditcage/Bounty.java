package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Bounty extends AuditCageBase implements Do2AuditCageProcessor,
		java.io.Serializable {

	private boolean validity = true;
	StringBuffer buffer = new StringBuffer();

	Rank rank;
	Employee employee;
	List<Allowance> allowances;

	static public final String NOT_ADMISSIBLE_RECRUIT = "AU:Not admissible to Recruit. \n";
	
	@Override
	public boolean isValid(Do2 do2) {

		if (do2.getColumnA().trim().equals("CEASED")) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}

		if (!isRecruit(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NOT_ADMISSIBLE_RECRUIT);
			validity = false;
		}

		if (validity) {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	public boolean isRecruit(Do2 do2) {
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		return rank != null && !rank.getRankCode().equals(Do2Manager.RECRUIT);
	}

	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		int bountyAllow = 0;
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if ( rank != null  && (Integer.parseInt(rank.getRankCode()) < Do2Manager.JCO_RANK_CODE)){
				 bountyAllow = 175;
			}else {
				 bountyAllow = 300;
			}
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.BOUNTY,bountyAllow);
			
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
}
