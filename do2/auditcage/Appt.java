package itsdc.dolphin.do2.auditcage;


import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AppointmentRate;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
 
 
@RestController
public class Appt extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
	static public final String IS_GETTING_APPOINTMENT_PAY = "AU:Already drawing Extra duty allowance. \n";
	static public final String IS_NCO = "AU:PBOR is not NCO. \n";
	static public final String NOT_ELIGIBLE_APPOINTMENT = "AU:Not an eligible appointment for the Rank";
	Employee employee;
	Pao pao;
	AppointmentRate appointmentRate;

	@Override
	public boolean isValid(Do2 do2) {
		StringBuffer buffer = new StringBuffer();
		boolean validity = true;
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (!isRateAvailable(do2)) {
			validity = false;
			if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				buffer.append(" APPT PAY IS MERGED WITH EXTWK (EXTRA WORK ALLOWANCE) w.e.f 01-07-2017");
			} else {
				buffer.append(Do2Manager.RATE_NOT_AVAILABLE);
			}
			
		}
		
		if (!isNco(do2)) {
			validity = false;
			buffer.append(IS_NCO);
		}
		if (appointmentRate != null) {
			if (!isEligibleAppointment(do2)) {
				validity = false;
				buffer.append(NOT_ELIGIBLE_APPOINTMENT);
			}
		}

		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());

		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
		do2Repo.save(do2);
	return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);	 
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			appointmentRate = getAppointmentRate(do2);
		AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.APTPAY,
				appointmentRate.getRate());
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_APPT+"");
		if (employee.getAppointmentDate() == null
				|| (employee.getAppointmentDate() != null && !do2.getFromDate()
						.before(employee.getAppointmentDate()))) {
			updateBdtInfo(do2);
		}

		return Do2Manager.SUCCESS;

	}

	public boolean isRateAvailable(Do2 do2) {

		return getAppointmentRate(do2) != null;
	}

	public boolean isGettingAppointmentPay(Do2 do2) {
        
		if ((employee.getAppointmentRate() == null)
				|| (employee.getAppointmentRate() != null && employee
						.getAppointmentRate().getAppointmentName().equals(
								"TSLNK")) || (do2.getColumnA().equals("TSLNK"))) {

			return false;
		} else if (employee.getAppointmentDate() !=null  && do2.getFromDate().after(employee.getAppointmentDate())) {
			if(employee.getAppointmentRate()!=null 
					&& !employee.getAppointmentRate().getAppointmentName().equals(do2.getColumnA())){
				return false;
			}
			return true;
		} else {
			List<EmployeeServiceDetail> serviceDetailList = Utils
					.getEmployeeServiceDetails(do2, Do2Manager.ESD_APPT);
			 if(serviceDetailList.size()>0){
				       if(serviceDetailList.get(0).getDo2().getColumnA() !=null 
				    		   && serviceDetailList.get(0).getDo2().getColumnA().equals(do2.getColumnA())){
				    	   return true;
				       }
			 }			
		}

		return false;
	}

	private boolean isNco(Do2 do2) {
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (rank == null
				|| !(Integer.parseInt(rank.getRankCode()) < Do2Manager.JCO_RANK_CODE))
			return false;

		return true;
	}

	private boolean isEligibleAppointment(Do2 do2) {
         //TODO check for null rank
		if (appointmentRate == null)
			appointmentRate = getAppointmentRate(do2);
		
		Rank rankOnFromDate = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
	//	System.out.println ( " RANK IN APPT " +rankOnFromDate.getRankCode());
	
		// The following lines commented  and above line added 'rankOnFromDate' 
		//since APPT PAY is admissble on grant of MACPS also
		// Authority : AT/I/3500/Dolphin/Vol.VII dated 14-12-2016.
		//Rank rankOnFromDate = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		
		if (rankOnFromDate==null){
			return false;
		}
		if (appointmentRate != null && rankOnFromDate != null &&
			appointmentRate.getRank().getRankCode().equals(rankOnFromDate.getRankCode())) {
			if (rankOnFromDate.getRankCode().equals("30")) {
				if (do2.getEmployee().getNaikDate() == null && do2.getEmployee().getAcpnaikDate() != null){
					return false;
				} else if (do2.getEmployee().getNaikDate() != null) {
					return true;
				} else {
					return false;
				}
			}
			/*if (rankOnFromDate.getRankCode().equals("40")) {
				if (do2.getColumnA().equals("HMT")) {
					System.out.println ( " RANK IN APPT111 " +rankOnFromDate.getRankCode());
					if (Utils.getPao().getPaoCode().equals("88")  && do2.getEmployee().getMusterHavildarFlag() == true &&
						!do2.getFromDate().before(DateManager.getRevisedMsthavImplDate())){
						return true;
					} else {  
						return false;
					}
				} 
				if (do2.getEmployee().getHavDate() == null){
					return false;
				} else {
					return true;
				}
			}*/
			
			// above code amended as stated below in view of amendment NO.24  to appendix_j for description MSTHAV
			
			if (rankOnFromDate.getRankCode().equals("40")) {
				if (do2.getColumnA().equals("HMT")) {
				//	System.out.println ( " RANK IN APPT111 " +rankOnFromDate.getRankCode());
					if (Utils.getPao().getPaoCode().equals("88")) {
						if (do2.getEmployee().getRemusteringDate()!=null && 
								!do2.getFromDate().before(do2.getEmployee().getRemusteringDate())) {
							return true;
						} else if (do2.getEmployee().getMusterHavildarDate()!=null && 
								!do2.getFromDate().before(do2.getEmployee().getMusterHavildarDate()) &&
								do2.getEmployee().getMusterHavildarFlag() == true &&
								!do2.getFromDate().before(DateManager.getRevisedMsthavImplDate())) {	
									return true;
							} else {
								return false;
							}
					}
				}
			}
			
			
			return true;		
		}		
		return false;	
	}
	
	private AppointmentRate getAppointmentRate(Do2 do2) {
		List<AppointmentRate> appointmentRateList=null;
		Pao pao = Utils.getPao();
		
		if(pao.getPaoCode().equals(Do2Manager.PAO_NASIK) && do2.getColumnA().equals(Do2Manager.PAO_NASIK_APPOINMENT)){
			pao = Utils.getPao();
		}else{
			pao= null;
		}
		String colA = do2.getColumnA();
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			if (do2.getColumnA().equals("CHM")){
				colA="CAL";
			} else if (do2.getColumnA().equals("BHM")){
				colA="BAL";
			} else if (do2.getColumnA().equals("CQMH")){
				colA="CALQ";
			} else if (do2.getColumnA().equals("BQMH")){
				colA="BALQ";
			}
		}
        if(pao == null){
        	appointmentRateList = apptRateRepo.findByApptNameFromDateBetweenOrFromDateGreater(colA, do2.getFromDate());
        	 
        }else{
        	 appointmentRateList =apptRateRepo.findByApptNamePaoIdFromDateBetweenOrFromDateGreater(colA,pao.getId(),do2.getFromDate());
        	 
        }
		
		
		if (appointmentRateList.size() > 0) {
			appointmentRate = appointmentRateList.get(0);

		}
		return appointmentRate;
	}

	private void updateEmployee(Do2 do2) {
			employee =do2.getEmployee();

			employee.setAppointmentRate(appointmentRate);
			employee.setAppointmentDate(do2.getFromDate());
	}

	private void reverseUpdateEmployee(Do2 refdo2) {
		List<EmployeeServiceDetail> serviceDetailList = Utils
				.getEmployeeServiceDetails(refdo2, Do2Manager.ESD_APPT);
		if (serviceDetailList.size() > 0) {
			String previousAppointment = serviceDetailList.get(0).getDo2()
					.getColumnA();
			employee.setAppointmentRate(apptRateRepo.findByAppointmentName(previousAppointment));
			employee.setAppointmentDate(serviceDetailList.get(0).getFromDate());

		} else {

			employee.setAppointmentRate(null);
			employee.setAppointmentDate(null);
		}
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		
		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		
		if (employee.getAppointmentRate() == null
				|| (employee.getAppointmentDate() != null && !refDo2
						.getFromDate().before(employee.getAppointmentDate()))) {
			reverseUpdateEmployee(refDo2);
		}

		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,
				Do2Manager.ESD_APPT);
		return Do2Manager.SUCCESS;
	}
	@Override 
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}

}
