package itsdc.dolphin.do2.auditcage;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

 
@RestController
public class AuditCageCeaseProcessor extends SpecialProcessor {

	@Override
	public String process(Do2 do2) {
		createCeasedDo2(do2);
		return Do2Manager.SUCCESS;
	}
	private boolean hasPosin(Do2 do2) {
	List<Do2> posins = do2Repo.findByEmployeeIdAndDescriptionAndFromDate(
			do2.getEmployee().getId(), "POSIN", do2.getFromDate());
	return !posins.isEmpty();
}

	private void createCeasedDo2(Do2 do2) {
	    employee = Utils.getCurrentEmployee(do2.getArmyNo());
	    Date fromDate = getCeasedDo2FromDate(do2);
	    System.out.println("ooooooooooooooooooooooooooooooo cease date from cease processor  " +fromDate);
	    if (employee==null && do2.getEmployee()!=null) {
	    	employee=do2.getEmployee(); // Situation wherein jcno is getting invalidated in reaudit and oldarmyno is filled in armyno column of employee and all dos2 with jc number getting unprocessed due to null pointer.
	    }
	    Date dolphinStartDate = Utils.getDolphinStartDate();
	    if(employee.getFsDueDate() !=null && fromDate.after(employee.getFsDueDate()))
	    	return;
	    if ((do2.getDescription().equals(Do2Manager.PARA) || do2.getDescription().equals(Do2Manager.PARAR) || 
	       	 do2.getDescription().equals("COMNDO") ||  do2.getDescription().equals("SFACOM")) && do2.getFromDate().before(dolphinStartDate)){
	    	return;
	    }
	    try {
			if (do2.getDescription().trim().equals("HRA") && do2.getColumnA().trim().equals("GRANTED")     		 
					 					&& !do2.getFromDate().before(DateManager.getDate("2022-01-01"))) { // Modified as 01012022 on 14072022 (Nasik email)
					 				return;
					 			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	    String refDo2No = Utils.getReferenceDo2No(do2);
	    //parameter status1 and status2 added on 15/04/2012
	    List<Do2> ceasedDo2List = do2Repo.findByDescColAFromDateEqualValidPendingTransType(employee.getId(),Do2Manager.VALID,Do2Manager.PENDING,
	    		do2.getDescription(),Do2Manager.CEASED,fromDate,Do2Manager.SYSTEM_GENERATED);
	    	 
	 // System.out.println(" ceased DO2 list "+ceasedDo2List.size());
	    
	    if(ceasedDo2List.size()>0)
	    	return;  
		Do2 ceasedDo2 = (Do2) Utils.getClonedEntity(do2);
		ceasedDo2.setToDate(null);
		ceasedDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		ceasedDo2.setQe(Utils.getCurrentQuarter());
		ceasedDo2.setInputQe(do2.getInputQe());
		ceasedDo2.setDescription(Do2Manager.getCeasedMap().get(do2.getDescription().trim()));
		ceasedDo2.setEmployee(employee);
		ceasedDo2.setAuditCage(true);
		ceasedDo2.setReason(null);

				
		if (!Do2Manager.getNoCeasedSet().contains(do2.getDescription().trim())) {
			ceasedDo2.setColumnA(Do2Manager.CEASED);
		}
		if(do2.getDescription().equals("ACP")){
			ceasedDo2.setColumnA(do2.getColumnA());
		}
		ceasedDo2.setColumnB(null);
		ceasedDo2.setColumnC(null);
		ceasedDo2.setFromDate(fromDate);

		//do2.setToDate(null);
		//Utils.getEntityManager().persist(do2);
		ceasedDo2.setStatus(Do2Manager.PENDING);

		ceasedDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		do2Repo.save(ceasedDo2);
	//	pendingDo2Exists(ceasedDo2);
		
	}

	private Date getCeasedDo2FromDate(Do2 do2) {
		
		if (do2.getDescription().equals("ADPOST")) {
			Date ceaseDate = DateManager.getDate(do2.getFromDate(), 180);
			List<Do2> do2List =do2Repo.getDo2ListForAdpost(do2.getEmployee().getId(),do2.getFromDate(), ceaseDate, Do2Manager.getAllLeaveTypes());
			
			if (do2List!=null && do2List.size()>0) {
				ceaseDate = do2List.get(0).getFromDate();
			}
	 		return ceaseDate;			
		}
		//commented for extwk in case of posin //180825
		/*
		 * if (do2.getDescription().equals("EXTWK")) { Date ceaseDate =
		 * DateManager.getDate(do2.getFromDate(), 365); List<Do2> do2List =
		 * do2Repo.getDo2ListForExtwk(do2.getEmployee().getId(),do2.getFromDate(),
		 * ceaseDate); if (do2List!=null && do2List.size()>0) { ceaseDate =
		 * do2List.get(0).getFromDate(); } return ceaseDate; }
		 */

		if (do2.getDescription().equals("EXTWK")) {
		Date originalFromDate = do2.getFromDate();
		// Date ceaseDate = DateManager.addYears(originalFromDate, 1); // Replaced
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(originalFromDate);
		calendar.add(Calendar.YEAR, 1);
		Date ceaseDate = calendar.getTime();

		if (hasPosin(do2)) {
			List<Do2> sameDayCeases = do2Repo.findByFromDateAndColAAndDescriptionAndStatusAndEmployeeId(
					do2.getFromDate(), "CEASED", "EXTWK", "V", do2.getEmployee().getId());
			if (!sameDayCeases.isEmpty()) {
				Do2 prevCease = sameDayCeases.get(0);
				String ref = prevCease.getRefDo2No();
				if (ref != null && !ref.isEmpty()) {
					String[] parts = ref.split("/");
					if (parts.length >= 3) {
						String do2Type = parts[0];
						String do2No = parts[1];
						String do2ItemNo = parts[2];
						List<Do2> prevGrants = do2Repo.findByDo2TypeAndDo2NoAndDo2ItemNoAndDescriptionAndColumnAAndEmployeeId(
								do2Type, do2No, do2ItemNo, "EXTWK", "GRANTED", do2.getEmployee().getId());
						if (!prevGrants.isEmpty()) {
							originalFromDate = prevGrants.get(0).getFromDate();
							// Calculate cease date from original grant
							calendar.setTime(originalFromDate);
							calendar.add(Calendar.YEAR, 1);
							ceaseDate = calendar.getTime();
						}
					}
				}
			}
		} else {
			List<Do2> do2List = do2Repo.getDo2ListForExtwk(do2.getEmployee().getId(), do2.getFromDate(), ceaseDate);
			if (do2List != null && do2List.size() > 0) {
				ceaseDate = do2List.get(0).getFromDate();
			}
		}
		return ceaseDate;
	}

		if ((do2.getDescription().equals(Do2Manager.PARA) || do2.getDescription().equals(Do2Manager.PARAR) || do2.getDescription().equals("COMNDO")
				|| do2.getDescription().equals("SFACOM"))
				&& do2.getToDate() == null) {
			return DateManager.getParaToDate(do2.getFromDate());
		}else if ((do2.getDescription().equals(Do2Manager.CILQ) || do2.getDescription().equals("HRA")
					|| do2.getDescription().equals("HRAFAM") || do2.getDescription().equals("FAA"))
				&& do2.getToDate() == null) {
			return DateManager.getCilqDate(do2.getFromDate());
		}else if (!do2.getDescription().equals(Do2Manager.CILQ)
				&& !do2.getDescription().equals(Do2Manager.PARA) && !do2.getDescription().equals(Do2Manager.PARAR)) {
			return DateManager.getNextDate(do2.getToDate());
		}else{
			return DateManager.getNextDate(do2.getToDate());
		}
            
	}
/*	public void pendingDo2Exists(Do2 do2){
		List<Do2> do2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.fromDate >=:fromDate  and d.status=:status and d.columnA=:columnA and (d.reason not like 'Batch%' or d.reason is null)")
								 .setParameter("emp",do2.getEmployee())
								 .setParameter("fromDate",do2.getFromDate())
								 .setParameter("status",Do2Manager.PENDING)
								 .setParameter("columnA",Do2Manager.GRANTED)
								  .getResultList();
		     System.out.println("do2 ceased map list size "+do2List.size());
		    for(Do2 d : do2List){
		    	if(isCeaseDo2(d)){
		    		createCeasedDo2(d);	
		    	} 
		    	
		    }
		
	}*/
/*	private boolean isCeaseDo2(Do2 do2) {
		
		if (do2.getColumnA() != null
				&& do2.getColumnA().equals(Do2Manager.CEASED)) {
			return false;
		}
		if (Do2Manager.getCeasedMap().get(do2.getDescription()) != null
				&& do2.getToDate() != null
				|| (do2.getDescription().equals("CILQ") && do2.getToDate() == null)
				|| (do2.getDescription().equals("PARA") && do2.getToDate() == null)
				|| (do2.getDescription().equals("PARAR") && do2.getToDate() == null)
				|| (do2.getDescription().equals("FAA") && do2.getToDate() == null)
				|| (do2.getDescription().equals("HRA") && do2.getToDate() == null)
				|| (do2.getDescription().equals("HRAFAM") && do2.getToDate() == null)) {
			return true;
		}
		return false;
	}*/
//is valid make the do2 status true
/*	public boolean isPendingDo2Valid(Do2 d){
		try {
		 	
			do2Processor = (Do2AuditCageProcessor) Class.forName(
					"ind.gov.cgda.sagacity.dolphin.session.do2.auditcage."
							+ Utils.getCamelCase(d.getDescription()))
					.newInstance();
			if(do2Processor.isValid(d)){
				return true;
			}
            
		} catch (Exception e) {
			
	}
		return false;
	}*/

}
