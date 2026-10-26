package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;

/**
 * 
 * @author ITSDC, Secunderabad
 * 
 */
@RestController
public class Extnlv extends LeaveBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
}
