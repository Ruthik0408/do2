package itsdc.dolphin.do2.accountingcage.independent;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Conser extends AccountingCageBase implements java.io.Serializable {

	Rank rank;
	Employee employee;
	StringBuffer buffer = new StringBuffer();

//	static final String INVALID_PERIOD = "VL:CONSER Period not in complete years.\n";
	static final String INVALID_PERIOD = "VL:CONSER Period Beyond Permissible Age(55)/Service(15).\n";
	static final String INVALID_CONSER = "AU:CONSER exceeded beyond maximum service specified for rank.\n";
	static final String ENROLL_DATE_NULL = "VL:Enrollment Date Is Blank In Employee Table.\n";
	static final String BIRTH_DATE_NULL = "VL:Birth Date Is Blank In Employee Table.\n";
	static final String RANK_BLANK = "VL:Both Substantive And Acting Rank Are Blank In Employee Table.\n";
	static final String RANK_DATES_BLANK = "VL:Both Substantive And Acting Rank Dates Are Blank In Employee Table.\n";

	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getDo2Date(),"ALLCE");
		boolean validity = true;
// Validations modified as requested by PAO/DSC vide letter No.EDP/CAN/001/CORR-VII dated 13.08.2024 (received through email).
		if (employee.getEnrolmentDate()==null) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(ENROLL_DATE_NULL);
			validity = false;		 
		}
		
		if (employee.getBirthDate()==null) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(BIRTH_DATE_NULL);
			validity = false;		 
		}
		
		if (employee.getSubstantiveRank()==null && employee.getActingRank()==null) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(RANK_BLANK);
			validity = false;
		}
		
		if (employee.getSubstantiveRankDate()==null && employee.getActingRankDate()==null) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(RANK_DATES_BLANK);
			validity = false;
		}
		
		if (!isValidYears(do2, employee)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_PERIOD);
			validity = false;
		}
		
		if (!(isValidConser(do2, employee))) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_CONSER);
			validity = false;
		}
		
		do2.setReason(buffer.toString());
		
		if (validity) {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}

		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getDo2Date(),"ALLCE");
		int colorServ = Integer.parseInt(employee.getTermsColourService());
		if (DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()) == 24.0) {
			int newColorServ = colorServ + 2;
			employee.setTermsColourService(Integer.toString(newColorServ));
		} else if (DateManager
				.getNoOfMonths(do2.getFromDate(), do2.getToDate()) == 12.0) {
			int newColorServ = colorServ + 1;
			employee.setTermsColourService(Integer.toString(newColorServ));
		}
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		rank = Utils.getPborRank(currentDo2.getArmyNo(), refDo2.getDo2Date(),"ALLCE");		
		employee.setTermsColourService(rank.getColorService());
		return Do2Manager.SUCCESS;
	}

	// ************************************************************************
	public boolean isValidYears(Do2 do2, Employee emp) {
	//	if ((DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()) == 24.0)	|| (DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()) == 12.0))
		
		if (emp.getBirthDate()!=null && DateManager.getNoOfMonths(emp.getBirthDate(),do2.getToDate())>660) {
			return false;
		}
		
		if (emp.getEnrolmentDate()!=null && DateManager.getNoOfMonths(emp.getEnrolmentDate(),do2.getToDate())>180) {
			return false;
		}
	
		return true;	  
	}

	public boolean isValidConser(Do2 do2, Employee emp) {
	/*	boolean conserValidity = false;
		int colorServ = Integer.parseInt(employee.getTermsColourService());
		int maxServ = Integer.parseInt(rank.getMaximumService());

		if (DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()) == 24.0) {
			int newColorServ = colorServ + 2;
			if (newColorServ <= maxServ) {
				conserValidity = true;
			} else {
				conserValidity = false;
			}
		} else if (DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate()) == 12.0) {
			int newColorServ = colorServ + 1;
			if (newColorServ <= maxServ) {
				conserValidity = true;
			} else {
				conserValidity = false;
			}
		} else {
			conserValidity = false;
		}
		return conserValidity; */
		
		Rank rank=null;
		rank=emp.getSubstantiveRank();
		if (rank==null) {
			rank=emp.getActingRank();
		}
		
		if (rank==null) {
			return false;
		}
		double noOfMonths=DateManager.getNoOfMonths(emp.getBirthDate(), do2.getToDate());
		if (!rank.getRankName().trim().equalsIgnoreCase("SUBMAJ".trim())) {
			if (noOfMonths>684) {
				return false;
			}
		} else {
			if (emp.getSubstantiveRankDate()!=null) {
				noOfMonths=0;
				noOfMonths=DateManager.getNoOfMonths(emp.getSubstantiveRankDate(), do2.getToDate());
				if (noOfMonths>48) {
					return false;		
				}
			} else if (emp.getActingRankDate()!=null) {
				noOfMonths=0;
				noOfMonths=DateManager.getNoOfMonths(emp.getActingRankDate(), do2.getToDate());
				if (noOfMonths>48) {
					return false;		
				}
			}
		}
		return true;
	}
}