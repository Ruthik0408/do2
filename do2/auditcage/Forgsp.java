package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC,Secunderabad
 * 
 */

@RestController

public class Forgsp extends AuditCageBase implements
		Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;

	static public final String INVALID_REFDO2 = "AU:Incorrect Reference Do2 No.\n ";
	static public final String INVALID_GSPAY = "AU:PBOR is not drawing GS Pay.\n";
	static public final String INVALID_COLUMN_A = "AU:ColumnA is Blank.\n";
	static public final String RATE_NOT_AVAILABLE = "AU:Rate not available in the Allowance Rate table.\n";
	static public Date revised_appxj_impl_date ;
	private StringBuffer buffer = new StringBuffer();
	Employee employee;
	Do2 refDo2;
	Rank rank;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		
		if (do2.getDescription().equals("FORGSP") && do2.getDo2Group().equals("IB") &&
	        !do2.getDo2Date().before(revised_appxj_impl_date)) { 
			validity = false;
			buffer.append("Description should be FRFGSP");
	    }
		if (do2.getDo2Date().before(revised_appxj_impl_date)){
			if (!isValidRefDo2(do2)) {
				validity = false;
				buffer.append(INVALID_REFDO2);
			}
		}
		if (!isValidGspay(do2)) {
			validity = false;
			buffer.append(INVALID_GSPAY);
		}
		if (do2.getAcceptedQe() == null){
			if (do2.getColumnA() ==null ){
			    validity =false;
			    do2.setStatus(Do2Manager.INVALID);
			    buffer.append(INVALID_COLUMN_A);
			} else {
			    if ((!do2.getColumnA().equals("ALL") || !do2.getColumnA().equals("FIRST")) && !isRateAvailable(do2) ) {
			       validity = false;
			       do2.setStatus(Do2Manager.PENDING);
			       buffer.append(RATE_NOT_AVAILABLE);
				}
			}
		}
		if (validity == false) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);	
			}
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getDo2Group().equals("IB")) {
			AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PUNISHMENT+"");
		}
		updateBdtInfo(do2);
		String columnA = do2.getColumnA();
		List<Allowance> allowances = null;
		if (columnA !=null){
			if (columnA.equals("ALL") || columnA.equals("FIRST")){
				AuditCageHelper.removeAllowances(Arrays.asList(new String[] { Do2Manager.GSPAY }), newAuditCage);
			} else if (columnA.equals("SECOND")){
			   allowances = Utils.getApplicableAllowanceRates(Do2Manager.GSPAY,	"FIRST", do2.getFromDate(), do2.getFromDate());
			
			} else {
			   allowances = Utils.getApplicableAllowanceRates(Do2Manager.GSPAY,	"SECOND", do2.getFromDate(), do2.getFromDate());		
			}
		}
		if (allowances != null && allowances.size() > 0) {
			if (Integer.parseInt(rank.getRankCode()) < 50){
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.GSPAY, allowances.get(0).getRateAmount());
			}		   
		}
		return Do2Manager.SUCCESS;
	}

	public boolean isValidGspay(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());		
		if (Integer.parseInt(rank.getRankCode()) < 50) {
			if (employee.getCurrentGspayCode() != null) 
				return true;
		}
		return false;
	}

	public boolean isValidRefDo2(Do2 do2) {

		List<Do2> do2List = Utils.getReferenceDo2List(do2);
		for (Do2 forGsPayRefDo2 : do2List) {

			if (forGsPayRefDo2 != null &&
			   (forGsPayRefDo2.getDescription().equals(Do2Manager.AWL)
				|| forGsPayRefDo2.getDescription().equals(Do2Manager.OSL)
				|| forGsPayRefDo2.getDescription().equals(Do2Manager.REP)
				|| forGsPayRefDo2.getDescription().equals(Do2Manager.MILRI)
				|| forGsPayRefDo2.getDescription().equals(Do2Manager.LOW_CLASS) 
				|| forGsPayRefDo2.getDescription().equals(Do2Manager.SEVREP))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PUNISHMENT);
		reverseUpdateEmployee(currentDo2, refDo2);
		return Do2Manager.SUCCESS;
	}

	public boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances = Utils.getApplicableAllowanceRates(Do2Manager.GSPAY,
					do2.getColumnA(), do2.getFromDate(), do2.getFromDate());
				return allowances.size() > 0;
	}

	public void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if(do2.getColumnA()!=null){
		if (do2.getColumnA().equals("THIRD") ) {
			employee.setCurrentGspayCode("2");
		}
		if (do2.getColumnA().equals("SECOND")) {
			employee.setCurrentGspayCode("1");
		}
		if (do2.getColumnA().equals("FIRST") || do2.getColumnA().equals("ALL")) {
			employee.setCurrentGspayCode("0");
		}
		employee.setCurrentGsPayDate(do2.getFromDate());
		}
	}

	public void reverseUpdateEmployee(Do2 do2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> do2List = do2Repo.findByFromDateLessThanEqualToAndColAAndDescriptionAndStatus(employee.getId(), "GSPAY",
																						do2.getFromDate(), do2.getColumnA(),"V");
				
		/*		Utils.getEntityManager().createQuery(
						" SELECT d FROM Do2 d WHERE d.employee = :employee AND d.description= 'GSPAY' AND d.fromDate <= :fromdate "
								+ " AND d.columnA=:column AND d.status = 'V' ORDER BY d.fromDate DESC ")
				.setParameter("employee", employee)
				.setParameter("fromdate", do2.getFromDate())
				.setParameter("column",	do2.getColumnA()).getResultList();*/
		if (do2List.size() > 0) {
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			if (refDo2.getColumnA().equals("THIRD")	|| refDo2.getColumnA().equals("ALL")) {
				employee.setCurrentGspayCode("3");
			}
			if (refDo2.getColumnA().equals("SECOND")) {
				employee.setCurrentGspayCode("2");
			}
			if (refDo2.getColumnA().equals("FIRST")) {
				employee.setCurrentGspayCode("1");
			}
			employee.setCurrentGsPayDate(do2List.get(0).getFromDate());
		}
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}
