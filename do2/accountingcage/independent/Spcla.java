package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 

// SPECIAL CLOTHING ALLOWANCE
// Admissible only to those appointed on intelligence duties and certificate to
// that extent should be enclosed.
@RestController
public class Spcla extends AccountingCageBase implements java.io.Serializable {
	static final String RATE_NOT_FOUND = "AU:Rate not available in Allowance Rate Table";
	static final String IS_A_RECRUIT   = "AU:PBOR is a recruit";
	static final String NOT_ADMISSIBLE = "AU:Admissible only to PBORs of INT Corps. \n";
	Employee employee;
	Rank rank;
	StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		/*The following check is commented on 06/09/2013 -PAO ASC(South) Observation
		Auth : GOI MOD lr No.20451/6/MI 20[B]/256/II/D[GS III ] dated 1.2.1985 
		Admissible to personnel put on intelligence duties. Admissible during leave period also
		OC has to certify that the PBOR for whom claimed are put on intelligence duties
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_INT)
				|| Utils.getPao().getPaoCode().equals(Do2Manager.PAO_EME) ) {
			;
		}else{
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(NOT_ADMISSIBLE);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
			if (do2.getId()==0) {
			 	Utils.getEntityManager().persist(do2);
			} else {
			 	Utils.getEntityManager().merge(do2);
			}
			return false;
		}*/
		
		if (!(isValidRankForSpcla(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(IS_A_RECRUIT);
			validity = false;
		}

		if (!(isValidRateOfSpcla(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_FOUND);
			validity = false;
		}
		if (validity) {
			do2.setStatus(Do2Manager.VALID);
			if(do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			}else{
				if(!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		double spclaAmount = 0.0;
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
				.getDescription(), do2.getFromDate(), do2.getToDate());

		for (Allowance alw : alwList) {
			spclaAmount += (DateManager.getNoOfMonths(alw.getFromDate(), alw
					.getToDate()) * alw.getRateAmount());
		}

		Utils.addArrears(do2, Math.round(spclaAmount));

		return Do2Manager.SUCCESS;
	}

	// ****************************************************************************************

	// Reading Employee Master to check if Recruit
	public boolean isValidRankForSpcla(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if ( rank==null || rank.getRankCode().equals(Do2Manager.RECRUIT))
			return false;
		return true;

	}

	// Reading Allowance Rate To Get Rate Of SPCLA
	public boolean isValidRateOfSpcla(Do2 do2) {
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
				.getDescription(), do2.getFromDate(), do2.getToDate());
		if (alwList.size() > 0 && alwList.get(0).getRateAmount() > 0)
			return true;
		else
			return false;
	}
}
