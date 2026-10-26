package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;
 

/*
 * Authority : 
 * 
 * Author : A J Noronha & T Vanaja
 */
@RestController
public class Mca extends AccountingCageBase implements java.io.Serializable {
	static final String INVALID_RANK = "AU:Admissible only to Recruit / Sepoy.\n";
	static final String LESS_THAN_SIX_MONTHS = "AU:Less than 6 months of service.\n";
	static final String RATE_NOT_FOUND = "AU:Rate not available in Allowance Table.\n";
	static final String DO2_ADJUSTED_EARLIER = "DU:MCA Adjusted Earlier.\n";
	
	StringBuffer buffer = new StringBuffer();
	 
	Double mcaAmount;
	Employee employee;
	Rank rank;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
		if (rank == null) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(Do2Manager.SERVICE_DETAIL_MISSING);
			return false;
		}
	
		if (!(isValidRankForMca(do2))) {
			buffer.append(INVALID_RANK);
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
		}
		if (!(isMinimumService(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(LESS_THAN_SIX_MONTHS);
			validity = false;
		}

		if (!(isMcaAlreadyAdjusted(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(DO2_ADJUSTED_EARLIER);
			validity = false;
		}

		if (!(isValidRateOfMca(do2))) {
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
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");

		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
				.getDescription(), do2.getFromDate(), do2.getFromDate(), rank);

		Utils.addArrears(do2, alwList.get(0).getRateAmount());

		return Do2Manager.SUCCESS;
	}

	// ****************************************************************************************

	// Reading Do2 Master To Check If MCA was adjusted earlier
	public boolean isMcaAlreadyAdjusted(Do2 do2) {
		List<Do2> do2List = new ArrayList<Do2>();
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		do2List = do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(emp.getId(), do2.getDescription(), Do2Manager.VALID);
				
		/*		em
				.createQuery(
						"SELECT d FROM Do2 d WHERE "
								+ " d.armyNo=:dano AND d.description=:ddesn AND d.status=:dstatus")
				.setParameter("dano", do2.getArmyNo()).setParameter("ddesn",
						do2.getDescription()).setParameter("dstatus",
						Do2Manager.VALID).getResultList();*/
		System.out.println("size of do2list is; " + do2List.size());
		if (do2List.size() <= 1) { // size <=1 is checked because the curr rec
			// is 1st being written to do2 then its
			// checked
			return true;
		} else {
			return false;
		}
	}

	// To Get Rank Code : check if Recruit/Sepoy
	public boolean isValidRankForMca(Do2 do2) {
		if (rank.getRankCode().equals("20") || rank.getRankCode().equals("10"))
			return true;
		else
			return false;
	}

	// Reading Allowance Rate To Get Rate Of MCA
	public boolean isValidRateOfMca(Do2 do2) {
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
				.getDescription(), do2.getFromDate(), do2.getFromDate(), rank);
		if (alwList.size() > 0 && alwList.get(0).getRateAmount() > 0)
			return true;
		else
			return false;
	}

	public boolean isMinimumService(Do2 do2) {
		if (DateManager.getNoOfDays(employee.getEnrolmentDate(), do2
				.getFromDate()) >= Do2Manager.MCA_MINIMUM_SERVICE)
			return true;
		else
			return false;
	}

}
