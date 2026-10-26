package itsdc.dolphin.do2.auditcage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
 
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.Utils;
 
@RestController
public class AuditCageCancelProcessor extends SpecialProcessor implements java.io.Serializable {
	Do2 refDo2;
	Do2AuditCageProcessor do2Processor;
	Do2 currentDo2;
	static public String DO2_DUPLICATE_PUBLICATION = "DU:DupliCate Publication with \n ";
	// 13.09.2012
	/*@In (create = true)
	Do2Home do2Home;*/
	@Override
	public String process(Do2 do2) {
		refDo2 = do2;
		try {
			do2Processor = (Do2AuditCageProcessor) Class.forName(
					"itsdc.dolphin.do2.auditcage."
							+ Utils.getCamelCase(refDo2.getDescription()))
					.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			Utils.handleException(e);
		}
      	refDo2.setStatus(Do2Manager.DO2_CANCEL_STATUS);
  	    do2Repo.save(refDo2);
  	    employee =Utils.getCurrentEmployee(do2.getArmyNo());
  	    String profixDo2 = "PROFIX";
  	    List<String> descrList = Arrays.asList("PROMOT","MSTHAV","MSTJCO","MACPS");
  	    if (descrList.contains(refDo2.getDescription())){
  	    	if (refDo2.getDescription().equals("MACPS") || refDo2.getDescription().equals("ACP")){
  	    		profixDo2 = "ACPFIX";
  	    	} 
  	    	String refDo2No = Utils.getReferenceDo2No(refDo2);
  	    	List<Do2> do2List = do2Repo.findByEmpIdRefDo2NoDescStatus(employee.getId(), refDo2No, profixDo2,Do2Manager.VALID);
  	    			 
  	    	if (do2List.size() > 0) {
  	    		do2List.get(0).setStatus(Do2Manager.DO2_CANCEL_STATUS);
  	    		do2Repo.save(do2List.get(0));
  	    	}
  	    }
  	    
  	    
		dueDrawnAuditCage[0] = base;
		currentAuditCage.setCancelled(true);
		acRepo.save(currentAuditCage);
		 
		String refDo2No = Utils.getReferenceDo2No(refDo2);
		
		List<Do2> do2List =do2Repo.findByEmpIdRefDo2NoColA(employee.getId(), refDo2No, do2.getDescription());
		 
		if (do2List.size() > 0) {
			currentAuditCage.setCancelDo2(do2List.get(0));
		}
		drawnList = getAllowances(dueDrawnAuditCage[1]);
		dueList = getAllowances(base);
		processCancel();
		 
	//	processEarlierDo2();
		
		return Do2Manager.SUCCESS;
	}
	
	void processCancel() {
	 	try {
			Do2AuditCageProcessor processor = (Do2AuditCageProcessor) Class
					.forName(
							"itsdc.dolphin.do2.auditcage."
									+ Utils.getCamelCase(refDo2
											.getDescription())).getDeclaredConstructor().newInstance();
			((AuditCageBase) processor).setDo2Repo(do2Repo);
			((AuditCageBase) processor).setPsRepo(psRepo);
			((AuditCageBase) processor).setAcRepo(acRepo);
			((AuditCageBase) processor).setEmpRepo(empRepo);
			((AuditCageBase) processor).setAcaRepo(acaRepo);
 			((AuditCageBase) processor).setEsdRepo(esdRepo);
			((AuditCageBase) processor).setRankRepo(rankRepo);
			((AuditCageBase) processor).setGcRepo(gcRepo);
			((AuditCageBase) processor).setMatrixRepo(matrixRepo);
			((AuditCageBase) processor).setClassPayRepo(classPayRepo);
			((AuditCageBase) processor).setApptRateRepo(apptRateRepo);
			((AuditCageBase) processor).setAlwRateRepo(alwRateRepo);
			((AuditCageBase) processor).setScRepo(scRepo);
			((AuditCageBase) processor).setUnitRepo(unitRepo);
			((AuditCageBase) processor).setRrRepo(rrRepo);
			((AuditCageBase) processor).setLpcRepo(lpcRepo);
			((AuditCageBase) processor).setArrRepo(arrRepo);
			((AuditCageBase) processor).setLpcDepRepo(lpcDepRepo);
			((AuditCageBase) processor).setBhutanSlabRepo(bhutanSlabRepo);
			((AuditCageBase) processor).setEdRepo(edRepo);
			((AuditCageBase) processor).setFsRepo(fsRepo);
			((AuditCageBase) processor).setEeRepo(eeRepo);
			((AuditCageBase) processor).setF16Repo(f16Repo);
			((AuditCageBase) processor).setAdjCbRepo(adjCbRepo);
			((AuditCageBase) processor).setFdRepo(fdRepo);
			((AuditCageBase) processor).setTioRepo(tioRepo);
			((AuditCageBase) processor).setJdbcTemplate(jdbcTemplate);
			processor.processCancel(refDo2, refDo2);
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
	
		
	}
	
	// added code for processing of  already existing duplicate do2 in the system  which was received before cancellation of 
	// original do2 at the time of cancellation of original do2
	
	public String  processEarlierDo2() {
		Do2 duplicateDo2 = refDo2;
		Date toDate = duplicateDo2.getToDate();
		String do2No = duplicateDo2.getDo2Type() + "/"
				+ duplicateDo2.getDo2No() + "/"
				+ duplicateDo2.getDo2ItemNo();
		if (duplicateDo2.getToDate() == null) {
			toDate = duplicateDo2.getFromDate();
		}
		
		System.out.println ( " do2 Number " +do2No);
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		String rejectionType = "DU:  ";
		String subMessage = (rejectionType + DO2_DUPLICATE_PUBLICATION
				+ "ARMYNO  " + duplicateDo2.getArmyNo()
				+ duplicateDo2.getDescription() + " "
				+ dateFormat.format(duplicateDo2.getFromDate()) + "  To "
				+ dateFormat.format(toDate) + "   Do2 No. " + do2No
				+ "   Do2 Date. "
				+ dateFormat.format(duplicateDo2.getDo2Date()) + "QE" + duplicateDo2
				.getQe());
		List<Do2> dupList = do2Repo.findByDescFromDateEqualReason(refDo2.getEmployee().getId(),refDo2.getDescription(),refDo2.getFromDate(), subMessage);
				
				 
		System.out.println ( " Duplist size " +dupList.size());
		if (dupList.size()>0) {
		//	System.out.println ( " Duplist  " +dupList.get(0).getId());
		//	System.out.println ( " Duplist  " +dupList.get(0).getDo2No());
		//	System.out.println ( " duplicate do2 " +duplicateDo2.getDo2No());
			if (dupList.get(0).getDo2Type()!=duplicateDo2.getDo2Type() && dupList.get(0).getDo2No()!=duplicateDo2.getDo2No() &&
					dupList.get(0).getDo2ItemNo()!=duplicateDo2.getDo2ItemNo()){
				 
				do2Repo.save(dupList.get(0));
				 
			
			}
						
				
				//dupList.get(0).setStatus(Do2Manager.PENDING);
				//Utils.getEntityManager().persist(dupList.get(0));
				//Utils.getEntityManager().flush();
			}
		return Do2Manager.SUCCESS;
	}
	
}
