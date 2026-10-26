package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.models.AdjustmentCb;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;
 

/**
 * Authority :
 * 
 * Author : A J Noronha & T Vanaja
 */

// OUTFIT ALLOWANCE
@RestController
public class Outfit extends AccountingCageBase implements java.io.Serializable {

	Employee employee;
	Rank rank;
	StringBuffer buffer = new StringBuffer();
	 

	static final String NOT_VALID_JCO = "AU:Not A Nbsub or HonyLt as on DO2 fromdate.\n";
	static final String RATE_NOT_FOUND = "AU:No Rate Available.\n";
	static final String INVALID_MODULE = "VL:To be fed through Cont Bills prior to 01.06.2009.\n";
	static final String OUTFIT_ADJUSTED_EARLIER = "DU:Outfit Allowance already Adjusted.\n";
	static final String ADJUSTED_IN_ADJ_VR = "AU:Adjusted Earlier Thru Adj Vr.\n";
	static final String FROMDATE_BEFORE_PROMOTION = "AU:Outfit grant date before promotion date \n";
	static final String RATE_ZERO = "VL:Rate cannot be zero \n";
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		

		if (isAlreadyAdjThruAdjVr(do2)) {
			buffer.append(ADJUSTED_IN_ADJ_VR);
			validity = false;
		}

		if (!isValidModule(do2)) {
	 		buffer.append(INVALID_MODULE);
			validity = false;
		}
 
		if (!isFirstClaim(do2)) {
			buffer.append(OUTFIT_ADJUSTED_EARLIER);
			validity = false;
		}
		
		if (do2.getRate()<=0) {
			buffer.append(RATE_ZERO);
			validity=false;
		}
		

		if (!(isValidRefDo2ForOutfit(do2))) {
			buffer.append(NOT_VALID_JCO);
			validity = false;
		} 
		
		if (validity) {
			if (!isValidRateOfOutfit(do2)) {
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(RATE_NOT_FOUND);
				validity = false;
			}
		}
		
		
		if (!validity) {
			if ((do2.getStatus() == null)
					|| (do2.getStatus() != null && !do2.getStatus().equals(
							Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
		 	}
		} else {
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

	// ************************************************************************************

	@Override
	public String process(Do2 do2) {
		Double amount=0.0;
		Pao pao = null;
		
		
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (rank.getRankName().equals("HONYLT")) {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
				pao = Utils.getPao();
			} else {
				pao = null; 
			}
		}
		
		
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
				.getDescription(),do2.getFromDate(), do2.getFromDate(), rank,pao);
			amount = alwList.get(0).getRateAmount();
		 
		if (do2.getRate()<amount) {
			amount = do2.getRate();
		}
	
		Utils.addArrears(do2, amount);
		return Do2Manager.SUCCESS;
	}

	public boolean isAlreadyAdjThruAdjVr(Do2 do2) {
		List<AdjustmentCb> adjList = new ArrayList<AdjustmentCb>();
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		adjList = adjCbRepo.findByEmployeeIdPayCodeAndFromDate(emp.getId(), do2.getDescription().trim().toUpperCase(), do2.getFromDate());
				
		/*		em.createQuery(
				"SELECT adj FROM AdjustmentCb adj WHERE "
						+ " adj.payCode.payCode =:pc and "
						+ " adj.fromDate =:fromdate and adj.armyNo=:aano ")
				.setParameter("aano", do2.getArmyNo()).setParameter("pc",
						do2.getDescription().trim().toUpperCase())
				.setParameter("fromdate", do2.getFromDate()).getResultList();*/
		if (adjList.size() == 0)
			return false;
		else
			return true;
	}

	// Date Of Effect Check :
	public boolean isValidModule(Do2 do2) {
		
		Date cutOffDate = new Date();
		
		try {
			cutOffDate = DateManager.getDate("2009-06-01");
		} catch (Exception e) {
			Utils.handleException(e);
		}
	 	return (!do2.getFromDate().before(cutOffDate));
	}

	// Reading Do2 Master To Check If OUTFIT was adjusted earlier
	public boolean isFirstClaim(Do2 do2) {
		List<Do2> do2List = new ArrayList<Do2>();
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		do2List = do2Repo.findByEmployeeIdAndDescriptionAndStatusAndColumnA(emp.getId(),do2.getDescription(),Do2Manager.VALID,do2.getColumnA().trim().toUpperCase());
				
		/*		em.createQuery(
				"SELECT d FROM Do2 d WHERE "
						+ "d.armyNo=:ano AND d.description=:desc AND "
						+ "d.columnA=:col7a AND d.status=:status")
				.setParameter("ano", do2.getArmyNo()).setParameter("desc",
						do2.getDescription()).setParameter("col7a",
						do2.getColumnA().trim().toUpperCase()).setParameter(
						"status", Do2Manager.VALID).getResultList();*/
		if (do2List.size() == 0)
			return true;
		else
			return false;
	}

	// Read Employee Table To get Rank of Army Number
	public boolean isValidRefDo2ForOutfit(Do2 do2) {
		String rankName = " ";
		
		
		if (rank==null) {
			return false;
		}
		
		rankName = rank.getRankName();
		
		
		/*if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_ACR)) {
			rankName = rank.getAhmedNagarRankName();
		} else {
			 rankName = rank.getRankName();
		}*/

		if (Do2Manager.outfitValidRanksList.contains(rankName)) {
			return true;
		}
		
		return false;
		
 	/*	List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		
		for (Do2 d : refDo2List) {
			if ((d.getDescription().equals("PROMOT") || d.getDescription().equals("MSTJCO"))
					&& d.getColumnA().equals("NBSUB")|| (d.getDescription().equals("HCOMMN") && d.getColumnA().equals("HONYLT"))) {

				return true;
			}

		}
		return false;
*/	}

	// Reading Allowance Rate To Get Rate Of Outfit
	public boolean isValidRateOfOutfit(Do2 do2) {
		Pao pao = null;
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (rank.getRankName().equals("HONYLT")) {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
				pao = Utils.getPao();
			} else {
				pao = null; 
			}
		}
		
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2
				.getDescription(), do2.getFromDate(), do2.getFromDate(), rank, pao);
		if (alwList.size() > 0 && alwList.get(0).getRateAmount() > 0) {
			return true;
		} else
			return false;

	}
	
	public boolean isValidFromDate(Do2 do2) {
		String rankName = "";
		Date rankDate = null;
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_ACR)) {
			rankName = rank.getAhmedNagarRankName();
		} else {
			rankName = rank.getRankName();
		}
		
		if ((rankName.equals("NBSUB") || rankName.equals("NBRIS"))) {
			rankDate = employee.getNbsubDate();
		} else {
			rankDate = employee.getHonyLieuDate();
		}
		
		if (rankDate==null) {
			return false;
		}
		
		if (!do2.getFromDate().before(rankDate)) {
			return true;
		}
			 
		return false;
	}
}
