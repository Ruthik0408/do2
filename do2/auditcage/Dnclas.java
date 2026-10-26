package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.ClassPayRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Dnclas extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {

	static public final String INVALID_GROUP = "AU:Group given in DOII is not matching with current Employee group. \n";
	static public final String INVALID_CLASS = "AU:PBOR is not holding higher classification than the one given in Col.7B.\n";
	static public final String INVALID_LOCLASS = "AU:Class given in DOII is not the lowest class of the trade. \n";
	static public final String INVALID_RANK = "AU:PBOR already a JCO on from date. \n";

	private Employee employee;
	private GroupCategory groupCategory;
	private String currentDo2ClassPayCode, oldDo2ClassPayCode, currentDo2Group,
			employeeGroup;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		do2.setStatus(Do2Manager.VALID);
		StringBuffer buffer = new StringBuffer();

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		currentDo2ClassPayCode = do2.getColumnB().substring(
				do2.getColumnB().length() - 1);
		currentDo2Group = do2.getColumnA().substring(
				do2.getColumnA().length() - 1);

		if (!isValidGroup(do2, employee)) {
			buffer.append(INVALID_GROUP);
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
		}

		if (Integer.parseInt(employee.getSubstantiveRank()
				.getRankCode()) >= 50) {
			if (!isCurrentDo2EarliertoJCODo2(do2)) {
				buffer.append(INVALID_RANK);
				do2.setStatus(Do2Manager.PENDING);
				validity = false;
			}
		}

		if (do2.getDescription().equals("DNCLAS")) {
			if (!isOneClassBelow(employee, currentDo2ClassPayCode)) {
				buffer.append(INVALID_CLASS);
				do2.setStatus(Do2Manager.PENDING);
				validity = false;
			}
		} else {
			if ((!isValidClassInDo2(do2, currentDo2Group,
					currentDo2ClassPayCode))
					&& !(Integer
							.parseInt(employee.getSoldierClass().toString()) < Integer
							.parseInt(currentDo2ClassPayCode))) {
				buffer.append(INVALID_LOCLASS);
				do2.setStatus(Do2Manager.INVALID);
				validity = false;
			}
		}

		do2.setReason(buffer.toString());
	
		if (do2.getDescription().equals("LOCLAS")) {
			if (employee.getCurrentGspayCode() != null
					&& !employee.getCurrentGspayCode().equals("0")
					&& do2.getPunishment().equals("CM")) {
				if (isdo2GenerationRequired(do2)) {
					AuditCageHelper
							.generateForGspayDo2(do2, getGsPayGrade(do2));
				}
			}

		}
		do2Repo.save(do2);

		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		currentDo2ClassPayCode = newAuditCage.getDo2().getColumnB().substring(
				newAuditCage.getDo2().getColumnB().length() - 1);

		List<ClassPayRate> classPayRateList = classPayRepo.findByGroupCatClassPayCodeFromDateBetweenOrFromDateGreaterThan(Utils.getGroupCategory(newAuditCage.getDo2(),"CUR").getId(), currentDo2ClassPayCode,newAuditCage.getDo2().getFromDate());
				
			 

		if (classPayRateList.size() == 1) {
			newAuditCage.setClassPay( classPayRateList.get(0).getRate().intValue());
		}

		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (newDo2.getId() == do2.getId()) {
			updateBdtInfo(do2);
		}
		AuditCageHelper.updateEmployeeServiceDetail(do2,Do2Manager.ESD_CLASS_PAY+"");
		if (do2.getDescription().equals("LOCLAS")) {
			AuditCageHelper.updateEmployeeServiceDetail(do2,Do2Manager.ESD_PUNISHMENT+"");
			/*
			 * if (do2.getPunishment().equals("CM"))
			 * AuditCageHelper.updateEmployeeServiceDetail(do2,
			 * Do2Manager.ESD_GSPAY);
			 */
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		reverseUpdateEmployee(refDo2);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,
				Do2Manager.ESD_CLASS_PAY);
		if (refDo2.getDescription().equals("LOCLAS")) {
			AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,
					Do2Manager.ESD_PUNISHMENT);
		}
	/*	List <Do2> do2List =Utils.getReferenceDo2List(currentDo2);
		for(Do2 genDo2 : do2List){
			AuditCageHelper.generateCancelDo2(genDo2, currentDo2);
		}*/
		return Do2Manager.SUCCESS;
	}

	private void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		employee.setCurrentClassPayDate(do2.getFromDate());
		employee.setSoldierClass(do2.getColumnB().substring(
				do2.getColumnB().length() - 1));
		empRepo.save(employee);
	}

	private void reverseUpdateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<String> descList=Arrays.asList(new String[] {"UPCLAS","MSTHAV"});
		
		List<Do2> do2List =do2Repo.findByDescListFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(), Do2Manager.VALID, descList, do2.getFromDate()); 
		 
		if (do2List.size() > 0) {
			Do2 oldDo2 = do2List.get(0);
			int earlierClass = Integer.parseInt(oldDo2.getColumnB().substring(
					oldDo2.getColumnB().length() - 1));
			int currentDo2ClassPayCode = Integer.parseInt(do2.getColumnB()
					.substring(do2.getColumnB().length() - 1));
			if (do2.getDescription().equals("DNCLAS")) {
				if (earlierClass > 0
						&& (currentDo2ClassPayCode - 1) == earlierClass) {
					employee.setCurrentClassPayDate(oldDo2.getFromDate());
					employee.setSoldierClass(oldDo2.getColumnB().substring(
							oldDo2.getColumnB().length() - 1));
				}
			} else {
				employee.setCurrentClassPayDate(oldDo2.getFromDate());
				employee.setSoldierClass(oldDo2.getColumnB().substring(
						oldDo2.getColumnB().length() - 1));

				/*
				 * if (do2.getPunishment().equals("CM")) {
				 * List<EmployeeServiceDetail> esdList =
				 * Utils.getEmployeeServiceDetails(do2, Do2Manager.ESD_GSPAY);
				 * 
				 * if (esdList.size() > 0) { for (EmployeeServiceDetail esd :
				 * esdList) { if
				 * (esd.getDo2().getDescription().equals(Do2Manager.GSPAY)) { if
				 * (esd.getDo2().getColumnA().equals("THIRD")) {
				 * employee.setCurrentGspayCode('3');
				 * employee.setCurrentGsPayDate(esd.getDo2().getFromDate());
				 * break; } if (esd.getDo2().getColumnA().equals("SECOND")) {
				 * employee.setCurrentGspayCode('2');
				 * employee.setCurrentGsPayDate(esd.getDo2().getFromDate());
				 * break; } if (esd.getDo2().getColumnA().equals("FIRST")) {
				 * employee.setCurrentGspayCode('1');
				 * employee.setCurrentGsPayDate(esd.getDo2().getFromDate());
				 * break; } } } } }
				 */
			}
			empRepo.save(employee);
		}
	}

	public boolean isValidGroup(Do2 do2, Employee employee) {
		currentDo2Group = do2.getColumnA().substring(
				do2.getColumnA().length() - 1);

		if ((employee.getRemusteringDate() == null)
				|| (!do2.getFromDate().before(employee.getRemusteringDate()))) {
			employeeGroup = employee.getGroupCategory().getSoldierGroup() + "";
		} else {
			employeeGroup = employee.getPreviousGroupCategory()
					.getSoldierGroup()
					+ "";
		}

		return (employeeGroup.equals(currentDo2Group));
	}

	private boolean isOneClassBelow(Employee employee,
			String currentDo2ClassPayCode) {
		return ((Integer.parseInt(employee.getSoldierClass().toString()) == ((Integer
				.parseInt(currentDo2ClassPayCode)) - 1)));
	}

	private boolean isValidClassInDo2(Do2 do2, String currentDo2Group,
			String currentDo2ClassPayCode) {

		if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			if ((currentDo2Group.equals("Z") && ((Integer
					.parseInt(currentDo2ClassPayCode) >= 2) && (Integer
					.parseInt(currentDo2ClassPayCode) < 4)))) {
				return true;
			}
		}

		if ((currentDo2Group.equals("X"))
				&& ((Integer.parseInt(currentDo2ClassPayCode) > 1) && (Integer
						.parseInt(currentDo2ClassPayCode) < 4)))
			return true;

		if ((currentDo2Group.equals("Y"))
				&& ((Integer.parseInt(currentDo2ClassPayCode) >= 2) && (Integer
						.parseInt(currentDo2ClassPayCode) <= 4)))
			return true;

		return false;
	}

	private boolean isCurrentDo2EarliertoJCODo2(Do2 do2) {

		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (do2.getFromDate().before(employee.getSubstantiveRankDate())) {
			
			List<String> descList=Arrays.asList(new String[] {"UPCLAS","MSTHAV"});
			
			List<Do2> do2List =do2Repo.findByDescListFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(), Do2Manager.VALID, descList, do2.getFromDate()); 
		 
			if (do2List.size() > 0) {
				currentDo2ClassPayCode = do2.getColumnB().substring(
						do2.getColumnB().length() - 1);
				currentDo2Group = do2.getColumnA().substring(
						do2.getColumnA().length() - 1);
				oldDo2ClassPayCode = do2List.get(0).getColumnB().substring(
						do2List.get(0).getColumnB().length() - 1);
				return (Integer.parseInt(oldDo2ClassPayCode) < Integer
						.parseInt(currentDo2ClassPayCode));
			}
		}
		return false;
	}

	public boolean isdo2GenerationRequired(Do2 do2) {
		List<Do2> do2List =do2Repo.findByEmpIdRefDo2NoDescription(do2.getEmployee().getId(),Utils.getReferenceDo2No(do2),"FORGSP"); 
		 
		return do2List.size() == 0;
	}

	public String getGsPayGrade(Do2 do2) {
		String columnA = " ";
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (employee.getCurrentGspayCode().equals("3")) {
			columnA = "THIRD";
		} else if (employee.getCurrentGspayCode().equals("2")) {
			columnA = "SECOND";
		} else if (employee.getCurrentGspayCode().equals("1")) {
			columnA = "FIRST";
		}
		return columnA;

	}
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}
