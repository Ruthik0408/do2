package itsdc.dolphin.do2.accountingcage.independent;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Dak;
import itsdc.dolphin.models.DakType;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.FundPaymentAuthority;
import itsdc.dolphin.models.FundPurpose;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.Section;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Pfwdl extends AccountingCageBase implements java.io.Serializable {
	 
	static public String BLANK_ARMYNO_CD = "VL:ArmyNo/Checkdigit/PAN Is Blank. \n";
	static public String INVALID_ARMYNO = "VL:Invalid ArmyNo OR Checkdigit. \n";
	static public String BLANK_FROMDATE = "VL:FromDate Cannot Be Blank.\n ";
	static public String BEFORE_IMPLEMENTATION_DATE = "VL:FromDate Is Before Do2-Description Implementation Date.\n ";
	static public String FUTURE_DATE = "VL:FromDate Is In Future.\n ";
	static public String TODATE_BLANK = "VL:ToDate Is Blank.\n ";
	static public String RATE_BLANK = "VL:Rate Column Cannot Be Blank.\n ";
	static public String INVALID_AMOUNT = "VL:Incorrect Advance/Withdrawl Amount.\n ";
	static public String INVALID_INSTALMENT_COUNT = "VL:Incorrect Instalment Count. \n ";
	static public String INVALID_INSTALMENT_AMOUNT = "VL:Incorrect Instalment Amount.\n "; 
	static public String INVALID_LRA = "VL:Invalid Value Against LRA.\n "; 
	static public String BLANK_COL7AB = "VL:Column-7A Or Column-7B Is Blank.\n "; 
	static public String INCORRECT_COL7A = "VL:Incorrect Value Against Column-7A.\n ";
	static public String INCORRECT_COL7B = "VL:Incorrect Purpose Against Column-7B.\n ";
	static public String INCORRECT_COL7C = "VL:Incorrect Value Against Column-7C.\n "; 
	static public String INEFFECTIVE = "VL:Individual Is Not Effective.\n ";
	static public String EMPLOYEE_NOT_FOUND = "VL:Employee Record Not Found.\n ";
	static public String TERRITORIAL_ARMY = "VL:Individual Belongs To Territorial Army.\n ";
	static public String FSENTRY_FOUND = "VL:FS Due For The Individual.\n ";
	static public String INCORRECT_TODATE = "VL:ToDate Is Not After FromDate.\n ";
	static public String INCORRECT_REASON_WRT_TODATE = "VL:Discharge Date Not Less Than Two Years For Reason-DISCHARGE. \n";
	static public String DO2_WITH_PAN = "VL:Orders Awaited For Acceptance Of DOsII With PAN. \n";
	static public String DUPLICATE_WITH_CB = "DU: Claim Appears Passed Through ContingentBill In The Past Three Months. \n";
	static public String DUPLICATE_DO2 = "DU: Valid DOII Already Exists With Same Amount And Same FromDate. \n";
	static public String INVALID_DO2GROUP = "VL: DOII Group Is NULL Or Invalid DOII Group For The Description. \n";
	static public String NO_ATTACHMENT = "VL: Attachment Not Found. \n";
	static public String SECOND_DO2_SAME_MONTH = "VL: Another PFWDL DOII Processed / Under Process In This Month. \n";

	static String[] validPurposes = { "HOUSE", "PLOT", "MARRIAGE", "EDUCATION", "CONSUMER", "MEDICAL", "ADDITION", "ALTERATION", "VEHICLE", "REPAIRS", "DISCHARGE" };
	static public final List<String> VALID_PURPOSES = Arrays.asList(validPurposes);
	
	public boolean isValid(Do2 do2) {
		/*
		if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_AOC)) {      // Added on 05.06.2024
			StringBuffer buffer = new StringBuffer();
			buffer.append("Under Pilot Test At PAO-AOC Secunderabad");
			do2.setReason(buffer.toString());
			do2.setStatus("I");
			do2Repo.save(do2);
			return false;	 
		}*/
		StringBuffer buffer = new StringBuffer();
		boolean validity=true;
		Employee employee = null;	
		String me = Utils.getCurrentQuarter();
	
		if (do2.getArmyNo()==null && do2.getCheckDigit()==null && do2.getPan()!=null && do2.getPan().length()==10) {
			;
		} else if (do2.getArmyNo()!=null && do2.getCheckDigit()!=null && do2.getPan()==null) {
			;
		} else {
			validity=false;
			buffer.append(BLANK_ARMYNO_CD);
		}
		
		if (do2.getArmyNo()!=null && do2.getCheckDigit()!=null) { 
			if (Utils.getValidArmyNo(do2.getArmyNo(), do2.getCheckDigit())) {
				employee=Utils.getCurrentEmployee(do2.getArmyNo());
				System.out.println("................employee " +employee.getArmyNo()+"  "+employee.getStatusCode().getStatusCode());			
				if (employee!=null) {
					do2.setEmployee(employee);
					if (employee.getStatusCode().getId()!=1) {
						validity=false;
						buffer.append(INEFFECTIVE);
					}
					if (employee.getRegularTaFlag().equals("T")) {
						validity=false;
						buffer.append(TERRITORIAL_ARMY);
					}					
				} else {
					validity=false;
					buffer.append(EMPLOYEE_NOT_FOUND);
				}
			} else {
				validity=false;
				buffer.append(INVALID_ARMYNO);
			}
		} else if (do2.getPan()!=null) {
				validity=false;
				buffer.append(DO2_WITH_PAN);  // Following code to be opened and tested in case of do2 publication with PAN instead of Army No.
			/*
			List<Employee> emplist = empRepo.findByCurrentRecordAndPanNumberAndQe(true, do2.getPan().trim().toUpperCase(), Utils.getCurrentQuarter());
			if (emplist!=null && emplist.size()>0) {
				employee=emplist.get(0);
				if (employee!=null) {
					do2.setEmployee(employee);
					if (employee.getStatusCode().getId()!=1) {
						validity=false;
						buffer.append(INEFFECTIVE);
					}
					if (employee.getRegularTaFlag()=="T") {
						validity=false;
						buffer.append(TERRITORIAL_ARMY);
					}					
				} else {
					validity=false;
					buffer.append(EMPLOYEE_NOT_FOUND);
				}
			} else {
				validity=false;
				buffer.append(EMPLOYEE_NOT_FOUND);
			}
			*/
		}
		
		if (do2.getDo2Group()==null || (do2.getDo2Group()!=null && !do2.getDo2Group().equals("ID"))) {
			validity=false;
			buffer.append(INVALID_DO2GROUP);
		}
		
		if (do2.getAttachment()==null || (do2.getAttachment()!=null && do2.getAttachment().length()<=10)) {
			validity=false;
			buffer.append(NO_ATTACHMENT);
		}
	
		List<LpcRegister> lpcRegisterList = lpcRepo.findEntryApprovedFsList(do2.getArmyNo());
		if (lpcRegisterList!=null && lpcRegisterList.size()>0) {
			validity=false;
			buffer.append(FSENTRY_FOUND);
		} else if (employee!=null && employee.getOldArmyNo()!=null) {
			lpcRegisterList = null;
			lpcRegisterList = lpcRepo.findEntryApprovedFsList(employee.getOldArmyNo());
			if (lpcRegisterList!=null && lpcRegisterList.size()>0) {
				validity=false;
				buffer.append(FSENTRY_FOUND);
			} 
		}
		
		if (do2.getFromDate()==null) {
			validity=false;
			buffer.append(BLANK_FROMDATE); 
		}
		
		if (do2.getFromDate()!=null && do2.getFromDate().before(DateManager.getDo2ForFundImplementationDate())) {
			validity=false;
			buffer.append(BEFORE_IMPLEMENTATION_DATE); 
		}
		
		if (do2.getFromDate()!=null && do2.getFromDate().after(DateManager.getCurrentDate())) {
			validity=false; 
			buffer.append(FUTURE_DATE); 
		}
		
		if (do2.getToDate()==null) {
			validity=false;
			buffer.append(TODATE_BLANK); 
		}
		
		if (do2.getToDate()!=null && !do2.getToDate().after(do2.getFromDate())) {
			validity=false;
			buffer.append(INCORRECT_TODATE); 
		}
		
		if (do2.getColumnB()!=null && do2.getColumnB().trim().toLowerCase().equals("discharge".trim())) {
			if (do2.getToDate()!=null && DateManager.getNoOfMonths(do2.getFromDate(), do2.getToDate())>24) {
				validity=false;
				buffer.append(INCORRECT_REASON_WRT_TODATE);		
			}  		
		}
		
		if (do2.getRate()==null) {
			validity=false;
			buffer.append(RATE_BLANK); 
		}
		
		if (do2.getRate()==null) {
			validity=false;
			buffer.append(RATE_BLANK); 
		}
		
		if (do2.getRate()!=null && (do2.getRate()<100 || do2.getRate()>6000000)) {
			validity=false;
			buffer.append(INVALID_AMOUNT); 
		}
		
		if (do2.getTjra()!=null && (do2.getTjra()<1 || do2.getTjra()>60)
				&& do2.getColumnA()!=null && !do2.getColumnA().equalsIgnoreCase("final")) {
			validity=false;
			buffer.append(INVALID_INSTALMENT_COUNT); 
		}
		
		if (do2.getLra()!=null && do2.getLra()>0 
				&& do2.getColumnA()!=null && do2.getColumnA().equalsIgnoreCase("final")) {
			validity=false;			
			buffer.append(INVALID_INSTALMENT_AMOUNT); 
		}
		
		if (do2.getColumnA()==null || do2.getColumnB()==null) {
			validity=false;
			buffer.append(BLANK_COL7AB);			
		}
		
		if (do2.getColumnA()!=null && (do2.getColumnA().trim().equalsIgnoreCase("final") || do2.getColumnA().trim().equalsIgnoreCase("temporary"))) {
			;
		} else {
			validity=false;
			buffer.append(INCORRECT_COL7A);
		}
		
		if (do2.getColumnB()!=null && (VALID_PURPOSES.contains(do2.getColumnB().trim().toUpperCase()))) {
			;
		} else {
			validity=false;
			buffer.append(INCORRECT_COL7B);
		}
		
		if (do2.getColumnA()!=null && do2.getColumnA().trim().equalsIgnoreCase("final")) {
			if (do2.getTjra()==null || (do2.getTjra()!=null && do2.getTjra()==0)) {
				;
			} else {
				validity=false;
				buffer.append(INVALID_LRA);
			}
			if (do2.getLra()==null || (do2.getLra()!=null && do2.getLra()==0)) {
				;
			} else {
				validity=false;
				buffer.append(INVALID_LRA);
			}
		}		
		
		if (do2.getColumnC()==null || (do2.getColumnC()!=null && do2.getColumnC().trim().length()==0) || (do2.getColumnC()!=null && do2.getColumnC().trim().equalsIgnoreCase("special"))) {
			;
		} else {
			validity=false;
			buffer.append(INCORRECT_COL7C);			
		}
		
		if (employee!=null) {
			List<FundPaymentAuthority> fpaList = fpaRepo.findByEmpIdAndQeAndPreviousQe(employee.getId(), me, Utils.getPreviousQuarter(), Utils.getPreviousQuarter(Utils.getPreviousQuarter()));
			if (fpaList!=null && fpaList.size()>0 ) {				
				for (FundPaymentAuthority fpa : fpaList) {
					if (fpa.getApprovalAmount().intValue()==do2.getRate() && fpa.getApproved()==true && fpa.getRecordStatus().equals("V")) {
						if (do2.getColumnC()!=null && !do2.getColumnC().equalsIgnoreCase("special")) {
							validity=false;
							buffer.append(DUPLICATE_WITH_CB);	
						}
					}
					//Added on 27.05.2026
					if (fpa.getDo2()!=null && fpa.getDo2().getId()!=do2.getId()
							&& (fpa.getRecordStatus()!=null && (fpa.getRecordStatus().equals("V") || fpa.getRecordStatus().equals("P")))
							&& fpa.getQe().equals(Utils.getCurrentQuarter())) {
						validity=false;
						buffer.append(SECOND_DO2_SAME_MONTH);
					}
				}				
			}	
			List<Do2> dupDo2List = do2Repo.checkDuplicateFundDo2(employee.getId(), do2.getId(), do2.getRate(), do2.getFromDate());
			if (dupDo2List!=null && dupDo2List.size()>0) {
				validity=false;
				buffer.append(DUPLICATE_DO2);
			}
		}
		
		
		
		if (validity) {
			do2.setStatus(Do2Manager.VALID);	
			do2.setReason(null);
		} else {
			if (do2.getStatus()==null || (do2.getStatus()!=null && 
					(do2.getStatus().equals(Do2Manager.PENDING) || do2.getStatus().trim().length()<2))) { // less than 2, to ensure that it is not MR or MM
				do2.setApproved(true);   // condition added on 07.04.2025
			}
			do2.setReason(buffer.toString());			
			do2.setStatus(Do2Manager.INVALID);				
		}	
	 	
		if (do2.getAcceptedQe()==null) {
			do2.setAcceptedQe(Utils.getCurrentQuarter());	
		} else {
			if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())) {
				do2.setReviewQe(Utils.getCurrentQuarter());
			}
		}		
		do2Repo.save(do2);
		return validity;
	}
			 
	public String process(Do2 do2) {
		System.out.println("..........landed in pfwdl ..............."+do2.getArmyNo()+"  fpa repo "+fpaRepo);
		
		String me = Utils.getCurrentMonth(); 
		String fy = DateManager.getFinancialYear(me);
		DakType dkType = dakTypeRepo.findBySectionNameAndName("DO II Cell".trim().toLowerCase(), "DO II".trim().toLowerCase());
		Section srcSec = sectionRepo.findBySectionName("DO II Cell");
		Section destSec = sectionRepo.findBySectionName("Fund Cell");
		String dakidno = Utils.getDakIdNo(srcSec, destSec, dkType, fy, me);
		Dak fundDak = new Dak();
		fundDak.setDakidNo(dakidno);
		fundDak.setStatus("V");		 
		fundDak.setDakYear(fy);
		fundDak.setMonthEnding(me);
		fundDak.setSection(destSec);
		fundDak.setDakType(dkType);
		fundDak.setAmount(do2.getRate().longValue());
		fundDak.setReferenceNo(do2.getDo2Type()+"/"+do2.getDo2No()+"/"+do2.getDo2ItemNo());
		fundDak.setReferenceDate(do2.getDo2Date());
		if (do2.getColumnC()==null || (do2.getColumnC()!=null && do2.getColumnC().length()==0)) {
			fundDak.setSubject(do2.getUnitCode()+"-"+do2.getColumnA()+"-"+do2.getColumnB());
		} else {
			fundDak.setSubject(do2.getUnitCode()+"-"+do2.getColumnA()+"-"+do2.getColumnB()+"-"+do2.getColumnC());
		}
		if (do2.getEmployee() != null) {
			fundDak.setArmyNo(do2.getEmployee().getArmyNo());
			fundDak.setCheckDigit(do2.getEmployee().getCheckDigit());
			fundDak.setEmployee(do2.getEmployee());
			fundDak.setUnit(do2.getUnit());	 
		}
		fundDak.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		dakRepo.save(fundDak);			
			
		FundPaymentAuthority fpa=null;
		fpa=fpaRepo.findByDo2(do2.getId());
		if(fpa==null) {
			fpa=new FundPaymentAuthority();
			fpa.setDo2(do2);
			fpa.setDak(fundDak);
			fpa.setQe(do2.getAcceptedQe());				 
			if(do2.getRate()!=null)
				fpa.setApprovalAmount(do2.getRate().intValue());
			fpa.setRecordStatus("P");
			if(do2.getEmployee()!=null)
				fpa.setEmployee(do2.getEmployee());
			if(do2.getUnit()!=null)
				fpa.setUnit(do2.getUnit());
			fpa.setArmyNo(do2.getArmyNo());
			fpa.setCheckDigit(do2.getCheckDigit());			 
			fpa.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			fpa.setApproved(false);
			fpa.setDueForRetirement(false);
			if (do2.getColumnB().trim().toLowerCase().equals("discharge".trim())) {
				fpa.setDueForRetirement(true);
			}
			fpa.setBillDate(do2.getFromDate());
			fpa.setDischargeDate(do2.getToDate());
			fpa.setBulkApproval(false);
			fpa.setTransferedToEcs(false);
			fpa.setWithinSixMonths(false);
			fpa.setInstalment(0);
			fpa.setRate(0.0);
			fpa.setConsolidatedAmount(0);
			if (do2.getColumnA().trim().toLowerCase().equals("temporary")) {
				fpa.setTempFinal("T");
				fpa.setRate(do2.getLra().doubleValue());
				fpa.setInstalment(do2.getTjra());					
			} else {
				fpa.setTempFinal("F");
			}
	 		List<FundPurpose> fplist = fundPurposeRepo.getAllFundPurpose();
 			System.out.println("..........colb " +do2.getColumnB().toLowerCase()+"  ano " +do2.getArmyNo() +"  fplist size  "+fplist.size());
			if (fplist!=null && fplist.size()>0) {
				String purposeInDo2=do2.getColumnB().trim().toLowerCase();
				if (purposeInDo2.equals("plot")) {
					purposeInDo2="land";					
				}
		/*		if (purposeInDo2.equals("vehicle")) {           // commented on 23.04.2025
					purposeInDo2="car";
				}
				if (purposeInDo2.equals("repairs")) {
					purposeInDo2="alteration";
				}
		*/		if (purposeInDo2.equals("discharge")) {
					purposeInDo2="two year prior";    
				}
				for (FundPurpose fp : fplist) {
					if (fp.getPurpose().trim().toLowerCase().contains(purposeInDo2)) {
						fpa.setFundPurpose(fp);
						break;
					}
				}			
	 		}	
			if (do2.getColumnC()!=null && do2.getColumnC().trim().toLowerCase().equals("special")) {
				fpa.setSpecialSanction(true);
			}
			fpaRepo.save(fpa); 	
			return Do2Manager.SUCCESS;
		} else {
			return Do2Manager.FAILURE;
		}		
	}
}		
	