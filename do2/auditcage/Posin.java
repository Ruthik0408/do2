package itsdc.dolphin.do2.auditcage;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * Posting In Do2
 * 
 * @author Date: 08-07-2009
 * 
 *         Code added to process POSOUT also - 27.01.2010
 */

@RestController
public class Posin extends AuditCageBase implements	Do2AuditCageProcessor, java.io.Serializable {

	static public final String INVALID_UNITCODE_GIVEN_IN_COLUMN7C = "VL:Unit code not available in Unit table/not given in Col.7C.\n";
	static public final String POSTING_TO_CURRENT_UNIT = "AU:POSIN Unit and serving unit are same.\n";
	static public final String POSIN_POSOUT_DIFFERS_UNIT = "AU:Unit in POSOUT DOII and Unit in POSIN DOII are not the same.\n";
	static public final String POSOUT_TO_SAME_UNIT = "AU:Unit to which PBOR is posted and serving unit are same. \n.";
	static public final String NOT_IN_FS_LIST = "AU:This Army No is Not a FS Case/Fs Case Entry Not approved. \n.";

	
	static String[] specialCategory = { "CLKGD", "CAJ", "DVRMT", "DVRSPLVEH", "MSMITH", "OEMC", "PTRDECTR", "SHT", "SKT", "BARBER", "BSMITH",
			"CARPU", "CHEFSPEL", "CHEFMESS", "CHEFCOM", "ER", "MUSICIAN", "MASALCHI", "STEWD", "CSTEWD", "SWALA", "SDU", "SHGD", "SWRGD",
			"TLR", "TSMITH", "WM", "CLKPO", "SRCHEF", "MCHEF", "PKR", "ASH", "PTRU", "PE", "CLKSD", "SPSTFER", "MKEEPER" };
	static public final List<String> SPECIAL_CATEGORY = Arrays.asList(specialCategory);

	static String[] specialAppointments = { "TSLNK", "CQMH", "CHM", "BQMH", "BHM", "RQMH", "RHM", "TSALD", "SDM", "BDM", "RDM", "SQMD", "MQMD", "RQMD" };
	static public final List<String> SPECIAL_APPOINTMENTS = Arrays.asList(specialAppointments);

	private Unit unit = null;
	private Employee employee = null;
	private List<Do2> do2List = null;

	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (employee.getStatusCode()!=null && employee.getStatusCode().getId()>6) {
			if (do2.getDescription().equals("POSIN")) {
				if (isNe(employee, do2)) {
					do2.setStatus(Do2Manager.INVALID);
					do2.setReason("POSIN ALREADY EXISTS OR ON or AFTER DISCHARGE DATE - PLEASE CHECK");			
					do2Repo.save(do2);
					return false;
				}
			}
			
		}
	 	//added on 09062012 for fs cases
		if (do2.getColumnA() !=null && do2.getColumnA().equals("FS")){
			if (!Utils.isEmpThisMonthFs(employee) && employee.getStatusCode().getId() < 6){
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(NOT_IN_FS_LIST);
		      	do2Repo.save(do2);
		      	
	      	    return false;				
			}			
      	  	do2.setStatus(Do2Manager.VALID);
      	  	do2Repo.save(do2);
      	  	return true;
        }
		//added end
		
		if (newDo2!=null && newDo2.getId()==do2.getId()) {
			if (do2.getColumnA() == null && isEmpFs(employee, do2)) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(" FROM DATE IS WITHIN FS MONTH - IGNORED ");
				 if (Utils.getPao().getPaoCode().equals("96")){
					 do2.setReason("FROM DATE IS WITHIN 2 MONTHS FROM FS DATE - IGNORED");
				 }
				
		      	do2Repo.save(do2);
	      	    return false;				
			}
		}
		
		
		
		if (newDo2.getId()==do2.getId() || 
		   (do2.getStatus()==null || (do2.getStatus()!=null && !do2.getStatus().equals(Do2Manager.VALID)))) { 	
			if (do2.getDescription().equals("POSOUT")) {				
				/* 
				 *  In view of the amendments in revised appendix j unit code in col 7c is not reqd
				 */
				if (do2.getColumnC() == null || do2.getColumnC().length() == 0) {
					do2.setColumnC(null);
					do2Repo.save(do2);
				}				
			}
					
			if (do2.getDescription().equals(Do2Manager.POSIN) && employee.getUnit()!=null && do2.getUnit()!=null 
					&& employee.getUnit().getId()==do2.getUnit().getId() && employee.getPresentUnitDate().before(do2.getFromDate())) {
				 do2List = do2Repo.findByDescColCFromDateEqual(employee.getId(),"V","POSOUT", do2.getUnitCode(),
						 															do2.getFromDate());
						 
					/*	 Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 WHERE do2.employee=:employee " +
						" AND do2.description='POSOUT' AND status='V' AND do2.fromDate =:fromDate and do2.columnC != null and " +
						" do2.columnC= :unitcd")
						.setParameter("fromDate", do2.getFromDate())
						.setParameter("unitcd", do2.getUnitCode())
						.setParameter("employee", employee).getResultList();*/
				 if (do2List.size() > 0){
					 ;
				 } else {
					 do2.setStatus(Do2Manager.INVALID);
					 do2.setReason(POSTING_TO_CURRENT_UNIT);
					do2Repo.save(do2);			
					 return false;
				 }
			}
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}		
		do2Repo.save(do2);
		return (do2.getStatus()!=null && do2.getStatus().equals(Do2Manager.VALID));
	}
	
	public boolean isEmpFs(Employee employee, Do2 do2){
		List<LpcRegister> lpcList= lpcRepo.findByEmployee(employee.getId());
				
			//	Utils.getEntityManager().createQuery("select l FROM LpcRegister l where l.employee=:emp and l.recordStatus='F' and l.entryApproved=true")
				//											.setParameter("emp",employee).getResultList();
		 if (lpcList.size() >0) {
			 Date tmpdate = null;
			 Date dischdt = lpcList.get(0).getDischargeDate();
			 Calendar currentCal = new GregorianCalendar();
			 currentCal.setTime(dischdt);
			 int date = currentCal.get(Calendar.DATE);
			 int year = currentCal.get(Calendar.YEAR);
			 int month = currentCal.get(Calendar.MONTH);
			 month += 1;
			 try {
				tmpdate=DateManager.getDate(year+"-"+month+"-01");
			 } catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			 }
			 //within two months prior to discharge dat posin, posout are ignored  -- for pao para only as per their request
			 if (Utils.getPao().getPaoCode().equals("96")){
				 tmpdate = DateManager.getDate(dischdt, -60);
			 }
			 if (do2.getFromDate().after(tmpdate) && 
				 do2.getFromDate().before(dischdt)) {
				 return true;
			 }
		 }
		 return false;
	}
  
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		//this method is for checking whether posin is for fs and 
				//removing allowances limited to FS
				//***method added on 19122011
		if (do2.getDescription().equals("POSIN") && do2.getColumnA()!=null && do2.getColumnA().equals("FS")){
			AuditCageHelper.removeAllowancesForFs(newAuditCage);
			return Do2Manager.SUCCESS;
		}
		
		if(do2.getDescription().equals("POSOUT")) {
			do2List = do2Repo.findFsPosinDo2(do2.getEmployee().getId(), do2.getFromDate());
			return Do2Manager.SUCCESS;
		}
		
		if(do2.getDescription().equals("POSOUT")) {
			if( Utils.isEmpThisMonthFs(do2.getEmployee())) {
				return Do2Manager.SUCCESS;
			}
		}
		
		
		
		
		if (do2.getDescription().equals("POSIN") && do2.getColumnA()!=null && do2.getColumnA().equals("DANTAK")){
			AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.DEPU_DANTAK_BHUTAN+"");
		}
		updateAuditCageAllowanceForApptPay(do2, baseAuditCage);		
		if (do2.getDescription().equals(Do2Manager.POSIN) && employee.getUnit()!=null 
				&& employee.getUnit().getUnitCode().equals(do2.getUnitCode())
				&& employee.getPresentUnitDate()!=null && employee.getPresentUnitDate().equals(do2.getFromDate())) {
			return Do2Manager.SUCCESS;
		}
		if (do2.getDescription().equals(Do2Manager.POSIN)) {
			if (employee.getPresentUnitDate()!=null && do2.getFromDate().after(employee.getPresentUnitDate())) {
		 		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_POSIN+"");
	 		} else { 
	 			updateEmployeeServiceDetailForEarlierDo2(do2);
			}
			updateBdtInfo(do2);
		}				
		return Do2Manager.SUCCESS;
	}
	
	private void updateEmployee(Do2 do2) {
		if (newDo2.getId()==do2.getId() || newDo2.getDescription().equals("POSIN")) {
		    employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
			if (employee.getPresentUnitDate()!=null && do2.getFromDate().after(employee.getPresentUnitDate())) {
				employee.setPreviousUnitDate(employee.getPresentUnitDate());
				if (employee.getUnit()!=null)
					employee.setPreviousUnit(employee.getUnit());
			}
			
			if (employee.getPresentUnitDate()==null 
					|| (employee.getPresentUnitDate()!=null && do2.getFromDate().after(employee.getPresentUnitDate()))) {
				if (do2.getDescription().equals(Do2Manager.POSIN)){
					employee.setPresentUnitDate(do2.getFromDate());
					employee.setUnit(do2.getUnit());
				}
			}
			empRepo.save(employee);
			 
    	}		
	}

	private void updateEmployeeServiceDetailForEarlierDo2(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<EmployeeServiceDetail> esdList;
		esdList = Utils.getEmployeeServiceDetail(do2, Do2Manager.ESD_POSIN+"");

		EmployeeServiceDetail base = null;
		EmployeeServiceDetail follow = null;
		EmployeeServiceDetail current = null;
		int listSize = esdList.size();
		for (int i = 0; i < listSize; i++) {
			current = esdList.get(i);
		
			if (current.getDo2().getId()==do2.getId()) {
				return;
			}
			
			if (current.getFromDate().equals(do2.getFromDate()) 
					&& do2.getDescription().equals("POSIN") && current.getDo2().getDescription().equals("POSOUT")) {
				current.setDo2(do2);
				return;
			}
			
			if (current.getFromDate().equals(do2.getFromDate()) 
					&& do2.getDescription().equals("POSOUT") && current.getDo2().getDescription().equals("POSIN")) {
				current.setDo2(do2);
				return;
			}
 			
			if (do2.getFromDate().before(current.getFromDate())) {
				AuditCageHelper.addEmployeeServiceDetail(do2,Do2Manager.ESD_POSIN+"").setToDate(DateManager.getPreviousDate(current.getFromDate()));
				return;
			}
 
			if ((i < listSize - 1)
					&& (do2.getFromDate().after(current.getFromDate()))
					&& (do2.getFromDate().before(esdList.get(i + 1)
							.getFromDate()))) {
				base = current;
				follow = esdList.get(i + 1);
				base.setToDate(DateManager.getPreviousDate(do2.getFromDate()));
 
				AuditCageHelper.addEmployeeServiceDetail(do2, Do2Manager.ESD_POSIN+"").setToDate(DateManager.getPreviousDate(follow.getFromDate()));
				return;
			}
		}
	}
//method replaced on 15/04/2012
	private void updateAuditCageAllowanceForApptPay(Do2 do2, AuditCage baseAuditCage) {
		List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
				
		//		Utils.getEntityManager().createQuery("SELECT aca FROM AuditCageAllowance aca "
			//					+ " where aca.auditCage = :newAuditCage").setParameter("newAuditCage", newAuditCage).getResultList();

		AuditCageHelper.removeAllowancesForPosin(newAuditCage);
 
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<String> apptList = Arrays.asList("APTPAY");
		if (Do2Manager.APPOINTMENT_EXCEPTION_PAOS.contains(Utils.getPao().getPaoCode()))
			return;
		
		for (AuditCageAllowance aca : acaList) {
		
			if (aca.getPayCode().getPayCode().equals("APTPAY")){
				if (aca.getDo2() != null && aca.getDo2().getDescription().equals("MSTHAV"))
					return;
				if (aca.getDo2() !=null && SPECIAL_APPOINTMENTS.contains(aca.getDo2().getColumnA())){
					return;
				}
				
				if (Utils.getPao().getPaoCode().equals("88")) {
					Date revisedMsthavImplDate=null;		
					try {
						 revisedMsthavImplDate=DateManager.getDate("2014-10-01");
					} catch (Exception e) {
						Utils.handleException(e);
					}
					List<Do2>msthavDo2List = do2Repo.findByDescFromDateLessThanEqual(employee.getId(),"V","MSTHAV", do2.getFromDate());
							
					/*		Utils.getEntityManager().createQuery (" SELECT do2 From Do2 do2   "
							+ " where do2.employee=:employee and do2.description='MSTHAV' and do2.fromDate<=:fromDate and " 
							+ " do2.status='V'")
							.setParameter("employee", employee)
							.setParameter("fromDate", do2.getFromDate())
							.getResultList();*/
					if (msthavDo2List.size()>0) {
						if (!msthavDo2List.get(0).getFromDate().before(revisedMsthavImplDate)) {
							if (aca.getDo2() != null && aca.getDo2().getColumnA()!=null && aca.getDo2().getColumnA().trim().equals("HMT")) {
								return;
							}
						} else {
							return;
						}				 		
					}  						
				}
				if (aca.getDo2() !=null && (aca.getDo2().getColumnA().equals("LNK"))){
					GroupCategory gp = Utils.getGroupCategory(do2,"CUR");
					if (SPECIAL_CATEGORY.contains(gp.getSoldierCategory())){
						return;
					}
				}
				
				AuditCageHelper.removeAllowances(apptList, newAuditCage);
				List<EmployeeServiceDetail> esdList =  esdRepo.findByEmployeeDo2AndStatus(employee.getId(),aca.getDo2(),Do2Manager.ESD_APPT+"");
						
			/*			Utils.getEntityManager()
				.createQuery("SELECT esd FROM EmployeeServiceDetail esd"
								+ " WHERE esd.employee=:employee and esd.do2 = :do2 AND esd.status = :status")
				.setParameter("do2", aca.getDo2()).setParameter("status", Do2Manager.ESD_APPT)
				.setParameter("employee",employee).getResultList();*/
				for( EmployeeServiceDetail esd : esdList){
					if (esd.getToDate() == null)
						esd.setToDate(DateManager.getPreviousDate(do2.getFromDate()));
					break;
				}						
			}
		}
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		int gsoCode = 1;
		
		if (employee.getGsoCode()!=null){
			gsoCode = employee.getGsoCode();
			if (gsoCode==3) {
				gsoCode = 1;
			}
		}
		
		Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(), "PAYFIX");
		newAuditCage.setTransport(0);
		
		if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if ((employee.getInitialMusteringDate()!=null && !employee.getInitialMusteringDate().before(newAuditCage.getFromDate()))
					|| (employee.getAttestationDate()!=null && !employee.getAttestationDate().before(newAuditCage.getFromDate()))) {
				return newAuditCage;
			}
		}
		
		if (rank != null) {
			if (rank.getRankCode().equals(Do2Manager.RECRUIT) || 
				rank.getRankCode().equals(Do2Manager.HONYCP) ||
				rank.getRankCode().equals(Do2Manager.HONYLT)){
				;
			} else {
				List<Allowance> pmhaAlwList = Utils	.getApplicableAllowanceRates(Do2Manager.PMHAL, newAuditCage.getDo2().getFromDate(), 
						newAuditCage.getDo2().getFromDate(), gsoCode, (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) ? null : Utils.getPao()));
				if (pmhaAlwList.size() > 0) {
					newAuditCage.setPmha((int) pmhaAlwList.get(0).getRateAmount());
				}
			}
		}
			
		if (newAuditCage.getDo2().getDescription().equals("POSIN") && newAuditCage.getDo2().getColumnA()!=null 
				&& newAuditCage.getDo2().getColumnA().equals("DANTAK")){
			newAuditCage.setDeputationStatus(Do2Manager.DEPU_DANTAK_BHUTAN+"");			
		}
		if (newAuditCage.getDo2().getDescription().equals("POSOUT") && (newAuditCage.getDeputationStatus()!=null && 
			newAuditCage.getDeputationStatus().equals(Do2Manager.DEPU_DANTAK_BHUTAN+""))){
			newAuditCage.setDeputationStatus(null);
		}
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) { 
		reverseUpdateEmployeeServiceDetailForEarlierDo2(refDo2);
		if (refDo2.getDescription().equals("POSIN") && refDo2.getColumnA()!=null && refDo2.getColumnA().equals("DANTAK")) {
			AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.DEPU_DANTAK_BHUTAN);
		}
		List<EmployeeServiceDetail> esdApptList = esdRepo.findByFromDateEqualStatusAndDescription(Utils.getCurrentEmployee(refDo2.getArmyNo()).getId(),
																			"APPT",DateManager.getPreviousDate(refDo2.getFromDate()),"A");
				
			/*	Utils.getEntityManager().createQuery("SELECT esd FROM EmployeeServiceDetail esd "
								+ " where esd.employee=:employee and esd.do2.description='APPT' AND "
								+ " esd.toDate=:toDate AND esd.status='A' ").setParameter("toDate",	DateManager.getPreviousDate(refDo2.getFromDate()))
								.setParameter("employee", Utils.getCurrentEmployee(refDo2.getArmyNo())).getResultList();*/
		if (esdApptList.size()>0) {
			List<EmployeeServiceDetail> esdPosinList = esdRepo.findByEitherDescriptionAndFromDateGreaterThanAndStatus(Utils.getCurrentEmployee(refDo2.getArmyNo()).getId(),
					"POSIN","POSOUT",DateManager.getPreviousDate(refDo2.getFromDate()),"I");
					
			/*		Utils.getEntityManager().createQuery("SELECT esd FROM EmployeeServiceDetail esd "
									+ " where esd.employee=:employee and (esd.do2.description='POSIN' OR esd.do2.description='POSOUT') AND "
									+ " esd.fromDate > :fromDate AND esd.status='I' ORDER BY esd.fromDate ")
						.setParameter("fromDate", DateManager.getPreviousDate(refDo2.getFromDate())).setParameter("employee",Utils.getCurrentEmployee(refDo2.getArmyNo())).getResultList();*/

			if (esdPosinList.size()>0)
				esdApptList.get(0).setToDate(DateManager.getPreviousDate(esdPosinList.get(0).getFromDate()));
		}
		return Do2Manager.SUCCESS;
	}
	 
	private void reverseUpdateEmployee(Do2 refDo2) {

		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		List<EmployeeServiceDetail> serviceDetailList = Utils.getEmployeeServiceDetails(refDo2, Do2Manager.ESD_POSIN);

		if (serviceDetailList.size()>0) { // (>1 changed.) There are at least two POSINs after ENROLL			
			if (serviceDetailList.get(0).getDo2().getDescription().equals(Do2Manager.POSIN)) {
 				employee.setUnit(serviceDetailList.get(0).getDo2().getUnit());
				employee.setPresentUnitDate(serviceDetailList.get(0).getFromDate());
			} else if (serviceDetailList.get(0).getDo2().getDescription().equals("POSOUT") && serviceDetailList.get(0).getDo2().getColumnC()!=null){ // if Do2 Description is POSOUT
	 			
				unit=unitRepo.findByUnitCode(serviceDetailList.get(0).getDo2().getColumnC().trim());
			//	unit = (Unit) Utils.getEntityManager().createQuery("SELECT unit FROM Unit unit WHERE unit.unitCode=:unitCodeInCol7C")
			//			.setParameter("unitCodeInCol7C",serviceDetailList.get(0).getDo2().getColumnC().trim()).getSingleResult();
				employee.setUnit(unit);
				employee.setPresentUnitDate(serviceDetailList.get(0).getFromDate());
			}
		} else { // POSIN immediately after ENROLL is getting canceled
			List<Do2> do2List = do2Repo.findByDescStatusFromDateLess(employee.getId(),"V","ENROLL",refDo2.getFromDate());
					
			/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee AND "
									+ " d.description='ENROLL' AND d.fromDate<:fromDate AND d.status='V' ORDER BY d.fromDate desc")
									.setParameter("employee", employee).setParameter("fromDate", refDo2.getFromDate()).getResultList();*/
	 		if (do2List.size()>0) {
				employee.setUnit(do2List.get(0).getUnit());
				employee.setPresentUnitDate(do2List.get(0).getFromDate());
			} else if (employee.getPreviousUnit()!=null){
					employee.setUnit(employee.getPreviousUnit());
					employee.setPresentUnitDate(employee.getPreviousUnitDate());
			}
		}		
		empRepo.save(employee);
		 
	}

	private void reverseTransferGrant(Do2 refDo2) {
		String refDo2No = Utils.getReferenceDo2No(refDo2);		
		List<Do2> do2List = do2Repo.findByEitherDescriptionAndColBAndFromDateAndRefDo2(Utils.getCurrentEmployee(refDo2.getArmyNo()).getId(),
																refDo2No, refDo2.getFromDate(), Do2Manager.TRANSFER_GRANT_HIGH, Do2Manager.TRANSFER_GRANT_LOW, "INDMOVE");
				
	/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:employee and (d.refDo2No is null OR d.refDo2No=:refDo2No) AND d.fromDate=:fromDate "
						+ "	AND (d.description=:description OR d.description=:description1) AND (d.columnB!=null and d.columnB='INDMOVE') AND d.status='V' ")
						.setParameter("refDo2No", refDo2No).setParameter("fromDate", refDo2.getFromDate())
						.setParameter("description", Do2Manager.TRANSFER_GRANT_HIGH).setParameter("description1", Do2Manager.TRANSFER_GRANT_LOW)				 
						.setParameter("employee",Utils.getCurrentEmployee(refDo2.getArmyNo())).getResultList();*/
    	if (do2List!=null && do2List.size()>0) {
 			PayCode transferGrant = Utils.getPayCode(do2List.get(0).getDescription());
			List<Double> transferGrants = arrRepo.findByEmployeeIdPayCodeAndDo2(Utils.getCurrentEmployee(refDo2.getArmyNo()).getId(),
																					do2List.get(0), transferGrant);
					
					
				/*	Utils.getEntityManager().createQuery(
					"SELECT SUM(a.amount) FROM Arrears a WHERE a.employee=:employee and a.do2=:reverseDo2 AND "
							+ " a.payCode=:payCode group by a.payCode")
					.setParameter("reverseDo2", do2List.get(0)).setParameter("payCode", transferGrant)
					.setParameter("employee",Utils.getCurrentEmployee(refDo2.getArmyNo())).getResultList();*/
               
			if (transferGrants.size() > 0) {
				Do2 tfrgtDo2 = do2List.get(0);
				tfrgtDo2.setReviewQe(Utils.getCurrentQuarterEnding());					
				addArrearsForCancelDo2(tfrgtDo2, transferGrants.get(0)*-1, do2List.get(0).getDescription(), refDo2.getFromDate(), refDo2.getFromDate());				
				if (refDo2.getStatus().equals(Do2Manager.DO2_CANCEL_STATUS)){
					tfrgtDo2.setStatus(Do2Manager.DO2_CANCEL_STATUS);
					tfrgtDo2.setReason("This Part II order is cancelled based on Cancellation Posin Part II Order   "+refDo2.getDescription()+" "+refDo2.getFromDate());
				}
				do2Repo.save(tfrgtDo2);
				 
			}
		}
	}

	private void reverseUpdateEmployeeServiceDetailForEarlierDo2(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		boolean employeeReverseUpdated=false;
		EmployeeServiceDetail follow = null;
		EmployeeServiceDetail current = null;

		do2List = do2Repo.findByDescFromDateEqualIdNotEqual(employee.getId(),"V","POS%",do2.getFromDate(),do2.getId());
				
		/*		Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 WHERE do2.employee=:employee AND do2.id!=:do2Id " +
				" AND (do2.description like 'POS%') AND do2.fromDate=:fromDate AND status='V' ")
				.setParameter("fromDate", do2.getFromDate()).setParameter("do2Id", do2.getId()).setParameter("employee",employee).getResultList();*/

		List<EmployeeServiceDetail> esdList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(), "I");
				
		/*		Utils.getEntityManager().createQuery("SELECT e FROM EmployeeServiceDetail e " +
				" WHERE e.status='I' AND e.employee=:emp ORDER BY e.fromDate DESC, e.toDate DESC ")	.setParameter("emp", employee).getResultList(); */

		if (do2.getDescription().equals("POSIN")) {
			reverseTransferGrant(do2);
			
			if (do2.getUnit()!=null && employee.getUnit()!=null && do2.getUnit().equals(employee.getUnit())) {
				if (employee.getPreviousUnit()!=null && employee.getPresentUnitDate()!=null) {
					employee.setUnit(employee.getPreviousUnit());
					employee.setPresentUnitDate(employee.getPreviousUnitDate());
					empRepo.save(employee);
					 
					employeeReverseUpdated=true;
		 		}
			}
			
			int listSize = esdList.size();
			for (int i = 0; i < listSize; i++) {
				current = esdList.get(i);
				if (do2.getFromDate().before(DateManager.getRevisedAppendixJImplementationDate())) {				
	 				if (do2List.size() > 0) {
	 					if (do2List.get(0).getDescription().equals("POSOUT")) {
	 						if (current.getDo2().getId()==do2.getId() && do2List.get(0).getColumnC()!=null && do2List.get(0).getColumnC().length()==6) {
	 							current.setDo2(do2List.get(0));
	 				 			break;
	 						}	
	 					} else if (do2List.get(0).getDescription().equals("POSIN")) {
			 						if (current.getDo2().getId()==do2.getId()) {
			 							esdRepo.delete(current);
			 						//	Utils.getEntityManager().remove(current);
			 							 
			 							if (!employeeReverseUpdated)
			 								reverseUpdateEmployee(do2);
			 							if (i==0 && i<(listSize-1)) {
			 								esdList.get(i+1).setToDate(null);
			 								break;
			 							}
			 							if (i>0 && i<(listSize-1)) {
			 								follow = esdList.get(i + 1);
			 								follow.setToDate(DateManager.getPreviousDate(esdList.get(i-1).getFromDate()));
			 								break;
			 							}
			 						}
			 					}
		 				} else {
		 			 		if (current.getDo2().getId()==do2.getId()) {
		 			 			esdRepo.delete(current);
							//	Utils.getEntityManager().remove(current);
							//	Utils.getEntityManager().flush();
			 					if (!employeeReverseUpdated)
									reverseUpdateEmployee(do2);
								if (i==0 && i<(listSize-1)) {
									esdList.get(i+1).setToDate(null);
									break;
								}
								if (i>0 && i<(listSize-1)) {
									follow = esdList.get(i + 1);
									follow.setToDate(DateManager.getPreviousDate(esdList.get(i-1).getFromDate()));
									break;
								}
							}		
		 				}
				} else {
					if (current.getDo2().getId()==do2.getId()) {
						esdRepo.delete(current);
					//	Utils.getEntityManager().remove(current);
					//	Utils.getEntityManager().flush();
		 				if (!employeeReverseUpdated)
							reverseUpdateEmployee(do2);
						if (i==0 && i<(listSize-1)) {
							esdList.get(i+1).setToDate(null);
							break;
						}
						if (i>0 && i<(listSize-1)) {
							follow = esdList.get(i + 1);
							follow.setToDate(DateManager.getPreviousDate(esdList.get(i-1).getFromDate()));
							break;
						}
					}
					
				}
			}         
		}

		if (do2.getDescription().equals("POSOUT") && do2.getColumnC()!=null && do2.getColumnC().length()==6) {
			int listSize = esdList.size();
			for (int i = 0; i < listSize; i++) {
				current = esdList.get(i);
 				if (do2List.size() > 0) {
                    if (do2List.get(0).getDescription().equals("POSIN")) {
	 					if (current.getDo2().getId()==do2.getId())  {
	 						current.setDo2(do2List.get(0));
							break;
						}     
					} else if (do2List.get(0).getDescription().equals("POSOUT") && do2List.get(0).getColumnC()!=null) {
	 					if (current.getDo2().getId()==do2.getId()) {
	 						esdRepo.delete(current);
	 					//	Utils.getEntityManager().remove(current);
	 					//	Utils.getEntityManager().flush();
	 						if (!employeeReverseUpdated)
	 							reverseUpdateEmployee(do2);						
							if (i==0 && i<(listSize-1)) {
								esdList.get(i+1).setToDate(null);
								break;
							}
							if (i>0 && i<(listSize-1)) {
								follow = esdList.get(i + 1);
								follow.setToDate(DateManager.getPreviousDate(esdList.get(i-1).getFromDate()));
								break;
							}
						}
					}
 				}
			}
		}
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}

	
	public boolean isNe(Employee employee, Do2 do2){
		List<LpcRegister> lpcList= lpcRepo.findByRecordStatusEntryApprovedAndEcsGenerated(employee.getId());
				
		/*		Utils.getEntityManager().createQuery("select l FROM LpcRegister l where l.employee=:emp and l.recordStatus='F' "
				+ " and l.entryApproved=true and l.ecsGenerated=true ").setParameter("emp",employee).getResultList();*/
		if (lpcList.size() > 0) {
			if (lpcList.get(0).getDischargeDate()!=null && do2.getFromDate().after(lpcList.get(0).getDischargeDate())) {
				return true;
			}
		}
		 /*if (lpcList.size()>0) {
			 List<Do2>do2List = Utils.getEntityManager().createQuery("select d FROM Do2 d where d.employee=:emp and d.d.status='V' and d.columnA='FS' ")
				.setParameter("emp", employee).getResultList();
			 if (do2List.size()>0) { 
				 if (!do2.getFromDate().before(do2List.get(0).getFromDate())) {
					return true;
				 }
			 }
		 }*/
		 return false;
	}
			 
	static public void addArrearsForCancelDo2(Do2 do2, double amount, String payCode, Date fromDate, Date toDate) {		
				
		if (amount==0.0)
			return;
		
		Arrears arrears = new Arrears();
		arrears.setDo2(do2);
		arrears.setFromDate(fromDate);
		arrears.setToDate(toDate);
		arrears.setAmount((double) Math.round(amount));
		arrears.setPayCode(Utils.getPayCode(payCode));
		arrears.setQe(Utils.getCurrentQuarter());
		arrears.setDo2ReverseArrears(do2);
		arrears.setRoundOffAmount(false);
		arrears.setRegularPay(false);
		arrears.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
		arrears.setArrestPayment(false);
		arrears.setPaid(false);
		arrears.setCreatedAt(new Timestamp(System.currentTimeMillis()));
	//	arrRepo.save(arrears);
		//Utils.getEntityManager().persist(arrears);
		//Utils.getEntityManager().flush();
	}	
}
