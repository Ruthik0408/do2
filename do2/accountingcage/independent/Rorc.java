package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.do2.AccountingCageBase;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;
 

/**
 * Authority :
 * 
 * Author : A J Noronha & T Vanaja
 */

// REIMBURSEMENT OF RESERVATION/SLEEPER CHARGE
@RestController
public class Rorc extends AccountingCageBase implements java.io.Serializable {

	static final String RECRUIT_STATUS = "AU:Not Admissible To Recruits.\n";
	static final String INVALID_REFERENCE = "AU:Incorrect Reference Do2.\n";
	static public String INVALID_RATE = "VL:Rate column should not be blank.\n";
	static public String INVALID_FROMDT = "AU:RORC From Date not during period of AL/Temp Duty.\n";
	static public String REF_NOT_OF_AL_TD = "AU:Ref DoII is not of AL/Temporary Duty.\n";
	static public String RORC_ALREADY_ADJUSTED = "DU:Already adj twice(to&fro) within this leave/TD period.\n";

	 
	Employee employee;
	Rank rank;
	StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");

		if (!(isValidRank(do2))) {
			validity = false;
			buffer.append(RECRUIT_STATUS);
		}

		if (!(isValidRate(do2))) {
			validity = false;
			buffer.append(INVALID_RATE);
		}

		if (!(isRefDo2DescValid(do2))) {
			validity = false;
			buffer.append(REF_NOT_OF_AL_TD);
		}

		if (!(isValidFromDate(do2))) {
			validity = false;
			buffer.append(INVALID_FROMDT);
		}

		if (isRorcAdjTwiceWithinLeaveTdPeriod(do2)) {
			validity = false;
			buffer.append(RORC_ALREADY_ADJUSTED);
		}
		if(!validity){
			do2.setStatus(Do2Manager.INVALID);
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

	@Override
	public String process(Do2 do2) {
		Utils.addArrears(do2, Math.round(do2.getRate()));
		return Do2Manager.SUCCESS;
	}

	// ****************************************************************************************

	public boolean isValidRank(Do2 do2) {
		return (rank.getRankCode().compareTo(Do2Manager.RECRUIT) > 0);
	}

	private boolean isValidRate(Do2 do2) {
		if ((do2.getRate() == null) || (do2.getRate() == 0)) {
			return false;
		} else {
			return true;
		}
	}

	public boolean isRefDo2DescValid(Do2 do2) {
		Do2 refDo2 = null;
		final String[] abfValidWith = { "AL", "PAL", "BAL", "TYOJ", "TYRJ" };
		final List<String> leaveTd = Arrays.asList(abfValidWith);
		refDo2 = Utils.getReferenceDo2(do2, leaveTd);

		if (!((refDo2.getDescription().equals("AL")
				|| refDo2.getDescription().equals("TYOJ")
				|| refDo2.getDescription().equals("TYRJ")
				|| refDo2.getDescription().equals("PAL") || refDo2
				.getDescription().equals("BAL")))) {

			return false;
		} else {
			return true;
		}
	}

	public boolean isValidFromDate(Do2 do2) {
		Do2 refDo2 = null;
		final String[] abfValidWith = { "AL", "PAL", "BAL", "TYOJ", "TYRJ" };
		final List<String> leaveTd = Arrays.asList(abfValidWith);
		refDo2 = Utils.getReferenceDo2(do2, leaveTd);

		if (!((do2.getFromDate().equals(refDo2.getFromDate()) || (do2
				.getFromDate().after(refDo2.getFromDate()) && do2.getFromDate()
				.before(refDo2.getToDate())))
				|| do2.getFromDate().equals(refDo2.getToDate()) || (do2
				.getFromDate().before(refDo2.getToDate()) && do2.getFromDate()
				.after(refDo2.getFromDate())))) {

			return false;
		} else {
			return true;
		}
	}

	public boolean isRorcAdjTwiceWithinLeaveTdPeriod(Do2 do2) {
		Do2 refDo2 = null;
		final String[] abfValidWith = { "AL", "PAL", "BAL", "TYOJ", "TYRJ" };
		final List<String> leaveTd = Arrays.asList(abfValidWith);
		refDo2 = Utils.getReferenceDo2(do2, leaveTd);

		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> rorcDo2 = do2Repo.findByFromDateBetweenStatusAndDescription(emp.getId(),"RORC",Do2Manager.VALID,refDo2.getFromDate(),refDo2.getToDate());
				
	/*			Utils.getEntityManager().createQuery(
				"select rorcdo from Do2 rorcdo where "
						+ " rorcdo.description = 'RORC' and "
						+ " rorcdo.status = :status and "
						+ " rorcdo.fromDate between :fromdate and :todate and "
						+ " rorcdo.armyNo =:armyno and rorcdo.checkDigit=:cd ")
				.setParameter("armyno", do2.getArmyNo()).setParameter("cd",
						do2.getCheckDigit()).setParameter("fromdate",
						refDo2.getFromDate()).setParameter("todate",
						refDo2.getToDate()).setParameter("status",
						Do2Manager.VALID).getResultList();*/

		System.out.println("abfdo2s within Leave/TD period are :"
				+ rorcDo2.size());
		if (rorcDo2.size() < 2) {
			return false;
		} else {
			return true;
		}
	}
}
