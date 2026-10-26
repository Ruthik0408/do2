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
 * @author T.Subba Reddy & V.Naga Prasad
 * 
 */
@RestController
public class Extnal extends LeaveBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
}
