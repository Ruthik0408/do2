package itsdc.dolphin.do2.accountingcage.independent;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
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
// ALLOTMENT OF JC NUMBER
@RestController
public class Jcno extends AccountingCageBase implements java.io.Serializable {
	
	static final String ALREADY_WITH_JCNO = "AU:PBOR is already having JCNO - can't change. \n";
	static final String NOT_A_JCO_RANK = "AU:Not a JCO. \n";
	static final String NO_PROMOT_DO2 = "AU:No Promotion DoII/Reference DOII is incorrect. \n";
	static final String INVALID_COL7A = "VL:Col. 7A should contain JC/TJ Number. \n";
	static final String WRONG_CHECKDIGIT = "VL:Check digit of JCNO is incorrect. \n";
	
 
	StringBuffer buffer = new StringBuffer();
	Employee employee;
	Rank rank;

	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (employee.getRegularTaFlag().equals("T") && do2.getDescription().equals("JCNO")){
			buffer.append("PBOR is TA personnel. Description should be 'TJNO'");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
		}
		if (employee.getRegularTaFlag().equals("R") && do2.getDescription().equals("TJNO")){
			buffer.append("PBOR belongs to Regular Army. Description should be 'JCNO'");
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
		}
		if (!(isJcno())) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(ALREADY_WITH_JCNO);
			validity = false;
		}
		
		if (!(isValid7a(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_COL7A);
			validity = false;
		}
		
		if (!(isValidCheckDigit(do2))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(WRONG_CHECKDIGIT);
			validity = false;
		}

		if (!(isPromot(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_PROMOT_DO2);
			validity = false;
		}

		if (!(isValidRank())) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NOT_A_JCO_RANK);
			validity = false;
		}

		if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null	&& !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
			}
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
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
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		updateEmployee(do2);
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {// not here
		/*employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());		
		employee.setArmyNo(employee.getOldArmyNo());
		employee.setCheckDigit(employee.getOldArmyNoCheckDigit());
		employee.setOldArmyNo(null);
		employee.setOldArmyNoCheckDigit(null);
		Utils.getEntityManager().persist(employee);*/
		return Do2Manager.SUCCESS;
	}
	
	// ****************************************************************************************
	public boolean isJcno() {
		return !(employee.getArmyNo().startsWith("JC") || employee.getArmyNo().startsWith("TJ"));
	}
	
	public boolean isValid7a(Do2 do2) {
		if (do2.getDescription().equals("JCNO")){
			if ((do2.getColumnA() != null && 
			    do2.getColumnA().length() == 9)	&& do2.getColumnA().startsWith("JC"))
				return true;
		}
		if (do2.getDescription().equals("TJNO")){
			if ((do2.getColumnA() != null && 
			    do2.getColumnA().length() == 9)	&& do2.getColumnA().startsWith("TJ"))
				return true;
		}
		return false; 
	}
	
	public boolean isPromot(Do2 do2) {
		List <Do2> promotDo2List = null;
		promotDo2List = Utils.getReferenceDo2List(do2);
		for(Do2 promotDo2 : promotDo2List){
			if ((promotDo2.getDescription().equals("PROMOT") || promotDo2.getDescription().equals("MSTJCO"))
					&& (promotDo2.getColumnA().equals("NBSUB") || promotDo2.getColumnA().equals("NBRIS") || promotDo2.getColumnA().equals("SPNBSUB")
							|| (promotDo2.getColumnA().equals("SBSUB") && promotDo2.getColumnC().equals("OUTOFTURN"))  
							|| (promotDo2.getColumnA().equals("SPSUB") && promotDo2.getColumnC().equals("OUTOFTURN")))) {
				return true;
			} 
			if (promotDo2.getDescription().trim().equals("ENROLL") && promotDo2.getColumnC().startsWith("NSUB") && do2.getFromDate().after(promotDo2.getFromDate())) {    // added on 24.01.2025 - AOC irla
				//	if (do2.getEmployee().getGroupCategory()!=null && do2.getEmployee().getGroupCategory().getSoldierCategory().equals("RT")) {
				//		System.out.println("555555555555555555555");
						return true;
					//}
				}
		}		
		return false;
	}

	public boolean isValidRank() {
		int rankCode = 0;
		if (rank!=null){
			rankCode = Integer.parseInt(rank.getRankCode());
		}
		if (rankCode >=50)
			return true;
		return false;
		
	}

	
	public boolean isValidCheckDigit(Do2 do2) {
		char do2Cdgt = do2.getColumnA().charAt(8);
		char calCdgt = Utils.getCheckDigit(do2.getColumnA().substring(0, 8));
		if (!(calCdgt == do2Cdgt))
			return false;
		return true;
	}


	public void updateEmployee(Do2 do2) {
		employee.setOldArmyNo(employee.getArmyNo());
		employee.setOldArmyNoCheckDigit(employee.getCheckDigit());
		employee.setArmyNo(do2.getColumnA().substring(0, 8));
		employee.setCheckDigit(do2.getColumnA().charAt(8)+"");
		empRepo.save(employee);
		}

}
