package itsdc.dolphin.do2.auditcage;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.ClassPayRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Date;

/**
 * 
 * @author Venkat Date: 8-07-2009
 */

@RestController
public class Upclas extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	static public final String IS_PAO_DSC = "AU:DSC personnel not eligible for Classification Allowance. \n";
	static public final String ENTRY_MAX_EQUAL = "AU:Not eligible for Classification allowance. Entry Class and Maximum Class are same \n";
	static public final String NOT_MUSTERED = "AU: PBOR is not yet mustered. \n";
	static public final String NOT_NCO = "AU:PBOR is not a NCO - No Classification allowance. \n";
	static public final String CLASS_EQUAL = "DU:UPCLAS has already been adjusted. \n";
	static public final String GROUP_NOT_EQUAL = "AU:Group held by PBOR not matching with group in DOII. \n";
	static public final String LESS_THAN_MAX_CLASS = "AU:UPCLAS is less than maximum class. \n";
	static public final String LESS_THAN_THREE_MONTHS = "AU:Three months not completed after Down Clasification.\n";
	static public final String DATE_BEFORE_MUSTERING = "AU:UPCLAS date before date of initial mustering. \n";
	static public final String NOT_ABOVE_CURRENT_CLASS = "AU:UPCLAS is not next above to the existing class. \n";
	static public final String NOT_REMUSTERED = "AU:PBOR has not been remustered. \n";
	static public final String IS_SERVICE_DETAIL_AVAILABLE = "AU:Service Detail not available. \n";
	static public final String RATE_NOT_AVAILABLE = "AU:Class pay rate not available. \n";
	static public final String INVALID_GROUP = "AU:Group Z is not allowed from 01-01-2006.\n";
	static public String TA_DEPTAL_EMP = "AU:PBOR belongs to TA deptal category - not entitled\n";
	static public String APPOINTMENT_CATG_MISSING = "AU:Appointment Category for TA not available \n";
	Employee employee;
	GroupCategory groupCategory;
	String message, currentClassPayCode, currentDo2Group, employeeGroup,
			maximumClass, entryClass;

	static public List<EmployeeServiceDetail> serviceDetailList;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		StringBuffer buffer = new StringBuffer();

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (isPaoDsc()) {
			validity = false;
			buffer.append(IS_PAO_DSC);
		}
		
		if (employee.getRegularTaFlag().equals("T") && 
			employee.getTaAppointmentCategory() == null){
			validity=false;
			buffer.append(APPOINTMENT_CATG_MISSING);
		}
		if (employee.getRegularTaFlag().equals("T") && 
		   (employee.getTaAppointmentCategory() != null && 
			employee.getTaAppointmentCategory().equals("D"))){
			validity=false;
			buffer.append(TA_DEPTAL_EMP);
		}

		currentClassPayCode = do2.getColumnB().substring(
				do2.getColumnB().length() - 1);
		currentDo2Group = do2.getColumnA().substring(
				do2.getColumnA().length() - 1);
		/**
		 *In Remsustered cases,the group ,groupcategory are to be decided based
		 * on the date of Remuster and Do2 Fromdate
		 */
	
		if (do2.getColumnC() != null && 
			do2.getColumnC().equals(Do2Manager.REMUSTER)) {
			if (employee.getRemusteringDate() != null && 
			   !do2.getFromDate().before(employee.getRemusteringDate())) {
				do2.setStatus(Do2Manager.VALID);
				return true;
			}
			validity = false;
			buffer.append(NOT_REMUSTERED);
			do2.setStatus(Do2Manager.INVALID);
		}
		
		if(newDo2.getId() == do2.getId() && 
		   currentDo2Group.equals("Z") &&
		   !do2.getFromDate().before(DateManager.getSixthCpcDate())){
		   validity =false;
		   buffer.append(INVALID_GROUP);
		}
		
		if ((employee.getRegularTaFlag().equals("T") && !employee.getTaAppointmentCategory().equals("D") ||
			(employee.getRegularTaFlag().equals("R")))){
		
			getGroupClass(do2);

		/**
		 *In case of Upclas for Remustered cases no other validation except
		 * checking for remuster date and do2 FromDate after remuster date.
		 */

			if (!isEqualEntryMaximumClass()) {
				validity = false;
				buffer.append(ENTRY_MAX_EQUAL);
			}

			if (!isNco(do2)) {
				validity = false;
				buffer.append(NOT_NCO);
			}
			
			if(newDo2.getId() == do2.getId()) {
				if (isClassEqual(do2)) {
					validity = false;
					do2.setStatus(Do2Manager.PENDING);
					buffer.append(CLASS_EQUAL);
				}

			}
			
			if (!isGroupEqual(do2)) {
				validity = false;
				buffer.append(GROUP_NOT_EQUAL);
			}

			if (!isLessThanMaxClass(do2)) {
				validity = false;
				buffer.append(LESS_THAN_MAX_CLASS);
			}
			if (!isMustered()) {
				validity = false;
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(NOT_MUSTERED);
			}else if (isDateBeforeMustering(do2)) {
					validity = false;
					buffer.append(DATE_BEFORE_MUSTERING);
			}
		}

		if (employee.getRegularTaFlag().equals("R")){
			if (isLessThanThreeMonths(do2)) {
				validity = false;
				buffer.append(LESS_THAN_THREE_MONTHS);
			}
		}
		

		if (validity == true) {

			if(newDo2.getId() == do2.getId()) {
				if (!isAboveCurrentClass(do2)) {

					validity = false;
					do2.setStatus(Do2Manager.PENDING);
					buffer.append(NOT_ABOVE_CURRENT_CLASS);
				}
			}
			

	
			if (!isRateAvailable(do2)) {
				validity = false;
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(RATE_NOT_AVAILABLE);
			}
		}

		if (validity == false && do2.getStatus() != null
				&& !do2.getStatus().equals(Do2Manager.PENDING)) {
			do2.setStatus(Do2Manager.INVALID);
		}
		if (validity == false && do2.getStatus() == null) {
			do2.setStatus(Do2Manager.INVALID);
		}

		if (validity) {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	private boolean isPaoDsc() {
		if (Utils.getPao() != null
				&& !Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC))
			return false;
		return true;
	}

	private boolean isEqualEntryMaximumClass() {

		if ((entryClass!=null && maximumClass!=null) && !entryClass.equals(maximumClass)) {
			return true;
		}
		return false;
	} 

	private boolean isMustered() {
		if (employee.getInitialMusteringDate() != null || employee.getAttestationDate() !=null) {
			return true;
		}
		return false;
	}

	private boolean isNco(Do2 do2) {

		//Rank rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");

		if (rank == null
				|| !(Integer.parseInt(rank.getRankCode()) < Do2Manager.JCO_RANK_CODE))
			return false;

		return true;
	}

	private boolean isClassEqual(Do2 do2) {
       String soldierClass = getSoldierClass(do2);
	
		if (currentClassPayCode!=null && soldierClass!=null && (currentClassPayCode.compareTo(soldierClass) == 0)) {
			return true;
		}
		return false;
	}

	private boolean isGroupEqual(Do2 do2) {

		if (!do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			if ((currentDo2Group!=null && currentDo2Group.equals("Y")) && (employeeGroup!=null && employeeGroup.equals("Z"))) {
				return true;
			}
		}

		if (currentDo2Group!=null && employeeGroup!=null && currentDo2Group.compareTo(employeeGroup) == 0)
			return true;
		return false;
	}

	/*
	 * private boolean isLessThanMaxClass(Do2 do2) {
	 * 
	 * if (currentClassPayCode!=null && maximumClass!=null &&
	 * (maximumClass.compareTo(currentClassPayCode) <= 0)) {
	 * 
	 * return true; }
	 * 
	 * return false; }
	 */
    private boolean isLessThanMaxClass(Do2 do2) {
        System.out.println("Entering isLessThanMaxClass - currentClassPayCode: " + currentClassPayCode + ", maximumClass: " + maximumClass);

        if (currentClassPayCode != null && maximumClass != null) {
            try {
                int current = Integer.parseInt(currentClassPayCode.trim());
                int max = Integer.parseInt(maximumClass.trim());
                System.out.println("Parsed values - current: " + current + ", max: " + max);
                boolean result = max <= current;
                System.out.println("Comparison result (max <= current): " + result);
                return result;
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException caught: Invalid class code format - " + e.getMessage());
                return false;
            }
        } else {
            System.out.println("Null check failed - returning false");
        }
        return false;
    }

	private boolean isLessThanThreeMonths(Do2 do2) {

		serviceDetailList = esdRepo.findByMultipleDescriptionsAndOrderByFromDateDesc(employee.getId(), Do2Manager.ESD_CLASS_PAY+"",
																Do2Manager.DOWN_CLASS, Do2Manager.LOW_CLASS);
				
		/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT esd FROM EmployeeServiceDetail esd WHERE esd.do2.employee=:employee and esd.status=:status and (esd.do2.description =:description1 OR esd.do2.description=:description2) ORDER BY esd.fromDate desc")
				.setParameter("employee", employee).setParameter("status",
						Do2Manager.ESD_CLASS_PAY).setParameter("description1",
						Do2Manager.DOWN_CLASS).setParameter("description2",
						Do2Manager.LOW_CLASS).getResultList();*/

		if (serviceDetailList.size() > 0) {
			Date downClassDate = serviceDetailList.get(0).getFromDate();
			return DateManager.getNoOfMonths(downClassDate, do2.getFromDate()) < 3.0;
		}
		return false;
	}

	private boolean isDateBeforeMustering(Do2 do2) {
		
		Date musterDate = null;
		if(employee.getInitialMusteringDate() !=null){
			musterDate = employee.getInitialMusteringDate();
		}else{
			musterDate = employee.getAttestationDate();
		}
		if(do2.getFromDate().before(musterDate)){
			return true;
		}
		return false;
        //return DateManager.isFromDateBeforeToDate(musterDate, do2.getFromDate());
	}

	private boolean isAboveCurrentClass(Do2 do2) {
	      String soldierClass = getSoldierClass(do2);
		return ((Integer.parseInt(soldierClass) - (Integer.parseInt(currentClassPayCode))) == 1);

	}

	public boolean isRateAvailable(Do2 do2) {
		GroupCategory groupCategory = Utils.getGroupCategory(do2, "CUR");

		List<ClassPayRate> classPayRateList = classPayRepo.findByGroupCatClassPayCodeFromDateBetweenOrFromDateGreaterThan(groupCategory.getId(),
																									currentClassPayCode,do2.getFromDate());
				
			/*	Utils
				.getEntityManager()
				.createQuery(
						"SELECT cpr from ClassPayRate cpr where cpr.groupCategory= :groupCategory "
								+ " and cpr.classPayCode = :currentClassPayCode and "
								+ " ((:fromDate between cpr.fromDate and cpr.toDate) or (:fromDate >= cpr.fromDate and cpr.toDate is null))")
				.setParameter("groupCategory", groupCategory).setParameter(
						"currentClassPayCode", currentClassPayCode)
				.setParameter("fromDate", do2.getFromDate())
				.getResultList();*/
		return classPayRateList.size() > 0;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		/*if (employee.getCurrentClassPayDate() != null
				&& !employee.getCurrentClassPayDate().equals(do2.getFromDate())) {
			AuditCageHelper.updateEmployeeServiceDetail(do2,
					Do2Manager.ESD_CLASS_PAY);
			updateBdtInfo(do2);
		}*/
		AuditCageHelper.updateEmployeeServiceDetail(do2,Do2Manager.ESD_CLASS_PAY+"");
		updateBdtInfo(do2);
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		currentClassPayCode = newAuditCage.getDo2().getColumnB().substring(
				newAuditCage.getDo2().getColumnB().length() - 1);
		getGroupClass(newAuditCage.getDo2());
		 
			employee = Utils.getCurrentEmployee(newAuditCage.getDo2()
					.getArmyNo(), newAuditCage.getDo2().getCheckDigit());
		
			GroupCategory groupCategory = Utils.getGroupCategory(newAuditCage.getDo2(), "CUR");

		List<ClassPayRate> classPayRateList = classPayRepo.findByGroupCatClassPayCodeFromDateBetweenOrFromDateGreaterThan(groupCategory.getId(),
																			currentClassPayCode, newAuditCage.getDo2().getFromDate());
				
		/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT cpr from ClassPayRate cpr where cpr.groupCategory= :groupCategory "
								+ " and cpr.classPayCode = :currentClassPayCode and "
								+ " ((:fromDate between cpr.fromDate and cpr.toDate) or (:fromDate >= cpr.fromDate and cpr.toDate is null))")
				.setParameter("groupCategory", groupCategory).setParameter(
						"currentClassPayCode", currentClassPayCode)
				.setParameter("fromDate", newAuditCage.getDo2().getFromDate())
				.getResultList();*/

		if (classPayRateList.size() > 0) {
			newAuditCage.setClassPay(classPayRateList.get(0).getRate().intValue());
		}

		return newAuditCage;
	}

	private void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		employee.setSoldierClass(currentClassPayCode.charAt(0)+"");
		employee.setCurrentClassPayDate(do2.getFromDate());
	}

	private void reverseUpdateEmployee(Do2 refDo2) {

		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());

		if (employee.getRemusteringDate() == null
				|| (employee.getRemusteringDate() != null && refDo2
						.getFromDate().before(employee.getRemusteringDate()))) {

			serviceDetailList = Utils.getEmployeeServiceDetails(refDo2,
					Do2Manager.ESD_CLASS_PAY);
		} else {

			/**
			 * ServiceDetail are to be retrieved after remuster date. Separate
			 * list is generated for remuster cases.
			 */

			Date fromDate = employee.getRemusteringDate();
			serviceDetailList = getEmployeeServiceDetailsAfterMuster(refDo2,Do2Manager.ESD_CLASS_PAY+"", fromDate);
		}

		if (serviceDetailList.size() > 0) {
			String previousGroup = serviceDetailList.get(0).getDo2()
					.getColumnB().substring(
							serviceDetailList.get(0).getDo2().getColumnB()
									.length() - 1);
			employee.setCurrentClassPayDate(serviceDetailList.get(0)
					.getFromDate());
			employee.setSoldierClass(previousGroup.charAt(0)+"");

		} else {
			employee.setCurrentClassPayDate(null);
			if (employee.getRemusteringDate() == null
					|| (employee.getRemusteringDate() != null && !refDo2
							.getFromDate()
							.before(employee.getRemusteringDate()))) {
				employee.setSoldierClass(employee.getGroupCategory()
						.getEntryClass());
			} else {
				employee.setSoldierClass(employee.getPreviousGroupCategory()
						.getEntryClass());
			}
		}

	}

	private List<EmployeeServiceDetail> getEmployeeServiceDetailsAfterMuster(
			Do2 do2, String status, Date fromDate) {
		Employee employee =Utils.getCurrentEmployee(do2.getArmyNo());
		serviceDetailList = esdRepo.findByFromDateGreaterThanEqualToStatusOrderByFromDateDesc(employee.getId(),status,fromDate);
				
			/*	Utils
				.getEntityManager()
				.createQuery(
						"SELECT esd FROM EmployeeServiceDetail esd WHERE esd.status=:status AND "
								+ " esd.fromDate >= :fromDate AND esd.do2.employee=:employee ORDER BY esd.fromDate DESC")
				.setParameter("fromDate", fromDate).setParameter("employee",
						employee).setParameter("status", status)
				.getResultList();*/

		return serviceDetailList;
	}

	public void getGroupClass(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		GroupCategory groupCategory = Utils.getGroupCategory(do2, "CUR");
		employeeGroup = groupCategory.getSoldierGroup() + "";
		maximumClass = groupCategory.getMaximumClass() + "";
		entryClass = groupCategory.getEntryClass() + "";
	}
	
    public String getSoldierClass(Do2 do2){
    	String soldierClass = "";
    	String serviceDetailGroup="";
    	int previousClass = Integer.parseInt(currentClassPayCode)+1;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (newDo2.getId() == do2.getId()) {
			if ((employee.getRemusteringDate() == null && employee.getCurrentClassPayDate()!=null 
					&& !do2.getFromDate().before(employee.getCurrentClassPayDate()))
					|| (employee.getRemusteringDate() != null && !do2
							.getFromDate()
							.before(employee.getRemusteringDate()))) {
				soldierClass = employee.getSoldierClass() + "";
			} else {
				serviceDetailList = Utils.getEmployeeServiceDetails(do2,
						Do2Manager.ESD_CLASS_PAY);
				if (serviceDetailList.size() > 0) {
					soldierClass = serviceDetailList.get(0).getDo2()
							.getColumnB().substring(
									serviceDetailList.get(0).getDo2()
											.getColumnB().length() - 1);
				} else {
					if(previousClass >= Integer.parseInt(entryClass)){
						soldierClass = entryClass;
					}else{
						soldierClass = previousClass+"";	
					}
					
				}
			}
		} else {
			serviceDetailList = Utils.getEmployeeServiceDetails(do2,
					Do2Manager.ESD_CLASS_PAY);
			if (serviceDetailList.size() > 0) {
				soldierClass = serviceDetailList.get(0).getDo2().getColumnB()
						.substring(
								serviceDetailList.get(0).getDo2().getColumnB()
										.length() - 1);
				serviceDetailGroup = serviceDetailList.get(0).getDo2().getColumnA()
				.substring(
						serviceDetailList.get(0).getDo2().getColumnA()
													.length() - 1);
				String do2Group= do2.getColumnA().length() - 1+"";
				if(!do2Group.equals(serviceDetailGroup)){
					soldierClass = previousClass+"";
				}
				
			} else {
				if(previousClass >= Integer.parseInt(entryClass)){
					soldierClass = entryClass;
				}else{
					soldierClass = previousClass+"";	
				}
			
			}

		}
		//Added on 28.08.2023 for AOC FS IRLA
		//commented on 10-11-2023
		/*
		if (serviceDetailList!=null && serviceDetailList.size()>0) {
			if (employee.getRemusteringDate()!=null && employee.getCurrentClassPayDate()!=null) {
				if (serviceDetailList.get(0).getFromDate().before(employee.getRemusteringDate())) {
					System.out.println("entry class.................... " +entryClass);
					return entryClass;
				}
			}
		}*/
		
        return soldierClass;
    }
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,
				Do2Manager.ESD_CLASS_PAY);
		reverseUpdateEmployee(refDo2);
		return Do2Manager.SUCCESS;
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}
