package itsdc.dolphin.do2.auditcage;

 
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;

/**
 * 
 * @author IT&SDC Secunderabad
 * 
 */
@RestController
public class Sevrep extends Rep implements
		 Do2AuditCageProcessor, java.io.Serializable {
}
