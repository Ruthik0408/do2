package itsdc.dolphin.do2;

 

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.controllers.Do2Controller;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

/*
 * Author : IT & SDC, Secunderabad
 */
@RestController
public class IncrChange implements java.io.Serializable {
 
	 
	Boolean addChange, postponement;
	Date oldDateOfIncr;
	Date newDateOfIncr;
	Date oldIncrDate=null;
	Date newIncrDate=null;
	Date sixcpcDate=DateManager.getSixthCpcDate();;
	
	Do2Repository do2Repo;
	AuditCageRepository acRepo;
	 List<Do2> generatedDo2=new ArrayList<Do2>();
	
	
	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	 
	
	public Boolean getAddChange() {
		return addChange;
	}

	public void setAddChange(Boolean addChange) {
		this.addChange = addChange;
	}
	
	public Boolean getPostponement() {
		return postponement;
	}

	public void setPostponement(Boolean postponement) {
		this.postponement = postponement;
	}
	
	public Date getNewDateOfIncr() {
		return newDateOfIncr;
	}

	public void setNewDateOfIncr(Date newDateOfIncr) {
		this.newDateOfIncr = newDateOfIncr;
	}
	
	
	public Date getOldDateOfIncr() {
		return oldDateOfIncr;
	}

	public void setOldDateOfIncr(Date oldDateOfIncr) {
		this.oldDateOfIncr = oldDateOfIncr;
	}

	static String qe=Utils.getCurrentQuarterEnding();
	 	
	
	final static public long MILLIS_IN_DAY = 1000 * 3600 * 24;
	
	
	public IncrChange createInstance() {
		IncrChange incrgen = new IncrChange();
		return incrgen;
	} 
	 
	public Object[] generateIncrDo2(Employee emp){
		 Object[] obj=new Object[2];
		String mesg="";
		mesg=getValidDNI(emp, oldDateOfIncr, newDateOfIncr);
		obj[0]=mesg;
		obj[1]=generatedDo2;
		return obj;
	}
		
	public String getValidDNI(Employee emp, Date incrdt, Date newIncrDt){	
		String valid="valid";
		
        if (addChange == null)
        	addChange=false;
        if (postponement == null)
        	postponement=false;
		Do2 toBeCancelledDo2=null;
		Date incrementDate=null;
		if (emp == null){
			return "Current employee not found";
			
		}		
		
		if (emp.getStatusCode().getStatusCode().equals("R")) {
			return "PBOR is Recruit";
			 
		}
		
		if (!emp.getStatusCode().getStatusCode().equals("E") && !emp.getStatusCode().getStatusCode().equals("N")) {
			if (emp.getStatusCode().getStatusCode().equals("D")) {
				if (newIncrDt.after(emp.getStatusEffectDate())) {
					return "PBOR is Deserter or Non Effective";

				}
			}
			
		}
		
		if (!Utils.getPao().getPaoCode().equals("94")){
			if (!emp.getGroupCategory().getSoldierCategory().equals("RT")  && 
					emp.getEnrolmentDate().after(DateManager.getCutOffDate())&&
			   (emp.getInitialMusteringDate() == null || emp.getIncrementDate() == null)){
				return "Dt. of Initial Mustering or Dt. of Incr is null";
				
			}
		}
		
		if (addChange == true && incrdt == null){
			return "old Date of Incr is not keyed in";
			 
		}
		if (addChange == true && postponement == true){
			return "No postponement is accepted in change";
			 
		}
		// If change of increment
		if (addChange == true){	
			incrementDate=incrdt;
			Calendar cal = new GregorianCalendar();
			cal.setTime(incrdt);	
	        if (!sixcpcDo2Exists(emp)) {
	    //    	System.out.println("00000000");
	        	sixcpcDate=DateManager.getSixthCpcDate();
	        }
		//	System.out.println("11111111111111 incr sixcp date  " +incrementDate +"  "+sixcpcDate);
			if (incrementDate.after(sixcpcDate)) {
				valid ="Date of Increment is after 01.01.2006";
				incrementDate = null;	
			} else if (incrementDate.after(DateManager.getCutOffDate())){
				cal.set(Calendar.DATE,1);;			
			} else {
				valid="Date of Increment is before Cut-off-date";
				incrementDate = null;
			}
			if(incrementDate == null){
				return valid;
			}
			incrementDate.setTime(cal.getTimeInMillis());
			System.out.println("========================="+emp.getArmyNo()+"--"+incrementDate);
			if (Utils.getPborRank(emp.getArmyNo(), incrementDate,"ALLCE").getRankCode().equals("10")){
				valid="PBOR is recruit on the date of Incrmt";
		 	    return valid;
			}
			
			if (!Utils.getPao().getPaoCode().equals("94")){
				if (!emp.getGroupCategory().getSoldierCategory().equals("RT")  && 
					 emp.getEnrolmentDate().after(DateManager.getCutOffDate())&&
			       (emp.getInitialMusteringDate() != null && emp.getInitialMusteringDate().after(incrementDate))){
					valid="Date of Increment is before Initial Mustering Date";
					return valid;
				}
			}
			List<Do2> thisDo2 =do2Repo.findByDescFromDateEqual(emp.getId(), Do2Manager.VALID, "INCR", incrementDate);
					
					 
			if(thisDo2.size()==0)  { 
				valid="No Incr Do2 Record Found on the date of Incr";
				return valid;
			}
	
			if(thisDo2.size()>1) {
				valid="More than One Valid Incr Record on the date of Incr";
				return valid;
			} 
	
			if(thisDo2.size()==1) {	
				List<AuditCage> acList =acRepo.findEffectiveAuditCageForDo2FromDate(emp.getId(), thisDo2.get(0).getId(),incrementDate);
						
						 
				if (acList.size() == 0) {
					valid="No audit cage is found on the date of Incr";
					return valid;
				}
				toBeCancelledDo2=thisDo2.get(0);
			}
		}
		oldIncrDate=incrementDate;
		if (addChange == true && newIncrDt == null){	
			valid="Revised Date of Increment is not keyed in";
			return valid;
		} 
		if (addChange == false && newIncrDt == null){	
			valid="Date of Increment is not keyed in";
			return valid;
		} 
		
		incrementDate = newIncrDt;
		Calendar cal = new GregorianCalendar();
		cal.setTime(incrementDate);	
		int newincrmonth = cal.get(Calendar.MONTH)+1;
		int newincrdate = cal.get(Calendar.DATE);
		String firstIncrDate = "2006-07-01";
		Date firstIncrAfterSixCPC = null;
		try {
			firstIncrAfterSixCPC = DateManager.getDate(firstIncrDate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 if (!sixcpcDo2Exists(emp)) {
			    //    	System.out.println("00000000");
			        	sixcpcDate=DateManager.getSixthCpcDate();
			        }
		
		
	//	String IncrOneDateAfterSevenPc ="2016-07-01";
		
		if (incrementDate.before(DateManager.getCutOffDate())){
			valid="Date of Increment is before Cut-off-date";
			incrementDate = null;
		} 
		if (incrementDate.after(sixcpcDate) && 
			incrementDate.before(firstIncrAfterSixCPC)){
			    valid="Date of Increment is between Jan and July, 2006";
				incrementDate = null;	
			}
		if (postponement == false && incrementDate.after(DateManager.getCutOffDate())){
			cal.set(Calendar.DATE,1);
		}
		
		if (postponement == false && incrementDate.after(DateManager.getSeventhCpcDate())){
			if (((newincrmonth == 7) && (newincrdate == 1))  || ((newincrmonth == 1) && (newincrdate == 1))){
				;
			} else {
				valid="Date of Increment is after 01.01.2016";
				incrementDate = null;	
			}				
		} else if (postponement == false && incrementDate.after(sixcpcDate)){
			if ((newincrmonth == 7) && (newincrdate == 1)){
				;
			} else {
				valid="Date of Increment is after 01.01.2006";
				incrementDate = null;	
			}				
		}	
		
		if(incrementDate == null){
			return valid;
		}
		
		incrementDate.setTime(cal.getTimeInMillis());
		if (Utils.getPborRank(emp.getArmyNo(), incrementDate,"ALLCE").getRankCode().equals("10")){
			valid="PBOR is recruit on the date of Incrmt";
	 	    return valid;
		}

		if (Utils.getPao().getPaoCode().equals("94") || (emp.getGroupCategory().getSoldierCategory().equals("RT"))){			   
			if (emp.getEnrolmentDate().after(DateManager.getCutOffDate())){
				if (emp.getGraduateHavildarFlag() != null && emp.getGraduateHavildarFlag() == true &&
					emp.getGraduateHavildarDate() != null && emp.getGraduateHavildarDate().equals(emp.getEnrolmentDate()) &&
					(Utils.getPao().getPaoCode().equals("56") || Utils.getPao().getPaoCode().equals("93"))){
					;
				} else {
					if (emp.getInitialMusteringDate() != null && emp.getInitialMusteringDate().after(incrementDate)){
						valid="Date of Increment is before Initial Mustering Date";
						return valid;
					}
				}
			}		      
		}
		
		List<Do2> thisDo2 =do2Repo.findByDescFromDateEqual(emp.getId(), Do2Manager.VALID, "INCR", incrementDate);
				
				 
		if (thisDo2.size()!=0) {
			valid="Valid Incr Record is found on the new date of Incr";
			return valid;
		} 
		
		newIncrDate=incrementDate;
		
		if (addChange == true && oldIncrDate != null && newIncrDate != null) {
		    generateCancelDo2(emp, toBeCancelledDo2);
		    insertIncrmentDo2(emp, newIncrDate);
		    checkIncrDatesUpto2005(emp, oldIncrDate, newIncrDate);
		}  else if (addChange == false && newIncrDate != null) {
			if (doesIncrDo2ExistWithinOneYear(emp, newIncrDate)) {
				valid="There is a valid incr within previous one year";
				return valid;
			} else {
				insertIncrmentDo2(emp, newIncrDate);
			}
		}
		
		  return valid;
	}
	public void generateCancelDo2(Employee employee, Do2 toBeCancelledDo2) {
		Do2 cancelDo2 = new Do2();
		cancelDo2.setEmployee(employee);
		cancelDo2.setArmyNo(employee.getArmyNo());
		cancelDo2.setCheckDigit(employee.getCheckDigit());
		cancelDo2.setUnitCode(employee.getUnit().getUnitCode());
		cancelDo2.setUnit(employee.getUnit());
		cancelDo2.setDo2Group("IA");
		cancelDo2.setDo2Type("0");
		cancelDo2.setDo2No("8888");
		cancelDo2.setDo2ItemNo("999");
		cancelDo2.setDo2Date(toBeCancelledDo2.getFromDate());
		cancelDo2.setDescription("CANCEL");
		cancelDo2.setFromDate(toBeCancelledDo2.getFromDate());
		cancelDo2.setColumnA("INCR");
		cancelDo2.setRefDo2No(Utils.getReferenceDo2No(toBeCancelledDo2));
		cancelDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		cancelDo2.setAuditCage(true);
		cancelDo2.setStatus(Do2Manager.PENDING);
		cancelDo2.setQe(Utils.getCurrentQuarter());
		cancelDo2.setInputQe(Utils.getCurrentQuarter());
		cancelDo2.setCreatedAt(DateManager.getCurrentTimestamp());
		do2Repo.save(cancelDo2);
		generatedDo2.add(cancelDo2);
		 
	}
	public String insertIncrmentDo2(Employee employee, Date fromDate) {
		
		Do2 incrDo2 = new Do2();		
		incrDo2.setEmployee(employee);
		incrDo2.setArmyNo(employee.getArmyNo());
		incrDo2.setCheckDigit(employee.getCheckDigit());
		incrDo2.setUnitCode(employee.getUnit().getUnitCode());
		incrDo2.setUnit(employee.getUnit());
		incrDo2.setDo2Group("IA");
		incrDo2.setDo2Type("0");
		incrDo2.setDo2No("6666");
		incrDo2.setDo2ItemNo("666");
		incrDo2.setDo2Date(fromDate);
		incrDo2.setDescription("INCR");
		if (addChange == false && postponement == true){
			incrDo2.setColumnC("POSTPONED");
		}
		incrDo2.setFromDate(fromDate);
		incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		incrDo2.setAuditCage(true);
		incrDo2.setStatus(Do2Manager.PENDING);
		incrDo2.setQe(Utils.getCurrentQuarter());
		incrDo2.setInputQe(Utils.getCurrentQuarter());
		incrDo2.setCreatedAt(DateManager.getCurrentTimestamp());
		 do2Repo.save(incrDo2);
		 generatedDo2.add(incrDo2);
		 
		 return "Increment Do2 Generated from "+fromDate;
		 
	}
	
	Boolean mutuallyExclusiveFields;
	
	
	public Boolean getMutuallyExclusiveFields() {
		return mutuallyExclusiveFields;
	}

	public void setMutuallyExclusiveFields(Boolean mutuallyExclusiveFields) {
		this.mutuallyExclusiveFields = mutuallyExclusiveFields;
	}
	
	public void validateExclusiveFields() {
		if(addChange != null && addChange == true ) {
			setPostponement(false);
		}
		if (postponement != null && postponement == true ) {
			setAddChange(false);
		}
	}

	public boolean doesIncrDo2ExistWithinOneYear(Employee emp, Date incrDt) {		
		Date prevIncrDate=getPreviousIncrementDate(incrDt);
		Date prevDate = DateManager.getDate(incrDt, -1);
		
		try {
			if (incrDt.after(DateManager.getDate("2006-06-30")) &&  !incrDt.after(DateManager.getDate("2006-12-31"))) {
				incrDt = DateManager.getDate("2006-07-01");
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		List<Do2> incrDo2List = do2Repo.findIncrWithinOneYear(emp.getId(), prevIncrDate, prevDate);
				
				 
		if (incrDo2List == null || incrDo2List.size() == 0 )
					return false;
		
		if (incrDo2List.size() > 0 ){
			Calendar incrCal = new GregorianCalendar();
			incrCal.setTime(incrDo2List.get(0).getFromDate());
			int date = incrCal.get(Calendar.DATE);
			int year = incrCal.get(Calendar.YEAR);
			int month = incrCal.get(Calendar.MONTH) + 1 ;
			
			try {
				if (incrDt.equals(DateManager.getDate("2006-07-01"))){
					if ( incrDo2List.get(0).getFromDate().equals(DateManager.getDate("2006-01-01"))
							|| (year == 2005 && (month > 6 && month <= 12)))
						return false;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		} 
			return true;
	}
	static public Date getPreviousIncrementDate(Date currentIncrementDate) {
	
		Date dni = new Date();
		Calendar currentIncrCal = new GregorianCalendar();
		Calendar prevIncrCal = new GregorianCalendar();
		currentIncrCal.setTime(currentIncrementDate);
		int date = currentIncrCal.get(Calendar.DATE);
		int year = currentIncrCal.get(Calendar.YEAR);
		int month = currentIncrCal.get(Calendar.MONTH);
		if (year - 1 >= 2006) {
			prevIncrCal = new GregorianCalendar(year - 1, Calendar.JULY, 1);
		} else {
			prevIncrCal = new GregorianCalendar(year - 1, month, 1);
		}
		dni.setTime(prevIncrCal.getTimeInMillis());
		return dni;
	}
	public String checkIncrDatesUpto2005(Employee emp, Date oldIncrDate, Date newIncrDate) {
		Do2 toBeCancelledDo2 = null;
		Date incrDateOld = null;
		Date incrDateNew = null; 
		incrDateOld = Utils.getNextIncrementDate(oldIncrDate);
		String mesg="failure";
		
		if (incrDateOld.after(sixcpcDate)) {
            return "Incr date after sixcpc date";
		}
		List<Do2> oldDo2 = do2Repo.findByDescFromDateEqualValidPendingInvalid(emp.getId(), "INCR", incrDateOld);
				
				 
		if (oldDo2.size()==1) {		
			toBeCancelledDo2=oldDo2.get(0);
			generateCancelDo2(emp, toBeCancelledDo2);
			incrDateNew = Utils.getNextIncrementDate(newIncrDate);
			if (!incrDateNew.after(sixcpcDate)){
				List<Do2> newDo2 = do2Repo.findByDescFromDateEqualValidPending(emp.getId(),"INCR", incrDateNew);
						 
				if (newDo2.size()==0){
					List<Do2> promotDo2 = do2Repo.findPromotListForIncrement(emp.getId(),"V", newIncrDate, incrDateNew);
						 
					if (promotDo2.size()==0){
						mesg=insertIncrmentDo2(emp, incrDateNew);
					} else {
						return "failure";
					}						
				} else {
					return "failure";
				}
			} else {
				return "failure";
			}
		} else {
			return "failure";
		}
		checkIncrDatesUpto2005(emp, incrDateOld, incrDateNew);	
		return mesg;
	}
	
	public  boolean sixcpcDo2Exists(Employee emp){
		List<Do2> do2List =do2Repo.findByDescStatus(emp.getId(), "V", "SIXCPC");
				
				 
  //      System.out.println("44444444444444  do2list size " +do2List.size());
        if (do2List.size()>0) {
        	sixcpcDate = do2List.get(0).getFromDate();
 //       	System.out.println("55555555555");
        	return true;
        }
  //      System.out.println("6666666666666");
        return false;
	}
}
