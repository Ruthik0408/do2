package itsdc.dolphin.do2.auditcage;

import java.sql.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/*
 * 
 * author : IT & SDC (Secunderabad)
 * 
 */
@RestController
public class Lra extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
Employee employee;	
static String IS_A_RECRUIT = "AU:PBOR is a Recruit.\n";
public static String HONY_RANK = "AU:Not admissible to Hony Ranks.\n";
public static String DUPLICATE_DO2 = "VL: Duplicate DO2.\n";
StringBuffer buffer = new StringBuffer();
Rank rank;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rejects if pmhah is published for ta personnel appt category D and O
		if(employee.getRegularTaFlag() !=null && employee.getRegularTaFlag().equals("T")
				&& employee.getTaAppointmentCategory() !=null && 
				(employee.getTaAppointmentCategory().equals("D") ||
				employee.getTaAppointmentCategory().equals("O"))){
			buffer.append("LRA Not Admissible for TA Personnel: Departmental and Others");
	    	do2.setStatus(Do2Manager.INVALID);
	    	validity = false;		
		}
	   
		if ((isRecruit(do2))) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(IS_A_RECRUIT);
		} else if (isHonyRank(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(HONY_RANK);
		}

		if (isDuplicate(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(DUPLICATE_DO2);
		}
		
		if (validity){
			do2.setStatus(Do2Manager.VALID);	
		}
		
		do2.setReason(buffer.toString());
		
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		if (newAuditCage.getLra()>0 || baseAuditCage.getLra()>0) {
			   newAuditCage.setLra((int) Math.round(AuditCageHelper.getLraRate(newAuditCage)));
		} else if (newAuditCage.getLra()==0 && baseAuditCage.getLra()==0 && newAuditCage.getFromDate().equals(baseAuditCage.getFromDate()) 
					&& newAuditCage.getFromDate().equals(newAuditCage.getEmployee().getEnrolmentDate())  
							&& newAuditCage.getEmployee().getEnrolmentDate().equals(newAuditCage.getEmployee().getNbsubDate())) {
						   newAuditCage.setLra((int) Math.round(AuditCageHelper.getLraRate(newAuditCage)));
		}
		
		return newAuditCage;
	}
	public boolean isRecruit(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		return employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT);	
	}
	
	public boolean isHonyRank(Do2 do2) {

		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
		if (rank!=null && rank.getRankCode().equals(Do2Manager.HONYLT)) {
			return true;
		}
		
		if (rank!=null && rank.getRankCode().equals(Do2Manager.HONYCP)) {
			return true;
		}
		
		return false;
		
	}


	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		// blank
		return Do2Manager.SUCCESS;
	}
	
	public boolean isDuplicate(Do2 do2) {
		List<Do2> duplicateDo2List = do2Repo.findByDescFromDateEqualIdNotEqual(employee.getId(),"V","LRA",do2.getFromDate(),do2.getId());
				
	/*			Utils.getEntityManager().createQuery(
				" SELECT d FROM Do2 d WHERE d.id!=:newDo2Id AND d.employee=:employee  " +
				" and d.description='LRA' and d.fromDate=:fromDate and status='V' ")
		.setParameter("employee", employee)
		.setParameter("newDo2Id", do2.getId()) 
		.setParameter("fromDate", do2.getFromDate())
		.getResultList();*/
		
		if (duplicateDo2List!=null && duplicateDo2List.size()>0) {
			return true;
		} else {
			return false;
		}
	}

}
