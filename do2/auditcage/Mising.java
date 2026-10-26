package itsdc.dolphin.do2.auditcage;

 
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2AccountingCageProcessor;

/**
 * 
 * @author IT & SDC, Secunderabad
 * 
 */
@RestController
public class Mising extends Battle implements
		 Do2AuditCageProcessor, java.io.Serializable {
}