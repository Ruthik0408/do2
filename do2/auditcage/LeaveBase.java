package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EnrollDo2;
import itsdc.dolphin.models.FamilyDetail;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeaveBase extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	String message;
	Employee employee;
	static public String INVALID_ACCMAL = "AU:Invalid ACCMAL as already availed other than CL.\n ";
	static public String INVALID_REFDO2 = "VL:Col. 8 Reference Do2 No. is blank or Invalid.\n ";
	static public String INVALID_TJRA = "VL:Tjra days not mentioned or more than 8 days.\n";
	static public String INVALID_LRA = "VL:Lra days not mentioned or more than authorised days.\n";
	static public String INVALID_LEAVE_PERIOD = "VL:Leave Period not tallied.\n";
	static public String NOT_COMPLETED_SIXMONTHS = "AU:PBOR is a recruit And not completed 6 month from date of enrolment.\n";
	static public String INADMISSIBLE_LEAVE = "VL: CCL Admissible Only To Female Personnel Or Single Male Personneal.\n";
	static public String NOT_ADMISSIBLE_DURING_TRAINING_PROBATION = "VL: CCL Not Admissible During Training / Probation. \n";
	static public String INADMISSIBLE_PERIOD = "VL: Minimum Period For CCL Is 5 Days And Maximum Is 30 Days. \n";
	static public String EXCEEDED_MAXIMUM_PERIOD = "VL: Total Availed Period Exceeded 360 Days. \n";
	static public String EXCEEDED_MAXIMUM_PERIOD_FOR_CALENDAR_YEAR = "VL: Total Availed Period Exceeded 60 Days In A Calendar Year. \n";
	static public String INADMISSIBLE_PREFIX_SUFFIX_DESCRIPTION = "VL: Prefix/Suffix of CCL Permitted Only With CL. \n";
	static public String INADMISSIBLE_ADOPTION_LEAVE = "VL:Adoption Leave Admissible Only To Female Personnel.\n";
	static public String INADMISSIBLE_MATERNITY_LEAVE = "VL: Maternity Leave Admissible Only To Female Personnel.\n";
	static public String REFERENCE_DO2_REQUIRED = "VL: Reference DOII Number Required.\n";
	static public String INCORRECT_REFERENCE_DO2 = "VL: Incorrect Reference DOII Number Provided.\n";
	static public String TJRA_COLUMN_NOT_BLANK = "VL: TJRA Column Should Be Blank For This Description.\n";
	static public String LRA_COLUMN_BLANK = "VL: LRA Column Should Not Be Blank For This Description.\n";
	static public String INCORRECT_LRA_VALUES = "VL: LRA Column Values can Be Between 30 and 180.\n";
	static public String COOLING_OFF_PERIOD_NOT_COMPLETED = "VL: 24 Months Period Not Completed After Rejoining From Previous Maternity Leave.\n";
	static public String INCORRECT_START_DATE_FOR_EXTENSION = "VL: Extension Start Date Is Not After Completion Of Maternity Leave.\n";
	static public String REFERENCE_DO2NO_REQUIRED = "VL: Reference DOII Number of Maternity Leave Required For Its Extention.\n";
	static public String MATLVE_LIMIT_EXCEEDED = "VL: Maternity Leave Already Availed For Two Children.\n";
	static public String AVAILED_ADOPTION_LEAVE_TWO_CHILDREN = "VL: Already Availed Adoption Leave For Two Children.\n";
	static public String AVAILED_ADOPTION_MATERNITY_LEAVE_TWO_CHILDREN = "VL: Availed Maternity and Adoption Leave For Two Children. \n";
	static public String ADOPTION_LEAVE_LIMIT_EXCEEDED = "VL:  Adoption Leave Already Availed For Two Children.\n";
	static public String ADOPTED_CHILD_DOB_REQUIRED = "VL: Adopted Child Date Of Birth Not Available In ADOPT DOII. \n";
	static public String ADOPTION_LEAVE_NOT_ADMISSIBLE = "VL: Adoption Leave Not Admissible For Adoption Of Child Of Age Above One Year.\n";
	
	private StringBuffer buffer = new StringBuffer();
	int tjra = 0, lra = 0, tjralraDays = 0;
	private double do2Days = 0.0, noofMonths = 0.0;
	private String paoNo;
	
	List<String> cclNotPermittedSuffixPrefixList = Arrays.asList(new String[] { "AL", "ADVAL", "ACCMAL", "BAL", "CAL", "EXTNAL", "PAL", "SL", "EXTNSL", "EXTNLV" });
	
	@Override
	public boolean isValid(Do2 do2) {

		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		paoNo = Utils.getPao().getPaoCode();
		boolean validity = true;
		if (do2.getDescription().equals("ACCMAL")) {
			if (!isAccmal(do2)) {
				validity = false;
				buffer.append(INVALID_ACCMAL);
			}
		}

		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(INVALID_LRA);
		}
	 
		if (!isTjraLra(do2)) {
			validity = false;
			buffer.append(INVALID_LEAVE_PERIOD);
		}
		
		if (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			if (do2.getDescription().equals("CL") || do2.getDescription().equals("SL")) {
				;
			} else if (!isSixMonthCompleted(do2)) {
				validity = false;
				buffer.append(NOT_COMPLETED_SIXMONTHS);
			}
			if (do2.getDescription().equals("CCL")) {
				validity = false;
				buffer.append(NOT_ADMISSIBLE_DURING_TRAINING_PROBATION);
			}
		}

		if (do2.getDescription().equals("CCL")) {
			List<EnrollDo2> enrollDo2List = enrollDo2Repo.findValidApprovedEnrollDo2ByArmyNoOrderById(do2.getArmyNo());
			if (enrollDo2List==null || (enrollDo2List!=null && enrollDo2List.size()==0)) {
				String ano = do2.getArmyNo();
				if (ano.startsWith("JC")) {
					ano="00"+ano.substring(2);
					enrollDo2List = enrollDo2Repo.findValidApprovedEnrollDo2ByArmyNoOrderById(ano);
				}
			}
			if (enrollDo2List!=null && enrollDo2List.size()==1) {
				if (enrollDo2List.get(0).getGender()==null || (enrollDo2List.get(0).getGender()!=null && enrollDo2List.get(0).getGender().equals("M"))) {
					List<FamilyDetail> fdList = fdRepo.findByRelationshipStatusForSingleMarriedPersonnel(do2.getEmployee().getId());
					if (fdList==null || (fdList!=null && fdList.size()==0)) {
						validity=false;
						buffer.append(INADMISSIBLE_LEAVE);
					}					
				}
			}
			if (do2.getLra()!=null && (do2.getLra()<5 || do2.getLra()>30)) {
				validity=false;
				buffer.append(INADMISSIBLE_PERIOD);
			}
			List<Do2> adjustedCclDo2List = do2Repo.findAllByEmployeeIdAndDescriptionAndStatusOrderByFromDateDesc(do2.getEmployee().getId(), "CCL", Do2Manager.VALID);
			if (adjustedCclDo2List!=null && adjustedCclDo2List.size()>0) {
				Calendar cal = new GregorianCalendar();
				cal.setTime(do2.getFromDate());
				int year = cal.get(Calendar.YEAR);
				int totalCclDaysInService=0, totalCclDaysInCalendarYear=0;
				Date firstJanuary = DateManager.getDateFromYyyyMmDd(year+"_01_01");
				for (Do2 d : adjustedCclDo2List) {
					if (d.getFromDate().before(firstJanuary)) {
						totalCclDaysInCalendarYear+=d.getLra();
					}
					totalCclDaysInService+=d.getLra();					
				}
				if (totalCclDaysInService>360 || (totalCclDaysInService+do2.getLra())>360) {
					validity=false;
					buffer.append(EXCEEDED_MAXIMUM_PERIOD);
				}
				if (totalCclDaysInCalendarYear>60 || (!do2.getFromDate().before(firstJanuary) && (totalCclDaysInCalendarYear+do2.getLra())>60)) {
					validity=false;
					buffer.append(EXCEEDED_MAXIMUM_PERIOD_FOR_CALENDAR_YEAR);
				}
			}
			List<Do2> checkPrefixSuffixDo2List = do2Repo.findAllByEmployeeIdAndDescriptionList(do2.getEmployee().getId(), cclNotPermittedSuffixPrefixList,
					DateManager.getPreviousDate(do2.getFromDate()), DateManager.getNextDate(do2.getToDate()));
			if (checkPrefixSuffixDo2List!=null && checkPrefixSuffixDo2List.size()>0) {
				validity=false;
				buffer.append(INADMISSIBLE_PREFIX_SUFFIX_DESCRIPTION);
			}
		}
		if (do2.getDescription().equals("ADOLVE")) {
			List<EnrollDo2> enrollDo2List = enrollDo2Repo.findValidApprovedEnrollDo2ByArmyNoOrderById(do2.getArmyNo());
			if (enrollDo2List==null || (enrollDo2List!=null && enrollDo2List.size()==0)) {
				String ano = do2.getArmyNo();
				if (ano.startsWith("JC")) {
					ano="00"+ano.substring(2);
					enrollDo2List = enrollDo2Repo.findValidApprovedEnrollDo2ByArmyNoOrderById(ano);
				}
			}
			if (enrollDo2List!=null && enrollDo2List.size()==1) {
				if (enrollDo2List.get(0).getGender()==null || (enrollDo2List.get(0).getGender()!=null && enrollDo2List.get(0).getGender().equals("F"))) {
					;
				} else {
					validity=false;
					buffer.append(INADMISSIBLE_ADOPTION_LEAVE);				 				
				}
			}
			if (do2.getRefDo2No()==null || (do2.getRefDo2No()!=null && do2.getRefDo2No().trim().length()==0)) {
				validity=false;
				buffer.append(REFERENCE_DO2_REQUIRED);
			} else {
				List<Do2> adoptedDo2List = Utils.getReferenceDo2List(do2);
				if (adoptedDo2List!=null && adoptedDo2List.size()>0) {
					if (adoptedDo2List.get(0).getDescription().equals("ADOPT") && adoptedDo2List.get(0).getStatus().equals(Do2Manager.VALID)) {
						if (adoptedDo2List.get(0).getColumnC()==null || 
								(adoptedDo2List.get(0).getColumnC()!=null && adoptedDo2List.get(0).getColumnC().length()==0)) {
							validity = false;
							buffer.append(ADOPTED_CHILD_DOB_REQUIRED);
						} else {
							Date adoptedChildDoB = DateManager.getDateFromDdMmYyyy(adoptedDo2List.get(0).getColumnC().trim());
							if (DateManager.getNoOfDays(adoptedChildDoB, do2.getFromDate())<=366) {
								;
							} else {
								validity = false;
								buffer.append(ADOPTION_LEAVE_NOT_ADMISSIBLE);
							}
						}						
					} else {
						validity=false;
						buffer.append(INCORRECT_REFERENCE_DO2);
					}
				}
			}
			List<Do2> adolveDo2List = do2Repo.getPreviousAdoptionLeaveDescription(do2.getEmployee().getId(), do2.getFromDate());
			if (adolveDo2List!=null && adolveDo2List.size()>0) {
				if (adolveDo2List.size()>=2) {
					validity=false;
					buffer.append(ADOPTION_LEAVE_LIMIT_EXCEEDED);
				}
			}
			List<Do2> matlveDo2List = do2Repo.getPreviousMaternityLeaveDescription(do2.getEmployee().getId(), do2.getFromDate());
			if (matlveDo2List!=null && matlveDo2List.size()>0) {
				if (matlveDo2List.size()>=2) {
					validity=false;
					buffer.append(MATLVE_LIMIT_EXCEEDED);
				} else {
					adolveDo2List=null;
					adolveDo2List = do2Repo.getPreviousAdoptionLeaveDescription(do2.getEmployee().getId(), do2.getFromDate());
					if (adolveDo2List!=null && adolveDo2List.size()>0) {
						validity=false;
						buffer.append(AVAILED_ADOPTION_MATERNITY_LEAVE_TWO_CHILDREN);		
					}			
				}
			}		
		}
		if (do2.getDescription().equals("MATLVE")) {
			List<EnrollDo2> enrollDo2List = enrollDo2Repo.findValidApprovedEnrollDo2ByArmyNoOrderById(do2.getArmyNo());
			if (enrollDo2List==null || (enrollDo2List!=null && enrollDo2List.size()==0)) {
				String ano = do2.getArmyNo();
				if (ano.startsWith("JC")) {
					ano="00"+ano.substring(2);
					enrollDo2List = enrollDo2Repo.findValidApprovedEnrollDo2ByArmyNoOrderById(ano);
				}
			}
			if (enrollDo2List!=null && enrollDo2List.size()==1) {
				if (enrollDo2List.get(0).getGender()==null || (enrollDo2List.get(0).getGender()!=null && enrollDo2List.get(0).getGender().equals("F"))) {
					;
				} else {
					validity=false;
					buffer.append(INADMISSIBLE_MATERNITY_LEAVE);				 				
				}
			}
			if (do2.getTjra()==null || (do2.getTjra()!=null && do2.getTjra()==0)) {
				;
			} else {
				validity=false;
				buffer.append(TJRA_COLUMN_NOT_BLANK);
			}
			if (do2.getLra()==null || (do2.getLra()!=null && do2.getLra()==0)) {
				validity=false;
				buffer.append(LRA_COLUMN_BLANK);
			} else {
				if (do2.getLra()==180 || do2.getLra()==30) {
					;
				} else {
					validity=false;
					buffer.append(INCORRECT_LRA_VALUES);
				}
			}
			if (do2.getRefDo2No()!=null) {
				List<Do2> referredDo2List = Utils.getReferenceDo2List(do2);
				if (referredDo2List!=null && referredDo2List.size()>0) {
					if (referredDo2List.get(0).getDescription().equals("MATLVE")) {
						if (do2.getColumnA()!=null && do2.getColumnA().equals("EXTN")) {
							if (do2.getFromDate().equals(DateManager.getNextDate(referredDo2List.get(0).getToDate()))) {
								;
							} else {
								validity=false;
								buffer.append(INCORRECT_START_DATE_FOR_EXTENSION);							}
						} else {
							noofMonths=0;
							noofMonths=DateManager.getNoOfMonths(referredDo2List.get(0).getToDate(), do2.getFromDate());
							if (noofMonths>=24) {
								;
							} else {
								validity=false;
								buffer.append(COOLING_OFF_PERIOD_NOT_COMPLETED);
							}
						}
					}
				}
			} else {
				if (do2.getColumnA()!=null && do2.getColumnA().equals("EXTN")) {
					validity=false;
					buffer.append(REFERENCE_DO2NO_REQUIRED);
				}
			}
			List<Do2> matlveDo2List = do2Repo.getPreviousMaternityLeaveDescription(do2.getEmployee().getId(), do2.getFromDate());
			if (matlveDo2List!=null && matlveDo2List.size()>0) {
				if (matlveDo2List.size()>2) {
					validity=false;
					buffer.append(MATLVE_LIMIT_EXCEEDED);
				} else if (matlveDo2List.size()==2) {
					if (matlveDo2List.get(1).getColumnA().equals("EXTN")) {
						;
					} else {
						validity=false;
						buffer.append(MATLVE_LIMIT_EXCEEDED);
					}
				} 
			}	
			List<Do2> adolveDo2List = do2Repo.getPreviousAdoptionLeaveDescription(do2.getEmployee().getId(), do2.getFromDate());
			if (adolveDo2List!=null && adolveDo2List.size()>0) {
				if (adolveDo2List.size()>=2) {
					validity=false;
					buffer.append(AVAILED_ADOPTION_LEAVE_TWO_CHILDREN);
				} else {
					matlveDo2List=null;
					matlveDo2List = do2Repo.getPreviousMaternityLeaveDescription(do2.getEmployee().getId(), do2.getFromDate());
					if (matlveDo2List!=null && matlveDo2List.size()>0) {
						validity=false;
						buffer.append(AVAILED_ADOPTION_MATERNITY_LEAVE_TWO_CHILDREN);
					}
				} 
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
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		Date regularisedToDate = null;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		paoNo = Utils.getPao().getPaoCode();
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (newDo2.getDescription().equals(Do2Manager.REGAL)) {
			List<Do2> do2List = Utils.getReferenceDo2List(newDo2);
			for (Do2 refDo2 : do2List) {
				if (Do2Manager.STOP_ALL.contains(refDo2.getDescription())) {
					Do2 absentDo2 = refDo2;
					AuditCage absentAuditCage = Utils.getAuditCage(absentDo2);
					regularisedToDate = absentAuditCage.getToDate();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
					do2.setColumnA(df.format(regularisedToDate.getTime()));
				}
			}
		}
		if (newDo2.getDescription().equals(Do2Manager.CANCEL) && newDo2.getColumnA().equals(Do2Manager.REGAL)) {
			do2.setColumnA(null);
		}

		if (do2.getDescription().equals(Do2Manager.REGAL) && do2.getColumnA() != null
				&& do2.getColumnA().length() != 0) {
			try {
				do2Days = DateManager.getNoOfDays(do2.getFromDate(), DateManager.getDate(do2.getColumnA()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		}

		// Revision stopped from 23042012 one time program next month.
		/*
		 * if (do2.getDescription().equals("AL") || do2.getDescription().equals("PAL")
		 * || do2.getDescription().equals("BAL")) {
		 * revisedBhutanAllowance(newAuditCage); }
		 */
		// amended on 23042012
		// dnr - in case of extn - original period is to be taken into account
		if (do2.getDescription().equals("CL")) {
			stopAllowancesCl(do2);
		} else {
			stopAllowancesAl(do2);
		}

		return Do2Manager.SUCCESS;
	}

	private void stopAllowancesAl(Do2 do2) {
		List<String> alwToStop = new ArrayList<String>();
		
		do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		
		List<Do2> recallDo2List = do2Repo.findRecallDo2(employee,do2.getFromDate());
		System.out.println("!!!!!!!!!! do2 days " +do2Days +"  recalldo2list size  " +recallDo2List.size());
		if (recallDo2List!=null && recallDo2List.size()>0) {
			System.out.println("........recalltodate " +recallDo2List.get(0).getToDate()+"  do2.gettodate "+do2.getToDate());
			for (Do2 d : recallDo2List) {
				if (d.getToDate().equals(do2.getToDate())) {
					do2Days = DateManager.getNoOfDays(do2.getFromDate(), DateManager.getPreviousDate(d.getFromDate()));
				}
			}			
			System.out.println("....%%%%%%%% do2days " +do2Days);
		} 
		
		int IslndaLimit = 15;
		/*
		 * if (do2.getDescription().equals("ISLNDA") &&
		 * (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()))) {
		 * IslndaLimit =30; }else { IslndaLimit =15; }
		 */
		System.out.println("$$$$$$$$$$$$$ recall do2list size " +recallDo2List.size()+"  do2days "+do2Days);
		if (do2Days > Do2Manager.AL_CEASE_UPPER_LIMIT) {
			alwToStop = Arrays.asList(new String[] { Do2Manager.CHAFAA, Do2Manager.SCCIA, Do2Manager.HAA, Do2Manager.RISK });
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		if (do2Days > IslndaLimit && do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			for (AuditCageAllowance aca : newAuditCage.getAuditCageAllowances()) {
				if (aca.getPayCode().getPayCode().trim().equals("ISLNDA")) {
					alwToStop = Arrays.asList(new String[] { Do2Manager.ISLNDA });
					AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
					break;
				}
			}
		}
		if (do2Days > Do2Manager.LEAVE_ZERO_LIMIT) {
			// on the directions of HQRs Office vide their letter No.Mech/EDP/402(PC) 11/07
			// dated 09.07.09
			if (paoNo.equals(Do2Manager.PAO_LADAK_SCOUTS)) {
				if (Do2Manager.getExceptionLadakUnits().contains(do2.getUnitCode())) {
					alwToStop = Arrays.asList(new String[] { Do2Manager.SIACHN, "HRDALL" });
					AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
				} else {
					alwToStop = Arrays.asList(new String[] { Do2Manager.HAUCL, Do2Manager.HAUCH, Do2Manager.HAUCS,
							Do2Manager.SIACHN, "HRDALL","HAUCA" });
					AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
				}
			} else {
				alwToStop = Arrays.asList(new String[] { Do2Manager.HAUCL, Do2Manager.HAUCH, Do2Manager.HAUCS,
						Do2Manager.SIACHN, "HRDALL","HAUCA" });
				AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
			}
		}
	}

	private void stopAllowancesCl(Do2 do2) {
		do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		
		List<Do2> recallDo2List = do2Repo.findRecallDo2(employee,do2.getFromDate());
		if (recallDo2List.size()!=0) {
			if (recallDo2List.get(0).getToDate().equals(do2.getToDate())) {
				do2Days = DateManager.getNoOfDays(do2.getFromDate(), DateManager.getPreviousDate(recallDo2List.get(0).getFromDate()));
			}			
		} 		

		List<String> alwToStop = new ArrayList<String>();
		int IslndaLimit = 15;
		// (Auth:CGDA AT/I/O/75/V dated 27.7.2011 -
		// MOD letter No.PCB/37269/CI/AG/PS-3(A)/843/D (Pay/Services) dt.05.07.2011)
		// no recovery of SCCIA, CHAFAA, HAA upto 30 days on CL only
		Date ClThirtyDaysLimitDate = DateManager.getToday();
		try {
			ClThirtyDaysLimitDate = DateManager.getDate("2011-07-05");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (do2Days > Do2Manager.CL_CEASE_LOWER_LIMIT) {
			final String[] alwUnderProcess = { Do2Manager.SIACHN, Do2Manager.HAUCL, Do2Manager.HAUCH, Do2Manager.HAUCS,
					"HRDALL","HAUCA" };
			alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		if (do2Days > IslndaLimit && do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			alwToStop = Arrays.asList(new String[] { Do2Manager.ISLNDA });
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		if (!do2.getFromDate().before(ClThirtyDaysLimitDate)) {
			; // amended on 16112012
		} else if (do2Days > Do2Manager.CL_CEASE_UPPER_LIMIT) {
			final String[] alwUnderProcess = { Do2Manager.SCCIA, Do2Manager.CHAFAA, Do2Manager.HAA };
			alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}

	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	public boolean isValidTjra(Do2 do2) {

		if (Do2Manager.getLeaveExclude().contains(do2.getDescription())) {
			return true;
		}
		return (do2.getTjra() != null && do2.getTjra() <= 8);

	}

	public boolean isValidLra(Do2 do2) {
		if (do2.getLra() != null && do2.getTjra() != null) {
			if (do2.getDescription().equals("SL") || do2.getDescription().equals("EXTNSL"))
				return true;
			/*
			 * if ((do2.getDescription().equals("AL") || do2.getDescription().equals("PAL")
			 * || do2.getDescription().equals("BAL") ||
			 * do2.getDescription().equals("EXTNAL") ||
			 * do2.getDescription().equals("EXTNLV") && (do2.getColumnA().equals("AL") ||
			 * do2.getColumnA().equals("ADVAL") || do2.getColumnA().equals("BAL") ||
			 * do2.getColumnA().equals("PAL"))) && do2.getLra() <= 56) return true;
			 */
			if ((do2.getDescription().equals("AL") || do2.getDescription().equals("PAL")
					|| do2.getDescription().equals("BAL") || do2.getDescription().equals("EXTNAL"))
					&& do2.getLra() <= 56)
				return true;

			if ((do2.getDescription().equals("ADVAL") && do2.getLra() + do2.getTjra() <= 30))
				return true;
			if ((do2.getDescription().equals("PATLVE") && do2.getLra() <= 15))
				return true;
			if (do2.getDescription().equals("CAL") && do2.getLra() <= 116)
				return true;
			if (do2.getDescription().equals("ACCMAL") && do2.getLra() <= 86)
				return true;
		//	if (do2.getDescription().equals("CCLSMP") && do2.getLra() <= 30)
			if (do2.getDescription().equals("CCL") && do2.getLra()>=5 && do2.getLra()<=30)
				return true;
			if ((do2.getDescription().equals("CL") || do2.getDescription().equals("EXTNCL")
					|| (do2.getDescription().equals("EXTNLV") && do2.getColumnA().equals("CL"))) && do2.getLra() <= 30)
				return true;
		}

		if (do2.getLra() != null && (do2.getTjra() == null || (do2.getTjra()!=null && do2.getTjra()==0))) {
			if ((do2.getDescription().equals("EXTNLV")
					&& (do2.getColumnA().equals("AL") || do2.getColumnA().equals("ADVAL")
							|| do2.getColumnA().equals("BAL") || do2.getColumnA().equals("PAL")))
					&& do2.getLra() <= 56)
				return true;
			
			if (do2.getDescription().equals("EXTNLV")
					&& do2.getColumnA().equals("CL") && do2.getLra() <= 30) {			// added on 22.12.2023
				return true;
			}			
		}
		return false;
	}

	public boolean isSixMonthCompleted(Do2 do2) {

		if (employee != null)
			employee = Utils.getCurrentEmployee(do2.getArmyNo());

		return DateManager.getNoOfMonths(employee.getEnrolmentDate(), do2.getFromDate()) > 180;

	}

	public boolean isTjraLra(Do2 do2) {
		if (Do2Manager.getLeaveExclude().contains(do2.getDescription()))
			return true;
		do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
		return (do2Days == do2.getTjra() + do2.getLra());

	}

	public boolean isValidRefDo2(Do2 do2) {
		Do2 refDo2 = Utils.getReferenceDo2(do2);
		if (refDo2 != null) {

			if (do2.getDescription().equals("SL") && refDo2.getDescription().equals(Do2Manager.HOSPITAL))
				return true;

			if ((do2.getDescription().equals("EXTNCL")
					|| (do2.getDescription().equals("EXTNLV") && do2.getColumnA().equals("CL")))
					&& refDo2.getDescription().equals("CL"))

				return true;

			if ((do2.getDescription().equals("EXTNSL")) && refDo2.getDescription().equals("SL"))
				return true;

			if ((do2.getDescription().equals("EXTNAL") || (do2.getDescription().equals("EXTNLV")
					&& (do2.getColumnA().equals("AL") || do2.getColumnA().equals("ADVAL")
							|| do2.getColumnA().equals("BAL") || do2.getColumnA().equals("PAL"))))
					&& (refDo2.getDescription().equals("AL") || refDo2.getDescription().equals("PAL")
							|| refDo2.getDescription().equals("BAL") || refDo2.getDescription().equals("ADVAL")))
				return true;

			if (do2.getDescription().equals("BAL") && refDo2.getDescription().equals("PAL"))
				return true;

		}

		return true;

	}

	public boolean isAccmal(Do2 do2) {

		GregorianCalendar cal1 = new GregorianCalendar();
		cal1.setTime(do2.getFromDate());
		int accmalYear = (cal1.get(Calendar.YEAR)) - 1;
		GregorianCalendar cal3 = new GregorianCalendar(accmalYear, Calendar.JANUARY, 1);
		Date fromDate = new Date();

		fromDate.setTime(cal3.getTimeInMillis());
		Date toDate = DateManager.getPreviousDate(do2.getFromDate());
		List<String> nonAllowanceList = Arrays.asList(new String[] { "AL", "ADVAL", "ACCMAL", "BAL", "CAL", "EXTNAL",
				"EXTNCL", "PAL", "SL", "EXTNSL", "EXTNLV" });
		List<String> nonAllowList = Arrays.asList(new String[] { "CL" });

		if (Utils.isMutexAllowance(do2.getArmyNo(), nonAllowList, fromDate, toDate, do2)) {
			return true;
		} else if (Utils.isMutexAllowance(do2.getArmyNo(), nonAllowanceList, fromDate, toDate, do2)) {
			return false;
		}
		return true;
	}

	public void revisedBhutanAllowance(AuditCage auditCage) {
		List<AuditCageAllowance> auditCageAllowanceList = acaRepo.findByAuditCageIdAndPayCodeId(auditCage.getId(),
				"BHUTAN");
		/*
		 * Utils .getEntityManager() .createQuery(
		 * "SELECT aca FROM AuditCageAllowance aca where aca.auditCage=:auditCage AND aca.payCode.payCode=:payCode"
		 * ) .setParameter("auditCage", auditCage).setParameter("payCode",
		 * "BHUTAN").getResultList();
		 */
		if (auditCageAllowanceList.size() > 0) {
			int bhutanRate = AuditCageHelper.getBhutanRate(auditCage.getDo2());
			int newBhutanRate = (int) (bhutanRate * .75);
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, "BHUTAN", newBhutanRate);
		}
	}

	/*
	 * public void reviseBhutanAllowance(AuditCage auditCage) {
	 * List<AuditCageAllowance> auditCageAllowanceList = Utils .getEntityManager()
	 * .createQuery(
	 * "SELECT aca FROM AuditCageAllowance aca where aca.auditCage=:auditCage AND aca.payCode.payCode=:payCode"
	 * ) .setParameter("auditCage", auditCage).setParameter("payCode",
	 * "BHUTAN").getResultList();
	 * 
	 * if (auditCageAllowanceList.size() > 0) { double amount =
	 * (auditCageAllowanceList.get(0).getAmount() * .75);
	 * AuditCageHelper.updateAuditCageAllowance(newAuditCage,
	 * auditCageAllowanceList.get(0).getPayCode().getPayCode(), amount); } int
	 * bhutanAmount=0; if(auditCageAllowanceList.size() >0){ bhutanAmount
	 * =auditCageAllowanceList.get(0).getAmount(); Do2 leaveDo2 =
	 * auditCage.getDo2(); double seventyFivePercentAmount =
	 * (auditCageAllowanceList.get(0).getAmount() * .75 );
	 * 
	 * double recoveryAmount = (bhutanAmount - seventyFivePercentAmount)
	 * *(DateManager.getNoOfMonths(leaveDo2.getFromDate(),leaveDo2.getToDate())); //
	 * System.out.println("bhutan       "+bhutanAmount+"  "
	 * +seventyFivePercentAmount+"  "+recoveryAmount+"  "+(DateManager.getNoOfMonths
	 * (leaveDo2.getFromDate(),leaveDo2.getToDate())));
	 * Utils.addArrears(leaveDo2,recoveryAmount * -1, "BHUTAN",
	 * leaveDo2.getFromDate(),leaveDo2.getToDate()); } }
	 */

}
