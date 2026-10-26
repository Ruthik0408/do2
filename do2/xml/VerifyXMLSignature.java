package itsdc.dolphin.do2.xml;

import java.io.File;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class VerifyXMLSignature {
	/*
	public static void main(String args[]) {
		String filePath="/home/itsdcsec/Downloads/saamarth/sra.xml";
		//String keyPath="/var/springboot/keystore/dolphinpg.cer";
		String keyPath="/home/itsdcsec/Downloads/saamarth/ehrms_certificate.crt";

		try {
			String valid=validateSignature(filePath, keyPath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
*/
    public static String validateSignature(String xmlFilePath,String keyPath) throws Exception {

        // Load the XML document
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new File(xmlFilePath));

        // Find the Signature element
        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Cannot find Signature element");
        }

        // Load the certificate containing the public key
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new File(keyPath).toURI().toURL().openStream());
        PublicKey publicKey = cert.getPublicKey();

        // Create a validation context
        DOMValidateContext valContext = new DOMValidateContext(publicKey, nl.item(0));
        valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);
        // Unmarshal the signature
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature = factory.unmarshalXMLSignature(valContext);

        // Validate the signature
        boolean isValid = signature.validate(valContext);

        if (isValid) {
        	
            System.out.println("Signature is valid");
            return "valid";
        } else {
            System.out.println("Signature is invalid");
            return "invalid";
        }
    }
}