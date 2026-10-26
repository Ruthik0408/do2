package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */

@RestController
public class Rep extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;

	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n";
	private StringBuffer buffer = new StringBuffer();
	Employee employee;
	Do2 refDo2;
    Rank rank;
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
        rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
	/*	if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
			do2.setStatus(Do2Manager.INVALID);

		} else {*/
			do2.setStatus(Do2Manager.VALID);			
			if (do2.getPunishment().equals("CM") && Integer.parseInt(rank.getRankCode()) < 50 &&
				employee.getCurrentGspayCode() != null && employee.getCurrentGspayCode()!= "0") {
				if (isdo2GenerationRequired(do2)) {
					  String columnA= getGsPayGrade(do2);
					  if (columnA != null){
				 		  AuditCageHelper.generateForGspayDo2(do2, columnA);
					  }
				}
			}
		
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;

	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2,Do2Manager.ESD_PUNISHMENT+"");
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRefDo2(Do2 do2) {
		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		for (Do2 refDo2 : refDo2List) {
			if (refDo2.getDescription().equals(Do2Manager.AWL) || 
				refDo2.getDescription().equals(Do2Manager.OSL)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		return Do2Manager.SUCCESS;
	}
	
	public String getGsPayGrade(Do2 do2) {
		String columnA = null;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (employee.getCurrentGspayCode().equals("3")) {
			columnA = "THIRD";
		} else if (employee.getCurrentGspayCode().equals("2")) {
			columnA = "SECOND";
		} else if (employee.getCurrentGspayCode().equals("1")) {
			columnA = "FIRST";
		}
		return columnA ;

	}
	
	public boolean isdo2GenerationRequired(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> do2List = do2Repo.findByEmpIdRefDo2NoDescription(employee.getId(), Utils.getReferenceDo2No(do2),
																										"FORGSP");
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT d FROM Do2 d WHERE d.employee=:employee AND d.description=:description and d.refDo2No=:refDo2No")
				.setParameter("employee", employee)
				.setParameter("description", "FORGSP")
				.setParameter("refDo2No", Utils.getReferenceDo2No(do2)).getResultList();*/

		return do2List.size() == 0;
	}
}
