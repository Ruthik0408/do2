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
 * @author IT & SDC , Secunderabad
 * 
 */
@RestController
public class Sfasff extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	
	Rank rank;
	static public String RATE_NOT_AVAILABLE = "AU:Rate not available in the Allowance Rate table.\n ";
	static public String SERVICE_DETAIL_UNAVAILABLE = "AU:Employee Service Detail is not available.\n";
	static public String NOT_VALID_UNIT= "AU:Admissible to only personnel serving in Special Group i.e 4 Vikas.\n";
	private StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		if(do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		boolean validity = true;
		// Authority HQRS letter No.AT/I/3158/Vol-XII/OA No.454/2011 dated 31.10.2012
		if(do2.getId()== newDo2.getId()) {
			if(do2.getUnitCode()!=null && do2.getUnit().getUnitName().toUpperCase().trim().contains("4 VIKAS")) {
				;
			} else {
				validity = false;
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(NOT_VALID_UNIT);
			}
		}
		if (!isRankAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(SERVICE_DETAIL_UNAVAILABLE);
			
		}
		if (!isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_AVAILABLE);
			
		}
		if(validity){
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate()); 
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE"); 
		AuditCageHelper.copyCurrentAllowances(newAuditCage,	baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			AuditCageHelper.removeAllowances(Arrays.asList(new String[]{ Do2Manager.SFASFF }), newAuditCage);
		} 
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
 			List<Allowance> allowances  = Utils.getApplicableAllowanceRates(do2.getDescription(),
					do2.getFromDate(), do2.getFromDate(), rank);
		//	System.out.println("allowance size   "+allowances.size());
		if(allowances.size() >0){
					AuditCageHelper.updateAuditCageAllowance(newAuditCage,
						Do2Manager.SFASFF, allowances.get(0).getRateAmount());
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
	
	public boolean isRankAvailable(Do2 do2) {
		return rank !=null;		
		
	}
	
	public boolean isRateAvailable(Do2 do2) {		
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getFromDate(),
				do2.getFromDate(), rank);
		System.out.println("allowance size   "+allowances.size());
		return allowances.size() >0;
	}
}