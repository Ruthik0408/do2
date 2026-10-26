package itsdc.dolphin.do2.auditcage;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
 
@RestController
public class AuditCageLeaveProcessor extends SpecialProcessor {
	Do2 regeneratedLeaveDo2,regeneratedCancelLeaveDo2,tptlEarlierGrantDo2;
	String description,columnC;
	double rate;
	List<String> leaveList = Arrays.asList(new String[] { "AL", "ADVAL", "EXTNLV",
			"BAL", "ACCMAL", "EXTNAL", "EXTNCL", "PAL", "RL", "CL" });
	@Override
	public String process(Do2 do2) {
		
		if(do2.getDescription().equals("RECALL")){
			
			generateCancelDo2ForLeave(do2);
			do2 = regeneratedLeaveDo2;		    
			do2Repo.save(regeneratedLeaveDo2);
			
		}
		if(do2.getDescription().equals(Do2Manager.CANCEL) && do2.getColumnA().equals("RECALL")){
			   
			    List<Do2> do2List = Utils.getReferenceDo2List(do2);
			    for(Do2 refDo2 : do2List){
			         if (leaveList.contains(refDo2.getDescription())){
			    		
			    		 generateCancelDo2(refDo2);  
			    		 regenerateCancelledDo2(refDo2);
			    		 
			    	 }
			    }
			    do2 = regeneratedCancelLeaveDo2;
			    do2Repo.save(regeneratedLeaveDo2);
		}
		AuditCage previousAuditCage = AuditCageHelper.getPreviousDateAuditCage(
				do2.getFromDate(), do2);
	   	    	    
		if (previousAuditCage != null
				&& previousAuditCage.getTransport() != null
				&& previousAuditCage.getTransport() != 0) {
	
			Date tptlDate = getTptlDate(do2.getFromDate(), do2.getToDate(),do2);
			
	
			if(tptlDate !=null){
				List<Do2> tptlList = getTptlDo2List(do2);
			//	System.out.println("  TPTL LIST SIZE:  "+tptlList.size());	
				if(tptlList.size() >0){
					 description = tptlList.get(0).getDescription();
					 rate        = tptlList.get(0).getRate();
					 tptlEarlierGrantDo2 = tptlList.get(0);
					 columnC = tptlList.get(0).getColumnC();
					 generatetptlCeaseDo2(do2, tptlDate);
					 generateTptlGrantDo2(do2);
				}
				
			}
				
		}

		return Do2Manager.SUCCESS;
	}

	private Date getTptlDate(Date fromDate, Date toDate,Do2 do2) {
	//	System.out.println("  TPTL FROM DATE:  "+fromDate);
	//	System.out.println("  TPTL TO   DATE:  "+toDate);
		Date tptlDate = DateManager.getToday();
		Date ceaseDate = DateManager.getToday();
		GregorianCalendar fromCal = new GregorianCalendar();
		GregorianCalendar toCal = new GregorianCalendar();
		if (!fromDate.after(DateManager.getTptlDate())) {
			double do2Days = DateManager.getNoOfDays(fromDate, toDate);
			if (do2Days > 30) {
				fromCal.setTime(fromDate);generatetptlCeaseDo2(do2, tptlDate);
				generateTptlGrantDo2(do2);
				toCal.setTime(DateManager.getNextDate(toDate));
				ceaseDate.setTime(fromCal.getTimeInMillis());
			}
		} else {
			double monthCount = DateManager.getNoOfMonths(fromDate, toDate);
	//		System.out.println("  TPTL MONTH COUNT:  "+monthCount);
			if (monthCount < 1.0)
				return null;

			fromCal.setTime(fromDate);
			toCal.setTime(toDate);
			int toMonthMaxDays = DateManager.getNoOfDaysInMonth(toCal
					.get(Calendar.MONTH) + 1, toCal.get(Calendar.YEAR));

			// Doesn't span full month
			if (monthCount < 2.0){ 
			
				if((fromCal.get(Calendar.MONTH)+1 == toCal.get(Calendar.MONTH) && fromCal.get(Calendar.DATE) !=1 && toCal.get(Calendar.DATE) !=toMonthMaxDays)
						|| (fromCal.get(Calendar.MONTH)==Calendar.DECEMBER && toCal.get(Calendar.MONTH)==Calendar.JANUARY )
						&& fromCal.get(Calendar.DATE) !=1 && toCal.get(Calendar.DATE) != toMonthMaxDays)
					
				return null;
			}

			if (fromCal.get(Calendar.DATE) == 1
					&& toCal.get(Calendar.DATE) == toMonthMaxDays) {
		//		System.out.println("  TPTL First:  ");	
			} else if (fromCal.get(Calendar.DATE) != 1
					&& toCal.get(Calendar.DATE) == toMonthMaxDays) {
				fromCal.set(Calendar.MONTH, fromCal.get(Calendar.MONTH) + 1);
		//		System.out.println("  TPTL Second:  ");	
			} else if (fromCal.get(Calendar.DATE) != 1
					&& toCal.get(Calendar.DATE) != toMonthMaxDays) {
				fromCal.set(Calendar.MONTH, fromCal.get(Calendar.MONTH) + 1);
		//		System.out.println("  TPTL Third:  ");	
			} else if (fromCal.get(Calendar.DATE) == 1
					&& toCal.get(Calendar.DATE) != toMonthMaxDays) {
		//		System.out.println("  TPTL Fourth:  ");	
			}
			fromCal.set(Calendar.DATE, 1);
			ceaseDate.setTime(fromCal.getTimeInMillis());
			tptlDate = ceaseDate;

		}
	//	System.out.println("  TPTL DATE:  "+tptlDate);	
		return tptlDate;
	}

	private void generatetptlCeaseDo2(Do2 do2, Date ceaseDate){
	
		if(!isDo2Generated(description,Do2Manager.CEASED,ceaseDate,do2)){
			Do2 tptlCeaseDo2 = new Do2();
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			tptlCeaseDo2.setEmployee(employee);
			tptlCeaseDo2.setArmyNo(employee.getArmyNo());
			tptlCeaseDo2.setCheckDigit(employee.getCheckDigit());
			tptlCeaseDo2.setUnitCode(do2.getUnitCode());
			tptlCeaseDo2.setUnit(do2.getUnit());
			tptlCeaseDo2.setDo2Group("IA");
			tptlCeaseDo2.setDo2Type(do2.getDo2Type());
			tptlCeaseDo2.setDo2No(do2.getDo2No());
			tptlCeaseDo2.setDo2ItemNo(do2.getDo2ItemNo());
			tptlCeaseDo2.setDo2Date(do2.getDo2Date());
			tptlCeaseDo2.setDescription(description);
			tptlCeaseDo2.setFromDate(ceaseDate);
			tptlCeaseDo2.setColumnA(Do2Manager.CEASED);
			tptlCeaseDo2.setStatus(Do2Manager.PENDING);
			tptlCeaseDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			tptlCeaseDo2.setAuditCage(true);
			tptlCeaseDo2.setQe(Utils.getCurrentQuarter());
			tptlCeaseDo2.setAcceptedQe(Utils.getCurrentQuarter());
			tptlCeaseDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			do2Repo.save(tptlCeaseDo2);	
		}
		
	}
	
	private void generateTptlGrantDo2(Do2 do2){
		 Date tptlRegrantDate = getTptlRegrantDate(do2.getToDate());
		 
		if(!isDo2Generated(description,Do2Manager.GRANTED,tptlRegrantDate,do2)){
			Do2 tptlGrantDo2 = new Do2();
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			tptlGrantDo2.setEmployee(employee);
			tptlGrantDo2.setArmyNo(employee.getArmyNo());
			tptlGrantDo2.setCheckDigit(employee.getCheckDigit());
			tptlGrantDo2.setUnitCode(do2.getUnitCode());
			tptlGrantDo2.setUnit(do2.getUnit());
			tptlGrantDo2.setDo2Group("IA");
			tptlGrantDo2.setDo2Type(tptlEarlierGrantDo2.getDo2Type());
			tptlGrantDo2.setDo2No(tptlEarlierGrantDo2.getDo2No());
			tptlGrantDo2.setDo2ItemNo(tptlEarlierGrantDo2.getDo2ItemNo());
			tptlGrantDo2.setDo2Date(tptlEarlierGrantDo2.getDo2Date());
			tptlGrantDo2.setDescription(description);
			tptlGrantDo2.setFromDate(tptlRegrantDate);
			tptlGrantDo2.setColumnA(Do2Manager.GRANTED);
			tptlGrantDo2.setColumnC(columnC);
			tptlGrantDo2.setRate(rate);
			tptlGrantDo2.setStatus(Do2Manager.PENDING);
			tptlGrantDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			tptlGrantDo2.setAuditCage(true);
			tptlGrantDo2.setQe(Utils.getCurrentQuarter());
			tptlGrantDo2.setAcceptedQe(Utils.getCurrentQuarter());
			tptlGrantDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			do2Repo.save(tptlGrantDo2);
		
	}	
	
		 
	}
	private Date getTptlRegrantDate(Date toDate){
		   Date tptlRegrantDate=null;
		   GregorianCalendar fromCal = new GregorianCalendar();
		   fromCal.setTime(toDate);
		   int noOfDays = DateManager.getNoOfDaysInMonth(fromCal.get(Calendar.MONTH)+1,fromCal.get(Calendar.YEAR));
		   int datedays = fromCal.get(Calendar.DAY_OF_MONTH);
		  
		   if((noOfDays - datedays) == 0) {
			   fromCal.set(Calendar.MONTH,fromCal.get(Calendar.MONTH)+1);
			   fromCal.set(Calendar.DATE,1);
			    tptlRegrantDate = fromCal.getTime();
			  
		   }else{
			   fromCal.set(Calendar.DATE,1);
			   tptlRegrantDate = fromCal.getTime();
			   
		   }
		   return tptlRegrantDate;
	}

    private void generateCancelDo2ForLeave(Do2 do2){
    		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		Do2 leaveDo2=null;
		for(Do2 refDo2 : refDo2List){
			if(leaveList.contains(refDo2.getDescription())){
				leaveDo2 = refDo2;
			
			}
		}
		generateCancelDo2(leaveDo2);
		generateLeaveDo2(leaveDo2,do2);
    }
    private void generateCancelDo2(Do2 leaveDo2){
    	 	Do2 leaveCancelDo2 = new Do2();
			leaveCancelDo2.setEmployee(Utils.getCurrentEmployee(leaveDo2.getArmyNo()));
			leaveCancelDo2.setArmyNo(leaveDo2.getArmyNo());
			leaveCancelDo2.setCheckDigit(leaveDo2.getCheckDigit());
			leaveCancelDo2.setUnitCode(leaveDo2.getUnitCode());
			leaveCancelDo2.setUnit(leaveDo2.getUnit());
			leaveCancelDo2.setDo2Group("IA");
			leaveCancelDo2.setDo2Type(leaveDo2.getDo2Type());
			leaveCancelDo2.setDo2No(leaveDo2.getDo2No());
			leaveCancelDo2.setDo2ItemNo(leaveDo2.getDo2ItemNo());
			leaveCancelDo2.setDo2Date(leaveDo2.getDo2Date());
			leaveCancelDo2.setDescription(Do2Manager.CANCEL);
			leaveCancelDo2.setFromDate(leaveDo2.getFromDate());
			leaveCancelDo2.setColumnA(leaveDo2.getDescription());
			leaveCancelDo2.setRefDo2No(Utils.getReferenceDo2No(leaveDo2));
			leaveCancelDo2.setStatus(Do2Manager.PENDING);
			leaveCancelDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			leaveCancelDo2.setAuditCage(true);
			leaveCancelDo2.setQe(Utils.getCurrentQuarter());
			leaveCancelDo2.setAcceptedQe(Utils.getCurrentQuarter());
			leaveCancelDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			do2Repo.save(leaveCancelDo2);
		
    }
    private void generateLeaveDo2(Do2 leaveDo2,Do2 recallDo2){
    	Do2 revisedLeaveDo2 = new Do2();
		revisedLeaveDo2.setEmployee(Utils.getCurrentEmployee(recallDo2.getArmyNo()));
		revisedLeaveDo2.setArmyNo(recallDo2.getArmyNo());
		revisedLeaveDo2.setCheckDigit(recallDo2.getCheckDigit());
		revisedLeaveDo2.setUnitCode(recallDo2.getUnitCode());
		revisedLeaveDo2.setUnit(recallDo2.getUnit());
		revisedLeaveDo2.setDo2Group("IA");
		revisedLeaveDo2.setDo2Type(recallDo2.getDo2Type());
		revisedLeaveDo2.setDo2No(recallDo2.getDo2No());
		revisedLeaveDo2.setDo2ItemNo(recallDo2.getDo2ItemNo());
		revisedLeaveDo2.setDo2Date(recallDo2.getDo2Date());
		revisedLeaveDo2.setDescription(leaveDo2.getDescription());
		revisedLeaveDo2.setFromDate(leaveDo2.getFromDate());
		revisedLeaveDo2.setToDate(DateManager.getPreviousDate(recallDo2.getFromDate()));
		revisedLeaveDo2.setTjra(leaveDo2.getTjra()-recallDo2.getTjra());
		revisedLeaveDo2.setLra(leaveDo2.getLra()-recallDo2.getLra());
		revisedLeaveDo2.setRefDo2No(leaveDo2.getRefDo2No());
		revisedLeaveDo2.setStatus(Do2Manager.PENDING);
		revisedLeaveDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		revisedLeaveDo2.setAuditCage(true);
		revisedLeaveDo2.setQe(Utils.getCurrentQuarter());
		revisedLeaveDo2.setAcceptedQe(Utils.getCurrentQuarter());
		revisedLeaveDo2.setCreatedAt(DateManager.getCurrentTimestamp());
		do2Repo.save(revisedLeaveDo2);
		regeneratedLeaveDo2 = revisedLeaveDo2;
		recallDo2.setRefDo2No(Utils.getReferenceDo2No(regeneratedLeaveDo2));
		do2Repo.save(recallDo2);
    }
    private void regenerateCancelledDo2(Do2 regeneratedLeaveDo2){
    	
    	
    	List<Do2> do2List =do2Repo.findByDescFromDateEqual(regeneratedLeaveDo2.getEmployee().getId(),Do2Manager.DO2_CANCEL_STATUS,regeneratedLeaveDo2.getDescription(),regeneratedLeaveDo2.getFromDate()); 
    	
    	if(do2List.size() >0){
    		   Do2 cancelledLeaveDo2 = do2List.get(0);
    		
    	    	// System.out.println("cancelledLeaveDo2     "+cancelledLeaveDo2.getDescription());
    		 	Do2 leaveDo2 = new Do2();
    			leaveDo2.setEmployee(Utils.getCurrentEmployee(cancelledLeaveDo2.getArmyNo()));
    			leaveDo2.setArmyNo(cancelledLeaveDo2.getArmyNo());
    			leaveDo2.setCheckDigit(cancelledLeaveDo2.getCheckDigit());
    			leaveDo2.setUnitCode(cancelledLeaveDo2.getUnitCode());
    			leaveDo2.setUnit(regeneratedLeaveDo2.getUnit());
    			leaveDo2.setDo2Group("IA");
    			leaveDo2.setDo2Type(cancelledLeaveDo2.getDo2Type());
    			leaveDo2.setDo2No(cancelledLeaveDo2.getDo2No());
    			leaveDo2.setDo2ItemNo(cancelledLeaveDo2.getDo2ItemNo());
    			leaveDo2.setDo2Date(cancelledLeaveDo2.getDo2Date());
    			leaveDo2.setDescription(cancelledLeaveDo2.getDescription());
    			leaveDo2.setFromDate(cancelledLeaveDo2.getFromDate());
    			leaveDo2.setToDate(cancelledLeaveDo2.getToDate());
    			leaveDo2.setTjra(cancelledLeaveDo2.getTjra());
    			leaveDo2.setLra(cancelledLeaveDo2.getLra());
    			leaveDo2.setRefDo2No(cancelledLeaveDo2.getRefDo2No());
    			leaveDo2.setStatus(Do2Manager.PENDING);
    			leaveDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
    			leaveDo2.setAuditCage(true);
    			leaveDo2.setQe(Utils.getCurrentQuarter());
    			leaveDo2.setAcceptedQe(Utils.getCurrentQuarter());
    			leaveDo2.setCreatedAt(DateManager.getCurrentTimestamp());
    			do2Repo.save(leaveDo2);
    			regeneratedCancelLeaveDo2 = leaveDo2;
    			
    			
    	 }
    }
  public boolean isDo2Generated(String description,String columnA,Date fromDate,Do2 do2){
	        List<Do2> do2List = do2Repo.findByDescColAFromDateEqual(do2.getEmployee().getId(),Do2Manager.VALID, description, columnA, fromDate);
	        	 
	                                 
	   return do2List.size() > 0;
  }
  
  public List<Do2> getTptlDo2List(Do2 do2){
	  List<Do2> tptlList =do2Repo.findBy2DescColAFromDateLessThanEqual(do2.getEmployee().getId(), Do2Manager.VALID, Do2Manager.TPTL,Do2Manager.TPTH, Do2Manager.GRANTED,do2.getFromDate());
	 
	  if(tptlList.size() ==0)
		   return new ArrayList<Do2>();
	  return tptlList;
  }
	
}
