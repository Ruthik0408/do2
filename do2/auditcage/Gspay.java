package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
 
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.RestController;

 

@RestController
public class Gspay extends AuditCageBase implements Do2AccountingCageProcessor,
		Do2AuditCageProcessor {
	Employee employee;
	String status = Do2Manager.VALID;
	Allowance allowance = null;
	List<Allowance> alwList = null;
	Rank rank;

	private StringBuffer buffer = new StringBuffer();

	static public String IS_RECRUIT = "AU: PBOR is a Recruit.\n";
	static public String NOT_VALID_RANK = "AU:PBOR is not NCO\n";
	static public String NOT_VALID_QS = "AU:PBOR did not complete the required service.\n";
	static public String NOT_VALID_RATE = "AU:PBOR already drawing GSPAY.\n";
	static public String TA_DEPTAL_EMP = "AU:PBOR belongs to TA deptal category - not entitled\n";
	static public String APPOINTMENT_CATG_MISSING = "AU:Appointment Category for TA not available \n";
	static public String NOT_VALID_COOLING_PERIOD = "AU:Qualifying service between rates less than 12 months.\n";
	static public String RED_INK_ENTRY = "AU:Qualifying Service after last Red Ink Entry less than 12 months.\n";

	@Override
	public boolean isValid(Do2 do2) {
		buffer.setLength(0);
		boolean validity = true;
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (employee.getRegularTaFlag().equals(Do2Manager.TERRITORIAL_ARMY) && 
			employee.getTaAppointmentCategory() == null){
			validity=false;
		    buffer.append(APPOINTMENT_CATG_MISSING);
    	}
			
		if (employee.getRegularTaFlag().equals(Do2Manager.TERRITORIAL_ARMY) && 
		   (employee.getTaAppointmentCategory() != null && 
			employee.getTaAppointmentCategory().equals("D"))){
			validity=false;
			buffer.append(TA_DEPTAL_EMP);
		}
		
		if ((employee.getRegularTaFlag().equals(Do2Manager.TERRITORIAL_ARMY) && !employee.getTaAppointmentCategory().equals("D")) ||
		    (employee.getRegularTaFlag().equals("R"))){
			if (!(isServiceDetailAvailable())) {
				validity = false;
			//	do2.setStatus(Do2Manager.PENDING);
				buffer.append(Do2Manager.MISSING_SERVICE_DETAIL);
			}
			if (isRateAvailable(do2)) {
				validity = false;
			//	do2.setStatus(Do2Manager.PENDING);
				buffer.append(Do2Manager.RATE_NOT_AVAILABLE);
			}
			if (!(isValidRank(do2))) {
				validity = false;
				buffer.append(NOT_VALID_RANK);
			}
			if (!(isValidCode(do2))) {
				validity = false;
			//	do2.setStatus(Do2Manager.PENDING);
				buffer.append(NOT_VALID_RATE);
			}
		}
		
		if (employee.getRegularTaFlag().equals("R")){
			if (!(isQualifyingService(do2))) {
				validity = false;
				buffer.append(NOT_VALID_QS);
			}
		
			if (!(do2.getColumnA().equals("FIRST"))) {
				if (!isCoolingPeriod(do2)) {
					validity = false;
					buffer.append(NOT_VALID_COOLING_PERIOD);
				}
			}
		}
	
		if (!validity) {
			
				do2.setStatus(Do2Manager.PENDING);
			
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setReason(buffer.toString());
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
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		updateBdtInfo(do2);
	
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
 
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnA(), 
				                                                    do2.getFromDate(), do2.getFromDate());
		if (alwList.size() > 0) {
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2
					.getDescription(), alwList.get(0).getRateAmount());
		}

		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		restoreServiceDetails(refDo2);
		restoreEmployee(refDo2);
		return Do2Manager.SUCCESS;
	}

	// *******************************************************************************

	public boolean isServiceDetailAvailable() {
		if (rank == null)
			return false;
		else
			return true;
	}

	public boolean isRateAvailable(Do2 do2) {
		return getAllowanceRate(do2) != null;
	}

	public boolean isValidRank(Do2 do2) {
		boolean rankValidity = false;
		if (rank == null
				&& employee.getSubstantiveRank().getRankCode().equals(
						Do2Manager.RECRUIT)) {
			rankValidity = false;
		}
		if ((do2.getColumnA().equals("FIRST") || do2.getColumnA().equals(
				"SECOND"))
				&& ((rank.getRankCode().equals(Do2Manager.NK) || rank
						.getRankCode().equals(Do2Manager.HAV)))) {
			rankValidity = true;
		}

		if (do2.getColumnA().equals("THIRD")
				&& (rank.getRankCode().equals(Do2Manager.HAV))) {
			rankValidity = true;
		}

		return rankValidity;
	}

	public boolean isQualifyingService(Do2 do2) {
		boolean qsValidity = false;
		
 		Date rankDate = getRankDate(do2, Utils.getCurrentEmployee(do2.getArmyNo()));
		if(rankDate ==null)
			return false;
	 	if (do2.getColumnA().equals("FIRST")) {
			if (DateManager.getNoOfMonths(rankDate, do2.getFromDate()) >= 36.0) {
				qsValidity = true;
			}
		}
		if (do2.getColumnA().equals("SECOND")) {
			if (DateManager.getNoOfMonths(rankDate, do2.getFromDate()) >= 72.0) {
				qsValidity = true;
			}
		}
		if (do2.getColumnA().equals("THIRD")) {
			if (DateManager.getNoOfMonths(rankDate, do2.getFromDate()) >= 108.0) {
				qsValidity = true;
			}
		}
		return qsValidity;
	}

	/*
	 * public boolean isValidCode(Do2 do2) { try { if
	 * (do2.getFromDate().before(DateManager.getDate("2001-09-01"))) { return true;
	 * } } catch (Exception e) { Utils.handleException(e); }
	 * List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetails(do2,
	 * Do2Manager.ESD_GSPAY);
	 * 
	 * if (esdList.size()==0) { if (do2.getColumnA().equals("FIRST") &&
	 * employee.getCurrentGspayCode()==null) { return true; } else if
	 * (do2.getColumnA().equals("SECOND") && employee.getCurrentGspayCode()!=null &&
	 * employee.getCurrentGspayCode().equals("1") &&
	 * employee.getCurrentGsPayDate().before(do2.getFromDate())) { return true; }
	 * else if (do2.getColumnA().equals("THIRD") &&
	 * employee.getCurrentGspayCode()!=null &&
	 * employee.getCurrentGspayCode().equals("2") &&
	 * employee.getCurrentGsPayDate().before(do2.getFromDate())) { return true; } }
	 * 
	 * if (esdList.size()==0) { if (do2.getColumnA().equals("FIRST")) { return true;
	 * } } else if (do2.getColumnA().equals("SECOND")){ if
	 * (esdList.get(0).getDo2().getColumnA().equals("FIRST")) { return true; } }
	 * else if (do2.getColumnA().equals("THIRD")){ if
	 * (esdList.get(0).getDo2().getColumnA().equals("SECOND")) { return true; } }
	 * 
	 * if(esdList.size() > 0) NOT_VALID_RATE = NOT_VALID_RATE
	 * +"   "+esdList.get(0).getDo2().getColumnA()+"  FROM   "+esdList.get(0).getDo2
	 * ().getFromDate(); return false; }
	 */
	public boolean isValidCode(Do2 do2) {

        try {
            if (do2.getFromDate().before(DateManager.getDate("2001-09-01"))) {
                return true;
            }
        } catch (Exception e) {
            Utils.handleException(e);
        }
        List<EmployeeServiceDetail> allEsdList =
                Utils.getEmployeeServiceDetails(do2, Do2Manager.ESD_GSPAY);
        List<EmployeeServiceDetail> gspayEsdList = allEsdList.stream()
                .filter(esd ->
                        esd.getDo2() != null &&
                                esd.getDo2().getDescription() != null &&
                                esd.getDo2().getDescription().toUpperCase().contains("GSPAY")
                )
                .toList();
        if (gspayEsdList.isEmpty()) {

            if ("FIRST".equals(do2.getColumnA())
                    && employee.getCurrentGspayCode() == null) {
                return true;

            } else if ("SECOND".equals(do2.getColumnA())
                    && "1".equals(employee.getCurrentGspayCode())
                    && employee.getCurrentGsPayDate().before(do2.getFromDate())) {
                return true;

            } else if ("THIRD".equals(do2.getColumnA())
                    && "2".equals(employee.getCurrentGspayCode())
                    && employee.getCurrentGsPayDate().before(do2.getFromDate())) {
                return true;
            }
        }
        if (gspayEsdList.isEmpty()) {

            if ("FIRST".equals(do2.getColumnA())) {
                return true;
            }

        } else {

            Do2 lastGspayDo2 = gspayEsdList.get(0).getDo2();

            if ("SECOND".equals(do2.getColumnA())
                    && "FIRST".equals(lastGspayDo2.getColumnA())) {
                return true;

            } else if ("THIRD".equals(do2.getColumnA())
                    && "SECOND".equals(lastGspayDo2.getColumnA())) {
                return true;
            }

            NOT_VALID_RATE = NOT_VALID_RATE + "   "
                    + lastGspayDo2.getColumnA()
                    + "  FROM   "
                    + lastGspayDo2.getFromDate();
        }

        return false;
    }

	

	public boolean isCoolingPeriod(Do2 do2) {
		
		try {
			if (do2.getFromDate().before(DateManager.getDate("2001-09-01"))) {
				return true;
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}
		List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetails(
				do2, Do2Manager.ESD_GSPAY);
      	if (do2.getColumnA().equals("FIRST"))
			return true;
	/*	if (employee.getCurrentGsPayDate() == null)
			return false;
	*/	if (esdList.size() > 0) {
			if (DateManager.getNoOfMonths(do2.getFromDate(), esdList.get(0).getFromDate()) > 12.0) {
				return true;
			}
		} else {
			  if (do2.getColumnA().equals("SECOND") || do2.getColumnA().equals("THIRD")) {
				  if (employee.getCurrentGsPayDate()!=null) {
					  if (DateManager.getNoOfMonths(do2.getFromDate(), employee.getCurrentGsPayDate()) > 12.0) {
						  return true; 
					  }
				  }
				  
			  }
				
		}
	
		return false;

	}

	public boolean isRedEntry(Do2 do2) {
		boolean rivalidity = false;
		Calendar cal1 = new GregorianCalendar();
		cal1.setTime(do2.getFromDate());
		cal1.set(Calendar.YEAR, cal1.get(Calendar.YEAR) - 2);
		Date previousTwoYears = new Date();
		previousTwoYears.setTime(cal1.getTimeInMillis());
        Date twoYearStartDate = DateManager.getNextDate(previousTwoYears);
		Calendar cal2 = new GregorianCalendar();
		cal2.setTime(do2.getFromDate());
		cal2.set(Calendar.YEAR, cal2.get(Calendar.YEAR) - 1);
		Date previousOneYear = new Date();
		previousOneYear.setTime(cal2.getTimeInMillis());
		Date oneYearStartDate = DateManager.getNextDate(previousOneYear);
		
		List<EmployeeServiceDetail> esd =esdRepo.findByFromDateBetweenOrderByFromDate(employee.getId(), "P", twoYearStartDate,do2.getFromDate());
				
		/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT esd FROM EmployeeServiceDetail esd WHERE"
								+ " esd.employee=:employee AND "
								+ " esd.status=:status AND"
								+ " esd.fromDate between :pdt AND :fdt ORDER BY esd.fromDate")
				.setParameter("employee",employee).setParameter("status",
						'P').setParameter("pdt", twoYearStartDate)
				.setParameter("fdt",do2.getFromDate()).getResultList();*/

		List<EmployeeServiceDetail> esd1 = esdRepo.findByFromDateBetweenOrderByFromDate(employee.getId(), "P", oneYearStartDate,do2.getFromDate());
				
		/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT esd FROM EmployeeServiceDetail esd WHERE"
								+ " esd.employee=:employee AND "
								+ " esd.status=:status AND"
								+ " esd.fromDate between :pdt AND :fdt ORDER BY esd.fromDate")
				.setParameter("employee", employee).setParameter("status",
						'P').setParameter("pdt", oneYearStartDate).setParameter(
						"fdt", do2.getFromDate()).getResultList();*/
        System.out.println("esd size  "+esd1.size()+"  "+esd.size()+"  "+twoYearStartDate+"  "+oneYearStartDate);		
		if (do2.getColumnA().equals("FIRST")) {
			if (esd.size() > 2 || esd1.size() > 1) {
				rivalidity = true;
				return true;
			}
		} else {
			if (esd1.size() > 0) {
				rivalidity = true;
				return true;
			}
		}
		
		return false;
	}

	public void updateEmployee(Do2 do2) {
		if (do2.getColumnA().equals("FIRST"))
			employee.setCurrentGspayCode("1");
		else if (do2.getColumnA().equals("SECOND"))
			employee.setCurrentGspayCode("2");
		else if (do2.getColumnA().equals("THIRD"))
			employee.setCurrentGspayCode("3");
		employee.setCurrentGsPayDate(do2.getFromDate());
		empRepo.save(employee);
	//	Utils.getEntityManager().persist(employee);
	}

	public void updateServiceDetails(Do2 do2) {
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_GSPAY+"");
	}

	public void restoreEmployee(Do2 refDo2) {
		String gspayColumnA = "";
		List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetails(
				refDo2, Do2Manager.ESD_GSPAY);
		if (esdList.size() > 0) {
			gspayColumnA = esdList.get(0).getDo2().getColumnA();
		}
		if (esdList.size() > 0) {
			if (gspayColumnA.equals("SECOND")) {
				employee.setCurrentGspayCode("2");
				employee.setCurrentGsPayDate(esdList.get(0).getDo2()
						.getFromDate());
			}
			if (gspayColumnA.equals("FIRST")) {
				employee.setCurrentGspayCode("1");
				employee.setCurrentGsPayDate(esdList.get(0).getDo2()
						.getFromDate());
			}
		} else {
			employee.setCurrentGspayCode(null);
			employee.setCurrentGsPayDate(null);

		}

	}

	public void restoreServiceDetails(Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,
				Do2Manager.ESD_GSPAY);
	}

	public Allowance getAllowanceRate(Do2 do2) {
		// allowance = (Allowance)
		// Utils.getApplicableAllowanceRates(do2.getDescription(),
		// do2.getColumnA(), do2.getFromDate(),do2.getFromDate());
		/*
		 * alwList = Utils .getEntityManager() .createQuery(
		 * "select alwlist from AllowanceRate alwlist where " +
		 * "alwlist.allowanceName=:allowName and alwlist.subAllowanceName=:" +
		 * "suballowanceName and :fromdate between alwlist.fromDate and alwlist.toDate"
		 * ) .setParameter("allowName", do2.getDescription().trim())
		 * .setParameter("suballowanceName", do2.getColumnA().trim())
		 * .setParameter("fromdate", do2.getFromDate()).getResultList();
		 */
		return allowance;
	}
	
	public Date getRankDate(Do2 do2, Employee employee) {
		List <Rank> ranks = rankRepo.findAll();
		EmployeeServiceDetail esd = null;
		Date rankdate = null;
		Date nkdate = null;
		Date havdate = null;
		if (employee.getNaikDate() != null){
			nkdate = employee.getNaikDate();
		}
		if (employee.getSportHavildarFlag() != null
				&& employee.getSportHavildarFlag() == true) {
			havdate = employee.getSportHavildarDate();
		} else if (employee.getGraduateHavildarFlag() != null
					&& employee.getGraduateHavildarFlag() == true) {
			havdate = employee.getGraduateHavildarDate();
		} else if (employee.getMusterHavildarFlag() != null
				&& employee.getMusterHavildarFlag() == true) {
			havdate = employee.getMusterHavildarDate();
		} else 	if (employee.getHavDate() != null){
			havdate = employee.getHavDate();
		}
		List<EmployeeServiceDetail> employeeServiceDetails = esdRepo.findByDescriptionNotInRemustOwnMstOrderByFromDateToDate(employee.getId(),
																															"R");
				
			/*	Utils.getEntityManager().createQuery(
				" SELECT esd FROM EmployeeServiceDetail esd WHERE esd.status='R' AND esd.employee=:emp AND " +
				" esd.do2.description not in ('REMUST','OWNMST') ORDER BY esd.fromDate, esd.toDate ")
					.setParameter("emp", employee).getResultList();*/
		if (employeeServiceDetails.size() > 0) {
			for (int i = 0; i < employeeServiceDetails.size(); i++) {
				esd = employeeServiceDetails.get(i);	
				if (esd.getDo2().getDescription().equals(Do2Manager.MSTHAV)) {
					if ((havdate == null) || (havdate != null && havdate.after(esd.getDo2().getFromDate()))) {
						 havdate=esd.getDo2().getFromDate();
					}
				}
				if (esd.getDo2().getDescription().equals("PROMOT") || esd.getDo2().getDescription().equals("LATENR")) { 
					for (Rank rank : ranks) {						
						if (Do2Manager.getRankMap().get(esd.getDo2().getColumnA()).equals("NK")) {
							if (esd.getDo2().getDescription().equals("PROMOT")) {
								if (esd.getDo2().getColumnA().equals("PANK")){
									nkdate=esd.getDo2().getFromDate();
								} else if (esd.getDo2().getColumnA().equals("SBNK") && 
										  (nkdate == null || (nkdate != null && !nkdate.before(esd.getFromDate())))){
									nkdate=esd.getDo2().getFromDate();
								}
							} else {
								nkdate=esd.getDo2().getFromDate();
							}
						} else if (Do2Manager.getRankMap().get(esd.getDo2().getColumnA()).equals("HAV")) {
							if (esd.getDo2().getDescription().equals("PROMOT")) {
								if (esd.getDo2().getColumnA().equals("PAHAV")){
									havdate=esd.getDo2().getFromDate();
								} else if (esd.getDo2().getColumnA().equals("SBHAV") && 
										  (havdate == null || (havdate != null && !havdate.before(esd.getFromDate())))){
									havdate=esd.getDo2().getFromDate();
								}
							} else {
								havdate=esd.getDo2().getFromDate();
							}
						}
					}
				}
			}
		}		
		if (nkdate != null) {
			return nkdate;
		} else if (havdate != null) {
			return havdate;
		} else {
			return rankdate;
		}
	}
	@Override 
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
		updateServiceDetails(do2);
	}
}