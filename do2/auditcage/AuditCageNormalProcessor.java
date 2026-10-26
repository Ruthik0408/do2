package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.utils.Utils;

 

/**
 * 
 * @author Y Raja Reddy Date: 2009-07-09
 */

@RestController
public class AuditCageNormalProcessor extends SpecialProcessor implements java.io.Serializable{
	Do2AuditCageProcessor do2Processor;
	public String process(Do2 do2) {
		
		try {
			do2Processor = (Do2AuditCageProcessor) Class.forName(
					"itsdc.dolphin.do2.auditcage."
							+ Utils.getCamelCase(do2.getDescription()))
					.getConstructor().newInstance();
		} catch (Exception e) {
			Utils.handleException(e); 
		}
		((AuditCageBase)do2Processor).setNewDo2(newDo2);
		((AuditCageBase) do2Processor).setDo2Repo(do2Repo);
		//((PromotBase) do2Processor).setDo2Repo(do2Repo);
		((AuditCageBase) do2Processor).setPsRepo(psRepo);
		((AuditCageBase) do2Processor).setAcRepo(acRepo);
		((AuditCageBase) do2Processor).setEmpRepo(empRepo);
		((AuditCageBase) do2Processor).setAcaRepo(acaRepo);
		((AuditCageBase) do2Processor).setNewDo2(newDo2);
		((AuditCageBase) do2Processor).setEsdRepo(esdRepo);
		((AuditCageBase) do2Processor).setRankRepo(rankRepo);
		((AuditCageBase) do2Processor).setGcRepo(gcRepo);
		((AuditCageBase) do2Processor).setMatrixRepo(matrixRepo);
		((AuditCageBase) do2Processor).setClassPayRepo(classPayRepo);
		((AuditCageBase) do2Processor).setApptRateRepo(apptRateRepo);
		((AuditCageBase) do2Processor).setAlwRateRepo(alwRateRepo);
		((AuditCageBase) do2Processor).setScRepo(scRepo);
		((AuditCageBase) do2Processor).setUnitRepo(unitRepo);
		((AuditCageBase) do2Processor).setRrRepo(rrRepo);
		((AuditCageBase) do2Processor).setLpcRepo(lpcRepo);
		((AuditCageBase) do2Processor).setArrRepo(arrRepo);
		((AuditCageBase) do2Processor).setLpcDepRepo(lpcDepRepo);
		((AuditCageBase) do2Processor).setBhutanSlabRepo(bhutanSlabRepo);
		((AuditCageBase) do2Processor).setEdRepo(edRepo);
		((AuditCageBase) do2Processor).setFsRepo(fsRepo);
		((AuditCageBase) do2Processor).setEeRepo(eeRepo);
		((AuditCageBase) do2Processor).setF16Repo(f16Repo);
		((AuditCageBase) do2Processor).setAdjCbRepo(adjCbRepo);
		((AuditCageBase) do2Processor).setFdRepo(fdRepo);
		((AuditCageBase) do2Processor).setTioRepo(tioRepo);
		((AuditCageBase) do2Processor).setJdbcTemplate(jdbcTemplate);
		((AuditCageBase) do2Processor).setRhLocationRepo(rhLocationRepo);

		
		drawnList = getAllowances(dueDrawnAuditCage[1]);
		dueDrawnAuditCage[0]= do2Processor.getNewAuditCage(currentAuditCage, base);
		do2Processor.setNewAuditCage(dueDrawnAuditCage[0]);
		do2Processor.process(do2, base);
		dueList = getAllowances(dueDrawnAuditCage[0]);
		return Do2Manager.SUCCESS;
	}

}
