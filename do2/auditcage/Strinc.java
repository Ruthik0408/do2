package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.SoldierGroup;

import itsdc.dolphin.utils.Utils;
import itsdc.dolphin.utils.DateManager;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;

 

/**
 * 
 * @author ITSDC, Sec'bad.
 * 
 */

@RestController
public class Strinc extends AuditCageBase implements Do2AuditCageProcessor,
		 java.io.Serializable {

	Employee emp;
	Rank rank;

	static public final String INVALID_STRINC = "AU:PBOR is not married as per Employee Service Details.\n";
	static public final String MORE_THAN_TWO = "AU:More than two surviving children.\n";
	static public final String INVALID_START = "AU:From date should be from first day of the month.\n";
	static public final String STRINC_ALREADY_DRAWING = "PBOR is already drawing Sterilisation increment. \n";
	static public final String STILL_A_RECRUIT = "Pbor is still a recruit \n.";
	
	public StringBuffer buffer = new StringBuffer();

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");

		/*if (!isMarried(do2)) {
			validity = false;
			buffer.append(INVALID_STRINC);
		}
*/		
		if (emp.getSubstantiveRank()!=null && emp.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			validity=false;
			buffer.append(STILL_A_RECRUIT);
		}
		
		/*if (!isValidNoOfChildren(do2)) {
			validity = false;
			buffer.append(MORE_THAN_TWO);	}*/

		if (!is1stOfTheMonth(do2)) {
			validity = false;
			buffer.append(INVALID_START);
		}
		
		if (newDo2.getId()==do2.getId()) {
			if (isGettingStrinc(do2)) {
				validity = false;
				buffer.append(STRINC_ALREADY_DRAWING);
			}
		}
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}

		do2.setReason(buffer.toString());
	 		do2Repo.save(do2);
	 return validity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		emp.setStrincRank(rank);

		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);

	//	if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
	//  STRINC abolished with effect from 01-07-2017.		
	if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) && do2.getColumnA().equals(Do2Manager.GRANTED)) {		
			AuditCageHelper.updateAuditCageAllowance(newAuditCage,
					Do2Manager.STRINC, calculate_Increment(do2));
	} else if (do2.getColumnA().equals(Do2Manager.CEASED) || !do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			if (do2.getDescription().equals(Do2Manager.STRINC)) {
				final String[] alwUnderProcess = { do2.getDescription() };
				final List<String> alwToStop = Arrays.asList(alwUnderProcess);
				AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
			}
		}
		return Do2Manager.SUCCESS;
	}

	public boolean isMarried(Do2 do2) {
		return (emp.getMarried());
	}

	public boolean isGettingStrinc(Do2 do2) {
		return (emp.getStrincRank()!=null);
	}
	
	public boolean is1stOfTheMonth(Do2 do2) {
		Calendar c1 = new GregorianCalendar();
		c1.setTime(do2.getFromDate());
		return (c1.get(Calendar.DATE) == 1);
	}

	public boolean isValidNoOfChildren(Do2 do2) {
		boolean isvalid = false;
		int childKounter = 0;
		Date previousDob = null;
		boolean twinsInSecondIssue = false;
		boolean thirdIssue = false;
		List<FamilyDetail> fdlist = fdRepo.findByEmployeeRelationOrderByDateOfBirthAsc(emp.getId(), "SON", "DAUGHTER");
				
		/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT fd from FamilyDetail fd where "
								+ "fd.employee.id =:fdemp and (fd.relationship=:relation1 or fd.relationship =:relation2) "
								+ "order by fd.dateOfBirth ASC").setParameter(
						"fdemp", emp.getId()).setParameter("relation1", "SON")
				.setParameter("relation2", "DAUGHTER").getResultList();*/
		if (fdlist.size() > 0) {
			for (FamilyDetail fd : fdlist) {
				if (childKounter < 2) {
					childKounter += 1;
				} else if (childKounter >= 2) {
					if (fd.getDateOfBirth().equals(previousDob)) {
						twinsInSecondIssue = true;
					} else {
						thirdIssue = true;
					}
					childKounter += 1;
				}
				if (childKounter > 0) {
					previousDob = fd.getDateOfBirth();
				}
			}
		}
		if (childKounter <= 2) {
			isvalid = true;
		} else if (!thirdIssue) {
			if (childKounter > 2 && twinsInSecondIssue) {
				isvalid = true;
			}
		}
		return isvalid;
	}

	public int calculate_Increment(Do2 do2) {

		int increment_arrived_at = 0;
	
				if (do2.getFromDate().before(
						DateManager.getSixthCpcAllowanceDate())) {
						increment_arrived_at = getIncrementVcpc(do2);
				} else {

					List<Allowance> alwList = Utils
							.getApplicableAllowanceRates(do2.getDescription(),
									do2.getFromDate(), do2.getFromDate(), rank);
					if (alwList.size() > 0) {

						AuditCageHelper.updateAuditCageAllowance(newAuditCage,
								do2.getDescription(), alwList.get(0)
										.getRateAmount());
							increment_arrived_at = (int) alwList.get(0)
								.getRateAmount();
					}
				}


		return increment_arrived_at;
	}

	public int getIncrementVcpc(Do2 do2) {
		int incr = 0;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		String soldierGroup = employee.getGroupCategory().getSoldierGroup();
		if (employee.getRemusteringDate() != null
				&& do2.getFromDate().before(employee.getRemusteringDate())) {
			soldierGroup = employee.getPreviousGroupCategory()
					.getSoldierGroup();
		}

		Date fifthCpcDate = null;
		try {
			fifthCpcDate = DateManager.getDate("2005-12-31");
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
		List<PayScale> payList = psRepo.findByRankStartScaleGroupFromDateBetweenOrFromDateGreaterThanEqaual(rank.getId(),
																								soldierGroup, fifthCpcDate);
				
		/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank "
								+ " AND ps.startScale > 0 "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null ))")
				.setParameter("rank", rank).setParameter("grp", soldierGroup)
				.setParameter("fromDate", fifthCpcDate).getResultList();*/

		if (payList.size() > 0) {
			incr = payList.get(0).getIncr0();
		}
		return incr;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		emp = Utils.getCurrentEmployee(refDo2.getArmyNo());
		emp.setStrincRank(null);
		return Do2Manager.SUCCESS;
	}
}